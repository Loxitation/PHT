/** Base URL for the PHT server API, configurable at build time via .env (see .env.example). */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000'
