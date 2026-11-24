<template>
  <div class="page-shell profile-page">
    <div class="page-head">
      <div>
        <span class="eyebrow">MY ACCOUNT</span>
        <h1>个人中心</h1>
        <p>管理个人资料、校园卡余额与登录状态。</p>
      </div>
    </div>

    <section class="profile-hero">
      <div class="hero-pattern"></div>
      <div class="profile-avatar">{{ initialOf(auth.displayName) }}</div>
      <div class="profile-copy">
        <h2>{{ auth.displayName }}</h2>
        <p>
          <span>{{ auth.user?.username }}</span>
          <i></i>
          <span>{{ roleLabel }}</span>
        </p>
      </div>
      <div class="hero-balance">
        <span>校园卡余额</span>
        <strong>{{ money(balance) }}</strong>
        <small>可用于档口订单支付</small>
      </div>
    </section>

    <div class="profile-grid">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>个人资料</h3>
            <p>姓名与手机号用于取餐通知，请保持准确。</p>
          </div>
          <el-icon class="panel-icon"><User /></el-icon>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          class="profile-form"
        >
          <div class="form-grid">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model.trim="profileForm.realName" size="large" maxlength="20" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model.trim="profileForm.phone" size="large" maxlength="11" placeholder="用于接收取餐提醒" />
            </el-form-item>
          </div>

          <el-form-item label="学号 / 工号">
            <el-input :model-value="auth.user?.username" size="large" disabled />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-form>
      </section>

      <div class="side-column">
        <section class="panel recharge-panel">
          <div class="panel-head">
            <div>
              <h3>余额充值</h3>
              <p>充值后可直接支付订单。</p>
            </div>
            <el-icon class="panel-icon"><Wallet /></el-icon>
          </div>

          <div class="quick-amounts">
            <button
              v-for="item in quickAmounts"
              :key="item"
              type="button"
              class="amount-chip"
              :class="{ active: rechargeAmount === item }"
              @click="rechargeAmount = item"
            >
              ¥{{ item }}
            </button>
          </div>

          <div class="amount-input">
            <span>自定义金额</span>
            <el-input-number
              v-model="rechargeAmount"
              :min="1"
              :max="1000"
              :step="10"
              controls-position="right"
            />
          </div>

          <el-button
            type="primary"
            size="large"
            class="recharge-btn"
            :loading="recharging"
            @click="doRecharge"
          >
            立即充值
          </el-button>

          <p class="recharge-note">当前为演示支付环境，充值金额即时入账。</p>
        </section>

        <section class="panel account-panel">
          <div class="panel-head compact">
            <div>
              <h3>账号与安全</h3>
              <p>账号由食堂统一开通。</p>
            </div>
            <el-icon class="panel-icon"><Lock /></el-icon>
          </div>

          <dl class="account-list">
            <div>
              <dt>账号角色</dt>
              <dd>{{ roleLabel }}</dd>
            </div>
            <div>
              <dt>账号状态</dt>
              <dd><span class="status-dot"></span>正常</dd>
            </div>
            <div>
              <dt>登录方式</dt>
              <dd>账号密码 + JWT</dd>
            </div>
          </dl>

          <el-button class="logout-full" @click="logout">
            <el-icon><SwitchButton /></el-icon>退出当前账号
          </el-button>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, SwitchButton, User, Wallet } from '@element-plus/icons-vue'
import { getBalance, recharge, updateMe } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ROLE_MAP, initialOf, money } from '@/utils/format'
import type { Role } from '@/types'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

const profileFormRef = ref<FormInstance>()
const profileForm = reactive({ realName: '', phone: '' })
const balance = ref(Number(auth.user?.balance || 0))
const saving = ref(false)
const recharging = ref(false)
const rechargeAmount = ref(50)
const quickAmounts = [20, 50, 100, 200]

const roleLabel = computed(() => ROLE_MAP[(auth.role || 'STUDENT') as Role] || '用户')

const profileRules: FormRules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ]
}

function syncForm() {
  profileForm.realName = auth.user?.realName || ''
  const phone = auth.user?.phone || ''
  profileForm.phone = phone.includes('*') ? '' : phone
}

function resetForm() {
  syncForm()
  profileFormRef.value?.clearValidate()
}

async function saveProfile() {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateMe({
      realName: profileForm.realName,
      phone: profileForm.phone
    })
    await auth.fetchMe()
    syncForm()
    ElMessage.success('个人资料已更新')
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    saving.value = false
  }
}

