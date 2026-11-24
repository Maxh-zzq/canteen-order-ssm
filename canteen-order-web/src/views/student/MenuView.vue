<template>
  <div class="page-shell menu-page">
    <section class="menu-hero">
      <div class="hero-deco deco-one"></div>
      <div class="hero-deco deco-two"></div>

      <div class="hero-main">
        <span class="hero-eyebrow">TODAY'S MENU · {{ dateLabel }}</span>
        <h1>{{ greeting() }}，{{ auth.displayName }}<br /><em>今天想吃点什么？</em></h1>
        <p>今日菜单已更新，售完即止。支持校园卡余额支付，下单后请留意取餐提醒。</p>

        <div class="hero-search">
          <el-input
            v-model.trim="keyword"
            size="large"
            clearable
            placeholder="搜索菜品、档口，例如「红烧肉」"
            :prefix-icon="Search"
          />
        </div>
      </div>

      <div class="hero-side">
        <div class="hero-stat">
          <strong>{{ menus.length }}</strong>
          <span>道菜品在售</span>
        </div>
        <div class="hero-stat">
          <strong>{{ stalls.length }}</strong>
          <span>个档口营业</span>
        </div>
        <RouterLink to="/orders" class="hero-link">
          查看我的订单
          <el-icon><ArrowRight /></el-icon>
        </RouterLink>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <span class="filter-label">分类</span>
        <div class="chip-scroll">
          <button
            type="button"
            class="chip"
            :class="{ active: categoryId === 0 }"
            @click="categoryId = 0"
          >
            全部
            <small>{{ menus.length }}</small>
          </button>
          <button
            v-for="category in categories"
            :key="category.id"
            type="button"
            class="chip"
            :class="{ active: categoryId === category.id }"
            @click="categoryId = category.id"
          >
            {{ category.name }}
            <small>{{ categoryCount(category.id) }}</small>
          </button>
        </div>
      </div>

      <div class="filter-row">
        <span class="filter-label">档口</span>
        <div class="chip-scroll">
          <button
            type="button"
            class="chip stall-chip"
            :class="{ active: stallId === 0 }"
            @click="stallId = 0"
          >
            全部档口
          </button>
          <button
            v-for="stall in stalls"
            :key="stall.id"
            type="button"
            class="chip stall-chip"
            :class="{ active: stallId === stall.id }"
            @click="stallId = stall.id"
          >
            {{ stall.name }}
            <small>{{ stall.floor }} {{ stall.windowNo }}</small>
          </button>
        </div>
      </div>
    </section>

    <div class="section-head">
      <div>
        <h2>{{ activeStallName || '今日供应' }}</h2>
        <p>
          {{ filteredMenus.length }} 道菜
          <template v-if="keyword"> · 搜索「{{ keyword }}」</template>
          <template v-if="hasFilter"> · <button type="button" class="clear-link" @click="clearFilters">清除筛选</button></template>
        </p>
      </div>
      <div class="head-actions">
        <span class="tip"><el-icon><Clock /></el-icon>售完即止</span>
        <el-button text class="refresh-btn" :loading="loading" @click="loadData">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="dish-grid" aria-busy="true">
      <div v-for="index in 8" :key="index" class="skeleton-card">
        <div class="skeleton-cover shimmer"></div>
        <div class="skeleton-body">
          <span class="shimmer w40"></span>
          <span class="shimmer w70"></span>
          <span class="shimmer w30"></span>
        </div>
      </div>
    </div>

    <div v-else-if="filteredMenus.length" class="dish-grid">
      <DishCard
        v-for="menu in filteredMenus"
        :key="menu.menuId"
        :menu="menu"
        @add="addToCart"
      />
    </div>

    <div v-else class="empty-wrap">
      <EmptyState
        title="没有找到匹配的菜品"
        description="换个关键词，或者看看今日其他档口的供应。"
      >
        <el-button type="primary" plain round @click="clearFilters">查看全部菜品</el-button>
      </EmptyState>
    </div>

    <Transition name="cart-rise">
      <div v-if="cart.totalQuantity > 0" class="cart-float">
        <div class="cart-float-info">
          <span class="cart-badge">{{ cart.totalQuantity }}</span>
          <div>
            <strong>{{ money(cart.totalAmount) }}</strong>
            <small>已选 {{ cart.distinctCount }} 种菜品</small>
          </div>
        </div>
        <RouterLink to="/cart" class="cart-float-btn">
          去结算
          <el-icon><ArrowRight /></el-icon>
        </RouterLink>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Clock, Refresh, Search } from '@element-plus/icons-vue'
