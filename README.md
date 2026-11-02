# Novel Agent

单模块 Java 21 后端与独立 Vue 前端工程。

## 版本与结构

- 后端：Spring Boot 3.5.16、MyBatis-Plus 3.5.17、Maven、Java 21。
- 前端：Vue 3、Vite 7、TypeScript 5、npm；需要 Node.js 20.19+ 或 22.12+。
- `src/main/java/com/novel`：后端入口；新增业务按领域建包。
- `frontend/src`：前端入口；业务页面和 API 模块按需创建。

目前没有业务实体、数据库迁移或 API。MySQL 是后端启动所需的预设数据源；Redis 与 Spring AI Alibaba 尚未引入。

## 启动

创建 MySQL 数据库 `novel_agent`，在项目根目录的 `.env` 中填写 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，然后运行：

```sh
mvn spring-boot:run
```

`.env` 不会提交到 Git；`.env.example` 是可共享的字段模板。后端启动时从项目根目录读取 `.env` 中的 `SERVER_PORT` 和数据库配置；前端启动时读取 `FRONTEND_PORT`，并将 `/api` 代理到 `SERVER_PORT`。`DB_URL` 中的 `3306` 和 `REDIS_PORT` 也可在此修改。`REDIS_*` 和 `AI_API_KEY` 目前仅预留，尚未连接 Redis 或 AI 服务。修改 `.env` 后需重启对应服务。

另开终端运行前端：

```sh
cd frontend
nvm use 20
npm ci
npm run dev
```

开发服务器默认运行在 `5173`，并将 `/api` 请求代理到后端默认端口 `8080`。

## 验证

```sh
mvn test
cd frontend && npm run build
```
