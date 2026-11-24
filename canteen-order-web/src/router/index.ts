import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { clearSession } from '@/utils/session'
import type { Role } from '@/types'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    roles?: Role[]
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { roles: ['STUDENT'] },
    children: [
      { path: '', redirect: '/menu' },
      {
        path: 'menu',
        name: 'student-menu',
        component: () => import('@/views/student/MenuView.vue'),
        meta: { title: '今日点餐' }
      },
      {
        path: 'cart',
        name: 'student-cart',
        component: () => import('@/views/student/CartView.vue'),
        meta: { title: '购物车' }
      },
      {
        path: 'orders',
        name: 'student-orders',
        component: () => import('@/views/student/OrdersView.vue'),
        meta: { title: '我的订单' }
      },
      {
        path: 'profile',
        name: 'student-profile',
        component: () => import('@/views/student/ProfileView.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/staff',
    component: () => import('@/layouts/ConsoleLayout.vue'),
    meta: { roles: ['STAFF', 'ADMIN'] },
    children: [
      { path: '', redirect: '/staff/board' },
      {
        path: 'board',
        name: 'staff-board',
        component: () => import('@/views/staff/BoardView.vue'),
        meta: { title: '出餐看板' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/ConsoleLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '经营概览' }
      },
      {
        path: 'dishes',
        name: 'admin-dishes',
        component: () => import('@/views/admin/DishesView.vue'),
        meta: { title: '菜品管理' }
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/CategoriesView.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'stalls',
        name: 'admin-stalls',
        component: () => import('@/views/admin/StallsView.vue'),
        meta: { title: '档口管理' }
      },
      {
        path: 'menus',
        name: 'admin-menus',
        component: () => import('@/views/admin/MenusView.vue'),
        meta: { title: '每日菜单' }
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('@/views/admin/OrdersView.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/LogsView.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'configs',
        name: 'admin-configs',
        component: () => import('@/views/admin/ConfigsView.vue'),
        meta: { title: '参数配置' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

let hydrated = false

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia)

  if (auth.isLoggedIn && !hydrated) {
    hydrated = true
    try {
      await auth.fetchMe()
    } catch {
      auth.reset()
      clearSession()
    }
  }

  if (to.path === '/login') {
    return auth.isLoggedIn ? auth.homePath : true
  }

  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const roles = to.matched.flatMap((record) => (record.meta.roles as Role[] | undefined) || [])
  if (roles.length && !roles.includes(auth.role as Role)) {
    return auth.homePath
  }

  document.title = `${to.meta.title || '西工食堂'} · ${import.meta.env.VITE_APP_TITLE || '校园订餐'}`
  return true
})

export default router
