"""类目识别：把类目关键词灌成向量。

分层：
    schemas/category.py  定义数据形状
    dao/category_dao.py  只管 SQL
    services/category.py 就是这一层：读文件 → 算向量 → 交给 dao（不含任何 SQL）
"""

import json
import logging
import os

from app.core.embedding import embed_text, encode          # 原来是只 import encode
from app.schemas.category import CategoryDoc, CategoryHit  # 原来只有 CategoryDoc
from app.core.config import DATA_DIR
from app.core.embedding import encode
from app.dao import category_dao
from app.schemas.category import CategoryDoc

log = logging.getLogger("market.category")
# 置信度阈值：Top1 相似度低于这个值就不自动填，让用户手选
#   0.60 是经验起点，实测后可能要调（低风险偏高，高风险偏低）
SIM_THRESHOLD = 0.45


def load_docs() -> list[CategoryDoc]:
    """读 categories.json，并用 Pydantic 校验。

    为什么用 CategoryDoc(**item) 而不是直接返回 dict：
    文件是手写的，写错字段名（比如 categoryID）时立刻报错，
    不会等到灌库时才发现全是 None。
    """
    path = os.path.join(DATA_DIR, "categories.json")
    with open(path, encoding="utf-8") as f:
        raw = json.load(f)
    return [CategoryDoc(**item) for item in raw]


async def sync_categories() -> int:
    """把 categories.json 的类目全部算成向量写入 category_vec（幂等）。

    这一步是「离线」的：改了 json 或换机器才需要跑，日常请求不经过它。
    """
    docs = load_docs()
    if not docs:
        log.warning("categories.json 是空的，跳过灌库")
        return 0

    texts = [d.embedding_text() for d in docs]
    vectors = await encode(texts)

    rows = [
        (d.categoryId, d.name, d.path, text, vector)
        for d, text, vector in zip(docs, texts, vectors)
    ]

    written = await category_dao.upsert_many(rows)
    log.info("类目向量写入完成：%s 条", written)
    return written


async def recognize(
    title: str,
    description: str | None = None,
    top_k: int = 3,
) -> list[CategoryHit]:
    """给标题找最像的类目，返回 Top N（按相似度从高到低）。

    为什么只用 title（描述可选）：
    发布页的触发时机是「用户打完标题」，这时还没有描述。
    描述传进来就一起算，能提高准确率，但接口不依赖它。
    """
    text = embed_text(title, description)
    if not text.strip():
        return []

    # encode 收的是列表，这里只有一句话，取第 0 个
    vector = (await encode([text]))[0]

    rows = await category_dao.select_top_k(vector, top_k)

    return [
        CategoryHit(
            categoryId=row[0],
            name=row[1],
            path=row[2],
            score=round(float(row[3]), 4),
        )
        for row in rows
    ]