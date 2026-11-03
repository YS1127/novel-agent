package com.novel.story.controller;

import com.novel.story.StoryExport;
import com.novel.story.dto.StoryResponse;
import com.novel.story.entity.Story;
import com.novel.story.entity.StoryCandidate;
import com.novel.story.service.StoryService;
import com.novel.story.service.StoryWritingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories/{storyId}")
public class StoryWritingController {
    private final StoryService stories;
    private final StoryWritingService writing;

    public StoryWritingController(StoryService stories, StoryWritingService writing) {
        this.stories = stories;
        this.writing = writing;
    }

    public record VersionRequest(@NotNull Integer version) {}
    public record RewriteRequest(@NotNull Integer version, @NotNull @Min(0) Integer startOffset,
            @NotNull @Min(0) Integer endOffset, @NotBlank @Size(max = 10000) String selectedText,
            @NotBlank @Size(max = 2000) String instruction) {}
    public record CandidateResponse(Long id, String kind, String sourceText, String instruction,
            String proposedText, Integer startOffset, Integer endOffset, Integer baseVersion, String status) {
        static CandidateResponse from(StoryCandidate value) {
            return new CandidateResponse(value.getId(), value.getKind(), value.getSourceText(),
                    value.getInstruction(), value.getProposedText(), value.getStartOffset(),
                    value.getEndOffset(), value.getBaseVersion(), value.getStatus());
        }
    }

    /**
     * 读取最近 20 个待审阅候选。
     * @param storyId 作品 ID
     * @return 属于该作品的候选；作品不存在返回 404
     */
    @GetMapping("/candidates")
    public List<CandidateResponse> pending(@PathVariable long storyId) {
        return writing.pending(storyId).stream().map(CandidateResponse::from).toList();
    }

    /**
     * 依据已确认大纲生成初稿候选，不修改正文。
     * @param storyId 作品 ID
     * @param request 当前作品版本
     * @return 待审阅初稿；未确认大纲或版本冲突返回 409
     */
    @PostMapping("/draft-candidate")
    public CandidateResponse draft(@PathVariable long storyId, @Valid @RequestBody VersionRequest request) {
        return CandidateResponse.from(writing.generateDraft(storyId, request.version()));
    }

    /**
     * 明确采纳指定初稿候选。
     * @param storyId 作品 ID
     * @param candidateId 属于当前作品的初稿候选 ID
     * @param request 候选生成时的作品版本
     * @return 写入正文后的作品；候选过期返回 409
     */
    @PostMapping("/adopt-draft")
    public StoryResponse adoptDraft(@PathVariable long storyId, @RequestParam long candidateId,
            @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(writing.adopt(storyId, candidateId, request.version(), "DRAFT"));
    }

    /**
     * 对当前正文的 UTF-16 偏移范围生成片段改写候选。
     * @param storyId 作品 ID
     * @param request 当前版本、选区、原文与改写要求
     * @return 待审阅改写；选区不匹配返回 409
     */
    @PostMapping("/rewrites")
    public CandidateResponse rewrite(@PathVariable long storyId, @Valid @RequestBody RewriteRequest request) {
        return CandidateResponse.from(writing.generateRewrite(storyId, request.version(),
                request.startOffset(), request.endOffset(), request.selectedText(), request.instruction()));
    }

    /**
     * 明确采纳片段改写，正文变化后拒绝旧候选。
     * @param storyId 作品 ID
     * @param candidateId 属于当前作品的改写候选 ID
     * @param request 候选生成时的作品版本
     * @return 替换片段后的作品；冲突返回 409
     */
    @PostMapping("/rewrites/{candidateId}/adopt")
    public StoryResponse adoptRewrite(@PathVariable long storyId, @PathVariable long candidateId,
            @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(writing.adopt(storyId, candidateId, request.version(), "REWRITE"));
    }

    /**
     * 放弃候选，保留当前正文。
     * @param storyId 作品 ID
     * @param candidateId 属于当前作品的候选 ID
     */
    @DeleteMapping("/candidates/{candidateId}")
    public void dismiss(@PathVariable long storyId, @PathVariable long candidateId) {
        writing.dismiss(storyId, candidateId);
    }

    /**
     * 导出当前已保存正文。
     * @param storyId 作品 ID
     * @param format md 或 txt；其他格式返回 400
     * @return 带安全文件名的下载响应
     */
    @GetMapping("/export")
    public ResponseEntity<String> export(@PathVariable long storyId, @RequestParam String format) {
        Story story = stories.get(storyId);
        String content = StoryExport.content(story.getTitle(), story.getBody(), format);
        String filename = StoryExport.filename(story.getTitle(), format);
        MediaType mediaType = MediaType.parseMediaType("md".equals(format)
                ? "text/markdown;charset=UTF-8" : "text/plain;charset=UTF-8");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(mediaType)
                .body(content);
    }
}
