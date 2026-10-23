package com.nordgas.ngsvisitor;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PasswordHasherTest {
    @Test public void derivesDeterministicHashAndRejectsWrongPassword() {
        byte[] salt = new byte[PasswordHasher.SALT_BYTES];
        char[] correct = "a-long-admin-password".toCharArray();
        char[] wrong = "a-different-password".toCharArray();
        byte[] expected = PasswordHasher.derive(correct, salt);

        assertArrayEquals(expected, PasswordHasher.derive(correct, salt));
        assertTrue(PasswordHasher.verify(correct, salt, expected));
        assertFalse(PasswordHasher.verify(wrong, salt, expected));
    }
}
