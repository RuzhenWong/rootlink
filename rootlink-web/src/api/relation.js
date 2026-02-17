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
  // 全量重推
  startFullReInfer() { return request.post('/v1/relation/reinfer/full') },
  getReInferStatus(jobId) { return request.get('/v1/relation/reinfer/status', { jobId }) },
  // 家族关系网络（全节点+全边，用于关系树绘图）
  getRelationNetwork() { return request.get('/v1/relation/network') },
}
