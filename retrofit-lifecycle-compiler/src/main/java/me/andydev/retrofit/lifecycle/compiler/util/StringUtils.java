package me.andydev.retrofit.lifecycle.compiler.util;

import java.util.Iterator;

/**
 * Description:
 * Created by Andy on 2017/7/4
 */

public class StringUtils {

    public static String join(Iterator iterator, String separator) {
        if (separator == null) {
            separator = "";
        }

        StringBuilder buf = new StringBuilder(256);

        while (iterator.hasNext()) {
            buf.append(iterator.next());
            if (iterator.hasNext()) {
                buf.append(separator);
            }
        }

        return buf.toString();
    }
}
