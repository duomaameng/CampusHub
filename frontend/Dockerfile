FROM node:20-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_USE_MOCK=false
ARG VITE_API_BASE_URL=/api
ARG VITE_ASSET_BASE_URL=
ENV VITE_USE_MOCK=${VITE_USE_MOCK}
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_ASSET_BASE_URL=${VITE_ASSET_BASE_URL}

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
