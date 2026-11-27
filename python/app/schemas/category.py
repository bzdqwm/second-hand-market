"""Pydantic 模型：类目识别相关。

CategoryDoc    → categories.json 里的一条（灌库用）
CategoryRequest → /ai/category 的入参
CategoryHit/CategoryResult → 出参
"""

from pydantic import BaseModel, Field


class CategoryDoc(BaseModel):
    """categories.json 里的一条类目关键词配置。"""

    categoryId: int = Field(..., gt=0, description="MySQL category.id，必须是叶子类目")
    name: str = Field(..., min_length=1, max_length=50, description="类目名，如 平板")
    parentName: str = Field(..., min_length=1, max_length=50, description="父类目名，如 数码")
    keywords: list[str] = Field(..., min_length=1, description="用户可能怎么称呼它")

    @property
    def path(self) -> str:
        """给前端显示的完整路径，如 数码/平板。"""
        return f"{self.parentName}/{self.name}"

    def embedding_text(self) -> str:
        """真正送进模型算向量的那段文字。

        格式：父类目+子类目 作为一句开头，多给模型一点上下文；
              再把关键词用空格连起来。
        为什么用句号隔开：bge-m3 对句子边界敏感，
        把「类目名」和「关键词列表」分成两句，比糊成一句效果好。
        """
        return f"{self.parentName} {self.name}。" + " ".join(self.keywords)


class CategoryRequest(BaseModel):
    """类目识别请求：给一个标题，猜它属于哪个类目。"""

    title: str = Field(..., min_length=1, max_length=100, description="商品标题")
    description: str | None = Field(None, max_length=2000, description="商品描述，可选")
    topK: int = Field(3, ge=1, le=10, description="返回几个候选")


class CategoryHit(BaseModel):
    """一个候选类目。"""

    categoryId: int
    name: str
    path: str
    score: float = Field(..., description="相似度，0~1，越大越像")


class CategoryResult(BaseModel):
    """识别结果。

    hit        ：最像的那一个；置信度不够时为 None
    candidates ：Top N 候选，前端可以做「猜错了让用户改」的下拉
    matched    ：是否达到置信度阈值（前端据此决定要不要自动填）
    """

    title: str
    hit: CategoryHit | None
    candidates: list[CategoryHit]
    matched: bool
    threshold: float