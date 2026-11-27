package com.personalfinance.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    // Получены прежней реализацией User до переноса хеширования; данные только для тестов.
    private static final String LEGACY_PASSWORD = " Пароль до рефакторинга 🔐 ";
    private static final String LEGACY_HASH = "D/FGe38rFAyKvZJtqIpDMH/tPQJIfq+9ol4+ujsgtJs=";
    private static final String LEGACY_SALT = "cuKe3QwOA8AT1gatQwrX6Q==";

    @Test
    void hashesAndVerifiesExactlyAsPreviousImplementation() {
        assertEquals(LEGACY_HASH, PasswordHasher.hash(LEGACY_PASSWORD, LEGACY_SALT));
        assertTrue(PasswordHasher.verify(LEGACY_PASSWORD, LEGACY_HASH, LEGACY_SALT));
        assertFalse(PasswordHasher.verify("wrong", LEGACY_HASH, LEGACY_SALT));
        assertFalse(PasswordHasher.verify(LEGACY_PASSWORD.strip(), LEGACY_HASH, LEGACY_SALT));
        assertFalse(PasswordHasher.verify(null, LEGACY_HASH, LEGACY_SALT));
        assertFalse(PasswordHasher.verify(" ", LEGACY_HASH, LEGACY_SALT));
    }

    @Test
    void rejectsInvalidStoredCredentials() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHasher.validateCredentials("not-base64!", LEGACY_SALT));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHasher.validateCredentials(LEGACY_HASH, "c2hvcnQ="));
    }
}
