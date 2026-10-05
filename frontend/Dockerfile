FROM node:22.20-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM dependencies AS builder
COPY . .
ARG NEXT_PUBLIC_API_URL=http://localhost:8082
ARG NEXT_PUBLIC_WS_URL=ws://localhost:8082/ws
ARG BACKEND_API_URL=http://backend:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_WS_URL=$NEXT_PUBLIC_WS_URL \
    BACKEND_API_URL=$BACKEND_API_URL
RUN npm run build
RUN npm prune --omit=dev

FROM node:22.20-alpine AS runner
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=3000
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/package.json ./package.json
EXPOSE 3000
CMD ["npm", "run", "start"]
