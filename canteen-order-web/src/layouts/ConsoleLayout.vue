<template>
  <div class="console-shell">
    <aside class="aside">
      <div class="aside-brand">
        <BrandMark light />
        <span class="console-pill">{{ isAdmin ? 'ADMIN CONSOLE' : 'STAFF CONSOLE' }}</span>
      </div>

      <nav class="side-nav" aria-label="控制台导航">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="side-link"
          active-class="active"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <i class="side-arrow"></i>
        </RouterLink>
      </nav>

      <div class="aside-foot">
        <div class="side-user">
          <span class="side-avatar">{{ initialOf(auth.displayName) }}</span>
          <div class="side-user-copy">
            <strong>{{ auth.displayName }}</strong>
            <small>{{ auth.user?.username }} · {{ roleLabel }}</small>
          </div>
        </div>
        <el-button class="logout-btn" text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>退出
        </el-button>
      </div>
    </aside>

    <main class="console-main">
      <header class="console-top">
        <div>
          <div class="crumb">
            <span class="crumb-dot"></span>
            {{ isAdmin ? '经营管理' : '档口作业' }}
            <span class="crumb-sep">/</span>
            {{ currentTitle }}
          </div>
          <h1 class="console-title">{{ currentTitle }}</h1>
        </div>

        <div class="console-actions">
          <el-tooltip content="刷新当前页面" placement="bottom">
            <el-button circle class="icon-btn" @click="reload">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleCommand">
            <button class="console-user" type="button">
              <span class="avatar">{{ initialOf(auth.displayName) }}</span>
              <span>{{ auth.displayName }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="console-body">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Refresh, SwitchButton } from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ROLE_MAP, initialOf } from '@/utils/format'
import type { Role } from '@/types'

interface MenuItem {
  label: string
  path: string
  icon: string
}

const ADMIN_MENU: MenuItem[] = [
  { label: '经营概览', path: '/admin/dashboard', icon: 'Odometer' },
  { label: '菜品管理', path: '/admin/dishes', icon: 'Dish' },
  { label: '分类管理', path: '/admin/categories', icon: 'Files' },
  { label: '档口管理', path: '/admin/stalls', icon: 'Shop' },
  { label: '每日菜单', path: '/admin/menus', icon: 'Calendar' },
  { label: '订单管理', path: '/admin/orders', icon: 'List' },
  { label: '用户管理', path: '/admin/users', icon: 'User' },
  { label: '操作日志', path: '/admin/logs', icon: 'Document' },
  { label: '参数配置', path: '/admin/configs', icon: 'Setting' }
]

const STAFF_MENU: MenuItem[] = [
  { label: '出餐看板', path: '/staff/board', icon: 'BellFilled' }
]

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const route = useRoute()

const isAdmin = computed(() => auth.role === 'ADMIN')
const roleLabel = computed(() => ROLE_MAP[(auth.role || 'STUDENT') as Role] || '用户')
const menuItems = computed(() => (isAdmin.value ? ADMIN_MENU : STAFF_MENU))
const currentTitle = computed(() => String(route.meta.title || '控制台'))

function reload() {
  router.go(0)
}

async function handleLogout() {
  await auth.logout()
  cart.reset()
  ElMessage.success('已退出登录')
  router.push('/login')
}

function handleCommand(command: string) {
  if (command === 'logout') handleLogout()
}
</script>

<style scoped>
.console-shell {
  min-height: 100vh;
  display: flex;
  background: #f4f0e8;
}

.aside {
  position: sticky;
  top: 0;
  height: 100vh;
  width: 244px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 22px 16px 18px;
  color: #fff;
  background:
    radial-gradient(circle at 15% 0%, rgba(80, 143, 108, 0.45) 0, transparent 36%),
    linear-gradient(170deg, #1b4939 0%, #143327 62%, #0f2a20 100%);
  overflow: hidden;
}

.aside::after {
  content: '';
  position: absolute;
  right: -60px;
  bottom: 80px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 28px solid rgba(255, 255, 255, 0.05);
}

.aside-brand {
  position: relative;
  z-index: 2;
  padding: 4px 8px 20px;
}

.console-pill {
  display: inline-block;
  margin-top: 12px;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.62);
  background: rgba(255, 255, 255, 0.08);
}

.side-nav {
  position: relative;
  z-index: 2;
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  padding-right: 2px;
}

.side-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 13px;
  border-radius: 11px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  font-weight: 600;
  transition: color 0.18s ease, background 0.18s ease;
}

.side-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.side-link.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.side-link.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: #f08a5d;
  transform: translateY(-50%);
}

.side-arrow {
  margin-left: auto;
  width: 5px;
  height: 5px;
  border-right: 1px solid currentColor;
  border-top: 1px solid currentColor;
  transform: rotate(45deg);
  opacity: 0.4;
}

.aside-foot {
  position: relative;
  z-index: 2;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.side-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #173f32;
  background: #e9dcc8;
}

.side-user-copy {
  min-width: 0;
}

.side-user-copy strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.side-user-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.logout-btn {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
  padding: 6px;
}

.logout-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.console-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.console-top {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 32px 16px;
  background: rgba(246, 242, 234, 0.88);
  border-bottom: 1px solid rgba(232, 224, 211, 0.8);
  backdrop-filter: blur(14px);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.crumb-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}

.crumb-sep {
  color: #b8b0a2;
}

.console-title {
  margin: 5px 0 0;
  font-size: 22px;
  font-weight: 850;
  letter-spacing: -0.02em;
  color: var(--ink);
}

.console-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-btn {
  border-color: var(--line);
  background: #fff;
  color: #66716a;
}

.console-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 4px 10px 4px 4px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  font-weight: 650;
  color: var(--ink);
}

.console-user .avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(145deg, #2f8f5b, #1d6a42);
}

.console-body {
  flex: 1;
  padding: 26px 32px 46px;
}

@media (max-width: 900px) {
  .console-shell {
    display: block;
  }

  .aside {
    position: relative;
    top: 0;
    width: 100%;
    height: auto;
    padding: 12px 14px 10px;
    flex-direction: row;
    align-items: center;
    gap: 12px;
    overflow: visible;
  }

  .aside::after {
    display: none;
  }

  .aside-brand {
    padding: 0;
    flex-shrink: 0;
  }

  .aside-brand :deep(.brand-copy) {
    display: none;
  }

  .console-pill {
    display: none;
  }

  .side-nav {
    flex: 1;
    flex-direction: row;
    gap: 3px;
    overflow-x: auto;
    margin: 0;
    padding-bottom: 2px;
  }

  .side-link {
    flex-shrink: 0;
    padding: 8px 10px;
    white-space: nowrap;
    font-size: 11px;
  }

  .side-link.active::before,
  .side-arrow {
    display: none;
  }

  .aside-foot {
    display: none;
  }

  .console-top {
    position: relative;
    padding: 14px 16px 12px;
  }

  .console-title {
    font-size: 18px;
  }

  .console-body {
    padding: 16px 14px 34px;
  }

  .console-user span:not(.avatar) {
    display: none;
  }
}

@media (max-width: 560px) {
  .console-top {
    align-items: flex-start;
    gap: 10px;
  }

  .console-actions {
    gap: 6px;
  }

  .icon-btn {
    display: none;
  }
}</style>
