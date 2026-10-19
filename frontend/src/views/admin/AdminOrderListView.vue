<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminListOrders } from '@/api/orders'
import type { Order, OrderStatus } from '@/api/types'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { formatDateTime, formatPrice } from '@/utils/format'
import { ORDER_STATUS_OPTIONS } from '@/utils/order'

const router = useRouter()

const orders = ref<Order[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref<'' | OrderStatus>('')
const keyword = ref('')
const loadError = ref('')

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const result = await adminListOrders({
      status: statusFilter.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    orders.value = result.list
    total.value = result.total
  } catch {
    loadError.value = '订单加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

function reset(): void {
  statusFilter.value = ''
  keyword.value = ''
  page.value = 1
  void load()
}

function changePage(target: number): void {
  page.value = target
  void load()
}

function changePageSize(size: number): void {
  pageSize.value = size
  page.value = 1
  void load()
}

function openOrder(order: Order): void {
  router.push({ name: 'admin-order-detail', params: { id: order.id } })
}

function canUpload(order: Order): boolean {
  return order.status === 'SHOOTING_DONE' || order.status === 'MODIFYING'
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">订单管理</h1>
        <p class="page-subtitle">处理用户预约、拍摄进度与照片上传</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="全部状态" value="" />
          <el-option
            v-for="option in ORDER_STATUS_OPTIONS"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 主题"
          clearable
          style="width: 240px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 条订单</span>
      </div>

      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
        class="load-alert"
      >
        <template #default>
          <el-button size="small" @click="load">重新加载</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="orders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="username" label="用户" min-width="110" />
        <el-table-column label="主题 / 套餐" min-width="170">
          <template #default="{ row }">
            <div>{{ row.themeName }}</div>
            <div class="muted">{{ row.packageName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="拍摄时间" min-width="150">
          <template #default="{ row }">
            <div>{{ row.shootDate }}</div>
            <div class="muted">{{ row.shootSlotText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <OrderStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openOrder(row)">查看处理</el-button>
            <el-button
              v-if="canUpload(row)"
              link
              type="primary"
              @click="router.push({ name: 'admin-order-upload', params: { id: row.id } })"
            >
              上传照片
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无符合条件的订单" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          :page-sizes="[10, 20, 50]"
          @current-change="changePage"
          @size-change="changePageSize"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.load-alert {
  margin-bottom: 14px;
}
</style>
