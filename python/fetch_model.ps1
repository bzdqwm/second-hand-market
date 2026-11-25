# 从 ModelScope 拉 bge-m3 模型（多分片并行）
#
# 为什么不用 huggingface_hub 直接下：
#   huggingface.co 在本机不可达；hf-mirror.com 只有 ~120KB/s（2.1GB 要 5 小时）；
#   ModelScope 单连接 ~1MB/s，开 8 个分片并行能到 ~3MB/s，11 分钟左右。
#
# 产物是一个普通目录（不是 HF 缓存结构），直接喂给 SentenceTransformer 的路径参数。
#
# 用法：
#   cd python
#   .\fetch_model.ps1

param(
    [string]$Dest = (Join-Path $PSScriptRoot ".models\bge-m3"),
    [int]$Parallel = 8
)

$ErrorActionPreference = "Stop"
$base = "https://www.modelscope.cn/api/v1/models/BAAI/bge-m3/repo?Revision=master&FilePath="

# sentence-transformers 加载所需的文件（onnx / colbert / 图片都不要）
$files = @(
    "config.json",
    "config_sentence_transformers.json",
    "configuration.json",
    "modules.json",
    "sentence_bert_config.json",
    "tokenizer.json",
    "tokenizer_config.json",
    "special_tokens_map.json",
    "sentencepiece.bpe.model"
)
$bigFile = "pytorch_model.bin"

New-Item -ItemType Directory -Force -Path $Dest | Out-Null

Write-Host "[1/2] 下载小文件…" -ForegroundColor Cyan
foreach ($f in $files) {
    $target = Join-Path $Dest $f
    if ((Test-Path $target) -and (Get-Item $target).Length -gt 0) {
        Write-Host "  跳过（已存在）$f"
        continue
    }
    & curl.exe -s -L --retry 3 -o $target "$base$f"
    Write-Host ("  完成 {0}  {1:N0} 字节" -f $f, (Get-Item $target).Length)
}

# 1_Pooling 子目录必须存在，否则 ST 加载时报找不到 pooling 配置
$poolDir = Join-Path $Dest "1_Pooling"
New-Item -ItemType Directory -Force -Path $poolDir | Out-Null
$poolFile = Join-Path $poolDir "config.json"
if (-not (Test-Path $poolFile)) {
    & curl.exe -s -L --retry 3 -o $poolFile "${base}1_Pooling/config.json"
    Write-Host "  完成 1_Pooling/config.json"
}

$bigTarget = Join-Path $Dest $bigFile

# 真实大小必须问服务端，不能硬编码。
# 之前写死 2165939814，比真实值少 100MB，结果下出来的 bin 缺了 zip 中央目录，
# torch.load 报 "failed finding central directory"。
$head = & curl.exe -s -L -I "$base$bigFile"
$lenLine = $head | Select-String -Pattern '^content-length:\s*(\d+)' -CaseSensitive:$false | Select-Object -First 1
if (-not $lenLine) { throw "拿不到远端文件大小" }
$expected = [int64]$lenLine.Matches[0].Groups[1].Value
Write-Host ("  远端大小 {0:N0} 字节（{1:N1} MB）" -f $expected, ($expected / 1MB))

if ((Test-Path $bigTarget) -and (Get-Item $bigTarget).Length -eq $expected) {
    Write-Host "[2/2] $bigFile 已完整，跳过" -ForegroundColor Green
    return
}

Write-Host "[2/2] 下载 $bigFile（2.1GB，$Parallel 分片并行）…" -ForegroundColor Cyan

# 断点续传：本地已有（但不足）时只补缺的那一段，
# 不重新下 2GB —— 之前就是靠这个把少的 100MB 尾巴补回来的。
$resumeFrom = 0
if (Test-Path $bigTarget) {
    $have = (Get-Item $bigTarget).Length
    if ($have -gt 0 -and $have -lt $expected) {
        $resumeFrom = $have
        Write-Host ("  检测到不完整文件 {0:N0} 字节，从该位置续传" -f $have) -ForegroundColor Yellow
    }
}

$chunk = [int64][Math]::Ceiling(($expected - $resumeFrom) / $Parallel)
$parts = @()
$sw = [Diagnostics.Stopwatch]::StartNew()

$jobs = @()
for ($i = 0; $i -lt $Parallel; $i++) {
    $start = $resumeFrom + $i * $chunk
    $end = [Math]::Min($start + $chunk - 1, $expected - 1)
    if ($start -gt $end) { continue }
    $partPath = Join-Path $Dest "$bigFile.part$i"
    $parts += $partPath
    $jobs += Start-Job -ScriptBlock {
        param($url, $s, $e, $out)
        & curl.exe -s -L --retry 5 --retry-delay 2 -o $out --range "$s-$e" $url
        (Get-Item $out).Length
    } -ArgumentList "$base$bigFile", $start, $end, $partPath
}

$jobs | Wait-Job -Timeout 3600 | Out-Null
$sizes = $jobs | Receive-Job
$jobs | Remove-Job -Force -ErrorAction SilentlyContinue
$sw.Stop()

$total = ($sizes | Measure-Object -Sum).Sum
Write-Host ("  分片完成：{0:N0} / {1:N0} 字节，用时 {2:N1}s，均速 {3:N2} MB/s" -f `
    $total, ($expected - $resumeFrom), $sw.Elapsed.TotalSeconds, ($total / 1MB / $sw.Elapsed.TotalSeconds))

if (($total + $resumeFrom) -ne $expected) {
    throw "分片总大小不符（已有 $resumeFrom + 收到 $total，期望 $expected），删掉 .part* 重跑"
}

# 拼接：用 FileStream 逐块拷贝，避免 Get-Content 把二进制读坏
# 续传时用 Append 模式，不覆盖已有部分
Write-Host "  拼接分片…" -ForegroundColor Cyan
$mode = if ($resumeFrom -gt 0) { [System.IO.FileMode]::Append } else { [System.IO.FileMode]::Create }
$out = [System.IO.File]::Open($bigTarget, $mode)
try {
    foreach ($p in $parts) {
        $in = [System.IO.File]::OpenRead($p)
        try { $in.CopyTo($out) } finally { $in.Dispose() }
    }
} finally { $out.Dispose() }
$parts | ForEach-Object { Remove-Item $_ -Force }

Write-Host ("完成：{0}  {1:N1} MB" -f $bigTarget, ((Get-Item $bigTarget).Length / 1MB)) -ForegroundColor Green