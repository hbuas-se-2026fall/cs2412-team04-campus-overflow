FROM node:20-alpine AS build
WORKDIR /src
# BUILD-01：package-lock.json 已入库，必须一起复制并用 npm ci，保证镜像依赖与本地/CI 完全一致
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
