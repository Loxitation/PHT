import axios from 'axios'
import { API_BASE_URL } from './config'

/** Plain axios instance with no interceptors, used only for the refresh call itself to avoid
 * recursively triggering the 401-refresh interceptor defined on the main `apiClient` below. */
export const rawClient = axios.create({ baseURL: API_BASE_URL })

export const apiClient = axios.create({ baseURL: API_BASE_URL })

let refreshPromise: Promise<string | null> | null = null

apiClient.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('pht_access_token')
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status !== 401 || originalRequest._retried) {
      return Promise.reject(error)
    }
    originalRequest._retried = true

    const refreshToken = localStorage.getItem('pht_refresh_token')
    if (!refreshToken) {
      clearSession()
      return Promise.reject(error)
    }

    // De-duplicate concurrent refresh attempts (multiple requests failing at once).
    refreshPromise ??= refreshAccessToken(refreshToken)
    const newAccessToken = await refreshPromise
    refreshPromise = null

    if (!newAccessToken) {
      clearSession()
      return Promise.reject(error)
    }

    originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`)
    return apiClient(originalRequest)
  },
)

async function refreshAccessToken(refreshToken: string): Promise<string | null> {
  try {
    const response = await rawClient.post('/api/v1/auth/refresh', { refresh_token: refreshToken })
    const { access_token, refresh_token } = response.data
    localStorage.setItem('pht_access_token', access_token)
    localStorage.setItem('pht_refresh_token', refresh_token)
    return access_token as string
  } catch {
    return null
  }
}

function clearSession(): void {
  localStorage.removeItem('pht_access_token')
  localStorage.removeItem('pht_refresh_token')
  localStorage.removeItem('pht_username')
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}
