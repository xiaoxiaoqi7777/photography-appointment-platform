<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { updateMe } from '@/api/users'
import type { UpdateMePayload } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()
const activeTab = ref('profile')

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const savingProfile = ref(false)
const savingPassword = ref(false)

const profileForm = reactive({
  nickname: '',
  phone: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const isAdmin = computed(() => auth.isAdmin)

const profileRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 50, message: '昵称最长 50 个字符', trigger: 'blur' },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

function validateNewPassword(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6) {
    callback(new Error('新密码长度至少 6 个字符'))
  } else {
    callback()
  }
}

function validateConfirm(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
}

function syncProfileForm(): void {
  profileForm.nickname = auth.user?.nickname ?? auth.user?.username ?? ''
  profileForm.phone = auth.user?.phone ?? ''
}

watch(
  () => auth.user,
  () => syncProfileForm(),
  { immediate: true },
)

async function saveProfile(): Promise<void> {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return

  const originalNickname = auth.user?.nickname ?? ''
  const originalPhone = auth.user?.phone ?? ''
  const nickname = profileForm.nickname.trim()
  const phone = profileForm.phone.trim()

  const payload: UpdateMePayload = {}
  if (nickname !== originalNickname) {
    payload.nickname = nickname
  }
  if (phone !== originalPhone) {
    if (!phone) {
      ElMessage.warning('手机号不能清空')
      return
    }
    payload.phone = phone
  }
  if (Object.keys(payload).length === 0) {
    ElMessage.info('没有需要保存的修改')
    return
  }

  savingProfile.value = true
  try {
    const updated = await updateMe(payload)
    auth.setUser(updated)
    ElMessage.success('个人资料已更新')
  } catch {
    // 拦截器已提示
  } finally {
    savingProfile.value = false
  }
}

async function savePassword(): Promise<void> {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return

  savingPassword.value = true
  try {
    const username = auth.user?.username ?? ''
    await updateMe({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    auth.logout()
    router.replace({ name: 'login', query: { username } })
  } catch {
    // 拦截器已提示
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="container profile-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">个人中心</h1>
          <p class="page-subtitle">查看账号资料、维护联系方式与登录密码</p>
        </div>
      </div>

      <div class="profile-grid">
        <aside class="panel profile-card">
          <div class="profile-avatar">{{ (auth.displayName || 'U').charAt(0) }}</div>
          <h2>{{ auth.displayName || auth.user?.username }}</h2>
          <el-tag :type="isAdmin ? 'warning' : 'success'" effect="light" round>
            {{ isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
          <dl class="profile-meta">
            <div>
              <dt>用户名</dt>
              <dd>{{ auth.user?.username ?? '-' }}</dd>
            </div>
            <div>
              <dt>账号状态</dt>
              <dd>{{ auth.user?.status === 1 ? '正常' : '已禁用' }}</dd>
            </div>
            <div>
              <dt>注册时间</dt>
              <dd>{{ formatDateTime(auth.user?.createTime) }}</dd>
            </div>
          </dl>
        </aside>

        <section class="panel profile-main">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="profile">
              <el-form
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                label-width="90px"
                class="profile-form"
              >
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" maxlength="50" placeholder="请输入昵称" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" maxlength="11" placeholder="选填" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingProfile" @click="saveProfile">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-alert
                title="修改密码成功后需要重新登录"
                type="info"
                :closable="false"
                show-icon
                class="password-alert"
              />
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="96px"
                class="profile-form"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    show-password
                    autocomplete="current-password"
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    show-password
                    autocomplete="new-password"
                    placeholder="至少 6 位"
                  />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    show-password
                    autocomplete="new-password"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingPassword" @click="savePassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

.profile-card {
  text-align: center;
}

.profile-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin: 4px auto 14px;
  border-radius: 50%;
  background: linear-gradient(135deg, #c9b18f, #8c6a43);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
}

.profile-card h2 {
  margin: 0 0 10px;
  font-size: 19px;
}

.profile-meta {
  margin: 22px 0 0;
  text-align: left;
  border-top: 1px solid var(--border);
}

.profile-meta div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 13px;
}

.profile-meta dt {
  color: var(--text-secondary);
}

.profile-meta dd {
  margin: 0;
  word-break: break-all;
}

.profile-main {
  min-height: 360px;
}

.profile-form {
  max-width: 460px;
  padding-top: 8px;
}

.password-alert {
  max-width: 520px;
  margin-bottom: 18px;
}

@media (max-width: 820px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
