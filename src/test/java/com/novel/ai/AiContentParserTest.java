package com.novel.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiContentParserTest {
    private final AiContentParser parser = new AiContentParser();

    @Test
    void acceptsAtMostThreeNonBlankQuestions() {
        assertEquals(3, parser.questions("{\"questions\":[\"主角是谁？\",\"冲突是什么？\",\"结局怎样？\",\"多余的问题\"]}").size());
    }

    @Test
    void rejectsMalformedPlan() {
        assertThrows(AiServiceException.class, () -> parser.plan("{\"proposal\":\"方案\"}"));
    }
}
