<script setup>
// 管理端 · 审核台
//
// 三栏布局：
//   左：待审队列（点一条，中间和右边跟着变）
//   中：商品图文（审核的主要依据）
//   右：机审结论 + 处置按钮
//
// 数据来源：
//   GET /api/admin/audit/list   待审列表
//   GET /api/admin/audit/{id}   商品图文 + 最近一次机审记录
//   PUT /api/admin/audit/{id}   处置（PASS 通过 / REJECT 驳回）
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuditList, getAuditDetail, handleAudit } from '../api/audit'
import { riskLabel, riskType, statusLabel, statusType } from '../utils/itemStatus'

const rows = ref([])          // 待审队列
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const detail = ref(null)      // 当前选中的商品详情
const detailLoading = ref(false)
const handling = ref('')      // 正在处置的结论：'' | 'PASS' | 'REJECT'

// 机审记录：详情里的 machineRecord 可能为 null（没跑过机审）
const machine = computed(() => (detail.value ? detail.value.machineRecord : null))

// 只有「待审核」的商品才能处置；处置请求进行中也禁用按钮，防连点
const canHandle = computed(
  () => !!detail.value && detail.value.status === 'PENDING_AUDIT' && !handling.value
)

async function loadList() {
  loading.value = true
  try {
    const data = await getAuditList({ page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
    // 自动选中第一条，省一次点击；队列空了就清空中间和右边
    if (rows.value.length) {
      await selectItem(rows.value[0].id)
    } else {
      detail.value = null
    }
  } catch (e) {
    // 错误提示由 request.js 统一弹出，这里只要不让异常冒到控制台
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function selectItem(itemId) {
  if (!itemId) return
  detailLoading.value = true
  try {
    detail.value = await getAuditDetail(itemId)
  } catch (e) {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  loadList()
}

async function onPass() {
  try {
    await ElMessageBox.confirm(
      `确认让「${detail.value.title}」通过审核并上架？通过后它会立刻出现在首页。`,
      '通过审核',
      { confirmButtonText: '通过并上架', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return // 用户点了取消
  }
  await submit('PASS')
}

async function onReject() {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('驳回理由会展示给卖家，请写清楚要改什么。', '驳回商品', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：描述含违规内容 / 图片与实物不符',
      inputValidator: (v) => {
        if (!v || !v.trim()) return '驳回必须填写理由'
        if (v.trim().length < 2) return '理由至少 2 个字'
        return true
      }
    })
    reason = value.trim()
  } catch (e) {
    return
  }
  await submit('REJECT', reason)
}

async function submit(result, reason) {
  handling.value = result
  try {
    await handleAudit(detail.value.id, { result, reason })
    ElMessage.success(result === 'PASS' ? '已通过，商品已上架' : '已驳回，理由已记录')
    detail.value = null
    await loadList()
  } catch (e) {
    // 400 的提示已经在拦截器里弹过，这里保持界面不变
  } finally {
    handling.value = ''
  }
}

onMounted(loadList)
</script>

<template>
  <div class="audit-page">
    <div class="audit-head">
      <h1>审核台</h1>
      <span class="count">待审核 {{ total }} 件</span>
      <el-button class="refresh" size="small" :loading="loading" @click="loadList">刷新</el-button>
    </div>

    <div class="audit-body">
      <!-- ---------- 左：待审队列 ---------- -->
      <aside class="queue">
        <div v-if="loading && !rows.length" class="queue-empty">加载中…</div>

        <template v-else-if="rows.length">
          <div
            v-for="row in rows"
            :key="row.id"
            class="queue-item"
            :class="{ active: detail && detail.id === row.id }"
            @click="selectItem(row.id)"
          >
            <img v-if="row.coverThumbUrl" class="q-thumb" :src="row.coverThumbUrl" alt="" />
            <div v-else class="q-thumb q-thumb-empty">无图</div>
            <div class="q-info">
              <div class="q-title">{{ row.title }}</div>
              <div class="q-meta">
                <span class="q-price">¥{{ row.price }}</span>
                <el-tag size="small" effect="light" :type="riskType(row.risk)">
                  {{ riskLabel(row.risk) }}
                </el-tag>
              </div>
            </div>
          </div>

          <el-pagination
            v-if="total > size"
            class="queue-page"
            small
            layout="prev, pager, next"
            :total="total"
            :page-size="size"
            :current-page="page"
            @current-change="onPageChange"
          />
        </template>

        <el-empty v-else description="暂无待审商品" :image-size="80" />
      </aside>

      <!-- ---------- 中：商品图文 ---------- -->
      <section class="preview" v-loading="detailLoading">
        <template v-if="detail">
          <div class="gallery">
            <el-carousel
              v-if="detail.images && detail.images.length > 1"
              height="330px"
              :autoplay="false"
              arrow="hover"
            >
              <el-carousel-item v-for="(img, i) in detail.images" :key="i">
                <img class="big-img" :src="img.url" alt="" />
              </el-carousel-item>
            </el-carousel>
            <img
              v-else-if="detail.images && detail.images.length"
              class="big-img"
              :src="detail.images[0].url"
              alt=""
            />
            <div v-else class="big-img big-img-empty">卖家没有上传图片</div>
          </div>

          <div class="preview-info">
            <div class="pv-title-row">
              <h2>{{ detail.title }}</h2>
              <el-tag effect="light" :type="statusType(detail.status)">
                {{ statusLabel(detail.status) }}
              </el-tag>
            </div>
            <div class="pv-price">¥{{ detail.price }}</div>
            <div class="pv-meta">
              <span>卖家：{{ detail.seller ? detail.seller.nickname : '—' }}</span>
              <span>提交时间：{{ detail.createTime }}</span>
              <span>商品 ID：{{ detail.id }}</span>
            </div>
            <p class="pv-desc">{{ detail.description || '（卖家没有填写描述）' }}</p>
          </div>
        </template>

        <el-empty v-else description="从左侧选一件商品开始审核" />
      </section>

      <!-- ---------- 右：机审结论 + 处置 ---------- -->
      <aside class="verdict">
        <h3>机审结论</h3>

        <template v-if="machine">
          <div class="risk-row">
            <el-tag size="large" effect="dark" :type="riskType(machine.risk)">
              {{ riskLabel(machine.risk) }}
            </el-tag>
            <span class="score">风险分 {{ machine.score === null ? '—' : machine.score }}</span>
          </div>
          <p class="reason">{{ machine.reason || '（机审未给出说明）' }}</p>
          <div class="line"><span>机审结论</span><b>{{ machine.machineResult || '—' }}</b></div>
          <div class="line"><span>机审时间</span><b>{{ machine.createTime }}</b></div>
        </template>

        <el-empty v-else description="没有机审记录" :image-size="70" />

        <div class="actions">
          <el-button
            type="success"
            class="act-btn"
            :disabled="!canHandle"
            :loading="handling === 'PASS'"
            @click="onPass"
          >
            通过并上架
          </el-button>
          <el-button
            type="danger"
            class="act-btn"
            :disabled="!canHandle"
            :loading="handling === 'REJECT'"
            @click="onReject"
          >
            驳回
          </el-button>
        </div>

        <p v-if="detail && detail.status !== 'PENDING_AUDIT'" class="tip">
          该商品当前状态为「{{ statusLabel(detail.status) }}」，不能在这里处置。
        </p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.audit-page {
  max-width: 1280px;
  margin: 0 auto;
}

.audit-head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.audit-head h1 {
  font-size: 20px;
  color: var(--text-main);
}

.count {
  font-size: 13px;
  color: var(--text-muted);
}

.refresh {
  margin-left: auto;
}

/* 三栏：固定左 260 / 右 300，中间自适应 */
.audit-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 300px;
  gap: var(--space-md);
  align-items: start;
}

/* ---------- 左：队列 ---------- */
.queue {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-xs);
  max-height: 660px;
  overflow-y: auto;
}

.queue-empty {
  padding: var(--space-lg);
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

.queue-item {
  display: flex;
  gap: var(--space-xs);
  padding: 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  border: 1px solid transparent;
}

.queue-item:hover {
  background: var(--brand-light);
}

.queue-item.active {
  background: var(--brand-light);
  border-color: var(--brand);
}

.q-thumb {
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  border-radius: var(--radius-xs);
  object-fit: cover;
  background: var(--bg-page);
}

.q-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-faint);
}

.q-info {
  min-width: 0;
  flex: 1;
}

.q-title {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.q-price {
  font-size: 13px;
  font-weight: 700;
  color: var(--brand);
}

.queue-page {
  justify-content: center;
  padding: var(--space-xs) 0;
}

/* ---------- 中：预览 ---------- */
.preview {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  min-height: 420px;
}

.gallery {
  background: var(--bg-page);
}

.big-img {
  width: 100%;
  height: 330px;
  object-fit: contain;
  display: block;
  background: var(--bg-page);
}

.big-img-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-faint);
  font-size: 13px;
}

.preview-info {
  padding: var(--space-md);
}

.pv-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.pv-title-row h2 {
  font-size: 18px;
  color: var(--text-main);
  flex: 1;
  min-width: 0;
}

.pv-price {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--brand);
}

