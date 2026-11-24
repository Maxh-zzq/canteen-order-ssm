import { request } from './http'
import type { CaptchaVO, LoginPayload, LoginResult } from '@/types'

export function login(payload: LoginPayload): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/login', method: 'post', data: payload })
}

export function logout(): Promise<void> {
  return request<void>({ url: '/auth/logout', method: 'post' })
}

export function getCaptcha(): Promise<CaptchaVO> {
  return request<CaptchaVO>({ url: '/auth/captcha', method: 'get' })
}
