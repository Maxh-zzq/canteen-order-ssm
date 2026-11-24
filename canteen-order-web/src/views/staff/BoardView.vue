<template>
  <div class="board-page">
    <div class="board-toolbar">
      <div class="live-line">
        <span class="live-dot"></span>
        <strong>实时出餐看板</strong>
        <span class="updated">最后更新 {{ updatedAt }}</span>
      </div>

      <div class="toolbar-actions">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="搜索订单号 / 取餐码 / 菜品"
          :prefix-icon="Search"
          class="search-input"
        />
        <div class="auto-refresh">
          <span>自动刷新</span>
          <el-switch v-model="autoRefresh" />
        </div>
        <el-button circle :loading="loading" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="board-scope" v-if="scopeLoaded && myStalls.length">
      <div class="scope-title">
        <el-icon><Location /></el-icon>
        <strong>我的档口</strong>
        <span>仅展示并允许操作以下档口的订单</span>
      </div>
      <div class="scope-tags">
        <el-tag v-for="stall in myStalls" :key="stall.id" effect="plain" round class="scope-tag">
          {{ stall.name }}
          <small v-if="stall.floor || stall.windowNo">{{ stall.floor }} {{ stall.windowNo }}</small>
        </el-tag>
      </div>
    </div>

    <el-alert
      v-if="scopeLoaded && !myStalls.length"
      type="warning"
      :closable="false"
      show-icon
      title="当前账号未绑定档口，无法查看或操作订单，请联系管理员分配档口权限。"
      class="scope-alert"
    />

    <div class="stat-row">
      <div class="stat-card new">
        <div class="stat-icon"><el-icon><Bell /></el-icon></div>
        <div>
          <span>待接单</span>
          <strong>{{ viewColumns[20].length }}</strong>
        </div>
        <small>请尽快接单</small>
      </div>
      <div class="stat-card cooking">
        <div class="stat-icon"><el-icon><Timer /></el-icon></div>
        <div>
          <span>备餐中</span>
          <strong>{{ viewColumns[30].length }}</strong>
        </div>
        <small>制作中订单</small>
      </div>
      <div class="stat-card ready">
        <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
        <div>
          <span>待取餐</span>
          <strong>{{ viewColumns[40].length }}</strong>
        </div>
        <small>等待学生取餐</small>
      </div>
    </div>

    <div v-if="loading && totalActive === 0" class="board-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else class="kanban">
      <section v-for="column in columnMeta" :key="column.status" class="kanban-column" :class="column.tone">
        <header class="column-head">
          <div class="column-title">
            <span class="column-dot"></span>
            <h2>{{ column.title }}</h2>
            <span class="column-count">{{ viewColumns[column.status].length }}</span>
          </div>
          <span class="column-hint">{{ column.hint }}</span>
        </header>

        <div class="column-body">
          <article
            v-for="order in viewColumns[column.status]"
            :key="order.orderNo"
            class="ticket"
            :class="column.tone"
          >
            <div class="ticket-head">
              <div class="pickup">
                <small>取餐码</small>
                <strong>{{ order.pickupCode }}</strong>
              </div>
              <div class="ticket-meta">
                <span>{{ shortTime(order.createTime) }}</span>
                <span v-if="order.stallName">{{ order.stallName }}</span>
              </div>
            </div>

            <div class="ticket-items">
              <div v-for="item in order.items" :key="`${item.dishId}-${item.specName || ''}`" class="ticket-item">
                <span>{{ item.dishName }}<small v-if="item.specName">{{ item.specName }}</small></span>
                <strong>×{{ item.quantity }}</strong>
              </div>
            </div>

            <div class="ticket-foot">
              <div class="ticket-amount">
                <span>{{ order.orderNo.slice(-8) }}</span>
                <strong>{{ money(order.payAmount) }}</strong>
              </div>

              <el-button
                v-if="order.status === 20"
                type="primary"
                size="small"
                :loading="busyKey === `${order.orderNo}-accept`"
                @click="accept(order)"
              >
                接单
              </el-button>
              <el-button
                v-else-if="order.status === 30"
                type="warning"
                size="small"
                :loading="busyKey === `${order.orderNo}-ready`"
                @click="ready(order)"
              >
                出餐完成
              </el-button>
              <el-button
                v-else
                type="success"
                size="small"
                :loading="busyKey === `${order.orderNo}-pickup`"
                @click="pickup(order)"
              >
                确认取餐
              </el-button>
            </div>
          </article>

          <div v-if="viewColumns[column.status].length === 0" class="column-empty">
            <el-icon><CoffeeCup /></el-icon>
            <p>{{ column.emptyText }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  CircleCheck,
  CoffeeCup,
  Location,
  Refresh,
  Search,
  Timer
} from '@element-plus/icons-vue'
import { acceptOrder, getMyStalls, getStaffOrders, pickupOrder, readyOrder } from '@/api/staff'
import type { OrderVO, StallVO } from '@/types'
import { money, shortTime } from '@/utils/format'

