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
        <!-- 临时入口：图片上传组件验证用，发布页做出来后删掉 -->
        <RouterLink to="/demo/upload">图片上传</RouterLink>
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
/* 全局骨架样式。色值/尺寸一律来自 styles/variables.css */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: var(--font-family);
  background: var(--bg-page);
  color: var(--text-main);
}

.topbar {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  height: var(--header-h);
  padding: 0 var(--space-lg);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
}

.logo {
  font-weight: 700;
  font-size: 17px;
  color: var(--brand);
}

.topbar nav {
  display: flex;
  gap: var(--space-md);
}

.topbar a {
  color: var(--text-sub);
  text-decoration: none;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}

.topbar a.router-link-active {
  color: var(--brand);
  background: var(--brand-light);
}

main {
  padding: 32px var(--space-lg);
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
  color: var(--text-main);
  font-weight: 600;
}

.badge {
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--brand-light);
  color: var(--brand);
  font-size: 11px;
}

.logout {
  padding: 4px 10px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-sub);
  font-size: 12px;
  cursor: pointer;
}

.logout:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.to-login {
  color: var(--brand) !important;
}
</style>