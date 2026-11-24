<template>
  <div class="page-shell cart-page">
    <div class="page-head">
      <div>
        <span class="eyebrow">MY CART</span>
        <h1>购物车</h1>
        <p>确认菜品与数量，提交后请在 15 分钟内完成支付。</p>
      </div>
      <RouterLink to="/menu" class="back-link">
        <el-icon><ArrowLeft /></el-icon>
        继续点餐
      </RouterLink>
    </div>

    <div v-if="cart.loading && !cart.loaded" class="loading-panel">
      <el-skeleton :rows="4" animated />
    </div>

    <div v-else-if="cart.isEmpty" class="empty-panel">
      <EmptyState
        title="购物车还是空的"
        description="去今日菜单挑几道想吃的，到店直接取餐。"
      >
        <RouterLink to="/menu">
          <el-button type="primary" round>去点餐</el-button>
        </RouterLink>
      </EmptyState>
    </div>

    <div v-else class="cart-layout">
      <section class="cart-list panel">
        <div class="list-head">
          <div>
            <strong>已选菜品</strong>
            <span>{{ cart.distinctCount }} 种 · {{ cart.totalQuantity }} 件</span>
          </div>
          <el-button text class="clear-btn" @click="clearAll">
            <el-icon><Delete /></el-icon>清空
          </el-button>
        </div>

        <div class="stall-notice">
          <el-icon><Location /></el-icon>
          <span>取餐档口：{{ stallSummary }}</span>
        </div>

        <div class="cart-items">
          <article
            v-for="item in cart.items"
            :key="itemKey(item)"
            class="cart-item"
            :class="{ busy: busyKey === itemKey(item) }"
          >
            <div class="item-cover">
              <img v-if="item.coverUrl" :src="assetUrl(item.coverUrl)" :alt="item.dishName" />
              <span v-else>{{ initialOf(item.dishName) }}</span>
            </div>

            <div class="item-info">
              <div class="item-top">
                <h3>{{ item.dishName }}</h3>
                <button class="remove-btn" type="button" aria-label="移除菜品" @click="removeItem(item)">
                  <el-icon><Close /></el-icon>
                </button>
              </div>
              <div class="item-meta">
                <span v-if="item.specName" class="spec">{{ item.specName }}</span>
                <span class="stall-name">{{ stallOf(item) }}</span>
              </div>
              <div class="item-price">{{ money(item.price) }}</div>
            </div>

            <div class="item-actions">
              <div class="stepper">
                <button
                  type="button"
                  :disabled="item.quantity <= 1 || busyKey === itemKey(item)"
                  @click="changeQuantity(item, -1)"
                >
                  −
                </button>
                <span>{{ item.quantity }}</span>
                <button
                  type="button"
                  :disabled="busyKey === itemKey(item) || reachedStock(item)"
                  @click="changeQuantity(item, 1)"
                >
                  +
                </button>
              </div>
              <strong class="amount">{{ money(item.amount) }}</strong>
            </div>
          </article>
        </div>

        <p class="stock-tip">
          <el-icon><InfoFilled /></el-icon>
          库存随下单实时扣减，若提示库存不足请减少数量或选择其他菜品。
        </p>
      </section>

      <aside class="summary">
        <div class="summary-card">
          <div class="summary-head">
            <h2>订单摘要</h2>
            <span class="summary-tag">到店自取</span>
          </div>

          <div class="summary-row">
            <span>商品件数</span>
            <strong>{{ cart.totalQuantity }} 件</strong>
          </div>
          <div class="summary-row">
            <span>商品金额</span>
            <strong>{{ money(cart.totalAmount) }}</strong>
          </div>
          <div class="summary-row">
            <span>取餐档口</span>
            <strong class="ellipsis">{{ stallSummary }}</strong>
          </div>

          <div class="summary-divider"></div>

          <div class="summary-total">
            <span>应付金额</span>
            <strong>{{ money(cart.totalAmount) }}</strong>
          </div>

          <div class="balance-row" :class="{ low: !canPay }">
            <div>
              <span>校园卡余额</span>
              <strong>{{ money(balance) }}</strong>
            </div>
            <RouterLink to="/profile" class="recharge-link">去充值</RouterLink>
          </div>

          <el-alert
            v-if="!canPay"
            type="warning"
            :closable="false"
            show-icon
            class="balance-alert"
            title="余额不足，请先充值后再支付"
          />

          <el-button
            type="primary"
            size="large"
            class="checkout-btn"
            :loading="creating"
            @click="openCheckout"
          >
            提交订单
            <el-icon><ArrowRight /></el-icon>
          </el-button>

          <p class="summary-note">
            <el-icon><Clock /></el-icon>
            提交后 15 分钟内未支付，订单将自动取消
          </p>
        </div>

        <div class="pickup-card">
          <div class="pickup-icon"><el-icon><Location /></el-icon></div>
          <div>
            <strong>到店自取</strong>
            <p>支付成功后凭取餐码到对应档口取餐，请留意备餐进度。</p>
          </div>
        </div>
      </aside>
    </div>

    <el-dialog
      v-model="checkoutVisible"
      :title="createdOrder ? (payDone ? '支付成功' : '订单已创建') : '确认订单'"
      width="520px"
      class="checkout-dialog"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <template v-if="!createdOrder">
        <div class="confirm-block">
          <div class="confirm-stall">
            <span class="confirm-label">取餐档口</span>
            <strong>{{ stallSummary }}</strong>
          </div>

          <div class="confirm-items">
            <div v-for="item in cart.items" :key="itemKey(item)" class="confirm-item">
              <span class="confirm-name">
                {{ item.dishName }}
                <small v-if="item.specName">{{ item.specName }}</small>
              </span>
              <span class="confirm-qty">×{{ item.quantity }}</span>
              <span class="confirm-amount">{{ money(item.amount) }}</span>
            </div>
          </div>

          <div class="confirm-total">
            <span>合计</span>
            <strong>{{ money(cart.totalAmount) }}</strong>
          </div>

          <el-form label-position="top" class="remark-form">
            <el-form-item label="订单备注（选填）">
              <el-input
                v-model="remark"
                type="textarea"
                :rows="2"
                maxlength="100"
                show-word-limit
                placeholder="例如：少辣、不要香菜"
              />
            </el-form-item>
          </el-form>

          <div class="confirm-balance">
            <span>支付方式：校园卡余额（{{ money(balance) }}）</span>
            <span v-if="!canPay" class="danger-text">余额不足</span>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="order-result">
          <div class="result-badge" :class="{ paid: payDone }">
            <el-icon v-if="payDone" :size="30"><CircleCheckFilled /></el-icon>
            <el-icon v-else :size="30"><Clock /></el-icon>
          </div>

          <h3>{{ payDone ? '支付成功，请前往档口取餐' : '订单已创建，等待支付' }}</h3>
          <p v-if="!payDone" class="result-sub">
            请在 <strong>{{ remaining }}</strong> 内完成支付，超时订单将自动取消。
          </p>
          <p v-else class="result-sub">档口已收到订单，备餐完成后会更新取餐状态。</p>

          <div class="pickup-code">
            <small>PICKUP CODE</small>
            <strong>{{ createdOrder.pickupCode }}</strong>
          </div>

          <div class="result-rows">
            <div>
              <span>订单号</span>
              <strong>{{ createdOrder.orderNo }}</strong>
            </div>
            <div>
              <span>取餐档口</span>
              <strong>{{ createdOrder.stallName || stallSummary }}</strong>
            </div>
            <div>
              <span>支付金额</span>
              <strong>{{ money(createdOrder.payAmount) }}</strong>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <template v-if="!createdOrder">
          <el-button @click="checkoutVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="submitOrder">
            提交订单 · {{ money(cart.totalAmount) }}
          </el-button>
        </template>
        <template v-else>
          <template v-if="payDone">
            <el-button @click="checkoutVisible = false">留在购物车</el-button>
            <el-button type="primary" @click="goOrders">查看订单</el-button>
          </template>
          <template v-else>
            <el-button @click="payLater">稍后支付</el-button>
            <el-button
              type="primary"
              :loading="paying"
              :disabled="!canPay"
              @click="payNow"
            >
              立即支付 · {{ money(createdOrder.payAmount) }}
            </el-button>
          </template>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  CircleCheckFilled,
  Clock,
  Close,
  Delete,
  InfoFilled,
  Location
} from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getTodayMenus } from '@/api/dish'
import { placeOrder } from '@/api/order'
import { payByBalance } from '@/api/payment'
import { getBalance } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { CartItemVO, MenuVO, OrderVO } from '@/types'
import { assetUrl, countdownText, initialOf, money } from '@/utils/format'
import { newRequestId } from '@/utils'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

