import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/measurements' },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    {
      path: '/measurements',
      name: 'measurements',
      component: () => import('../views/MeasurementsView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'measurements' }
  }
  return true
})

export default router
