#!/bin/bash

echo "=== Nginx 配置测试 ==="
echo ""

# 测试 1: 基本 API
echo "测试 1: 基本 API 请求"
echo "直接访问后端: http://localhost:8080/api/hello"
curl -s http://localhost:8080/api/hello
echo ""
echo "通过 Nginx 访问: http://localhost/api/hello"
curl -s http://localhost/api/hello
echo ""
echo ""

# 测试 2: 检查响应头
echo "测试 2: 检查响应头"
curl -I http://localhost/api/hello
echo ""

# 测试 3: 文件上传（需要准备 test.jpg）
echo "测试 3: 文件上传（如果有 test.jpg）"
if [ -f "test.jpg" ]; then
    curl -X POST http://localhost/api/upload/image -F "file=@test.jpg"
else
    echo "跳过：test.jpg 不存在"
fi
echo ""

echo "=== 测试完成 ==="
