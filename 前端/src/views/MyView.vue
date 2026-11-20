<script setup>
// 我发布的：卖家看自己所有状态的商品（包括被驳回的和已下架的）
//
// 为什么需要这页：商品下架后，首页看不到、也没有别的入口回到它，
// 卖家的商品等于"丢了"。这页就是那个入口。
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyItems } from '../api/item'
import { statusLabel, statusType, MY_FILTERS } from '../utils/itemStatus'

const router = useRouter()

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getMyItems({ page: page.value, size: size.value, status: statusFilter.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onFilter(value) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

function goDetail(id) {
  router.push(`/item/${id}`)
}

onMounted(load)
</script>

<template>
  <div class="my-page">
    <div class="my-card">
      <div class="my-head">
        <h1>我发布的</h1>
        <el-button type="primary" round @click="router.push('/sell')">发布闲置</el-button>
      </div>

      <div class="filters">
        <span
          v-for="f in MY_FILTERS"
          :key="f.value"
          class="chip"
          :class="{ active: statusFilter === f.value }"
          @click="onFilter(f.value)"
        >
          {{ f.label }}
        </span>
        <span class="total">共 {{ total }} 件</span>
      </div>

      <div v-if="loading && !rows.length" class="state">加载中…</div>

      <el-empty v-else-if="!rows.length" description="这里还没有商品">
        <el-button type="primary" round @click="router.push('/sell')">去发布一件</el-button>
      </el-empty>

      <template v-else>
        <div v-for="row in rows" :key="row.id" class="row">
          <img
            v-if="row.coverThumbUrl"
            class="thumb"
            :src="row.coverThumbUrl"
            alt=""
            @click="goDetail(row.id)"
          />
          <div v-else class="thumb thumb-empty">无图</div>

          <div class="row-main">
            <div class="row-title" @click="goDetail(row.id)">{{ row.title }}</div>
            <div class="row-sub">¥{{ row.price }} · 发布 {{ row.createTime }} · ID {{ row.id }}</div>
            <div v-if="row.status === 'REJECTED' && row.rejectReason" class="row-reject">
              驳回理由：{{ row.rejectReason }}
            </div>
            <div v-else-if="row.status === 'PENDING_AUDIT'" class="row-pending">
              正在等待审核，通过后会出现在首页
            </div>
          </div>

          <el-tag class="row-status" effect="light" :type="statusType(row.status)">
            {{ statusLabel(row.status) }}
          </el-tag>

          <el-button text type="primary" @click="goDetail(row.id)">查看</el-button>
        </div>

        <el-pagination
          v-if="total > size"
          class="pager"
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
.my-page {
  max-width: 900px;
  margin: 0 auto;
}

.my-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}

.my-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}

.my-head h1 {
  font-size: 20px;
  color: var(--text-main);
}

/* ---------- 状态筛选 ---------- */
.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-xs);
  padding-bottom: var(--space-sm);
  border-bottom: 1px solid var(--border-light);
}

.chip {
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--text-body);
  background: var(--bg-page);
  cursor: pointer;
  user-select: none;
}

.chip:hover {
  color: var(--brand);
}

.chip.active {
  color: var(--brand-text);
  background: var(--brand);
}

.total {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-muted);
}

.state {
  padding: 40px 0;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

/* ---------- 每一行商品 ---------- */
.row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--border-light);
}

.thumb {
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  border-radius: var(--radius-md);
  object-fit: cover;
  background: var(--bg-page);
  cursor: pointer;
}

.thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-faint);
  cursor: default;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-title:hover {
  color: var(--brand);
}

.row-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.row-reject {
  margin-top: 6px;
  padding: 6px 10px;
  border-radius: var(--radius-xs);
  background: #fef0f0;
  color: #c45656;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.row-pending {
  margin-top: 6px;
  padding: 6px 10px;
  border-radius: var(--radius-xs);
  background: var(--warn-bg);
  color: var(--warn-text);
  font-size: 12px;
}

.row-status {
  flex: 0 0 auto;
}

.pager {
  justify-content: center;
  margin-top: var(--space-md);
}

@media (max-width: 640px) {
  .row {
    flex-wrap: wrap;
  }

  .row-status {
    margin-left: 84px;
  }
}
</style>