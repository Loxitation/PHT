/** Base URL for the PHT server API.
 *
 * Resolution order:
 * 1. Runtime config from public/config.js (window.__PHT_CONFIG__) - this is what the Docker
 *    image uses, rewritten at container startup from the API_BASE_URL env var so one built
 *    image works for any deployment.
 * 2. Vite build-time env var (VITE_API_BASE_URL), used for local `npm run dev` / `npm run build`.
 * 3. Hardcoded localhost fallback.
 */
export const API_BASE_URL: string =
  window.__PHT_CONFIG__?.apiBaseUrl ?? import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000'
