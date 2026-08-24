import axios, { type AxiosRequestConfig } from 'axios'

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

function getAuthToken(): string | null {
  return sessionStorage.getItem('campus-hub-token') || localStorage.getItem('campus-hub-token')
}

http.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config).catch((err: unknown) => {
    if (axios.isAxiosError<ApiEnvelope<unknown>>(err)) {
      const message = err.response?.data?.message
      if (message) throw new Error(message)
    }
    throw err
  })
  const body = response.data

  if (typeof body?.code === 'number' && body.code !== 0) {
    throw new Error(body.message || '请求失败')
  }

  return body.data
}
