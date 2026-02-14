import { request } from '@/utils/request'

export const relationApi = {
  searchUser(idCard) { return request.get('/v1/relation/search', { idCard }) },
  applyRelation(data) { return request.post('/v1/relation/apply', data) },
  getPendingApplies() { return request.get('/v1/relation/pending-applies') },
  handleApply(applyId, data) { return request.post(`/v1/relation/apply/${applyId}/handle`, data) },
  getMyRelations() { return request.get('/v1/relation/my-relations') },
  getPendingInferred() { return request.get('/v1/relation/inferred-pending') },
  confirmInferred(id) { return request.post(`/v1/relation/inferred/${id}/confirm`) },
  rejectInferred(id) { return request.delete(`/v1/relation/inferred/${id}`) },
  removeRelation(relationId) { return request.delete(`/v1/relation/${relationId}`) },
}
