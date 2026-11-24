<template>
  <div class="dashboard">
    <div class="dash-toolbar">
      <div>
        <h2>今日经营概览</h2>
        <p>{{ todayLabel }} · 数据实时汇总自订单与评价</p>
      </div>
      <div class="toolbar-actions">
        <el-radio-group v-model="days" size="small">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" @click="loadAll">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <article class="stat-card revenue">
        <div class="stat-top">
          <span>今日营业额</span>
          <el-icon><Wallet /></el-icon>
        </div>
        <strong>{{ money(overview.todayRevenue) }}</strong>
        <small>已支付订单合计</small>
      </article>

      <article class="stat-card orders">
        <div class="stat-top">
          <span>今日订单</span>
          <el-icon><Tickets /></el-icon>
        </div>
        <strong>{{ overview.todayOrderCount }}</strong>
        <small>含待支付与已完成</small>
      </article>

      <article class="stat-card pending">
        <div class="stat-top">
          <span>待处理订单</span>
          <el-icon><Bell /></el-icon>
        </div>
        <strong>{{ overview.pendingOrders }}</strong>
        <small>待支付 / 待接单 / 备餐中</small>
      </article>

      <article class="stat-card score">
        <div class="stat-top">
          <span>平均评分</span>
          <el-icon><Star /></el-icon>
        </div>
        <strong>{{ Number(overview.avgScore || 0).toFixed(1) }}</strong>
        <small>基于学生评价</small>
      </article>

      <article class="stat-card users">
        <div class="stat-top">
          <span>注册用户</span>
          <el-icon><User /></el-icon>
        </div>
        <strong>{{ overview.totalUsers }}</strong>
        <small>学生 / 员工 / 管理员</small>
      </article>
    </div>

    <div class="chart-grid">
      <section class="chart-panel">
        <div class="panel-head">
          <div>
            <h3>销售趋势</h3>
            <p>营业额与订单量的变化</p>
          </div>
          <span class="panel-tag">近 {{ days }} 天</span>
        </div>
        <div class="chart-wrap">
          <div ref="trendRef" class="chart-box"></div>
          <div v-if="!hasTrendData" class="chart-empty">
            <EmptyState title="暂无销售数据" description="产生已支付订单后，这里会展示营业额与订单趋势。" />
          </div>
        </div>
      </section>

      <section class="rank-panel">
        <div class="panel-head">
          <div>
            <h3>热销菜品</h3>
            <p>按销量排序的前 8 名</p>
          </div>
          <span class="panel-tag">近 {{ days }} 天</span>
        </div>

        <div v-if="topDishes.length" class="rank-list">
          <div v-for="(dish, index) in topDishes" :key="dish.dishName" class="rank-item">
            <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <div class="rank-main">
              <div class="rank-name">
                <span>{{ dish.dishName }}</span>
                <small>{{ money(dish.amount) }}</small>
              </div>
              <div class="rank-track">
                <i :style="{ width: rankWidth(dish.quantity) }"></i>
              </div>
            </div>
            <strong class="rank-qty">{{ dish.quantity }} 份</strong>
          </div>
        </div>

        <EmptyState v-else title="暂无销售数据" description="有订单完成后，这里会展示热销菜品。" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { Bell, Refresh, Star, Tickets, User, Wallet } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { getOverview, getSalesTrend, getTopDishes } from '@/api/admin'
import type { SalesTrendVO, StatsOverviewVO, TopDishVO } from '@/types'
import { money } from '@/utils/format'

const overview = ref<StatsOverviewVO>({
  todayOrderCount: 0,
  todayRevenue: 0,
  totalUsers: 0,
  avgScore: 0,
  pendingOrders: 0
})
const trend = ref<SalesTrendVO[]>([])
const topDishes = ref<TopDishVO[]>([])
const days = ref(7)
const loading = ref(false)
const trendRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const hasTrendData = computed(() =>
  trend.value.some((item) => Number(item.revenue) > 0 || Number(item.orderCount) > 0)
)

const todayLabel = computed(() => dayjs().format('YYYY年M月D日'))

function rankWidth(quantity: number) {
  const max = Math.max(...topDishes.value.map((item) => item.quantity), 1)
  return `${Math.max(8, Math.round((quantity / max) * 100))}%`
}

