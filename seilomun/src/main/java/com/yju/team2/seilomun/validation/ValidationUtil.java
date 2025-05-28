package com.yju.team2.seilomun.validation;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ValidationUtil {

    private final SecureRandom random = new SecureRandom();

    public String createCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public boolean isValidPhone(String phone) {
        return phone != null && phone.length() == 10 && phone.matches("^[0-9]{10}");
    }

}
