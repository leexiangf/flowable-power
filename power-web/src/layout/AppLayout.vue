<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Fold,
  Expand,
  HomeFilled,
  Menu as MenuIcon,
  Setting,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { MenuVO } from '@/types/api'
import { resetRouter } from '@/router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)

const asideWidth = computed(() => (collapsed.value ? '56px' : '200px'))

const activeMenu = computed(() => route.path)

const iconMap: Record<string, unknown> = {
  setting: Setting,
  user: User,
  menu: MenuIcon,
  tree: MenuIcon,
  example: HomeFilled,
  guide: HomeFilled,
  list: MenuIcon,
  edit: MenuIcon,
  form: MenuIcon,
  'tree-table': MenuIcon,
  build: MenuIcon,
  peoples: User,
}

function resolveIcon(name?: string) {
  if (!name) return MenuIcon
  return iconMap[name] || MenuIcon
}

function menuIndex(menu: MenuVO, parentPath = '') {
  const segment = (menu.path || '').replace(/^\//, '')
  if (menu.path?.startsWith('/')) {
    return menu.path
  }
  const base = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath
  return `${base}/${segment}`.replace(/\/+/g, '/')
}

async function handleLogout() {
  await auth.logout()
  resetRouter()
  router.replace('/login')
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="asideWidth" class="aside">
      <div class="logo">
        <span class="logo-mark">P</span>
        <span v-if="!collapsed" class="logo-text">Power</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="side-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon :size="14"><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <template v-for="menu in auth.menus" :key="menu.id">
            <el-sub-menu v-if="menu.children?.length" :index="menuIndex(menu)">
              <template #title>
                <el-icon :size="14"><component :is="resolveIcon(menu.icon)" /></el-icon>
                <span>{{ menu.menuName }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.id"
                :index="menuIndex(child, menuIndex(menu))"
              >
                <span>{{ child.menuName }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menuIndex(menu)">
              <el-icon :size="14"><component :is="resolveIcon(menu.icon)" /></el-icon>
              <template #title>{{ menu.menuName }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="header" height="var(--power-header-h)">
        <div class="header-left">
          <button class="icon-btn" type="button" @click="collapsed = !collapsed">
            <el-icon :size="16">
              <Fold v-if="!collapsed" />
              <Expand v-else />
            </el-icon>
          </button>
          <span class="page-title">{{ route.meta.title || 'Power' }}</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-entry">
              <span class="user-avatar">{{ (auth.displayName || 'U').charAt(0) }}</span>
              <span class="user-name">{{ auth.displayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  background: var(--power-bg);
}

.aside {
  display: flex;
  flex-direction: column;
  background: var(--power-surface);
  border-right: 1px solid var(--power-border);
  transition: width 0.2s ease;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: var(--power-header-h);
  padding: 0 14px;
  border-bottom: 1px solid var(--power-border-light);
  flex-shrink: 0;

  .logo-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 5px;
    background: var(--power-primary);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 13px;
    font-weight: 600;
    color: var(--power-text);
    letter-spacing: -0.02em;
  }
}

.menu-scroll {
  flex: 1;
}

.side-menu {
  border-right: none;
  padding: 6px 8px;
  background: transparent;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 32px;
    line-height: 32px;
    margin-bottom: 1px;
    border-radius: var(--power-radius);
    color: var(--power-text-secondary);
    font-size: 12px;

    .el-icon {
      margin-right: 6px;
    }
  }

  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    background: var(--power-border-light);
    color: var(--power-text);
  }

  :deep(.el-menu-item.is-active) {
    background: var(--power-primary-light);
    color: var(--power-primary);
    font-weight: 500;
  }

  :deep(.el-sub-menu .el-menu-item) {
    min-width: auto;
    padding-left: 38px !important;
    height: 30px;
    line-height: 30px;
    font-size: 11px;
  }

  :deep(.el-sub-menu__icon-arrow) {
    font-size: 11px;
  }
}

.main-wrap {
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--power-surface);
  border-bottom: 1px solid var(--power-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--power-radius);
  background: transparent;
  color: var(--power-text-secondary);
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: var(--power-border-light);
    color: var(--power-text);
  }
}

.page-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--power-text);
  margin-left: 4px;
}

.header-right {
  flex-shrink: 0;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: var(--power-radius);
  transition: background 0.15s;

  &:hover {
    background: var(--power-border-light);
  }
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--power-primary-light);
  color: var(--power-primary);
  font-size: 10px;
  font-weight: 600;
}

.user-name {
  font-size: 12px;
  color: var(--power-text);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  padding: 10px 14px;
  background: var(--power-bg);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.12s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
