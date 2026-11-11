<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/auth'
import { setToken, setUser } from '../utils/token'

const route = useRoute()
const router = useRouter()

// 被路由守卫踢过来时会带 ?redirect=/原来想去的地址
// 登录成功后优先回到那里；只接受站内路径，防止被构造成跳去外部网站
const redirectPath = (() => {
  const r = route.query.redirect
  const path = Array.isArray(r) ? r[0] : r
  return typeof path === 'string' && path.startsWith('/') ? path : ''
})()

// 当前 Tab：login / register
const tab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)

// ---------------- 登录 ----------------
const loginFormRef = ref()
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  // 前端先校验一遍，省一次没必要的请求
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loginLoading.value = true
  try {
    // 拦截器已经把 { code, msg, data } 拆掉了，这里拿到的就是 data
    const info = await login({
      username: loginForm.username,
      password: loginForm.password
    })

    // 存 token 和用户信息，后续请求由拦截器自动带上
    setToken(info.token)
    setUser({
      id: info.id,
      username: info.username,
      nickname: info.nickname,
      avatar: info.avatar,
      role: info.role
    })

    ElMessage.success('登录成功，欢迎 ' + (info.nickname || info.username))
    // 优先回到被拦截前想去的页面；没有就按角色分流
    if (redirectPath) {
      router.push(redirectPath)
    } else {
      router.push(info.role === 'ADMIN' ? '/admin' : '/')
    }
  } catch (e) {
    // 错误提示已由 request.js 统一弹过，这里不用再弹
  } finally {
    loginLoading.value = false
  }
}

// ---------------- 注册 ----------------
const registerFormRef = ref()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

// 规则和后端 RegisterDTO 上的注解保持一致
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度必须是 3~20 位', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字、下划线',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须是 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再输入一次密码', trigger: 'blur' },
    {
      // 自定义校验：和上面填的密码比对
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ max: 20, message: '昵称最长 20 位', trigger: 'blur' }]
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  registerLoading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      // 留空就不传，后端会用用户名当昵称
      nickname: registerForm.nickname || undefined
    })

    ElMessage.success('注册成功，请登录')
    // 注册完自动带回登录 Tab 并填好用户名，少让用户输一遍
    loginForm.username = registerForm.username
    loginForm.password = ''
    tab.value = 'login'
    registerFormRef.value.resetFields()
  } catch (e) {
    // 提示已由 request.js 处理（如"用户名已被注册"）
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">二手交易平台</div>
      <p class="subtitle">校园二手，买卖都放心</p>
      <p v-if="redirectPath" class="redirect-tip">
        请先登录，登录后将返回你刚才访问的页面
      </p>

      <el-tabs v-model="tab" stretch>
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @submit.prevent="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                clearable
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit"
              :loading="loginLoading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            @submit.prevent="handleRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="3~20 位字母、数字、下划线"
                size="large"
                clearable
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="6~20 位"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="再输入一次密码"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item label="昵称（可留空）" prop="nickname">
              <el-input
                v-model="registerForm.nickname"
                placeholder="不填则用用户名"
                size="large"
                clearable
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit"
              :loading="registerLoading"
              @click="handleRegister"
            >
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <p class="tip">注册即表示同意平台交易规范</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
}

.login-card {
  width: 400px;
  padding: 36px 32px 24px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
}

.brand {
  font-size: 22px;
  font-weight: 700;
  color: var(--brand);
  text-align: center;
}

.subtitle {
  margin: 6px 0 20px;
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
}

.redirect-tip {
  margin: -8px 0 var(--space-md);
  padding: 8px 10px;
  border-radius: var(--radius-xs);
  background: var(--warn-bg);
  color: var(--warn-text);
  font-size: 12px;
  text-align: center;
}

.submit {
  width: 100%;
  margin-top: 8px;
  background: var(--brand);
  border-color: var(--brand);
}

.submit:hover {
  background: var(--brand-hover);
  border-color: var(--brand-hover);
}

.tip {
  margin-top: 18px;
  font-size: 12px;
  color: var(--text-faint);
  text-align: center;
}

:deep(.el-tabs__item) {
  font-size: 15px;
}
</style>