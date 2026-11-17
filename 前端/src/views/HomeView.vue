<script setup>
// 首页集市（京东式布局）
//
// 结构：
//   橙色渐变头部
//     ├─ 搜索框
//     └─ 圆角面板：左「一级分类（hover 弹二级）」 + 右「轮播图」
//   为你推荐
//     └─ 工具条（价格区间 / 排序 / 清空 / 总数）+ 商品网格 + 分页
//
// 轮播图现在用 CSS 渐变占位，没有图片文件也能正常显示；
// 以后有真实海报，只要给 banners 里的项加 image 字段（模板已留位置）。

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getItemList } from '../api/item'
import { getCategoryList } from '../api/category'
import { toCascaderOptions } from '../utils/category'

const router = useRouter()

const categories = ref([])      // 两级树
const activeTopId = ref(null)   // 当前选中一级
const hoverTop = ref(null)      // 鼠标悬停的一级（控制二级浮层）
const listLoading = ref(false)
const items = ref([])
const total = ref(0)
const keywordInput = ref('')
const recommendRef = ref()

// 轮播位：先占位，后补图
// 补图时只加 image 字段，模板里 <img v-if="b.image"> 会接管
const banners = [
  {
    title: '把闲置，变成现金',
    desc: '拍照 → 填信息 → 挂出，三步就完事',
    gradient: 'linear-gradient(120deg, #ff8a3c 0%, #ff5e62 100%)',
    image: ''
  },
  {
    title: '九成新数码好物',
    desc: '手机 / 平板 / 笔记本，同城可面交',
    gradient: 'linear-gradient(120deg, #ffb347 0%, #ff7b54 100%)',
    image: ''
  },
  {
    title: '毕业季 · 清仓专场',
    desc: '家具 / 厨具 / 小电器，低价出',
    gradient: 'linear-gradient(120deg, #ffa17f 0%, #ff6f61 100%)',
    image: ''
  }
]

const query = reactive({
  page: 1,
  size: 12,
  categoryId: null,
  keyword: '',
  minPrice: null,
  maxPrice: null,
  sort: 'latest'
})

onMounted(async () => {
  await loadCategories()
  await fetchList()
})

async function loadCategories() {
  try {
    const list = await getCategoryList()
    categories.value = toCascaderOptions(list)
  } catch (e) {
    // 拦截器已弹提示
  }
}

