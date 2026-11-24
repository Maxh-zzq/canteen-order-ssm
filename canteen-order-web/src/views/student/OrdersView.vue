<template>
  <div class="page-shell orders-page">
    <div class="page-head">
      <div>
        <span class="eyebrow">MY ORDERS</span>
        <h1>我的订单</h1>
        <p>查看备餐进度、取餐码与历史订单。</p>
      </div>
      <RouterLink to="/menu" class="back-link">
        <el-icon><ArrowLeft /></el-icon>
        继续点餐
      </RouterLink>
    </div>

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
      <el-button text class="refresh" :loading="loading" @click="load(pageNum)">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <div v-if="loading && orders.length === 0" class="order-list">
      <div v-for="index in 3" :key="index" class="order-card skeleton">
        <el-skeleton :rows="3" animated />
      </div>
    </div>

    <div v-else-if="orders.length === 0" class="empty-panel">
      <EmptyState
        :title="activeStatus === 0 ? '还没有订单' : '该状态下暂无订单'"
        description="今天想吃什么？去菜单看看各档口的供应吧。"
      >
        <RouterLink to="/menu">
          <el-button type="primary" round>去点餐</el-button>
        </RouterLink>
      </EmptyState>
    </div>

    <template v-else>
      <div class="order-list">
        <article v-for="order in orders" :key="order.orderNo" class="order-card">
          <header class="order-head">
            <div class="order-head-left">
              <span class="order-no">{{ order.orderNo }}</span>
              <span class="order-time">{{ dateTimeText(order.createTime) }}</span>
            </div>
            <StatusTag :status="order.status" />
          </header>

          <div class="order-body">
            <div class="order-items">
              <div class="stall-line">
                <el-icon><Location /></el-icon>
                {{ order.stallName || '食堂档口' }}
              </div>

              <div class="item-lines">
                <div v-for="item in order.items" :key="`${item.dishId}-${item.specName || ''}`" class="item-line">
                  <span class="item-name">
                    {{ item.dishName }}
                    <small v-if="item.specName">{{ item.specName }}</small>
                  </span>
                  <span class="item-qty">×{{ item.quantity }}</span>
                  <span class="item-amount">{{ money(item.amount) }}</span>
                </div>
              </div>

              <div class="order-total">
                共 {{ order.items.reduce((sum, item) => sum + item.quantity, 0) }} 件
                <strong>实付 {{ money(order.payAmount) }}</strong>
              </div>
            </div>

            <div class="pickup-side">
              <template v-if="order.status === 10">
                <span class="side-label">待支付</span>
                <strong class="countdown">{{ remainingOf(order) }}</strong>
                <small>超时自动取消</small>
              </template>
              <template v-else-if="order.status >= 20 && order.status <= 40">
                <span class="side-label">取餐码</span>
                <strong class="pickup-code">{{ order.pickupCode }}</strong>
                <small>{{ statusHint(order.status) }}</small>
              </template>
              <template v-else>
                <span class="side-label">取餐码</span>
                <strong class="pickup-code muted">{{ order.pickupCode }}</strong>
                <small>{{ order.statusText }}</small>
              </template>
            </div>
          </div>

          <footer class="order-foot">
            <button type="button" class="detail-link" @click="openDetail(order)">
              订单详情
              <el-icon><ArrowRight /></el-icon>
            </button>

            <div class="order-actions">
              <el-button
                v-if="order.status === 10"
                size="small"
                @click="cancel(order)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.status === 10"
                type="primary"
                size="small"
                @click="pay(order)"
              >
                立即支付
              </el-button>
              <el-button
                v-if="order.canCancel && order.status === 20"
                size="small"
                @click="cancel(order)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.status === 50"
                type="primary"
                plain
                size="small"
                @click="openReview(order)"
              >
                评价菜品
              </el-button>
              <el-button
                v-if="order.status === 40"
                type="success"
                size="small"
                @click="openDetail(order)"
              >
                查看取餐码
              </el-button>
            </div>
          </footer>
        </article>
      </div>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="handlePage"
        />
      </div>
    </template>

    <el-dialog v-model="detailVisible" title="订单详情" width="560px">
      <template v-if="detail">
        <div class="detail-top">
          <StatusTag :status="detail.status" />
          <span class="detail-no">{{ detail.orderNo }}</span>
        </div>

        <div class="detail-pickup">
          <div>
            <small>取餐码</small>
            <strong>{{ detail.pickupCode }}</strong>
          </div>
          <div class="detail-pickup-right">
            <span>{{ detail.stallName || '食堂档口' }}</span>
            <span>{{ statusHint(detail.status) }}</span>
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
          <div><dt>下单时间</dt><dd>{{ dateTimeText(detail.createTime) }}</dd></div>
          <div><dt>支付时间</dt><dd>{{ dateTimeText(detail.payTime) }}</dd></div>
          <div><dt>支付金额</dt><dd>{{ money(detail.payAmount) }}</dd></div>
          <div><dt>订单备注</dt><dd>{{ detail.remark || '无' }}</dd></div>
          <div v-if="detail.cancelReason"><dt>取消原因</dt><dd>{{ detail.cancelReason }}</dd></div>
        </dl>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && detail.status === 10"
          type="primary"
          @click="pay(detail)"
        >
          立即支付
        </el-button>
        <el-button
          v-if="detail && detail.status === 50"
          type="primary"
          plain
          @click="openReview(detail)"
        >
          评价菜品
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="评价菜品" width="460px">
      <div class="review-head">
        <span>{{ reviewTarget?.stallName || '本次点餐' }}</span>
        <small>{{ reviewTarget?.orderNo }}</small>
      </div>
      <el-form label-position="top">
        <el-form-item label="整体评分">
          <el-rate v-model="reviewForm.score" :max="5" show-text :texts="['很差', '较差', '一般', '满意', '超赞']" />
        </el-form-item>
        <el-form-item label="评价内容（选填）">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="菜品口味、分量、出餐速度怎么样？"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Location,
  Refresh
} from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { cancelOrder, getOrders } from '@/api/order'
import { payByBalance } from '@/api/payment'
import { createReview } from '@/api/review'
import { getBalance } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { OrderVO } from '@/types'
import { countdownText, dateTimeText, money } from '@/utils/format'

