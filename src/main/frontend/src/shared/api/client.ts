import axios from 'axios'
import { attachRefreshInterceptor } from './refreshInterceptor'

// CSRF: no token header needed. All state-changing requests use HttpOnly cookies with
// SameSite=Strict (configured server-side), which block cross-origin requests.
// Spring Security CSRF protection is explicitly disabled on the backend for this reason.
export const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

attachRefreshInterceptor(client)