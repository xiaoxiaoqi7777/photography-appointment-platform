<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTheme } from '@/api/themes'
import { listPackagesByTheme } from '@/api/packages'
import type { PhotoPackage, Theme } from '@/api/types'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const theme = ref<Theme | null>(null)
const packages = ref<PhotoPackage[]>([])
const loading = ref(false)
const loadError = ref('')

const themeId = computed(() => Number(route.params.id))

async function load(): Promise<void> {
  if (!Number.isFinite(themeId.value)) {
    loadError.value = '主题不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const [themeData, packageList] = await Promise.all([
      getTheme(themeId.value),
      listPackagesByTheme(themeId.value),
    ])
    theme.value = themeData
    packages.value = packageList
  } catch {
    loadError.value = '主题不存在或已下架'
    theme.value = null
    packages.value = []
  } finally {
    loading.value = false
  }
}

function book(pkg: PhotoPackage): void {
  router.push({
    name: 'booking',
    query: { themeId: String(pkg.themeId), packageId: String(pkg.id) },
  })
}

onMounted(load)
watch(themeId, load)
</script>

<template>
  <div class="page">
    <div class="container" v-loading="loading">
      <button class="back-link" type="button" @click="router.push({ name: 'home' })">← 返回主题列表</button>

      <el-result
        v-if="loadError"
        icon="warning"
        title="无法查看该主题"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'home' })">返回首页</el-button>
        </template>
      </el-result>

      <template v-else-if="theme">
        <section class="theme-hero panel">
          <div class="theme-cover">
            <el-image v-if="theme.coverUrl" :src="theme.coverUrl" fit="cover" class="cover-image">
              <template #error>
                <div class="cover-fallback">{{ theme.name.charAt(0) }}</div>
              </template>
            </el-image>
            <div v-else class="cover-fallback">{{ theme.name.charAt(0) }}</div>
          </div>
          <div class="theme-info">
            <div class="theme-tags">
              <el-tag
                v-for="tag in theme.tags"
                :key="tag"
                size="small"
                effect="plain"
                type="info"
              >
                {{ tag }}
              </el-tag>
            </div>
            <h1>{{ theme.name }}</h1>
            <p class="theme-desc pre-wrap">{{ theme.description || '用镜头记录属于你的故事。' }}</p>
            <div class="theme-meta">
              <span>共 {{ packages.length }} 个套餐可选</span>
              <span class="dot">·</span>
              <span>支持在线预约与进度查询</span>
            </div>
          </div>
        </section>

        <section class="package-section">
          <div class="page-header">
            <div>
              <h2 class="page-title">拍摄套餐</h2>
              <p class="page-subtitle">选择适合你的拍摄方案，价格透明</p>
            </div>
          </div>

          <div v-if="packages.length === 0" class="panel empty-block">
            <el-empty description="该主题暂未上架套餐" />
          </div>

          <div v-else class="package-grid">
            <article v-for="pkg in packages" :key="pkg.id" class="package-card">
              <div class="package-cover">
                <el-image v-if="pkg.coverUrl" :src="pkg.coverUrl" fit="cover" class="cover-image">
                  <template #error>
                    <div class="cover-fallback package-cover-fallback">
                      {{ pkg.name.charAt(0) }}
                    </div>
                  </template>
                </el-image>
                <div v-else class="cover-fallback package-cover-fallback">
                  {{ pkg.name.charAt(0) }}
                </div>
              </div>
              <div class="package-head">
                <h3>{{ pkg.name }}</h3>
                <span class="price price-large">¥{{ formatPrice(pkg.price) }}</span>
              </div>
              <ul class="package-facts">
                <li>
                  <span class="fact-label">精修张数</span>
                  <span>{{ pkg.retouchCount }} 张</span>
                </li>
                <li>
                  <span class="fact-label">拍摄时长</span>
                  <span>{{ pkg.duration || '以门店沟通为准' }}</span>
                </li>
              </ul>
              <p class="package-includes pre-wrap">{{ pkg.includes || '具体包含内容请咨询门店。' }}</p>
              <div class="package-actions">
                <el-button @click="router.push({ name: 'package-detail', params: { id: pkg.id } })">
                  套餐详情
                </el-button>
                <el-button type="primary" @click="book(pkg)">立即预约</el-button>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.theme-hero {
  display: grid;
  grid-template-columns: minmax(260px, 420px) 1fr;
  gap: 32px;
  padding: 22px;
}

.theme-cover {
  aspect-ratio: 4 / 3;
  border-radius: 10px;
  overflow: hidden;
  background: #efe9e0;
}

.cover-image,
.cover-image :deep(img) {
  width: 100%;
  height: 100%;
}

.theme-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
}

.theme-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.theme-info h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 0.04em;
}

.theme-desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.9;
}

.theme-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.dot {
  color: var(--border);
}

.package-section {
  margin-top: 28px;
}

.package-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.package-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 0 22px 22px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow);
}

.package-cover {
  aspect-ratio: 16 / 10;
  margin: 0 -22px;
  overflow: hidden;
  background: #efe9e0;
}

.package-cover-fallback {
  font-size: 22px;
}

.package-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.package-head h3 {
  margin: 0;
  font-size: 18px;
}

.package-facts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin: 0;
  padding: 12px 14px;
  list-style: none;
  background: var(--brand-soft);
  border-radius: 10px;
  font-size: 13px;
}

.package-facts li {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fact-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.package-includes {
  flex: 1;
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
}

.package-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 820px) {
  .theme-hero {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .theme-info h1 {
    font-size: 24px;
  }
}
</style>
