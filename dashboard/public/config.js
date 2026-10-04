// Default runtime config for local development (`npm run dev` / `vite preview`).
// In the Docker image, docker-entrypoint.sh overwrites this file at container startup from the
// API_BASE_URL environment variable, so the same built image works for any deployment without
// rebuilding (see Dockerfile / docker-entrypoint.sh).
window.__PHT_CONFIG__ = {
  apiBaseUrl: 'http://localhost:8000',
}
