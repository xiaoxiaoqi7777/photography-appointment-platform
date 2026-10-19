<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, listMyOrders } from '@/api/orders'
import type { Order, OrderStatus } from '@/api/types'
import { formatDateTime, formatPrice } from '@/utils/format'
import { ORDER_STATUS_OPTIONS } from '@/utils/order'
import OrderStatusTag from '@/components/OrderStatusTag.vue'

type TabValue = 'ALL' | OrderStatus

const router = useRouter()
const activeTab = ref<TabValue>('ALL')
const orders = ref<Order[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loadError = ref('')
const cancellingId = ref<number | null>(null)

const tabs: Array<{ value: TabValue; label: string }> = [
  { value: 'ALL', label: '全部' },
  ...ORDER_STATUS_OPTIONS.map((option) => ({ value: option.value, label: option.label })),
]

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const result = await listMyOrders({
      status: activeTab.value === 'ALL' ? undefined : activeTab.value,
      page: page.value,
      pageSize,
    })
    orders.value = result.list
    total.value = result.total
  } catch {
    loadError.value = '订单加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function handleTabChange(name: string | number): void {
  activeTab.value = name as TabValue
  page.value = 1
  void load()
}

function changePage(target: number): void {
  page.value = target
  void load()
}

async function cancel(order: Order): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定取消订单 ${order.orderNo} 吗？取消后不可恢复。`,
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '保留订单', type: 'warning' },
    )
  } catch {
    return
  }
  cancellingId.value = order.id
  try {
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    cancellingId.value = null
  }
}

function canShowPhotos(order: Order): boolean {
  return ['UPLOADED', 'MODIFYING', 'DELIVERED'].includes(order.status)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-header">
        <div>
          <h1 class="page-title">我的订单</h1>
          <p class="page-subtitle">查看预约进度、修改意见与精修照片</p>
        </div>
        <el-button type="primary" plain @click="router.push({ name: 'home' })">继续预约</el-button>
      </div>

      <div class="panel">
        <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
          <el-tab-pane
            v-for="tab in tabs"
            :key="tab.value"
            :label="tab.label"
            :name="tab.value"
          />
        </el-tabs>

        <div v-loading="loading" class="order-list">
          <el-alert
            v-if="loadError"
            :title="loadError"
            type="error"
            show-icon
            :closable="false"
            class="list-alert"
          >
            <template #default>
              <el-button size="small" @click="load">重新加载</el-button>
            </template>
          </el-alert>

          <el-empty
            v-else-if="!loading && orders.length === 0"
            description="暂无相关订单"
            class="empty-block"
          >
            <el-button type="primary" @click="router.push({ name: 'home' })">去挑选主题</el-button>
          </el-empty>

          <article v-for="order in orders" :key="order.id" class="order-card">
            <div class="order-card__head">
              <div>
                <span class="order-no">{{ order.orderNo }}</span>
                <span class="muted created-at">{{ formatDateTime(order.createTime) }}</span>
              </div>
              <OrderStatusTag :status="order.status" />
            </div>

            <div class="order-card__body">
              <div class="order-main">
                <h3>{{ order.themeName }} · {{ order.packageName }}</h3>
                <div class="order-facts">
                  <span>拍摄日期：{{ order.shootDate }}</span>
                  <span>{{ order.shootSlotText }}</span>
                </div>
                <p v-if="order.remark" class="order-remark muted">备注：{{ order.remark }}</p>
                <el-alert
                  v-if="order.status === 'REJECTED' && order.rejectReason"
                  :title="`拒绝原因：${order.rejectReason}`"
                  type="error"
                  :closable="false"
                  show-icon
                  class="reject-alert"
                />
              </div>
              <div class="order-side">
                <span class="price">¥{{ formatPrice(order.price) }}</span>
              </div>
            </div>

            <div class="order-card__actions">
              <el-button @click="router.push({ name: 'order-detail', params: { id: order.id } })">
                订单详情
              </el-button>
              <el-button
                v-if="canShowPhotos(order)"
                @click="router.push({ name: 'order-photos', params: { id: order.id } })"
              >
                查看照片
              </el-button>
              <el-button
                v-if="order.status === 'PENDING'"
                type="danger"
                plain
                :loading="cancellingId === order.id"
                @click="cancel(order)"
              >
                取消订单
              </el-button>
            </div>
          </article>

          <div v-if="total > pageSize" class="pagination-row">
            <el-pagination
              layout="total, prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="page"
              @current-change="changePage"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-list {
  min-height: 180px;
}

.list-alert {
  margin-bottom: 16px;
}

.order-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 18px;
  background: #fffdfa;
}

.order-card + .order-card {
  margin-top: 14px;
}

.order-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--border);
}

.order-no {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.created-at {
  margin-left: 12px;
}

.order-card__body {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 0;
}

.order-main {
  min-width: 0;
}

.order-main h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.order-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 13px;
}

.order-remark {
  margin: 8px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 620px;
}

.reject-alert {
  margin-top: 10px;
}

.order-side {
  flex-shrink: 0;
  font-size: 18px;
}

.order-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}

@media (max-width: 640px) {
  .order-card__body {
    flex-direction: column;
    gap: 8px;
  }

  .order-card__actions {
    flex-wrap: wrap;
  }

  .created-at {
    display: block;
    margin: 4px 0 0;
  }
}
</style>
