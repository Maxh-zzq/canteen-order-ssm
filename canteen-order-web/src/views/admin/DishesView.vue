<template>
  <div class="admin-page">
    <div class="toolbar panel">
      <div class="filters">
        <el-input
          v-model.trim="query.keyword"
          clearable
          placeholder="搜索菜品名称"
          :prefix-icon="Search"
          class="filter-input"
          @keyup.enter="load(1)"
          @clear="load(1)"
        />
        <el-select v-model="query.categoryId" clearable placeholder="全部分类" class="filter-select" @change="load(1)">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.stallId" clearable placeholder="全部档口" class="filter-select" @change="load(1)">
          <el-option v-for="stall in stalls" :key="stall.id" :label="stall.name" :value="stall.id" />
        </el-select>
        <el-button @click="load(1)">查询</el-button>
        <el-button text @click="resetFilters">重置</el-button>
      </div>

      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新增菜品
      </el-button>
    </div>

    <div class="table-panel">
      <el-table :data="dishes" v-loading="loading" row-key="id" class="admin-table">
        <el-table-column label="菜品" min-width="220">
          <template #default="{ row }">
            <div class="dish-cell">
              <div class="dish-thumb">
                <img v-if="row.coverUrl" :src="assetUrl(row.coverUrl)" :alt="row.name" />
                <span v-else>{{ initialOf(row.name) }}</span>
              </div>
              <div class="dish-copy">
                <strong>{{ row.name }}</strong>
                <small>{{ row.description || '暂无描述' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="档口 / 分类" min-width="150">
          <template #default="{ row }">
            <div class="stack-cell">
              <span>{{ row.stallName }}</span>
              <small>{{ row.categoryName }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">
            <strong class="price-cell">{{ money(row.price) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="日库存" width="90" align="center">
          <template #default="{ row }">{{ row.dailyStock }}</template>
        </el-table-column>

        <el-table-column label="销量" width="80" align="center">
          <template #default="{ row }">{{ row.sales }}</template>
        </el-table-column>

        <el-table-column label="评分" width="90" align="center">
          <template #default="{ row }">
            <span class="score-cell">
              <el-icon><Star /></el-icon>
              {{ Number(row.avgScore || 0).toFixed(1) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small" round>
              {{ row.status === 1 ? '在售' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState title="暂无菜品" description="新增菜品后，即可发布到每日菜单。" />
        </template>
      </el-table>

      <div class="table-footer">
        <span>共 {{ total }} 条菜品</span>
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

    <el-drawer
      v-model="drawerVisible"
      :title="editingId ? '编辑菜品' : '新增菜品'"
      size="540px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dish-form">
        <el-form-item label="菜品图片">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :http-request="uploadCover"
            :before-upload="beforeUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <div v-if="form.coverUrl" class="cover-preview">
              <img :src="assetUrl(form.coverUrl)" alt="菜品封面" />
              <span class="cover-change">点击更换</span>
            </div>
            <div v-else class="cover-empty">
              <el-icon><Plus /></el-icon>
              <span>上传封面图</span>
              <small>JPG / PNG / WEBP，≤ 5MB</small>
            </div>
          </el-upload>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="菜品名称" prop="name">
            <el-input v-model.trim="form.name" maxlength="30" placeholder="例如：红烧肉" />
          </el-form-item>
          <el-form-item label="售价（元）" prop="price">
            <el-input-number v-model="form.price" :min="0.01" :max="999" :precision="2" :step="1" controls-position="right" class="full" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="所属档口" prop="stallId">
            <el-select v-model="form.stallId" placeholder="请选择档口" class="full">
              <el-option v-for="stall in stalls" :key="stall.id" :label="stall.name" :value="stall.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" class="full">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="每日库存（份）" prop="dailyStock">
            <el-input-number v-model="form.dailyStock" :min="0" :max="9999" :step="10" controls-position="right" class="full" />
          </el-form-item>
          <el-form-item label="售卖状态">
            <div class="status-line">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
              <span>{{ form.status === 1 ? '在售' : '停用' }}</span>
            </div>
          </el-form-item>
        </div>

        <el-form-item label="菜品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="口味、食材或推荐理由"
          />
        </el-form-item>

        <div class="spec-head">
          <div>
            <strong>规格与加价</strong>
            <small>如「大份 +2 元」，可不配置</small>
          </div>
          <el-button text type="primary" @click="addSpec">
            <el-icon><Plus /></el-icon>添加规格
          </el-button>
        </div>

        <div v-if="form.specs.length" class="spec-list">
          <div v-for="(spec, index) in form.specs" :key="index" class="spec-row">
            <el-input v-model.trim="spec.name" maxlength="20" placeholder="规格名称，如大份" />
            <el-input-number v-model="spec.priceDiff" :min="-99" :max="99" :precision="2" :step="1" controls-position="right" />
            <el-button text type="danger" @click="removeSpec(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <p v-else class="spec-empty">暂未配置规格，菜品将按基础价格售卖。</p>

        <div class="drawer-actions">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存菜品</el-button>
        </div>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { Delete, Plus, Search, Star } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  createDish,
  deleteDish,
  getAdminCategories,
  getAdminDishes,
  getAdminDish,
  getAdminStalls,
  updateDish,
  uploadDishImage
} from '@/api/admin'
import type { CategoryVO, DishPayload, DishSpecVO, DishVO, StallVO } from '@/types'
import { assetUrl, initialOf, money } from '@/utils/format'

const dishes = ref<DishVO[]>([])
const categories = ref<CategoryVO[]>([])
const stalls = ref<StallVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const query = reactive<{
  keyword: string
  categoryId: number | undefined
  stallId: number | undefined
}>({
  keyword: '',
  categoryId: undefined,
  stallId: undefined
})

interface SpecForm {
  name: string
  priceDiff: number
}

const form = reactive({
  stallId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  name: '',
  coverUrl: '' as string | null,
  price: 10,
  description: '',
  dailyStock: 100,
  status: 1,
  specs: [] as SpecForm[]
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入菜品名称', trigger: 'blur' },
    { min: 2, max: 30, message: '名称长度为 2-30 个字符', trigger: 'blur' }
  ],
  price: [{ required: true, message: '请输入售价', trigger: 'change' }],
  stallId: [{ required: true, message: '请选择档口', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  dailyStock: [{ required: true, message: '请输入每日库存', trigger: 'change' }]
}

async function loadOptions() {
  try {
    const [categoryList, stallList] = await Promise.all([getAdminCategories(), getAdminStalls()])
    categories.value = categoryList
    stalls.value = stallList
  } catch {
    // 忽略
  }
}

async function load(page = pageNum.value) {
  loading.value = true
  try {
    const data = await getAdminDishes({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      stallId: query.stallId || undefined,
      pageNum: page,
      pageSize
    })
    dishes.value = data.list
    total.value = data.total
    pageNum.value = page
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  query.keyword = ''
  query.categoryId = undefined
  query.stallId = undefined
  load(1)
}

function resetForm() {
  editingId.value = null
  form.stallId = stalls.value[0]?.id
  form.categoryId = categories.value[0]?.id
  form.name = ''
  form.coverUrl = ''
  form.price = 10
  form.description = ''
  form.dailyStock = 100
  form.status = 1
  form.specs = []
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  drawerVisible.value = true
}

async function openEdit(row: DishVO) {
  resetForm()
  editingId.value = row.id
  drawerVisible.value = true

  let source = row
  try {
    source = await getAdminDish(row.id)
  } catch {
    // 详情接口不可用时回退到列表数据
  }

  form.stallId = source.stallId
  form.categoryId = source.categoryId
  form.name = source.name
  form.coverUrl = source.coverUrl || ''
  form.price = Number(source.price)
  form.description = source.description || ''
  form.dailyStock = source.dailyStock
  form.status = source.status
  form.specs = (source.specs || []).map((spec: DishSpecVO) => ({
    name: spec.name,
    priceDiff: Number(spec.priceDiff)
  }))
}

function addSpec() {
  form.specs.push({ name: '', priceDiff: 0 })
}

function removeSpec(index: number) {
  form.specs.splice(index, 1)
}

function beforeUpload(file: File) {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WEBP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function uploadCover(options: UploadRequestOptions) {
  try {
    const url = await uploadDishImage(options.file)
    form.coverUrl = url
    ElMessage.success('图片上传成功')
  } catch {
    // 错误提示由请求层统一处理
  }
}

function buildPayload(status?: number): DishPayload {
  return {
    stallId: Number(form.stallId),
    categoryId: Number(form.categoryId),
    name: form.name,
    coverUrl: form.coverUrl || null,
    price: Number(form.price),
    description: form.description,
    dailyStock: Number(form.dailyStock),
    status: status ?? form.status,
    specs: form.specs
      .filter((spec) => spec.name.trim())
      .map((spec) => ({ name: spec.name.trim(), priceDiff: Number(spec.priceDiff) }))
  }
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.specs.some((spec) => !spec.name.trim())) {
    ElMessage.warning('请填写完整的规格名称，或删除空白规格')
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await updateDish(editingId.value, buildPayload())
      ElMessage.success('菜品已更新')
    } else {
      await createDish(buildPayload())
      ElMessage.success('菜品已创建')
    }
    drawerVisible.value = false
    await load(editingId.value ? pageNum.value : 1)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: DishVO) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateDish(row.id, {
      stallId: row.stallId,
      categoryId: row.categoryId,
      name: row.name,
      coverUrl: row.coverUrl,
      price: Number(row.price),
      description: row.description,
      dailyStock: row.dailyStock,
      status: nextStatus,
      specs: (row.specs || []).map((spec) => ({ name: spec.name, priceDiff: Number(spec.priceDiff) }))
    })
    ElMessage.success(nextStatus === 1 ? '菜品已上架' : '菜品已下架')
    await load(pageNum.value)
  } catch {
    // 错误提示由请求层统一处理
  }
}

async function remove(row: DishVO) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？删除后不可恢复。`, '删除菜品', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDish(row.id)
    ElMessage.success('菜品已删除')
    await load(pageNum.value)
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(async () => {
  await loadOptions()
  load(1)
})
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
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-input {
  width: 210px;
}

.filter-select {
  width: 145px;
}

.table-panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 15px;
  overflow: hidden;
}

.admin-table {
  width: 100%;
}

.admin-table :deep(.el-table__header th) {
  background: #fbf8f1;
  color: #647068;
  font-size: 11px;
  font-weight: 700;
}

.admin-table :deep(.el-table__row) {
  font-size: 12px;
}

.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-thumb {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 9px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f6e6cf, #e8c39e);
  color: rgba(92, 54, 22, 0.7);
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 21px;
  font-weight: 800;
}

.dish-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-copy {
  min-width: 0;
}

.dish-copy strong {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.dish-copy small {
  display: block;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
  color: #a0988b;
  font-size: 9px;
}

.stack-cell span {
  display: block;
  font-size: 11px;
  color: #4b584f;
}

.stack-cell small {
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

.score-cell {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #b67d2a;
  font-size: 11px;
  font-weight: 650;
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

.dish-form :deep(.el-form-item__label) {
  padding-bottom: 4px;
  color: #536159;
  font-size: 11px;
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

.cover-uploader {
  width: 100%;
}

.cover-preview,
.cover-empty {
  position: relative;
  width: 100%;
  height: 148px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px dashed #ddcfb8;
  background: #fcfaf6;
  display: grid;
  place-items: center;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-change {
  position: absolute;
  inset: auto 0 0;
  padding: 7px;
  text-align: center;
  color: #fff;
  font-size: 10px;
  background: rgba(24, 46, 35, 0.58);
}

.cover-empty {
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: #9c9282;
  font-size: 11px;
  cursor: pointer;
}

.cover-empty .el-icon {
  font-size: 24px;
}

.cover-empty small {
  color: #b8ae9e;
  font-size: 9px;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  color: #66736a;
  font-size: 11px;
}

.spec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 8px;
  padding-top: 12px;
  border-top: 1px solid #f3ece2;
}

.spec-head strong {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #536159;
}

.spec-head small {
  display: block;
  margin-top: 2px;
  color: #a89e8e;
  font-size: 9px;
}

.spec-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.spec-row {
  display: flex;
  align-items: center;
  gap: 7px;
}

.spec-row .el-input {
  flex: 1;
}

.spec-row :deep(.el-input-number) {
  width: 130px;
}

.spec-empty {
  margin: 0;
  padding: 10px 12px;
  border-radius: 9px;
  background: #faf7f1;
  color: #a39886;
  font-size: 10px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f3ece2;
}

@media (max-width: 760px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-input,
  .filter-select {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
