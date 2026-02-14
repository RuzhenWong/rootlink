import { request } from '@/utils/request'

export const testamentApi = {
  create(data) { return request.post('/v1/testament', data) },
  update(id, data) { return request.put(`/v1/testament/${id}`, data) },
  delete(id) { return request.delete(`/v1/testament/${id}`) },
  getMyList() { return request.get('/v1/testament/my') },
  getDetail(id) { return request.get(`/v1/testament/${id}`) },
}
