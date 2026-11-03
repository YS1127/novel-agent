CREATE TABLE story_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    story_id BIGINT NOT NULL,
    kind VARCHAR(16) NOT NULL,
    source_text LONGTEXT NULL,
    instruction_text TEXT NULL,
    proposed_text LONGTEXT NOT NULL,
    start_offset INT NULL,
    end_offset INT NULL,
    base_version INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_candidates_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    INDEX idx_candidates_story_status (story_id, status, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
