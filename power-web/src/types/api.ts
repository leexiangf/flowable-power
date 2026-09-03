/** 与后端 com.power.common.result.R 对齐 */

export interface ApiResult<T = unknown> {

  code: number

  message: string

  data: T

  traceId?: string

}



export interface LoginResponse {

  accessToken: string

  refreshToken: string

  tokenType: string

  expiresIn: number

  /** 雪花 ID，后端超出 JS 安全整数时以 string 返回 */

  userId: string

  username: string

  platform: string

  authorities: string[]

}



export interface CurrentUser {

  userId: string

  username: string

  nickname?: string

  email?: string

  phone?: string

  avatar?: string

  status?: number

  roles: string[]

  authorities: string[]

}



export interface PasswordChangeRequest {
  oldPassword: string
  newPassword: string
}

export interface ProfileUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

export interface MenuVO {

  id: string

  parentId: string

  menuName: string

  menuType: number

  path?: string

  component?: string

  perms?: string

  icon?: string

  sort?: number

  children?: MenuVO[]

}

