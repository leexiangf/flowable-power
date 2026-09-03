import { useAuthStore } from '@/stores/auth'

/** 是否拥有任一权限码（与后端 @authz.permit 一致） */
export function hasPerm(perm: string): boolean {
  const auth = useAuthStore()
  if (!perm) return true
  const authorities = auth.authorities
  if (authorities.includes('*')) return true
  return authorities.includes(perm)
}

/** 是否拥有任一权限码 */
export function hasAnyPerm(perms: string[]): boolean {
  return perms.some((p) => hasPerm(p))
}
