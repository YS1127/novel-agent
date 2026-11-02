package com.novel.story.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.ai.AiChatClient;
import com.novel.ai.AiServiceException;
import com.novel.story.RewriteText;
import com.novel.story.StoryConflictException;
import com.novel.story.StoryNotFoundException;
import com.novel.story.entity.Story;
import com.novel.story.entity.StoryCandidate;
import com.novel.story.mapper.StoryCandidateMapper;
import com.novel.story.mapper.StoryMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StoryWritingService {
    private final StoryService stories;
    private final StoryMapper storyMapper;
    private final StoryCandidateMapper candidates;
    private final AiChatClient ai;
    private final TransactionTemplate transactions;

    public StoryWritingService(StoryService stories, StoryMapper storyMapper, StoryCandidateMapper candidates,
            AiChatClient ai, TransactionTemplate transactions) {
        this.stories = stories;
        this.storyMapper = storyMapper;
        this.candidates = candidates;
        this.ai = ai;
        this.transactions = transactions;
    }

    public List<StoryCandidate> pending(long storyId) {
        stories.get(storyId);
        return candidates.selectPage(new Page<>(1, 20), new LambdaQueryWrapper<StoryCandidate>()
                .eq(StoryCandidate::getStoryId, storyId).eq(StoryCandidate::getStatus, "PENDING")
                .orderByDesc(StoryCandidate::getCreatedAt, StoryCandidate::getId)).getRecords();
    }

    public StoryCandidate generateDraft(long storyId, int version) {
        Story story = stories.get(storyId);
        ensureVersion(story, version);
        ensureConfirmed(story);
        String text = ai.complete("你是中文小说写作助手。根据已确认的故事方案和大纲，写一篇连贯的中文中短篇小说初稿。只返回正文，不要标题或解释。",
                "标题：" + story.getTitle() + "\n点子：" + story.getIdea() + "\n文风：" + story.getStyleGuide()
                + "\n故事方案：" + story.getProposal() + "\n大纲：" + story.getOutline());
        return insertCandidate(storyId, "DRAFT", story.getBody(), "", text, null, null, version);
    }

    public StoryCandidate generateRewrite(long storyId, int version, int start, int end,
            String selectedText, String instruction) {
        Story story = stories.get(storyId);
        ensureVersion(story, version);
        if (instruction == null || instruction.isBlank()) throw new IllegalArgumentException("请填写改写要求");
        RewriteText.apply(story.getBody(), start, end, selectedText, selectedText);
        if (selectedText.isBlank()) throw new IllegalArgumentException("请选择需要改写的正文片段");
        String text = ai.complete("你是中文小说编辑。只改写用户选中的片段，保持原意与上下文连贯。只返回改写后的片段，不要解释。",
                "作品：" + story.getTitle() + "\n文风：" + story.getStyleGuide()
                + "\n原片段：" + selectedText + "\n改写要求：" + instruction);
        return insertCandidate(storyId, "REWRITE", selectedText, instruction.trim(), text, start, end, version);
    }

    public Story adopt(long storyId, long candidateId, int version, String expectedKind) {
        return transactions.execute(status -> {
            StoryCandidate candidate = getCandidate(storyId, candidateId);
            if (!candidate.getKind().equals(expectedKind) || !"PENDING".equals(candidate.getStatus())) {
                throw new StoryConflictException("候选内容已失效");
            }
            Story story = stories.get(storyId);
            ensureVersion(story, version);
            if (candidate.getBaseVersion() != version) throw new StoryConflictException("正文已变化，请重新生成候选内容");
            if ("DRAFT".equals(expectedKind)) ensureConfirmed(story);
            String nextBody = "DRAFT".equals(expectedKind) ? candidate.getProposedText()
                    : RewriteText.apply(story.getBody(), candidate.getStartOffset(), candidate.getEndOffset(),
                            candidate.getSourceText(), candidate.getProposedText());
            int changed = storyMapper.update(null, new LambdaUpdateWrapper<Story>()
                    .eq(Story::getId, storyId).eq(Story::getVersion, version)
                    .set(Story::getBody, nextBody).set(Story::getStage, "DRAFT")
                    .set(Story::getVersion, version + 1).set(Story::getUpdatedAt, LocalDateTime.now()));
            if (changed != 1) throw new StoryConflictException("作品已被修改，请刷新后重试");
            candidate.setStatus("ADOPTED");
            candidates.updateById(candidate);
            return stories.get(storyId);
        });
    }

    public void dismiss(long storyId, long candidateId) {
        transactions.executeWithoutResult(status -> {
            StoryCandidate candidate = getCandidate(storyId, candidateId);
            if (!"PENDING".equals(candidate.getStatus())) return;
            candidate.setStatus("DISMISSED");
            candidates.updateById(candidate);
        });
    }

    private StoryCandidate insertCandidate(long storyId, String kind, String source, String instruction,
            String proposed, Integer start, Integer end, int version) {
        if (proposed == null || proposed.isBlank()) throw new AiServiceException("AI 返回空内容，请重试");
        StoryCandidate candidate = new StoryCandidate();
        candidate.setStoryId(storyId);
        candidate.setKind(kind);
        candidate.setSourceText(source == null ? "" : source);
        candidate.setInstruction(instruction);
        candidate.setProposedText(proposed.trim());
        candidate.setStartOffset(start);
        candidate.setEndOffset(end);
        candidate.setBaseVersion(version);
        candidate.setStatus("PENDING");
        candidate.setCreatedAt(LocalDateTime.now());
        candidates.insert(candidate);
        return candidate;
    }

    private StoryCandidate getCandidate(long storyId, long candidateId) {
        StoryCandidate candidate = candidates.selectById(candidateId);
        if (candidate == null || candidate.getStoryId() != storyId) throw new StoryNotFoundException(candidateId);
        return candidate;
    }

    private void ensureConfirmed(Story story) {
        if (!("CONFIRMED".equals(story.getStage()) || "DRAFT".equals(story.getStage()))) {
            throw new StoryConflictException("请先确认故事方案和大纲");
        }
    }

    private void ensureVersion(Story story, int version) {
        if (story.getVersion() != version) throw new StoryConflictException("作品已被修改，请刷新后重试");
    }
}
