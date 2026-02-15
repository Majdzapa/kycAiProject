FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# Show what was actually built
RUN echo "=== Build completed. Showing dist structure: ===" && \
    ls -laR dist/ || echo "No dist directory found"

# Production
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy dist directory as-is
COPY --from=builder /app/dist /tmp/dist

# Robustly handle different Angular output structures
RUN set -ex && \
    echo "=== Searching for built files ===" && \
    ls -la /tmp/dist/ && \
    if [ -d "/tmp/dist/kyc-frontend/browser" ]; then \
        echo "Using /tmp/dist/kyc-frontend/browser"; \
        cp -r /tmp/dist/kyc-frontend/browser/* /usr/share/nginx/html/; \
    elif [ -d "/tmp/dist/kyc-frontend" ] && [ -f "/tmp/dist/kyc-frontend/index.html" ]; then \
        echo "Using /tmp/dist/kyc-frontend"; \
        cp -r /tmp/dist/kyc-frontend/* /usr/share/nginx/html/; \
    elif [ -f "/tmp/dist/index.html" ]; then \
        echo "Using /tmp/dist"; \
        cp -r /tmp/dist/* /usr/share/nginx/html/; \
    else \
        echo "ERROR: Cannot find index.html in any expected location"; \
        echo "Contents of /tmp/dist:"; \
        find /tmp/dist -type f | head -20; \
        exit 1; \
    fi && \
    echo "=== Final nginx directory ===" && \
    ls -la /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]