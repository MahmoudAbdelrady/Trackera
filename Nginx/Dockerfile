FROM nginx:1.31.6-alpine3.24-slim

ARG APK_CACHE_BUST

RUN apk upgrade --no-cache \
    && addgroup -S -g 1001 trackera \
    && adduser -S -u 1001 -G trackera -H -D trackera-app \
    && sed -i -e '/^user /d' -e 's|^pid .*|pid /tmp/nginx.pid;|' /etc/nginx/nginx.conf \
    && chown -R trackera-app:trackera /var/cache/nginx

COPY nginx.conf /etc/nginx/conf.d/default.conf

USER trackera-app:trackera
