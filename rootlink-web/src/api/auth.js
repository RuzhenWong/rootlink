import { request } from '@/utils/request'

export const authApi = {
  // 发送验证码
  sendCode(data) {
    return request.post('/v1/auth/sms/send', data)
  },

  // 用户注册
  register(data) {
    return request.post('/v1/auth/register', data)
  },

  // 用户登录
  login(data) {
    return request.post('/v1/auth/login', data)
  },

  // 登出
  logout() {
    return request.post('/v1/auth/logout')
  },
}
