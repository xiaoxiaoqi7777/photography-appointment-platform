<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminGetOrderDetail,
  confirmOrder,
  rejectOrder,
  shootDone,
} from '@/api/orders'
import type { OrderDetail } from '@/api/types'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { formatDateTime, formatPrice } from '@/utils/format'
import { actionLabel, statusLabel } from '@/utils/order'

const route = useRoute()
const router = useRouter()

const detail = ref<OrderDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const actionLoading = ref('')
const currentAction = ref('')
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectError = ref('')

const orderId = computed(() => Number(route.params.id))
const order = computed(() => detail.value?.order ?? null)
const timeline = computed(() => detail.value?.timeline ?? [])
const feedbacks = computed(() => detail.value?.feedbacks ?? [])
const lastTimelineIndex = computed(() => timeline.value.length - 1)

async function load(): Promise<void> {
  if (!Number.isFinite(orderId.value)) {
    loadError.value = '订单不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await adminGetOrderDetail(orderId.value)
  } catch {
    loadError.value = '订单不存在或加载失败'
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function runAction(action: string, request: () => Promise<unknown>, successText: string): Promise<void> {
  actionLoading.value = action
  currentAction.value = action
  try {
    await request()
    ElMessage.success(successText)
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

async function handleConfirm(): Promise<void> {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `确认接受订单 ${order.value.orderNo} 吗？确认后将进入待拍摄状态。`,
      '确认订单',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  await runAction('confirm', () => confirmOrder(order.value!.id), '订单已确认')
}

function openReject(): void {
  rejectReason.value = ''
  rejectError.value = ''
  rejectVisible.value = true
}

async function submitReject(): Promise<void> {
  if (!order.value) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    rejectError.value = '拒绝原因不能为空'
    return
  }
  if (reason.length > 200) {
    rejectError.value = '拒绝原因最长 200 个字符'
    return
  }
  rejectVisible.value = false
  await runAction('reject', () => rejectOrder(order.value!.id, reason), '订单已拒绝')
}

async function handleShootDone(): Promise<void> {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      '确认线下拍摄已经完成吗？确认后将进入修图阶段，并可上传精修照片。',
      '标记拍摄完成',
      { confirmButtonText: '确认完成', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  await runAction('shoot-done', () => shootDone(order.value!.id), '已标记拍摄完成')
}

onMounted(load)
</script>

<template>
  <div>
    <button class="back-link" type="button" @click="router.push({ name: 'admin-orders' })">
      ← 返回订单列表
    </button>

    <div v-loading="loading">
      <el-result
        v-if="loadError"
        icon="warning"
        title="无法查看订单"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'admin-orders' })">返回列表</el-button>
        </template>
      </el-result>

      <template v-else-if="order">
        <div class="panel order-head">
          <div>
            <div class="order-no-line">
              <span class="order-no">{{ order.orderNo }}</span>
              <OrderStatusTag :status="order.status" />
            </div>
            <h1>{{ order.themeName }} · {{ order.packageName }}</h1>
            <p class="muted">
              用户 {{ order.username }} · 下单于 {{ formatDateTime(order.createTime) }}
            </p>
          </div>
          <div class="order-actions">
            <template v-if="order.status === 'PENDING'">
              <el-button
                type="primary"
                :loading="actionLoading === 'confirm' && currentAction === 'confirm'"
                @click="handleConfirm"
              >
                确认订单
              </el-button>
              <el-button type="danger" plain @click="openReject">拒绝订单</el-button>
            </template>
            <template v-if="order.status === 'CONFIRMED'">
              <el-button
                type="primary"
                :loading="actionLoading === 'shoot-done' && currentAction === 'shoot-done'"
                @click="handleShootDone"
              >
                标记拍摄完成
              </el-button>
            </template>
            <template v-if="order.status === 'SHOOTING_DONE' || order.status === 'MODIFYING'">
              <el-button
                type="primary"
                @click="router.push({ name: 'admin-order-upload', params: { id: order.id } })"
              >
                上传精修照片
              </el-button>
            </template>
          </div>
        </div>

        <el-alert
          v-if="order.status === 'REJECTED' && order.rejectReason"
          :title="`已拒绝：${order.rejectReason}`"
          type="error"
          :closable="false"
          show-icon
          class="reject-alert"
        />

        <section class="panel">
          <h2 class="panel-title">订单信息</h2>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="订单状态">
              <OrderStatusTag :status="order.status" />
            </el-descriptions-item>
            <el-descriptions-item label="用户">{{ order.username }}</el-descriptions-item>
            <el-descriptions-item label="金额">
              <span class="price">¥{{ formatPrice(order.price) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="主题">{{ order.themeName }}</el-descriptions-item>
            <el-descriptions-item label="套餐">{{ order.packageName }}</el-descriptions-item>
            <el-descriptions-item label="拍摄日期">{{ order.shootDate }}</el-descriptions-item>
            <el-descriptions-item label="拍摄时段">{{ order.shootSlotText }}</el-descriptions-item>
            <el-descriptions-item label="交付时间">{{ formatDateTime(order.deliverTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(order.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="用户备注" :span="3">
              <span class="pre-wrap">{{ order.remark || '无' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <div class="detail-grid">
          <section class="panel">
            <h2 class="panel-title">状态时间线</h2>
            <el-timeline v-if="timeline.length > 0" class="timeline">
              <el-timeline-item
                v-for="(log, index) in timeline"
                :key="index"
                :timestamp="formatDateTime(log.createTime)"
                placement="top"
                :type="index === lastTimelineIndex ? 'primary' : 'info'"
              >
                <div class="timeline-title">{{ actionLabel(log.action) }}</div>
                <div class="muted">
                  {{ log.fromStatus ? `${statusLabel(log.fromStatus)} → ` : '' }}{{ statusLabel(log.toStatus) }}
                </div>
                <div v-if="log.remark" class="timeline-remark">{{ log.remark }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无状态记录" :image-size="70" />
          </section>

          <section class="panel">
            <h2 class="panel-title">
              用户历轮反馈
              <span class="muted">共 {{ feedbacks.length }} 轮</span>
            </h2>
            <div v-if="feedbacks.length > 0" class="feedback-list">
              <article v-for="feedback in feedbacks" :key="feedback.id" class="feedback-item">
                <div class="feedback-head">
                  <el-tag type="warning" effect="plain" round>第 {{ feedback.round }} 轮</el-tag>
                  <span class="muted">对应第 {{ feedback.photoRound }} 批照片</span>
                  <span class="muted feedback-time">{{ formatDateTime(feedback.createTime) }}</span>
                </div>
                <p class="feedback-content pre-wrap">{{ feedback.content }}</p>
              </article>
            </div>
            <el-empty v-else description="用户暂未提交修改意见" :image-size="70" />
          </section>
        </div>
      </template>
    </div>

    <el-dialog v-model="rejectVisible" title="拒绝订单" width="min(520px, 92vw)">
      <p class="muted dialog-hint">拒绝原因将展示给用户，请填写清晰可理解的原因（≤200 字）。</p>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="例如：所选日期档期已满，请另选日期后重新预约。"
        @input="rejectError = ''"
      />
      <div v-if="rejectError" class="reject-error">{{ rejectError }}</div>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="actionLoading === 'reject' && currentAction === 'reject'"
          @click="submitReject"
        >
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.order-no-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.order-no {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--text-secondary);
}

.order-head h1 {
  margin: 0 0 8px;
  font-size: 22px;
}

.order-head p {
  margin: 0;
}

.order-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.reject-alert {
  margin-top: 18px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 20px;
  margin-top: 20px;
}

.timeline {
  padding-left: 4px;
}

.timeline-title {
  font-weight: 600;
  font-size: 14px;
}

.timeline-remark {
  margin-top: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #faf6f0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-item {
  padding: 14px 16px;
  background: #fffdf8;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.feedback-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.feedback-time {
  margin-left: auto;
}

.feedback-content {
  margin: 10px 0 0;
  line-height: 1.8;
}

.dialog-hint {
  margin: 0 0 12px;
  line-height: 1.7;
}

.reject-error {
  margin-top: 8px;
  color: var(--el-color-danger);
  font-size: 12px;
}

@media (max-width: 900px) {
  .order-head {
    flex-direction: column;
  }

  .order-actions {
    justify-content: flex-start;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