async function fetchList() {
  listLoading.value = true
  try {
    const data = await getItemList(query)
    items.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    items.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

// 用户主动改条件时：回到第 1 页 + 滚到推荐区
function applyFilter() {
  query.page = 1
  fetchList()
  scrollToRecommend()
}

function scrollToRecommend() {
  recommendRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 点一级类目：再点一次取消
function selectTop(id) {
  hoverTop.value = null
  if (activeTopId.value === id) {
    activeTopId.value = null
    query.categoryId = null
  } else {
    activeTopId.value = id
    query.categoryId = id
  }
  applyFilter()
}

// 点二级类目：再点一次退回一级
function selectSub(id) {
  query.categoryId = query.categoryId === id ? activeTopId.value : id
  applyFilter()
}

function onSearch() {
  query.keyword = keywordInput.value.trim()
  applyFilter()
}

function resetAll() {
  activeTopId.value = null
  hoverTop.value = null
  keywordInput.value = ''
  query.categoryId = null
  query.keyword = ''
  query.minPrice = null
  query.maxPrice = null
  query.sort = 'latest'
  applyFilter()
}

function onPageChange(page) {
  query.page = page
  fetchList()
  scrollToRecommend()
}

function goDetail(item) {
  router.push(`/item/${item.id}`)
}

function fmtPrice(v) {
  return '¥' + Number(v || 0).toFixed(2)
}

function fmtDate(s) {
  return s ? s.slice(0, 10) : ''
}
</script>

<template>
  <div class="home">
    <!-- ================= 橙色渐变头部 ================= -->
    <section class="hero">
      <div class="hero-inner">
        <!-- 搜索框 -->
        <div class="searchbar">
          <div class="search-pill">
            <el-input
              v-model="keywordInput"
              placeholder="搜你想要的：iPad、机械键盘、考研书…"
              size="large"
              clearable
              class="search-input"
              @keyup.enter="onSearch"
              @clear="onSearch"
            />
            <el-button class="search-btn" @click="onSearch">搜索</el-button>
          </div>
        </div>

        <!-- 圆角面板：左类目 + 右轮播 -->
        <div class="market-panel">
          <!-- 左侧分类导航 -->
          <aside class="cat-nav" @mouseleave="hoverTop = null">
            <div
              class="cat-item cat-all"
              :class="{ active: activeTopId === null && !query.keyword }"
              @click="resetAll"
            >
              <span class="cat-name">全部商品</span>
            </div>

            <div
              v-for="c in categories"
              :key="c.id"
              class="cat-item"
              :class="{ active: activeTopId === c.id }"
              @mouseenter="hoverTop = c"
              @click="selectTop(c.id)"
            >
              <span class="cat-name">{{ c.name }}</span>
              <span class="cat-arrow">›</span>

              <!-- 二级浮层 -->
              <div
                v-if="hoverTop && hoverTop.id === c.id && c.children && c.children.length"
                class="cat-flyout"
              >
                <div class="flyout-head">
                  <span class="flyout-title">{{ c.name }}</span>
                  <span class="flyout-more" @click.stop="selectTop(c.id)">查看全部 ›</span>
                </div>
                <div class="flyout-chips">
                  <span
                    v-for="s in c.children"
                    :key="s.id"
                    class="fchip"
                    :class="{ active: query.categoryId === s.id }"
                    @click.stop="selectSub(s.id)"
                  >{{ s.name }}</span>
                </div>
              </div>
            </div>
          </aside>

          <!-- 右侧轮播 -->
          <div class="banner">
            <el-carousel height="360px" :interval="4500" arrow="hover">
              <el-carousel-item v-for="(b, i) in banners" :key="i">
                <div class="banner-slide" :style="{ background: b.gradient }">
                  <img v-if="b.image" :src="b.image" :alt="b.title" class="banner-img" />
                  <div class="banner-text">
                    <h2 class="banner-title">{{ b.title }}</h2>
                    <p class="banner-desc">{{ b.desc }}</p>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= 为你推荐 ================= -->
    <section ref="recommendRef" class="recommend">
      <div class="recommend-inner">
        <div class="recommend-head">
          <h2 class="recommend-title">
            <span class="title-dot"></span>为你推荐
          </h2>

          <div class="tools">
            <div class="price-filter">
              <el-input-number
                v-model="query.minPrice"
                :min="0"
                :controls="false"
                placeholder="最低价"
                class="price-box"
                @change="applyFilter"
              />
              <span class="dash">—</span>
              <el-input-number
                v-model="query.maxPrice"
                :min="0"
                :controls="false"
                placeholder="最高价"
                class="price-box"
                @change="applyFilter"
              />
            </div>

            <el-select v-model="query.sort" class="sort-select" @change="applyFilter">
              <el-option label="最新发布" value="latest" />
              <el-option label="价格从低到高" value="price_asc" />
              <el-option label="价格从高到低" value="price_desc" />
            </el-select>

            <el-button link type="primary" @click="resetAll">清空筛选</el-button>
            <span class="total-hint">共 {{ total }} 件</span>
          </div>
        </div>

        <div v-loading="listLoading" class="grid-wrap">
          <div v-if="items.length" class="grid">
            <div
              v-for="it in items"
              :key="it.id"
              class="item-card"
              @click="goDetail(it)"
            >
              <div class="cover">
                <img :src="it.coverThumbUrl" :alt="it.title" loading="lazy" />
              </div>
              <div class="card-body">
                <p class="card-title">{{ it.title }}</p>
                <p class="card-price">{{ fmtPrice(it.price) }}</p>
                <div class="card-meta">
                  <span class="seller">{{ it.sellerNickname }}</span>
                  <span class="date">{{ fmtDate(it.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>

          <el-empty
            v-else-if="!listLoading"
            :description="query.categoryId || query.keyword || query.minPrice !== null || query.maxPrice !== null
              ? '没有符合条件的商品，换个条件试试'
              : '还没有在售商品，去发布第一件吧'"
          />
        </div>

        <div v-if="total > query.size" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="query.size"
            :current-page="query.page"
            @current-change="onPageChange"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ================= 头部渐变 ================= */
.hero {
  /* 从顶部的品牌橙，慢慢过渡到白色，接住下面的内容区 */
  background: linear-gradient(
    180deg,
    #ff6b35 0%,
    #ff7f45 26%,
    #ffb08a 52%,
    #ffe6da 76%,
    #ffffff 100%
  );
  padding: calc(var(--header-h) + 18px) var(--space-lg) var(--space-lg);
}

.hero-inner {
  max-width: 1180px;
  margin: 0 auto;
}

.searchbar {
  display: flex;
  justify-content: center;
  margin-bottom: var(--space-lg);
}

/* 整条搜索栏做成白底胶囊（和右侧菜单一样的圆润感） */
.search-pill {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  max-width: 660px;
  padding: 4px 4px 4px 20px;
  background: #fff;
  border-radius: 999px;
  box-shadow: 0 6px 18px rgba(180, 80, 30, 0.14);
}

/* 输入框本体去掉自己的边框和背景，交给外层胶囊 */
.search-input {
  flex: 1;
  min-width: 0;
}

.search-input :deep(.el-input__wrapper) {
  padding-left: 0;
  background: transparent;
  box-shadow: none !important;
}

.search-input :deep(.el-input__inner) {
  font-size: 14px;
}

/* 搜索按钮：橙底白字的小胶囊，不贴死搜索栏 */
.search-btn {
  flex: 0 0 auto;
  width: 104px;
  height: 40px;
  padding: 0;
  border: none !important;
  border-radius: 999px !important;
  background: var(--brand) !important;
  color: var(--brand-text) !important;
  font-size: 15px;
  font-weight: 600;
}

.search-btn:hover,
.search-btn:focus {
  background: var(--brand-hover) !important;
  color: var(--brand-text) !important;
}

/* ================= 圆角面板 ================= */
.market-panel {
  display: flex;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: 0 10px 28px rgba(180, 80, 30, 0.14);
  /* 不写 overflow:hidden —— 否则二级浮层会被裁掉 */
}

/* ---------- 左：分类导航 ---------- */
.cat-nav {
  position: relative;
  flex: 0 0 208px;
  padding: var(--space-xs) 0;
  border-right: 1px solid var(--border-light);
}

.cat-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 46px;
  padding: 0 var(--space-md);
  font-size: 14px;
  color: var(--text-body);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.cat-item:hover,
.cat-item.active {
  background: var(--brand-light);
  color: var(--brand);
}

.cat-all {
  font-weight: 600;
  color: var(--text-main);
}

.cat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-arrow {
  color: var(--text-faint);
  font-size: 15px;
}

.cat-item:hover .cat-arrow {
  color: var(--brand);
}

/* 二级浮层 */
.cat-flyout {
  position: absolute;
  top: -1px;
  left: 100%;
  z-index: 40;
  width: 520px;
  padding: var(--space-md);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  box-shadow: 8px 8px 20px rgba(0, 0, 0, 0.08);
}

.flyout-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-xs);
}

.flyout-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.flyout-more {
  font-size: 12px;
  color: var(--brand);
  cursor: pointer;
}

.flyout-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}

.fchip {
  padding: 5px 12px;
  border-radius: var(--radius-xs);
  font-size: 13px;
  color: var(--text-body);
  cursor: pointer;
}

.fchip:hover {
  color: var(--brand);
  background: var(--brand-light);
}

.fchip.active {
  color: var(--brand-text);
  background: var(--brand);
}

/* ---------- 右：轮播 ---------- */
.banner {
  flex: 1;
  min-width: 0;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  overflow: hidden;
}

.banner-slide {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 56px;
}

.banner-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-text {
  position: relative;
  z-index: 1;
  color: #fff;
}

.banner-title {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.16);
}

.banner-desc {
  margin: 0;
  font-size: 14px;
  opacity: 0.92;
}

/* ================= 为你推荐 ================= */
.recommend {
  background: var(--bg-page);
  padding: var(--space-lg) var(--space-lg) 48px;
}

.recommend-inner {
  max-width: 1180px;
  margin: 0 auto;
}

.recommend-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.recommend-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: var(--text-main);
}

.title-dot {
  width: 6px;
  height: 18px;
  border-radius: 3px;
  background: var(--brand);
}

.tools {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-left: auto;
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 4px;
}

.price-box {
  width: 92px;
}

.dash {
  color: var(--text-faint);
}

.sort-select {
  width: 140px;
}

.total-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.grid-wrap {
  min-height: 240px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: var(--space-md);
}

.item-card {
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
}

.item-card:hover {
  transform: translateY(-3px);
  border-color: var(--brand);
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.16);
}

.cover {
  aspect-ratio: 1 / 1;
  background: var(--border-light);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-body {
  padding: var(--space-xs) var(--space-sm) var(--space-sm);
}

.card-title {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--text-main);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.card-price {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 700;
  color: var(--brand);
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
}

.seller {
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  display: flex;
  justify-content: center;
  padding: var(--space-lg) 0 0;
}

/* 窄屏 */
@media (max-width: 900px) {
  .cat-nav {
    display: none;
  }

  .banner {
    border-radius: var(--radius-lg);
  }

  .banner-title {
    font-size: 22px;
  }

  .tools {
    margin-left: 0;
    width: 100%;
  }
}
</style>