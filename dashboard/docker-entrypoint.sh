#!/bin/sh
set -eu

# Regenerate config.js from the runtime environment so a single pre-built image works for any
# deployment's server URL, instead of requiring a rebuild per deployment (see
# dashboard/src/api/config.ts for the client-side resolution order).
API_BASE_URL="${API_BASE_URL:-http://localhost:8000}"

cat > /usr/share/nginx/html/config.js <<EOF
window.__PHT_CONFIG__ = {
  apiBaseUrl: "${API_BASE_URL}",
};
EOF

exec nginx -g 'daemon off;'
