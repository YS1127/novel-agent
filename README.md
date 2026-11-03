# 小说工坊

本机使用的中文小说创作助手：保存多篇作品，依次完成点子、AI 追问、故事方案与大纲、正文初稿、片段改写和导出。AI 生成的正文和改写会先显示为候选，只有手动采纳才写入作品。

## 技术与目录

- 后端：Java 21、Spring Boot 3.5.16、MyBatis-Plus 3.5.17、Maven、MySQL 8、Flyway。版本固定在 `pom.xml`。
- 前端：Vue 3、Vite 7、TypeScript 5、npm、Vitest；需要 Node.js 20.19+ 或 22.12+。
- `src/main/java/com/novel/story`：作品、构思、正文、导出。
- `src/main/java/com/novel/ai`：兼容 OpenAI 的模型请求与密钥配置。
- `src/main/resources/db/migration`：按版本执行的数据库迁移。
- `frontend/src`：创作工作台和集中 API 客户端。

Redis 暂无业务用途，当前只在 `.env` 保留连接字段；没有加入 Redis 依赖。项目不提供用户认证，服务只监听本机 `127.0.0.1`。

## 本地配置

复制 `.env.example` 为项目根目录的 `.env`，填写 MySQL 地址、账号和密码。请先创建 `DB_URL` 指向的数据库，例如 `novel_agent`；启动后 Flyway 会自动创建表。

`.env` 被 Git 忽略，权限建议为 `600`。`SERVER_PORT` 是后端端口，`FRONTEND_PORT` 是前端开发端口，Vite 会将 `/api` 代理到后端。修改端口或密钥后需重启相应服务。

在页面的“AI 设置”中填写兼容 OpenAI 接口的**基础地址**（例如 `https://api.example.com/v1`）、模型名称和 API Key。保存新 API Key 前，在 `.env` 中设置 `AI_CONFIG_ENCRYPTION_KEY`：

```sh
openssl rand -base64 32
```

将命令输出填入该字段，只生成一次并妥善保管。页面保存的密钥以 AES-GCM 加密存入 MySQL，读取设置时不会返回明文。丢失加密主密钥后，已保存的 API Key 无法解密。也可以只在 `.env` 中设置 `AI_API_KEY` 作为本机回退密钥，并在页面保存服务地址和模型名称。首版只允许 HTTPS 服务地址或本机 HTTP 地址。

## 启动

在项目根目录启动后端：

```sh
mvn spring-boot:run
```

另开终端启动前端：

```sh
cd frontend
nvm use 20
npm ci
npm run dev
```

打开 Vite 输出的本机地址。新建作品后，按页面提示生成并回答关键问题，生成并确认方案与大纲，再生成正文候选。可手动编辑正文、选中片段请求改写，最后导出 Markdown 或纯文本。正文或方案有未保存修改时，请先保存再执行 AI 操作。

## 验证

```sh
mvn test
cd frontend && npm test && npm run build
```

测试默认使用可控替身，不依赖本机 MySQL 或外部 AI 服务。首次启动会执行 `V1`～`V3` 迁移；真实模型的文字质量需要人工检查。
