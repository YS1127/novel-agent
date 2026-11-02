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

    @GetMapping
    public AiSettingsService.PublicSettings get() { return service.get(); }

    @PutMapping
    public AiSettingsService.PublicSettings save(@Valid @RequestBody UpdateSettingsRequest request) {
        return service.save(request.baseUrl(), request.model(), request.apiKey());
    }
}
