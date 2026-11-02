package com.novel.story;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novel.ai.AiChatClient;
import com.novel.story.entity.Story;
import com.novel.story.mapper.StoryCandidateMapper;
import com.novel.story.mapper.StoryMapper;
import com.novel.story.service.StoryService;
import com.novel.story.service.StoryWritingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

class StoryWritingServiceTest {
    private final StoryService stories = Mockito.mock(StoryService.class);
    private final StoryMapper storyMapper = Mockito.mock(StoryMapper.class);
    private final StoryCandidateMapper candidates = Mockito.mock(StoryCandidateMapper.class);
    private final AiChatClient ai = Mockito.mock(AiChatClient.class);
    private final TransactionTemplate transactions = Mockito.mock(TransactionTemplate.class);
    private StoryWritingService service;
    private Story story;

    @BeforeEach
    void setUp() {
        story = new Story();
        story.setId(4L);
        story.setVersion(0);
        story.setStage("IDEA");
        story.setBody("甲乙丙");
        when(stories.get(4L)).thenReturn(story);
        service = new StoryWritingService(stories, storyMapper, candidates, ai, transactions);
    }

    @Test
    void refusesDraftBeforeOutlineConfirmation() {
        assertThrows(StoryConflictException.class, () -> service.generateDraft(4L, 0));
        verify(ai, never()).complete(anyString(), anyString());
    }

    @Test
    void rewriteSelectionMismatchDoesNotCallAiOrChangeStory() {
        assertThrows(StoryConflictException.class,
                () -> service.generateRewrite(4L, 0, 1, 2, "错误片段", "更紧张"));
        verify(ai, never()).complete(anyString(), anyString());
        verify(storyMapper, never()).update(any(), any());
    }
}
