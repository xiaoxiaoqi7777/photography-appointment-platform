import { http } from '@/api/http'
import type {
  CreateOrderPayload,
  CreateOrderResult,
  Order,
  OrderDetail,
  OrderStatus,
  PageQuery,
  PageResult,
} from '@/api/types'

export interface MyOrderQuery extends PageQuery {
  status?: OrderStatus
}

export interface AdminOrderQuery extends PageQuery {
  status?: OrderStatus
  keyword?: string
}

export function createOrder(payload: CreateOrderPayload): Promise<CreateOrderResult> {
  return http.post<CreateOrderResult>('/orders', payload)
}

export function listMyOrders(query: MyOrderQuery = {}): Promise<PageResult<Order>> {
  return http.get<PageResult<Order>>('/orders', { ...query })
}

export function getOrderDetail(id: number): Promise<OrderDetail> {
  return http.get<OrderDetail>(`/orders/${id}`)
}

export function cancelOrder(id: number): Promise<Order> {
  return http.post<Order>(`/orders/${id}/cancel`)
}

export function deliverOrder(id: number): Promise<Order> {
  return http.post<Order>(`/orders/${id}/deliver`)
}

export function adminListOrders(query: AdminOrderQuery = {}): Promise<PageResult<Order>> {
  return http.get<PageResult<Order>>('/admin/orders', { ...query })
}

export function adminGetOrderDetail(id: number): Promise<OrderDetail> {
  return http.get<OrderDetail>(`/admin/orders/${id}`)
}

export function confirmOrder(id: number): Promise<Order> {
  return http.post<Order>(`/admin/orders/${id}/confirm`)
}

export function rejectOrder(id: number, rejectReason: string): Promise<Order> {
  return http.post<Order>(`/admin/orders/${id}/reject`, { rejectReason })
}

export function shootDone(id: number): Promise<Order> {
  return http.post<Order>(`/admin/orders/${id}/shoot-done`)
}
