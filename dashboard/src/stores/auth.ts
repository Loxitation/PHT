import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginRequest } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('pht_access_token'))
  const username = ref<string | null>(localStorage.getItem('pht_username'))

  const isAuthenticated = computed(() => accessToken.value !== null)

  async function login(usernameInput: string, password: string): Promise<void> {
    const tokens = await loginRequest(usernameInput, password)
    accessToken.value = tokens.access_token
    username.value = usernameInput
    localStorage.setItem('pht_access_token', tokens.access_token)
    localStorage.setItem('pht_refresh_token', tokens.refresh_token)
    localStorage.setItem('pht_username', usernameInput)
  }

  function logout(): void {
    accessToken.value = null
    username.value = null
    localStorage.removeItem('pht_access_token')
    localStorage.removeItem('pht_refresh_token')
    localStorage.removeItem('pht_username')
  }

  return { accessToken, username, isAuthenticated, login, logout }
})