import DishCard from '@/components/DishCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getCategories, getStalls, getTodayMenus } from '@/api/dish'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { CategoryVO, MenuVO, StallVO } from '@/types'
import { greeting, money } from '@/utils/format'

const auth = useAuthStore()
const cart = useCartStore()

const categories = ref<CategoryVO[]>([])
const stalls = ref<StallVO[]>([])
const menus = ref<MenuVO[]>([])
const loading = ref(true)
const categoryId = ref(0)
const stallId = ref(0)
const keyword = ref('')

const dateLabel = computed(() => {
  const d = dayjs()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.day()]
  return `${d.format('M月D日')} · ${week}`
})

const activeStallName = computed(() => stalls.value.find((item) => item.id === stallId.value)?.name || '')

const hasFilter = computed(() => categoryId.value !== 0 || stallId.value !== 0 || keyword.value !== '')

const filteredMenus = computed(() => {
  const kw = keyword.value.toLowerCase()
  return menus.value.filter((menu) => {
    if (categoryId.value !== 0 && menu.categoryId !== categoryId.value) return false
    if (stallId.value !== 0 && menu.stallId !== stallId.value) return false
    if (kw && !menu.dishName.toLowerCase().includes(kw) && !menu.stallName.toLowerCase().includes(kw)) {
      return false
    }
    return true
  })
})

function categoryCount(id: number) {
  return menus.value.filter((menu) => menu.categoryId === id).length
}

function clearFilters() {
  categoryId.value = 0
  stallId.value = 0
  keyword.value = ''
}

async function loadData() {
  loading.value = true
  try {
    const [categoryList, stallList, menuList] = await Promise.all([
      getCategories(),
      getStalls(),
      getTodayMenus()
    ])
    categories.value = categoryList
    stalls.value = stallList
    menus.value = [...menuList].sort((a, b) => {
      if (a.stallId !== b.stallId) return a.stallId - b.stallId
      return b.sales - a.sales
    })
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function currentCartStallId(): number | null {
  for (const item of cart.items) {
    const match = menus.value.find((menu) => menu.dishId === item.dishId)
    if (match) return match.stallId
  }
  return null
}

function stallNameOf(id: number) {
  return stalls.value.find((item) => item.id === id)?.name || '其他档口'
}

async function addToCart(menu: MenuVO) {
  try {
    const currentStall = cart.items.length ? currentCartStallId() : null
    if (currentStall && currentStall !== menu.stallId) {
      await ElMessageBox.confirm(
        `购物车中已有「${stallNameOf(currentStall)}」的菜品。每次只能在一个档口下单，继续添加将清空原购物车。`,
        '更换档口',
        {
          confirmButtonText: '清空并添加',
          cancelButtonText: '保留购物车',
          type: 'warning'
        }
      )
      await cart.clear()
    }

    await cart.add({ dishId: menu.dishId, quantity: 1 })
    ElMessage.success(`已加入购物车：${menu.dishName}`)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

onMounted(() => {
  loadData()
  cart.fetchCart().catch(() => undefined)
})
</script>

<style scoped>
.menu-page {
  position: relative;
}

.menu-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 34px;
  padding: 38px 42px 34px;
  border-radius: 24px;
  color: #fff;
  background:
    radial-gradient(circle at 85% 0%, rgba(228, 137, 92, 0.24) 0, transparent 34%),
    radial-gradient(circle at 8% 100%, rgba(84, 148, 111, 0.35) 0, transparent 36%),
    linear-gradient(132deg, #1b4938 0%, #143528 65%, #102a20 100%);
  box-shadow: 0 22px 50px rgba(20, 48, 37, 0.18);
}

.hero-deco {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.deco-one {
  width: 220px;
  height: 220px;
  right: -70px;
  top: -90px;
  border: 26px solid rgba(255, 255, 255, 0.07);
}

.deco-two {
  width: 110px;
  height: 110px;
  right: 130px;
  bottom: -60px;
  background: rgba(216, 87, 42, 0.18);
  filter: blur(8px);
}

.hero-main {
  position: relative;
  z-index: 2;
  flex: 1;
  min-width: 0;
}

.hero-eyebrow {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.52);
}

h1 {
  margin: 12px 0 0;
  font-size: clamp(30px, 3vw, 42px);
  line-height: 1.22;
  font-weight: 850;
  letter-spacing: -0.04em;
}

h1 em {
  font-style: normal;
  color: #f0a781;
}

.menu-hero p {
  margin: 12px 0 0;
  max-width: 560px;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.63);
}

.hero-search {
  max-width: 470px;
  margin-top: 24px;
}

.hero-search :deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.hero-search :deep(.el-input__inner) {
  color: #fff;
}

.hero-search :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.42);
}

.hero-search :deep(.el-input__prefix-inner),
.hero-search :deep(.el-input__suffix-inner) {
  color: rgba(255, 255, 255, 0.6);
}

.hero-side {
  position: relative;
  z-index: 2;
  width: 176px;
  flex-shrink: 0;
  padding-left: 26px;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-stat strong {
  display: block;
  font-size: 30px;
  line-height: 1;
  font-weight: 850;
  letter-spacing: -0.03em;
}

.hero-stat span {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.hero-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: auto;
  font-size: 12px;
  font-weight: 650;
  color: rgba(255, 255, 255, 0.82);
}

.hero-link:hover {
  color: #fff;
}

.filter-panel {
  margin-top: 18px;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 13px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 8px 22px rgba(47, 34, 18, 0.05);
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.filter-label {
  flex-shrink: 0;
  width: 32px;
  padding-top: 7px;
  font-size: 12px;
  font-weight: 750;
  color: #55635a;
}

.chip-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: thin;
}

.chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 13px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: #f6f2ea;
  color: #5b675f;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
  transition: all 0.18s ease;
}

.chip small {
  font-size: 10px;
  font-weight: 600;
  color: #a0988b;
}

.chip:hover {
  border-color: #e6c3b0;
  color: var(--brand-dark);
}

.chip.active {
  color: #fff;
  background: var(--brand);
  border-color: var(--brand);
  box-shadow: 0 6px 14px rgba(216, 87, 42, 0.2);
}

.chip.active small {
  color: rgba(255, 255, 255, 0.75);
}

.stall-chip small {
  font-weight: 500;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin: 30px 0 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 23px;
  font-weight: 850;
  letter-spacing: -0.025em;
}

.section-head p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--muted);
}

