import { http } from '@/api/http'
import type { Feedback } from '@/api/types'

export function submitFeedback(orderId: number, content: string): Promise<Feedback> {
  return http.post<Feedback>(`/orders/${orderId}/feedback`, { content })
}

export function listFeedbacks(orderId: number): Promise<Feedback[]> {
  return http.get<Feedback[]>(`/orders/${orderId}/feedbacks`)
}
