import type { User } from '@/entities/user'
import type { TotpSetupData } from '../model/types'

export interface ITotpApi {
  verify(code: string): Promise<User>
  setup(): Promise<TotpSetupData>
  confirm(code: string): Promise<void>
  getStatus(): Promise<{ totpEnabled: boolean }>
}