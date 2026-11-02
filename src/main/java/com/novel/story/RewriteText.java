package com.novel.story;

public final class RewriteText {
    private RewriteText() {}

    public static String apply(String body, int start, int end, String expected, String replacement) {
        if (body == null || expected == null || replacement == null || start < 0 || end < start
                || end > body.length() || !body.substring(start, end).equals(expected)) {
            throw new StoryConflictException("正文已变化，请重新选中片段后改写");
        }
        return body.substring(0, start) + replacement + body.substring(end);
    }
}
