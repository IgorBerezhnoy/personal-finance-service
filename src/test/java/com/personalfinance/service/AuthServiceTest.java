package com.personalfinance.service;

import com.personalfinance.model.Transaction;
import com.personalfinance.model.TransactionType;
import com.personalfinance.model.User;
import com.personalfinance.persistence.JsonFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    @TempDir
    Path directory;

    @Test
    void authenticatesPreviousVersionFileAndPreservesWalletAfterAnotherSave() throws IOException {
        Path file = directory.resolve("users.json");
        try (InputStream fixture = getClass().getResourceAsStream("/persistence/legacy-users-v1.json")) {
            assertNotNull(fixture);
            Files.copy(fixture, file);
        }
        JsonFileStorage storage = new JsonFileStorage(file);
        AuthService auth = new AuthService();
        auth.load(storage);
        assertThrows(IllegalArgumentException.class, () -> auth.login("legacy-student", "wrong"));
        auth.login("legacy-student", " Пароль до рефакторинга 🔐 ");
        User restored = auth.getCurrentUser();
        assertEquals(new BigDecimal("30.25"), restored.getWallet().getBalance());
        assertEquals(new BigDecimal("87.75"), restored.getWallet().getRemainingBudget(" еДА "));
        assertEquals(LocalDateTime.parse("2020-02-29T13:45:12.123456789"),
                restored.getWallet().getTransactions().getFirst().getCreatedAt());

        auth.save(storage);
        AuthService restarted = new AuthService();
        restarted.load(storage);
        restarted.login("legacy-student", " Пароль до рефакторинга 🔐 ");
        assertEquals(restored.getPasswordHash(), restarted.getCurrentUser().getPasswordHash());
        assertEquals(restored.getPasswordSalt(), restarted.getCurrentUser().getPasswordSalt());
        assertEquals(new BigDecimal("30.25"), restarted.getCurrentUser().getWallet().getBalance());
        assertEquals(2, restarted.getCurrentUser().getWallet().getTransactions().size());
    }

    @Test
    void registeredUsersCanLogInAfterRestartWithIndependentWallets() throws IOException {
        AuthService original = new AuthService();
        original.register("alice", "alice-secret");
        original.register("bob", "bob-secret");
        original.login("alice", "alice-secret");
        original.getCurrentUser().getWallet().addTransaction(
                new Transaction(new BigDecimal("42.50"), TransactionType.INCOME, "Подарок"));
        JsonFileStorage storage = new JsonFileStorage(directory.resolve("users.json"));
        original.save(storage);

        AuthService restarted = new AuthService();
        restarted.load(storage);
        assertNull(restarted.getCurrentUser());
        assertThrows(IllegalArgumentException.class, () -> restarted.login("alice", "wrong"));
        assertThrows(IllegalArgumentException.class, () -> restarted.login("alice", "bob-secret"));
        assertThrows(IllegalArgumentException.class, () -> restarted.login("unknown", "alice-secret"));
        assertNull(restarted.getCurrentUser());
        restarted.login("alice", "alice-secret");
        User alice = restarted.getCurrentUser();
        assertEquals(new BigDecimal("42.50"), alice.getWallet().getBalance());
        assertThrows(IllegalStateException.class, () -> restarted.login("bob", "bob-secret"));
        restarted.logout();
        restarted.login("bob", "bob-secret");
        assertEquals("bob", restarted.getCurrentUser().getLogin());
        assertEquals(BigDecimal.ZERO, restarted.getCurrentUser().getWallet().getBalance());
        assertTrue(restarted.getCurrentUser().getWallet().getTransactions().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> restarted.register("alice", "new-secret"));

        restarted.register("charlie", "charlie-secret");
        restarted.save(storage);
        AuthService nextRestart = new AuthService();
        nextRestart.load(storage);
        nextRestart.login("charlie", "charlie-secret");
        assertEquals(3, nextRestart.getUsers().size());
    }

}
