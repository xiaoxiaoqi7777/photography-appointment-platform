<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadCover } from '@/api/covers'

const MAX_FILE_SIZE = 5 * 1024 * 1024
const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png']

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const coverUrl = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})

function extensionOf(name: string): string {
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index + 1).toLowerCase() : ''
}

function pickFile(): void {
  fileInput.value?.click()
}

async function onFilePicked(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!ALLOWED_EXTENSIONS.includes(extensionOf(file.name))) {
    ElMessage.warning('仅支持 JPG / JPEG / PNG 格式')
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.warning('封面图片不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    const result = await uploadCover(file)
    emit('update:modelValue', result.url)
    ElMessage.success('封面图片已上传')
  } catch {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

function clearCover(): void {
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="cover-uploader">
    <div v-if="coverUrl" class="cover-preview">
      <el-image :src="coverUrl" fit="cover" class="cover-preview-image">
        <template #error>
          <div class="cover-preview-fallback">封面加载失败</div>
        </template>
      </el-image>
      <el-button link type="danger" class="cover-clear" @click="clearCover">移除</el-button>
    </div>
    <div v-else class="cover-empty">尚未设置封面</div>

    <div class="cover-actions">
      <input
        ref="fileInput"
        type="file"
        accept=".jpg,.jpeg,.png,image/jpeg,image/png"
        hidden
        @change="onFilePicked"
      />
      <el-button :loading="uploading" @click="pickFile">上传本地图片</el-button>
      <span class="muted">支持 JPG / JPEG / PNG，≤ 5MB</span>
    </div>

    <el-input
      v-model="coverUrl"
      maxlength="255"
      clearable
      placeholder="或直接填写图片地址，如 https://..."
      class="cover-url-input"
    />
  </div>
</template>

<style scoped>
.cover-uploader {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.cover-preview {
  position: relative;
  width: 180px;
  height: 116px;
  border-radius: 8px;
  overflow: hidden;
  background: #efe9e0;
}

.cover-preview-image {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-preview-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-secondary);
  font-size: 13px;
}

.cover-clear {
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 0 6px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180px;
  height: 116px;
  border: 1px dashed var(--border);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.cover-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.cover-url-input {
  max-width: 460px;
}
</style>
