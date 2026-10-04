<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()
const router = useRouter()

function handleLogout(): void {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header v-if="authStore.isAuthenticated" class="app-header">
      <span class="brand">PHT</span>
      <span class="spacer" />
      <span class="username">{{ authStore.username }}</span>
      <button @click="handleLogout">Abmelden</button>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid #333;
}
.brand {
  font-weight: 700;
}
.spacer {
  flex: 1;
}
.username {
  opacity: 0.8;
}
button {
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  border: 1px solid #444;
  background: transparent;
  color: inherit;
  cursor: pointer;
}
</style>
