"""rule_vec（审核规则向量表）的读写。Step 13 正式使用，先把通路搭好。"""

from app.core import db


async def upsert_many(rows: list[tuple]) -> int:
    """批量写入/更新规则向量（幂等），返回写入条数。

    rows 的每一项：(rule_id, title, content, embedding, rule_ver)
    """
    if not rows:
        return 0

    async with db.pool.connection() as conn:
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
    return len(rows)


async def count() -> int:
    """表里当前有多少条。"""
    async with db.pool.connection() as conn:
        cur = await conn.execute("SELECT count(*) FROM rule_vec")
        return (await cur.fetchone())[0]