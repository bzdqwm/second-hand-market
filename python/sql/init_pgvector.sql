-- ============================================================
-- Step 10：向量表初始化（PostgreSQL + pgvector）
--
-- 执行方式（宿主机）：
--   docker exec -i market-postgres psql -U market -d market < init_pgvector.sql
--
-- 这两张表不在 MySQL，理由是 MySQL 没有向量索引，
-- 余弦距离（<=>）和 HNSW 索引只有 pgvector 提供。
--
-- ⚠️ 维度 1024 一旦灌了数据就不能改。
--    换 embedding 模型（维度变了）必须：建新表 → 重灌 → 切过去。
-- ============================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- ------------------------------------------------------------
-- item_vec：商品向量
--   找货 / 智能估价 / 智能问答 的检索源
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS item_vec (
    item_id     BIGINT        PRIMARY KEY,          -- 对应 MySQL item.id
    content     TEXT          NOT NULL,             -- 拼起来送去 embedding 的原文，便于排查
    embedding   VECTOR(1024)  NOT NULL,             -- bge-m3 输出，1024 维
    update_time TIMESTAMPTZ   NOT NULL DEFAULT now()
);

COMMENT ON TABLE  item_vec           IS '商品语义向量，来源 MySQL item.title + description';
COMMENT ON COLUMN item_vec.content   IS 'embedding 的输入原文，重灌时不用回查 MySQL';
COMMENT ON COLUMN item_vec.embedding IS 'bge-m3 向量，1024 维';

-- HNSW 索引：近似最近邻搜索
--   m                每个节点的最大连接数，越大越准越占内存（16 是常用默认）
--   ef_construction  建索引时的候选集大小，越大越准、建得越慢
--   vector_cosine_ops 距离算子族，对应查询里的 <=>（余弦距离）
-- 没有这个索引，每次检索都是全表扫描 + 逐行算距离
CREATE INDEX IF NOT EXISTS idx_item_vec_embedding
    ON item_vec USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ------------------------------------------------------------
-- rule_vec：审核规则向量
--   Step 13「规则库 + 规则检索」和 Step 14 内容审核直接用
--   现在建好，省得到时候再改表结构
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rule_vec (
    rule_id     VARCHAR(64)   PRIMARY KEY,          -- 规则编号，如 R012
    title       TEXT          NOT NULL,             -- 规则名，如「管制刀具」
    content     TEXT          NOT NULL,             -- 规则正文（送 embedding 的原文）
    embedding   VECTOR(1024)  NOT NULL,
    rule_ver    VARCHAR(20)   NOT NULL DEFAULT 'v1',-- 规则库版本，对应 audit_record.rule_ver
    update_time TIMESTAMPTZ   NOT NULL DEFAULT now()
);

COMMENT ON TABLE  rule_vec        IS '审核规则语义向量，Step 13 灌入';
COMMENT ON COLUMN rule_vec.rule_ver IS '规则库版本，审核记录里回溯用的是哪一版规则';

CREATE INDEX IF NOT EXISTS idx_rule_vec_embedding
    ON rule_vec USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);