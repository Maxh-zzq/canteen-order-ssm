<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div>
        <h2>每日菜单</h2>
        <p>按日期发布供应菜品，并实时监控库存与销量。</p>
      </div>
      <div class="toolbar-right">
        <el-date-picker
          v-model="serveDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          :clearable="false"
          class="date-picker"
          @change="load"
        />
        <el-button type="primary" @click="openPublish">
          <el-icon><Plus /></el-icon>发布菜单
        </el-button>
      </div>
    </div>

    <div class="menu-stats">
      <div class="mini-stat">
        <span>供应菜品</span>
        <strong>{{ menus.length }}</strong>
        <small>道</small>
      </div>
      <div class="mini-stat">
        <span>总库存</span>
        <strong>{{ totalStock }}</strong>
        <small>份</small>
      </div>
      <div class="mini-stat">
        <span>已售出</span>
        <strong>{{ totalSold }}</strong>
        <small>份</small>
      </div>
      <div class="mini-stat">
        <span>剩余可售</span>
        <strong>{{ totalLeft }}</strong>
        <small>份</small>
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="menus" v-loading="loading" row-key="menuId">
        <el-table-column label="菜品" min-width="220">
          <template #default="{ row }">
            <div class="dish-cell">
              <div class="dish-thumb">
                <img v-if="row.coverUrl" :src="assetUrl(row.coverUrl)" :alt="row.dishName" />
                <span v-else>{{ initialOf(row.dishName) }}</span>
              </div>
              <div>
                <strong>{{ row.dishName }}</strong>
                <small>{{ row.stallName }} · {{ row.categoryName }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">
            <strong class="price-cell">{{ money(row.price) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="总库存" width="90" align="center">
          <template #default="{ row }">{{ row.stockTotal }}</template>
        </el-table-column>

        <el-table-column label="剩余" width="90" align="center">
          <template #default="{ row }">
            <span :class="['stock-cell', { low: row.stockLeft <= 10, zero: row.stockLeft <= 0 }]">
              {{ row.stockLeft }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="已售" width="80" align="center">
          <template #default="{ row }">{{ row.sales }}</template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.stockLeft > 0 && row.status === 1 ? 'success' : 'info'" effect="light" size="small" round>
              {{ row.stockLeft > 0 && row.status === 1 ? '供应中' : '已售罄' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openStock(row)">调整库存</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="当日暂无菜单" description="点击右上角「发布菜单」，选择菜品即可上架。" />
        </template>
      </el-table>
    </div>

    <el-dialog
      v-model="publishVisible"
      title="发布每日菜单"
      width="560px"
      :close-on-click-modal="false"
    >
      <div class="publish-head">
        <el-form-item label="供应日期">
          <el-date-picker
            v-model="publishDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            :clearable="false"
            class="full"
            @change="syncPublishSelection"
          />
        </el-form-item>
      </div>

      <div class="publish-label">
        <strong>选择菜品</strong>
        <span>已选 {{ selectedDishIds.length }} 道</span>
      </div>

      <el-select
        v-model="selectedDishIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="从菜品库中选择要供应的菜品"
        class="full"
        :max-collapse-tags="4"
      >
        <el-option-group v-for="group in dishGroups" :key="group.stallId" :label="group.stallName">
          <el-option
            v-for="dish in group.dishes"
            :key="dish.id"
            :label="`${dish.name} · ${money(dish.price)}`"
            :value="dish.id"
          />
        </el-option-group>
      </el-select>

      <p class="publish-note">
        <el-icon><InfoFilled /></el-icon>
        发布后学生端将看到所选菜品；库存按菜品每日库存初始化。
      </p>

      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublish">
          发布菜单
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="调整库存" width="400px">
      <div v-if="stockTarget" class="stock-dialog">
        <div class="stock-dish">{{ stockTarget.dishName }}</div>
        <el-form label-position="top">
          <el-form-item label="总库存（份）">
            <el-input-number v-model="stockTotal" :min="stockTarget.sales" :max="9999" controls-position="right" class="full" />
          </el-form-item>
        </el-form>
        <p class="stock-hint">
          已售 {{ stockTarget.sales }} 份，调整后剩余
          <strong>{{ Math.max(stockTotal - stockTarget.sales, 0) }}</strong> 份。
        </p>
      </div>

      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="stockSaving" @click="saveStock">保存库存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { InfoFilled, Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getAdminDishes,
  getAdminMenus,
  publishMenu,
  updateMenuStock
} from '@/api/admin'
import type { DishVO, MenuVO } from '@/types'
import { assetUrl, initialOf, money, todayString } from '@/utils/format'

const serveDate = ref(todayString())
const menus = ref<MenuVO[]>([])
const loading = ref(false)
const publishVisible = ref(false)
const publishing = ref(false)
const publishDate = ref(todayString())
const allDishes = ref<DishVO[]>([])
const selectedDishIds = ref<number[]>([])
const stockVisible = ref(false)
const stockSaving = ref(false)
const stockTarget = ref<MenuVO | null>(null)
const stockTotal = ref(0)

const totalStock = computed(() => menus.value.reduce((sum, item) => sum + item.stockTotal, 0))
const totalSold = computed(() => menus.value.reduce((sum, item) => sum + item.sales, 0))
const totalLeft = computed(() => menus.value.reduce((sum, item) => sum + item.stockLeft, 0))

const dishGroups = computed(() => {
  const map = new Map<number, { stallId: number; stallName: string; dishes: DishVO[] }>()
  for (const dish of allDishes.value) {
    if (dish.status !== 1) continue
    if (!map.has(dish.stallId)) {
      map.set(dish.stallId, { stallId: dish.stallId, stallName: dish.stallName, dishes: [] })
    }
    map.get(dish.stallId)!.dishes.push(dish)
  }
  return Array.from(map.values())
})

async function load() {
  loading.value = true
  try {
    menus.value = await getAdminMenus(serveDate.value)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

async function loadDishes() {
  try {
    const data = await getAdminDishes({ pageNum: 1, pageSize: 200 })
    allDishes.value = data.list
  } catch {
    // 忽略
  }
}

async function openPublish() {
  publishDate.value = serveDate.value || todayString()
  selectedDishIds.value = menus.value.map((item) => item.dishId)
  publishVisible.value = true
  if (allDishes.value.length === 0) await loadDishes()
}

async function syncPublishSelection(value: string | null) {
  if (!value) return
  try {
    const list = await getAdminMenus(value)
    selectedDishIds.value = list.map((item) => item.dishId)
  } catch {
    selectedDishIds.value = []
  }
}

async function doPublish() {
  if (selectedDishIds.value.length === 0) {
    ElMessage.warning('请至少选择一道菜品')
    return
  }

  publishing.value = true
  try {
    await publishMenu({
      serveDate: publishDate.value,
      dishIds: selectedDishIds.value
    })
    ElMessage.success('菜单已发布')
    publishVisible.value = false
    if (publishDate.value === serveDate.value) {
      await load()
    } else {
      serveDate.value = publishDate.value
      await load()
    }
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    publishing.value = false
  }
}

function openStock(row: MenuVO) {
  stockTarget.value = row
  stockTotal.value = row.stockTotal
  stockVisible.value = true
}

async function saveStock() {
  if (!stockTarget.value) return
  if (stockTotal.value < stockTarget.value.sales) {
    ElMessage.warning('总库存不能小于已售数量')
    return
  }

  stockSaving.value = true
  try {
    await updateMenuStock(stockTarget.value.menuId, stockTotal.value)
    ElMessage.success('库存已更新')
    stockVisible.value = false
    await load()
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    stockSaving.value = false
  }
}

onMounted(() => {
  load()
  loadDishes()
})
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
  flex-wrap: wrap;
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

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-picker {
  width: 160px;
}

.menu-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.mini-stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 13px 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--line);
}

.mini-stat span {
  color: #8d948c;
  font-size: 10px;
}

.mini-stat strong {
  margin-left: auto;
  color: var(--ink);
  font-size: 20px;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.mini-stat small {
  color: #a0988b;
  font-size: 9px;
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

.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-thumb {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f6e6cf, #e8c39e);
  color: rgba(92, 54, 22, 0.7);
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 19px;
  font-weight: 800;
}

.dish-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-cell strong {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.dish-cell small {
  display: block;
  margin-top: 2px;
  color: #a0988b;
  font-size: 9px;
}

.price-cell {
  color: var(--brand);
  font-size: 13px;
  font-weight: 800;
}

.stock-cell {
  font-size: 13px;
  font-weight: 750;
  color: #3b7d55;
  font-variant-numeric: tabular-nums;
}

.stock-cell.low {
  color: #c2811c;
}

.stock-cell.zero {
  color: #b45443;
}

.publish-head {
  margin-bottom: 16px;
}

.publish-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
}

.publish-label strong {
  font-size: 11px;
  color: #536159;
}

.publish-label span {
  color: #a0988b;
  font-size: 10px;
}

.full {
  width: 100%;
}

.publish-note {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin: 12px 0 0;
  padding: 9px 11px;
  border-radius: 8px;
  background: #faf6ee;
  color: #8d8271;
  font-size: 10px;
  line-height: 1.7;
}

.stock-dish {
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: 9px;
  background: #faf6ee;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
}

.stock-hint {
  margin: 0;
  color: #8d948c;
  font-size: 10px;
}

.stock-hint strong {
  color: var(--brand);
}

@media (max-width: 760px) {
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .menu-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .date-picker {
    width: 100%;
  }
}
</style>