.clear-link {
  border: none;
  background: none;
  padding: 0;
  color: var(--brand);
  font-size: inherit;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 999px;
  color: #8b6a3f;
  background: #faf1df;
  font-size: 10px;
}

.refresh-btn {
  color: #67736b;
  font-size: 12px;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(218px, 1fr));
  gap: 18px;
}

.skeleton-card {
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--line);
}

.skeleton-cover {
  height: 158px;
  background: #efe7da;
}

.skeleton-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.shimmer {
  display: block;
  height: 11px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f1eadf 25%, #faf6ee 37%, #f1eadf 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.w30 {
  width: 30%;
  height: 22px;
}

.w40 {
  width: 40%;
}

.w70 {
  width: 70%;
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.empty-wrap {
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
}

.cart-float {
  position: fixed;
  left: 50%;
  bottom: 26px;
  transform: translateX(-50%);
  z-index: 45;
  width: min(520px, calc(100vw - 32px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px 10px 14px;
  border-radius: 16px;
  background: rgba(24, 52, 40, 0.96);
  color: #fff;
  box-shadow: 0 18px 44px rgba(20, 42, 33, 0.34);
  backdrop-filter: blur(12px);
}

.cart-float-info {
  display: flex;
  align-items: center;
  gap: 11px;
}

.cart-badge {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 800;
  color: #fff;
  background: var(--brand);
}

.cart-float-info strong {
  display: block;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
}

.cart-float-info small {
  display: block;
  margin-top: 2px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.55);
}

.cart-float-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 10px 20px;
  border-radius: 11px;
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  font-weight: 750;
  transition: background 0.18s ease;
}

.cart-float-btn:hover {
  background: #e7683a;
}

.cart-rise-enter-active,
.cart-rise-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.cart-rise-enter-from,
.cart-rise-leave-to {
  opacity: 0;
  transform: translate(-50%, 12px);
}

@media (max-width: 900px) {
  .menu-hero {
    flex-direction: column;
    gap: 22px;
    padding: 26px 22px;
    border-radius: 18px;
  }

  h1 {
    font-size: 30px;
  }

  .hero-side {
    width: 100%;
    padding: 16px 0 0;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.12);
    flex-direction: row;
    align-items: center;
    gap: 28px;
  }

  .hero-stat strong {
    font-size: 24px;
  }

  .hero-link {
    margin-left: auto;
  }

  .filter-panel {
    padding: 14px 14px;
  }

  .filter-row {
    flex-direction: column;
    gap: 7px;
  }

  .filter-label {
    padding-top: 0;
  }

  .section-head {
    margin-top: 22px;
  }

  .section-head h2 {
    font-size: 20px;
  }

  .dish-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .cart-float {
    bottom: 82px;
  }
}

@media (max-width: 380px) {
  .dish-grid {
    grid-template-columns: 1fr;
  }
}
</style>
