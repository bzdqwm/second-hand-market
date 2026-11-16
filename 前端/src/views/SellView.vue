<script setup>
// 发布闲置页面
//
// 流程：填标题/描述/选类目/填价格 → 上传图片 → 点发布
// 图片先单独传到 OSS（ImageUploader 内部），这里只提交 objectKey

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
    // Step 8 做完商品详情页后，这里改成 router.push(`/item/${id}`)
    console.log('新商品 id =', id)
    router.push('/')
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
    <div class="page-head">
      <h2>发布闲置</h2>
      <p class="sub">填好信息、传几张实拍图，就能挂出去卖啦</p>
    </div>

    <!-- ★ AI 提示区：Step 11 在这里点亮（AI 推荐类目 / 合理价格区间） -->
    <div class="ai-tip">
      <span class="ai-badge">AI 助手</span>
      <span class="ai-text">填写标题后，AI 会帮你推荐类目和合理价格区间（Step 11 接入）</span>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="88px"
      label-position="right"
      class="sell-form"
    >
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="form.title"
          maxlength="200"
          show-word-limit
          placeholder="例如：九成新 iPad Air 5 64G 深空灰"
        />
      </el-form-item>

      <el-form-item label="类目" prop="categoryId">
        <el-cascader
          v-model="form.categoryId"
          :options="categoryOptions"
          :props="cascaderProps"
          :loading="categoryLoading"
          placeholder="请选择类目"
          clearable
          class="full-width"
        />
      </el-form-item>

      <el-form-item label="价格" prop="price">
        <el-input-number
          v-model="form.price"
          :min="0.01"
          :max="99999999.99"
          :precision="2"
          :step="1"
          controls-position="right"
          placeholder="0.00"
          class="price-input"
        />
        <span class="unit">元</span>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="5"
          maxlength="5000"
          show-word-limit
          placeholder="说说成色、购买时间、有无磕碰、配件是否齐全…"
        />
      </el-form-item>

      <el-form-item label="图片" prop="images">
        <div class="upload-wrap">
          <ImageUploader v-model="form.images" :limit="5" />
          <p class="upload-tip">第一张会作为封面图，建议放最清晰的一张</p>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          发布
        </el-button>
        <el-button :disabled="submitting" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.sell-page {
  max-width: 760px;
  margin: 0 auto;
}

.page-head h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: var(--text-main);
}

.page-head .sub {
  margin: 0 0 var(--space-md);
  font-size: 13px;
  color: var(--text-muted);
}

/* AI 提示区：先占位，Step 11 再填内容 */
.ai-tip {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-bottom: var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  border: 1px dashed var(--brand);
  border-radius: var(--radius-md);
  background: var(--brand-light);
  font-size: 13px;
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

.sell-form {
  padding: var(--space-lg);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.full-width {
  width: 100%;
}

.price-input {
  width: 200px;
}

.unit {
  margin-left: var(--space-xs);
  font-size: 13px;
  color: var(--text-muted);
}

.upload-wrap {
  width: 100%;
}

.upload-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}
</style>