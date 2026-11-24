import type { OrderVO } from '@/types'

/** 生成与后端约定一致的 requestId（UUID） */
export function newRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return `${Date.now()}${Math.random().toString(16).slice(2, 10)}`
}

/** 订单商品摘要，例如“番茄炒蛋盖饭 ×1、宫保鸡丁盖饭 ×2” */
export function orderSummary(order: OrderVO): string {
  if (!order.items?.length) return '暂无商品明细'
  return order.items.map((item) => `${item.dishName} ×${item.quantity}`).join('、')
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
