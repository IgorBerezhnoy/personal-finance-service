package com.personalfinance.model;

public class User {
    private final String login;
    private final String password;

    public User(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "Логин не должен быть пустым"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Пароль не должен быть пустым"
            );
        }

        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }
}