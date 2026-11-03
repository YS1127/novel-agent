package com.novel.story.controller;

import com.novel.story.dto.StoryResponse;
import com.novel.story.entity.StoryQuestion;
import com.novel.story.service.StoryPlanningService;
import com.novel.story.service.StoryPlanningService.Answer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories/{storyId}")
public class StoryPlanningController {
    private final StoryPlanningService service;
    public StoryPlanningController(StoryPlanningService service) { this.service = service; }

    public record VersionRequest(@NotNull Integer version) {}
    public record AnswersRequest(@NotNull Integer version, @NotNull List<@Valid Answer> answers) {}
    public record QuestionResponse(Long id, int ordinal, String question, String answer) {
        static QuestionResponse from(StoryQuestion item) {
            return new QuestionResponse(item.getId(), item.getOrdinal(), item.getQuestion(), item.getAnswer());
        }
    }

    /**
     * 读取当前作品的问题与回答。
     * @param storyId 作品 ID
     * @return 按提问顺序排列的问题；作品不存在返回 404
     */
    @GetMapping("/questions")
    public List<QuestionResponse> questions(@PathVariable long storyId) {
        return service.listQuestions(storyId).stream().map(QuestionResponse::from).toList();
    }

    /**
     * 生成最多三个问题并替换旧问题；AI 失败时原问题不变。
     * @param storyId 作品 ID
     * @param request 当前作品版本
     * @return 新问题；版本冲突返回 409，AI 失败返回 502
     */
    @PostMapping("/questions")
    public List<QuestionResponse> generateQuestions(@PathVariable long storyId,
            @Valid @RequestBody VersionRequest request) {
        return service.generateQuestions(storyId, request.version()).stream().map(QuestionResponse::from).toList();
    }

    /**
     * 保存问题回答；空回答表示跳过该问题。
     * @param storyId 作品 ID
     * @param request 当前版本及属于该作品的问题回答
     * @return 已保存的问题；问题归属错误返回 400，版本冲突返回 409
     */
    @PutMapping("/answers")
    public List<QuestionResponse> answers(@PathVariable long storyId,
            @Valid @RequestBody AnswersRequest request) {
        return service.saveAnswers(storyId, request.version(), request.answers()).stream()
                .map(QuestionResponse::from).toList();
    }

    /**
     * 依据点子、文风与已保存回答生成可编辑方案和大纲。
     * @param storyId 作品 ID
     * @param request 当前作品版本
     * @return 阶段为 PLAN 的作品；AI 失败返回 502
     */
    @PostMapping("/plan")
    public StoryResponse generatePlan(@PathVariable long storyId, @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(service.generatePlan(storyId, request.version()));
    }

    /**
     * 确认当前方案和大纲，之后才可生成正文候选。
     * @param storyId 作品 ID
     * @param request 当前作品版本
     * @return 阶段为 CONFIRMED 的作品；旧方案或版本返回 409
     */
    @PostMapping("/confirm-plan")
    public StoryResponse confirmPlan(@PathVariable long storyId, @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(service.confirmPlan(storyId, request.version()));
    }
}