const menuMap = ref(new Map<number, MenuVO>())
const balance = ref(Number(auth.user?.balance || 0))
const busyKey = ref('')
const creating = ref(false)
const paying = ref(false)
const checkoutVisible = ref(false)
const createdOrder = ref<OrderVO | null>(null)
const payDone = ref(false)
const remark = ref('')
const nowTick = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const canPay = computed(() => balance.value >= Number(cart.totalAmount || 0))

const stallSummary = computed(() => {
  const names = Array.from(
    new Set(cart.items.map((item) => menuMap.value.get(item.dishId)?.stallName).filter(Boolean))
  )
  return names.length ? names.join(' / ') : '食堂档口'
})

const remaining = computed(() => {
  nowTick.value
  return countdownText(createdOrder.value?.expireTime) || '15:00'
})

function itemKey(item: CartItemVO) {
  return `${item.dishId}-${item.specId ?? 0}`
}

function stallOf(item: CartItemVO) {
  return menuMap.value.get(item.dishId)?.stallName || '食堂档口'
}

function reachedStock(item: CartItemVO) {
  return item.stockLeft !== null && item.quantity >= item.stockLeft
}

async function refreshBalance() {
  try {
    const value = await getBalance()
    balance.value = Number(value || 0)
    if (auth.user) auth.user.balance = balance.value
  } catch {
    // 忽略余额刷新失败
  }
}

