export interface UserDTO {
  id: string
  username: string
  role: 'ADMIN' | 'USER'
}