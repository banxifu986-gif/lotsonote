# 后端项目规则

## 适用范围
- 仅适用于 `backend/`
- 未单独说明的事项，遵循全局规则

## 技术栈
- Java 17
- Spring Boot
- Spring Security
- MyBatis
- MySQL
- Redis
- WebSocket

## 代码组织
- 基础包名统一使用 `com.banny.lotsonote`
- 按现有分层组织代码，优先沿用 `controller`、`service`、`service.impl`、`mapper`、`model`、`config`、`aspect`、`utils`
- 新增代码优先放入现有模块，不为单次需求新建一层抽象

## 后端实现约束
- 修改现有类时沿用同文件风格，不顺手改写整类注入方式、返回结构或异常处理模式
- 鉴权、切面、统一返回、分页、工具类等横切逻辑优先复用现有实现，不另起一套
- 数据访问优先沿用现有 MyBatis 写法，不混入新的 ORM 或查询风格
- 只有逻辑确实不明显时才补最少注释

## 验证
- 后端代码改动优先执行现有 Maven 校验，默认先用 `mvnw.cmd test`
- 如果 `test` 不可用，再根据改动范围选择最小必要校验命令
- 如果失败由环境、依赖或仓库现状导致，要明确说明失败原因
