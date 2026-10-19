<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { fetchPhotoBlob } from '@/api/photos'

const props = withDefaults(
  defineProps<{
    src: string
    alt?: string
    eager?: boolean
  }>(),
  { alt: '照片', eager: false },
)

const root = ref<HTMLElement | null>(null)
const objectUrl = ref('')
const loading = ref(false)
const failed = ref(false)
let requested = false
let observer: IntersectionObserver | null = null

function revoke(): void {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}

async function load(): Promise<void> {
  if (requested || !props.src) {
    return
  }
  requested = true
  loading.value = true
  failed.value = false
  try {
    const blob = await fetchPhotoBlob(props.src)
    objectUrl.value = URL.createObjectURL(blob)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function observe(): void {
  if (!('IntersectionObserver' in window) || !root.value) {
    void load()
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        observer?.disconnect()
        observer = null
        void load()
      }
    },
    { rootMargin: '240px' },
  )
  observer.observe(root.value)
}

onMounted(() => {
  if (props.eager) {
    void load()
  } else {
    observe()
  }
})

watch(
  () => props.src,
  () => {
    observer?.disconnect()
    observer = null
    requested = false
    revoke()
    failed.value = false
    if (props.eager) {
      void load()
    } else {
      observe()
    }
  },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  revoke()
})
</script>

<template>
  <div ref="root" class="auth-image">
    <el-image
      v-if="objectUrl"
      :src="objectUrl"
      :alt="alt"
      fit="cover"
      :preview-src-list="[objectUrl]"
      preview-teleported
      hide-on-click-modal
      class="auth-image__img"
    />
    <div v-else class="auth-image__state">
      {{ failed ? '预览加载失败' : loading ? '加载中…' : '待加载' }}
    </div>
  </div>
</template>

<style scoped>
.auth-image {
  width: 100%;
  height: 100%;
  background: #f1ece4;
}

.auth-image__img {
  width: 100%;
  height: 100%;
}

.auth-image__state {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 120px;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
