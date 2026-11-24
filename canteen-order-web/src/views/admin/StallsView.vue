<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div>
        <h2>档口管理</h2>
        <p>维护食堂档口的位置、营业时间与启用状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新增档口
      </el-button>
    </div>

    <div class="stall-grid">
      <article v-for="stall in stalls" :key="stall.id" class="stall-card">
        <div class="stall-top">
          <div class="stall-mark"><el-icon><Shop /></el-icon></div>
          <el-tag :type="stall.status === 1 ? 'success' : 'info'" effect="light" size="small" round>
            {{ stall.status === 1 ? '营业中' : '已停用' }}
          </el-tag>
        </div>

        <h3>{{ stall.name }}</h3>
        <p class="stall-place">
          <el-icon><Location /></el-icon>
          {{ stall.floor || '未设置楼层' }} · {{ stall.windowNo || '未设置窗口' }}
        </p>

        <div class="stall-time">
          <el-icon><Clock /></el-icon>
          {{ stall.openTime || '—' }} — {{ stall.closeTime || '—' }}
        </div>

        <div class="stall-actions">
          <el-button text type="primary" size="small" @click="openEdit(stall)">编辑</el-button>
          <el-button text type="danger" size="small" @click="remove(stall)">删除</el-button>
        </div>
      </article>

      <button type="button" class="add-card" @click="openCreate">
        <el-icon><Plus /></el-icon>
        <span>新增档口</span>
      </button>
    </div>

    <el-empty v-if="!loading && stalls.length === 0" description="暂无档口，请先新增档口" />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑档口' : '新增档口'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="档口名称" prop="name">
          <el-input v-model.trim="form.name" maxlength="20" placeholder="例如：一食堂 · 家常菜" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="所在楼层" prop="floor">
            <el-input v-model.trim="form.floor" maxlength="20" placeholder="例如：一楼" />
          </el-form-item>
          <el-form-item label="窗口号" prop="windowNo">
            <el-input v-model.trim="form.windowNo" maxlength="10" placeholder="例如：A03" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="开始营业" prop="openTime">
            <el-time-picker
              v-model="form.openTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="选择时间"
              class="full"
            />
          </el-form-item>
          <el-form-item label="结束营业" prop="closeTime">
            <el-time-picker
              v-model="form.closeTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="选择时间"
              class="full"
            />
          </el-form-item>
        </div>

        <el-form-item label="营业状态">
          <div class="status-line">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            <span>{{ form.status === 1 ? '营业中' : '已停用' }}</span>
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
import { Clock, Location, Plus, Shop } from '@element-plus/icons-vue'
import { createStall, deleteStall, getAdminStalls, updateStall } from '@/api/admin'
import type { StallVO } from '@/types'

const stalls = ref<StallVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  floor: '',
  windowNo: '',
  openTime: '07:00:00',
  closeTime: '20:00:00',
  status: 1
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入档口名称', trigger: 'blur' },
    { min: 2, max: 20, message: '名称长度为 2-20 个字符', trigger: 'blur' }
  ],
  openTime: [{ required: true, message: '请选择开始营业时间', trigger: 'change' }],
  closeTime: [{ required: true, message: '请选择结束营业时间', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    stalls.value = await getAdminStalls()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.floor = ''
  form.windowNo = ''
  form.openTime = '07:00:00'
  form.closeTime = '20:00:00'
  form.status = 1
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: StallVO) {
  editingId.value = row.id
  form.name = row.name
  form.floor = row.floor
  form.windowNo = row.windowNo
  form.openTime = row.openTime || '07:00:00'
  form.closeTime = row.closeTime || '20:00:00'
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
      await updateStall(editingId.value, { ...form })
      ElMessage.success('档口已更新')
    } else {
      await createStall({ ...form })
      ElMessage.success('档口已创建')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    saving.value = false
  }
}

async function remove(row: StallVO) {
  try {
    await ElMessageBox.confirm(`确定删除档口「${row.name}」吗？`, '删除档口', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteStall(row.id)
    ElMessage.success('档口已删除')
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

.stall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 14px;
}

.stall-card {
  padding: 18px;
  border-radius: 15px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 6px 18px rgba(47, 34, 18, 0.05);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.stall-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(47, 34, 18, 0.09);
}

.stall-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stall-mark {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: #edf4ef;
  color: #2f7550;
  font-size: 18px;
}

.stall-card h3 {
  margin: 13px 0 6px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.stall-place,
.stall-time {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 5px;
  color: #8d948c;
  font-size: 10px;
}

.stall-time {
  color: #6f7a72;
}

.stall-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #f0e8dc;
}

.add-card {
  min-height: 176px;
  border: 1px dashed #d9cbb8;
  border-radius: 15px;
  background: #fcfaf6;
  color: #a19480;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease;
}

.add-card:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.add-card .el-icon {
  font-size: 22px;
}

.add-card span {
  font-size: 12px;
  font-weight: 650;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.full {
  width: 100%;
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

  .stall-grid {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
