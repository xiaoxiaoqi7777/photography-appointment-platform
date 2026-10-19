<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  adminListThemes,
  createTheme,
  updateTheme,
  updateThemeStatus,
} from '@/api/themes'
import type { Theme, ThemeSavePayload } from '@/api/types'
import { formatDateTime } from '@/utils/format'

interface ThemeForm {
  name: string
  coverUrl: string
  description: string
  tags: string[]
  sort: number
  status: number
}

const themes = ref<Theme[]>([])
const loading = ref(false)
const loadError = ref('')
const statusFilter = ref<'' | 0 | 1>('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<ThemeForm>({
  name: '',
  coverUrl: '',
  description: '',
  tags: [],
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入主题名称', trigger: 'blur' },
    { max: 50, message: '主题名称最长 50 个字符', trigger: 'blur' },
  ],
  coverUrl: [{ max: 255, message: '封面地址最长 255 个字符', trigger: 'blur' }],
  description: [{ max: 500, message: '主题描述最长 500 个字符', trigger: 'blur' }],
}

function emptyForm(): ThemeForm {
  return {
    name: '',
    coverUrl: '',
    description: '',
    tags: [],
    sort: 0,
    status: 1,
  }
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const result = await adminListThemes({
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    themes.value = result.list
    total.value = result.total
  } catch {
    loadError.value = '主题加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function search(): void {
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

function openCreate(): void {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: Theme): void {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    coverUrl: row.coverUrl ?? '',
    description: row.description ?? '',
    tags: [...(row.tags ?? [])],
    sort: row.sort ?? 0,
    status: row.status ?? 1,
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function save(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().then(() => true).catch(() => false)
  if (!valid) return

  const payload: ThemeSavePayload = {
    name: form.name.trim(),
    coverUrl: form.coverUrl.trim() || undefined,
    description: form.description.trim() || undefined,
    tags: form.tags,
    sort: form.sort,
    status: form.status,
  }

  saving.value = true
  try {
    if (editingId.value === null) {
      await createTheme(payload)
      ElMessage.success('主题已创建')
    } else {
      await updateTheme(editingId.value, payload)
      ElMessage.success('主题已更新')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Theme, value: string | number | boolean): Promise<void> {
  const target = value ? 1 : 0
  try {
    const updated = await updateThemeStatus(row.id, target)
    row.status = updated.status
    ElMessage.success(target === 1 ? `主题「${row.name}」已上架` : `主题「${row.name}」已下架`)
  } catch {
    // 失败时保持原状态
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">主题管理</h1>
        <p class="page-subtitle">维护拍摄主题；仅支持上下架，不提供删除</p>
      </div>
      <el-button type="primary" @click="openCreate">新增主题</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-select v-model="statusFilter" style="width: 150px" @change="search">
          <el-option label="全部状态" value="" />
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 个主题</span>
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

      <el-table v-loading="loading" :data="themes" stripe>
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" class="cover-thumb">
              <template #error>
                <div class="thumb-fallback">{{ row.name.charAt(0) }}</div>
              </template>
            </el-image>
            <div v-else class="thumb-fallback">{{ row.name.charAt(0) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="主题名称" min-width="150" />
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag
                v-for="tag in row.tags"
                :key="tag"
                size="small"
                effect="plain"
                type="info"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!row.tags || row.tags.length === 0" class="muted">无</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="上架"
              inactive-text="下架"
              inline-prompt
              @change="(value: string | number | boolean) => toggleStatus(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无主题" :image-size="80" />
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId === null ? '新增主题' : '编辑主题'"
      width="min(640px, 94vw)"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="主题名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如：城市写真" />
        </el-form-item>
        <el-form-item label="封面地址" prop="coverUrl">
          <el-input v-model="form.coverUrl" maxlength="255" placeholder="https://...（选填）" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            placeholder="输入标签后回车，可添加多个"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="主题描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="简要介绍主题风格与适合人群"
          />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="99999" />
          <span class="muted field-hint">数值越大越靠前</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.load-alert {
  margin-bottom: 14px;
}

.cover-thumb {
  width: 64px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
}

.thumb-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 48px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e7d9c6, #c9b18f);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.field-hint {
  margin-left: 12px;
}
</style>
