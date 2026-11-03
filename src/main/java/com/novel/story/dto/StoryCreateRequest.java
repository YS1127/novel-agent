package com.novel.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 65535) String idea,
        @Size(max = 65535) String styleGuide
) {}
