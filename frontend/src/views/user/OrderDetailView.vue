<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, deliverOrder, getOrderDetail } from '@/api/orders'
import { submitFeedback } from '@/api/feedback'
import type { OrderDetail } from '@/api/types'
import { formatDateTime, formatPrice } from '@/utils/format'
import { actionLabel, statusLabel } from '@/utils/order'
import OrderStatusTag from '@/components/OrderStatusTag.vue'

const route = useRoute()
const router = useRouter()

const detail = ref<OrderDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const actionLoading = ref('')
const feedbackVisible = ref(false)
const feedbackContent = ref('')
const feedbackSubmitting = ref(false)

const orderId = computed(() => Number(route.params.id))
const order = computed(() => detail.value?.order ?? null)
const feedbacks = computed(() => detail.value?.feedbacks ?? [])
const timeline = computed(() => detail.value?.timeline ?? [])
const lastTimelineIndex = computed(() => timeline.value.length - 1)

async function load(): Promise<void> {
  if (!Number.isFinite(orderId.value)) {
    loadError.value = '订单不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getOrderDetail(orderId.value)
  } catch {
    loadError.value = '订单不存在或无权查看'
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function handleCancel(): Promise<void> {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `确定取消订单 ${order.value.orderNo} 吗？取消后不可恢复。`,
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '保留订单', type: 'warning' },
    )
  } catch {
    return
  }
  actionLoading.value = 'cancel'
  try {
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

async function handleDeliver(): Promise<void> {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      '确认接收本轮全部精修照片吗？\n确认交付后订单结束，将无法再申请修改。',
      '确认交付',
      {
        confirmButtonText: '确认交付',
        cancelButtonText: '再看看',
        type: 'warning',
        customStyle: { whiteSpace: 'pre-line' },
      },
    )
  } catch {
    return
  }
  actionLoading.value = 'deliver'
  try {
    await deliverOrder(order.value.id)
    ElMessage.success('订单已交付，可下载原图')
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    actionLoading.value = ''
  }
}

function openFeedback(): void {
  feedbackContent.value = ''
  feedbackVisible.value = true
}

async function submitFeedbackAction(): Promise<void> {
  if (!order.value) return
  const content = feedbackContent.value.trim()
  if (!content) {
    ElMessage.warning('请填写修改意见')
    return
  }
  if (content.length > 500) {
    ElMessage.warning('修改意见不能超过 500 字')
    return
  }
  feedbackSubmitting.value = true
  try {
    await submitFeedback(order.value.id, content)
    ElMessage.success('修改意见已提交，等待摄影师重新上传')
    feedbackVisible.value = false
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    feedbackSubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="container">
      <button class="back-link" type="button" @click="router.push({ name: 'orders' })">
        ← 返回我的订单
      </button>

      <div v-loading="loading">
        <el-result
          v-if="loadError"
          icon="warning"
          title="无法查看订单"
          :sub-title="loadError"
        >
          <template #extra>
            <el-button type="primary" @click="router.push({ name: 'orders' })">返回订单列表</el-button>
          </template>
        </el-result>

        <template v-else-if="order">
          <section class="panel order-head">
            <div class="order-head__main">
              <div class="order-no-line">
                <span class="order-no">{{ order.orderNo }}</span>
                <OrderStatusTag :status="order.status" />
              </div>
              <h1>{{ order.themeName }} · {{ order.packageName }}</h1>
              <p class="muted">下单时间：{{ formatDateTime(order.createTime) }}</p>
            </div>
            <div class="order-head__actions">
              <template v-if="order.status === 'PENDING'">
                <el-button
                  type="danger"
                  plain
                  :loading="actionLoading === 'cancel'"
                  @click="handleCancel"
                >
                  取消订单
                </el-button>
              </template>
              <template v-if="order.status === 'UPLOADED'">
                <el-button
                  type="primary"
                  plain
                  @click="router.push({ name: 'order-photos', params: { id: order.id } })"
                >
                  查看照片
                </el-button>
                <el-button @click="openFeedback">申请修改</el-button>
                <el-button
                  type="primary"
                  :loading="actionLoading === 'deliver'"
                  @click="handleDeliver"
                >
                  确认交付
                </el-button>
              </template>
              <template v-if="order.status === 'MODIFYING' || order.status === 'DELIVERED'">
                <el-button
                  type="primary"
                  plain
                  @click="router.push({ name: 'order-photos', params: { id: order.id } })"
                >
                  {{ order.status === 'DELIVERED' ? '查看并下载照片' : '查看照片' }}
                </el-button>
              </template>
            </div>
          </section>

          <el-alert
            v-if="order.status === 'REJECTED' && order.rejectReason"
            :title="`订单被拒绝：${order.rejectReason}`"
            type="error"
            :closable="false"
            show-icon
            class="reject-alert"
          />

          <section class="panel">
            <h2 class="panel-title">订单信息</h2>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="订单状态">
                <OrderStatusTag :status="order.status" />
              </el-descriptions-item>
              <el-descriptions-item label="订单金额">
                <span class="price">¥{{ formatPrice(order.price) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="拍摄主题">{{ order.themeName }}</el-descriptions-item>
              <el-descriptions-item label="拍摄套餐">{{ order.packageName }}</el-descriptions-item>
              <el-descriptions-item label="拍摄日期">{{ order.shootDate }}</el-descriptions-item>
              <el-descriptions-item label="拍摄时段">{{ order.shootSlotText }}</el-descriptions-item>
              <el-descriptions-item label="交付时间">
                {{ formatDateTime(order.deliverTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="更新时间">
                {{ formatDateTime(order.updateTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                <span class="pre-wrap">{{ order.remark || '无' }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </section>

          <div class="detail-grid">
            <section class="panel">
              <h2 class="panel-title">
                状态时间线
                <span class="muted">{{ timeline.length }} 条记录</span>
              </h2>
              <el-timeline v-if="timeline.length > 0" class="timeline">
                <el-timeline-item
                  v-for="(log, index) in timeline"
                  :key="index"
                  :timestamp="formatDateTime(log.createTime)"
                  placement="top"
                  :type="index === lastTimelineIndex ? 'primary' : 'info'"
                >
                  <div class="timeline-title">{{ actionLabel(log.action) }}</div>
                  <div class="muted timeline-status">
                    {{ log.fromStatus ? `${statusLabel(log.fromStatus)} → ` : '' }}{{ statusLabel(log.toStatus) }}
                  </div>
                  <div v-if="log.remark" class="timeline-remark">{{ log.remark }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无状态记录" :image-size="70" />
            </section>

            <section class="panel">
              <h2 class="panel-title">
                历轮修改意见
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
              <el-empty v-else description="暂无修改意见" :image-size="70" />
            </section>
          </div>
        </template>
      </div>
    </div>

    <el-dialog v-model="feedbackVisible" title="申请修改" width="min(560px, 92vw)">
      <p class="muted dialog-hint">
        请说明需要调整的内容。提交后订单将进入「修改中」，摄影师重新上传后即可再次查看。
      </p>
      <el-input
        v-model="feedbackContent"
        type="textarea"
        :rows="6"
        maxlength="500"
        show-word-limit
        placeholder="例如：第 2 张肤色希望更自然一些；第 4 张请裁掉右侧路人。"
      />
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedbackAction">
          提交修改意见
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
  font-size: 23px;
}

.order-head p {
  margin: 0;
}

.order-head__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.reject-alert {
  margin-top: 20px;
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

.timeline-status {
  margin-top: 4px;
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

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .order-head {
    flex-direction: column;
  }

  .order-head__actions {
    justify-content: flex-start;
  }
}
</style>
