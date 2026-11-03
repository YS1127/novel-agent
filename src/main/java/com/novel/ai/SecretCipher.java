package com.novel.ai;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] masterKey;

    public SecretCipher(@Value("${AI_CONFIG_ENCRYPTION_KEY:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            this.masterKey = null;
        } else {
            byte[] decoded;
            try { decoded = Base64.getDecoder().decode(encodedKey); }
            catch (IllegalArgumentException error) { throw new IllegalStateException("AI 加密主密钥必须是 Base64", error); }
            if (decoded.length != 32) throw new IllegalStateException("AI 加密主密钥必须是 32 字节");
            this.masterKey = decoded;
        }
    }

    public boolean configured() { return masterKey != null; }

    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception error) { throw new IllegalStateException("AI 密钥加密失败", error); }
    }

    public String decrypt(String encoded) {
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length < 29) throw new IllegalArgumentException("密文长度无效");
            byte[] nonce = Arrays.copyOfRange(combined, 0, 12);
            byte[] ciphertext = Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception error) { throw new IllegalStateException("AI 密钥解密失败，请检查主密钥", error); }
    }

    private void requireKey() {
        if (masterKey == null) throw new IllegalStateException("请在 .env 中配置 AI_CONFIG_ENCRYPTION_KEY");
    }
}
