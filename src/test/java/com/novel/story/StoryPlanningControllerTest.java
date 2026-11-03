package com.novel.story;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novel.story.controller.StoryPlanningController;
import com.novel.story.service.StoryPlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoryPlanningController.class)
class StoryPlanningControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private StoryPlanningService planningService;

    @Test
    void rejectsAnswerLongerThanDatabaseColumn() throws Exception {
        String payload = "{\"version\":0,\"answers\":[{\"id\":1,\"answer\":\"" + "字".repeat(65536) + "\"}]}";
        mvc.perform(put("/api/stories/1/answers").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
