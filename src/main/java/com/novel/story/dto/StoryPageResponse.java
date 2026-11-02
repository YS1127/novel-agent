package com.novel.story.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.story.entity.Story;
import java.util.List;

public record StoryPageResponse(List<StoryResponse> items, long total, long page, long size) {
    public static StoryPageResponse from(Page<Story> data) {
        return new StoryPageResponse(data.getRecords().stream().map(StoryResponse::from).toList(),
                data.getTotal(), data.getCurrent(), data.getSize());
    }
}
