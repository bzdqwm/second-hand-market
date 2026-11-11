<script setup>
// 管理端空壳。Step 9 做审核台，Step 20 做举报/用户管理，都长在这个路由下面。
</script>

<template>
  <div class="card">
    <h1>管理端</h1>
    <p class="hint">空壳。后续顺序：审核台（Step 9）→ 举报处理（Step 20）→ 用户管理（Step 20）。</p>
  </div>
</template>

<style scoped>
.card {
  max-width: 760px;
  padding: var(--space-lg);
  background: var(--bg-card);
  border-radius: var(--radius-md);
}

h1 {
  font-size: 20px;
  margin-bottom: 8px;
}

.hint {
  color: var(--text-muted);
  font-size: 13px;
}
</style>