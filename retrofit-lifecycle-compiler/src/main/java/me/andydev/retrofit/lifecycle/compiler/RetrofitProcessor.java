package me.andydev.retrofit.lifecycle.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import me.andydev.retrofit.lifecycle.common.Cancellable;
import me.andydev.retrofit.lifecycle.common.RetrofitInterface;
import me.andydev.retrofit.lifecycle.compiler.util.StringUtils;
import retrofit2.Call;

/**
 * Description: Processor for RetrofitLifecycle to generate proxy class
 * Created by Andy on 2017/7/4
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class RetrofitProcessor extends AbstractProcessor {

    private ProcessingEnvironment mProcessingEnvironment;
    private Messager mMessager;
    private Filer mFiler;

    private String mInterfaceImplFieldName = "mInterfaceImpl";
    private String mCallListFieldName = "mCallList";
    private String mGeneratedClassSuffix = "InvokeProxy";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mProcessingEnvironment = processingEnv;
        mMessager = processingEnv.getMessager();
        mFiler = mProcessingEnvironment.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        LinkedHashSet<String> annotations = new LinkedHashSet<>();
        annotations.add(RetrofitInterface.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(RetrofitInterface.class);
        for (Element element : elementsAnnotatedWith) {
            if (!element.getKind().isInterface()) {
                continue;
            }

            List<MethodSpec> methodSpecList = new ArrayList<>();
            for (Element enclosedElement : element.getEnclosedElements()) {
                //just process methods in Interface, ignored fields etc.
                if (enclosedElement.getKind() == ElementKind.METHOD) {
                    ExecutableElement methodElement = (ExecutableElement) enclosedElement;
                    MethodInfo methodInfo = generateMethodInfo(methodElement);
                    MethodSpec methodSpec = generateProxyMethod(methodInfo);
                    methodSpecList.add(methodSpec);
                }
            }

            //get TypeName of interface annotated by @RetrofitInterface
            TypeName interfaceTypeName = getTypeName(element);
            TypeName cancellableInterfaceTypeName = ClassName.get(Cancellable.class);

            FieldSpec callListFieldSpec = generateCallListFieldSpec();
            FieldSpec interfaceImplFieldSpec = generateInterfaceImplFieldSpec(interfaceTypeName);
            MethodSpec constructorMethodSpec = generateConstructorMethod(interfaceTypeName);

            String generatedProxyClassName = getSimpleName(element) + mGeneratedClassSuffix;
            String generatedProxyClassPath = getOutputPackagePath(element);

            TypeSpec classSpec = TypeSpec.classBuilder(generatedProxyClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(interfaceTypeName)
                    .addSuperinterface(cancellableInterfaceTypeName)
                    .addField(interfaceImplFieldSpec)
                    .addField(callListFieldSpec)
                    .addMethod(constructorMethodSpec)
                    .addMethods(methodSpecList)
                    .addMethod(generateCancelMethod())
                    .build();

            JavaFile javaFile = JavaFile.builder(generatedProxyClassPath, classSpec)
                    .build();

            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                mMessager.printMessage(Kind.ERROR, "fail to write to file.");
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private FieldSpec generateInterfaceImplFieldSpec(TypeName interfaceTypeName) {
        return FieldSpec.builder(interfaceTypeName, mInterfaceImplFieldName, Modifier.PRIVATE).build();
    }

    private FieldSpec generateCallListFieldSpec() {
        ParameterizedTypeName callListTypeName = ParameterizedTypeName.get(List.class, Call.class);
        ParameterizedTypeName callListInitializeTypeName = ParameterizedTypeName.get(ArrayList.class, Call.class);

        ClassName collectionsClassName = ClassName.get("java.util", "Collections");
        return FieldSpec
                .builder(callListTypeName, mCallListFieldName, Modifier.PRIVATE)
                .initializer("$T.synchronizedList(new $T())", collectionsClassName, callListInitializeTypeName)
                .build();
    }

    private MethodSpec generateProxyMethod(MethodInfo methodInfo) {
        //Call<T> call = mInterfaceImpl.xxx();
        TypeName proxyMethodReturnType = methodInfo.getTypeName();
        String proxyMethodName = methodInfo.getMethodName();
        String proxyMethodParams = StringUtils.join(methodInfo.getMethodParametersSimple().iterator(), ",");
        String proxyMethodStatement = "$T call = $L.$L($L)";
        //mCallList.add(call);
        String recordCallStatement = "$L.add(call)";
        //return call;
        String returnCallStatement = "return call";

        return MethodSpec.methodBuilder(methodInfo.getMethodName())
                .returns(methodInfo.getTypeName())
                .addModifiers(methodInfo.getMethodModifiers())
                .addParameters(methodInfo.getMethodParameters())
                .addStatement(proxyMethodStatement, proxyMethodReturnType,
                        mInterfaceImplFieldName, proxyMethodName, proxyMethodParams)
                .addStatement(recordCallStatement, mCallListFieldName)
                .addStatement(returnCallStatement)
                .build();
    }

    private MethodSpec generateCancelMethod() {
        String excludeCalls = "excludes";
        String code = ""
                + "if ($L.length > 0) {\n" //excludeCalls
                + "    $L.removeAll($T.asList($L));\n" //mCallListFieldName, Arrays, excludeCalls
                + "}\n"
                + "if ($L != null) {\n" //mCallListFieldName
                + "   for (Call call : $L) {\n" //mCallListFieldName
                + "       if (call != null && !call.isCanceled()) {\n"
                + "           call.cancel();\n"
                + "       }\n"
                + " }\n"
                + " $L.clear();\n" //mCallListFieldName
                + " $L = null;\n" //mCallListFieldName
                + "}\n";

        return MethodSpec.methodBuilder("cancelAll")
                .varargs()
                .addParameter(ArrayTypeName.of(Call.class), excludeCalls)
                .addModifiers(Modifier.PUBLIC)
                .addCode(code, excludeCalls, mCallListFieldName, ClassName.get(Arrays.class),
                        excludeCalls, mCallListFieldName, mCallListFieldName, mCallListFieldName,
                        mCallListFieldName)
                .build();
    }

    private MethodSpec generateConstructorMethod(TypeName interfaceTypeName) {
        String parameterName = "apiImplGeneratedByRetrofit";
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(interfaceTypeName, parameterName)
                .addStatement("$L = $L", mInterfaceImplFieldName, parameterName)
                .build();
    }

    private MethodInfo generateMethodInfo(ExecutableElement methodElement) {
        //modifiers
        ArrayList<Modifier> methodModifiers = new ArrayList<>();
        methodModifiers.add(Modifier.PUBLIC);
        //name
        String methodName = getSimpleName(methodElement);
        //params
        List<VariableElement> methodParams = new ArrayList<>();
        for (VariableElement typeParameterElement : methodElement.getParameters()) {
            methodParams.add(typeParameterElement);
        }
        //return type
        TypeMirror methodReturnType = methodElement.getReturnType();
        return new MethodInfo().setMethodName(methodName)
                .setMethodModifiers(methodModifiers)
                .setMethodParameters(methodParams)
                .setMethodReturnType(methodReturnType);
    }

    private TypeName getTypeName(Element element) {
        TypeMirror interfaceTypeMirror = element.asType();
        return ClassName.get(interfaceTypeMirror);
    }

    private String getSimpleName(Element element) {
        return element.getSimpleName().toString();
    }

    private String getOutputPackagePath(Element element) {
        String interfacePath = ((TypeElement) element).getQualifiedName().toString();
        int i = interfacePath.lastIndexOf(".");
        if (i != -1) {
            return interfacePath.substring(0, i);
        }
        return "me.andydev.retrofit.lifecycle.proxy";
    }
}