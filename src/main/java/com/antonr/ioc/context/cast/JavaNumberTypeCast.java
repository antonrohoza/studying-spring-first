package com.antonr.ioc.context.cast;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JavaNumberTypeCast {
    ;
//    SHORT("short", Short.class),
//    LONG("long", Long.class),
//    FLOAT("float", Float.class),
//    DOUBLE("double", Double.class),
//    BYTE("byte", Byte.class),
//    BOOLEAN("boolean", Boolean.class),
//    INTEGER("int", Integer.class);

    private static final String SHORT = "short";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BYTE = "byte";
    private static final String BOOLEAN = "boolean";
    private static final String INT = "int";

//    private final String primitive;
//    private final Class<?> wrapperClass;

//    public static Object castPrimitive(String value, Class<?> clazz) {
//        return Arrays.stream(JavaNumberTypeCast.values())
//                     .filter(type -> clazz.getName().equals(type.getPrimitive()))
//                     .map(type -> type.getWrapperClass().cast(value))
//                     .findFirst()
//                     .orElseThrow(RuntimeException::new);
//    }

    public static Object castPrimitive(String value, Class<?> clazz) {
        if (clazz.getName().equals(INT)) {
            return Integer.parseInt(value);
        }
        if (clazz.getName().equals(BOOLEAN)) {
            return Boolean.valueOf(value);
        }
        if (clazz.getName().equals(BYTE)) {
            return Byte.valueOf(value);
        }
        if (clazz.getName().equals(DOUBLE)) {
            return Double.valueOf(value);
        }
        if (clazz.getName().equals(FLOAT)) {
            return Float.parseFloat(value);
        }
        if (clazz.getName().equals(LONG)) {
            return Long.parseLong(value);
        }
        if (clazz.getName().equals(SHORT)) {
            return Short.parseShort(value);
        }
        return null;
    }

}