async function changeQuantity(item: CartItemVO, delta: number) {
  const next = item.quantity + delta
  if (next < 1) {
    await removeItem(item)
    return
  }
  if (item.stockLeft !== null && next > item.stockLeft) {
    ElMessage.warning(`「${item.dishName}」库存仅剩 ${item.stockLeft} 份`)
    return
  }

  busyKey.value = itemKey(item)
  try {
    await cart.update({ dishId: item.dishId, specId: item.specId, quantity: next })
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    busyKey.value = ''
  }
}

async function removeItem(item: CartItemVO) {
  busyKey.value = itemKey(item)
  try {
    await cart.remove(item.dishId, item.specId)
    ElMessage.success(`已移除「${item.dishName}」`)
  } catch {
    // 忽略
  } finally {
    busyKey.value = ''
  }
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空购物车中的全部菜品吗？', '清空购物车', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cart.clear()
    ElMessage.success('购物车已清空')
  } catch {
    // 用户取消
  }
}

function openCheckout() {
  if (cart.isEmpty) return
  createdOrder.value = null
  payDone.value = false
  remark.value = ''
  checkoutVisible.value = true
}

async function submitOrder() {
  creating.value = true
  try {
    const order = await placeOrder({
      requestId: newRequestId(),
      remark: remark.value.trim() || undefined
    })
    createdOrder.value = order
    cart.reset()
    startTimer()
    refreshBalance()
    ElMessage.success('订单创建成功，请尽快完成支付')
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    creating.value = false
  }
}

async function payNow() {
  if (!createdOrder.value) return
  if (!canPay.value) {
    ElMessage.warning('余额不足，请先到个人中心充值')
    return
  }

  paying.value = true
  try {
    const paid = await payByBalance(createdOrder.value.orderNo)
    createdOrder.value = paid
    payDone.value = true
    stopTimer()
    await refreshBalance()
    ElMessage.success(`支付成功，取餐码 ${paid.pickupCode}`)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    paying.value = false
  }
}

