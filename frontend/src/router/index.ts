import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
    requiresUser?: boolean
    guestOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', guestOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', guestOnly: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/user/HomeView.vue'),
        meta: { title: '主题摄影' },
      },
      {
        path: 'themes/:id',
        name: 'theme-detail',
        component: () => import('@/views/user/ThemeDetailView.vue'),
        meta: { title: '主题详情' },
      },
      {
        path: 'packages/:id',
        name: 'package-detail',
        component: () => import('@/views/user/PackageDetailView.vue'),
        meta: { title: '套餐详情' },
      },
      {
        path: 'booking',
        name: 'booking',
        component: () => import('@/views/user/BookingView.vue'),
        meta: { title: '预约拍摄', requiresAuth: true, requiresUser: true },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/user/OrderListView.vue'),
        meta: { title: '我的订单', requiresAuth: true, requiresUser: true },
      },
      {
        path: 'orders/:id',
        name: 'order-detail',
        component: () => import('@/views/user/OrderDetailView.vue'),
        meta: { title: '订单详情', requiresAuth: true, requiresUser: true },
      },
      {
        path: 'orders/:id/photos',
        name: 'order-photos',
        component: () => import('@/views/user/OrderPhotosView.vue'),
        meta: { title: '照片预览', requiresAuth: true, requiresUser: true },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: { name: 'admin-orders' },
    children: [
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('@/views/admin/AdminOrderListView.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'orders/:id',
        name: 'admin-order-detail',
        component: () => import('@/views/admin/AdminOrderDetailView.vue'),
        meta: { title: '订单处理' },
      },
      {
        path: 'orders/:id/upload',
        name: 'admin-order-upload',
        component: () => import('@/views/admin/PhotoUploadView.vue'),
        meta: { title: '上传精修照片' },
      },
      {
        path: 'themes',
        name: 'admin-themes',
        component: () => import('@/views/admin/ThemeManageView.vue'),
        meta: { title: '主题管理' },
      },
      {
        path: 'packages',
        name: 'admin-packages',
        component: () => import('@/views/admin/PackageManageView.vue'),
        meta: { title: '套餐管理' },
      },
      {
        path: 'profile',
        name: 'admin-profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心' },
      },
    ],
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { title: '无权访问' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (auth.token && !auth.user) {
    try {
      await auth.loadProfile()
    } catch {
      auth.logout()
    }
  }

  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'forbidden' }
  }
  if (to.meta.requiresUser && !auth.isUser) {
    return { name: 'forbidden' }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return auth.isAdmin ? { name: 'admin-orders' } : { name: 'home' }
  }

  document.title = to.meta.title ? `${to.meta.title} - 解忧摄影` : '解忧摄影'
  return true
})

export default router
