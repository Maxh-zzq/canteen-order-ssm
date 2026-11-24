<template>
  <div class="admin-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="搜索姓名 / 账号 / 手机号"
          :prefix-icon="Search"
          class="search-input"
          @keyup.enter="load(1)"
          @clear="load(1)"
        />
        <el-button @click="load(1)">查询</el-button>
      </div>

      <div class="toolbar-note">
        <el-icon><User /></el-icon>
        共 {{ total }} 个账号
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="users" v-loading="loading" row-key="id">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar" :class="roleTone(row.roleCode)">{{ initialOf(row.realName || row.username) }}</span>
              <div>
                <strong>{{ row.realName || '未填写姓名' }}</strong>
                <small>{{ row.username }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="手机号" width="150">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>

        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.roleCode)" effect="light" size="small" round>
              {{ row.roleName || roleNameOf(row.roleCode) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="余额" width="120" align="right">
          <template #default="{ row }">
            <strong class="balance">{{ money(row.balance) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status-switch">
              <el-switch
                :model-value="row.status === 1"
                :loading="switchingId === row.id"
                :disabled="row.username === 'admin'"
                @change="(value: string | number | boolean) => changeStatus(row, Boolean(value))"
              />
              <span>{{ row.status === 1 ? '启用' : '禁用' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.roleCode === 'STAFF'"
              link
              type="primary"
              size="small"
              @click="openStallDialog(row)"
            >
              档口权限
            </el-button>
            <span v-else class="muted-action">—</span>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="暂无用户" description="没有匹配的账号记录。" />
        </template>
      </el-table>

      <div class="table-footer">
        <span>共 {{ total }} 个账号</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog
      v-model="stallDialogVisible"
      :title="stallDialogTitle"
      width="520px"
      class="stall-dialog"
      :close-on-click-modal="false"
    >
      <div v-loading="stallLoading" class="stall-dialog-body">
        <p class="stall-tip">
          员工只能查看并操作已勾选档口的订单。未勾选任何档口时，该员工无法进入出餐业务。
        </p>

        <el-checkbox-group v-if="allStalls.length" v-model="selectedStallIds" class="stall-grid">
          <el-checkbox
            v-for="stall in allStalls"
            :key="stall.id"
            :value="stall.id"
            border
            class="stall-option"
          >
            <span class="stall-name">{{ stall.name }}</span>
            <small>{{ stall.floor }} {{ stall.windowNo }}</small>
          </el-checkbox>
        </el-checkbox-group>

        <EmptyState
          v-else-if="!stallLoading"
          title="暂无可分配档口"
          description="请先在档口管理中新增档口。"
        />
      </div>

      <template #footer>
        <el-button @click="stallDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStalls" @click="saveStalls">
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getAdminStalls,
  getUserStalls,
  getUsers,
  updateUserStalls,
  updateUserStatus
} from '@/api/admin'
import type { Role, StallVO, UserVO } from '@/types'
import { ROLE_MAP, initialOf, money } from '@/utils/format'

const users = ref<UserVO[]>([])
const keyword = ref('')
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const switchingId = ref<string | null>(null)
const stallDialogVisible = ref(false)
const stallTarget = ref<UserVO | null>(null)
const allStalls = ref<StallVO[]>([])
const selectedStallIds = ref<number[]>([])
const stallLoading = ref(false)
const savingStalls = ref(false)

const stallDialogTitle = computed(() => {
  const name = stallTarget.value?.realName || stallTarget.value?.username || ''
  return name ? `档口权限 · ${name}` : '档口权限'
})

function roleTagType(role: Role): 'danger' | 'warning' | 'success' {  if (role === 'ADMIN') return 'danger'
  if (role === 'STAFF') return 'warning'
  return 'success'
}

function roleTone(role: Role) {
  if (role === 'ADMIN') return 'admin'
  if (role === 'STAFF') return 'staff'
  return 'student'
}

function roleNameOf(role: string) {
  return ROLE_MAP[role as Role] || role
}

async function load(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getUsers({
      keyword: keyword.value || undefined,
      pageNum: page,
      pageSize
    })
    users.value = data.list
    total.value = data.total
    pageNum.value = page
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

async function changeStatus(row: UserVO, enabled: boolean) {
  const nextStatus = enabled ? 1 : 0
  if (nextStatus === row.status) return

  try {
    await ElMessageBox.confirm(
      `确定${enabled ? '启用' : '禁用'}账号「${row.username}」吗？`,
      '账号状态',
      {
        confirmButtonText: enabled ? '启用' : '禁用',
        cancelButtonText: '取消',
        type: enabled ? 'info' : 'warning'
      }
    )
  } catch {
    return
  }

  switchingId.value = row.id
  try {
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success(enabled ? '账号已启用' : '账号已禁用')
    await load(pageNum.value)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    switchingId.value = null
  }
}

async function openStallDialog(row: UserVO) {
  if (row.roleCode !== 'STAFF') return
  stallTarget.value = row
  selectedStallIds.value = []
  stallDialogVisible.value = true
  stallLoading.value = true
  try {
    const [stalls, boundIds] = await Promise.all([getAdminStalls(), getUserStalls(row.id)])
    allStalls.value = stalls
    selectedStallIds.value = boundIds
  } catch {
    stallDialogVisible.value = false
  } finally {
    stallLoading.value = false
  }
}

async function saveStalls() {
  if (!stallTarget.value) return
  savingStalls.value = true
  try {
    await updateUserStalls(stallTarget.value.id, selectedStallIds.value)
    ElMessage.success(`已更新「${stallTarget.value.realName || stallTarget.value.username}」的档口权限`)
    stallDialogVisible.value = false
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    savingStalls.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 15px;
  box-shadow: 0 6px 18px rgba(47, 34, 18, 0.04);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 250px;
}

.toolbar-note {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #9a9285;
  font-size: 10px;
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.avatar.student {
  background: linear-gradient(145deg, #4f9a70, #2f7550);
}

.avatar.staff {
  background: linear-gradient(145deg, #d99a4e, #b5731e);
}

.avatar.admin {
  background: linear-gradient(145deg, #6e7d8f, #465564);
}

.user-cell strong {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.user-cell small {
  display: block;
  margin-top: 2px;
  color: #a0988b;
  font-size: 9px;
}

.balance {
  color: #3b6d50;
  font-size: 12px;
  font-weight: 750;
  font-variant-numeric: tabular-nums;
}

.status-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #7d887f;
  font-size: 10px;
}

.muted-action {
  color: #b8b1a5;
  font-size: 10px;
}

.stall-dialog-body {
  min-height: 120px;
}

.stall-tip {
  margin: 0 0 14px;
  padding: 10px 12px;
  background: #fbf8f1;
  border-left: 3px solid #d99a4e;
  border-radius: 8px;
  color: #837766;
  font-size: 11px;
  line-height: 1.7;
}

.stall-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.stall-option {
  margin: 0 !important;
  width: 100%;
  height: auto !important;
  padding: 9px 12px !important;
  border-radius: 9px;
}

.stall-option :deep(.el-checkbox__label) {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  overflow: hidden;
  white-space: nowrap;
}

.stall-name {
  color: var(--ink);
  font-size: 11px;
  font-weight: 700;
}

.stall-option small {
  color: #a0988b;
  font-size: 9px;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #f5efe5;
}

.table-footer span {
  color: #a0988b;
  font-size: 10px;
}

@media (max-width: 640px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .filters {
    width: 100%;
  }

  .stall-dialog {
    width: 92% !important;
  }

  .stall-grid {
    grid-template-columns: 1fr;
  }
}
</style>
