<template>
  <div class="login-page">
    <section class="login-visual">
      <div class="visual-glow glow-a"></div>
      <div class="visual-glow glow-b"></div>
      <div class="grain"></div>

      <div class="visual-top">
        <BrandMark light />
        <span class="visual-badge">XATU · 2026</span>
      </div>

      <div class="visual-copy">
        <span class="eyebrow">CAMPUS CANTEEN · ONLINE ORDER</span>
        <h1>把排队的时间<br /><em>留给好好吃饭</em></h1>
        <p>今日菜单、档口库存、取餐进度一屏掌握。提前下单，到店即取，午高峰也能从容一点。</p>
      </div>

      <div class="visual-stats">
        <div>
          <strong>3</strong>
          <span>个食堂档口</span>
        </div>
        <div>
          <strong>10+</strong>
          <span>每日供应菜品</span>
        </div>
        <div>
          <strong>15<span>min</span></strong>
          <span>订单保留时间</span>
        </div>
      </div>

      <div class="ticket" aria-hidden="true">
        <div class="ticket-left">
          <small>PICKUP CODE</small>
          <strong>A · 028</strong>
        </div>
        <div class="ticket-cut"></div>
        <div class="ticket-right">
          <span>一食堂 · 家常菜</span>
          <span>07:00 — 20:00</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="mobile-brand">
          <BrandMark />
        </div>

        <span class="form-eyebrow">SIGN IN</span>
        <h2>欢迎回来</h2>
        <p class="form-sub">使用校园账号登录，开始今天的一餐。</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
          @submit.prevent="submit"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model.trim="form.username"
              size="large"
              placeholder="学号 / 工号 / 管理员账号"
              :prefix-icon="User"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="auth.loading"
            @click="submit"
          >
            进入食堂
            <el-icon class="submit-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="divider"><span>演示账号 · 密码均为 123456</span></div>

        <div class="demo-accounts">
          <button
            v-for="account in demoAccounts"
            :key="account.username"
            type="button"
            class="demo-account"
            @click="fill(account.username)"
          >
            <span class="demo-dot" :class="account.tone"></span>
            <span class="demo-copy">
              <strong>{{ account.label }}</strong>
              <small>{{ account.username }}</small>
            </span>
          </button>
        </div>

        <p class="login-tip">账号由食堂统一开通，如无法登录请联系系统管理员。</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const demoAccounts = [
  { label: '学生点餐', username: '20230001', tone: 'orange' },
  { label: '档口员工', username: 'staff01', tone: 'green' },
  { label: '系统管理员', username: 'admin', tone: 'dark' }
]

