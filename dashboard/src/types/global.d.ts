export {}

declare global {
  interface Window {
    /** Runtime config injected via public/config.js (overwritten at container startup in
     * Docker, see docker-entrypoint.sh). Not present during unit tests. */
    __PHT_CONFIG__?: {
      apiBaseUrl: string
    }
  }
}
