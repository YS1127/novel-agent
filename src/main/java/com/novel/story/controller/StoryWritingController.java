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
    public record RewriteRequest(@NotNull Integer version, @NotNull Integer startOffset,
            @NotNull Integer endOffset, @NotBlank String selectedText, @NotBlank String instruction) {}
    public record CandidateResponse(Long id, String kind, String sourceText, String instruction,
            String proposedText, Integer startOffset, Integer endOffset, Integer baseVersion, String status) {
        static CandidateResponse from(StoryCandidate value) {
            return new CandidateResponse(value.getId(), value.getKind(), value.getSourceText(),
                    value.getInstruction(), value.getProposedText(), value.getStartOffset(),
                    value.getEndOffset(), value.getBaseVersion(), value.getStatus());
        }
    }

    @GetMapping("/candidates")
    public List<CandidateResponse> pending(@PathVariable long storyId) {
        return writing.pending(storyId).stream().map(CandidateResponse::from).toList();
    }

    @PostMapping("/draft-candidate")
    public CandidateResponse draft(@PathVariable long storyId, @Valid @RequestBody VersionRequest request) {
        return CandidateResponse.from(writing.generateDraft(storyId, request.version()));
    }

    @PostMapping("/adopt-draft")
    public StoryResponse adoptDraft(@PathVariable long storyId, @RequestParam long candidateId,
            @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(writing.adopt(storyId, candidateId, request.version(), "DRAFT"));
    }

    @PostMapping("/rewrites")
    public CandidateResponse rewrite(@PathVariable long storyId, @Valid @RequestBody RewriteRequest request) {
        return CandidateResponse.from(writing.generateRewrite(storyId, request.version(),
                request.startOffset(), request.endOffset(), request.selectedText(), request.instruction()));
    }

    @PostMapping("/rewrites/{candidateId}/adopt")
    public StoryResponse adoptRewrite(@PathVariable long storyId, @PathVariable long candidateId,
            @Valid @RequestBody VersionRequest request) {
        return StoryResponse.from(writing.adopt(storyId, candidateId, request.version(), "REWRITE"));
    }

    @DeleteMapping("/candidates/{candidateId}")
    public void dismiss(@PathVariable long storyId, @PathVariable long candidateId) {
        writing.dismiss(storyId, candidateId);
    }

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
