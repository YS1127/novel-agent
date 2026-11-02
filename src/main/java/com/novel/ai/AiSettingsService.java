package com.novel.ai;

import com.novel.ai.entity.AiSettings;
import com.novel.ai.mapper.AiSettingsMapper;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSettingsService {
    private final AiSettingsMapper mapper;
    private final SecretCipher cipher;
    private final String envBaseUrl;
    private final String envModel;
    private final String envApiKey;

    public record PublicSettings(String baseUrl, String model, boolean hasKey, boolean canSaveKey) {}
    public record ResolvedSettings(String baseUrl, String model, String apiKey) {}

    public AiSettingsService(AiSettingsMapper mapper, SecretCipher cipher,
            @Value("${AI_BASE_URL:}") String envBaseUrl,
            @Value("${AI_MODEL:}") String envModel,
            @Value("${AI_API_KEY:}") String envApiKey) {
        this.mapper = mapper;
        this.cipher = cipher;
        this.envBaseUrl = envBaseUrl;
        this.envModel = envModel;
        this.envApiKey = envApiKey;
    }

    @Transactional(readOnly = true)
    public PublicSettings get() {
        AiSettings stored = mapper.selectById(1L);
        return publicView(stored);
    }

    @Transactional
    public PublicSettings save(String baseUrl, String model, String apiKey) {
        String validatedUrl = validateBaseUrl(baseUrl);
        if (model == null || model.isBlank() || model.length() > 120) {
            throw new IllegalArgumentException("请输入有效的模型名称");
        }
        AiSettings stored = mapper.selectById(1L);
        String encrypted = stored == null ? null : stored.getEncryptedApiKey();
        if (apiKey != null && !apiKey.isBlank()) encrypted = cipher.encrypt(apiKey.trim());
        AiSettings next = new AiSettings();
        next.setId(1L);
        next.setBaseUrl(validatedUrl);
        next.setModelName(model.trim());
        next.setEncryptedApiKey(encrypted);
        next.setUpdatedAt(LocalDateTime.now());
        if (stored == null) mapper.insert(next);
        else mapper.updateById(next);
        return publicView(next);
    }

    @Transactional(readOnly = true)
    public ResolvedSettings resolved() {
        AiSettings stored = mapper.selectById(1L);
        String baseUrl = stored == null ? envBaseUrl : stored.getBaseUrl();
        String model = stored == null ? envModel : stored.getModelName();
        String apiKey = stored != null && stored.getEncryptedApiKey() != null
                ? cipher.decrypt(stored.getEncryptedApiKey()) : envApiKey;
        if (baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()
                || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置 AI 服务地址、模型和 API Key");
        }
        return new ResolvedSettings(validateBaseUrl(baseUrl), model, apiKey);
    }

    private PublicSettings publicView(AiSettings stored) {
        return new PublicSettings(stored == null ? envBaseUrl : stored.getBaseUrl(),
                stored == null ? envModel : stored.getModelName(),
                (stored != null && stored.getEncryptedApiKey() != null) || !envApiKey.isBlank(),
                cipher.configured());
    }

    private String validateBaseUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 500) {
            throw new IllegalArgumentException("请输入有效的 AI 服务地址");
        }
        URI uri;
        try { uri = URI.create(value.trim()); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("AI 服务地址格式无效"); }
        String host = uri.getHost();
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        if (host == null || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || (local && "http".equalsIgnoreCase(uri.getScheme())))) {
            throw new IllegalArgumentException("AI 服务地址必须使用 HTTPS，本机地址可使用 HTTP");
        }
        return value.trim().replaceAll("/+$", "");
    }
}
