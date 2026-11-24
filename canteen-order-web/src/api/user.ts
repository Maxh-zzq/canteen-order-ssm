import { request } from './http'
import type { RechargePayload, UpdateProfilePayload, UserVO } from '@/types'

export function getMe(): Promise<UserVO> {
  return request<UserVO>({ url: '/users/me', method: 'get' })
}

export function updateMe(payload: UpdateProfilePayload): Promise<UserVO> {
  return request<UserVO>({ url: '/users/me', method: 'put', data: payload })
}

export function getBalance(): Promise<number> {
  return request<number>({ url: '/users/me/balance', method: 'get' })
}

export function recharge(payload: RechargePayload): Promise<number> {
  return request<number>({ url: '/users/me/recharge', method: 'post', data: payload })
}
