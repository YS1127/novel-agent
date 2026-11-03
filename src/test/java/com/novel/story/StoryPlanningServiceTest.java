package com.novel.story;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;

import com.novel.ai.AiChatClient;
import com.novel.ai.AiContentParser;
import com.novel.ai.AiServiceException;
import com.novel.story.entity.Story;
import com.novel.story.entity.StoryQuestion;
import com.novel.story.mapper.StoryMapper;
import com.novel.story.mapper.StoryQuestionMapper;
import com.novel.story.service.StoryPlanningService;
import com.novel.story.service.StoryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

class StoryPlanningServiceTest {
    private final StoryService stories = Mockito.mock(StoryService.class);
    private final StoryMapper storyMapper = Mockito.mock(StoryMapper.class);
    private final StoryQuestionMapper questions = Mockito.mock(StoryQuestionMapper.class);
    private final AiChatClient ai = Mockito.mock(AiChatClient.class);
    private final TransactionTemplate transactions = Mockito.mock(TransactionTemplate.class);
    private StoryPlanningService service;

    @BeforeEach
    void setUp() {
        Story story = new Story();
        story.setId(3L);
        story.setVersion(0);
        story.setIdea("点子");
        story.setTitle("标题");
        story.setStage("IDEA");
        when(stories.get(3L)).thenReturn(story);
        when(questions.selectList(any())).thenReturn(List.of());
        service = new StoryPlanningService(stories, storyMapper, questions, ai, new AiContentParser(), transactions);
    }

    @Test
    void malformedAiQuestionsDoNotChangeStory() {
        when(ai.complete(anyString(), anyString())).thenReturn("not-json");
        assertThrows(AiServiceException.class, () -> service.generateQuestions(3L, 0));
        verify(storyMapper, never()).update(any(), any());
        verify(questions, never()).insert(any(StoryQuestion.class));
    }

    @Test
    void aiFailureDoesNotChangeStoryPlan() {
        when(ai.complete(anyString(), anyString())).thenThrow(new AiServiceException("服务不可用"));
        assertThrows(AiServiceException.class, () -> service.generatePlan(3L, 0));
        verify(storyMapper, never()).update(any(), any());
    }

    @Test
    void cannotConfirmOldPlanAfterAnswersChanged() {
        Story story = stories.get(3L);
        story.setStage("QUESTIONS");
        story.setProposal("旧方案");
        story.setOutline("旧大纲");
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(Mockito.mock(TransactionStatus.class));
        });

        assertThrows(StoryConflictException.class, () -> service.confirmPlan(3L, 0));
        verifyNoInteractions(storyMapper);
    }
}
