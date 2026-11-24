import { request } from './http'
import type { OrderVO, PageResult, PlaceOrderPayload } from '@/types'

export interface OrderQuery {
  status?: number
  pageNum?: number
  pageSize?: number
}

export function placeOrder(payload: PlaceOrderPayload): Promise<OrderVO> {
  return request<OrderVO>({ url: '/orders', method: 'post', data: payload })
}

export function getOrders(params: OrderQuery = {}): Promise<PageResult<OrderVO>> {
  return request<PageResult<OrderVO>>({ url: '/orders', method: 'get', params })
}

export function getOrder(orderNo: string): Promise<OrderVO> {
  return request<OrderVO>({ url: `/orders/${orderNo}`, method: 'get' })
}

export function cancelOrder(orderNo: string, reason?: string): Promise<void> {
  return request<void>({
    url: `/orders/${orderNo}/cancel`,
    method: 'post',
    params: reason ? { reason } : {}
  })
}
