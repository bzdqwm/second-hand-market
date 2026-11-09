import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

import HomeView from './views/HomeView.vue'
import AdminView from './views/AdminView.vue'
import LoginView from './views/LoginView.vue'
import { getToken, getUser } from './utils/token'

// 路由表
// /        → 用户端首页（游客可看）
// /admin   → 管理端，需要登录 + ADMIN 角色
// /login   → 登录 + 注册（同一个页面的两个 Tab）
//
// meta.requiresAuth : 标记"必须登录才能进"
// meta.roles        : 标记"必须是指定角色之一"
// meta.title        : 浏览器标签页标题
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: '首页' }
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminView,
      meta: { requiresAuth: true, roles: ['ADMIN'], title: '管理端' }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录' }
    },
    // 兜底：访问不存在的路径回首页，别让用户看到白屏
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      redirect: '/'
    }
  ]
})

// 全局前置守卫：每次路由跳转前都会跑一遍
// 返回值规则：
//   return true                → 放行
//   return '/xxx'              → 改跳这个路径
//   return { path, query }     → 改跳（可带参数）
router.beforeEach((to) => {
  const token = getToken()
  const user = getUser()

  // ① 需要登录，但没有 token → 去登录页，并把原本想去的地址带上
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // ② 已经登录了还去登录页 → 按角色送回各自首页，避免重复登录
  if (to.path === '/login' && token) {
    return user && user.role === 'ADMIN' ? '/admin' : '/'
  }

  // ③ 页面要求特定角色，但当前用户不满足 → 回首页
  //    注意：这只是"体验层"的拦截，真正的权限由后端 Security 保证
  if (to.meta.roles && (!user || !to.meta.roles.includes(user.role))) {
    ElMessage.warning('该页面仅管理员可访问')
    return '/'
  }

  return true
})

// 切换页面时同步浏览器标签标题
router.afterEach((to) => {
  const name = to.meta && to.meta.title
  document.title = (name ? name + ' - ' : '') + '二手交易平台'
})

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')