"""PG 连接池。

用 psycopg3 的 AsyncConnectionPool：FastAPI 是异步的，
同步驱动会把事件循环堵住（一个慢查询卡死所有请求）。

池子本身在 lifespan 里创建和关闭，全局只留一个。
"""

import logging

from psycopg_pool import AsyncConnectionPool
from pgvector.psycopg import register_vector_async

from app.core.config import PG_DSN, PG_POOL_MIN, PG_POOL_MAX

log = logging.getLogger("market.db")

# 全局连接池（lifespan 里赋值，路由里直接用）
pool: AsyncConnectionPool | None = None


async def _configure(conn):
    """每个新连接做一次：注册 vector 类型。

    不注册的话，Python 的 list[float] 传不进 VECTOR 字段，
    psycopg 会把它当数组，报 "column embedding is of type vector but expression is of type numeric[]"。
    """
    await register_vector_async(conn)


async def open_pool():
    global pool
    if pool is not None:
        return pool

    pool = AsyncConnectionPool(
        conninfo=PG_DSN,
        min_size=PG_POOL_MIN,
        max_size=PG_POOL_MAX,
        configure=_configure,
        open=False,          # 显式 open，避免构造期间就开始连
    )
    await pool.open()
    # 探活：连不上就启动失败，而不是等第一个请求才炸
    async with pool.connection() as conn:
        await conn.execute("SELECT 1")
    log.info("PG 连接池就绪：min=%s max=%s", PG_POOL_MIN, PG_POOL_MAX)
    return pool


async def close_pool():
    global pool
    if pool is not None:
        await pool.close()
        pool = None
        log.info("PG 连接池已关闭")