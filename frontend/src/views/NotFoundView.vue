<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="state-page">
    <div class="state-card">
      <div class="state-code">404</div>
      <h2>页面不存在</h2>
      <p class="muted">你访问的页面可能已被移除，或地址输入有误。</p>
      <el-button type="primary" @click="router.replace({ name: 'home' })">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.state-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--bg);
}

.state-card {
  width: min(440px, 100%);
  padding: 44px 32px;
  text-align: center;
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow);
}

.state-code {
  font-size: 52px;
  font-weight: 700;
  color: var(--brand);
  letter-spacing: 0.08em;
}

.state-card h2 {
  margin: 12px 0 8px;
  font-size: 20px;
}

.state-card .muted {
  margin-bottom: 22px;
}
</style>
