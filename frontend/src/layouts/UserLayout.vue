<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const homeActive = computed(() => route.path === '/' || route.path.startsWith('/themes') || route.path.startsWith('/packages'))
const ordersActive = computed(() => route.path.startsWith('/orders'))

function handleCommand(command: string): void {
  if (command === 'profile') {
    router.push({ name: auth.isAdmin ? 'admin-profile' : 'profile' })
  } else if (command === 'admin') {
    router.push({ name: 'admin-orders' })
  } else if (command === 'logout') {
    auth.logout()
    router.push({ name: 'home' })
  }
}
</script>

<template>
  <div class="user-shell">
    <header class="user-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-name">解忧摄影</span>
          <span class="brand-sub">JIEYOU STUDIO</span>
        </router-link>

        <nav class="main-nav">
          <router-link to="/" class="nav-link" :class="{ active: homeActive }">主题摄影</router-link>
          <router-link
            v-if="auth.isUser"
            to="/orders"
            class="nav-link"
            :class="{ active: ordersActive }"
          >
            我的订单
          </router-link>
          <router-link v-if="auth.isAdmin" to="/admin/orders" class="nav-link">管理端</router-link>
        </nav>

        <div class="user-area">
          <template v-if="auth.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="user-chip" type="button">
                <span class="avatar-dot">{{ (auth.displayName || 'U').charAt(0) }}</span>
                <span class="user-name">{{ auth.displayName || '用户' }}</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin">进入管理端</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push({ name: 'login' })">登录</el-button>
            <el-button type="primary" plain @click="router.push({ name: 'register' })">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="user-main">
      <router-view />
    </main>

    <footer class="user-footer">
      <div class="container footer-inner">
        <span>© 2026 解忧摄影 · 记录每一份值得珍藏的时光</span>
        <span class="muted">JIÉYŌU PHOTOGRAPHY</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.user-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.user-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(10px);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 68px;
}

.brand {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
  color: var(--text);
}

.brand-name {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 0.22em;
}

.brand-sub {
  margin-top: 5px;
  font-size: 9px;
  letter-spacing: 0.28em;
  color: var(--text-secondary);
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.nav-link {
  padding: 8px 14px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  transition: all 0.2s ease;
}

.nav-link:hover,
.nav-link.active {
  color: var(--brand-dark);
  background: var(--brand-soft);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff;
  color: var(--text);
  cursor: pointer;
  font-family: inherit;
}

.avatar-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.user-main {
  flex: 1;
}

.user-footer {
  border-top: 1px solid var(--border);
  background: #fff;
  padding: 22px 0;
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 720px) {
  .header-inner {
    gap: 12px;
    height: auto;
    padding: 12px 0;
    flex-wrap: wrap;
  }

  .main-nav {
    order: 3;
    flex-basis: 100%;
    gap: 4px;
  }

  .brand-name {
    font-size: 18px;
  }

  .user-name {
    display: none;
  }
}
</style>
