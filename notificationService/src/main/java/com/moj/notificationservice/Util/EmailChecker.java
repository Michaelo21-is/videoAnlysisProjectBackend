package com.moj.notificationservice.Util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailChecker {
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
