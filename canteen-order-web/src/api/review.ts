import { request } from './http'
import type { CreateReviewPayload, ReviewVO } from '@/types'

export function createReview(payload: CreateReviewPayload): Promise<ReviewVO> {
  return request<ReviewVO>({ url: '/reviews', method: 'post', data: payload })
}
