<template>
  <div class="student-shell">
    <header class="topbar">
      <div class="page-shell topbar-inner">
        <RouterLink to="/menu" class="brand-link" aria-label="返回点餐首页">
          <BrandMark />
        </RouterLink>

        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink to="/menu" class="nav-link" active-class="active">今日点餐</RouterLink>
          <RouterLink to="/cart" class="nav-link" active-class="active">购物车</RouterLink>
          <RouterLink to="/orders" class="nav-link" active-class="active">我的订单</RouterLink>
          <RouterLink to="/profile" class="nav-link" active-class="active">个人中心</RouterLink>
        </nav>

        <div class="top-actions">
          <RouterLink to="/cart" class="cart-entry" aria-label="购物车">
            <el-badge :value="cart.totalQuantity" :hidden="cart.totalQuantity === 0" :max="99">
              <el-icon :size="19"><ShoppingCart /></el-icon>
            </el-badge>
            <span>购物车</span>
          </RouterLink>

          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-chip" type="button">
              <span class="avatar">{{ initialOf(auth.displayName) }}</span>
              <span class="user-name">{{ auth.displayName }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="student-main">
      <router-view />
    </main>

    <footer class="student-footer">
      <div class="page-shell footer-inner">
        <span>西工食堂 · 校园网上订餐</span>
        <span>提前点餐，到店即取</span>
      </div>
    </footer>

    <nav class="mobile-tabs" aria-label="移动端导航">
      <RouterLink to="/menu" active-class="active">
        <el-icon><ForkSpoon /></el-icon>
        <span>点餐</span>
      </RouterLink>
      <RouterLink to="/cart" active-class="active">
        <el-badge :value="cart.totalQuantity" :hidden="cart.totalQuantity === 0" :max="99">
          <el-icon><ShoppingCart /></el-icon>
        </el-badge>
        <span>购物车</span>
      </RouterLink>
      <RouterLink to="/orders" active-class="active">
        <el-icon><Tickets /></el-icon>
        <span>订单</span>
      </RouterLink>
      <RouterLink to="/profile" active-class="active">
        <el-icon><User /></el-icon>
        <span>我的</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ForkSpoon, ShoppingCart, SwitchButton, Tickets, User } from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { initialOf } from '@/utils/format'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

onMounted(() => {
  if (auth.role === 'STUDENT') {
    cart.fetchCart().catch(() => undefined)
  }
})

async function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    await auth.logout()
    cart.reset()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.student-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  background: rgba(255, 252, 247, 0.86);
  border-bottom: 1px solid rgba(232, 224, 211, 0.9);
  backdrop-filter: blur(16px);
}

.topbar-inner {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 34px;
}

.brand-link {
  display: inline-flex;
  flex-shrink: 0;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.nav-link {
  position: relative;
  padding: 9px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 650;
  color: #5f6b63;
  transition: color 0.18s ease, background 0.18s ease;
}

.nav-link:hover {
  color: var(--ink);
  background: #f7f0e5;
}

.nav-link.active {
  color: var(--brand-dark);
  background: var(--brand-soft);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  color: #4e5a53;
  font-size: 13px;
  font-weight: 650;
  background: #fff;
  border: 1px solid var(--line);
  transition: border-color 0.18s ease, color 0.18s ease;
}

.cart-entry:hover {
  color: var(--brand);
  border-color: #efc3b2;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 5px;
  border: none;
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
  color: var(--ink);
}

.user-chip:hover {
  background: #f5eee2;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 15px;
  background: linear-gradient(145deg, #2f8f5b, #1d6a42);
}

.user-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 650;
}

.student-main {
  flex: 1;
  padding: 30px 0 70px;
}

.student-footer {
  border-top: 1px solid rgba(232, 224, 211, 0.8);
  background: rgba(255, 252, 247, 0.6);
  padding: 18px 0 24px;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #8a948d;
  font-size: 12px;
}

.mobile-tabs {
  display: none;
}

@media (max-width: 900px) {
  .topbar-inner {
    height: 64px;
    gap: 14px;
  }

  .desktop-nav,
  .cart-entry span,
  .user-name {
    display: none;
  }

  .top-actions {
    margin-left: auto;
  }

  .student-main {
    padding: 18px 0 82px;
  }

  .student-footer {
    display: none;
  }

  .mobile-tabs {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: calc(10px + env(safe-area-inset-bottom));
    z-index: 50;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    padding: 7px 6px;
    border-radius: 18px;
    background: rgba(255, 252, 247, 0.94);
    border: 1px solid var(--line);
    box-shadow: 0 12px 34px rgba(47, 34, 18, 0.16);
    backdrop-filter: blur(14px);
  }

  .mobile-tabs a {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: 7px 4px;
    border-radius: 12px;
    color: #6d776f;
    font-size: 11px;
    font-weight: 650;
  }

  .mobile-tabs a.active {
    color: var(--brand);
    background: var(--brand-soft);
  }

  .mobile-tabs :deep(.el-icon) {
    font-size: 19px;
  }
}
</style>
