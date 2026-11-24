<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div>
        <h2>参数配置</h2>
        <p>集中维护订单超时、取餐提醒等业务参数，保存后即时生效。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新增参数
      </el-button>
    </div>

    <div class="table-panel">
      <el-table :data="configs" v-loading="loading" row-key="id">
        <el-table-column label="参数键" min-width="220">
          <template #default="{ row }">
            <code class="config-key">{{ row.configKey }}</code>
          </template>
        </el-table-column>

        <el-table-column label="参数值" min-width="200">
          <template #default="{ row }">
            <span class="config-value">{{ row.configValue || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="说明" min-width="220">
          <template #default="{ row }">
            <span class="config-remark">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="暂无参数配置" description="新增业务参数后，将在此统一管理。" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑参数' : '新增参数'"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="参数键" prop="configKey">
          <el-input
            v-model.trim="form.configKey"
            :disabled="editing"
            maxlength="60"
            placeholder="例如：order_timeout_minutes"
          />
          <span class="field-hint">建议使用小写字母与下划线，保存后不建议修改。</span>
        </el-form-item>

        <el-form-item label="参数值" prop="configValue">
          <el-input v-model.trim="form.configValue" maxlength="200" placeholder="请输入参数值" />
        </el-form-item>

        <el-form-item label="说明">
          <el-input
            v-model.trim="form.remark"
            type="textarea"
            :rows="2"
            maxlength="100"
            show-word-limit
            placeholder="说明该参数的业务含义"
          />
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
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getConfigs, saveConfig } from '@/api/admin'
import type { ConfigVO } from '@/types'

const configs = ref<ConfigVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  configKey: '',
  configValue: '',
  remark: ''
})

const rules: FormRules = {
  configKey: [
    { required: true, message: '请输入参数键', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,59}$/, message: '请使用小写字母、数字与下划线', trigger: 'blur' }
  ],
  configValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    configs.value = await getConfigs()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  form.configKey = ''
  form.configValue = ''
  form.remark = ''
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: ConfigVO) {
  editing.value = true
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.remark = row.remark || ''
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await saveConfig({ ...form })
    ElMessage.success(editing.value ? '参数已更新' : '参数已创建')
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    saving.value = false
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

.config-key {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 5px;
  background: #f5f1e8;
  color: #6a5845;
  font-family: 'Consolas', monospace;
  font-size: 10px;
}

.config-value {
  color: #36443b;
  font-size: 11px;
  font-weight: 600;
}

.config-remark {
  color: #8d948c;
  font-size: 10px;
}

.field-hint {
  display: block;
  margin-top: 4px;
  color: #a89e8e;
  font-size: 9px;
}

@media (max-width: 640px) {
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
