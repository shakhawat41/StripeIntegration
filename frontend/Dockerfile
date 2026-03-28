# =============================================================================
# Frontend Dockerfile — Copy-only (pre-built static files)
# Build locally with: npm run build
# Docker just copies the dist/ folder into nginx.
# =============================================================================

FROM nginx:alpine
# Copy the pre-built React production bundle
COPY dist /usr/share/nginx/html
# Custom nginx config that proxies /api/* to the backend container
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
