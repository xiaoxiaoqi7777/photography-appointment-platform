<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { queryString } from '@/utils/format'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  username: queryString(route.query.username),
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const result = await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    if (result.role === 'ADMIN') {
      router.replace({ name: 'admin-orders' })
      return
    }
    const redirect = queryString(route.query.redirect)
    const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('/admin') ? redirect : '/'
    router.replace(safeRedirect)
  } catch {
    // 统一错误提示由 axios 拦截器负责
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <section class="auth-brand">
        <div class="auth-brand__mark">解忧摄影</div>
        <h1>把时光<br />留在光里</h1>
        <p>主题摄影预约 · 精修交付 · 全程可追溯</p>
        <div class="auth-brand__dots"></div>
      </section>

      <section class="auth-form">
        <h2>登录</h2>
        <p class="auth-hint">欢迎回来，请登录你的账号</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="submitting"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form>

        <div class="auth-links">
          <span class="muted">还没有账号？</span>
          <router-link :to="{ name: 'register' }">立即注册</router-link>
          <router-link :to="{ name: 'home' }" class="back-home">返回首页</router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px 16px;
  background:
    radial-gradient(circle at 12% 18%, rgba(140, 106, 67, 0.16), transparent 34%),
    radial-gradient(circle at 88% 82%, rgba(140, 106, 67, 0.12), transparent 30%),
    #f4efe7;
}

.auth-card {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  width: min(900px, 100%);
  min-height: 520px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 30px 70px rgba(74, 58, 40, 0.16);
}

.auth-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  padding: 48px 42px;
  background: linear-gradient(150deg, #3a2f24, #241d16);
  color: #f1e7d9;
  overflow: hidden;
}

.auth-brand__mark {
  font-size: 14px;
  letter-spacing: 0.38em;
  color: #cbb08b;
}

.auth-brand h1 {
  margin: 0;
  font-size: 40px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: 0.04em;
}

.auth-brand p {
  margin: 0;
  color: #b4a48f;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.auth-brand__dots {
  margin-top: 26px;
  width: 120px;
  height: 4px;
  background-image: radial-gradient(circle, #b08a5a 2px, transparent 2.5px);
  background-size: 16px 4px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 54px;
}

.auth-form h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
}

.auth-hint {
  margin: 8px 0 26px;
  color: var(--text-secondary);
  font-size: 13px;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
  letter-spacing: 0.2em;
}

.auth-links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 13px;
}

.back-home {
  margin-left: auto;
  color: var(--text-secondary);
}

@media (max-width: 760px) {
  .auth-card {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-brand {
    padding: 30px 28px;
  }

  .auth-brand h1 {
    font-size: 28px;
  }

  .auth-brand p,
  .auth-brand__dots {
    display: none;
  }

  .auth-form {
    padding: 32px 26px 36px;
  }
}
</style>
