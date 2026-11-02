package com.novel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.novel.story.mapper.StoryMapper;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
class ApplicationTests {
    @MockitoBean
    private StoryMapper storyMapper;

    @Test
    void contextLoads() {
    }
}
