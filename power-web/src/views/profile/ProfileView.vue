<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageCard from '@/components/PageCard.vue'
import { changePassword, updateProfile } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { resetRouter } from '@/router'

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('profile')
const profileSubmitting = ref(false)
const passwordSubmitting = ref(false)
const profileRef = ref<FormInstance>()
const passwordRef = ref<FormInstance>()

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const roleText = computed(() => auth.user?.roles?.join('、') || '-')

const profileRules: FormRules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

function syncProfileForm() {
  const user = auth.user
  profileForm.nickname = user?.nickname || ''
  profileForm.email = user?.email || ''
  profileForm.phone = user?.phone || ''
  profileForm.avatar = user?.avatar || ''
}

async function submitProfile() {
  await profileRef.value?.validate()
  profileSubmitting.value = true
  try {
    const user = await updateProfile({
      nickname: profileForm.nickname.trim() || undefined,
      email: profileForm.email.trim() || undefined,
      phone: profileForm.phone.trim() || undefined,
      avatar: profileForm.avatar.trim() || undefined,
    })
    auth.user = user
    ElMessage.success('资料已保存')
  } finally {
    profileSubmitting.value = false
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordRef.value?.clearValidate()
}

async function submitPassword() {
  await passwordRef.value?.validate()
  passwordSubmitting.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    await auth.logout()
    resetRouter()
    router.replace('/login')
  } finally {
    passwordSubmitting.value = false
  }
}

onMounted(() => {
  syncProfileForm()
})
</script>

<template>
  <PageCard title="个人中心">
    <div class="profile-layout">
      <aside class="profile-aside">
        <div class="avatar">{{ (auth.displayName || 'U').charAt(0).toUpperCase() }}</div>
        <div class="aside-name">{{ auth.displayName }}</div>
        <div class="aside-username">@{{ auth.user?.username }}</div>
        <dl class="aside-meta">
          <div class="meta-row">
            <dt>角色</dt>
            <dd>{{ roleText }}</dd>
          </div>
          <div class="meta-row">
            <dt>状态</dt>
            <dd>
              <el-tag :type="auth.user?.status === 1 ? 'success' : 'info'" size="small">
                {{ auth.user?.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </dd>
          </div>
        </dl>
      </aside>

      <section class="profile-main">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基本资料" name="profile">
            <el-form
              ref="profileRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="72px"
              class="profile-form"
            >
              <el-form-item label="登录名">
                <el-input :model-value="auth.user?.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="显示名称" maxlength="64" />
              </el-form-item>
              <el-form-item label="手机" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="手机号" maxlength="20" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" placeholder="邮箱" maxlength="128" />
              </el-form-item>
              <el-form-item label="头像">
                <el-input v-model="profileForm.avatar" placeholder="头像 URL（可选）" maxlength="255" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="profileSubmitting" @click="submitProfile">
                  保存资料
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="pwd-tip"
              title="修改成功后当前登录态将失效，需使用新密码重新登录。"
            />
            <el-form
              ref="passwordRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="72px"
              class="profile-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  show-password
                  autocomplete="current-password"
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="至少 6 位"
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">
                  修改密码
                </el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </PageCard>
</template>

<style scoped lang="scss">
.profile-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.profile-aside {
  flex-shrink: 0;
  width: 168px;
  padding: 16px 12px;
  border: 1px solid var(--power-border-light);
  border-radius: var(--power-radius);
  background: var(--el-fill-color-light);
  text-align: center;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: var(--power-primary-light);
  color: var(--power-primary);
  font-size: 20px;
  font-weight: 600;
}

.aside-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--power-text);
}

.aside-username {
  margin-top: 2px;
  font-size: 11px;
  color: var(--power-text-muted);
}

.aside-meta {
  margin: 14px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--power-border);
  text-align: left;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11px;

  dt {
    flex-shrink: 0;
    width: 32px;
    margin: 0;
    color: var(--power-text-muted);
  }

  dd {
    margin: 0;
    color: var(--power-text-secondary);
    word-break: break-all;
  }
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }

  :deep(.el-tabs__item) {
    font-size: 12px;
    height: 32px;
  }
}

.profile-form {
  max-width: 420px;
}

.pwd-tip {
  max-width: 420px;
  margin-bottom: 12px;
}

@media (max-width: 720px) {
  .profile-layout {
    flex-direction: column;
  }

  .profile-aside {
    width: 100%;
  }
}
</style>