function payLater() {
  checkoutVisible.value = false
}

function goOrders() {
  checkoutVisible.value = false
  router.push('/orders')
}

function handleDialogClosed() {
  if (createdOrder.value) {
    createdOrder.value = null
    payDone.value = false
    stopTimer()
    router.push('/orders')
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  try {
    const menus = await getTodayMenus()
    menuMap.value = new Map(menus.map((menu) => [menu.dishId, menu]))
  } catch {
    // 菜单映射失败时仍允许查看购物车
  }
  cart.fetchCart().catch(() => undefined)
  refreshBalance()
})

onBeforeUnmount(stopTimer)
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

.loading-panel,
.empty-panel {
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
}

.cart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 344px;
  gap: 20px;
  align-items: start;
}

.panel {
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 8px 24px rgba(47, 34, 18, 0.05);
}

.cart-list {
  overflow: hidden;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 14px;
}

.list-head strong {
  font-size: 15px;
  font-weight: 800;
  color: var(--ink);
}

.list-head span {
  margin-left: 10px;
  color: var(--muted);
  font-size: 11px;
}

.clear-btn {
  color: #8c8477;
  font-size: 12px;
}

.stall-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 22px 8px;
  padding: 8px 12px;
  border-radius: 9px;
  background: #f3f7f3;
  color: #397050;
  font-size: 11px;
}

.cart-items {
  padding: 0 12px 6px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 10px;
  border-bottom: 1px solid #f2ece2;
  transition: opacity 0.2s ease;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item.busy {
  opacity: 0.55;
}

.item-cover {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: 13px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f6e6cf, #e8c39e);
  color: rgba(92, 54, 22, 0.72);
  font-family: 'STKaiti', 'KaiTi', 'Songti SC', serif;
  font-size: 29px;
  font-weight: 800;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-top h3 {
  margin: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 750;
  color: var(--ink);
}

.remove-btn {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #b2a99a;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.remove-btn:hover {
  color: #c34b2e;
  background: #fbeee9;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 5px;
  font-size: 10px;
  color: #9d9487;
}

.spec {
  padding: 1px 6px;
  border-radius: 4px;
  color: #8a5b2a;
  background: #f8eedc;
}

.item-price {
  margin-top: 7px;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.item-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.stepper {
  display: inline-flex;
  align-items: center;
  height: 32px;
  border: 1px solid #e8e0d3;
  border-radius: 9px;
  overflow: hidden;
}

.stepper button {
  width: 31px;
  height: 31px;
  border: none;
  background: #faf6ee;
  color: #5e6a62;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.stepper button:hover:not(:disabled) {
  background: #f2e8d8;
  color: var(--brand);
}

.stepper button:disabled {
  color: #c9c0b2;
  cursor: not-allowed;
}

.stepper span {
  width: 34px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.amount {
  color: var(--ink);
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.stock-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 12px 22px 16px;
  color: #a49a8b;
  font-size: 10px;
  border-top: 1px dashed #f0e8dc;
}

.summary {
  position: sticky;
  top: 96px;
}

.summary-card {
  padding: 22px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 12px 30px rgba(47, 34, 18, 0.08);
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.summary-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.summary-tag {
  padding: 3px 8px;
  border-radius: 5px;
  background: #eef6ef;
  color: #36724b;
  font-size: 10px;
  font-weight: 700;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 11px;
  font-size: 12px;
  color: #6f7a72;
}

.summary-row strong {
  color: #36443b;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.ellipsis {
  max-width: 170px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-divider {
  height: 1px;
  margin: 15px 0;
  background: #f0e8dc;
}

.summary-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.summary-total span {
  font-size: 12px;
  color: #6f7a72;
}

.summary-total strong {
  color: var(--brand);
  font-size: 29px;
  font-weight: 850;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}

.balance-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 12px;
  border-radius: 10px;
  background: #f7f4ed;
  margin-bottom: 12px;
}

.balance-row > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.balance-row span {
  font-size: 10px;
  color: #8d948c;
}

.balance-row strong {
  font-size: 14px;
  font-weight: 750;
  color: #36443b;
  font-variant-numeric: tabular-nums;
}

.balance-row.low strong {
  color: #c24a2c;
}

.recharge-link {
  font-size: 11px;
  font-weight: 700;
  color: var(--brand);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.balance-alert {
  margin-bottom: 12px;
}

.checkout-btn {
  width: 100%;
  height: 46px;
  font-size: 14px;
}

.summary-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin: 13px 0 0;
  color: #a49a8b;
  font-size: 10px;
}

.pickup-card {
  display: flex;
  gap: 12px;
  margin-top: 14px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #edf4ef;
  border: 1px solid #dbe9df;
}

.pickup-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #d9eadf;
  color: #2f7550;
}

.pickup-card strong {
  font-size: 12px;
  color: #295c40;
}

.pickup-card p {
  margin: 3px 0 0;
  color: #63806d;
  font-size: 10px;
  line-height: 1.7;
}

.confirm-block {
  padding-top: 2px;
}

.confirm-stall {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f4ed;
  margin-bottom: 14px;
}

.confirm-label {
  color: #8d948c;
  font-size: 11px;
}

.confirm-stall strong {
  font-size: 13px;
  color: #36443b;
}

.confirm-items {
  max-height: 220px;
  overflow-y: auto;
  padding: 2px 2px 6px;
}

.confirm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0e8dc;
}

.confirm-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #39463e;
}

.confirm-name small {
  margin-left: 5px;
  color: #a49a8b;
  font-size: 10px;
}

.confirm-qty {
  color: #8d948c;
  font-size: 11px;
}

.confirm-amount {
  width: 72px;
  text-align: right;
  font-size: 12px;
  font-weight: 700;
  color: #36443b;
}

.confirm-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 14px 2px 4px;
}

.confirm-total span {
  color: #6f7a72;
  font-size: 12px;
}

.confirm-total strong {
  color: var(--brand);
  font-size: 24px;
  font-weight: 850;
}

.remark-form {
  margin-top: 12px;
}

.remark-form :deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 11px;
  color: #6f7a72;
}

