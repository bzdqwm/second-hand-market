<script setup>
// 账户设置：左边分区菜单，右边表单
//
// 两块内容：
//   基本资料 → PUT /api/user/profile（昵称 + 头像）
//   账户安全 → PUT /api/user/password（原密码 + 新密码）
//
// 改密码成功后必须清本地 token 并回登录页：新密码已生效，
// 旧 token 继续用会让人误以为"密码白改了"。
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMe, updateProfile, updatePassword } from '../api/user'
import { uploadImage } from '../api/upload'
import { setUser, getUser, clearAuth } from '../utils/token'

const router = useRouter()

const tab = ref('profile')        // 'profile' | 'security'
const loading = ref(false)
const me = ref(null)              // GET /user/me 的原始返回

// 基本资料表单
const profile = reactive({
  nickname: '',
  avatarKey: '',                  // 提交用：items/... 开头的 objectKey（没换头像时为空）
  avatarPreview: ''               // 显示用：签名地址
})

// 密码表单
const password = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const savingProfile = ref(false)
const savingPassword = ref(false)
const uploading = ref(false)
const fileInput = ref(null)

/** 首字兜底：没头像时 el-avatar 显示昵称第一个字 */
function initialOf() {
  const name = profile.nickname || (me.value && me.value.username) || '?'
  return name.slice(0, 1).toUpperCase()
}

async function load() {
  loading.value = true
  try {
    const data = await getMe()
    me.value = data
    profile.nickname = data.nickname || ''
    profile.avatarPreview = data.avatar || ''   // 后端已签好名
    profile.avatarKey = ''                      // 清空"待提交的新头像"
  } catch (e) {
    // 401 由拦截器处理并跳登录
  } finally {
    loading.value = false
  }
}

function pickAvatar() {
  if (uploading.value) return
  fileInput.value && fileInput.value.click()
}

async function onFileChange(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''      // 清空，才能重复选同一个文件
  if (!file) return

  uploading.value = true
  try {
    const data = await uploadImage(file)
    profile.avatarKey = data.objectKey      // 提交时用这个
    profile.avatarPreview = data.url        // 预览用这个
    ElMessage.success('头像已上传，记得点「保存修改」')
  } catch (e) {
    // 上传失败的提示由拦截器统一弹
  } finally {
    uploading.value = false
  }
}

