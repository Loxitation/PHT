import { apiClient } from './client'
import type { CurrentUser, TokenPair } from '../types/auth'

export async function login(username: string, password: string): Promise<TokenPair> {
  const response = await apiClient.post<TokenPair>('/api/v1/auth/login', { username, password })
  return response.data
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  const response = await apiClient.get<CurrentUser>('/api/v1/auth/me')
  return response.data
}
