export type HttpMethod =
  | 'GET'
  | 'POST'
  | 'PUT'
  | 'DELETE'
  | 'PATCH'
  | 'OPTIONS'
  | 'HEAD'

type RequestPath = string

export type RequestTuple = [HttpMethod, RequestPath]

export type Options = {
  headers?: Record<string, string>
  body?: FormData | Record<string, any>
  queryParams?: Record<string, any>
  pathParams?: Array<any>
}

export interface HttpClient {
  request: <T>(
    requestTuple: RequestTuple,
    options?: Options,
  ) => Promise<Response<T>>
}

export type ApiList = {
  [key: string]: RequestTuple
}

export type Pagination = {
  page: number
  pageSize: number
  total: number
}

export enum Code {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  pagination?: Pagination
  token?: string
}

export type Response<T> = {
  code: number
  message: string
  data: T
  pagination?: Pagination
  token?: string
}
