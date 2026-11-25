"""Market AI Service

lifespan 里做两件「只做一次」的事：
  1. 开 PG 连接池（向量库）
  2. 加载 embedding 模型（bge-m3，2GB，绝不能放在请求里）

关服务时按相反顺序释放。
"""

import asyncio
import logging
import os
from contextlib import asynccontextmanager
from datetime import datetime

from fastapi import FastAPI

from app.core import db, embedding
from app.core.config import WARMUP_ON_START
from app.routers import index as index_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
log = logging.getLogger("market.main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ① 连接池：连不上直接启动失败，别等第一个请求才发现 PG 没起
    await db.open_pool()

    # ② 模型加载是同步且很慢的（读 2GB 权重），丢线程里避免卡住启动日志
    import asyncio

    await asyncio.to_thread(embedding.load_model)
    if WARMUP_ON_START:
        await embedding.warmup()

    log.info("AI 服务就绪：PG 连接池 + embedding 模型")
    yield

    # ③ 释放（顺序反过来）
    await db.close_pool()
    log.info("AI 服务已关闭")


app = FastAPI(title="Market AI Service", lifespan=lifespan)

app.include_router(index_router.router)


@app.get("/health")
def health():
    return {"status": "ok", "service": "python-ai"}


@app.get("/ai/ping")
def ai_ping():
    return {
        "status": "ok",
        "time": datetime.now().astimezone().isoformat(),
        "msg": "pong from FastAPI",
    }


if __name__ == "__main__":
    import uvicorn

    # loop 必须指定自定义工厂：Windows 下 uvicorn 默认给 ProactorEventLoop，
    # psycopg 异步驱动不支持，连接池会直接拿不到连接（见 app/common/loop_factory.py）
    uvicorn.run(
        "main:app",
        host="127.0.0.1",
        port=8000,
        reload=False,
        loop="app.core.event_loop:selector_loop_factory",
    )