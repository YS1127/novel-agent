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
    public record AnswersRequest(@NotNull Integer version, @NotNull List<Answer> answers) {}
    public record QuestionResponse(Long id, int ordinal, String question, String answer) {
        static QuestionResponse from(StoryQuestion item) {
            return new QuestionResponse(item.getId(), item.getOrdinal(), item.getQuestion(), item.getAnswer());
        }
    }

    @GetMapping("/questions")
    public List<QuestionResponse> questions(@PathVariable long storyId) {
        return service.listQuestions(storyId).stream().map(QuestionResponse::from).toList();
    }

    @PostMapping("/questions")
    public List<QuestionResponse> generateQuestions(@PathVariable long storyId,
            @Valid @RequestBody VersionRequest request) {
        return service.generateQuestions(storyId, request.version()).stream().map(QuestionResponse::from).toList();
    }

    @PutMapping("/answers")
    public List<QuestionResponse> answers(@PathVariable long storyId,
            @Valid @RequestBody AnswersRequest request) {
        return service.saveAnswers(storyId, request.version(), request.answers()).stream()
                .map(QuestionResponse::from).toList();
    }

    @PostMapping("/plan")
    public StoryResponse generatePlan(@PathVariable long storyId, @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(service.generatePlan(storyId, request.version()));
    }

    @PostMapping("/confirm-plan")
    public StoryResponse confirmPlan(@PathVariable long storyId, @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(service.confirmPlan(storyId, request.version()));
    }
}
