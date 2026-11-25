"""Pydantic 模型：/ai/index 的入参与出参。

为什么不用裸 dict：
- 字段类型错了自动 422，不用自己写 if
- OpenAPI 文档自动生成，Apifox 能直接看到结构
"""

from pydantic import BaseModel, Field


class ItemDoc(BaseModel):
    """一条待索引的商品。

    注意：Python 不连 MySQL，商品数据一律由 Java 推过来。
    Python 只负责「文本 → 向量 → 存 PG」，职责单一。
    """

    itemId: int = Field(..., gt=0, description="MySQL item.id")
    title: str = Field(..., min_length=1, max_length=100, description="商品标题")
    description: str | None = Field(None, max_length=5000, description="商品描述，可空")


class RuleDoc(BaseModel):
    """一条审核规则（Step 13 灌入，这里先支持着）。"""

    ruleId: str = Field(..., min_length=1, max_length=64, description="规则编号，如 R012")
    title: str = Field(..., min_length=1, max_length=200, description="规则名")
    content: str = Field(..., min_length=1, max_length=20000, description="规则正文")
    ruleVer: str = Field("v1", max_length=20, description="规则库版本")


class IndexRequest(BaseModel):
    """建索引请求。

    mode 只起「说明意图 + 校验」的作用：
      one   → items 必须 1 条（Java 上架时触发）
      batch → items 任意条（分批重建）
      all   → items 是全部商品（全量重建）
    三种模式走的是同一段 upsert 逻辑，差别只在校验，所以这里不写三份代码。
    """

    mode: str = Field("batch", pattern="^(one|batch|all)$")
    items: list[ItemDoc] = Field(default_factory=list)
    rules: list[RuleDoc] = Field(default_factory=list)


class IndexResult(BaseModel):
    """建索引结果。

    indexed：真正写入/更新的条数
    total  ：收到的条数
    两者正常情况下相等；不等说明有数据被跳过（比如空文本）
    """

    indexed: int
    total: int
    itemVecCount: int
    ruleVecCount: int