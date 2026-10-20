<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminListThemes } from '@/api/themes'
import {
  adminListPackages,
  createPackage,
  updatePackage,
  updatePackageStatus,
} from '@/api/packages'
import type { PackageSavePayload, PhotoPackage, Theme } from '@/api/types'
import CoverUploader from '@/components/CoverUploader.vue'
import { formatDateTime, formatPrice } from '@/utils/format'

interface PackageForm {
  themeId: number | null
  name: string
  price: number
  retouchCount: number
  duration: string
  includes: string
  coverUrl: string
  sort: number
  status: number
}

const packages = ref<PhotoPackage[]>([])
const themes = ref<Theme[]>([])
const loading = ref(false)
const loadError = ref('')
const themeFilter = ref<'' | number>('')
const statusFilter = ref<'' | 0 | 1>('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<PackageForm>({
  themeId: null,
  name: '',
  price: 0,
  retouchCount: 1,
  duration: '',
  includes: '',
  coverUrl: '',
  sort: 0,
  status: 1,
})

const themeNameMap = computed(() => new Map(themes.value.map((theme) => [theme.id, theme.name])))

function themeName(id: number): string {
  return themeNameMap.value.get(id) ?? `#${id}`
}

function validatePrice(_rule: unknown, value: number, callback: (error?: Error) => void): void {
  if (value === null || value === undefined || Number(value) <= 0) {
    callback(new Error('价格必须大于 0'))
  } else if (!/^\d+(\.\d{1,2})?$/.test(String(value))) {
    callback(new Error('价格最多保留两位小数'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  themeId: [{ required: true, message: '请选择关联主题', trigger: 'change' }],
  name: [
    { required: true, message: '请输入套餐名称', trigger: 'blur' },
    { max: 50, message: '套餐名称最长 50 个字符', trigger: 'blur' },
  ],
  price: [{ required: true, validator: validatePrice, trigger: 'change' }],
  retouchCount: [{ required: true, message: '精修张数至少为 1', trigger: 'change' }],
  duration: [{ max: 50, message: '拍摄时长最长 50 个字符', trigger: 'blur' }],
  includes: [{ max: 500, message: '包含内容最长 500 个字符', trigger: 'blur' }],
  coverUrl: [{ max: 255, message: '封面地址最长 255 个字符', trigger: 'blur' }],
}

async function loadThemeOptions(): Promise<void> {
  try {
    const all: Theme[] = []
    let current = 1
    for (;;) {
      const result = await adminListThemes({ page: current, pageSize: 100 })
      all.push(...result.list)
      if (all.length >= result.total || result.list.length === 0 || current >= 20) {
        break
      }
      current += 1
    }
    themes.value = all
  } catch {
    // 主题下拉加载失败不阻塞列表
  }
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const result = await adminListPackages({
      themeId: themeFilter.value === '' ? undefined : themeFilter.value,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    packages.value = result.list
    total.value = result.total
  } catch {
    loadError.value = '套餐加载失败，请稍后重试'
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

function emptyForm(): PackageForm {
  return {
    themeId: typeof themeFilter.value === 'number' ? themeFilter.value : null,
    name: '',
    price: 0,
    retouchCount: 1,
    duration: '',
    includes: '',
    coverUrl: '',
    sort: 0,
    status: 1,
  }
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: PhotoPackage): void {
  editingId.value = row.id
  Object.assign(form, {
    themeId: row.themeId,
    name: row.name,
    price: Number(row.price),
    retouchCount: row.retouchCount,
    duration: row.duration ?? '',
    includes: row.includes ?? '',
    coverUrl: row.coverUrl ?? '',
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
  if (form.themeId === null) return

  const payload: PackageSavePayload = {
    themeId: form.themeId,
    name: form.name.trim(),
    price: form.price,
    retouchCount: form.retouchCount,
    duration: form.duration.trim() || undefined,
    includes: form.includes.trim() || undefined,
    coverUrl: form.coverUrl.trim() || undefined,
    sort: form.sort,
    status: form.status,
  }

  saving.value = true
  try {
    if (editingId.value === null) {
      await createPackage(payload)
    } else {
      await updatePackage(editingId.value, payload)
    }
    const relatedTheme = themes.value.find((theme) => theme.id === form.themeId)
    if (relatedTheme && relatedTheme.status !== 1) {
      ElMessage.warning('套餐已保存，关联主题当前已下架')
    } else {
      ElMessage.success(editingId.value === null ? '套餐已创建' : '套餐已更新')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: PhotoPackage, value: string | number | boolean): Promise<void> {
  const target = value ? 1 : 0
  try {
    const updated = await updatePackageStatus(row.id, target)
    row.status = updated.status
    ElMessage.success(target === 1 ? `套餐「${row.name}」已上架` : `套餐「${row.name}」已下架`)
  } catch {
    // 失败时保持原状态
  }
}

onMounted(async () => {
  await Promise.all([loadThemeOptions(), load()])
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">套餐管理</h1>
        <p class="page-subtitle">维护主题下的拍摄套餐；仅支持上下架，不提供删除</p>
      </div>
      <el-button type="primary" @click="openCreate">新增套餐</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-select v-model="themeFilter" placeholder="全部主题" style="width: 190px" @change="search">
          <el-option label="全部主题" value="" />
          <el-option
            v-for="theme in themes"
            :key="theme.id"
            :label="theme.name"
            :value="theme.id"
          />
        </el-select>
        <el-select v-model="statusFilter" style="width: 150px" @change="search">
          <el-option label="全部状态" value="" />
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <span class="spacer"></span>
        <span class="muted">共 {{ total }} 个套餐</span>
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

      <el-table v-loading="loading" :data="packages" stripe>
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
        <el-table-column prop="name" label="套餐名称" min-width="150" />
        <el-table-column label="所属主题" min-width="130">
          <template #default="{ row }">{{ themeName(row.themeId) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="retouchCount" label="精修张数" width="100" />
        <el-table-column prop="duration" label="拍摄时长" min-width="110">
          <template #default="{ row }">{{ row.duration || '-' }}</template>
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
          <el-empty description="暂无套餐" :image-size="80" />
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
      :title="editingId === null ? '新增套餐' : '编辑套餐'"
      width="min(660px, 94vw)"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="关联主题" prop="themeId">
          <el-select v-model="form.themeId" placeholder="请选择主题" filterable style="width: 100%">
            <el-option
              v-for="theme in themes"
              :key="theme.id"
              :label="theme.status === 1 ? theme.name : `${theme.name}（已下架）`"
              :value="theme.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如：经典双人写真" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="价格" prop="price">
            <el-input-number
              v-model="form.price"
              :min="0.01"
              :max="99999999"
              :precision="2"
              :step="10"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="精修张数" prop="retouchCount">
            <el-input-number
              v-model="form.retouchCount"
              :min="1"
              :max="999"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="拍摄时长" prop="duration">
            <el-input v-model="form.duration" maxlength="50" placeholder="如：2 小时（选填）" />
          </el-form-item>
          <el-form-item label="排序值" prop="sort">
            <el-input-number v-model="form.sort" :min="0" :max="99999" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="包含内容" prop="includes">
          <el-input
            v-model="form.includes"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="如：服装 2 套、妆造 1 次、精修 10 张（选填）"
          />
        </el-form-item>
        <el-form-item label="封面图片" prop="coverUrl">
          <CoverUploader v-model="form.coverUrl" />
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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
