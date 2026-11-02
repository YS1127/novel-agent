package com.novel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.novel.story.mapper.StoryMapper;
import com.novel.story.mapper.StoryQuestionMapper;
import com.novel.ai.mapper.AiSettingsMapper;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
class ApplicationTests {
    @MockitoBean
    private StoryMapper storyMapper;
    @MockitoBean
    private StoryQuestionMapper storyQuestionMapper;
    @MockitoBean
    private AiSettingsMapper aiSettingsMapper;
    @MockitoBean
    private TransactionTemplate transactionTemplate;

    @Test
    void contextLoads() {
    }
}
