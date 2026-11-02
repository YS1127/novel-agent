# Novel Agent

单模块 Java 21 后端与独立 Vue 前端工程。

## 版本与结构

- 后端：Spring Boot 3.5.16、MyBatis-Plus 3.5.17、Maven、Java 21。
- 前端：Vue 3、Vite 7、TypeScript 5、npm；需要 Node.js 20.19+ 或 22.12+。
- `src/main/java/com/novel`：后端入口；新增业务按领域建包。
- `frontend/src`：前端入口；业务页面和 API 模块按需创建。

目前没有业务实体、数据库迁移或 API。MySQL 是后端启动所需的预设数据源；Redis 与 Spring AI Alibaba 尚未引入。

## 启动

创建 MySQL 数据库 `novel_agent`，按需设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，然后运行：

```sh
mvn spring-boot:run
```

另开终端运行前端：

```sh
cd frontend
nvm use 20
npm ci
npm run dev
```

开发服务器会将 `/api` 请求代理到 `http://localhost:8080`；可通过 `VITE_API_TARGET` 覆盖代理目标。

## 验证

```sh
mvn test
cd frontend && npm run build
```
