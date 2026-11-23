package com.culturalcenter.common;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // единственный экземпляр
    private static PasswordUtil instance;

    // приватный конструктор
    private PasswordUtil() {}

    // глобальная точка доступа
    public static PasswordUtil getInstance() {
        if (instance == null) {
            instance = new PasswordUtil();
        }
        return instance;
    }


    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }


    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}