import axios from "axios"
import { authStore } from "@/store/auth.store"

export const api = axios.create({
  baseURL: '/api',
  headers: {
    "Content-Type": "application/json",
  },
})
api.interceptors.request.use((config) => {
  const token = authStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Single-flight : un seul refresh à la fois pour toutes les requêtes 401 concurrentes
let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = authStore.getState().refreshToken
  if (!refreshToken) return null
  try {
    // axios brut : évite la boucle via l'intercepteur api
    const response = await axios.post('/api/auth/refresh', { refreshToken })
    const newToken = response.data?.token as string | undefined
    const newRefreshToken = response.data?.refreshToken as string | undefined
    if (!newToken) return null
    authStore.setState({ token: newToken, refreshToken: newRefreshToken ?? null })
    return newToken
  } catch {
    return null
  }
}

function isAuthEndpoint(url = ''): boolean {
  return ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout', '/auth/forgot-password', '/auth/reset-password']
    .some((path) => url.includes(path))
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    // 401 sur une requête métier → tentative de refresh puis retry unique
    if (status === 401 && original && !original._retry && !isAuthEndpoint(original.url)) {
      original._retry = true
      refreshPromise = refreshPromise ?? refreshAccessToken()
      const newToken = await refreshPromise
      refreshPromise = null

      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      }
      authStore.getState().logout()
    }

    return Promise.reject(error)
  }
)
