<script setup>
// 顶层组件：只负责布局 + 导航，具体内容交给 router-view 渲染
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, getUser, clearAuth } from './utils/token'
import SideMenu from './components/SideMenu.vue'

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

// 右侧快捷菜单：登录页不需要
const showSideMenu = computed(() => route.name !== 'login')

// 首页是"橙色渐变大头图"，顶栏要压在渐变上透明显示
const isHome = computed(() => route.name === 'home')

// 发布页顶栏做成品牌橙实底（参考闲鱼的黄色顶条）
const isSell = computed(() => route.name === 'sell')
</script>

<template>
  <div class="app" :class="{ 'on-hero': isHome, 'on-brand': isSell }">
    <header class="topbar" :class="{ 'on-hero': isHome, 'on-brand': isSell }">
      <!-- 点标题回首页 -->
      <RouterLink to="/" class="logo">二手交易平台</RouterLink>
      <nav>
        <RouterLink to="/">用户端</RouterLink>
        <RouterLink to="/admin">管理端</RouterLink>
        <!-- 「卖闲置」入口已移到右侧悬浮菜单（SideMenu.vue） -->
        <!-- 图片上传独立测试页：路由保留（/demo/upload），入口先隐藏 -->
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

    <!-- 右侧悬浮快捷菜单（固定在最右边，不随页面滚动） -->
    <SideMenu v-if="showSideMenu" />
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

/* 标题现在是个链接（点了回首页），要去掉链接默认样式 */
.topbar a.logo {
  font-weight: 700;
  font-size: 17px;
  color: var(--brand);
  text-decoration: none;
  padding: 0;
  cursor: pointer;
}

.topbar a.logo:hover {
  opacity: 0.85;
}

.topbar a.logo.router-link-active {
  /* 首页时标题也保持品牌色，不要变成"选中态"的底色块 */
  color: var(--brand);
  background: transparent;
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

/* ============================================================
 * 首页模式：顶栏浮在橙色渐变上
 *
 * 做法：顶栏改成绝对定位 + 透明背景，main 去掉内边距，
 * 让 HomeView 的渐变色块从页面最顶端开始铺。
 * ============================================================ */
.topbar.on-hero {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 30;
  background: transparent;
  border-bottom: none;
}

.topbar.on-hero a.logo,
.topbar.on-hero .logo {
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.12);
}

.topbar.on-hero nav a {
  color: rgba(255, 255, 255, 0.92);
}

.topbar.on-hero nav a.router-link-active {
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
}

.topbar.on-hero .who {
  color: #fff;
}

.topbar.on-hero .to-login {
  color: #fff !important;
}

.topbar.on-hero .badge {
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}

.topbar.on-hero .logout {
  border-color: rgba(255, 255, 255, 0.55);
  background: transparent;
  color: #fff;
}

.topbar.on-hero .logout:hover {
  border-color: #fff;
  color: #fff;
}

.app.on-hero main {
  padding: 0;
}

/* ============================================================
 * 发布页：顶栏改成品牌橙实底（参考闲鱼的黄色顶条）
 * 只换底色和文字色，位置仍在正常流里，不影响页面布局
 * ============================================================ */
.topbar.on-brand {
  background: var(--brand);
  border-bottom: none;
}

.topbar.on-brand a.logo,
.topbar.on-brand .logo,
.topbar.on-brand nav a,
.topbar.on-brand .who,
.topbar.on-brand .to-login,
.topbar.on-brand .logout {
  color: #fff;
}

.topbar.on-brand nav a.router-link-active {
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
}

.topbar.on-brand .badge {
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}

.topbar.on-brand .logout {
  border-color: rgba(255, 255, 255, 0.55);
  background: transparent;
}

.topbar.on-brand .logout:hover {
  border-color: #fff;
  color: #fff;
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