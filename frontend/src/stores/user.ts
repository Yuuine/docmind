import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User } from '@/types'
import { userApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const isLoggedIn = ref(false)

  function setUser(userData: User) {
    user.value = userData
    isLoggedIn.value = true
  }

  function clearUser() {
    user.value = null
    isLoggedIn.value = false
  }

  async function login(credentials: { username: string; password: string }) {
    const userData = await userApi.login(credentials)
    setUser(userData)
    return userData
  }

  async function register(data: { username: string; password: string; email?: string; phone?: string }) {
    const userData = await userApi.register(data)
    setUser(userData)
    return userData
  }

  function logout() {
    clearUser()
  }

  return {
    user,
    isLoggedIn,
    setUser,
    clearUser,
    login,
    register,
    logout
  }
})
