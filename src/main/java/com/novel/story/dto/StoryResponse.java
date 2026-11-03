package com.novel.story.dto;

import com.novel.story.entity.Story;
import java.time.LocalDateTime;

public record StoryResponse(
        Long id, String title, String idea, String styleGuide, String proposal,
        String outline, String body, String stage, Integer version, LocalDateTime updatedAt
) {
    public static StoryResponse from(Story story) {
        return new StoryResponse(story.getId(), story.getTitle(), story.getIdea(),
                story.getStyleGuide(), story.getProposal(), story.getOutline(), story.getBody(),
                story.getStage(), story.getVersion(), story.getUpdatedAt());
    }
}
