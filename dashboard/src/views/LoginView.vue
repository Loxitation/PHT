<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)

const authStore = useAuthStore()
const router = useRouter()

async function handleSubmit(): Promise<void> {
  errorMessage.value = ''
  isSubmitting.value = true
  try {
    await authStore.login(username.value, password.value)
    await router.push('/measurements')
  } catch {
    errorMessage.value = 'Login fehlgeschlagen. Bitte Zugangsdaten prüfen.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="login-card" @submit.prevent="handleSubmit">
      <h1>PHT Dashboard</h1>
      <label>
        Benutzername
        <input v-model="username" type="text" autocomplete="username" required />
      </label>
      <label>
        Passwort
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? 'Anmeldung läuft…' : 'Anmelden' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.login-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 320px;
  padding: 2rem;
  border-radius: 8px;
  background: var(--card-bg, #1e1e1e);
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.9rem;
}
input {
  padding: 0.5rem;
  border-radius: 4px;
  border: 1px solid #444;
  background: #111;
  color: inherit;
}
button {
  padding: 0.6rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  background: #1b5e20;
  color: white;
  font-weight: 600;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error {
  color: #ef5350;
  font-size: 0.85rem;
  margin: 0;
}
</style>
