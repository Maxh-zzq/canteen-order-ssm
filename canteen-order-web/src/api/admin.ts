import { request, requestBlob, uploadMultipart } from './http'
import type {
  CategoryPayload,
  CategoryVO,
  ConfigPayload,
  ConfigVO,
  DishPayload,
  DishVO,
  MenuPublishPayload,
  MenuVO,
  OperLogVO,
  PageResult,
  SalesTrendVO,
  StallPayload,
  StallVO,
  StatsOverviewVO,
  TopDishVO,
  UserStatusPayload,
  UserVO
} from '@/types'

export interface AdminDishQuery {
  categoryId?: number
  stallId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface UserQuery {
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface LogQuery {
  pageNum?: number
  pageSize?: number
}

/* ---------- 分类 ---------- */

export function getAdminCategories(): Promise<CategoryVO[]> {
  return request<CategoryVO[]>({ url: '/admin/categories', method: 'get' })
}

export function createCategory(payload: CategoryPayload): Promise<CategoryVO> {
  return request<CategoryVO>({ url: '/admin/categories', method: 'post', data: payload })
}

export function updateCategory(id: number, payload: CategoryPayload): Promise<CategoryVO> {
  return request<CategoryVO>({ url: `/admin/categories/${id}`, method: 'put', data: payload })
}

export function deleteCategory(id: number): Promise<void> {
  return request<void>({ url: `/admin/categories/${id}`, method: 'delete' })
}

/* ---------- 档口 ---------- */

export function getAdminStalls(): Promise<StallVO[]> {
  return request<StallVO[]>({ url: '/admin/stalls', method: 'get' })
}

export function createStall(payload: StallPayload): Promise<StallVO> {
  return request<StallVO>({ url: '/admin/stalls', method: 'post', data: payload })
}

export function updateStall(id: number, payload: StallPayload): Promise<StallVO> {
  return request<StallVO>({ url: `/admin/stalls/${id}`, method: 'put', data: payload })
}

export function deleteStall(id: number): Promise<void> {
  return request<void>({ url: `/admin/stalls/${id}`, method: 'delete' })
}

/* ---------- 菜品 ---------- */

export function getAdminDishes(params: AdminDishQuery = {}): Promise<PageResult<DishVO>> {
  return request<PageResult<DishVO>>({ url: '/admin/dishes', method: 'get', params })
}

export function getAdminDish(id: number): Promise<DishVO> {
  return request<DishVO>({ url: `/admin/dishes/${id}`, method: 'get' })
}

export function createDish(payload: DishPayload): Promise<DishVO> {
  return request<DishVO>({ url: '/admin/dishes', method: 'post', data: payload })
}

export function updateDish(id: number, payload: DishPayload): Promise<DishVO> {
  return request<DishVO>({ url: `/admin/dishes/${id}`, method: 'put', data: payload })
}

export function deleteDish(id: number): Promise<void> {
  return request<void>({ url: `/admin/dishes/${id}`, method: 'delete' })
}

export function uploadDishImage(file: File): Promise<string> {
  return uploadMultipart<string>('/admin/files/upload', file)
}

/* ---------- 每日菜单 ---------- */

export function getAdminMenus(date?: string): Promise<MenuVO[]> {
  return request<MenuVO[]>({ url: '/admin/menus', method: 'get', params: date ? { date } : {} })
}

export function publishMenu(payload: MenuPublishPayload): Promise<number> {
  return request<number>({ url: '/admin/menus/publish', method: 'post', data: payload })
}

export function updateMenuStock(menuId: number, stockTotal: number): Promise<MenuVO> {
  return request<MenuVO>({
    url: `/admin/menus/${menuId}/stock`,
    method: 'put',
    data: { stockTotal }
  })
}

/* ---------- 用户 / 日志 / 配置 ---------- */

export function getUsers(params: UserQuery = {}): Promise<PageResult<UserVO>> {
  return request<PageResult<UserVO>>({ url: '/admin/users', method: 'get', params })
}

export function updateUserStatus(id: string, status: number): Promise<UserVO> {
  const payload: UserStatusPayload = { status }
  return request<UserVO>({ url: `/admin/users/${id}/status`, method: 'put', data: payload })
}

export function getUserStalls(id: string): Promise<number[]> {
  return request<number[]>({ url: `/admin/users/${id}/stalls`, method: 'get' })
}

export function updateUserStalls(id: string, stallIds: number[]): Promise<number[]> {
  return request<number[]>({ url: `/admin/users/${id}/stalls`, method: 'put', data: { stallIds } })
}

export function getLogs(params: LogQuery = {}): Promise<PageResult<OperLogVO>> {
  return request<PageResult<OperLogVO>>({ url: '/admin/logs', method: 'get', params })
}

export function getConfigs(): Promise<ConfigVO[]> {
  return request<ConfigVO[]>({ url: '/admin/configs', method: 'get' })
}

export function saveConfig(payload: ConfigPayload): Promise<ConfigVO> {
  return request<ConfigVO>({ url: '/admin/configs', method: 'put', data: payload })
}

/* ---------- 统计 / 导出 ---------- */

export function getOverview(): Promise<StatsOverviewVO> {
  return request<StatsOverviewVO>({ url: '/admin/stats/overview', method: 'get' })
}

export function getSalesTrend(days = 7): Promise<SalesTrendVO[]> {
  return request<SalesTrendVO[]>({ url: '/admin/stats/sales-trend', method: 'get', params: { days } })
}

export function getTopDishes(days = 7, limit = 8): Promise<TopDishVO[]> {
  return request<TopDishVO[]>({
    url: '/admin/stats/top-dishes',
    method: 'get',
    params: { days, limit }
  })
}

export function exportOrders(startDate?: string, endDate?: string): Promise<{ blob: Blob; filename: string }> {
  return requestBlob({
    url: '/admin/orders/export',
    method: 'get',
    params: {
      ...(startDate ? { startDate } : {}),
      ...(endDate ? { endDate } : {})
    }
  })
}
