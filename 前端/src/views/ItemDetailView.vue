<script setup>
// 商品详情页：图片轮播 + 信息区 + 卖家信息
//
// 「立即购买 / 下载 / 问答」三个按钮按计划书先占位，不接逻辑

import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getItemDetail, changeItemStatus } from '../api/item'

const route = useRoute()

const item = ref(null)
const loading = ref(false)
const failed = ref('')
const statusLoading = ref(false)

const STATUS_TEXT = {
  ON_SALE: '在售',
  OFF_SALE: '已下架',
  PENDING_AUDIT: '审核中',
  SOLD: '已售出',
  REJECTED: '已驳回',
  DRAFT: '草稿'
}

const statusText = computed(() => STATUS_TEXT[item.value?.status] || item.value?.status || '')

// 下架/已售的商品对买家不可见，卖家自己还能看到（后端 owner 已算好）
const canToggle = computed(
  () => item.value?.owner && ['ON_SALE', 'OFF_SALE', 'PENDING_AUDIT'].includes(item.value.status)
)

onMounted(load)

async function load() {
  loading.value = true
  failed.value = ''
  try {
    item.value = await getItemDetail(route.params.id)
  } catch (e) {
    // 404 的商品走到这里：拦截器已经弹过提示，这里只负责显示空态
    item.value = null
    failed.value = e?.message || '商品不存在或已下架'
  } finally {
    loading.value = false
  }
}

async function toggleStatus() {
  const target = item.value.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  statusLoading.value = true
  try {
    await changeItemStatus(item.value.id, target)
    ElMessage.success(target === 'ON_SALE' ? '已重新上架' : '已下架')
    await load()
  } finally {
    statusLoading.value = false
  }
}

function placeholder(name) {
  ElMessage.info(`${name}功能会在后续 Step 接入`)
}

function fmtPrice(v) {
  return Number(v || 0).toFixed(2)
}
</script>

<template>
  <div v-loading="loading" class="detail">
    <!-- 加载失败 / 商品不存在 -->
    <el-empty v-if="!loading && !item" :description="failed || '商品不存在或已下架'">
      <el-button type="primary" @click="$router.push('/')">回首页看看别的</el-button>
    </el-empty>

    <template v-else-if="item">
      <div class="main">
        <!-- 左：图片 -->
        <div class="gallery">
          <el-carousel
            v-if="item.images && item.images.length > 1"
            height="440px"
            :autoplay="false"
            indicator-position="outside"
          >
            <el-carousel-item v-for="(img, i) in item.images" :key="i">
              <img :src="img.url" :alt="item.title" class="big-img" />
            </el-carousel-item>
          </el-carousel>

          <div v-else-if="item.images && item.images.length === 1" class="single">
            <img :src="item.images[0].url" :alt="item.title" class="big-img" />
          </div>

          <el-empty v-else description="这个商品没有图片" />
        </div>

        <!-- 右：信息区 -->
        <div class="info">
          <div class="price-row">
            <span class="price">¥{{ fmtPrice(item.price) }}</span>
            <el-tag v-if="item.status !== 'ON_SALE'" type="info" size="small">{{ statusText }}</el-tag>
          </div>

          <h1 class="title">{{ item.title }}</h1>

          <div class="attrs">
            <span class="attr">类目：{{ item.categoryName || '未分类' }}</span>
            <span class="attr">发布：{{ item.createTime }}</span>
          </div>

          <!-- 卖家卡片 -->
          <div v-if="item.seller" class="seller-box">
            <el-avatar :size="40" :src="item.seller.avatar || undefined">
              {{ (item.seller.nickname || '?').slice(0, 1) }}
            </el-avatar>
            <div class="seller-meta">
              <p class="seller-name">{{ item.seller.nickname }}</p>
              <p class="seller-since">{{ (item.seller.createTime || '').slice(0, 10) }} 入驻</p>
            </div>
            <el-tag v-if="item.owner" size="small" type="warning">你的商品</el-tag>
          </div>

          <!-- 卖家自己的操作：上下架（Step 7 的接口） -->
          <div v-if="canToggle" class="owner-bar">
            <el-button :loading="statusLoading" size="small" @click="toggleStatus">
              {{ item.status === 'ON_SALE' ? '下架商品' : '重新上架' }}
            </el-button>
            <span class="owner-hint">上架后才会出现在首页</span>
          </div>

          <!-- 三个占位按钮 -->
          <div class="actions">
            <el-button type="primary" size="large" class="act-main" @click="placeholder('立即购买')">
              立即购买
            </el-button>
            <el-button size="large" @click="placeholder('下载')">下载</el-button>
            <el-button size="large" @click="placeholder('问答')">问答</el-button>
          </div>
          <p class="actions-hint">交易、下载、问答能力按计划书在后续 Step 实现</p>
        </div>
      </div>

      <!-- 描述 -->
      <div class="desc-card">
        <h2>商品描述</h2>
        <p class="desc">{{ item.description || '卖家没有填写描述' }}</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail {
  max-width: 1040px;
  margin: 0 auto;
  min-height: 320px;
}

.main {
  display: flex;
  gap: var(--space-lg);
  align-items: flex-start;
}

/* 左侧图片 */
.gallery {
  flex: 0 0 440px;
  width: 440px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.big-img {
  width: 100%;
  height: 440px;
  object-fit: contain;
  background: #fafafa;
  display: block;
}

.single .big-img {
  border-radius: var(--radius-md);
}

/* 右侧信息 */
.info {
  flex: 1;
  min-width: 0;
  padding: var(--space-lg);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}

.price-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.price {
  font-size: 28px;
  font-weight: 700;
  color: var(--brand);
}

.title {
  margin: var(--space-xs) 0 var(--space-sm);
  font-size: 18px;
  line-height: 1.5;
  color: var(--text-main);
}

.attrs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  padding-bottom: var(--space-sm);
  border-bottom: 1px solid var(--border-light);
  font-size: 12px;
  color: var(--text-muted);
}

.seller-box {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--border-light);
}

.seller-meta {
  flex: 1;
  min-width: 0;
}

.seller-name {
  margin: 0;
  font-size: 14px;
  color: var(--text-main);
}

.seller-since {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-muted);
}

.owner-bar {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding-top: var(--space-sm);
}

.owner-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.actions {
  display: flex;
  gap: var(--space-xs);
  margin-top: var(--space-lg);
}

.act-main {
  flex: 1;
  max-width: 200px;
}

.actions-hint {
  margin: var(--space-xs) 0 0;
  font-size: 12px;
  color: var(--text-faint);
}

/* 描述 */
.desc-card {
  margin-top: var(--space-md);
  padding: var(--space-lg);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}

.desc-card h2 {
  margin: 0 0 var(--space-sm);
  font-size: 15px;
  color: var(--text-main);
}

.desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-body);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 窄屏：图片区变全宽 */
@media (max-width: 860px) {
  .main {
    flex-direction: column;
  }

  .gallery {
    flex: none;
    width: 100%;
  }
}
</style>