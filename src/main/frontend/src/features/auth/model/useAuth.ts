import { authApi } from '../api/authApi'
import { createAuthStore } from './authStore'

export const useAuth = createAuthStore(authApi)