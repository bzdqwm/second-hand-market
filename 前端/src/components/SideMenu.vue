<script setup>
// 右侧悬浮快捷菜单（参考闲鱼右侧竖排那一条）
//
// 只有「卖闲置」有交互，其余按钮先占位：有样子、点了不响应。
// 以后要做哪个，就把 noops 里对应项移到可点击列表里。

import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '../utils/token'

const router = useRouter()

// 暂时无功能的入口（点击无反应）
const placeholders = [
  { key: 'msg', label: '消息', d: 'M4 5h16v11H9l-5 4V5z' },
  { key: 'app', label: 'APP', d: 'M8 3h8v18H8z M11 18h2' },
  { key: 'feedback', label: '反馈', d: 'M6 3v18 M6 4h11l-2.5 4L17 12H6' },
  { key: 'service', label: '客服', d: 'M5 13v-1a7 7 0 0 1 14 0v1 M3 13h3v5H4a1 1 0 0 1-1-1v-4z M21 13h-3v5h2a1 1 0 0 0 1-1v-4z' }
]

function goSell() {
  if (getToken()) {
    router.push('/sell')
  } else {
    ElMessage.info('请先登录后再发布')
    router.push({ path: '/login', query: { redirect: '/sell' } })
  }
}
</script>

<template>
  <aside class="side-menu">
    <!-- 唯一可点的：卖闲置 -->
    <button class="side-item side-btn" title="发闲置" @click="goSell">
      <span class="sell-badge">
        <svg class="side-icon icon-in-badge" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 4h8l9 9-7 7-9-9V4z" />
          <path d="M8 8h.01" />
        </svg>
      </span>
      <span class="side-label">卖闲置</span>
    </button>

    <div class="side-divider"></div>

    <!-- 占位入口：只有样子，点了没反应 -->
    <div
      v-for="p in placeholders"
      :key="p.key"
      class="side-item side-disabled"
      :title="p.label + '（后续版本开放）'"
    >
      <svg class="side-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path :d="p.d" />
      </svg>
      <span class="side-label">{{ p.label }}</span>
    </div>
  </aside>
</template>

<style scoped>
.side-menu {
  position: fixed;
  top: 50%;
  right: 14px;
  z-index: 60;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 0;
  /* 整条做成胶囊：左右两端全圆，浮在页面右侧不贴边 */
  background: var(--bg-card);
  border-radius: 999px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-50%);
  /* 让 hover 背景被圆角裁掉，不会在弧线上露出直角 */
  overflow: hidden;
}

.side-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 58px;
  padding: 9px 0;
  border: none;
  background: transparent;
  color: var(--text-sub);
  font-family: inherit;
}

.side-icon {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* 卖闲置：橙色圆形徽标里套一个图标 */
.sell-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border-radius: 50%;
  background: var(--brand);
}

.icon-in-badge {
  width: 15px;
  height: 15px;
  stroke: #fff;
  stroke-width: 2;
}

.side-label {
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
}

/* 可点的那个 */
.side-btn {
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.side-btn:hover {
  color: var(--brand);
  background: var(--brand-light);
}

/* 占位的那几个：不给手型、不给 hover，点了确实没反应 */
.side-disabled {
  cursor: default;
  color: var(--text-faint);
  user-select: none;
}

.side-divider {
  width: 30px;
  height: 1px;
  margin: 3px 0;
  background: var(--border-light);
}
</style>