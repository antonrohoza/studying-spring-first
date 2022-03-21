package com.antonr.ioc.context.cast;

public enum JavaNumberTypeCast {
    ;

    private static final String SHORT = "short";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BYTE = "byte";
    private static final String BOOLEAN = "boolean";
    private static final String INT = "int";

    public static Object castPrimitive(String value, Class<?> clazz) {
        if (clazz.getName().equals(INT)) {
            return Integer.parseInt(value);
        }
        if (clazz.getName().equals(BOOLEAN)) {
            return Boolean.parseBoolean(value);
        }
        if (clazz.getName().equals(BYTE)) {
            return Byte.parseByte(value);
        }
        if (clazz.getName().equals(DOUBLE)) {
            return Double.parseDouble(value);
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
