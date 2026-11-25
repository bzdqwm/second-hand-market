<script setup>
// 顶层组件：只负责布局 + 导航，具体内容交给 router-view 渲染
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, getUser, setUser, clearAuth } from './utils/token'
import { getMe } from './api/user'
import SideMenu from './components/SideMenu.vue'

const route = useRoute()
const router = useRouter()

// 当前登录用户（未登录为 null）
const user = ref(getToken() ? getUser() : null)

function refreshUser() {
  user.value = getToken() ? getUser() : null
}

// 两个刷新时机：
//   ① 路由变化（登录成功会跳路由、退出也会跳）
//   ② 设置页保存资料后派发的自定义事件（不跳路由也要立刻变）
watch(() => route.fullPath, refreshUser)
onMounted(() => {
  window.addEventListener('market:user-updated', refreshUser)
  syncFromServer()
})
onBeforeUnmount(() => window.removeEventListener('market:user-updated', refreshUser))

/**
 * 进站时用服务端数据覆盖本地缓存。
 * <p>
 * 为什么需要：头像存的是签名地址，几小时后会过期，
 * 光靠 localStorage 里的旧值，过一夜再看顶栏头像就 404 了。
 * 这里刷新一次，昵称/头像都拿到最新的。
 */
async function syncFromServer() {
  if (!getToken()) return
  try {
    const me = await getMe()
    setUser({
      id: me.id,
      username: me.username,
      nickname: me.nickname,
      avatar: me.avatar,
      role: me.role
    })
  } catch (e) {
    // 401 由拦截器处理（清登录态跳登录页），其它错误忽略，继续用缓存值
  }
}

// 头像没图时显示昵称首字
const userInitial = computed(() => {
  const u = user.value
  if (!u) return ''
  const name = u.nickname || u.username || ''
  return name.slice(0, 1).toUpperCase()
})

// 顶栏下拉菜单的三项
function onUserCommand(cmd) {
  if (cmd === 'settings') {
    router.push('/settings')
  } else if (cmd === 'my') {
    router.push('/my')
  } else if (cmd === 'logout') {
    logout()
  }
}

function logout() {
  clearAuth()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 右侧快捷菜单：登录页不需要
const showSideMenu = computed(() => route.name !== 'login')

// 只有管理员才看到导航（用户端 / 管理端）；普通用户和游客只看标题
const isAdmin = computed(() => user.value?.role === 'ADMIN')

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
      <nav v-if="isAdmin">
        <RouterLink to="/">用户端</RouterLink>
        <RouterLink to="/admin">管理端</RouterLink>
        <!-- 「卖闲置」入口已移到右侧悬浮菜单（SideMenu.vue） -->
        <!-- 图片上传独立测试页：路由保留（/demo/upload），入口先隐藏 -->
      </nav>

      <div class="user-area">
        <template v-if="user">
          <!-- 点开是下拉菜单：账户设置 / 我发布的 / 退出登录 -->
          <el-dropdown trigger="click" placement="bottom-end" @command="onUserCommand">
            <span class="user-trigger">
              <el-avatar :size="28" :src="user.avatar || undefined" class="u-avatar">
                {{ userInitial }}
              </el-avatar>
              <span class="who">{{ user.nickname || user.username }}</span>
              <span v-if="user.role === 'ADMIN'" class="badge">管理员</span>
              <span class="caret">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">账户设置</el-dropdown-item>
                <el-dropdown-item command="my">我发布的</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

/* 顶栏用户下拉：头像 + 昵称 + 小三角 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  outline: none;
}

.user-trigger:hover {
  background: rgba(255, 107, 53, 0.08);
}

.u-avatar {
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  flex: 0 0 auto;
}

.caret {
  font-size: 10px;
  color: var(--text-muted);
}

.topbar.on-hero .user-trigger:hover,
.topbar.on-brand .user-trigger:hover {
  background: rgba(255, 255, 255, 0.18);
}

.topbar.on-hero .caret,
.topbar.on-brand .caret {
  color: rgba(255, 255, 255, 0.9);
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