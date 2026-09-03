import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import { buildRoutesFromMenus } from '@/router/menu'
import { useAuthStore } from '@/stores/auth'
import { getAccessToken } from '@/utils/auth'
import AppLayout from '@/layout/AppLayout.vue'

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    name: 'Root',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '首页', affix: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
})

let dynamicRouteNames: string[] = []
let notFoundRegistered = false

function removeDynamicRoutes() {
  dynamicRouteNames.forEach((name) => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  dynamicRouteNames = []

  if (notFoundRegistered && router.hasRoute('NotFound')) {
    router.removeRoute('NotFound')
    notFoundRegistered = false
  }
}

export function resetRouter() {
  removeDynamicRoutes()
  const auth = useAuthStore()
  auth.routesLoaded = false
}

export async function setupDynamicRoutes(): Promise<boolean> {
  const auth = useAuthStore()
  if (auth.routesLoaded) {
    return false
  }

  removeDynamicRoutes()

  const dynamicRoutes = buildRoutesFromMenus(auth.menus)
  dynamicRoutes.forEach((route) => {
    router.addRoute('Root', route)
    if (route.name) {
      dynamicRouteNames.push(route.name as string)
    }
    route.children?.forEach((child) => {
      if (child.name) {
        dynamicRouteNames.push(child.name as string)
      }
    })
  })

  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404' },
  })
  notFoundRegistered = true

  auth.markRoutesLoaded()
  return true
}

function resolveRedirectTarget(to: RouteLocationNormalized) {
  if (to.name) {
    return {
      name: to.name,
      params: to.params,
      query: to.query,
      hash: to.hash,
      replace: true,
    }
  }
  return {
    path: to.path,
    query: to.query,
    hash: to.hash,
    replace: true,
  }
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const token = getAccessToken()

  if (!token) {
    if (to.meta.public === true) {
      return true
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login') {
    return { name: 'Dashboard', replace: true }
  }

  if (!auth.user) {
    try {
      await auth.loadUserContext()
    } catch {
      auth.reset()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  const routesJustAdded = await setupDynamicRoutes()
  if (routesJustAdded) {
    return resolveRedirectTarget(to)
  }

  return true
})

export default router
