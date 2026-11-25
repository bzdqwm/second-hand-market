"""/ai/index 路由。"""

import logging

from fastapi import APIRouter, HTTPException

from app.services import indexing
from app.core.embedding import is_loaded
from app.schemas.index import IndexRequest, IndexResult

log = logging.getLogger("market.router.index")

router = APIRouter(prefix="/ai", tags=["ai-index"])


@router.post("/index", response_model=IndexResult)
async def build_index(req: IndexRequest):
    """建/重建向量索引。

    - mode=one   单条（Java 商品上架后触发）
    - mode=batch 批量
    - mode=all   全量

    幂等：同样的数据跑多少次，item_vec 里条数都不变（靠 ON CONFLICT）。
    """
    if not is_loaded():
        # 模型还没加载完就来了请求，明确告诉调用方稍后重试，别让 Java 干等
        raise HTTPException(status_code=503, detail="embedding 模型尚未就绪")

    if req.mode == "one" and len(req.items) != 1:
        raise HTTPException(status_code=400, detail="mode=one 时必须且只能传 1 条 items")

    if not req.items and not req.rules:
        raise HTTPException(status_code=400, detail="items 和 rules 不能同时为空")

    indexed = 0
    indexed += await indexing.index_items(req.items)
    indexed += await indexing.index_rules(req.rules)

    item_count, rule_count = await indexing.counts()

    return IndexResult(
        indexed=indexed,
        total=len(req.items) + len(req.rules),
        itemVecCount=item_count,
        ruleVecCount=rule_count,
    )


@router.get("/index/stats")
async def index_stats():
    """只查条数，验收用：连续跑两次全量，这个数字应该不变。"""
    item_count, rule_count = await indexing.counts()
    return {
        "itemVecCount": item_count,
        "ruleVecCount": rule_count,
        "modelLoaded": is_loaded(),
    }