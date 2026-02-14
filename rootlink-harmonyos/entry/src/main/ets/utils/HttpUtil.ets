import http from '@ohos.net.http'
import preferences from '@ohos.data.preferences'
import { BASE_URL, STORAGE_TOKEN } from './Constants'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

async function getToken(): Promise<string> {
  try {
    const pref = await preferences.getPreferences(getContext(), 'rl_prefs')
    const val = await pref.get(STORAGE_TOKEN, '')
    return val as string
  } catch (e) {
    return ''
  }
}

export async function request<T>(
  method: http.RequestMethod,
  path: string,
  body?: object
): Promise<T> {
  const token = await getToken()
  const req = http.createHttp()
  const url = BASE_URL + path

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
  if (token) {
    headers['Authorization'] = 'Bearer ' + token
  }

  const options: http.HttpRequestOptions = {
    method,
    header: headers,
    connectTimeout: 15000,
    readTimeout: 15000,
    expectDataType: http.HttpDataType.STRING
  }
  if (body) {
    options.extraData = JSON.stringify(body)
  }

  try {
    const resp = await req.request(url, options)
    const result = JSON.parse(resp.result as string) as ApiResponse<T>
    if (result.code !== 200) {
      throw new Error(result.message || '请求失败')
    }
    return result.data
  } finally {
    req.destroy()
  }
}

export function get<T>(path: string): Promise<T> {
  return request<T>(http.RequestMethod.GET, path)
}

export function post<T>(path: string, body?: object): Promise<T> {
  return request<T>(http.RequestMethod.POST, path, body)
}

export function put<T>(path: string, body?: object): Promise<T> {
  return request<T>(http.RequestMethod.PUT, path, body)
}

export function patch<T>(path: string, body?: object): Promise<T> {
  return request<T>(http.RequestMethod.PATCH, path, body)
}

export function del<T>(path: string): Promise<T> {
  return request<T>(http.RequestMethod.DELETE, path)
}
