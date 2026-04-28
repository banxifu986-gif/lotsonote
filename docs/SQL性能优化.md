# SQL 性能优化建议

## 问题概述

项目中多处使用 `DATE(created_at) = CURDATE()` 导致索引失效，造成全表扫描。

---

## 核心问题

### ❌ 当前写法

```sql
WHERE DATE(created_at) = CURDATE()
```

**问题**：
1. 对 `created_at` 列使用 `DATE()` 函数
2. MySQL 无法使用 `created_at` 索引
3. 必须全表扫描，逐行计算 `DATE(created_at)`
4. 数据量大时性能极差

---

## 优化方案

### ✅ 推荐写法

```sql
WHERE created_at >= CURDATE()
  AND created_at < CURDATE() + INTERVAL 1 DAY
```

**优势**：
- 可以使用 `created_at` 索引
- 索引范围扫描，性能高
- 不需要对每行计算函数

---

## 性能对比

假设 `note` 表有 **100 万条数据**，当天有 **1000 条**：

| 写法 | 扫描方式 | 扫描行数 | 查询时间 | 索引使用 |
|------|---------|---------|---------|---------|
| `DATE(created_at) = CURDATE()` | 全表扫描 | 100万行 | ~2-5秒 | ❌ 否 |
| `created_at >= CURDATE() AND ...` | 索引范围扫描 | ~1000行 | ~10ms | ✅ 是 |

**性能提升**：200-500 倍

---

## 需要优化的位置

### 1. NoteMapper.xml - submitNoteRank (排行榜查询)

**位置**：第 153 行

**当前代码**：
```sql
WHERE DATE(note.created_at) = CURDATE()
```

**优化后**：
```sql
WHERE note.created_at >= CURDATE()
  AND note.created_at < CURDATE() + INTERVAL 1 DAY
```

---

### 2. NoteMapper.xml - getTodayNoteCount

**位置**：第 238 行

**当前代码**：
```sql
WHERE DATE(created_at) = CURDATE()
```

**优化后**：
```sql
WHERE created_at >= CURDATE()
  AND created_at < CURDATE() + INTERVAL 1 DAY
```

---

### 3. NoteMapper.xml - getTodaySubmitNoteUserCount

**位置**：第 244 行

**当前代码**：
```sql
WHERE DATE(created_at) = CURDATE()
```

**优化后**：
```sql
WHERE created_at >= CURDATE()
  AND created_at < CURDATE() + INTERVAL 1 DAY
```

---

### 4. UserMapper.xml

**位置**：第 108 行

**当前代码**：
```sql
WHERE DATE(created_at) = CURDATE()
```

**优化后**：
```sql
WHERE created_at >= CURDATE()
  AND created_at < CURDATE() + INTERVAL 1 DAY
```

---
