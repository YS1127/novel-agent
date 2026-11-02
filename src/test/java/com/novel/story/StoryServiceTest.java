package com.novel.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novel.story.entity.Story;
import com.novel.story.mapper.StoryMapper;
import com.novel.story.service.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StoryServiceTest {
    @Mock
    private StoryMapper mapper;
    private StoryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new StoryService(mapper);
    }

    @Test
    void rejectsBlankTitleAndIdea() {
        assertThrows(IllegalArgumentException.class, () -> service.create("  ", "点子", ""));
        assertThrows(IllegalArgumentException.class, () -> service.create("标题", "  ", ""));
    }

    @Test
    void createsStoryWithInitialVersionAndStage() {
        when(mapper.insert(any(Story.class))).thenAnswer(invocation -> {
            Story story = invocation.getArgument(0);
            story.setId(7L);
            return 1;
        });

        Story created = service.create(" 标题 ", " 点子 ", " 简洁 ");

        assertEquals(7L, created.getId());
        assertEquals("标题", created.getTitle());
        assertEquals("点子", created.getIdea());
        assertEquals("简洁", created.getStyleGuide());
        assertEquals("IDEA", created.getStage());
        assertEquals(0, created.getVersion());
        verify(mapper).insert(any(Story.class));
    }

    @Test
    void rejectsStaleUpdate() {
        Story current = new Story();
        current.setId(7L);
        current.setVersion(3);
        when(mapper.selectById(7L)).thenReturn(current);

        assertThrows(StoryConflictException.class,
                () -> service.update(7L, "标题", "点子", "", "", "", "", 2));
    }
}
