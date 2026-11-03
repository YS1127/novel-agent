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

    /**
     * 创建新作品。标题和点子必填。
     * @param request 创作起点与可选文风
     * @return 初始阶段为 IDEA 的作品
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse create(@Valid @RequestBody StoryCreateRequest request) {
        return StoryResponse.from(service.create(request.title(), request.idea(), request.styleGuide()));
    }

    /**
     * 分页列出作品，按更新时间和 ID 倒序。
     * @param page 从 1 开始的页码
     * @param size 每页 1～100 条
     * @return 作品分页结果；非法分页参数返回 400
     */
    @GetMapping
    public StoryPageResponse list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return StoryPageResponse.from(service.list(page, size));
    }

    /**
     * 读取单篇作品。
     * @param id 作品 ID
     * @return 作品内容；不存在返回 404
     */
    @GetMapping("/{id}")
    public StoryResponse get(@PathVariable long id) {
        return StoryResponse.from(service.get(id));
    }

    /**
     * 保存手动编辑，使用 version 防止覆盖较新的内容。
     * @param id 作品 ID
     * @param request 完整的可编辑字段与当前版本
     * @return 新版本作品；版本不一致返回 409
     */
    @PutMapping("/{id}")
    public StoryResponse update(@PathVariable long id, @Valid @RequestBody StoryUpdateRequest request) {
        return StoryResponse.from(service.update(id, request.title(), request.idea(), request.styleGuide(),
                request.proposal(), request.outline(), request.body(), request.version()));
    }
}