interface ColumnMeta {
  status: number
  title: string
  hint: string
  emptyText: string
  tone: string
}

const columnMeta: ColumnMeta[] = [
  { status: 20, title: '待接单', hint: '学生已支付', emptyText: '暂无新订单，喝口水吧', tone: 'new' },
  { status: 30, title: '备餐中', hint: '制作进行中', emptyText: '暂无备餐中的订单', tone: 'cooking' },
  { status: 40, title: '待取餐', hint: '已通知取餐', emptyText: '暂无等待取餐的订单', tone: 'ready' }
]

const columns = reactive<Record<number, OrderVO[]>>({
  20: [],
  30: [],
  40: []
})
const keyword = ref('')
const loading = ref(false)
const autoRefresh = ref(true)
const updatedAt = ref('—')
const busyKey = ref('')
const myStalls = ref<StallVO[]>([])
const scopeLoaded = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const viewColumns = computed<Record<number, OrderVO[]>>(() => {
  const kw = keyword.value.toLowerCase()
  if (!kw) {
    return {
      20: columns[20] || [],
      30: columns[30] || [],
      40: columns[40] || []
    }
  }

  const result: Record<number, OrderVO[]> = { 20: [], 30: [], 40: [] }
  for (const status of [20, 30, 40]) {
    result[status] = (columns[status] || []).filter((order) => {
      if (order.orderNo.toLowerCase().includes(kw)) return true
      if (order.pickupCode.toLowerCase().includes(kw)) return true
      return order.items.some((item) => item.dishName.toLowerCase().includes(kw))
    })
  }
  return result
})

const totalActive = computed(() =>
  viewColumns.value[20].length + viewColumns.value[30].length + viewColumns.value[40].length
)

async function loadMyStalls() {
  try {
    myStalls.value = await getMyStalls()
  } catch {
    myStalls.value = []
  } finally {
    scopeLoaded.value = true
  }
}

