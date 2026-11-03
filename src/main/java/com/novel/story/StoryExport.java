package com.novel.story;

public final class StoryExport {
    private StoryExport() {}

    public static String content(String title, String body, String format) {
        validate(format);
        String safeTitle = title == null || title.isBlank() ? "未命名作品" : title.trim();
        String safeBody = body == null ? "" : body;
        return ("md".equals(format) ? "# " : "") + safeTitle + "\n\n" + safeBody + "\n";
    }

    public static String filename(String title, String format) {
        validate(format);
        String name = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "")
                .replace("..", "").trim();
        if (name.isEmpty()) name = "未命名作品";
        if (name.length() > 80) name = name.substring(0, 80);
        return name + "." + format;
    }

    private static void validate(String format) {
        if (!"md".equals(format) && !"txt".equals(format)) {
            throw new IllegalArgumentException("仅支持 md 或 txt 格式");
        }
    }
}
