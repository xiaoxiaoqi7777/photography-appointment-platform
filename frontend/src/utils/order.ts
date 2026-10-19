import type { OrderStatus, ShootSlot } from '@/api/types'

export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

export interface OrderStatusOption {
  value: OrderStatus
  label: string
  type: TagType
}

export const ORDER_STATUS_OPTIONS: OrderStatusOption[] = [
  { value: 'PENDING', label: '待确认', type: 'warning' },
  { value: 'CONFIRMED', label: '已确认', type: 'primary' },
  { value: 'SHOOTING_DONE', label: '拍摄完成', type: 'primary' },
  { value: 'UPLOADED', label: '待反馈', type: 'success' },
  { value: 'MODIFYING', label: '修改中', type: 'warning' },
  { value: 'DELIVERED', label: '已交付', type: 'success' },
  { value: 'REJECTED', label: '已拒绝', type: 'danger' },
  { value: 'CANCELLED', label: '已取消', type: 'info' },
]

const STATUS_MAP = new Map<string, OrderStatusOption>(
  ORDER_STATUS_OPTIONS.map((option) => [option.value, option]),
)

export function statusLabel(status: string): string {
  return STATUS_MAP.get(status)?.label ?? status
}

export function statusType(status: string): TagType {
  return STATUS_MAP.get(status)?.type ?? 'info'
}

export const SHOOT_SLOT_OPTIONS: Array<{ value: ShootSlot; label: string }> = [
  { value: 'MORNING', label: '上午 09:00-12:00' },
  { value: 'AFTERNOON', label: '下午 13:00-18:00' },
  { value: 'FULL_DAY', label: '全天 09:00-18:00' },
]

export const ACTION_LABELS: Record<string, string> = {
  CREATE: '提交预约',
  CONFIRM: '确认订单',
  REJECT: '拒绝订单',
  CANCEL: '取消订单',
  SHOOT_DONE: '标记拍摄完成',
  UPLOAD: '上传精修照片',
  MODIFY: '申请修改',
  DELIVER: '确认交付',
}

export function actionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action
}
