package com.novel.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class RewriteTextTest {
    @Test
    void replacesOnlySelectedRange() {
        assertEquals("甲新丙", RewriteText.apply("甲乙丙", 1, 2, "乙", "新"));
    }

    @Test
    void rejectsRangeThatNoLongerMatches() {
        assertThrows(StoryConflictException.class,
                () -> RewriteText.apply("甲丁丙", 1, 2, "乙", "新"));
    }
}
