# Build stage
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serve stage
FROM nginx:1.27-alpine AS serve
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 80
# API_BASE_URL is read at container startup (not build time), so this image is reusable across
# deployments; see docker-entrypoint.sh.
ENV API_BASE_URL=http://localhost:8000
ENTRYPOINT ["/docker-entrypoint.sh"]
