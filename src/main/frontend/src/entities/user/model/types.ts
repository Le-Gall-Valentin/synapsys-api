export interface UserDTO {
  id: string
  username: string
  role: 'SUPER_ADMIN' | 'ADMIN' | 'USER'
}