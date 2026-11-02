CREATE TABLE stories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    idea TEXT NOT NULL,
    style_guide TEXT NULL,
    proposal MEDIUMTEXT NULL,
    outline_text MEDIUMTEXT NULL,
    body LONGTEXT NULL,
    stage VARCHAR(32) NOT NULL DEFAULT 'IDEA',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    INDEX idx_stories_updated (updated_at DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
