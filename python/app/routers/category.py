"""/ai/category 路由：类目识别 + 重建类目索引。"""

import logging

from fastapi import APIRouter, HTTPException

from app.core.embedding import is_loaded
from app.dao import category_dao
from app.schemas.category import CategoryRequest, CategoryResult
from app.services import category as category_service
from app.services.category import SIM_THRESHOLD

log = logging.getLogger("market.router.category")

router = APIRouter(prefix="/ai", tags=["ai-category"])


@router.post("/category", response_model=CategoryResult)
async def recognize_category(req: CategoryRequest):
    """根据标题猜类目。

    返回 Top3 候选 + 是否达到阈值：
      matched=true  → 前端自动选中 hit.categoryId
      matched=false → 只展示候选，让用户自己选
    """
    if not is_loaded():
        raise HTTPException(status_code=503, detail="embedding 模型尚未就绪")

    hits = await category_service.recognize(req.title, req.description, req.topK)

    return CategoryResult(
        title=req.title,
        hit=hits[0] if hits else None,
        candidates=hits,
        matched=bool(hits) and hits[0].score >= SIM_THRESHOLD,
        threshold=SIM_THRESHOLD,
    )


@router.post("/category/index")
async def rebuild_category_index():
    """重建类目向量（改了 categories.json 后调一次）。

    幂等：跑几次都是 18 行，不会翻倍。
    """
    if not is_loaded():
        raise HTTPException(status_code=503, detail="embedding 模型尚未就绪")

    written = await category_service.sync_categories()
    return {"indexed": written, "total": await category_dao.count()}