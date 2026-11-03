CREATE TABLE ai_settings (
    id BIGINT NOT NULL PRIMARY KEY,
    base_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    encrypted_api_key TEXT NULL,
    updated_at DATETIME(3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE story_questions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    story_id BIGINT NOT NULL,
    ordinal INT NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer TEXT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_questions_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    UNIQUE KEY uq_story_question_ordinal (story_id, ordinal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