async function refreshBalance() {
  try {
    const value = await getBalance()
    balance.value = Number(value || 0)
    if (auth.user) auth.user.balance = balance.value
  } catch {
    // 忽略
  }
}

async function doRecharge() {
  if (rechargeAmount.value < 1) {
    ElMessage.warning('充值金额至少为 1 元')
    return
  }

  recharging.value = true
  try {
    const value = await recharge({ amount: rechargeAmount.value })
    balance.value = Number(value || 0)
    if (auth.user) auth.user.balance = balance.value
    ElMessage.success(`充值成功，当前余额 ${money(balance.value)}`)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    recharging.value = false
  }
}

async function logout() {
  await auth.logout()
  cart.reset()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(async () => {
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      // 忽略
    }
  }
  syncForm()
  refreshBalance()
})
</script>

<style scoped>
.page-head {
  margin-bottom: 22px;
}

.eyebrow {
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.22em;
  color: var(--brand);
}

h1 {
  margin: 7px 0 0;
  font-size: 30px;
  font-weight: 850;
  letter-spacing: -0.035em;
}

.page-head p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.profile-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 28px 32px;
  border-radius: 22px;
  color: #fff;
  background:
    radial-gradient(circle at 82% 20%, rgba(230, 142, 98, 0.24) 0, transparent 32%),
    linear-gradient(130deg, #1c4a39 0%, #133126 70%, #0f2a20 100%);
  box-shadow: 0 18px 42px rgba(20, 48, 37, 0.18);
  margin-bottom: 20px;
}

.hero-pattern {
  position: absolute;
  right: -50px;
  top: -80px;
  width: 230px;
  height: 230px;
  border-radius: 50%;
  border: 26px solid rgba(255, 255, 255, 0.07);
}

.profile-avatar {
  position: relative;
  z-index: 2;
  width: 68px;
  height: 68px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 850;
  color: #234335;
  background: #efe3cf;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.15);
}

.profile-copy {
  position: relative;
  z-index: 2;
  flex: 1;
  min-width: 0;
}

.profile-copy h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.profile-copy p {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
}

.profile-copy p i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
}

.hero-balance {
  position: relative;
  z-index: 2;
  min-width: 180px;
  padding-left: 26px;
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  display: flex;
  flex-direction: column;
}

.hero-balance span {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.hero-balance strong {
  margin-top: 3px;
  font-size: 32px;
  line-height: 1.1;
  font-weight: 850;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
}

.hero-balance small {
  margin-top: 3px;
  font-size: 9px;
  color: rgba(255, 255, 255, 0.4);
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: start;
}

.panel {
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 8px 24px rgba(47, 34, 18, 0.05);
  padding: 22px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.panel-head h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.panel-head p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.panel-head.compact {
  margin-bottom: 14px;
}

.panel-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: var(--brand);
  background: var(--brand-soft);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
}

.profile-form :deep(.el-form-item__label) {
  padding-bottom: 5px;
  color: #4c5850;
  font-size: 11px;
  font-weight: 650;
}

.form-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.amount-chip {
  padding: 10px 0;
  border: 1px solid #e8dfd1;
  border-radius: 9px;
  background: #fcfaf6;
  color: #57645c;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.amount-chip:hover {
  border-color: #e6bdab;
  color: var(--brand);
}

.amount-chip.active {
  border-color: var(--brand);
  color: var(--brand);
  background: #fdf0e9;
}

.amount-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  color: #6f7a72;
  font-size: 11px;
}

.amount-input :deep(.el-input-number) {
  width: 150px;
}

.recharge-btn {
  width: 100%;
  height: 44px;
}

.recharge-note {
  margin: 10px 0 0;
  text-align: center;
  color: #a49a8b;
  font-size: 9px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.account-list {
  margin: 0 0 16px;
}

.account-list > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 2px;
  border-bottom: 1px solid #f6f0e7;
  font-size: 11px;
}

.account-list dt {
  color: #8d948c;
}

.account-list dd {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  color: #46524b;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #41a06a;
}

.logout-full {
  width: 100%;
  color: #8a8376;
}

.logout-full:hover {
  color: #c24a2c;
  border-color: #edc3b7;
  background: #fdf3ef;
}

@media (max-width: 1000px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .profile-hero {
    flex-wrap: wrap;
    padding: 22px;
  }

  .hero-balance {
    width: 100%;
    min-width: 0;
    padding: 14px 0 0;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.14);
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