function renderChart() {
  if (!trendRef.value) return
  if (!chart) {
    chart = echarts.init(trendRef.value)
  }

  const dates = trend.value.map((item) => dayjs(item.statDate).format('MM-DD'))

  chart.setOption({
    color: ['#d8572a', '#2f8f5b'],
    grid: { left: 12, right: 16, top: 34, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(24, 44, 34, 0.94)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 11 },
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(216, 87, 42, 0.06)' } }
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#7b857d', fontSize: 10 }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e8e0d3' } },
      axisTick: { show: false },
      axisLabel: { color: '#92998f', fontSize: 10, margin: 12 }
    },
    yAxis: [
      {
        type: 'value',
        name: '营业额',
        nameTextStyle: { color: '#a39a8b', fontSize: 9, padding: [0, 0, 4, 0] },
        axisLabel: { color: '#92998f', fontSize: 9 },
        splitLine: { lineStyle: { color: '#f2ece2', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '订单',
        nameTextStyle: { color: '#a39a8b', fontSize: 9, padding: [0, 0, 4, 0] },
        axisLabel: { color: '#92998f', fontSize: 9 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '营业额',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#e77b4f' },
            { offset: 1, color: '#c9491f' }
          ])
        },
        data: trend.value.map((item) => Number(item.revenue))
      },
      {
        name: '订单量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2 },
        itemStyle: { color: '#2f8f5b' },
        data: trend.value.map((item) => item.orderCount)
      }
    ]
  })
}

async function loadAll() {
  loading.value = true
  try {
    const [overviewData, trendData, topData] = await Promise.all([
      getOverview(),
      getSalesTrend(days.value),
      getTopDishes(days.value, 8)
    ])
    overview.value = overviewData
    trend.value = trendData
    topDishes.value = topData
    await nextTick()
    renderChart()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  loading.value = true
  try {
    const [trendData, topData] = await Promise.all([
      getSalesTrend(days.value),
      getTopDishes(days.value, 8)
    ])
    trend.value = trendData
    topDishes.value = topData
    await nextTick()
    renderChart()
  } catch {
    // 忽略
  } finally {
    loading.value = false
  }
}

watch(days, loadTrend)

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dash-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.dash-toolbar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.dash-toolbar p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 16px 17px;
  border-radius: 15px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 6px 18px rgba(47, 34, 18, 0.05);
}

.stat-card::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -32px;
  width: 82px;
  height: 82px;
  border-radius: 50%;
  opacity: 0.07;
}

.stat-card.revenue::after {
  background: #d8572a;
}

.stat-card.orders::after {
  background: #2f8f5b;
}

.stat-card.pending::after {
  background: #d98a24;
}

.stat-card.score::after {
  background: #486f9e;
}

.stat-card.users::after {
  background: #7c6a52;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #8d948c;
  font-size: 10px;
}

.stat-card.revenue .stat-top .el-icon {
  color: #c9491f;
}

.stat-card.orders .stat-top .el-icon {
  color: #2f8f5b;
}

.stat-card.pending .stat-top .el-icon {
  color: #c2811c;
}

.stat-card.score .stat-top .el-icon {
  color: #486f9e;
}

.stat-card.users .stat-top .el-icon {
  color: #7c6a52;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: var(--ink);
  font-size: 24px;
  line-height: 1.1;
  font-weight: 850;
  letter-spacing: -0.035em;
  font-variant-numeric: tabular-nums;
}

.stat-card small {
  display: block;
  margin-top: 5px;
  color: #a49a8b;
  font-size: 9px;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 1fr);
  gap: 16px;
}

.chart-panel,
.rank-panel {
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 6px 18px rgba(47, 34, 18, 0.05);
  padding: 18px 20px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.panel-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 10px;
}

.panel-tag {
  padding: 3px 8px;
  border-radius: 999px;
  background: #f7f1e6;
  color: #9a6a3a;
  font-size: 9px;
}

.chart-box {
  width: 100%;
  height: 318px;
  margin-top: 8px;
}

.chart-wrap {
  position: relative;
}

.chart-empty {
  position: absolute;
  inset: 38px 0 0;
  display: grid;
  place-items: center;
  background: #fff;
}

.chart-empty :deep(.empty-state) {
  padding-top: 20px;
  padding-bottom: 20px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 9px;
}

.rank-no {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 6px;
  display: grid;
  place-items: center;
  background: #f3efe8;
  color: #9a9285;
  font-size: 10px;
  font-weight: 800;
}

.rank-no.top {
  color: #fff;
  background: var(--brand);
}

.rank-main {
  flex: 1;
  min-width: 0;
}

.rank-name {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.rank-name span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #38463d;
  font-size: 11px;
  font-weight: 650;
}

.rank-name small {
  color: #a49a8b;
  font-size: 9px;
  font-variant-numeric: tabular-nums;
}

.rank-track {
  height: 5px;
  border-radius: 3px;
  background: #f4efe6;
  overflow: hidden;
}

.rank-track i {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #efb08d, #d8572a);
}

.rank-qty {
  width: 46px;
  flex-shrink: 0;
  text-align: right;
  color: #68756c;
  font-size: 10px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .dash-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-box {
    height: 260px;
  }
}
</style>
