import { request } from './http'
import type { CategoryVO, DishVO, MenuVO, PageResult, ReviewVO, StallVO } from '@/types'

export interface DishQuery {
  categoryId?: number
  stallId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export function getCategories(): Promise<CategoryVO[]> {
  return request<CategoryVO[]>({ url: '/categories', method: 'get' })
}

export function getStalls(): Promise<StallVO[]> {
  return request<StallVO[]>({ url: '/stalls', method: 'get' })
}

export function getDishes(params: DishQuery = {}): Promise<PageResult<DishVO>> {
  return request<PageResult<DishVO>>({ url: '/dishes', method: 'get', params })
}

export function getDish(id: number): Promise<DishVO> {
  return request<DishVO>({ url: `/dishes/${id}`, method: 'get' })
}

export function getTodayMenus(): Promise<MenuVO[]> {
  return request<MenuVO[]>({ url: '/menus/today', method: 'get' })
}

export function getDishReviews(dishId: number, limit = 20): Promise<ReviewVO[]> {
  return request<ReviewVO[]>({ url: `/dishes/${dishId}/reviews`, method: 'get', params: { limit } })
}
