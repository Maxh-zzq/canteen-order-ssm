<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div>
        <h2>操作日志</h2>
        <p>AOP 记录的关键接口调用，用于审计与问题排查。</p>
      </div>
      <el-button :loading="loading" @click="load(pageNum)">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <div class="table-panel">
      <el-table :data="logs" v-loading="loading" row-key="id">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            <span class="time">{{ dateTimeText(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作人" width="110">
          <template #default="{ row }">
            <span class="username">{{ row.username || '匿名' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="模块" width="120">
          <template #default="{ row }">{{ row.module || '—' }}</template>
        </el-table-column>

        <el-table-column label="请求方法" min-width="190">
          <template #default="{ row }">
            <span class="method">{{ row.method }}</span>
          </template>
        </el-table-column>

        <el-table-column label="IP" width="130">
          <template #default="{ row }">{{ row.ip || '—' }}</template>
        </el-table-column>

        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">
            <span :class="['cost', { slow: row.costMs > 1000 }]">{{ row.costMs }} ms</span>
          </template>
        </el-table-column>

        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="light" size="small" round>
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Trace ID" width="150">
          <template #default="{ row }">
            <span class="trace">{{ row.traceId || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="错误信息" min-width="180">
          <template #default="{ row }">
            <span v-if="row.errorMsg" class="error-text">{{ row.errorMsg }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="暂无操作日志" description="系统产生关键操作后，日志会自动记录在这里。" />
        </template>
      </el-table>

      <div class="table-footer">
        <span>共 {{ total }} 条日志</span>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getLogs } from '@/api/admin'
import type { OperLogVO } from '@/types'
import { dateTimeText } from '@/utils/format'

const logs = ref<OperLogVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const loading = ref(false)

async function load(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getLogs({ pageNum: page, pageSize })
    logs.value = data.list
    total.value = data.total
    pageNum.value = page
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
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
  font-size: 11px;
}

.time {
  color: #5c6860;
  font-size: 10px;
  white-space: nowrap;
}

.username {
  font-weight: 650;
  color: #3b4940;
}

.method {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 2px 7px;
  border-radius: 5px;
  background: #f6f2ea;
  color: #6f5b46;
  font-size: 10px;
}

.cost {
  color: #5e7f67;
  font-variant-numeric: tabular-nums;
}

.cost.slow {
  color: #c2811c;
  font-weight: 700;
}

.trace {
  color: #9a9285;
  font-family: 'Consolas', monospace;
  font-size: 9px;
}

.error-text {
  display: block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #c24a2c;
  font-size: 10px;
}

.muted {
  color: #b3aa9b;
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
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
