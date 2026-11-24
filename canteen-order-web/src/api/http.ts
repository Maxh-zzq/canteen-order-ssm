import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult, LoginResult } from '@/types'
import { clearSession, getAccessToken, getRefreshToken, saveSession } from '@/utils/session'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const http = axios.create({
  baseURL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const raw = axios.create({ baseURL, timeout: 15000 })
let refreshing: Promise<boolean> | null = null

async function refreshSession(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const res = await raw.post<ApiResult<LoginResult>>('/auth/refresh', { refreshToken })
    if (res.data.code === 200 && res.data.data) {
      saveSession(res.data.data)
      window.dispatchEvent(new CustomEvent('canteen:session-refreshed', { detail: res.data.data }))
      return true
    }
    return false
  } catch {
    return false
  }
}

function ensureRefresh(): Promise<boolean> {
  if (!refreshing) {
    refreshing = refreshSession().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

interface RetryConfig extends AxiosRequestConfig {
  _retry?: boolean
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryConfig | undefined
    if (
      error.response?.status === 401 &&
      config &&
      !config._retry &&
      !String(config.url || '').includes('/auth/')
    ) {
      config._retry = true
      const refreshed = await ensureRefresh()
      if (refreshed) {
        config.headers = {
          ...(config.headers || {}),
          Authorization: `Bearer ${getAccessToken()}`
        } as any
        return http.request(config)
      }
      clearSession()
      ElMessage.error('登录已过期，请重新登录')
      setTimeout(() => window.location.assign('/login'), 500)
    }
    return Promise.reject(error)
  }
)

export class BizError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'BizError'
    this.code = code
  }
}

export function getApiBaseUrl(): string {
  return baseURL
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await http.request<ApiResult<T>>(config)
    const body = response.data
    if (!body || typeof body.code === 'undefined') {
      return body as unknown as T
    }
    if (body.code !== 200) {
      if (body.code === 401) {
        clearSession()
        ElMessage.error(body.message || '登录已失效，请重新登录')
        setTimeout(() => window.location.assign('/login'), 500)
      } else {
        ElMessage.error(body.message || '操作失败')
      }
      throw new BizError(body.code, body.message)
    }
    return body.data
  } catch (error) {
    if (error instanceof BizError) throw error
    const axiosError = error as AxiosError
    if (!axiosError.response) {
      ElMessage.error('网络异常，请确认后端 8083 服务已启动')
    } else if (axiosError.response.status === 401) {
      // 401 由响应拦截器统一处理
    } else if (axiosError.response.status === 403) {
      ElMessage.error('当前账号没有权限执行该操作')
    } else if (axiosError.response.status >= 500) {
      ElMessage.error('服务端异常，请稍后重试')
    }
    throw error
  }
}

export async function requestBlob(config: AxiosRequestConfig): Promise<{ blob: Blob; filename: string }> {
  const response = await http.request<Blob>({ ...config, responseType: 'blob' })
  const contentType = String(response.headers['content-type'] || '')
  if (contentType.includes('application/json')) {
    const text = await response.data.text()
    let message = '导出失败'
    try {
      message = JSON.parse(text).message || message
    } catch {
      // ignore
    }
    ElMessage.error(message)
    throw new BizError(-1, message)
  }
  const disposition = String(response.headers['content-disposition'] || '')
  const match =
    disposition.match(/filename\*=UTF-8''([^;]+)/i) ||
    disposition.match(/filename="?([^";]+)"?/i)
  const filename = match ? decodeURIComponent(match[1]) : 'export.csv'
  return { blob: response.data, filename }
}

export async function uploadMultipart<T>(url: string, file: File, field = 'file'): Promise<T> {
  const form = new FormData()
  form.append(field, file)
  return request<T>({ url, method: 'post', data: form })
}
