<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '@/api/orders'
import { downloadPhoto, listPhotos } from '@/api/photos'
import type { OrderDetail, PhotoItem } from '@/api/types'
import AuthImage from '@/components/AuthImage.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { saveBlobResponse } from '@/utils/download'
import { formatFileSize } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const detail = ref<OrderDetail | null>(null)
const photos = ref<PhotoItem[]>([])
const loading = ref(false)
const loadError = ref('')
const downloadingId = ref<number | null>(null)

const orderId = computed(() => Number(route.params.id))
const order = computed(() => detail.value?.order ?? null)
const delivered = computed(() => order.value?.status === 'DELIVERED')

const groups = computed(() => {
  const map = new Map<number, PhotoItem[]>()
  photos.value.forEach((photo) => {
    const list = map.get(photo.uploadRound) ?? []
    list.push(photo)
    map.set(photo.uploadRound, list)
  })
  return Array.from(map.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([round, items]) => ({ round, items }))
})

const emptyDescription = computed(() => {
  if (!order.value) return '暂无照片'
  if (order.value.status === 'DELIVERED') return '本轮订单暂无照片'
  return '摄影师还未上传照片，请耐心等待'
})

async function load(): Promise<void> {
  if (!Number.isFinite(orderId.value)) {
    loadError.value = '订单不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const [orderDetail, photoList] = await Promise.all([
      getOrderDetail(orderId.value),
      listPhotos(orderId.value),
    ])
    detail.value = orderDetail
    photos.value = photoList
  } catch {
    loadError.value = '订单不存在或无权查看'
    detail.value = null
    photos.value = []
  } finally {
    loading.value = false
  }
}

async function download(photo: PhotoItem): Promise<void> {
  downloadingId.value = photo.id
  try {
    const response = await downloadPhoto(orderId.value, photo.id)
    saveBlobResponse(response, photo.originalName)
    ElMessage.success('开始下载原图')
  } catch {
    // 拦截器已提示
  } finally {
    downloadingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="container" v-loading="loading">
      <button
        class="back-link"
        type="button"
        @click="router.push({ name: 'order-detail', params: { id: orderId } })"
      >
        ← 返回订单详情
      </button>

      <el-result
        v-if="loadError"
        icon="warning"
        title="无法查看照片"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'orders' })">返回订单列表</el-button>
        </template>
      </el-result>

      <template v-else>
        <div class="page-header">
          <div>
            <div class="title-line">
              <h1 class="page-title">照片预览</h1>
              <OrderStatusTag v-if="order" :status="order.status" />
            </div>
            <p class="page-subtitle">
              {{ order ? `${order.orderNo} · ${order.themeName} / ${order.packageName}` : '' }}
            </p>
          </div>
        </div>

        <el-alert
          v-if="delivered"
          title="订单已交付，可下载原图"
          description="原图下载接口需登录鉴权，下载文件名为拍摄时的原始文件名。"
          type="success"
          show-icon
          :closable="false"
          class="notice"
        />
        <el-alert
          v-else
          title="交付前仅展示带水印的预览图"
          description="确认交付后，本页将开放原图下载入口。"
          type="info"
          show-icon
          :closable="false"
          class="notice"
        />

        <div v-if="photos.length === 0" class="panel empty-block">
          <el-empty :description="emptyDescription" />
        </div>

        <section v-for="group in groups" :key="group.round" class="photo-group">
          <div class="group-head">
            <h2>第 {{ group.round }} 批精修</h2>
            <span class="muted">共 {{ group.items.length }} 张</span>
          </div>
          <div class="photo-grid">
            <article v-for="photo in group.items" :key="photo.id" class="photo-card">
              <div class="photo-frame">
                <AuthImage :src="photo.previewUrl" :alt="photo.originalName" />
              </div>
              <div class="photo-meta">
                <div class="photo-name" :title="photo.originalName">{{ photo.originalName }}</div>
                <div class="photo-sub muted">{{ formatFileSize(photo.fileSize) }}</div>
              </div>
              <div v-if="delivered" class="photo-actions">
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :loading="downloadingId === photo.id"
                  @click="download(photo)"
                >
                  下载原图
                </el-button>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.title-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice {
  margin-bottom: 20px;
}

.photo-group + .photo-group {
  margin-top: 28px;
}

.group-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.group-head h2 {
  margin: 0;
  font-size: 17px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.photo-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(74, 58, 40, 0.05);
}

.photo-frame {
  aspect-ratio: 4 / 3;
  background: #f1ece4;
  cursor: zoom-in;
}

.photo-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
}

.photo-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.photo-sub {
  flex-shrink: 0;
}

.photo-actions {
  padding: 0 12px 12px;
  text-align: right;
}
</style>
