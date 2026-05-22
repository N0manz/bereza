# syntax=docker/dockerfile:1.7
# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json ./
RUN npm install --no-audit --no-fund --prefer-offline

COPY . .
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.frontend.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=2s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ || exit 1
