<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminGetOrderDetail } from '@/api/orders'
import { uploadPhotos } from '@/api/photos'
import type { FailedFile, OrderDetail, UploadResult } from '@/api/types'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { formatFileSize } from '@/utils/format'
import { statusLabel } from '@/utils/order'

const MAX_FILES = 20
const MAX_FILE_SIZE = 10 * 1024 * 1024
const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png']

const route = useRoute()
const router = useRouter()

const detail = ref<OrderDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const selectedFiles = ref<File[]>([])
const precheckFailures = ref<FailedFile[]>([])
const uploadResult = ref<UploadResult | null>(null)
const uploading = ref(false)
const progress = ref(0)
const fileInput = ref<HTMLInputElement | null>(null)

const orderId = computed(() => Number(route.params.id))
const order = computed(() => detail.value?.order ?? null)
const canUpload = computed(
  () => order.value?.status === 'SHOOTING_DONE' || order.value?.status === 'MODIFYING',
)

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

function pickFiles(): void {
  fileInput.value?.click()
}

function extensionOf(name: string): string {
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index + 1).toLowerCase() : ''
}

function onFilesPicked(event: Event): void {
  const input = event.target as HTMLInputElement
  const picked = Array.from(input.files ?? [])
  input.value = ''

  const merged = [...selectedFiles.value]
  const failures: FailedFile[] = []

  picked.forEach((file) => {
    if (!ALLOWED_EXTENSIONS.includes(extensionOf(file.name))) {
      failures.push({ name: file.name, reason: '仅支持 jpg / jpeg / png 格式' })
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      failures.push({ name: file.name, reason: '文件超过 10MB' })
      return
    }
    if (merged.some((item) => item.name === file.name && item.size === file.size)) {
      return
    }
    if (merged.length >= MAX_FILES) {
      failures.push({ name: file.name, reason: `单次最多上传 ${MAX_FILES} 张` })
      return
    }
    merged.push(file)
  })

  selectedFiles.value = merged
  if (failures.length > 0) {
    precheckFailures.value = [...precheckFailures.value, ...failures]
    ElMessage.warning(`${failures.length} 个文件未通过校验，请查看失败清单`)
  }
}

function removeFile(index: number): void {
  selectedFiles.value.splice(index, 1)
}

function clearPrecheckFailures(): void {
  precheckFailures.value = []
}

