package com.personalfinance.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    private static final int PASSWORD_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не должен быть пустым");
        }
        byte[] saltBytes = decodeCredential(salt, SALT_BYTES);
        return Base64.getEncoder().encodeToString(deriveHash(password, saltBytes));
    }

    public static boolean verify(String candidate, String storedHash, String storedSalt) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        byte[] expected = decodeCredential(storedHash, HASH_BYTES);
        byte[] salt = decodeCredential(storedSalt, SALT_BYTES);
        byte[] actual = deriveHash(candidate, salt);
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            Arrays.fill(actual, (byte) 0);
        }
    }

    public static void validateCredentials(String hash, String salt) {
        decodeCredential(hash, HASH_BYTES);
        decodeCredential(salt, SALT_BYTES);
    }

    private static byte[] deriveHash(String password, byte[] salt) {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, PASSWORD_ITERATIONS, HASH_BYTES * 8);
        Arrays.fill(characters, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Не удалось вычислить хеш пароля", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] decodeCredential(String encoded, int expectedLength) {
        if (encoded == null) {
            throw new IllegalArgumentException("Хеш и соль не могут быть null");
        }
        byte[] decoded = Base64.getDecoder().decode(encoded);
        if (decoded.length != expectedLength) {
            throw new IllegalArgumentException("Некорректная длина хеша или соли");
        }
        return decoded;
    }
}
