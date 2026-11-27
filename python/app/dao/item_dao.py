"""item_vec（商品向量表）的读写。"""

from app.core import db


async def upsert_many(rows: list[tuple]) -> int:
    """批量写入/更新商品向量（幂等），返回写入条数。

    rows 的每一项：(item_id, content, embedding)

    幂等靠 ON CONFLICT (item_id) DO UPDATE：
    同一个 item_id 重跑只覆盖，不会多出一行。
    """
    if not rows:
        return 0

    async with db.pool.connection() as conn:
        # executemany 挂在游标上，不在连接上（psycopg3 的 AsyncConnection 没有这个方法）
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
    return len(rows)


async def count() -> int:
    """表里当前有多少条。"""
    async with db.pool.connection() as conn:
        cur = await conn.execute("SELECT count(*) FROM item_vec")
        return (await cur.fetchone())[0]