async function saveProfile() {
  const nickname = profile.nickname.trim()
  if (!nickname) return ElMessage.warning('昵称不能为空')
  if (nickname.length > 8) return ElMessage.warning('昵称最多 8 个字')

  savingProfile.value = true
  try {
    const payload = { nickname }
    // 只有真上传了新头像才带 avatar 字段：
    // 不带 = 后端不动头像，带了 = 换成新 key
    if (profile.avatarKey) payload.avatar = profile.avatarKey

    await updateProfile(payload)

    // 同步本地登录态，顶栏昵称/头像立刻更新（不刷新页面）
    const cached = getUser() || {}
    setUser({ ...cached, nickname, avatar: profile.avatarPreview })

    ElMessage.success('资料已更新')
    await load()
  } catch (e) {
    // 400 的提示由拦截器弹
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!password.oldPassword) return ElMessage.warning('请输入原密码')
  if (!password.newPassword) return ElMessage.warning('请输入新密码')
  if (password.newPassword !== password.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  if (password.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (password.newPassword.length > 20) return ElMessage.warning('新密码最多 20 位')
  if (password.newPassword === password.oldPassword) {
    return ElMessage.warning('新密码不能与原密码相同')
  }

  savingPassword.value = true
  try {
    await updatePassword({
      oldPassword: password.oldPassword,
      newPassword: password.newPassword
    })
    // 改完就清登录态：让用户用新密码重新登一次，避免"改了密码还在用旧会话"
    clearAuth()
    ElMessage.success('密码已修改，请用新密码重新登录')
    router.push('/login')
  } catch (e) {
    // 400「原密码不正确」等提示由拦截器弹
  } finally {
    savingPassword.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="settings-page">
    <div class="settings-card" v-loading="loading">
      <!-- 左：分区菜单 -->
      <aside class="menu">
        <div class="menu-title">账户设置</div>
        <div class="menu-item" :class="{ active: tab === 'profile' }" @click="tab = 'profile'">
          基本资料
        </div>
        <div class="menu-item" :class="{ active: tab === 'security' }" @click="tab = 'security'">
          账户安全
        </div>
      </aside>

      <!-- 右：表单 -->
      <section class="panel">
        <!-- ---------- 基本资料 ---------- -->
        <template v-if="tab === 'profile'">
          <h2>基本资料</h2>

          <div class="avatar-row">
            <div class="avatar-wrap" :class="{ uploading }" @click="pickAvatar">
              <el-avatar :size="76" :src="profile.avatarPreview || undefined">
                {{ initialOf() }}
              </el-avatar>
              <div class="avatar-mask">{{ uploading ? '上传中…' : '更换头像' }}</div>
            </div>
            <div class="avatar-hint">
              点击头像更换<br />
              支持 JPG / PNG，不超过 10MB
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png"
              class="hidden-input"
              @change="onFileChange"
            />
          </div>

          <div class="field">
            <label>昵称</label>
            <el-input
              v-model="profile.nickname"
              maxlength="8"
              show-word-limit
              placeholder="最多 8 个字"
            />
          </div>

          <div class="field">
            <label>用户名</label>
            <el-input :model-value="me ? me.username : ''" disabled />
            <p class="tip">用户名是登录凭据，暂不支持修改</p>
          </div>

          <el-button type="primary" round :loading="savingProfile" @click="saveProfile">
            保存修改
          </el-button>
        </template>

        <!-- ---------- 账户安全 ---------- -->
        <template v-else>
          <h2>账户安全</h2>

          <div class="field">
            <label>原密码</label>
            <el-input
              v-model="password.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </div>

          <div class="field">
            <label>新密码</label>
            <el-input
              v-model="password.newPassword"
              type="password"
              show-password
              maxlength="20"
              placeholder="6~20 位"
            />
          </div>

          <div class="field">
            <label>确认新密码</label>
            <el-input
              v-model="password.confirmPassword"
              type="password"
              show-password
              maxlength="20"
              placeholder="再输一次新密码"
            />
          </div>

          <p class="tip warn">修改成功后需要用新密码重新登录</p>

          <el-button type="primary" round :loading="savingPassword" @click="savePassword">
            修改密码
          </el-button>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 860px;
  margin: 0 auto;
}

.settings-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  min-height: 460px;
}

/* ---------- 左菜单 ---------- */
.menu {
  padding: var(--space-md) var(--space-xs);
  border-right: 1px solid var(--border-light);
  background: #fafbfc;
}

.menu-title {
  padding: 0 var(--space-sm) var(--space-md);
  font-size: 13px;
  color: var(--text-muted);
}

.menu-item {
  padding: 10px var(--space-sm);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-body);
  cursor: pointer;
  user-select: none;
}

.menu-item:hover {
  background: var(--brand-light);
  color: var(--brand);
}

.menu-item.active {
  background: var(--brand-light);
  color: var(--brand);
  font-weight: 600;
}

/* ---------- 右表单 ---------- */
.panel {
  padding: var(--space-lg);
}

.panel h2 {
  font-size: 17px;
  color: var(--text-main);
  margin-bottom: var(--space-lg);
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.avatar-wrap {
  position: relative;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  flex: 0 0 auto;
  line-height: 0;
}

.avatar-wrap.uploading {
  cursor: progress;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.15s;
}

.avatar-wrap:hover .avatar-mask,
.avatar-wrap.uploading .avatar-mask {
  opacity: 1;
}

.avatar-hint {
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-muted);
}

.hidden-input {
  display: none;
}

.field {
  margin-bottom: var(--space-md);
}

.field label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-body);
}

.tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.tip.warn {
  margin-bottom: var(--space-md);
  padding: 8px 10px;
  border-radius: var(--radius-xs);
  background: var(--warn-bg);
  color: var(--warn-text);
}

@media (max-width: 640px) {
  .settings-card {
    grid-template-columns: 1fr;
  }

  .menu {
    display: flex;
    gap: var(--space-xs);
    border-right: none;
    border-bottom: 1px solid var(--border-light);
    overflow-x: auto;
  }

  .menu-title {
    display: none;
  }

  .menu-item {
    white-space: nowrap;
  }
}
</style>