import { request } from '@/utils/request'

export const eulogyApi = {
  submitEulogy(data) {
    return request.post('/v1/eulogy/submit', data)
  },

  reviewEulogy(eulogyId, data) {
    return request.post(`/v1/eulogy/${eulogyId}/review`, data)
  },

  getMyPendingReviews() {
    return request.get('/v1/eulogy/my-pending-reviews')
  },

  // 查全部挽联墙（不按人筛选）
  getEulogyWall(params) {
    return request.get('/v1/eulogy/wall', params)
  },

  // 查指定已故用户的挽联墙
  getEulogyWallByUser(targetUserId, params) {
    return request.get(`/v1/eulogy/wall/${targetUserId}`, params)
  },

  getEulogyDetail(eulogyId) {
    return request.get(`/v1/eulogy/${eulogyId}`)
  },
}