.confirm-balance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 9px;
  background: #f7f4ed;
  color: #6f7a72;
  font-size: 11px;
}

.danger-text {
  color: #c24a2c;
  font-weight: 700;
}

.order-result {
  text-align: center;
  padding: 6px 8px 2px;
}

.result-badge {
  width: 62px;
  height: 62px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #a06a25;
  background: #fbf0dc;
}

.result-badge.paid {
  color: #2f7a50;
  background: #e4f3e9;
}

.order-result h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--ink);
}

.result-sub {
  margin: 7px 0 16px;
  color: #8b948c;
  font-size: 11px;
}

.result-sub strong {
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}

.pickup-code {
  padding: 14px;
  border-radius: 12px;
  background: #f7f2e8;
  border: 1px dashed #ddcfb8;
  margin-bottom: 14px;
}

.pickup-code small {
  display: block;
  font-size: 8px;
  letter-spacing: 0.2em;
  color: #a2947f;
}

.pickup-code strong {
  display: block;
  margin-top: 4px;
  color: #b8461f;
  font-size: 30px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.result-rows {
  text-align: left;
  border-top: 1px solid #f0e8dc;
}

.result-rows > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 2px;
  border-bottom: 1px solid #f7f1e8;
  font-size: 11px;
}

.result-rows span {
  color: #8d948c;
}

.result-rows strong {
  color: #36443b;
  font-weight: 650;
}

@media (max-width: 1000px) {
  .cart-layout {
    grid-template-columns: 1fr;
  }

  .summary {
    position: static;
  }
}

@media (max-width: 640px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  h1 {
    font-size: 25px;
  }

  .cart-item {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .item-cover {
    width: 64px;
    height: 64px;
  }

  .item-actions {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-left: 78px;
  }

  .summary-card {
    padding: 18px;
  }
}

</style>
