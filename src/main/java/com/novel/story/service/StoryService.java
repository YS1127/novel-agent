package com.novel.story.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.story.StoryConflictException;
import com.novel.story.StoryNotFoundException;
import com.novel.story.entity.Story;
import com.novel.story.mapper.StoryMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryService {
    private final StoryMapper mapper;

    public StoryService(StoryMapper mapper) {
        this.mapper = mapper;
    }

    /** 创建作品；标题和点子不可为空，初始阶段为 IDEA。 */
    @Transactional
    public Story create(String title, String idea, String styleGuide) {
        requireText(title, "标题");
        requireText(idea, "点子");
        Story story = new Story();
        story.setTitle(title.trim());
        story.setIdea(idea.trim());
        story.setStyleGuide(trimNullable(styleGuide));
        story.setStage("IDEA");
        story.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        story.setCreatedAt(now);
        story.setUpdatedAt(now);
        mapper.insert(story);
        return story;
    }

    @Transactional(readOnly = true)
    public Story get(long id) {
        Story story = mapper.selectById(id);
        if (story == null) throw new StoryNotFoundException(id);
        return story;
    }

    /** 按更新时间与 ID 降序分页，页码从 1 开始，单页最多 100 条。 */
    @Transactional(readOnly = true)
    public Page<Story> list(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new IllegalArgumentException("分页参数无效");
        return mapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Story>()
                .orderByDesc(Story::getUpdatedAt, Story::getId));
    }

    /**
     * 保存创作者的编辑。version 必须等于当前版本，否则抛出 StoryConflictException。
     * 修改已确认的方案或大纲会退回 PLAN，要求再次确认。
     */
    @Transactional
    public Story update(long id, String title, String idea, String styleGuide,
                        String proposal, String outline, String body, int version) {
        requireText(title, "标题");
        requireText(idea, "点子");
        Story current = get(id);
        if (current.getVersion() != version) throw new StoryConflictException("作品已被修改，请刷新后重试");
        boolean changedPlan = !java.util.Objects.equals(current.getProposal(), trimNullable(proposal))
                || !java.util.Objects.equals(current.getOutline(), trimNullable(outline));
        String nextStage = current.getStage();
        if (changedPlan && ("CONFIRMED".equals(current.getStage()) || "DRAFT".equals(current.getStage()))) nextStage = "PLAN";
        else if (changedPlan && !trimNullable(proposal).isBlank() && !trimNullable(outline).isBlank()) nextStage = "PLAN";
        else if (body != null && !body.isBlank() && "CONFIRMED".equals(current.getStage())) nextStage = "DRAFT";
        int updated = mapper.update(null, new LambdaUpdateWrapper<Story>()
                .eq(Story::getId, id).eq(Story::getVersion, version)
                .set(Story::getTitle, title.trim())
                .set(Story::getIdea, idea.trim())
                .set(Story::getStyleGuide, trimNullable(styleGuide))
                .set(Story::getProposal, trimNullable(proposal))
                .set(Story::getOutline, trimNullable(outline))
                .set(Story::getBody, body == null ? "" : body)
                .set(Story::getStage, nextStage)
                .set(Story::getVersion, version + 1)
                .set(Story::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) throw new StoryConflictException("作品已被修改，请刷新后重试");
        return get(id);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + "不能为空");
    }

    private static String trimNullable(String value) {
        return value == null ? "" : value.trim();
    }
}
