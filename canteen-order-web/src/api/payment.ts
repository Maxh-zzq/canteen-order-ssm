import { request } from './http'
import type { OrderVO } from '@/types'

export function payByBalance(orderNo: string): Promise<OrderVO> {
  return request<OrderVO>({ url: `/payments/balance/${orderNo}`, method: 'post' })
}

export function refundOrder(orderNo: string, reason: string): Promise<void> {
  return request<void>({
    url: `/payments/${orderNo}/refund`,
    method: 'post',
    data: { reason }
  })
}