.pv-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.pv-desc {
  margin-top: var(--space-sm);
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-body);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- 右：机审结论 ---------- */
.verdict {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  position: sticky;
  top: var(--space-md);
}

.verdict h3 {
  font-size: 15px;
  color: var(--text-main);
  margin-bottom: var(--space-sm);
}

.risk-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-xs);
}

.score {
  font-size: 12px;
  color: var(--text-muted);
}

.reason {
  margin: var(--space-sm) 0;
  padding: 10px;
  border-radius: var(--radius-xs);
  background: var(--bg-page);
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-body);
  word-break: break-word;
}

.line {
  display: flex;
  justify-content: space-between;
  gap: var(--space-xs);
  padding: 6px 0;
  font-size: 13px;
  color: var(--text-muted);
  border-top: 1px dashed var(--border-light);
}

.line b {
  color: var(--text-main);
  font-weight: 600;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  margin-top: var(--space-md);
}

.act-btn {
  width: 100%;
  margin: 0 !important;
}

.tip {
  margin-top: var(--space-xs);
  font-size: 12px;
  line-height: 1.6;
  color: var(--warn-text);
  background: var(--warn-bg);
  border-radius: var(--radius-xs);
  padding: 8px;
}

/* 窄屏：三栏堆成一栏 */
@media (max-width: 1080px) {
  .audit-body {
    grid-template-columns: 1fr;
  }

  .queue {
    max-height: none;
  }

  .verdict {
    position: static;
  }
}
</style>