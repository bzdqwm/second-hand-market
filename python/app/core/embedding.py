"""embedding 模型封装。

三个设计点：
1. 模型只加载一次（2 GB 的东西不能每个请求加一次）
2. encode 是 CPU 阻塞操作，必须丢到线程里，否则整个 FastAPI 卡住
3. 支持批量：一次 encode 20 条比 encode 20 次快得多（模型前向可以批处理）
"""

import asyncio
import logging

from app.core.config import EMBED_DEVICE, EMBED_MODEL, MODEL_CACHE_DIR

log = logging.getLogger("market.embedding")

_model = None


def load_model():
    """加载模型（同步、耗时，启动时调一次）。"""
    global _model
    if _model is not None:
        return _model

    from sentence_transformers import SentenceTransformer

    kwargs = {"device": EMBED_DEVICE}
    if MODEL_CACHE_DIR:
        kwargs["cache_folder"] = MODEL_CACHE_DIR

    log.info("开始加载 embedding 模型：%s (device=%s)", EMBED_MODEL, EMBED_DEVICE)
    _model = SentenceTransformer(EMBED_MODEL, **kwargs)

    dim = _model.get_sentence_embedding_dimension()
    log.info("模型加载完成，维度=%s", dim)
    if dim != 1024:
        # 维度对不上时早报错，别等到写库才炸
        raise RuntimeError(
            f"模型维度 {dim} 与建表的 VECTOR(1024) 不一致，"
            f"要么换回 1024 维模型，要么重建向量表"
        )
    return _model


def is_loaded() -> bool:
    return _model is not None


async def encode(texts: list[str]) -> list[list[float]]:
    """文本 → 向量。

    normalize_embeddings=True：把向量归一化成长度 1，
    这样余弦距离和点积等价，检索时不用再除。

    convert_to_numpy 默认 True，转成 list 是为了让 pgvector 适配器直接吃。
    """
    if not texts:
        return []

    model = load_model()

    def _run():
        vectors = model.encode(
            texts,
            batch_size=8,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return [v.tolist() for v in vectors]

    # 丢到线程池：encode 是纯 CPU 计算，直接在事件循环里跑会阻塞所有请求
    return await asyncio.to_thread(_run)


async def warmup():
    """启动时跑一次，把 lazy 初始化（比如 torch 首次调用）的开销挪到启动阶段。"""
    await encode(["预热"])


def embed_text(title: str | None, description: str | None) -> str:
    """商品标题 + 描述拼成一段送进模型。

    标题权重天然更高一点：用户搜索时说的多半是标题里的词。
    """
    parts = []
    if title and title.strip():
        parts.append(title.strip())
    if description and description.strip():
        parts.append(description.strip())
    return "\n".join(parts)