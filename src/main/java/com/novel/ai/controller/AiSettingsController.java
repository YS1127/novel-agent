package com.novel.ai.controller;

import com.novel.ai.AiSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/settings")
public class AiSettingsController {
    private final AiSettingsService service;
    public AiSettingsController(AiSettingsService service) { this.service = service; }

    public record UpdateSettingsRequest(@NotBlank String baseUrl, @NotBlank String model, String apiKey) {}

    /**
     * 读取可公开的 AI 设置。
     * @return 地址、模型和密钥是否存在；绝不返回密钥明文
     */
    @GetMapping
    public AiSettingsService.PublicSettings get() { return service.get(); }

    /**
     * 保存 AI 设置；空 apiKey 保留原密钥或本机环境变量回退值。
     * @param request 基础地址、模型与可选新密钥
     * @return 不含密钥明文的设置；缺加密主密钥时返回 400
     */
    @PutMapping
    public AiSettingsService.PublicSettings save(@Valid @RequestBody UpdateSettingsRequest request) {
        return service.save(request.baseUrl(), request.model(), request.apiKey());
    }
}
