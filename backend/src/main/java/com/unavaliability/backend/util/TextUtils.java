package com.unavaliability.backend.util;

import java.util.regex.Pattern;


public final class TextUtils {

    private static final Pattern EMAIL_RE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private TextUtils() {
    }

    public static String cleanText(Object str) {
        return cleanText(str, 500);
    }

    public static String cleanText(Object str, int maxLen) {
        if (str == null) {
            return "";
        }
        String s = String.valueOf(str).trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_RE.matcher(email).matches();
    }
    public static boolean toBool(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() == 1;
        }
        if (v instanceof String s) {
            return "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        return false;
    }
}
