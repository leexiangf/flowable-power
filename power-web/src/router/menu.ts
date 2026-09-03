import type { RouteRecordRaw } from 'vue-router'
import type { MenuVO } from '@/types/api'
import RouteView from '@/layout/RouteView.vue'

const viewModules = import.meta.glob('@/views/**/*.vue')

function resolveView(component?: string) {
  if (!component) {
    return RouteView
  }
  const suffix = `/views/${component}.vue`
  const entry = Object.keys(viewModules).find((key) => key.endsWith(suffix))
  if (entry) {
    return viewModules[entry]
  }
  return () => import('@/views/error/Placeholder.vue')
}

function normalizePath(path?: string, isChild = false) {
  if (!path) return isChild ? 'index' : '/'
  if (isChild) {
    return path.replace(/^\//, '')
  }
  return path.startsWith('/') ? path : `/${path}`
}

function menuToRoutes(menus: MenuVO[], isChild = false): RouteRecordRaw[] {
  return menus.map((menu) => {
    const hasChildren = menu.children && menu.children.length > 0
    const isDirectory = menu.menuType === 1 || hasChildren

    const route = {
      path: normalizePath(menu.path, isChild),
      name: `menu-${menu.id}`,
      meta: {
        title: menu.menuName,
        icon: menu.icon,
        perms: menu.perms,
        menuId: menu.id,
      },
      component: RouteView,
    } as RouteRecordRaw

    if (isDirectory) {
      if (hasChildren) {
        ;(route as RouteRecordRaw & { children?: RouteRecordRaw[] }).children =
          menuToRoutes(menu.children!, true)
        const firstLeaf = findFirstLeaf(menu.children!)
        if (firstLeaf?.path) {
          route.redirect = { name: `menu-${firstLeaf.id}` }
        }
      }
    } else {
      route.component = resolveView(menu.component)
    }

    return route
  })
}

function findFirstLeaf(menus: MenuVO[]): MenuVO | undefined {
  for (const menu of menus) {
    if (menu.menuType === 2 && menu.component) {
      return menu
    }
    if (menu.children?.length) {
      const found = findFirstLeaf(menu.children)
      if (found) return found
    }
  }
  return menus[0]
}

export function buildRoutesFromMenus(menus: MenuVO[]): RouteRecordRaw[] {
  return menuToRoutes(menus)
}
