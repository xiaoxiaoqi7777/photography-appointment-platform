<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '@/api/auth'
import { ApiError } from '@/api/http'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverError = ref('')

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
})

function validateConfirm(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度须为 3~20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
  nickname: [{ max: 50, message: '昵称最长 50 个字符', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return

  submitting.value = true
  serverError.value = ''
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined,
      phone: form.phone.trim() || undefined,
    })
    ElMessage.success('注册成功，请登录')
    router.replace({ name: 'login', query: { username: form.username.trim() } })
  } catch (error) {
    if (error instanceof ApiError && error.code === 40901) {
      serverError.value = '用户名已被占用，请更换后重试'
    } else if (error instanceof ApiError) {
      serverError.value = error.message
    }
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
        <h1>遇见<br />更好的自己</h1>
        <p>注册后即可预约主题拍摄、查看精修进度</p>
        <div class="auth-brand__dots"></div>
      </section>

      <section class="auth-form">
        <h2>创建账号</h2>
        <p class="auth-hint">带 * 为必填项</p>

        <el-alert
          v-if="serverError"
          :title="serverError"
          type="error"
          show-icon
          :closable="false"
          class="server-alert"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="3~20 个字符" size="large" autocomplete="username" />
          </el-form-item>
          <div class="form-row">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="至少 6 位"
                size="large"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="再次输入密码"
                size="large"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
          </div>
          <div class="form-row">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="选填" size="large" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="选填" size="large" />
            </el-form-item>
          </div>
          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="submitting"
            @click="handleSubmit"
          >
            注册
          </el-button>
        </el-form>

        <div class="auth-links">
          <span class="muted">已有账号？</span>
          <router-link :to="{ name: 'login' }">返回登录</router-link>
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
  width: min(940px, 100%);
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 30px 70px rgba(74, 58, 40, 0.16);
}

.auth-brand {
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
  padding: 44px 54px;
}

.auth-form h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
}

.auth-hint {
  margin: 8px 0 20px;
  color: var(--text-secondary);
  font-size: 13px;
}

.server-alert {
  margin-bottom: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
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

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
