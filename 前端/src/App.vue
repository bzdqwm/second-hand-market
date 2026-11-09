<script setup>
// 顶层组件：只负责布局 + 导航，具体内容交给 router-view 渲染
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, getUser, clearAuth } from './utils/token'

const route = useRoute()
const router = useRouter()

// 当前登录用户（未登录为 null）
// 用 watch 监听路由变化来刷新：登录成功后会 push 到新路由，退出登录也会跳转
const user = ref(getToken() ? getUser() : null)
watch(
  () => route.fullPath,
  () => {
    user.value = getToken() ? getUser() : null
  }
)

function logout() {
  clearAuth()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="app">
    <header class="topbar">
      <span class="logo">二手交易平台</span>
      <nav>
        <RouterLink to="/">用户端</RouterLink>
        <RouterLink to="/admin">管理端</RouterLink>
      </nav>

      <div class="user-area">
        <template v-if="user">
          <span class="who">{{ user.nickname || user.username }}</span>
          <span v-if="user.role === 'ADMIN'" class="badge">管理员</span>
          <button class="logout" @click="logout">退出</button>
        </template>
        <RouterLink v-else to="/login" class="to-login">登录 / 注册</RouterLink>
      </div>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Microsoft YaHei', system-ui, sans-serif;
  background: #f5f6f8;
  color: #333;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.logo {
  font-weight: 700;
  font-size: 17px;
  color: #ff6b35;
}

.topbar nav {
  display: flex;
  gap: 16px;
}

.topbar a {
  color: #666;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
}

.topbar a.router-link-active {
  color: #ff6b35;
  background: #fff3ee;
}

main {
  padding: 32px 24px;
}

/* 顶栏右侧：登录状态 */
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  font-size: 13px;
}

.who {
  color: #333;
  font-weight: 600;
}

.badge {
  padding: 1px 6px;
  border-radius: 3px;
  background: #fff3ee;
  color: #ff6b35;
  font-size: 11px;
}

.logout {
  padding: 4px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  color: #666;
  font-size: 12px;
  cursor: pointer;
}

.logout:hover {
  border-color: #ff6b35;
  color: #ff6b35;
}

.to-login {
  color: #ff6b35 !important;
}
</style>