function fill(username: string) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    const data = await auth.login({ username: form.username, password: form.password })
    if (data.user.roleCode === 'STUDENT') {
      cart.fetchCart().catch(() => undefined)
    }
    ElMessage.success(`欢迎回来，${data.user.realName || data.user.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const target = redirect.startsWith('/') ? redirect : auth.homePath
    await router.replace(target)
  } catch {
    // 错误提示由请求层统一处理
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  background: #f6f1e8;
}

.login-visual {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  padding: 38px 54px 40px;
  display: flex;
  flex-direction: column;
  color: #fff;
  background:
    radial-gradient(circle at 18% 12%, rgba(90, 155, 120, 0.4) 0, transparent 34%),
    radial-gradient(circle at 82% 82%, rgba(216, 87, 42, 0.3) 0, transparent 34%),
    linear-gradient(155deg, #1c4a39 0%, #123227 58%, #0d241c 100%);
}

.visual-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}

.glow-a {
  width: 460px;
  height: 460px;
  right: -160px;
  top: -140px;
  border: 38px solid rgba(255, 255, 255, 0.06);
}

.glow-b {
  width: 320px;
  height: 320px;
  left: -120px;
  bottom: -80px;
  background: rgba(216, 87, 42, 0.16);
  filter: blur(30px);
}

.grain {
  position: absolute;
  inset: 0;
  opacity: 0.18;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.4) 0.7px, transparent 0.7px);
  background-size: 7px 7px;
  mask-image: linear-gradient(to bottom, #000, transparent 65%);
}

.visual-top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.visual-badge {
  padding: 5px 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.7);
}

.visual-copy {
  position: relative;
  z-index: 2;
  margin-top: clamp(48px, 9vh, 104px);
  max-width: 560px;
}

.eyebrow {
  display: block;
  margin-bottom: 18px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(255, 255, 255, 0.5);
}

h1 {
  margin: 0;
  font-size: clamp(38px, 4.4vw, 62px);
  line-height: 1.13;
  font-weight: 850;
  letter-spacing: -0.045em;
}

h1 em {
  font-style: normal;
  color: #f4b48e;
}

.visual-copy p {
  max-width: 460px;
  margin: 22px 0 0;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.68);
}

.visual-stats {
  position: relative;
  z-index: 2;
  display: flex;
  gap: clamp(24px, 4vw, 58px);
  margin-top: auto;
  padding-top: 38px;
}

.visual-stats strong {
  display: block;
  font-size: clamp(24px, 2.6vw, 34px);
  line-height: 1;
  font-weight: 850;
  letter-spacing: -0.03em;
  color: #fff;
}

.visual-stats strong span {
  font-size: 12px;
  margin-left: 2px;
  color: #f4b48e;
}

.visual-stats span {
  display: block;
  margin-top: 7px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.ticket {
  position: absolute;
  right: 48px;
  bottom: 42px;
  z-index: 2;
  width: 244px;
  padding: 15px 16px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 250, 242, 0.94);
  color: #24342c;
  box-shadow: 0 22px 50px rgba(0, 0, 0, 0.28);
  transform: rotate(-4deg);
}

.ticket-left small {
  display: block;
  font-size: 8px;
  letter-spacing: 0.18em;
  color: #9c8f7d;
}

.ticket-left strong {
  display: block;
  margin-top: 4px;
  font-size: 23px;
  letter-spacing: 0.06em;
  color: #b8461f;
}

.ticket-cut {
  width: 1px;
  align-self: stretch;
  background: repeating-linear-gradient(to bottom, #d8ccbb 0 4px, transparent 4px 8px);
}

.ticket-right {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 10px;
  color: #6f675c;
}

.ticket-right span:first-child {
  font-weight: 700;
  color: #29362f;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background:
    radial-gradient(circle at 90% 0%, rgba(255, 244, 228, 0.9) 0, transparent 35%),
    #f6f1e8;
}

.login-card {
  width: min(430px, 100%);
  padding: 38px 38px 30px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(232, 224, 211, 0.9);
  box-shadow: 0 30px 70px rgba(47, 34, 18, 0.12);
  backdrop-filter: blur(10px);
}

.mobile-brand {
  display: none;
  margin-bottom: 26px;
}

.form-eyebrow {
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.22em;
  color: var(--brand);
}

h2 {
  margin: 8px 0 0;
  font-size: 30px;
  font-weight: 850;
  letter-spacing: -0.03em;
}

.form-sub {
  margin: 8px 0 26px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  color: #4c5850;
  font-size: 12px;
  font-weight: 650;
}

.login-form :deep(.el-input__wrapper) {
  padding: 3px 13px;
  background: #fcfaf6;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  font-size: 14px;
}

.submit-arrow {
  margin-left: 6px;
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 16px;
  color: #a89d8d;
  font-size: 10px;
  letter-spacing: 0.03em;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eee5d8;
}

.demo-accounts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.demo-account {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fffcf7;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.demo-account:hover {
  border-color: #e5b7a3;
  transform: translateY(-1px);
}

.demo-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.demo-dot.orange {
  background: var(--brand);
}

.demo-dot.green {
  background: var(--accent);
}

.demo-dot.dark {
  background: var(--forest);
}

.demo-copy {
  min-width: 0;
}

.demo-copy strong {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: #46524b;
  white-space: nowrap;
}

.demo-copy small {
  display: block;
  margin-top: 1px;
  font-size: 9px;
  color: #9a9285;
}

.login-tip {
  margin: 20px 0 0;
  color: #a59b8c;
  font-size: 10px;
  line-height: 1.6;
  text-align: center;
}

@media (max-width: 1080px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-visual {
    display: none;
  }

  .login-panel {
    min-height: 100vh;
    padding: 28px 20px;
  }

  .mobile-brand {
    display: block;
  }
}
</style>
