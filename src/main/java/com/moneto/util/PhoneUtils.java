package com.moneto.util;

public class PhoneUtils {

    public static String normalize(String phone) {
        if (phone == null) return "";

        String onlyNumbers = phone.replaceAll("\\D", "");

        // Se vier sem DDI (ex: 67992149284)
        if (onlyNumbers.length() == 11) {
            return "55" + onlyNumbers;
        }

        // Se já estiver correto (ex: 5567992149284)
        if (onlyNumbers.length() == 13 && onlyNumbers.startsWith("55")) {
            return onlyNumbers;
        }

        return onlyNumbers;
    }
}