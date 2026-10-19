export type Role = 'USER' | 'ADMIN'

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'SHOOTING_DONE'
  | 'UPLOADED'
  | 'MODIFYING'
  | 'DELIVERED'

export type ShootSlot = 'MORNING' | 'AFTERNOON' | 'FULL_DAY'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

export interface PageQuery {
  page?: number
  pageSize?: number
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  role: Role
  expiresIn: number
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  phone?: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string | null
  phone: string | null
  avatar: string | null
  role: Role
  status: number
  createTime: string
}

export interface UpdateMePayload {
  nickname?: string
  phone?: string
  oldPassword?: string
  newPassword?: string
}

export interface Theme {
  id: number
  name: string
  coverUrl: string | null
  description: string | null
  tags: string[]
  sort: number
  status: number
  createTime: string
  updateTime: string
}

export interface ThemeSavePayload {
  name?: string
  coverUrl?: string
  description?: string
  tags?: string[]
  sort?: number
  status?: number
}

export interface PhotoPackage {
  id: number
  themeId: number
  name: string
  price: number
  retouchCount: number
  duration: string | null
  includes: string | null
  coverUrl: string | null
  sort: number
  status: number
  createTime: string
  updateTime: string
}

export interface PackageSavePayload {
  themeId?: number
  name?: string
  price?: number
  retouchCount?: number
  duration?: string
  includes?: string
  coverUrl?: string
  sort?: number
  status?: number
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  username: string
  themeId: number
  themeName: string
  packageId: number
  packageName: string
  shootDate: string
  shootSlot: ShootSlot
  shootSlotText: string
  remark: string | null
  status: OrderStatus
  rejectReason: string | null
  price: number
  deliverTime: string | null
  createTime: string
  updateTime: string
}

export interface OrderLog {
  action: string
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  remark: string | null
  createTime: string
}

export interface Feedback {
  id: number
  round: number
  photoRound: number
  content: string
  createTime: string
}

export interface OrderDetail {
  order: Order
  timeline: OrderLog[]
  feedbacks: Feedback[]
}

export interface CreateOrderPayload {
  themeId: number
  packageId: number
  shootDate: string
  shootSlot: ShootSlot
  remark?: string
}

export interface CreateOrderResult {
  id: number
  orderNo: string
  status: OrderStatus
}

export interface PhotoItem {
  id: number
  originalName: string
  fileSize: number
  uploadRound: number
  previewUrl: string
}

export interface FailedFile {
  name: string
  reason: string
}

export interface UploadResult {
  success: string[]
  failed: FailedFile[]
}
