package com.evcharging.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Secure password hashing without an external dependency. */
public final class PasswordUtil {
    private static final String PREFIX = "PBKDF2";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordUtil() {}

    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean matches(String password, String storedValue) {
        if (password == null || storedValue == null || storedValue.isBlank()) {
            return false;
        }
        if (!isHashed(storedValue)) {
            // Backward compatibility for existing records. Successful legacy
            // logins are re-hashed by UserService.
            return MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    storedValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            String[] parts = storedValue.split("\\$", -1);
            if (parts.length != 4 || !PREFIX.equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHashed(String value) {
        return value != null && value.startsWith(PREFIX + "$");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password.", e);
        }
    }
}
