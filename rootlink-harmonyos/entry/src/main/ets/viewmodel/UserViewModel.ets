export interface UserInfo {
  id: number
  uuid: string
  phone: string
  realName: string
  realNameStatus: number
  lifeStatus: number
  allowSearch: number
  email?: string
}

export interface UserProfile {
  gender: number
  nation: string
  birthDate: string
  province: string
  city: string
  district: string
  residence: string
  wechat: string
  qq: string
  workUnit: string
  position: string
  education: string
  maritalStatus: number
  familyRank: number
  bio: string
  avatarUrl: string
}

export interface RelationItem {
  relationId: number
  relatedUserId: number
  relationType: number
  relationDesc: string
  realName: string
  phone: string
  lifeStatus: number
  inferStatus: number
}

export interface TestamentItem {
  id: number
  title: string
  testamentType: number
  unlockStatus: number
  visibility: number
  createTime: string
}