async function load() {
  loading.value = true
  try {
    const [pending, cooking, readyOrders] = await Promise.all([
      getStaffOrders({ status: 20, pageNum: 1, pageSize: 50 }),
      getStaffOrders({ status: 30, pageNum: 1, pageSize: 50 }),
      getStaffOrders({ status: 40, pageNum: 1, pageSize: 50 })
    ])
    columns[20] = pending.list
    columns[30] = cooking.list
    columns[40] = readyOrders.list
    updatedAt.value = dayjs().format('HH:mm:ss')
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

async function accept(order: OrderVO) {
  busyKey.value = `${order.orderNo}-accept`
  try {
    await acceptOrder(order.orderNo)
    ElMessage.success(`已接单：${order.pickupCode}`)
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    busyKey.value = ''
  }
}

async function ready(order: OrderVO) {
  busyKey.value = `${order.orderNo}-ready`
  try {
    await readyOrder(order.orderNo)
    ElMessage.success(`已出餐：${order.pickupCode}`)
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    busyKey.value = ''
  }
}

async function pickup(order: OrderVO) {
  try {
    await ElMessageBox.confirm(
      `确认学生已凭取餐码 ${order.pickupCode} 取走餐品？`,
      '确认取餐',
      { confirmButtonText: '确认取餐', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  busyKey.value = `${order.orderNo}-pickup`
  try {
    await pickupOrder(order.orderNo, order.pickupCode)
    ElMessage.success('订单已完成')
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    busyKey.value = ''
  }
}

function startTimer() {
  stopTimer()
  if (autoRefresh.value) {
    timer = setInterval(load, 15000)
  }
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(autoRefresh, (enabled) => {
  if (enabled) {
    startTimer()
    ElMessage.success('已开启自动刷新（15 秒）')
  } else {
    stopTimer()
    ElMessage.info('已关闭自动刷新')
  }
})

onMounted(() => {
  loadMyStalls()
  load()
  startTimer()
})

onBeforeUnmount(stopTimer)
</script>

<style scoped>
.board-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.board-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.board-scope {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 13px;
  box-shadow: 0 5px 16px rgba(47, 34, 18, 0.035);
}

.scope-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #6f7a72;
  font-size: 11px;
}

.scope-title .el-icon {
  color: #b5731e;
}

.scope-title strong {
  color: var(--ink);
  font-size: 12px;
  font-weight: 750;
}

.scope-title span {
  color: #a0988b;
}

.scope-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.scope-tag {
  --el-tag-bg-color: #fbf4e6;
  --el-tag-border-color: #ecdcc0;
  --el-tag-text-color: #8b5c1c;
  font-size: 11px;
}

.scope-tag small {
  margin-left: 4px;
  color: #b09a76;
  font-size: 9px;
}

.scope-alert {
  border-radius: 12px;
}

.scope-alert :deep(.el-alert__title) {
  font-size: 11px;
}

.live-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4d5b52;
  font-size: 12px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #40a56a;
  box-shadow: 0 0 0 4px rgba(64, 165, 106, 0.14);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 3px rgba(64, 165, 106, 0.12);
  }
  50% {
    box-shadow: 0 0 0 7px rgba(64, 165, 106, 0.04);
  }
}

.live-line strong {
  font-size: 13px;
  font-weight: 750;
  color: var(--ink);
}

.updated {
  color: #9d9487;
  font-size: 10px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 240px;
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 11px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: #fff;
  color: #6f7a72;
  font-size: 11px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 16px 20px;
  border-radius: 15px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 6px 18px rgba(47, 34, 18, 0.05);
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  right: -26px;
  top: -26px;
  width: 74px;
  height: 74px;
  border-radius: 50%;
  opacity: 0.08;
}

.stat-card.new::after {
  background: #d98a24;
}

.stat-card.cooking::after {
  background: #d8572a;
}

.stat-card.ready::after {
  background: #2f8f5b;
}

.stat-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 19px;
}

.stat-card.new .stat-icon {
  color: #a26a16;
  background: #fbf1db;
}

.stat-card.cooking .stat-icon {
  color: #bc4a22;
  background: #fdeee7;
}

.stat-card.ready .stat-icon {
  color: #28734b;
  background: #e8f4ec;
}

.stat-card > div:nth-child(2) {
  flex: 1;
}

.stat-card span {
  display: block;
  font-size: 10px;
  color: #8d948c;
}

.stat-card strong {
  display: block;
  margin-top: 1px;
  font-size: 27px;
  line-height: 1.1;
  font-weight: 850;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
  color: var(--ink);
}

.stat-card small {
  align-self: flex-end;
  color: #a49a8b;
  font-size: 9px;
}

.board-loading {
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
}

.kanban {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: start;
}

.kanban-column {
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  overflow: hidden;
  min-height: 360px;
}

.column-head {
  padding: 14px 16px;
  border-bottom: 1px solid #f2ece2;
  background: #fffdf9;
}

.column-title {
  display: flex;
  align-items: center;
  gap: 7px;
}

.column-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.kanban-column.new .column-dot {
  background: #d98a24;
}

.kanban-column.cooking .column-dot {
  background: #d8572a;
}

.kanban-column.ready .column-dot {
  background: #2f8f5b;
}

.column-title h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
  color: var(--ink);
}

.column-count {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 800;
  color: #fff;
  background: #b9afa1;
}

.kanban-column.new .column-count {
  background: #d98a24;
}

.kanban-column.cooking .column-count {
  background: #d8572a;
}

.kanban-column.ready .column-count {
  background: #2f8f5b;
}

.column-hint {
  display: block;
  margin-top: 3px;
  padding-left: 14px;
  color: #a0988b;
  font-size: 9px;
}

.column-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 330px);
  overflow-y: auto;
}

.ticket {
  border: 1px solid #eee6da;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(47, 34, 18, 0.04);
}

.ticket.new {
  border-top: 3px solid #d98a24;
}

.ticket.cooking {
  border-top: 3px solid #d8572a;
}

.ticket.ready {
  border-top: 3px solid #2f8f5b;
}

.ticket-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 11px 13px 8px;
}

.pickup small {
  display: block;
  color: #9b9386;
  font-size: 8px;
  letter-spacing: 0.14em;
}

.pickup strong {
  display: block;
  margin-top: 1px;
  color: #b8461f;
  font-size: 21px;
  line-height: 1.1;
  font-weight: 850;
  letter-spacing: 0.08em;
}

.ticket-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  color: #a0988b;
  font-size: 9px;
}

.ticket-items {
  padding: 2px 13px 8px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.ticket-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #3d4a42;
  font-size: 12px;
}

.ticket-item span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-item small {
  margin-left: 4px;
  color: #a49a8b;
  font-size: 9px;
}

.ticket-item strong {
  font-size: 12px;
  font-weight: 700;
  color: #59665e;
}

.ticket-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 13px;
  border-top: 1px dashed #eee6da;
}

.ticket-amount {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.ticket-amount span {
  color: #a49a8b;
  font-size: 8px;
  letter-spacing: 0.04em;
}

.ticket-amount strong {
  color: var(--ink);
  font-size: 13px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.column-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 190px;
  color: #b7af9f;
}

.column-empty .el-icon {
  font-size: 26px;
}

.column-empty p {
  margin: 0;
  font-size: 10px;
}

@media (max-width: 1100px) {
  .kanban {
    grid-template-columns: 1fr;
  }

  .column-body {
    max-height: none;
  }

  .stat-row {
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 200px;
  }
}

@media (max-width: 680px) {
  .board-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }
}
</style>
