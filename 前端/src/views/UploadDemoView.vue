<script setup>
// 临时演示页：验证图片上传组件（Step 6 收尾用）
// 等「发布商品」页面做出来后，这个页面就可以删掉，组件直接搬到发布页

import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ImageUploader from '../components/ImageUploader.vue'

const images = ref([])

function copy(text) {
  navigator.clipboard
    .writeText(text)
    .then(() => ElMessage.success('已复制'))
    .catch(() => ElMessage.error('复制失败，请手动选中'))
}

function clearAll() {
  images.value = []
}
</script>

<template>
  <div class="demo-page">
    <h2>图片上传测试</h2>
    <p class="desc">
      选 5 张图上传 → 缩略图显示在列表里 → 点缩略图可看带水印的大图。
      <br />
      注意：地址是 30 分钟有效的签名链接，服务端存的是 objectKey。
    </p>

    <ImageUploader v-model="images" :limit="5" />

    <div class="result">
      <div class="result-head">
        <span>已上传结果（{{ images.length }} 条）</span>
        <el-button v-if="images.length" size="small" text type="danger" @click="clearAll">
          清空
        </el-button>
      </div>

      <el-empty v-if="!images.length" description="还没有上传任何图片" :image-size="60" />

      <div v-for="(img, i) in images" :key="img.objectKey" class="item">
        <div class="item-title">第 {{ i + 1 }} 张</div>
        <div class="row">
          <span class="label">objectKey</span>
          <code>{{ img.objectKey }}</code>
          <el-button size="small" text @click="copy(img.objectKey)">复制</el-button>
        </div>
        <div class="row">
          <span class="label">尺寸</span>
          <code>{{ img.width }} × {{ img.height }}</code>
        </div>
        <div class="row">
          <span class="label">原图地址（带水印）</span>
          <a :href="img.url" target="_blank" rel="noreferrer">新标签页打开</a>
          <el-button size="small" text @click="copy(img.url)">复制</el-button>
        </div>
        <div class="row">
          <span class="label">缩略图地址</span>
          <a :href="img.thumbUrl" target="_blank" rel="noreferrer">新标签页打开</a>
          <el-button size="small" text @click="copy(img.thumbUrl)">复制</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.demo-page {
  max-width: 880px;
  margin: 0 auto;
}

h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.desc {
  margin: 0 0 var(--space-md);
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-muted);
}

.result {
  margin-top: var(--space-lg);
  padding: var(--space-md);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
}

.item {
  padding: var(--space-sm) 0;
  border-top: 1px dashed var(--border-light);
}

.item-title {
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--brand);
}

.row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-bottom: 4px;
  font-size: 12px;
}

.label {
  flex: 0 0 130px;
  color: var(--text-muted);
}

code {
  flex: 1;
  word-break: break-all;
  color: var(--text-body);
}

a {
  color: var(--brand);
}
</style>