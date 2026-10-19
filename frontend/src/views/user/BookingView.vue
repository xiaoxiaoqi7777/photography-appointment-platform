<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getPackage } from '@/api/packages'
import { getTheme } from '@/api/themes'
import { createOrder } from '@/api/orders'
import type { PhotoPackage, ShootSlot, Theme } from '@/api/types'
import { formatPrice, queryString, todayStart } from '@/utils/format'
import { SHOOT_SLOT_OPTIONS } from '@/utils/order'

const route = useRoute()
const router = useRouter()

const pkg = ref<PhotoPackage | null>(null)
const theme = ref<Theme | null>(null)
const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const formRef = ref<FormInstance>()

const packageId = computed(() => Number(queryString(route.query.packageId)))

const form = reactive({
  shootDate: '',
  shootSlot: 'MORNING' as ShootSlot,
  remark: '',
})

const rules: FormRules = {
  shootDate: [{ required: true, message: '请选择拍摄日期', trigger: 'change' }],
  shootSlot: [{ required: true, message: '请选择拍摄时段', trigger: 'change' }],
  remark: [{ max: 200, message: '备注最长 200 个字符', trigger: 'blur' }],
}

function disabledDate(date: Date): boolean {
  return date.getTime() < todayStart().getTime()
}

async function load(): Promise<void> {
  if (!Number.isFinite(packageId.value) || packageId.value <= 0) {
    loadError.value = '缺少套餐信息，请从套餐详情页进入预约'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const data = await getPackage(packageId.value)
    pkg.value = data
    try {
      theme.value = await getTheme(data.themeId)
    } catch {
      theme.value = null
    }
  } catch {
    loadError.value = '套餐不存在或已下架'
    pkg.value = null
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  if (!pkg.value || !formRef.value) return
  const valid = await formRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return

  const slotLabel = SHOOT_SLOT_OPTIONS.find((item) => item.value === form.shootSlot)?.label ?? ''
  try {
    await ElMessageBox.confirm(
      `确认预约「${pkg.value.name}」？\n拍摄日期：${form.shootDate} ${slotLabel}`,
      '确认预约',
      {
        confirmButtonText: '确认预约',
        cancelButtonText: '再想想',
        type: 'info',
        customStyle: { whiteSpace: 'pre-line' },
      },
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const result = await createOrder({
      themeId: pkg.value.themeId,
      packageId: pkg.value.id,
      shootDate: form.shootDate,
      shootSlot: form.shootSlot,
      remark: form.remark.trim() || undefined,
    })
    ElMessage.success(`预约提交成功，订单号 ${result.orderNo}`)
    router.replace({ name: 'order-detail', params: { id: result.id } })
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="container" v-loading="loading">
      <button class="back-link" type="button" @click="router.back()">← 返回</button>

      <el-result
        v-if="loadError"
        icon="warning"
        title="无法发起预约"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'home' })">返回首页</el-button>
        </template>
      </el-result>

      <template v-else-if="pkg">
        <div class="page-header">
          <div>
            <h1 class="page-title">预约拍摄</h1>
            <p class="page-subtitle">填写拍摄信息，提交后等待工作人员确认</p>
          </div>
        </div>

        <div class="booking-layout">
          <section class="panel">
            <h2 class="panel-title">拍摄信息</h2>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="booking-form">
              <el-form-item label="拍摄主题">
                <router-link
                  v-if="theme"
                  :to="{ name: 'theme-detail', params: { id: theme.id } }"
                >
                  {{ theme.name }}
                </router-link>
                <span v-else>—</span>
              </el-form-item>
              <el-form-item label="拍摄套餐">
                <span>{{ pkg.name }}</span>
              </el-form-item>
              <el-form-item label="拍摄日期" prop="shootDate">
                <el-date-picker
                  v-model="form.shootDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择拍摄日期"
                  :disabled-date="disabledDate"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="拍摄时段" prop="shootSlot">
                <el-radio-group v-model="form.shootSlot">
                  <el-radio v-for="slot in SHOOT_SLOT_OPTIONS" :key="slot.value" :value="slot.value">
                    {{ slot.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="4"
                  maxlength="200"
                  show-word-limit
                  placeholder="可填写拍摄偏好、人数等信息（选填）"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" :loading="submitting" @click="submit">
                  提交预约
                </el-button>
                <el-button size="large" @click="router.back()">取消</el-button>
              </el-form-item>
            </el-form>
          </section>

          <aside class="panel summary-card">
            <h2 class="panel-title">预约摘要</h2>
            <div class="summary-theme">{{ theme?.name ?? '主题' }}</div>
            <div class="summary-name">{{ pkg.name }}</div>
            <div class="summary-row">
              <span>精修张数</span>
              <strong>{{ pkg.retouchCount }} 张</strong>
            </div>
            <div class="summary-row">
              <span>拍摄时长</span>
              <strong>{{ pkg.duration || '以门店沟通为准' }}</strong>
            </div>
            <div class="summary-row">
              <span>拍摄日期</span>
              <strong>{{ form.shootDate || '未选择' }}</strong>
            </div>
            <div class="summary-row">
              <span>拍摄时段</span>
              <strong>
                {{ SHOOT_SLOT_OPTIONS.find((item) => item.value === form.shootSlot)?.label }}
              </strong>
            </div>
            <div class="summary-total">
              <span>合计</span>
              <span class="price price-large">¥{{ formatPrice(pkg.price) }}</span>
            </div>
            <p class="muted tip">提交预约不产生支付，价格以下单时套餐价格为准。</p>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.booking-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  align-items: start;
}

.booking-form {
  max-width: 680px;
}

.summary-card {
  background: linear-gradient(180deg, #fffdf9, #f8f3ea);
}

.summary-theme {
  color: var(--text-secondary);
  font-size: 13px;
  letter-spacing: 0.08em;
}

.summary-name {
  margin: 6px 0 16px;
  font-size: 20px;
  font-weight: 600;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 13px;
}

.summary-row span {
  color: var(--text-secondary);
}

.summary-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 18px;
}

.tip {
  margin: 14px 0 0;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .booking-layout {
    grid-template-columns: 1fr;
  }

  .booking-form {
    max-width: none;
  }
}
</style>
