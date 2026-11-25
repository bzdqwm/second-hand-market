"""集中管理环境配置。

全部走环境变量，带默认值 —— 本地开发不配也能跑，
上线时改环境变量就行，不用改代码。
"""

import os

# 项目根目录（这个文件在 app/common/ 下，往上三层就是 python/）
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# PostgreSQL（向量库）连接串
#   host=127.0.0.1 是因为容器把 5432 映射到了宿主机
PG_DSN = os.getenv(
    "MARKET_PG_DSN",
    "host=127.0.0.1 port=5432 dbname=market user=market password=market123",
)

# 连接池上下限
PG_POOL_MIN = int(os.getenv("MARKET_PG_POOL_MIN", "1"))
PG_POOL_MAX = int(os.getenv("MARKET_PG_POOL_MAX", "8"))

# 本地模型目录（fetch_model.ps1 下到这里）
LOCAL_MODEL_DIR = os.path.join(BASE_DIR, ".models", "bge-m3")

# embedding 模型。bge-m3 输出 1024 维，和建表时的 VECTOR(1024) 必须一致
#   优先用本地目录：huggingface.co 在本机不可达，写 ID 会卡在下载上
#   本地没有时才退回模型 ID（这时需要 HF_ENDPOINT 指向镜像站）
EMBED_MODEL = os.getenv(
    "MARKET_EMBED_MODEL",
    LOCAL_MODEL_DIR if os.path.isdir(LOCAL_MODEL_DIR) else "BAAI/bge-m3",
)

# cpu / cuda。没有 N 卡就 cpu，别写 cuda 否则启动直接报错
EMBED_DEVICE = os.getenv("MARKET_EMBED_DEVICE", "cpu")

# 模型缓存目录。放到项目里，换机器拷走就行，不用重新下 2GB
MODEL_CACHE_DIR = os.getenv("MARKET_MODEL_CACHE", "")

# 是否在启动时预热模型（真正 encode 一次），关掉能加快启动
WARMUP_ON_START = os.getenv("MARKET_WARMUP", "1") == "1"