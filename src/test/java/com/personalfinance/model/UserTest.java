package com.personalfinance.model;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @Test
    void samePasswordHasDifferentSaltAndHashForEachUser() {
        User first = new User("alice", "общий пароль");
        User second = new User("bob", "общий пароль");

        assertNotEquals(first.getPasswordSalt(), second.getPasswordSalt());
        assertNotEquals(first.getPasswordHash(), second.getPasswordHash());
        assertEquals(16, Base64.getDecoder().decode(first.getPasswordSalt()).length);
        assertEquals(32, Base64.getDecoder().decode(first.getPasswordHash()).length);
        assertTrue(first.checkPassword("общий пароль"));
        assertFalse(first.checkPassword("неверный пароль"));
        assertFalse(first.checkPassword(null));
    }

    @Test
    void restoreUsesStoredHashWithoutHashingItAgain() {
        User original = new User("alice", " пароль 🔐 ");
        Wallet wallet = new Wallet();
        User restored = User.restore(original.getLogin(), original.getPasswordHash(),
                original.getPasswordSalt(), wallet);

        assertEquals(original.getPasswordHash(), restored.getPasswordHash());
        assertEquals(original.getPasswordSalt(), restored.getPasswordSalt());
        assertSame(wallet, restored.getWallet());
        assertTrue(restored.checkPassword(" пароль 🔐 "));
        assertFalse(restored.checkPassword("пароль 🔐"));
        assertFalse(restored.checkPassword(original.getPasswordHash()));
    }

}
