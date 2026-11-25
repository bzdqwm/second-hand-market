"""向量索引的读写。

核心是 upsert（INSERT ... ON CONFLICT DO UPDATE）：
计划书要求「重跑不重复插入」，靠 item_id / rule_id 的主键约束 + ON CONFLICT 实现。
不用「先删后插」是因为那会出现「删完插失败 → 数据没了」的空窗。
"""

import logging

from app.core import db
from app.core.embedding import embed_text, encode
from app.schemas.index import ItemDoc, RuleDoc

log = logging.getLogger("market.index")

# 单次请求最多处理多少条，防止一次全量把内存吃光
MAX_BATCH = 500


def _chunks(seq: list, size: int):
    for i in range(0, len(seq), size):
        yield seq[i:i + size]


async def index_items(items: list[ItemDoc]) -> int:
    """商品入库（幂等）。返回真正写入的条数。"""
    if not items:
        return 0

    # 同一批里 itemId 重复的话，后一条会覆盖前一条，去重避免白算
    unique: dict[int, ItemDoc] = {it.itemId: it for it in items}
    docs = list(unique.values())

    written = 0
    async with db.pool.connection() as conn:
        for chunk in _chunks(docs, MAX_BATCH):
            texts = [embed_text(d.title, d.description) for d in chunk]
            vectors = await encode(texts)

            rows = [
                (d.itemId, text, vector)
                for d, text, vector in zip(chunk, texts, vectors)
                if text.strip()          # 标题描述全空的不入库
            ]
            if not rows:
                continue

            # executemany 挂在游标上，不在连接上（psycopg3 的 AsyncConnection 没有这个方法）。
            # 批量走一次往返，比循环 execute 快一个数量级
            async with conn.cursor() as cur:
                await cur.executemany(
                    """
                    INSERT INTO item_vec (item_id, content, embedding, update_time)
                    VALUES (%s, %s, %s, now())
                    ON CONFLICT (item_id) DO UPDATE
                       SET content     = EXCLUDED.content,
                           embedding   = EXCLUDED.embedding,
                           update_time = now()
                    """,
                    rows,
                )
            written += len(rows)

    log.info("商品向量写入完成：收到 %s 条，实际写入 %s 条", len(items), written)
    return written


async def index_rules(rules: list[RuleDoc]) -> int:
    """审核规则入库（幂等）。Step 13 用，现在先把通路搭好。"""
    if not rules:
        return 0

    unique: dict[str, RuleDoc] = {r.ruleId: r for r in rules}
    docs = list(unique.values())

    written = 0
    async with db.pool.connection() as conn:
        for chunk in _chunks(docs, MAX_BATCH):
            texts = [f"{d.title}\n{d.content}" for d in chunk]
            vectors = await encode(texts)

            rows = [
                (d.ruleId, d.title, d.content, vector, d.ruleVer)
                for d, vector in zip(chunk, vectors)
                if d.content.strip()
            ]
            if not rows:
                continue

            async with conn.cursor() as cur:
                await cur.executemany(
                    """
                    INSERT INTO rule_vec (rule_id, title, content, embedding, rule_ver, update_time)
                    VALUES (%s, %s, %s, %s, %s, now())
                    ON CONFLICT (rule_id) DO UPDATE
                       SET title       = EXCLUDED.title,
                           content     = EXCLUDED.content,
                           embedding   = EXCLUDED.embedding,
                           rule_ver    = EXCLUDED.rule_ver,
                           update_time = now()
                    """,
                    rows,
                )
            written += len(rows)

    log.info("规则向量写入完成：收到 %s 条，实际写入 %s 条", len(rules), written)
    return written


async def counts() -> tuple[int, int]:
    """两张表当前条数，给返回体和验收用（跑两次条数不变）。"""
    async with db.pool.connection() as conn:
        cur = await conn.execute("SELECT (SELECT count(*) FROM item_vec), (SELECT count(*) FROM rule_vec)")
        item_count, rule_count = await cur.fetchone()
    return item_count, rule_count