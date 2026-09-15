package com.huige233.transcend.util;


/** 限制可强制返回的方法及类型，并校验编辑器输入的布尔值和非负有限生命值。 */
public final class EditorReturnValues {
    private EditorReturnValues() {}

    public static String type(String method) {
        return switch (method) {
            case "getHealth" -> "float";
            case "hurt", "isDeadOrDying" -> "boolean";
            default -> throw new IllegalArgumentException("没有该方法的强制返回注入点: " + method);
        };
    }

    public static Object parse(String method, String suppliedType, String text) {
        String type = type(method);
        String supplied = suppliedType == null ? "" : suppliedType.split(" // ", 2)[0].trim();
        if (!type.equals(supplied)) throw new IllegalArgumentException("返回类型不匹配: " + supplied);
        String value = text == null ? "" : text.trim();
        if (type.equals("boolean")) {
            if (value.equalsIgnoreCase("true")) return Boolean.TRUE;
            if (value.equalsIgnoreCase("false")) return Boolean.FALSE;
            throw new IllegalArgumentException("布尔值必须为 true 或 false");
        }
        float number = Float.parseFloat(value);
        if (!Float.isFinite(number) || number < 0) throw new IllegalArgumentException("生命值必须为非负有限数字");
        return number;
    }
}
