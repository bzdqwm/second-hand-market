<script setup>
// 顶层组件：只负责布局 + 导航，具体内容交给 router-view 渲染
</script>

<template>
  <div class="app">
    <header class="topbar">
      <span class="logo">二手交易平台</span>
      <nav>
        <RouterLink to="/">用户端</RouterLink>
        <RouterLink to="/admin">管理端</RouterLink>
      </nav>
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
</style>