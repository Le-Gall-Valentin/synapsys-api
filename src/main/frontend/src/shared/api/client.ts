import axios from 'axios'
import { attachRefreshInterceptor } from './refreshInterceptor'

export const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

attachRefreshInterceptor(client)