# Nginx 反向代理配置说明

## 一、添加前后对比

### 添加前
- 前端直接访问后端 `http://localhost:8080`
- 跨域问题需要在 Spring Boot 中配置 CORS
- 无法实现负载均衡
- 静态资源和 API 请求混在一起
- 无统一入口，难以管理

### 添加后
- 统一通过 Nginx 80 端口访问
- 前端访问 `http://localhost/api/xxx` 自动转发到后端
- Nginx 层面处理跨域，后端无需配置
- 可以轻松实现负载均衡和高可用
- 统一入口，便于监控和管理

---

## 二、为什么要添加 Nginx？

### 1. 生产环境标准架构
在生产环境中，直接暴露应用服务器端口是不安全的。Nginx 作为反向代理是行业标准做法。

### 2. 解决跨域问题
前后端分离项目中，前端（如 3000 端口）访问后端（8080 端口）会产生跨域。通过 Nginx 代理，前后端使用同一域名和端口，从根本上避免跨域。

### 3. 性能优化
- **静态资源缓存**：Nginx 处理静态文件效率远高于 Spring Boot
- **Gzip 压缩**：减少传输数据量
- **连接复用**：减少后端服务器压力

### 4. 安全性提升
- 隐藏真实后端服务器地址和端口
- 可以配置 SSL/TLS 加密
- 防止恶意请求直接攻击应用服务器

### 5. 扩展性
- 轻松实现多实例负载均衡
- 支持灰度发布和蓝绿部署
- 便于后续添加缓存层（如 Redis）

---

## 三、配置详解

### 1. upstream 配置
```nginx
upstream backend {
    server localhost:8080;
}
```
**作用**：定义后端服务器组，可以配置多个服务器实现负载均衡。

**扩展示例**：
```nginx
upstream backend {
    server localhost:8080 weight=3;
    server localhost:8081 weight=1;
    # 权重比 3:1，8080 处理更多请求
}
```

### 2. client_max_body_size 配置
```nginx
client_max_body_size 50M;
```
**作用**：限制客户端上传文件大小为 50MB，与 Spring Boot 的 `spring.servlet.multipart.max-file-size` 配置保持一致。

**为什么需要**：如果 Nginx 限制小于后端限制，大文件上传会在 Nginx 层被拒绝，返回 413 错误。

### 3. API 请求代理
```nginx
location /api/ {
    proxy_pass http://backend/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_connect_timeout 60s;
    proxy_read_timeout 60s;
}
```

**关键配置说明**：
- `proxy_pass http://backend/`：转发到后端服务器，注意末尾的 `/` 会去掉 `/api` 前缀
- `Host`：保留原始请求的 Host 头
- `X-Real-IP`：传递客户端真实 IP
- `X-Forwarded-For`：记录请求经过的代理链
- `proxy_connect_timeout`：连接后端超时时间
- `proxy_read_timeout`：读取后端响应超时时间

**请求转发示例**：
- 客户端请求：`http://localhost/api/user/login`
- Nginx 转发到：`http://localhost:8080/user/login`

### 4. WebSocket 支持
```nginx
location /ws/ {
    proxy_pass http://backend/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

**作用**：支持 WebSocket 长连接，用于实时通信功能。

**关键配置**：
- `proxy_http_version 1.1`：WebSocket 需要 HTTP/1.1
- `Upgrade` 和 `Connection`：协议升级头，从 HTTP 升级到 WebSocket

---

## 四、部署使用

### 1. 安装 Nginx（Linux）
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
```

### 2. 配置文件部署
```bash
# 复制配置文件
sudo cp backend/nginx/default.conf /etc/nginx/conf.d/lotsonote.conf

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

### 3. 启动顺序
```bash
# 1. 启动后端服务
cd backend
mvn spring-boot:run

# 2. 确认 Nginx 运行
sudo systemctl status nginx
```

### 4. 访问测试
- API 接口：`http://localhost/api/user/login`
- WebSocket：`ws://localhost/ws/chat`

---

## 五、常见问题

### 1. 502 Bad Gateway
**原因**：后端服务未启动或端口不对
**解决**：检查 Spring Boot 是否运行在 8080 端口

### 2. 413 Request Entity Too Large
**原因**：上传文件超过限制
**解决**：调整 `client_max_body_size` 值

### 3. WebSocket 连接失败
**原因**：缺少 Upgrade 头配置
**解决**：确认 `/ws/` location 配置正确

---

## 六、后续优化建议

1. **HTTPS 配置**：添加 SSL 证书，启用 HTTPS
2. **Gzip 压缩**：减少传输数据量
3. **缓存策略**：静态资源添加缓存头
4. **限流配置**：防止恶意请求
5. **日志分析**：配置访问日志和错误日志

---

## 七、前端配置修改

### 使用 Nginx 前后的区别

**之前（直接访问后端）：**
```javascript
// 前端直接访问后端 8080 端口
axios.get('http://localhost:8080/api/users')
```
- 存在跨域问题
- 需要在后端配置 CORS 白名单
- 暴露后端端口

**使用 Nginx 后：**
```javascript
// 通过 Nginx 80 端口访问
axios.get('http://localhost/api/users')

// 或使用相对路径（推荐）
axios.get('/api/users')
```
- 无跨域问题（同域同端口）
- 统一入口
- 隐藏后端实现

### 前端代码修改

**修改 API baseURL 配置：**

```javascript
// src/api/config.js 或类似文件
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NODE_ENV === 'production'
    ? 'https://yourdomain.com'  // 生产环境
    : 'http://localhost',        // 开发环境（通过 Nginx）
  timeout: 10000
});

export default api;
```

**使用示例：**
```javascript
// 之前
axios.get('http://localhost:8080/api/users')

// 之后
api.get('/api/users')  // 自动添加 baseURL
```

### 跨域问题对比

| 方案 | 架构 | 跨域 | CORS 配置 |
|------|------|------|-----------|
| **直接访问** | 前端:5173 → 后端:8080 | ✅ 存在 | 必须配置 |
| **Nginx 代理** | 前端:80 → Nginx:80 → 后端:8080 | ❌ 不存在 | 可选 |

**注意：** 使用 Nginx 后，需要在后端 CORS 配置中添加 `http://localhost` 到白名单，或使用通配符。

