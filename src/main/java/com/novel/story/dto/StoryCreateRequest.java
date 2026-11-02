package com.novel.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String idea,
        String styleGuide
) {}
