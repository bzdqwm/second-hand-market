<script setup>
// 发布闲置页面（布局参考闲鱼「发闲置」）
//
// 流程：填标题/选类目/传图/写描述/填价格 → 点发布
// 图片先单独传到 OSS（ImageUploader 内部），这里只提交 objectKey
//
// ⚠️ 只做项目里真实存在的字段：标题、类目、图片、描述、价格。
//    闲鱼上的「宝贝所在地」「发货方式」「原价」「属性规格」等一律不做。

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ImageUploader from '../components/ImageUploader.vue'
import { createItem } from '../api/item'
import { getCategoryList } from '../api/category'
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

        <!-- ★ AI 提示区：Step 11 在这里点亮（类目推荐 / 合理价格区间） -->
        <div class="ai-tip">
          <span class="ai-badge">AI 助手</span>
          <span class="ai-text">上传主图、填写标题后，AI 会帮你推荐类目和合理价格区间（Step 11 接入）</span>
        </div>

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