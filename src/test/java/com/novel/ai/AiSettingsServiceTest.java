package com.novel.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.novel.ai.entity.AiSettings;
import com.novel.ai.mapper.AiSettingsMapper;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiSettingsServiceTest {
    private final AiSettingsMapper mapper = Mockito.mock(AiSettingsMapper.class);

    @Test
    void storesCiphertextAndNeverReturnsPlaintext() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        AiSettingsService service = new AiSettingsService(mapper, new SecretCipher(key), "", "", "");
        when(mapper.selectById(1L)).thenReturn(null);
        when(mapper.insert(any(AiSettings.class))).thenAnswer(invocation -> {
            AiSettings saved = invocation.getArgument(0);
            assertFalse(saved.getEncryptedApiKey().contains("sk-secret-value"));
            return 1;
        });

        var response = service.save("https://api.example.com/v1", "fiction-model", "sk-secret-value");
        assertEquals("https://api.example.com/v1", response.baseUrl());
        assertEquals(true, response.hasKey());
        assertFalse(response.toString().contains("sk-secret-value"));
    }

    @Test
    void refusesKeyPersistenceWithoutMasterKey() {
        AiSettingsService service = new AiSettingsService(mapper, new SecretCipher(""), "", "", "");
        assertThrows(IllegalStateException.class,
                () -> service.save("https://api.example.com/v1", "fiction-model", "sk-secret-value"));
    }

    @Test
    void rejectsBaseUrlWithQueryThatWouldMisrouteChatCompletion() {
        AiSettingsService service = new AiSettingsService(mapper, new SecretCipher(""), "", "", "");
        assertThrows(IllegalArgumentException.class,
                () -> service.save("https://api.example.com/v1?token=value", "fiction-model", ""));
    }
}
