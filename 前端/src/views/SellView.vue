<script setup>
// 发布闲置页面（布局参考闲鱼「发闲置」）
//
// 流程：填标题/选类目/传图/写描述/填价格 → 点发布
// 图片先单独传到 OSS（ImageUploader 内部），这里只提交 objectKey
//
// ⚠️ 只做项目里真实存在的字段：标题、类目、图片、描述、价格。
//    闲鱼上的「宝贝所在地」「发货方式」「原价」「属性规格」等一律不做。

import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ImageUploader from '../components/ImageUploader.vue'
import { createItem } from '../api/item'
import { getCategoryList } from '../api/category'
import { recognizeCategory } from '../api/ai'
import { toCascaderOptions } from '../utils/category'

const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const categoryOptions = ref([])
const categoryLoading = ref(false)

// form.images 存的是 UploadVO 对象（含 objectKey/url/thumbUrl），
// 提交时才映射成 objectKey 数组
const form = reactive({
  title: '',
  description: '',
  categoryId: null,
  price: null,
  images: []
})

const rules = {
  title: [
    { required: true, message: '请填写标题', trigger: 'blur' },
    { max: 200, message: '标题最多 200 字', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择类目', trigger: 'change' }],
  price: [{ required: true, message: '请填写价格', trigger: 'blur' }],
  description: [{ max: 5000, message: '描述最多 5000 字', trigger: 'blur' }],
  images: [
    { type: 'array', required: true, min: 1, message: '至少上传 1 张图片', trigger: 'change' }
  ]
}

// 级联选择器配置
const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  // 只取最后一级的 id（不要 [一级id, 二级id] 这种数组）
  emitPath: false,
  expandTrigger: 'hover'
}

onMounted(loadCategories)

// ==================== AI 类目推荐 ====================
// 交互：用户打标题 → 停手 800ms → 调后端猜类目 → 够可信就自动填好
//
// 为什么要防抖：打「iPad 2019 95新」会触发 10 次 input 事件，
// 不防抖就是 10 次 HTTP + 10 次 embedding 计算，白烧算力。
const AI_DEBOUNCE_MS = 800
// 太短的标题没信息量（「i」「ip」），别浪费请求
const AI_MIN_TITLE_LEN = 2

const aiLoading = ref(false)
// { hit, candidates, matched, threshold } —— 后端返回的整包结果
const aiResult = ref(null)
// 当前类目是不是 AI 填的（用户自己点过类目就置 false，不再显示「已自动填」）
const aiFilled = ref(false)

let aiTimer = null
// 请求序号：只认最后一次响应，防止先发的慢请求覆盖后发的快请求
let aiSeq = 0

// 标题变化 → 重新计时
watch(
  () => form.title,
  (val) => {
    clearTimeout(aiTimer)

    const title = (val || '').trim()
    if (title.length < AI_MIN_TITLE_LEN) {
      aiResult.value = null
      aiFilled.value = false
      aiLoading.value = false
      aiSeq++ // 让飞行中的请求作废
      return
    }

    aiTimer = setTimeout(() => runRecognize(title), AI_DEBOUNCE_MS)
  }
)

// 用户自己改了类目 → 取消「AI 自动填写」的状态标记
watch(
  () => form.categoryId,
  (val) => {
    if (aiFilled.value && val !== aiResult.value?.hit?.categoryId) {
      aiFilled.value = false
    }
  }
)

async function runRecognize(title) {
  const seq = ++aiSeq
  aiLoading.value = true

  try {
    const res = await recognizeCategory({ title, topK: 3 })
    if (seq !== aiSeq) return // 已经有更新的请求了，这个结果丢掉

    aiResult.value = res

    // 只有 matched 才自动填；不达标就只把候选摆出来让用户点
    if (res.matched && res.hit) {
      form.categoryId = res.hit.categoryId
      aiFilled.value = true
      // 程序赋值不会触发 el-form 的 change 校验，手动把红字清掉
      formRef.value?.clearValidate('categoryId')
    }
  } catch {
    // 拦截器已经弹过提示，这里只需把加载态收掉（AI 挂了不影响手动发布）
    if (seq === aiSeq) aiResult.value = null
  } finally {
    if (seq === aiSeq) aiLoading.value = false
  }
}

/** 用户点了候选类目 */
function pickCategory(hit) {
  form.categoryId = hit.categoryId
  aiFilled.value = false
  formRef.value?.clearValidate('categoryId')
}

/** 0.5048 → "50%" */
function scorePct(score) {
  return Math.round((score || 0) * 100) + '%'
}

onBeforeUnmount(() => clearTimeout(aiTimer))

async function loadCategories() {
  categoryLoading.value = true
  try {
    const list = await getCategoryList()
    categoryOptions.value = toCascaderOptions(list)
  } catch (e) {
    // 错误提示由 axios 拦截器统一弹
  } finally {
    categoryLoading.value = false
  }
}

async function handleSubmit() {
  // el-form 校验：不通过会 reject，直接返回
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const id = await createItem({
      title: form.title.trim(),
      description: form.description.trim() || null,
      categoryId: form.categoryId,
      price: form.price,
      // ★ 只发 objectKey。url / thumbUrl 是 30 分钟的签名地址，绝不能存库
      images: form.images.map((img) => img.objectKey)
    })

    ElMessage.success('发布成功')
    // 跳到自己刚发布的详情页（非在售状态只有卖家本人能打开，正是这里）
    router.push(`/item/${id}`)
  } catch (e) {
    // 失败提示由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  form.images = []
}
</script>

<template>
  <div class="sell-page">
    <div class="sell-card">
      <h1 class="page-title">发闲置</h1>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="sell-form"
      >
        <!-- ============ 基础信息 ============ -->
        <h2 class="section-title">基础信息</h2>

        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
            size="large"
            placeholder="例如：九成新 iPad Air 5 64G 深空灰"
          />
        </el-form-item>

        <el-form-item label="类目" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="categoryOptions"
            :props="cascaderProps"
            :loading="categoryLoading"
            size="large"
            placeholder="请选择类目"
            clearable
            class="full-width"
          />
        </el-form-item>

        <!-- ★ AI 类目助手：标题打完 800ms 自动识别，紧贴类目选择器 -->
        <div class="ai-tip">
          <span class="ai-badge">AI 助手</span>

          <div class="ai-body">
            <!-- 识别中 -->
            <template v-if="aiLoading">
              <span class="ai-text">正在识别类目…</span>
            </template>

            <!-- 识别成功且够可信：已经帮你填好 -->
            <template v-else-if="aiResult && aiResult.matched && aiFilled">
              <span class="ai-text">
                已自动填好类目：<b>{{ aiResult.hit.path }}</b>
                <span class="ai-score">置信度 {{ scorePct(aiResult.hit.score) }}</span>
              </span>
              <span class="ai-hint">选错了直接在上面改</span>
            </template>

            <!-- 有候选但不够可信：摆出来让用户点 -->
            <template v-else-if="aiResult && aiResult.candidates.length">
              <span class="ai-text">不太确定，猜的是这几个，点一下就用：</span>
              <div class="ai-cands">
                <el-tag
                  v-for="c in aiResult.candidates"
                  :key="c.categoryId"
                  class="ai-cand"
                  effect="plain"
                  @click="pickCategory(c)"
                >
                  {{ c.path }} · {{ scorePct(c.score) }}
                </el-tag>
              </div>
            </template>

            <!-- 初始态 -->
            <template v-else>
              <span class="ai-text">填好标题后，AI 会自动帮你识别类目</span>
            </template>
          </div>
        </div>

        <el-form-item label="宝贝图片" prop="images">
          <div class="upload-wrap">
            <ImageUploader v-model="form.images" :limit="5" />
            <p class="field-tip">第一张会作为封面图，建议放最清晰的一张</p>
          </div>
        </el-form-item>

        <el-form-item label="宝贝描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            maxlength="5000"
            show-word-limit
            placeholder="描述一下宝贝的品牌型号、成色、购买时间、有无磕碰、配件是否齐全…"
          />
        </el-form-item>

        <!-- ============ 价格 ============ -->
        <h2 class="section-title">价格</h2>

        <el-form-item label="价格" prop="price">
          <div class="price-field">
            <span class="yen">¥</span>
            <el-input-number
              v-model="form.price"
              :min="0.01"
              :max="99999999.99"
              :precision="2"
              :controls="false"
              placeholder="0.00"
              class="price-input"
            />
          </div>
          <p class="field-tip">定个能卖出去的价。发布后可在详情页下架修改</p>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部吸底提交条 -->
    <div class="submit-bar">
      <div class="submit-inner">
        <el-button link :disabled="submitting" @click="handleReset">重置</el-button>
        <el-button
          class="publish-btn"
          :loading="submitting"
          @click="handleSubmit"
        >
          发布
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sell-page {
  max-width: 760px;
  margin: 0 auto;
  padding-bottom: 96px; /* 给底部吸底条留位，别遮住价格输入框 */
}

/* 白色大卡片（闲鱼那种一整块） */
.sell-card {
  padding: 32px 40px 8px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-title {
  margin: 0 0 var(--space-lg);
  font-size: 26px;
  font-weight: 700;
  color: var(--text-main);
}

/* 分区小标题 */
.section-title {
  margin: var(--space-lg) 0 var(--space-sm);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.section-title:first-of-type {
  margin-top: 0;
}

/* 标签在上方：比默认的大一点、深一点 */
.sell-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.sell-form :deep(.el-form-item) {
  margin-bottom: var(--space-md);
}

.full-width {
  width: 100%;
}

.upload-wrap {
  width: 100%;
}

.field-tip {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-muted);
}

/* 价格：左侧 ¥ 号 + 无步进按钮的输入框 */
.price-field {
  position: relative;
  width: 220px;
}

.yen {
  position: absolute;
  top: 50%;
  left: 12px;
  z-index: 2;
  transform: translateY(-50%);
  font-size: 14px;
  color: var(--text-sub);
  pointer-events: none;
}

.price-input {
  width: 100%;
}

.price-input :deep(.el-input__inner) {
  padding-left: 28px;
  font-size: 16px;
}

/* AI 提示区：先占位，Step 11 再填内容 */
.ai-tip {
  display: flex;
  align-items: flex-start;
  gap: var(--space-xs);
  margin: var(--space-md) 0 var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  border: 1px dashed var(--brand);
  border-radius: var(--radius-md);
  background: var(--brand-light);
  font-size: 13px;
  line-height: 1.6;
}

.ai-badge {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: var(--brand);
  color: var(--brand-text);
  font-size: 11px;
}

.ai-text {
  color: var(--text-sub);
}

/* AI 提示区内部排版：左边徽标，右边多行内容 */
.ai-body {
  flex: 1;
  min-width: 0;
}

.ai-score {
  margin-left: 6px;
  padding: 0 6px;
  border-radius: var(--radius-sm);
  background: var(--brand);
  color: var(--brand-text);
  font-size: 11px;
  white-space: nowrap;
}

.ai-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

/* 候选类目：可点的标签 */
.ai-cands {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.ai-cand {
  cursor: pointer;
}

.ai-cand:hover {
  border-color: var(--brand);
  color: var(--brand);
}

/* ================= 吸底提交条 ================= */
.submit-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(6px);
  border-top: 1px solid var(--border);
}

.submit-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  max-width: 760px;
  margin: 0 auto;
  padding: 12px var(--space-md);
}

/* 橙色胶囊按钮，和顶栏同色 */
.publish-btn {
  width: 240px;
  height: 46px;
  border: none;
  border-radius: 999px;
  background: var(--brand);
  color: var(--brand-text);
  font-size: 16px;
  font-weight: 600;
}

.publish-btn:hover,
.publish-btn:focus {
  background: var(--brand-hover);
  color: var(--brand-text);
}
</style>