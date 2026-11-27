"""category_vec（类目向量表）的读写。"""

from app.core import db


async def upsert_many(rows: list[tuple]) -> int:
    """批量写入/更新类目向量（幂等），返回写入条数。

    rows 的每一项：(category_id, name, path, keywords, embedding)
    """
    if not rows:
        return 0

    async with db.pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.executemany(
                """
                INSERT INTO category_vec (category_id, name, path, keywords, embedding, update_time)
                VALUES (%s, %s, %s, %s, %s, now())
                ON CONFLICT (category_id) DO UPDATE
                   SET name        = EXCLUDED.name,
                       path        = EXCLUDED.path,
                       keywords    = EXCLUDED.keywords,
                       embedding   = EXCLUDED.embedding,
                       update_time = now()
                """,
                rows,
            )
    return len(rows)


async def select_top_k(vector: list[float], k: int) -> list[tuple]:
    """按余弦距离取最像的 k 个类目。

    返回每一项：(category_id, name, path, score)

    两个 %s::vector 传的是同一个向量（用两次，位置互补）：
        第一个是算相似度用的，第二个是排序用的。
    必须在 SQL 里排序（ORDER BY），才能让 PG 用上 HNSW 索引；
    如果把结果捞回 Python 再排，就是全表扫描 + 内存排序。
    """
    async with db.pool.connection() as conn:
        cur = await conn.execute(
            """
            SELECT category_id,
                   name,
                   path,
                   1 - (embedding <=> %s::vector) AS score
            FROM category_vec
            ORDER BY embedding <=> %s::vector
            LIMIT %s
            """,
            (vector, vector, k),
        )
        return await cur.fetchall()


async def count() -> int:
    """表里当前有多少条。"""
    async with db.pool.connection() as conn:
        cur = await conn.execute("SELECT count(*) FROM category_vec")
        return (await cur.fetchone())[0]