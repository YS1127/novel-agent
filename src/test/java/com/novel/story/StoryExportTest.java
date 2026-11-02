package com.novel.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class StoryExportTest {
    @Test
    void rendersMarkdownAndTextWithoutInternalFields() {
        assertEquals("# 标题\n\n第一段\n", StoryExport.content("标题", "第一段", "md"));
        assertEquals("标题\n\n第一段\n", StoryExport.content("标题", "第一段", "txt"));
    }

    @Test
    void makesFilenameSafeAndRejectsUnknownFormat() {
        assertEquals("我的作品.md", StoryExport.filename("../我的作品", "md"));
        assertThrows(IllegalArgumentException.class, () -> StoryExport.content("标题", "正文", "pdf"));
    }
}
