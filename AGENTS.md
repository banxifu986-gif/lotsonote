# 项目级 AGENTS 指令

## 项目概况
- 项目名称：LotsoNote
- 形态：前后端分离的笔记系统
- 主要目录：
- `backend/`：Spring Boot 后端
- `frontend/`：React + TypeScript 前端
- `docs/`：公开项目文档
- `sql/`：数据库脚本

## 技术栈
- 后端：Java 17、Spring Boot、Spring Security、MyBatis、MySQL、Redis、WebSocket
- 前端：React、TypeScript、Vite、Redux Toolkit、React Router、Ant Design、Tailwind CSS

## 项目校验入口
- 后端基线校验优先执行 `backend/mvnw.cmd test`
- 如果基线失败，要先说明失败点，再继续本次改动
- 前端当前未约定统一测试入口时，按改动范围选择最小必要校验命令

## 现有代码风格

### 后端
- 基础包名统一使用 `com.banny.lotsonote`
- 代码按 `config`、`controller`、`service`、`service.impl`、`aspect`、`mapper`、`model`、`utils` 等现有层级组织
- 保持 Java 代码现有风格：4 空格缩进，花括号与声明同行
- Spring 注解用法较直接，已有代码里存在 `@Autowired` 字段注入；修改现有类时优先沿用同文件风格
- 现有注释较少，仅在逻辑不明显时补最少说明
- 返回结构、鉴权、切面等优先复用现有工具类和既有模式，不自行另起一套

### 前端
- 以 React 函数组件和 TypeScript 为主
- 本地导入普遍显式带 `.ts` / `.tsx` 后缀
- 代码风格以不写分号为主，修改时保持现有格式
- 目录按 `apps`、`domain`、`base`、`request`、`store` 分层，新增代码优先放回对应层级
- 路由懒加载、Redux Toolkit slice、常量化路由路径已在使用，优先复用现有模式

## 项目特有边界
- 默认只读取 `README.md`、源码、测试代码、`docs/` 下公开文档
- 默认不读取 `docs_private/`
- 未经确认，不修改部署、容器、代理、数据库初始化、CI/CD 等环境级配置
