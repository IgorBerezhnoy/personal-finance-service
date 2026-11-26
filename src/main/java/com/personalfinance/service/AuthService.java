package com.personalfinance.service;

import com.personalfinance.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private final Map<String, User> users;
    private User currentUser;


    public AuthService() {
        currentUser = null;
        users = new HashMap<>();

    }

    public void register(String login, String password) {
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

        if (users.containsKey(login)) {
            throw new IllegalArgumentException(
                    "Пользователь с логином '" + login + "' уже существует"
            );
        }

        User newUser = new User(login, password);

        users.put(login, newUser);
    }

    public void login(String login, String password) {
        if (currentUser != null) {
            throw new IllegalStateException(
                    "Сначала выйдите из текущего аккаунта"
            );
        }

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

        User user = users.get(login);

        if (user == null || !user.checkPassword(password)) {
            throw new IllegalArgumentException(
                    "Неверный логин или пароль"
            );
        }

        currentUser = user;

    }


    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Map<String, User> getUsers() {
        return Collections.unmodifiableMap(users);
    }

}
