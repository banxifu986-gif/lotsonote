import { Code, HttpClient, Options, RequestTuple, Response } from './types.ts'
import { kamanoteUserToken } from '../base/constants'
import { getApiBaseUrl } from '../base/utils/apiBaseUrl.ts'

export default class FetchClient implements HttpClient {
  private processPathParams(path: string, pathParams: Array<any>): string {
    let paramIndex = 0
    return path.replace(/\{(\w+)\}/g, () => {
      const param = pathParams[paramIndex++]
      if (param === undefined) {
        throw new Error('Missing path parameter')
      }
      return encodeURIComponent(param)
    })
  }

  private clearAuth() {
    localStorage.removeItem(kamanoteUserToken)
    localStorage.removeItem('currentUser')
    window.dispatchEvent(new CustomEvent('auth:required'))
  }

  async request<T>(
    requestTuple: RequestTuple,
    options?: Options,
  ): Promise<Response<T>> {
    const [method, requestPath] = requestTuple
    let requestURL = `${getApiBaseUrl()}${requestPath}`

    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...(options?.headers || {}),
    }

    const token = localStorage.getItem(kamanoteUserToken)
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const fetchOptions: RequestInit = {
      method,
      headers,
    }

    if (options?.pathParams) {
      requestURL = this.processPathParams(requestURL, options.pathParams)
    }

    if (method === 'GET' && options?.queryParams) {
      const queryParams = Object.fromEntries(
        Object.entries(options.queryParams).filter(
          ([, value]) => value !== undefined,
        ),
      )
      const queryString = new URLSearchParams(queryParams).toString()
      requestURL += queryString ? `?${queryString}` : ''
    }

    if (method !== 'GET' && options?.body) {
      if (options.body instanceof FormData) {
        delete headers['Content-Type']
        fetchOptions.body = options.body
      } else {
        fetchOptions.body = JSON.stringify(options.body)
      }
    }

    const response = await fetch(requestURL, fetchOptions)
    const result = (await response.json()) as Response<T>

    if (result.code === Code.UNAUTHORIZED) {
      this.clearAuth()
      throw new Error(result.message || '未登录或登录已过期')
    }

    if (result.code !== Code.SUCCESS) {
      throw new Error(result.message || '请求失败')
    }

    return result
  }
}
