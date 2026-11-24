import dayjs from 'dayjs'
import type { Role } from '@/types'

export const ORDER_STATUS_MAP: Record<number, { text: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }> = {
  10: { text: '待支付', type: 'warning' },
  20: { text: '已支付', type: 'primary' },
  30: { text: '备餐中', type: 'primary' },
  40: { text: '可取餐', type: 'success' },
  50: { text: '已完成', type: 'info' },
  60: { text: '已取消', type: 'info' },
  70: { text: '已退款', type: 'danger' }
}

export const ROLE_MAP: Record<Role, string> = {
  STUDENT: '学生',
  STAFF: '档口员工',
  ADMIN: '系统管理员'
}

export function orderStatusText(status: number): string {
  return ORDER_STATUS_MAP[status]?.text || `状态 ${status}`
}

export function orderStatusType(status: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  return ORDER_STATUS_MAP[status]?.type || 'info'
}

export function money(value: number | string | null | undefined): string {
  const num = Number(value ?? 0)
  return `¥${num.toFixed(2)}`
}

export function dateText(value?: string | null): string {
  if (!value) return '—'
  return dayjs(value).format('YYYY-MM-DD')
}

export function dateTimeText(value?: string | null): string {
  if (!value) return '—'
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export function shortTime(value?: string | null): string {
  if (!value) return '—'
  return dayjs(value).format('MM-dd HH:mm')
}

export function todayString(): string {
  return dayjs().format('YYYY-MM-DD')
}

export function greeting(): string {
  const hour = dayjs().hour()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

export function countdownText(expireTime?: string | null): string {
  if (!expireTime) return ''
  const seconds = dayjs(expireTime).diff(dayjs(), 'second')
  if (seconds <= 0) return '已超时'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

/** 后端返回的 /uploads 相对地址，开发环境由 Vite 代理，生产由 Nginx 代理 */
export function assetUrl(url?: string | null): string {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

export function initialOf(name?: string | null): string {
  return (name || '食').slice(0, 1)
}
