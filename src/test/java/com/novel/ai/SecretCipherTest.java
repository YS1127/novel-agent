package com.novel.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecretCipherTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsWithoutStoringPlaintextAndDecrypts() {
        SecretCipher cipher = new SecretCipher(KEY);
        String encrypted = cipher.encrypt("sk-secret-value");
        assertFalse(encrypted.contains("sk-secret-value"));
        assertEquals("sk-secret-value", cipher.decrypt(encrypted));
    }

    @Test
    void refusesEncryptionWithoutMasterKey() {
        SecretCipher cipher = new SecretCipher("");
        assertThrows(IllegalStateException.class, () -> cipher.encrypt("sk-secret-value"));
    }
}
