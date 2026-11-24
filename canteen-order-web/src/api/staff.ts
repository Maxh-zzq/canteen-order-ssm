import { request } from './http'
import type { OrderVO, PageResult, StallVO } from '@/types'

export interface StaffOrderQuery {
  status?: number
  pageNum?: number
  pageSize?: number
}

export function getStaffOrders(params: StaffOrderQuery = {}): Promise<PageResult<OrderVO>> {
  return request<PageResult<OrderVO>>({ url: '/staff/orders', method: 'get', params })
}

export function getMyStalls(): Promise<StallVO[]> {
  return request<StallVO[]>({ url: '/staff/stalls', method: 'get' })
}

export function acceptOrder(orderNo: string): Promise<void> {
  return request<void>({ url: `/staff/orders/${orderNo}/accept`, method: 'post' })
}

export function readyOrder(orderNo: string): Promise<void> {
  return request<void>({ url: `/staff/orders/${orderNo}/ready`, method: 'post' })
}

export function pickupOrder(orderNo: string, pickupCode: string): Promise<void> {
  return request<void>({
    url: `/staff/orders/${orderNo}/pickup`,
    method: 'post',
    params: { pickupCode }
  })
}
