import axios, { AxiosResponse } from 'axios'
import { message } from 'antd'
import { kamanoteUserToken } from '../base/constants'
import { ApiResponse } from './types'
import { getApiBaseUrl } from '../base/utils/apiBaseUrl.ts'

function clearAuth() {
  localStorage.removeItem(kamanoteUserToken)
  localStorage.removeItem('currentUser')
  window.dispatchEvent(new CustomEvent('auth:required'))
}

export const http = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(kamanoteUserToken)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const res = response.data

    if (res.code === 401) {
      clearAuth()
      return Promise.reject(new Error(res.message || '未登录或登录已过期'))
    }

    if (res.code !== 200) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      return Promise.reject(new Error('未登录或登录已过期'))
    }

    if (error.response) {
      message.error(error.response.data?.message || '请求失败，请稍后重试')
    } else if (error.request) {
      message.error('网络连接失败，请检查网络')
    } else {
      message.error('请求配置错误')
    }

    return Promise.reject(error)
  },
)
