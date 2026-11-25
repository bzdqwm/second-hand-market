# 启动 FastAPI AI 服务（含向量索引）
#
# 为什么用 main.py 而不是 `uvicorn main:app`：
#   Windows 下 uvicorn 0.54 的默认 loop 是 ProactorEventLoop，
#   psycopg3 的异步模式不支持它，连接池会拿不到连接、30 秒后 PoolTimeout。
#   main.py 里用 loop="app.common.loop_factory:selector_loop_factory" 换成了
#   SelectorEventLoop，命令行直接敲 uvicorn 会绕过这个设置。
#
# 用法：
#   cd python
#   .\start.ps1

$ErrorActionPreference = "Stop"

# 模型镜像（huggingface.co 在本机不可达，只有镜像站通）
#   只有本地 .models 缺文件时才会联网，正常启动用不到
$env:HF_ENDPOINT = "https://hf-mirror.com"
$env:HF_HUB_DISABLE_XET = "1"      # Xet 传输在本机会报 os error 5

# PG 连接（对应 docker 里的 market-postgres）
$env:MARKET_PG_DSN = "host=127.0.0.1 port=5432 dbname=market user=market password=market123"

# Windows 控制台默认 GBK，Python 输出中文会乱码
$env:PYTHONIOENCODING = "utf-8"

$py = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"
if (-not (Test-Path $py)) {
    Write-Error "找不到 $py，先建虚拟环境：python -m venv .venv"
}

$model = Join-Path $PSScriptRoot ".models\bge-m3\pytorch_model.bin"
if (-not (Test-Path $model)) {
    Write-Host "本地没有模型，先跑 .\fetch_model.ps1 下载（约 2.1GB）" -ForegroundColor Yellow
}

Write-Host "启动 AI 服务：http://127.0.0.1:8000" -ForegroundColor Cyan
Set-Location $PSScriptRoot
& $py main.py