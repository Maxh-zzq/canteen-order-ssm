import { request } from './http'
import type { AddCartPayload, CartVO, UpdateCartPayload } from '@/types'

export function getCart(): Promise<CartVO> {
  return request<CartVO>({ url: '/cart', method: 'get' })
}

export function addCartItem(payload: AddCartPayload): Promise<CartVO> {
  return request<CartVO>({ url: '/cart/items', method: 'post', data: payload })
}

export function updateCartItem(payload: UpdateCartPayload): Promise<CartVO> {
  return request<CartVO>({ url: '/cart/items', method: 'put', data: payload })
}

export function removeCartItem(dishId: number, specId?: number | null): Promise<CartVO> {
  return request<CartVO>({
    url: '/cart/items',
    method: 'delete',
    params: { dishId, ...(specId ? { specId } : {}) }
  })
}

export function clearCart(): Promise<void> {
  return request<void>({ url: '/cart', method: 'delete' })
}
