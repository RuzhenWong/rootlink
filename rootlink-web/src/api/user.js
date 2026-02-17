import { request } from '@/utils/request'

export const userApi = {
  getCurrentUserInfo() { return request.get('/v1/user/current') },
  getFullProfile()     { return request.get('/v1/user/profile') },
  updateSettings(data) { return request.patch('/v1/user/settings', data) },
  updateProfile(data)  { return request.put('/v1/user/profile', data) },

  // 查看亲属详情（按隐私规则）
  getRelativeProfile(relativeUserId) {
    return request.get(`/v1/user/relative/${relativeUserId}`)
  },

  // 头像上传：直接传 FormData
  uploadAvatar(formData) {
    return request.post('/v1/user/avatar', formData)
  },

  submitRealName(data) { return request.post('/v1/user/realname/submit', data) },
  getRealNameStatus()  { return request.get('/v1/user/realname/status') },
  getMyIdCard()        { return request.get('/v1/user/realname/idcard') },
  deactivateAccount()  { return request.delete('/v1/user/deactivate') },
}
