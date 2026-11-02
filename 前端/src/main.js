import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

import HomeView from './views/HomeView.vue'
import AdminView from './views/AdminView.vue'

// 路由表
// /        → 用户端首页
// /admin   → 管理端（目前只有空壳）
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/admin', name: 'admin', component: AdminView }
  ]
})

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')