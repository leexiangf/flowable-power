<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { resetRouter } from '@/router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123',
})

onMounted(() => {
  if (route.query.kicked) {
    ElMessage.warning('账号已在其他端登录，请重新登录')
  }
})

async function navigateAfterLogin() {
  const redirect = route.query.redirect as string | undefined
  if (redirect && redirect.startsWith('/') && redirect !== '/login') {
    await router.replace(redirect)
    return
  }
  await router.replace({ name: 'Dashboard' })
}

async function handleLogin() {
  if (loading.value) return
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    resetRouter()
    await auth.login(form.username, form.password)
    await nextTick()
    await navigateAfterLogin()
  } catch (err: unknown) {
    console.error('login failed', err)
    const msg =
      (err as { message?: string })?.message ||
      (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || '登录失败，请检查账号密码或后端网关是否已启动（8080）')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-panel">
      <div class="brand">
        <span class="brand-mark">P</span>
        <div>
          <h1>Power 管理平台</h1>
          <p class="subtitle">企业权限与工作流管理</p>
        </div>
      </div>

      <el-form :model="form" class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" native-type="submit" class="login-btn" :loading="loading">
          登录
        </el-button>
      </el-form>

      <p class="hint">默认账号 admin / admin123</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--power-bg);
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 50% at 20% 40%, rgba(37, 99, 235, 0.08), transparent),
    radial-gradient(ellipse 50% 40% at 80% 60%, rgba(37, 99, 235, 0.05), transparent);
  pointer-events: none;
}

.login-panel {
  position: relative;
  width: 100%;
  max-width: 360px;
  padding: 32px 28px;
  background: var(--power-surface);
  border: 1px solid var(--power-border);
  border-radius: 10px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;

  .brand-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 7px;
    background: var(--power-primary);
    color: #fff;
    font-size: 13px;
    font-weight: 700;
    flex-shrink: 0;
  }

  h1 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--power-text);
  }

  .subtitle {
    margin: 2px 0 0;
    font-size: 11px;
    color: var(--power-text-muted);
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.hint {
  margin: 14px 0 0;
  text-align: center;
  color: var(--power-text-muted);
  font-size: 10px;
}
</style>
