import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import LoginRegisterPage from '@/pages/LoginRegisterPage.vue'
import GlobalLayout from '@/components/layout/GlobalLayout.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage
    },
    {
      path: '/login',
      name: 'login',
      component: LoginRegisterPage
    },
    {
      path: '/',
      component: GlobalLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/pages/ChatPage.vue')
        },
        {
          path: 'documents',
          name: 'documents',
          component: () => import('@/pages/DocumentsPage.vue')
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/pages/SettingsPage.vue')
        }
      ]
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
