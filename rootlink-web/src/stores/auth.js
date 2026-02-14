import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(storage.getToken() || '')
  const userInfo = ref(storage.getUserInfo() || null)
  const isLoggedIn = ref(!!token.value)

  // 登录
  const login = async (loginData) => {
    const res = await authApi.login(loginData)
    token.value = res.token
    storage.setToken(res.token)
    storage.setUserId(res.userId)
    isLoggedIn.value = true

    // 获取用户信息
    await fetchUserInfo()

    return res
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    const res = await userApi.getCurrentUserInfo()
    userInfo.value = res
    storage.setUserInfo(res)
    return res
  }

  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    storage.clear()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    fetchUserInfo,
    logout,
  }
})