async function startUpload(): Promise<void> {
  if (!order.value) return
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择要上传的照片')
    return
  }

  uploading.value = true
  progress.value = 0
  try {
    const result = await uploadPhotos(order.value.id, selectedFiles.value, (percent) => {
      progress.value = percent
    })
    uploadResult.value = {
      success: result.success,
      failed: [...precheckFailures.value, ...result.failed],
    }
    precheckFailures.value = []

    if (result.success.length > 0) {
      ElMessage.success(`成功上传 ${result.success.length} 张照片`)
      selectedFiles.value = []
      await load()
    } else {
      ElMessage.warning('本次没有照片上传成功，请根据失败原因调整后重试')
    }
  } catch {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <button
      class="back-link"
      type="button"
      @click="router.push({ name: 'admin-order-detail', params: { id: orderId } })"
    >
      ← 返回订单详情
    </button>

    <div v-loading="loading">
      <el-result
        v-if="loadError"
        icon="warning"
        title="无法打开上传页"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'admin-orders' })">返回订单列表</el-button>
        </template>
      </el-result>

      <template v-else-if="order">
        <div class="page-header">
          <div>
            <div class="title-line">
              <h1 class="page-title">上传精修照片</h1>
              <OrderStatusTag :status="order.status" />
            </div>
            <p class="page-subtitle">
              {{ order.orderNo }} · {{ order.themeName }} / {{ order.packageName }} · 用户 {{ order.username }}
            </p>
          </div>
        </div>

        <section v-if="uploadResult" class="panel result-panel">
          <h2 class="panel-title">本次上传结果</h2>
          <div class="result-summary">
            <el-tag type="success" effect="light" round>成功 {{ uploadResult.success.length }} 张</el-tag>
            <el-tag type="danger" effect="light" round>失败 {{ uploadResult.failed.length }} 张</el-tag>
          </div>

          <div v-if="uploadResult.success.length > 0" class="result-block">
            <h3>成功清单</h3>
            <div class="success-list">
              <el-tag
                v-for="name in uploadResult.success"
                :key="name"
                type="success"
                effect="plain"
              >
                {{ name }}
              </el-tag>
            </div>
            <el-alert
              v-if="!canUpload"
              title="照片已上传，订单进入待反馈状态"
              description="未成功的文件需等待用户提交修改意见、订单进入「修改中」后重新上传。"
              type="info"
              show-icon
              :closable="false"
              class="result-note"
            />
          </div>

          <div v-if="uploadResult.failed.length > 0" class="result-block">
            <h3>失败清单</h3>
            <el-table :data="uploadResult.failed" size="small" border>
              <el-table-column prop="name" label="文件名" min-width="200" />
              <el-table-column prop="reason" label="失败原因" min-width="220" />
            </el-table>
          </div>

          <div class="result-actions">
            <el-button
              type="primary"
              @click="router.push({ name: 'admin-order-detail', params: { id: order.id } })"
            >
              返回订单详情
            </el-button>
          </div>
        </section>

        <section v-if="canUpload" class="panel">
          <h2 class="panel-title">选择照片</h2>
          <div class="upload-tips">
            <el-tag effect="plain" type="info">单次最多 {{ MAX_FILES }} 张</el-tag>
            <el-tag effect="plain" type="info">仅支持 jpg / jpeg / png</el-tag>
            <el-tag effect="plain" type="info">单个文件 ≤ 10MB</el-tag>
          </div>

          <input
            ref="fileInput"
            type="file"
            accept=".jpg,.jpeg,.png,image/jpeg,image/png"
            multiple
            hidden
            @change="onFilesPicked"
          />

          <div class="picker-row">
            <el-button type="primary" plain @click="pickFiles">选择照片</el-button>
            <span class="muted">已选择 {{ selectedFiles.length }} / {{ MAX_FILES }} 张</span>
            <el-button
              v-if="precheckFailures.length > 0"
              link
              @click="clearPrecheckFailures"
            >
              清空未通过校验清单
            </el-button>
          </div>

          <div v-if="precheckFailures.length > 0" class="precheck-block">
            <el-alert
              :title="`${precheckFailures.length} 个文件未通过前端校验，不会上传`"
              type="warning"
              show-icon
              :closable="false"
            />
            <el-table :data="precheckFailures" size="small" border class="precheck-table">
              <el-table-column prop="name" label="文件名" min-width="200" />
              <el-table-column prop="reason" label="原因" min-width="220" />
            </el-table>
          </div>

          <el-table v-if="selectedFiles.length > 0" :data="selectedFiles" size="small" border>
            <el-table-column label="文件名" min-width="220">
              <template #default="{ row }">{{ row.name }}</template>
            </el-table-column>
            <el-table-column label="大小" width="120">
              <template #default="{ row }">{{ formatFileSize(row.size) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeFile($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else class="empty-selection muted">
            还没有选择照片。选择后点击「开始上传」。
          </div>

          <el-progress
            v-if="uploading"
            :percentage="progress"
            :stroke-width="10"
            class="progress"
          />

          <div class="upload-actions">
            <el-button
              type="primary"
              size="large"
              :loading="uploading"
              :disabled="selectedFiles.length === 0"
              @click="startUpload"
            >
              开始上传
            </el-button>
          </div>
        </section>

        <el-result
          v-if="!canUpload && !uploadResult"
          icon="info"
          title="当前订单状态不可上传照片"
          :sub-title="`订单当前状态为「${statusLabel(order.status)}」，仅拍摄完成或修改中的订单可上传精修照片。`"
        >
          <template #extra>
            <el-button type="primary" @click="router.push({ name: 'admin-order-detail', params: { id: order.id } })">
              返回订单详情
            </el-button>
          </template>
        </el-result>
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

.result-panel {
  border-color: #cfe3d0;
}

.result-summary {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.result-block + .result-block {
  margin-top: 20px;
}

.result-block h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 600;
}

.success-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.result-note {
  margin-top: 14px;
}

.result-actions {
  margin-top: 22px;
}

.upload-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.picker-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.precheck-block {
  margin-bottom: 18px;
}

.precheck-table {
  margin-top: 10px;
}

.empty-selection {
  padding: 26px 0;
  text-align: center;
  border: 1px dashed var(--border);
  border-radius: 10px;
}

.progress {
  margin-top: 18px;
}

.upload-actions {
  margin-top: 20px;
}
</style>
