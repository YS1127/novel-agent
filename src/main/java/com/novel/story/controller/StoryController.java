package com.novel.story.controller;

import com.novel.story.dto.StoryCreateRequest;
import com.novel.story.dto.StoryPageResponse;
import com.novel.story.dto.StoryResponse;
import com.novel.story.dto.StoryUpdateRequest;
import com.novel.story.service.StoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories")
public class StoryController {
    private final StoryService service;

    public StoryController(StoryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse create(@Valid @RequestBody StoryCreateRequest request) {
        return StoryResponse.from(service.create(request.title(), request.idea(), request.styleGuide()));
    }

    @GetMapping
    public StoryPageResponse list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return StoryPageResponse.from(service.list(page, size));
    }

    @GetMapping("/{id}")
    public StoryResponse get(@PathVariable long id) {
        return StoryResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    public StoryResponse update(@PathVariable long id, @Valid @RequestBody StoryUpdateRequest request) {
        return StoryResponse.from(service.update(id, request.title(), request.idea(), request.styleGuide(),
                request.proposal(), request.outline(), request.body(), request.version()));
    }
}
