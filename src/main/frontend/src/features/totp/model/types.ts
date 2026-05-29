import type { User } from '@/entities/user'

export interface TotpSetupData {
  otpauthUri: string
  secret: string
}