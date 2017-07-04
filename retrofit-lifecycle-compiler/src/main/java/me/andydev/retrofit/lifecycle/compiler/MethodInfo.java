package me.andydev.retrofit.lifecycle.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Description: Helper class to record method info needed by Processor
 * Created by Andy on 2017/7/4
 */

public class MethodInfo {
    private String methodName;
    private List<Modifier> methodModifiers;
    private List<VariableElement> methodParameters;
    private TypeMirror methodReturnType;

    public MethodInfo() {
    }

    public MethodInfo setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public MethodInfo setMethodModifiers(List<Modifier> methodModifiers) {
        this.methodModifiers = methodModifiers;
        return this;
    }

    public MethodInfo setMethodParameters(List<VariableElement> methodParameters) {
        this.methodParameters = methodParameters;
        return this;
    }

    public MethodInfo setMethodReturnType(TypeMirror methodReturnType) {
        this.methodReturnType = methodReturnType;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Modifier> getMethodModifiers() {
        return methodModifiers;
    }

    public List<ParameterSpec> getMethodParameters() {
        List<ParameterSpec> parameterSpecs = new ArrayList<>();
        for (VariableElement variableElement : methodParameters) {
            parameterSpecs.add(ParameterSpec.get(variableElement));
        }
        return parameterSpecs;
    }

    public List<String> getMethodParametersSimple() {
        List<String> params = new ArrayList<>();
        for (VariableElement methodParameter : methodParameters) {
            params.add(methodParameter.getSimpleName().toString());
        }
        return params;
    }

    public TypeName getTypeName() {
        return ClassName.get(methodReturnType);
    }
}
