import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/api/types'
import { clearAuth, getToken } from '@/utils/storage'

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
})

function redirectToLogin(): void {
  if (window.location.pathname.startsWith('/login')) {
    return
  }
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `/login?redirect=${redirect}`
}

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 后端返回的图片地址带 /api 前缀，baseURL 已包含 /api，避免重复拼接
  if (config.url?.startsWith('/api/')) {
    config.url = config.url.slice(4)
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data as ApiResponse<unknown> | undefined
    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data as never
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new ApiError(body.code, body.message))
    }
    return response
  },
  async (error: AxiosError) => {
    let code = -1
    let message = '网络异常，请稍后重试'
    const response = error.response
    if (response) {
      const data = response.data
      if (data instanceof Blob) {
        try {
          const parsed = JSON.parse(await data.text()) as Partial<ApiResponse<unknown>>
          if (parsed.message) message = parsed.message
          if (typeof parsed.code === 'number') code = parsed.code
        } catch {
          message = '文件获取失败'
        }
      } else if (data && typeof data === 'object') {
        const body = data as Partial<ApiResponse<unknown>>
        if (body.message) message = body.message
        if (typeof body.code === 'number') code = body.code
      }

      if (response.status === 401 || code === 40100) {
        clearAuth()
        ElMessage.error('登录已失效，请重新登录')
        redirectToLogin()
      } else if (response.status === 403 || code === 40300) {
        ElMessage.error(message || '无权限执行该操作')
      } else {
        ElMessage.error(message)
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(new ApiError(code, message))
  },
)

function request<T>(config: AxiosRequestConfig): Promise<T> {
  return instance.request(config) as unknown as Promise<T>
}

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ method: 'GET', url, params })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'POST', url, data })
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'PUT', url, data })
  },
  patch<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ method: 'PATCH', url, data })
  },
  getBlob(url: string): Promise<AxiosResponse<Blob>> {
    return instance.get<Blob>(url, { responseType: 'blob', timeout: 60000 })
  },
  upload<T>(
    url: string,
    formData: FormData,
    onProgress?: (percent: number) => void,
  ): Promise<T> {
    return request<T>({
      method: 'POST',
      url,
      data: formData,
      timeout: 180000,
      onUploadProgress: (event) => {
        if (onProgress && event.total) {
          onProgress(Math.round((event.loaded * 100) / event.total))
        }
      },
    })
  },
}
