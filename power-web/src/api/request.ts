import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/auth'

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

/** 未认证 / Token 无效 — 应回登录页 */
const AUTH_FAIL_CODES = new Set([10002, 20001])

export const request = axios.create({
  baseURL,
  timeout: 30000,
})

let refreshing = false
let refreshQueue: Array<{
  resolve: (token: string) => void
  reject: (reason?: unknown) => void
}> = []
/** 防止并发 401/10002 多次跳转、多次 toast */
let loginRedirecting = false

function flushQueueSuccess(token: string) {
  refreshQueue.forEach(({ resolve }) => resolve(token))
  refreshQueue = []
}

function flushQueueFailure(reason?: unknown) {
  refreshQueue.forEach(({ reject }) => reject(reason))
  refreshQueue = []
}

function redirectToLogin(options?: { message?: string; kicked?: boolean }) {
  if (loginRedirecting) {
    return
  }
  const path = window.location.pathname
  if (path === '/login' || path.startsWith('/login?')) {
    clearTokens()
    return
  }
  loginRedirecting = true
  clearTokens()

  const msg = options?.message || '登录已失效，请重新登录'
  ElMessage.warning(msg)

  const q = new URLSearchParams()
  if (options?.kicked) {
    q.set('kicked', '1')
  }
  const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
  if (redirect && redirect !== '/' && !redirect.startsWith('/login')) {
    q.set('redirect', redirect)
  }
  const qs = q.toString()
  // 整页跳转，清掉路由/动态菜单脏状态
  window.location.href = qs ? `/login?${qs}` : '/login'
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

function isAuthFailCode(code: unknown): boolean {
  return typeof code === 'number' && AUTH_FAIL_CODES.has(code)
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
        redirectToLogin({
          message: payload.message || '账号已在其他端登录',
          kicked: true,
        })
        return Promise.reject(payload)
      }
      if (isAuthFailCode(payload.code)) {
        redirectToLogin({ message: payload.message || '未登录或登录已失效' })
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
    const url = original?.url || ''

    // 业务码未认证（少数网关/过滤器以非 401 返回）
    if (payload && isAuthFailCode(payload.code)) {
      redirectToLogin({ message: payload.message || '未登录或登录已失效' })
      return Promise.reject(error)
    }

    if (status === 401 && original && !original._retry && !url.includes('/auth/login')) {
      if (!getRefreshToken()) {
        redirectToLogin({ message: '未登录或登录已失效' })
        return Promise.reject(error)
      }
      if (refreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({
            resolve: (token: string) => {
              original.headers.Authorization = `Bearer ${token}`
              request(original).then(resolve).catch(reject)
            },
            reject,
          })
        })
      }
      original._retry = true
      refreshing = true
      try {
        const newToken = await refreshAccessToken()
        flushQueueSuccess(newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return request(original)
      } catch (refreshErr) {
        flushQueueFailure(refreshErr)
        redirectToLogin({ message: '登录已过期，请重新登录' })
        return Promise.reject(error)
      } finally {
        refreshing = false
      }
    }

    if (status === 401) {
      redirectToLogin({ message: payload?.message || '未登录或登录已失效' })
      return Promise.reject(error)
    }

    const msg =
      (payload && typeof payload === 'object' && 'message' in payload && payload.message) ||
      error.message ||
      '网络异常'
    // 避免把整段 JSON 当成提示
    ElMessage.error(typeof msg === 'string' ? msg : '请求失败')
    return Promise.reject(error)
  },
)

export async function unwrap<T>(promise: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const res = await promise
  return res.data.data as T
}
