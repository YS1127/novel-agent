package com.novel.story;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException(long id) {
        super("作品不存在：" + id);
    }
}
