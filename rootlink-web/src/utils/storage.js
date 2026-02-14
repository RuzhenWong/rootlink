const TOKEN_KEY = 'rootlink_token'
const USER_ID_KEY = 'rootlink_user_id'
const USER_INFO_KEY = 'rootlink_user_info'

export const storage = {
  // Token相关
  setToken(token) {
    localStorage.setItem(TOKEN_KEY, token)
  },

  getToken() {
    return localStorage.getItem(TOKEN_KEY)
  },

  removeToken() {
    localStorage.removeItem(TOKEN_KEY)
  },

  // 用户ID相关
  setUserId(userId) {
    localStorage.setItem(USER_ID_KEY, userId)
  },

  getUserId() {
    return localStorage.getItem(USER_ID_KEY)
  },

  removeUserId() {
    localStorage.removeItem(USER_ID_KEY)
  },

  // 用户信息相关
  setUserInfo(userInfo) {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  },

  getUserInfo() {
    const userInfo = localStorage.getItem(USER_INFO_KEY)
    return userInfo ? JSON.parse(userInfo) : null
  },

  removeUserInfo() {
    localStorage.removeItem(USER_INFO_KEY)
  },

  // 清空所有
  clear() {
    this.removeToken()
    this.removeUserId()
    this.removeUserInfo()
  },
}
