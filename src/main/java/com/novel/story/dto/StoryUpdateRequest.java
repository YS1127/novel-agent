package com.novel.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoryUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String idea,
        String styleGuide,
        String proposal,
        String outline,
        String body,
        @NotNull Integer version
) {}