const auth = useAuthStore()

const tabs = [
  { label: '全部', value: 0 },
  { label: '待支付', value: 10 },
  { label: '已支付', value: 20 },
  { label: '备餐中', value: 30 },
  { label: '可取餐', value: 40 },
  { label: '已完成', value: 50 }
]

const activeStatus = ref(0)
const orders = ref<OrderVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 8
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<OrderVO | null>(null)
const reviewVisible = ref(false)
const reviewTarget = ref<OrderVO | null>(null)
const reviewing = ref(false)
const reviewForm = reactive({ score: 5, content: '' })
const nowTick = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

function statusHint(status: number) {
  if (status === 20) return '档口即将接单'
  if (status === 30) return '正在为你备餐'
  if (status === 40) return '请尽快到档口取餐'
  if (status === 50) return '订单已完成'
  if (status === 70) return '已退款'
  return '订单已取消'
}

function remainingOf(order: OrderVO) {
  nowTick.value
  return countdownText(order.expireTime) || '—'
}

async function load(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getOrders({
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

function handlePage(page: number) {
  load(page)
}

function openDetail(order: OrderVO) {
  detail.value = order
  detailVisible.value = true
}

async function refreshBalance() {
  try {
    const value = await getBalance()
    if (auth.user) auth.user.balance = Number(value || 0)
  } catch {
    // 忽略
  }
}

async function pay(order: OrderVO) {
  try {
    await ElMessageBox.confirm(
      `确认使用校园卡余额支付 ${money(order.payAmount)} 吗？`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '再想想', type: 'info' }
    )
    const paid = await payByBalance(order.orderNo)
    ElMessage.success(`支付成功，取餐码 ${paid.pickupCode}`)
    detailVisible.value = false
    await Promise.all([load(pageNum.value), refreshBalance()])
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

async function cancel(order: OrderVO) {
  try {
    await ElMessageBox.confirm('取消后库存将释放，确定取消这笔订单吗？', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '保留订单',
      type: 'warning'
    })
    await cancelOrder(order.orderNo, '用户主动取消')
    ElMessage.success('订单已取消')
    await load(pageNum.value)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

function openReview(order: OrderVO) {
  reviewTarget.value = order
  reviewForm.score = 5
  reviewForm.content = ''
  detailVisible.value = false
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewTarget.value) return
  reviewing.value = true
  try {
    await createReview({
      orderNo: reviewTarget.value.orderNo,
      score: reviewForm.score,
      content: reviewForm.content.trim() || undefined
    })
    ElMessage.success('感谢你的评价')
    reviewVisible.value = false
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    reviewing.value = false
  }
}

watch(activeStatus, () => {
  pageNum.value = 1
  load(1)
})

onMounted(() => {
  load(1)
  timer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.eyebrow {
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.22em;
  color: var(--brand);
}

h1 {
  margin: 7px 0 0;
  font-size: 30px;
  font-weight: 850;
  letter-spacing: -0.035em;
}

.page-head p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 9px 15px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: #59665e;
  font-size: 12px;
  font-weight: 650;
}

.back-link:hover {
  color: var(--brand);
  border-color: #eac4b3;
}

.status-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  margin-bottom: 18px;
  border-radius: 13px;
  background: #fff;
  border: 1px solid var(--line);
  overflow-x: auto;
}

.status-tab {
  flex-shrink: 0;
  padding: 8px 15px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #6a756d;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
  transition: all 0.18s ease;
}

.status-tab:hover {
  color: var(--brand-dark);
  background: #f7f0e5;
}

.status-tab.active {
  color: #fff;
  background: var(--forest);
}

.refresh {
  margin-left: auto;
  flex-shrink: 0;
  color: #7d887f;
  font-size: 11px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 960px;
}

.order-card {
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 8px 24px rgba(47, 34, 18, 0.05);
  overflow: hidden;
}

.order-card.skeleton {
  padding: 24px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 22px;
  border-bottom: 1px solid #f4eee5;
  background: #fffdf9;
}

.order-head-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.order-no {
  font-size: 12px;
  font-weight: 750;
  color: #44524a;
  letter-spacing: 0.02em;
}

.order-time {
  font-size: 10px;
  color: #a0988b;
}

.order-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 18px;
  padding: 20px 22px 18px;
}

.stall-line {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 12px;
  color: #397050;
  font-size: 11px;
  font-weight: 700;
}

.item-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #3d4a42;
}

.item-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-name small {
  margin-left: 5px;
  color: #a49a8b;
  font-size: 10px;
}

.item-qty {
  color: #98a098;
  font-size: 11px;
}

.item-amount {
  width: 76px;
  text-align: right;
  font-size: 12px;
  font-weight: 650;
  color: #36443b;
  font-variant-numeric: tabular-nums;
}

.order-total {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #eee6da;
  text-align: right;
  color: #90988f;
  font-size: 11px;
}

.order-total strong {
  margin-left: 10px;
  color: var(--brand);
  font-size: 17px;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.pickup-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding-left: 18px;
  border-left: 1px dashed #eee6da;
  text-align: center;
}

.side-label {
  color: #9b9386;
  font-size: 9px;
  letter-spacing: 0.14em;
}

.pickup-code {
  color: #b8461f;
  font-size: 25px;
  font-weight: 850;
  letter-spacing: 0.1em;
}

.pickup-code.muted {
  color: #9c9487;
}

.countdown {
  color: #c47818;
  font-size: 22px;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.pickup-side small {
  color: #a0988b;
  font-size: 9px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 22px 14px;
  border-top: 1px solid #f6f1e8;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  padding: 0;
  color: #7f8981;
  font-size: 11px;
  cursor: pointer;
}

.detail-link:hover {
  color: var(--brand);
}

.order-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.empty-panel {
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
}

.detail-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.detail-no {
  font-size: 11px;
  color: #8d948c;
}

.detail-pickup {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-radius: 12px;
  background: #f7f2e8;
  border: 1px dashed #ddcfb8;
  margin-bottom: 16px;
}

.detail-pickup small {
  display: block;
  font-size: 8px;
  letter-spacing: 0.16em;
  color: #a2947f;
}

.detail-pickup strong {
  display: block;
  margin-top: 3px;
  color: #b8461f;
  font-size: 26px;
  letter-spacing: 0.1em;
}

.detail-pickup-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: #766c5c;
  font-size: 11px;
}

.detail-items {
  border-top: 1px solid #f0e8dc;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 2px;
  border-bottom: 1px solid #f7f1e8;
  font-size: 13px;
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
  font-size: 12px;
  color: #36443b;
}

.detail-meta {
  margin: 14px 0 0;
}

.detail-meta > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 2px;
  font-size: 11px;
}

.detail-meta dt {
  color: #8d948c;
}

.detail-meta dd {
  margin: 0;
  color: #46524b;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 13px;
  margin-bottom: 14px;
  border-radius: 9px;
  background: #f7f4ed;
  font-size: 12px;
  font-weight: 700;
  color: #36443b;
}

.review-head small {
  color: #a0988b;
  font-size: 10px;
  font-weight: 500;
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  h1 {
    font-size: 25px;
  }

  .status-tabs {
    gap: 3px;
  }

  .status-tab {
    padding: 7px 11px;
    font-size: 11px;
  }

  .refresh {
    display: none;
  }

  .order-body {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .pickup-side {
    padding: 14px 0 0;
    border-left: none;
    border-top: 1px dashed #eee6da;
  }

  .order-head,
  .order-foot {
    padding-left: 16px;
    padding-right: 16px;
  }

  .order-head-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 3px;
  }
}
</style>
