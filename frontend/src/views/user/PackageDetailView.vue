<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPackage } from '@/api/packages'
import { getTheme } from '@/api/themes'
import type { PhotoPackage, Theme } from '@/api/types'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const pkg = ref<PhotoPackage | null>(null)
const theme = ref<Theme | null>(null)
const loading = ref(false)
const loadError = ref('')

const packageId = computed(() => Number(route.params.id))

async function load(): Promise<void> {
  if (!Number.isFinite(packageId.value)) {
    loadError.value = '套餐不存在'
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

function book(): void {
  if (!pkg.value) return
  router.push({
    name: 'booking',
    query: { themeId: String(pkg.value.themeId), packageId: String(pkg.value.id) },
  })
}

onMounted(load)
watch(packageId, load)
</script>

<template>
  <div class="page">
    <div class="container" v-loading="loading">
      <button class="back-link" type="button" @click="router.back()">← 返回</button>

      <el-result
        v-if="loadError"
        icon="warning"
        title="无法查看该套餐"
        :sub-title="loadError"
      >
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'home' })">返回首页</el-button>
        </template>
      </el-result>

      <template v-else-if="pkg">
        <div class="package-layout">
          <section class="panel package-detail">
            <div class="package-head">
              <div>
                <div v-if="theme" class="theme-link">
                  <router-link :to="{ name: 'theme-detail', params: { id: theme.id } }">
                    {{ theme.name }}
                  </router-link>
                  <span class="muted">/ 套餐详情</span>
                </div>
                <h1>{{ pkg.name }}</h1>
              </div>
              <div class="price-block">
                <span class="price price-large">¥{{ formatPrice(pkg.price) }}</span>
                <span class="muted">套系价</span>
              </div>
            </div>

            <el-descriptions :column="2" border class="package-facts">
              <el-descriptions-item label="精修张数">{{ pkg.retouchCount }} 张</el-descriptions-item>
              <el-descriptions-item label="拍摄时长">{{ pkg.duration || '以门店沟通为准' }}</el-descriptions-item>
              <el-descriptions-item label="所属主题">
                <router-link
                  v-if="theme"
                  :to="{ name: 'theme-detail', params: { id: theme.id } }"
                >
                  {{ theme.name }}
                </router-link>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="套餐状态">
                <el-tag :type="pkg.status === 1 ? 'success' : 'info'" effect="light" round>
                  {{ pkg.status === 1 ? '上架中' : '已下架' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>

            <div class="section-block">
              <h2>套餐包含</h2>
              <p class="pre-wrap content-text">{{ pkg.includes || '具体包含内容请咨询门店。' }}</p>
            </div>

            <div v-if="pkg.coverUrl" class="cover-block">
              <el-image :src="pkg.coverUrl" fit="cover" class="cover-image" />
            </div>
          </section>

          <aside class="panel booking-card">
            <h2>预约拍摄</h2>
            <p class="muted">提交预约后，工作人员会尽快与你确认档期。</p>
            <div class="summary-row">
              <span>套餐</span>
              <strong>{{ pkg.name }}</strong>
            </div>
            <div class="summary-row">
              <span>精修</span>
              <strong>{{ pkg.retouchCount }} 张</strong>
            </div>
            <div class="summary-row">
              <span>合计</span>
              <strong class="price">¥{{ formatPrice(pkg.price) }}</strong>
            </div>
            <el-button type="primary" size="large" class="book-button" @click="book">
              立即预约
            </el-button>
            <p class="muted tip">价格以下单时的套餐价格为准</p>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.package-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  align-items: start;
}

.package-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.theme-link {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
}

.package-head h1 {
  margin: 0;
  font-size: 28px;
}

.price-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.package-facts {
  margin-top: 20px;
}

.section-block {
  margin-top: 24px;
}

.section-block h2 {
  margin: 0 0 10px;
  font-size: 16px;
}

.content-text {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.9;
}

.cover-block {
  margin-top: 22px;
  max-width: 520px;
  border-radius: 12px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  aspect-ratio: 4 / 3;
}

.booking-card h2 {
  margin: 0 0 8px;
  font-size: 18px;
}

.booking-card .muted {
  line-height: 1.7;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 14px;
}

.summary-row span {
  color: var(--text-secondary);
}

.book-button {
  width: 100%;
  margin-top: 20px;
}

.tip {
  margin: 12px 0 0;
  text-align: center;
}

@media (max-width: 900px) {
  .package-layout {
    grid-template-columns: 1fr;
  }
}
</style>
