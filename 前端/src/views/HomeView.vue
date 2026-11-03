<script setup>
import { ref } from 'vue'

// 三个服务的连通状态
const javaStatus = ref('未检测')
const aiStatus = ref('未检测')
const loading = ref(false)
const aiLoading = ref(false)

// 调用 Java：注意请求的是 /api/ping，
// 浏览器发到 5173，Vite 的 proxy 转发到 8081
async function checkJava() {
  loading.value = true
  javaStatus.value = '请求中...'
  try {
    const res = await fetch('/api/ping')
    const body = await res.json()
    // Step 3 起统一返回体是 { code, msg, data }，业务成败看 code，不看 HTTP 状态码
    if (body.code !== undefined && body.code !== 200) {
      javaStatus.value = '❌ ' + body.code + ' ' + body.msg
      return
    }
    javaStatus.value = '✅ 通　' + JSON.stringify(body.data)
  } catch (e) {
    javaStatus.value = '❌ 失败：' + e.message
  } finally {
    loading.value = false
  }
}

// 调用 Java 的 /api/ping-ai：
// 浏览器 → 5173 → Vite proxy → 8081 → Java 再用 RestClient 请求 Python 的 /ai/ping
// 所以这一个按钮同时验证了「前端→Java」和「Java→Python」两段链路
async function checkAI() {
  aiLoading.value = true
  aiStatus.value = '请求中...'
  try {
    const res = await fetch('/api/ping-ai')
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const body = await res.json()
    if (body.code !== undefined && body.code !== 200) {
      aiStatus.value = '❌ ' + body.code + ' ' + body.msg
      return
    }
    // 注意：Python 的响应被 Java 包进了 data.python，外面还有一层 Result 的 data
    aiStatus.value = '✅ 通　' + JSON.stringify(body.data.python)
  } catch (e) {
    aiStatus.value = '❌ 失败：' + e.message
  } finally {
    aiLoading.value = false
  }
}
</script>

<template>
  <div class="card">
    <h1>用户端首页</h1>
    <p class="hint">这个页面现在只有连通性检测，真正的商品列表要到 Step 7。</p>

    <div class="row">
      <span class="label">Java 后端</span>
      <button :disabled="loading" @click="checkJava">检测</button>
      <code class="status">{{ javaStatus }}</code>
    </div>

    <div class="row">
      <span class="label">Python AI</span>
      <button :disabled="aiLoading" @click="checkAI">检测</button>
      <code class="status">{{ aiStatus }}</code>
    </div>
  </div>
</template>

<style scoped>
.card {
  max-width: 760px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}

h1 {
  font-size: 20px;
  margin-bottom: 8px;
}

.hint {
  color: #999;
  font-size: 13px;
  margin-bottom: 24px;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
}

.label {
  width: 90px;
  font-size: 14px;
  color: #666;
}

button {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background: #ff6b35;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.status {
  font-size: 12px;
  color: #555;
  word-break: break-all;
}
</style>