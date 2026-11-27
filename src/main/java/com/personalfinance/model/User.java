package com.personalfinance.model;

import com.personalfinance.security.PasswordHasher;

public class User {
    private final String login;
    private final String passwordHash;
    private final String passwordSalt;
    private final Wallet wallet;

    public User(String login, String password) {
        validateLogin(login);

        this.login = login;
        this.passwordSalt = PasswordHasher.generateSalt();
        this.passwordHash = PasswordHasher.hash(password, passwordSalt);
        this.wallet = new Wallet();
    }

    private User(String login, String passwordHash, String passwordSalt, Wallet wallet) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.wallet = wallet;
    }

    public static User restore(String login, String passwordHash, String passwordSalt, Wallet wallet) {
        validateLogin(login);
        if (wallet == null) {
            throw new IllegalArgumentException("Кошелёк не может быть null");
        }

        PasswordHasher.validateCredentials(passwordHash, passwordSalt);
        return new User(login, passwordHash, passwordSalt, wallet);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getLogin() {
        return login;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public boolean checkPassword(String candidate) {
        return PasswordHasher.verify(candidate, passwordHash, passwordSalt);
    }

    private static void validateLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не должен быть пустым");
        }
    }
}
