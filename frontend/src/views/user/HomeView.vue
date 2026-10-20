<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listThemes } from '@/api/themes'
import type { Theme } from '@/api/types'

const themes = ref<Theme[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)
const loadError = ref('')

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const result = await listThemes({ page: page.value, pageSize })
    themes.value = result.list
    total.value = result.total
  } catch {
    loadError.value = '主题加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function changePage(target: number): void {
  page.value = target
  void load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function scrollToThemes(): void {
  document.getElementById('themes')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(load)
</script>

<template>
  <div>
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-copy">
          <span class="hero-eyebrow">JIEYOU PHOTOGRAPHY</span>
          <h1>在光与影之间<br />记录独一无二的你</h1>
          <p>精选主题、透明套餐、在线预约，拍摄进度与精修照片全程可查。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="scrollToThemes">浏览主题</el-button>
            <el-button size="large" plain @click="$router.push({ name: 'orders' })">我的订单</el-button>
          </div>
        </div>
        <div class="hero-art">
          <div class="hero-frame">
            <div class="hero-frame__inner">光 · 影 · 时 · 光</div>
          </div>
        </div>
      </div>
    </section>

    <section id="themes" class="page">
      <div class="container">
        <div class="page-header">
          <div>
            <h2 class="page-title">主题摄影</h2>
            <p class="page-subtitle">选择喜欢的主题，开启一次专属拍摄</p>
          </div>
          <span class="muted">共 {{ total }} 个主题</span>
        </div>

        <div v-loading="loading" class="theme-area">
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

          <div v-else-if="!loading && themes.length === 0" class="panel empty-block">
            <el-empty description="暂无上架主题，敬请期待">
              <el-button v-if="loadError" @click="load">刷新</el-button>
            </el-empty>
          </div>

          <div v-else class="theme-grid">
            <router-link
              v-for="theme in themes"
              :key="theme.id"
              :to="{ name: 'theme-detail', params: { id: theme.id } }"
              class="theme-card"
            >
              <div class="theme-cover">
                <el-image v-if="theme.coverUrl" :src="theme.coverUrl" fit="cover" class="cover-image">
                  <template #error>
                    <div class="cover-fallback">{{ theme.name.charAt(0) }}</div>
                  </template>
                </el-image>
                <div v-else class="cover-fallback">{{ theme.name.charAt(0) }}</div>
              </div>
              <div class="theme-body">
                <h3>{{ theme.name }}</h3>
                <p class="theme-desc">{{ theme.description || '用镜头记录属于你的故事。' }}</p>
                <div class="theme-tags">
                  <el-tag
                    v-for="tag in theme.tags.slice(0, 3)"
                    :key="tag"
                    size="small"
                    effect="plain"
                    type="info"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
              <div class="theme-footer">
                <span class="muted">查看套餐</span>
                <span class="arrow">→</span>
              </div>
            </router-link>
          </div>

          <div v-if="total > pageSize" class="pagination-row">
            <el-pagination
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="page"
              @current-change="changePage"
            />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  padding: 64px 0 72px;
  background:
    radial-gradient(circle at 78% 30%, rgba(140, 106, 67, 0.18), transparent 36%),
    linear-gradient(160deg, #f6f1e9, #efe7db);
  overflow: hidden;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 36px;
  align-items: center;
}

.hero-eyebrow {
  font-size: 11px;
  letter-spacing: 0.38em;
  color: var(--brand);
}

.hero-copy h1 {
  margin: 16px 0;
  font-size: clamp(30px, 4vw, 46px);
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.03em;
}

.hero-copy p {
  max-width: 460px;
  margin: 0 0 28px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-art {
  display: flex;
  justify-content: center;
}

.hero-frame {
  width: min(340px, 100%);
  aspect-ratio: 4 / 5;
  padding: 14px;
  border: 1px solid rgba(140, 106, 67, 0.35);
  border-radius: 160px 160px 18px 18px;
  background: rgba(255, 255, 255, 0.35);
}

.hero-frame__inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border-radius: 150px 150px 10px 10px;
  background: linear-gradient(150deg, #4a3b2c, #2a2119);
  color: #d9c4a5;
  font-size: 18px;
  letter-spacing: 0.5em;
  writing-mode: vertical-rl;
}

.theme-area {
  min-height: 200px;
}

.load-alert {
  margin-bottom: 16px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.theme-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  color: var(--text);
  box-shadow: 0 6px 20px rgba(74, 58, 40, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.theme-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 36px rgba(74, 58, 40, 0.12);
  color: var(--text);
}

.theme-cover {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #efe9e0;
}

/* 绝对定位避免图片原始比例撑高卡片，保证所有主题封面高度一致 */
.cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.cover-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.theme-body {
  flex: 1;
  padding: 16px 18px 10px;
}

.theme-body h3 {
  margin: 0 0 8px;
  font-size: 17px;
  letter-spacing: 0.03em;
}

.theme-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
  margin: 0 0 12px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.theme-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.theme-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px dashed var(--border);
  font-size: 13px;
}

.arrow {
  color: var(--brand);
  font-size: 16px;
}

@media (max-width: 820px) {
  .hero {
    padding: 40px 0 48px;
  }

  .hero-inner {
    grid-template-columns: 1fr;
  }

  .hero-art {
    display: none;
  }
}
</style>
