<template>
  <div class="admin-page">
    <div class="toolbar panel">
      <div class="status-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          class="status-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="activeStatus = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="toolbar-right">
        <el-button :loading="loading" @click="load(pageNum)">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button @click="exportVisible = true">
          <el-icon><Download /></el-icon>导出订单
        </el-button>
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="orders" v-loading="loading" row-key="orderNo">
        <el-table-column label="订单号 / 时间" min-width="190">
          <template #default="{ row }">
            <div class="order-cell">
              <strong>{{ row.orderNo }}</strong>
              <small>{{ dateTimeText(row.createTime) }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="取餐码" width="100" align="center">
          <template #default="{ row }">
            <span class="pickup">{{ row.pickupCode }}</span>
          </template>
        </el-table-column>

        <el-table-column label="档口" width="140">
          <template #default="{ row }">{{ row.stallName || '—' }}</template>
        </el-table-column>

        <el-table-column label="菜品" min-width="220">
          <template #default="{ row }">
            <span class="items-text">{{ orderSummary(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">
            <strong class="amount">{{ money(row.payAmount) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canRefund(row)"
              text
              type="danger"
              size="small"
              @click="refund(row)"
            >
              退款
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="暂无订单" description="当前筛选条件下没有订单记录。" />
        </template>
      </el-table>

      <div class="table-footer">
        <span>共 {{ total }} 笔订单</span>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <template v-if="detail">
        <div class="detail-head">
          <div>
            <strong>{{ detail.orderNo }}</strong>
            <small>{{ dateTimeText(detail.createTime) }}</small>
          </div>
          <StatusTag :status="detail.status" />
        </div>

        <div class="detail-summary">
          <div>
            <small>取餐码</small>
            <strong class="big-code">{{ detail.pickupCode }}</strong>
          </div>
          <div>
            <small>档口</small>
            <strong>{{ detail.stallName || '—' }}</strong>
          </div>
          <div>
            <small>实付金额</small>
            <strong>{{ money(detail.payAmount) }}</strong>
          </div>
        </div>

        <div class="detail-items">
          <div v-for="item in detail.items" :key="`${item.dishId}-${item.specName || ''}`" class="detail-item">
            <span>{{ item.dishName }}<small v-if="item.specName">{{ item.specName }}</small></span>
            <span>×{{ item.quantity }}</span>
            <strong>{{ money(item.amount) }}</strong>
          </div>
        </div>

        <dl class="detail-meta">
          <div><dt>支付时间</dt><dd>{{ dateTimeText(detail.payTime) }}</dd></div>
          <div><dt>完成时间</dt><dd>{{ dateTimeText(detail.finishTime) }}</dd></div>
          <div><dt>备注</dt><dd>{{ detail.remark || '无' }}</dd></div>
          <div v-if="detail.cancelReason"><dt>取消/退款原因</dt><dd>{{ detail.cancelReason }}</dd></div>
        </dl>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="exportVisible" title="导出订单" width="440px">
      <el-form label-position="top">
        <el-form-item label="订单日期范围">
          <el-date-picker
            v-model="exportRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="full"
          />
        </el-form-item>
      </el-form>
      <p class="export-note">导出为 CSV 文件，包含订单号、状态、金额、取餐码等信息。</p>

      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="doExport">导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { exportOrders } from '@/api/admin'
import { refundOrder } from '@/api/payment'
import { getStaffOrders } from '@/api/staff'
import type { OrderVO } from '@/types'
import { dateTimeText, money } from '@/utils/format'
import { downloadBlob, orderSummary } from '@/utils'

const tabs = [
  { label: '全部', value: 0 },
  { label: '待支付', value: 10 },
  { label: '已支付', value: 20 },
  { label: '备餐中', value: 30 },
  { label: '可取餐', value: 40 },
  { label: '已完成', value: 50 },
  { label: '已取消', value: 60 },
  { label: '已退款', value: 70 }
]

const activeStatus = ref(0)
const orders = ref<OrderVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<OrderVO | null>(null)
const exportVisible = ref(false)
const exporting = ref(false)
const exportRange = ref<[string, string]>([
  dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])

function canRefund(order: OrderVO) {
  return [20, 30, 40, 50].includes(order.status)
}

async function load(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getStaffOrders({
      status: activeStatus.value || undefined,
      pageNum: page,
      pageSize
    })
    orders.value = data.list
    total.value = data.total
    pageNum.value = page
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function openDetail(order: OrderVO) {
  detail.value = order
  detailVisible.value = true
}

async function refund(order: OrderVO) {
  try {
    const result = await ElMessageBox.prompt(
      `订单 ${order.orderNo} 将退款 ${money(order.payAmount)}，请填写退款原因。`,
      '订单退款',
      {
        confirmButtonText: '确认退款',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：档口售罄，无法出餐',
        inputValidator: (value) => {
          if (!value || value.trim().length < 2) return '退款原因至少 2 个字符'
          return true
        }
      }
    )
    await refundOrder(order.orderNo, result.value.trim())
    ElMessage.success('退款已提交')
    await load(pageNum.value)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

async function doExport() {
  if (!exportRange.value) {
    ElMessage.warning('请选择订单日期范围')
    return
  }

  exporting.value = true
  try {
    const { blob, filename } = await exportOrders(exportRange.value[0], exportRange.value[1])
    downloadBlob(blob, filename || `orders_${exportRange.value[0]}_${exportRange.value[1]}.csv`)
    ElMessage.success('订单已导出')
    exportVisible.value = false
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    exporting.value = false
  }
}

watch(activeStatus, () => load(1))

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
  padding: 10px 14px;
  flex-wrap: wrap;
}

.status-tabs {
  display: flex;
  align-items: center;
  gap: 3px;
  overflow-x: auto;
}

.status-tab {
  flex-shrink: 0;
  padding: 7px 11px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #6a756d;
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

.status-tab:hover {
  background: #f7f0e5;
  color: var(--brand-dark);
}

.status-tab.active {
  color: #fff;
  background: var(--forest);
}

.toolbar-right {
  display: flex;
  gap: 8px;
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

.order-cell strong {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #3b4940;
}

.order-cell small {
  display: block;
  margin-top: 3px;
  color: #a0988b;
  font-size: 9px;
}

.pickup {
  color: #b8461f;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.items-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
  color: #5c6860;
  font-size: 11px;
}

.amount {
  color: var(--brand);
  font-size: 12px;
  font-weight: 800;
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

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.detail-head strong {
  display: block;
  font-size: 12px;
  color: #3b4940;
}

.detail-head small {
  display: block;
  margin-top: 3px;
  color: #a0988b;
  font-size: 10px;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f9f5ed;
  margin-bottom: 15px;
}

.detail-summary small {
  display: block;
  color: #a2947f;
  font-size: 9px;
}

.detail-summary strong {
  display: block;
  margin-top: 3px;
  color: #36443b;
  font-size: 12px;
  font-weight: 700;
}

.big-code {
  color: #b8461f !important;
  font-size: 22px !important;
  letter-spacing: 0.08em;
}

.detail-items {
  border-top: 1px solid #f0e8dc;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 2px;
  border-bottom: 1px solid #f7f1e8;
  font-size: 12px;
}

.detail-item span:first-child {
  flex: 1;
}

.detail-item small {
  margin-left: 5px;
  color: #a49a8b;
  font-size: 10px;
}

.detail-item span:nth-child(2) {
  color: #8d948c;
  font-size: 11px;
}

.detail-item strong {
  width: 80px;
  text-align: right;
  font-size: 11px;
  color: #36443b;
}

.detail-meta {
  margin: 12px 0 0;
}

.detail-meta > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 2px;
  font-size: 11px;
}

.detail-meta dt {
  color: #8d948c;
}

.detail-meta dd {
  margin: 0;
  color: #46524b;
}

.full {
  width: 100%;
}

.export-note {
  margin: 0;
  color: #8d948c;
  font-size: 10px;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-summary {
    grid-template-columns: 1fr;
  }
}
</style>
