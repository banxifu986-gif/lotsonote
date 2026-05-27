# LotsoNote

一个前后端分离的笔记系统项目。

## 技术栈

### 后端

- Java 17
- Spring Boot
- Spring Security
- MyBatis
- MySQL
- Redis
- WebSocket

### 前端

- React
- TypeScript
- Vite
- Redux Toolkit
- React Router
- Ant Design
- Tailwind CSS

## 主要功能

- 用户注册、登录
- 用户资料维护
- 头像上传
- 笔记发布、编辑、删除、查看
- 评论、点赞、收藏
- 搜索
- 站内消息
- 题目与题单管理
- 文件上传
- 统计与排行榜

## 项目结构

```text
lotso-note/
├── backend/    Spring Boot 后端
├── frontend/   React + TypeScript 前端
├── docs/       项目文档
├── sql/        数据库脚本
├── upload/     上传目录
└── README.md
```

## 运行环境

- JDK 17
- Node.js
- MySQL
- Redis

## 启动方式

### 初始化数据库

执行 `sql/lotsonote_tech_v3.sql`

### 启动后端

```bash
cd backend
mvnw.cmd spring-boot:run
```

后端基础包名已统一为 `com.banny.lotsonote`，启动类为 `LotsoNoteApplication`。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端模块标识已统一为 `lotso-note-frontend`，后端构建标识已统一为 `lotso-note-backend`。

## 开源协议

本项目基于 [kamanotes](https://github.com/youngyangyang04/kamanotes) 二次开发学习，沿用原项目 [MIT License](./LICENSE)。

Copyright (c) 2025 程序员Carl
