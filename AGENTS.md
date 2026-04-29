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

## 沟通与工作方式
- 中文沟通，回复简洁直接
- 简单调整直接做，复杂改动先给方案再动手
- 改代码前先做基线校验，优先执行后端 `mvnw.cmd test`
- 如果基线测试本身失败，先说明失败点，再继续本次改动
- 修改超过 3 个文件时先拆成小任务，少量多次修改
- 保持现有实现风格，不借机做大规模重构
- 未经用户确认，不新增依赖、不写兼容性代码、不提交、不推送

## 现有代码风格

### 后端
- 基础包名统一使用 `com.banny.lotsonote`
- 代码按 `config`、`controller`、`service`、`aspect`、`mapper` 等常规分层组织
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

## 边界
- 默认只读取 `README.md`、源码、测试代码、`docs/` 下公开文档
- 严禁主动读取敏感文件或私密资料，包括但不限于：
- `.env`
- `.env.*`
- `application*.yml`
- `application*.yaml`
- `application*.properties`
- `bootstrap*.yml`
- `bootstrap*.yaml`
- `bootstrap*.properties`
- 各类密钥、证书、私钥文件
- `docs_private/`
- 如需排查配置问题，必须先向用户说明将读取的具体文件，并得到明确授权后再读
- 不在代码中硬编码密钥、密码、令牌、数据库连接串
- 未经确认，不修改部署、容器、代理、数据库初始化、CI/CD 等环境级配置

## 危险操作禁令
- 禁止执行破坏性命令，包括但不限于：`rm -rf`、`del /f /s /q`、`Remove-Item -Recurse -Force`
- 禁止执行高风险 Git 回滚命令，包括但不限于：`git reset --hard`、`git checkout --`
- 禁止未经确认删除数据库、上传目录、日志目录、缓存目录或批量清空文件
- 任何删除、覆盖、回滚历史、批量移动操作，必须先得到用户明确确认
