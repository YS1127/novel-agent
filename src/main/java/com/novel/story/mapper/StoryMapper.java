package com.novel.story.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.story.entity.Story;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoryMapper extends BaseMapper<Story> {
}
