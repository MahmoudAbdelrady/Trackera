# Build stage
FROM node:25.4.0-alpine3.22 AS build

# Install pnpm
RUN npm install -g pnpm

WORKDIR /app

# Build arguments
ARG TRACKERA_BACKEND_URL
ARG TRACKERA_MAX_FILE_SIZE

# Environment variables for Vite
ENV VITE_TRACKERA_BACKEND_URL=$TRACKERA_BACKEND_URL
ENV VITE_TRACKERA_MAX_FILE_SIZE=$TRACKERA_MAX_FILE_SIZE

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm run build

# Production stage
FROM nginx:1.29.4-alpine

# Copy build output to Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copy Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]