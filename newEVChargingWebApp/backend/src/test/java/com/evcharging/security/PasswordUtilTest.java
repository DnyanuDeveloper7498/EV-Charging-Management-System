package com.evcharging.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashedPasswordMatchesOnlyTheOriginal() {
        String hash = PasswordUtil.hash("Secret#123");

        assertTrue(PasswordUtil.isHashed(hash));
        assertTrue(PasswordUtil.matches("Secret#123", hash));
        assertFalse(PasswordUtil.matches("secret#123", hash));
    }

    @Test
    void sameSecretGivesDifferentHashesBecauseOfSalt() {
        assertNotEquals(PasswordUtil.hash("Secret#123"), PasswordUtil.hash("Secret#123"));
    }

    @Test
    void legacyPlainTextPasswordsStillMatch() {
        assertFalse(PasswordUtil.isHashed("oldpassword"));
        assertTrue(PasswordUtil.matches("oldpassword", "oldpassword"));
        assertFalse(PasswordUtil.matches("other", "oldpassword"));
    }

    @Test
    void blankPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(" "));
    }
}
