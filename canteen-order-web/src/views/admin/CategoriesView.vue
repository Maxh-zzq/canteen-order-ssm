<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div>
        <h2>菜品分类</h2>
        <p>分类用于学生端筛选与菜单组织，建议按主食、炒菜、饮品等划分。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新增分类
      </el-button>
    </div>

    <div class="table-panel">
      <el-table :data="categories" v-loading="loading" row-key="id">
        <el-table-column label="排序" width="80" align="center">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <el-table-column label="分类名称" min-width="200">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="cat-mark">{{ initialOf(row.name) }}</span>
              <strong>{{ row.name }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small" round>
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="暂无分类" description="新增分类后，菜品可按分类展示。" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="20" placeholder="例如：家常炒菜" />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="1" :max="99" controls-position="right" class="full" />
          <span class="field-hint">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="启用状态">
          <div class="status-line">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            <span>{{ form.status === 1 ? '启用' : '停用' }}</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { createCategory, deleteCategory, getAdminCategories, updateCategory } from '@/api/admin'
import type { CategoryVO } from '@/types'
import { initialOf } from '@/utils/format'

const categories = ref<CategoryVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  sort: 10,
  status: 1
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 20, message: '名称不超过 20 个字符', trigger: 'blur' }
  ],
  sort: [{ required: true, message: '请输入排序值', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    categories.value = await getAdminCategories()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sort = (categories.value.at(-1)?.sort || 0) + 1
  form.status = 1
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: CategoryVO) {
  editingId.value = row.id
  form.name = row.name
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { ...form })
      ElMessage.success('分类已更新')
    } else {
      await createCategory({ ...form })
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    saving.value = false
  }
}

async function remove(row: CategoryVO) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除分类', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    await load()
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(load)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-toolbar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.page-toolbar p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.table-panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 15px;
  overflow: hidden;
}

.table-panel :deep(.el-table__header th) {
  background: #fbf8f1;
  color: #647068;
  font-size: 11px;
  font-weight: 700;
}

.table-panel :deep(.el-table__row) {
  font-size: 12px;
}

.sort-badge {
  display: inline-grid;
  place-items: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 6px;
  background: #f5efe4;
  color: #8d6a3e;
  font-size: 10px;
  font-weight: 700;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.cat-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 15px;
  font-weight: 800;
}

.name-cell strong {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.full {
  width: 100%;
}

.field-hint {
  display: block;
  margin-top: 4px;
  color: #a89e8e;
  font-size: 9px;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #66736a;
  font-size: 11px;
}

@media (max-width: 640px) {
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
