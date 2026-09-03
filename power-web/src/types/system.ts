export interface PageResult<T> {
  records: T[]
  total: number | string
  pageNum: number | string
  pageSize: number | string
}

export interface UserVO {
  id: string
  username: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  status: number
  remark?: string
  roleIds?: string[]
  roleCodes?: string[]
  createTime?: string
  updateTime?: string
}

export interface UserSaveRequest {
  username: string
  password?: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  status?: number
  remark?: string
  roleIds?: string[]
}

export interface RoleVO {
  id: string
  roleCode: string
  roleName: string
  sort?: number
  status: number
  remark?: string
  menuIds?: string[]
  createTime?: string
  updateTime?: string
}

export interface RoleSaveRequest {
  roleCode: string
  roleName: string
  sort?: number
  status?: number
  remark?: string
}

export interface MenuDetailVO {
  id: string
  parentId: string
  menuName: string
  menuType: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  remark?: string
  /** 内置菜单只读，不可编辑删除 */
  builtIn?: boolean
  children?: MenuDetailVO[]
}

export interface MenuSaveRequest {
  parentId: string
  menuName: string
  menuType: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  remark?: string
}
