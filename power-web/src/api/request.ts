import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/auth'

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

export const request = axios.create({
  baseURL,
  timeout: 30000,
})

let refreshing = false
let refreshQueue: Array<(token: string) => void> = []

function flushQueue(token: string) {
  refreshQueue.forEach((cb) => cb(token))
  refreshQueue = []
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('no refresh token')
  }
  const res = await axios.post<ApiResult<{
    accessToken: string
    refreshToken: string
  }>>(`${baseURL}/auth/refresh`, { refreshToken })
  if (res.data.code !== 0 || !res.data.data) {
    throw new Error(res.data.message || 'refresh failed')
  }
  const { accessToken, refreshToken: newRefresh } = res.data.data
  setTokens(accessToken, newRefresh)
  return accessToken
}

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResult
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      if (payload.code === 20004) {
        clearTokens()
        ElMessage.warning(payload.message || '账号已在其他端登录')
        window.location.href = '/login?kicked=1'
        return Promise.reject(payload)
      }
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(payload)
    }
    return response
  },
  async (error: AxiosError<ApiResult>) => {
    const status = error.response?.status
    const payload = error.response?.data
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (status === 401 && original && !original._retry && !original.url?.includes('/auth/login')) {
      if (refreshing) {
        return new Promise((resolve) => {
          refreshQueue.push((token) => {
            original.headers.Authorization = `Bearer ${token}`
            resolve(request(original))
          })
        })
      }
      original._retry = true
      refreshing = true
      try {
        const newToken = await refreshAccessToken()
        flushQueue(newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return request(original)
      } catch {
        clearTokens()
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
        return Promise.reject(error)
      } finally {
        refreshing = false
      }
    }

    const msg = payload?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export async function unwrap<T>(promise: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const res = await promise
  return res.data.data as T
}
