CREATE TABLE IF NOT EXISTS category_vec (
    category_id BIGINT       PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    path        VARCHAR(100) NOT NULL,   -- "数码/平板"，给前端显示
    keywords    TEXT         NOT NULL,   -- 参与算向量的原始文本，出问题好排查
    embedding   VECTOR(1024) NOT NULL,
    update_time TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_category_vec_embedding
    ON category_vec USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);