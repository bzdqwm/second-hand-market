<script setup>
// 多图上传组件
//
// 用法：
//   <ImageUploader v-model="images" :limit="5" />
//
// v-model 出去的是「上传成功」的图片数组，每项形如：
//   { objectKey, url, thumbUrl, width, height }
//
// 注意：url / thumbUrl 是 30 分钟的签名地址，只用来即时预览，
// 真正要存数据库的是 objectKey（永久有效）。

import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../api/upload'

const props = defineProps({
  /** 已上传完成的图片列表（父组件通过 v-model 绑定） */
  modelValue: { type: Array, default: () => [] },
  /** 最多几张 */
  limit: { type: Number, default: 5 },
  /** 单张大小上限（MB） */
  maxSizeMB: { type: Number, default: 10 }
})

const emit = defineEmits(['update:modelValue'])

// el-upload 自己的列表（负责状态、进度条、缩略图展示）
const fileList = ref([])

// 上传成功的结果，自己维护一份：uid → 后端返回的 UploadVO
// 为什么不直接读 fileList 里的 response？
//   多个文件是并发上传的，onSuccess 触发时 el-upload 还没把 response
//   写回列表项，直接读会导致"传了 3 张、结果只有 1 条"。
const results = ref([])

// 预览弹窗
const previewVisible = ref(false)
const previewUrl = ref('')
const previewKey = ref('')

// 还能不能加图（满了就把 + 号藏起来）
const full = computed(() => fileList.value.length >= props.limit)

/** 把上传成功的项同步给父组件（去掉内部用的 uid） */
function syncModel() {
  emit(
    'update:modelValue',
    results.value.map(({ uid, ...data }) => data)
  )
}

/** 选中文件后、上传前的校验 */
function beforeUpload(file) {
  const allowed = ['image/jpeg', 'image/png']
  if (!allowed.includes(file.type)) {
    ElMessage.error('只支持 JPG / PNG 格式')
    return false
  }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`单张图片不能超过 ${props.maxSizeMB}MB`)
    return false
  }
  return true
}

/** 自定义上传：走我们自己的 axios 实例（这样才能带上 token） */
async function customUpload(options) {
  const { file, onSuccess, onError } = options
  try {
    const data = await uploadImage(file)

    // 先记进自己的结果表，再通知 el-upload
    results.value = [
      ...results.value.filter((r) => r.uid !== file.uid),
      { uid: file.uid, ...data }
    ]

    // 列表里用缩略图地址显示，别加载几 MB 的原图
    const item = fileList.value.find((f) => f.uid === file.uid)
    if (item) item.url = data.thumbUrl

    // 顺手把返回值也挂到列表项上，方便预览时直接用
    if (item) item.response = data

    onSuccess(data, file)
    syncModel()
    ElMessage.success('上传成功')
  } catch (e) {
    onError(e)
  }
}

/** 超出数量限制 */
function handleExceed() {
  ElMessage.warning(`最多上传 ${props.limit} 张图片`)
}

/** 删除 */
function handleRemove(uploadFile) {
  // el-upload 已把它从 fileList 移除，这里把结果表里对应的也删掉
  results.value = results.value.filter((r) => r.uid !== uploadFile.uid)
  syncModel()
}

/** 点击缩略图放大预览 */
function handlePreview(file) {
  const data = results.value.find((r) => r.uid === file.uid) || file.response || {}
  // 预览用带水印的原图地址，没有就退回列表用的缩略图
  previewUrl.value = data.url || file.url || ''
  previewKey.value = data.objectKey || ''
  previewVisible.value = true
}

/** 父组件从外部清空时，同步清掉内部列表和结果表 */
watch(
  () => props.modelValue,
  (val) => {
    if (val.length === 0 && (fileList.value.length > 0 || results.value.length > 0)) {
      fileList.value = []
      results.value = []
    }
  }
)
</script>

<template>
  <div class="image-uploader">
    <el-upload
      v-model:file-list="fileList"
      list-type="picture-card"
      :multiple="true"
      :limit="limit"
      :http-request="customUpload"
      :before-upload="beforeUpload"
      :on-exceed="handleExceed"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      accept="image/jpeg,image/png"
      :class="{ 'is-full': full }"
    >
      <div class="add-btn">
        <span class="plus">+</span>
        <span class="tip">添加图片</span>
      </div>
    </el-upload>

    <p class="hint">
      已上传 {{ fileList.filter((f) => f.status === 'success').length }} / {{ limit }} 张，
      支持 JPG / PNG，单张不超过 {{ maxSizeMB }}MB
    </p>

    <!-- 预览大图 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="720px">
      <img v-if="previewUrl" :src="previewUrl" class="preview-img" alt="预览图片" />
      <p v-if="previewKey" class="preview-key">objectKey：{{ previewKey }}</p>
    </el-dialog>
  </div>
</template>

<style scoped>
.image-uploader {
  width: 100%;
}

.add-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
}

.add-btn .plus {
  font-size: 22px;
  line-height: 1;
}

.add-btn .tip {
  font-size: 12px;
}

/* 满 5 张后隐掉「+」按钮 */
.image-uploader :deep(.is-full .el-upload--picture-card) {
  display: none;
}

.hint {
  margin: var(--space-xs) 0 0;
  font-size: 12px;
  color: var(--text-muted);
}

.preview-img {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}

.preview-key {
  margin-top: var(--space-sm);
  font-size: 12px;
  color: var(--text-muted);
  word-break: break-all;
}
</style>