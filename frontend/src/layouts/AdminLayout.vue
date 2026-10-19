<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/orders')) return '/admin/orders'
  if (route.path.startsWith('/admin/themes')) return '/admin/themes'
  if (route.path.startsWith('/admin/packages')) return '/admin/packages'
  return '/admin/orders'
})

const pageTitle = computed(() => (route.meta.title as string | undefined) ?? '管理端')

function handleCommand(command: string): void {
  if (command === 'profile') {
    router.push({ name: 'admin-profile' })
  } else if (command === 'front') {
    router.push({ name: 'home' })
  } else if (command === 'logout') {
    auth.logout()
    router.push({ name: 'login' })
  }
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-aside">
      <div class="admin-brand">
        <span class="admin-brand__name">解忧摄影</span>
        <span class="admin-brand__sub">管理端</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="admin-menu"
        background-color="#241f1a"
        text-color="#cfc4b6"
        active-text-color="#f0d9b5"
      >
        <el-menu-item index="/admin/orders">订单管理</el-menu-item>
        <el-menu-item index="/admin/themes">主题管理</el-menu-item>
        <el-menu-item index="/admin/packages">套餐管理</el-menu-item>
      </el-menu>
    </aside>

    <div class="admin-body">
      <header class="admin-header">
        <div class="admin-header__title">{{ pageTitle }}</div>
        <el-dropdown trigger="click" @command="handleCommand">
          <button class="admin-user" type="button">
            <span class="avatar-dot">{{ (auth.displayName || 'A').charAt(0) }}</span>
            <span>{{ auth.displayName || '管理员' }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="front">返回用户端</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: #f5f2ed;
}

.admin-aside {
  width: 220px;
  flex-shrink: 0;
  background: #241f1a;
  color: #cfc4b6;
}

.admin-brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 24px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-brand__name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: #f0e7db;
}

.admin-brand__sub {
  font-size: 11px;
  letter-spacing: 0.32em;
  color: #8f8375;
}

.admin-menu {
  border-right: none;
  padding-top: 10px;
}

.admin-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 12px;
  border-radius: 8px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: #3a3026;
}

.admin-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 62px;
  padding: 0 28px;
  background: #fff;
  border-bottom: 1px solid var(--border);
}

.admin-header__title {
  font-size: 17px;
  font-weight: 600;
}

.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 6px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
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
  font-weight: 600;
}

.admin-main {
  flex: 1;
  padding: 24px 28px 40px;
  min-width: 0;
}

@media (max-width: 900px) {
  .admin-shell {
    display: block;
  }

  .admin-aside {
    width: auto;
  }

  .admin-brand {
    padding: 16px 20px;
    flex-direction: row;
    align-items: baseline;
    gap: 10px;
  }

  .admin-menu {
    display: flex;
    padding: 0 8px 8px;
  }

  .admin-menu :deep(.el-menu-item) {
    flex: 1;
    margin: 4px;
    justify-content: center;
  }

  .admin-header {
    padding: 0 18px;
  }

  .admin-main {
    padding: 18px 14px 32px;
  }
}
</style>
