# Build stage
FROM node:alpine AS build

# Install pnpm
RUN npm install -g pnpm

WORKDIR /app

# Build arguments
ARG TRACKERA_MAX_FILE_SIZE

# Environment variables for Vite
ENV VITE_TRACKERA_MAX_FILE_SIZE=$TRACKERA_MAX_FILE_SIZE

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm run build

# Production stage
FROM nginx:alpine

# Copy build output to Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copy Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]