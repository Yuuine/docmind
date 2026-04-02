# 审计日志系统

## 使用方法

### 1. 注解式接入

在需要审计的方法上添加 `@Audited` 注解：

```java
@Audited(
    action = AuditAction.USER_LOGIN,
    resourceType = "User",
    describe = "用户登录",
    sensitiveParams = {"password"},
    async = true
)
@PostMapping("/login")
public Result<UserResponse> login(@RequestBody UserLoginRequest request) {
    return Result.success(userService.login(request));
}
```

### 2. 注解参数说明

| 参数                   | 类型          | 必填 | 说明            |
| -------------------- | ----------- | -- | ------------- |
| `action`             | AuditAction | 是  | 操作类型枚举        |
| `resourceType`       | String      | 是  | 资源类型          |
| `resourceIdParam`    | String      | 否  | 从参数提取资源ID     |
| `resourceIdFromPath` | String      | 否  | 从URL路径提取资源ID  |
| `describe`           | String      | 否  | 操作描述          |
| `logRequest`         | boolean     | 否  | 记录请求数据，默认true |
| `logResponse`        | boolean     | 否  | 记录响应数据，默认true |
| `async`              | boolean     | 否  | 异步保存，默认true   |
| `sensitiveParams`    | String[]   | 否  | 额外屏蔽的敏感字段     |
| `excludeParams`      | String[]   | 否  | 排除不记录的参数      |

### 3. 操作类型

如需新增操作类型，在 `AuditAction` 枚举中添加：

```java
public enum AuditAction {
    USER_REGISTER,
    USER_LOGIN,
    YOUR_NEW_ACTION,  // 新增
    // ...
}
```

### 4. 敏感数据保护

系统自动屏蔽以下敏感字段：

- **关键字段匹配**：`password`、`token`、`secret`、`apiKey`、`creditCard`、`cvv`、`ssn`
- **正则模式匹配**：中国手机号（+86前缀）、身份证号（15/18位）、银行卡号、IP地址、邮箱地址

### 5. 审计日志查询

```bash
# 分页查询（返回完整的分页信息）
GET /api/v1/audit/logs?userId=1&action=USER_LOGIN&startDate=2026-04-01&page=1&pageSize=20

# 单条查询
GET /api/v1/audit/logs/{id}
```

**分页响应格式**：

```json
{
  "records": [...],
  "total": 100,
  "page": 1,
  "pageSize": 20,
  "totalPages": 5
}
```

***

## 设计思路

### 架构概览

```mermaid
flowchart LR
    A[请求] --> B[TraceIdFilter]
    B --> C[Controller]
    C --> D["@Audited"]
    D --> E[AuditLogAspect<br/>117行核心逻辑]
    
    E --> F[AuditDataCollector<br/>数据收集器]
    E --> G[SensitiveDataFilter<br/>敏感数据过滤]
    E --> H[AuditLogPersistenceService<br/>持久化服务]
    
    H --> I{async参数}
    I -->|true| J[异步线程池<br/>@Async]
    I -->|false| K[同步保存]
    
    J --> L[数据库插入]
    K --> L
    
    L --> M{是否成功}
    M -->|no| N[重试3次]
    N --> M
    M -->|yes| O[audit_logs]
    
    N -->|全部失败| P[降级文件<br/>logs/audit-fallback/]
    
    E --> Q[AUDIT_FILE<br/>审计日志文件]
```

### 核心组件

| 组件                        | 职责                                       | 代码行数 |
| ------------------------- | ---------------------------------------- | ---- |
| `TraceIdFilter`           | 请求入口生成/传递TraceId，记录到MDC              | 45   |
| `AuditLogAspect`          | AOP切面，拦截@Audited方法，**流程控制与异常处理**    | 117  |
| `AuditDataCollector`       | **数据收集辅助类**：请求数据构建、资源/用户ID解析       | 170  |
| `SensitiveDataFilter`      | 敏感数据脱敏（JSON + 正则模式）                     | 138  |
| `AuditLogPersistenceService` | 审计日志持久化（异步/同步），含重试和Fallback机制       | 74   |
| `AuditLogAsyncConfig`      | 线程池配置（支持外部化配置）                         | 45   |

> **设计改进** (v2.0): 
> - `AuditLogAspect` 从279行精简至117行（-58%），专职AOP流程控制
> - 新增 `AuditDataCollector` 辅助类，实现单一职责原则（SRP）
> - `AuditLogAsyncService` 重命名为 `AuditLogPersistenceService`，语义更清晰

### 审计数据采集流程

AOP拦截后通过 `AuditDataCollector` 采集以下数据：

| 字段               | 来源                                        |
| ---------------- | ----------------------------------------- |
| `userId`         | 请求头 `X-User-Id` 或 `UserPrincipal`           |
| `action`         | `@Audited(action=...)`                      |
| `resourceType`   | `@Audited(resourceType=...)`                |
| `resourceId`     | 参数提取或URL路径解析                              |
| `httpMethod`     | `HttpServletRequest.getMethod()`            |
| `requestPath`    | `HttpServletRequest.getRequestURI()`        |
| `ipAddress`      | `HttpServletRequest.getRemoteAddr()`        |
| `requestData`    | 方法参数JSON（经SensitiveDataFilter过滤后）             |
| `responseData`   | 返回结果JSON（可配置）                             |
| `executionTime`  | 方法执行耗时                                     |
| `traceId`        | MDC获取，用于全链路追踪                               |
| `serverHost`     | 当前服务器主机名                                    |

### 异步处理

线程池配置（支持外部化配置）：

```properties
# application.yml 或 application.properties
audit:
  log:
    thread-pool:
      core-size: 4        # 核心线程数（默认: 4）
      max-size: 10        # 最大线程数（默认: 10）
      queue-capacity: 500 # 队列容量（默认: 500）
```

**线程池特性**：
- 核心线程数：4（可配置）
- 最大线程数：10（可配置）
- 队列容量：500（可配置）
- 拒绝策略：记录日志并抛出 `RejectedExecutionException`
- 优雅关闭：等待任务完成，超时60秒

### 容错机制

1. **重试机制**：数据库插入失败自动重试3次，间隔递增（100ms → 200ms → 300ms）
2. **降级机制**：重试全部失败后写入本地文件 `logs/audit-fallback/audit-fallback-YYYY-MM-DD.log`

### 日志输出

| 日志文件                       | 用途             | 保留  | 配置位置            |
| -------------------------- | -------------- | --- | --------------- |
| `logs/docmind-audit.log`     | 审计操作日志         | 30天 | AUDIT_FILE appender |
| `logs/docmind-error.log`     | 错误日志           | 30天 | ERROR_FILE appender |
| `logs/docmind.log`           | 应用日志           | 7天  | FILE appender      |
| `logs/audit-fallback/*.log`  | 数据库不可用时的降级日志 | 永久  | Fallback文件写入     |

### 数据库表结构

```sql
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(50),
    resource_type VARCHAR(100),
    resource_id BIGINT,
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    query_string VARCHAR(1000),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_data TEXT,
    response_data TEXT,
    status VARCHAR(20),
    error_message TEXT,
    execution_time BIGINT,
    operation_description VARCHAR(500),
    server_host VARCHAR(100),
    trace_id VARCHAR(50),
    created_at DATETIME
);
```

> **注意**: 表结构中不包含 `before_state` 和 `after_state` 字段（已在v2.0中移除，属于早期规划但未实现的冗余设计）

---

## API 参考

### 查询接口

#### 分页查询审计日志

**请求**:
```
GET /api/v1/audit/logs
```

**查询参数**:

| 参数          | 类型      | 必填 | 默认值 | 说明           |
| ----------- | ------- | --- | ---- | ------------ |
| `userId`    | Long    | 否   | -    | 用户ID筛选       |
| `action`    | String  | 否   | -    | 操作类型筛选       |
| `resourceType` | String | 否   | -    | 资源类型筛选       |
| `status`    | String  | 否   | -    | 状态筛选（SUCCESS/FAILURE） |
| `startDate` | String  | 否   | -    | 开始日期（YYYY-MM-DD格式） |
| `endDate`   | String  | 否   | -    | 结束日期（YYYY-MM-DD格式） |
| `page`      | Integer | 否   | 1    | 页码            |
| `pageSize`  | Integer | 否   | 20   | 每页大小          |

**响应** (`PageResponse<AuditLogResponse>`):

```json
{
  "records": [
    {
      "id": 1,
      "userId": 100,
      "action": "USER_LOGIN",
      "resourceType": "User",
      "resourceId": null,
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "requestData": "{\"method\":\"POST\",\"uri\":\"/api/login\"}",
      "responseData": null,
      "status": "SUCCESS",
      "createdAt": "2026-04-02T10:30:00"
    }
  ],
  "total": 150,
  "page": 1,
  "pageSize": 20,
  "totalPages": 8
}
```

#### 查询单条审计日志

**请求**:
```
GET /api/v1/audit/logs/{id}
```

**响应** (`AuditLogResponse`):

```json
{
  "id": 1,
  "userId": 100,
  "action": "USER_LOGIN",
  "resourceType": "User",
  "resourceId": null,
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "requestData": "{...}",
  "responseData": "{...}",
  "status": "SUCCESS",
  "createdAt": "2026-04-02T10:30:00"
}
```

---

## 架构演进记录

### v2.0 (2026-04-02) - 代码审查与架构优化

**主要变更**：

1. **冗余代码清理**
   - ✅ 删除未使用的 `ChangeTracker` 类（108行死代码）
   - ✅ 移除 `AuditLog` 模型中的 `beforeState`/`afterState` 字段（数据库表不存在）
   - ✅ 移除 `AuditService.createAuditLog()` 冗余方法（与AOP重叠）

2. **架构重构**
   - ✅ `AuditLogAspect` 从279行精简至117行（-58%）
   - ✅ 新增 `AuditDataCollector` 辅助类，实现SRP
   - ✅ `AuditLogAsyncService` → `AuditLogPersistenceService`（命名优化）

3. **功能增强**
   - ✅ 分页查询返回完整的分页元信息（`PageResponse<T>`）
   - ✅ 线程池参数支持外部化配置（`@Value`）
   - ✅ 敏感数据过滤器优化（内联冗余方法）

4. **代码质量**
   - ✅ 修复4处重复导入/FQCN问题
   - ✅ 符合SOLID设计原则（特别是SRP和ISP）

**详细文档**: `.trae/specs/audit-log-review/spec.md`

---

## 最佳实践

### 1. 正确使用 @Audited 注解

```java
// ✅ 推荐：明确指定所有关键参数
@Audited(
    action = AuditAction.DOCUMENT_UPLOAD,
    resourceType = "Document",
    resourceIdParam = "documentId",  // 从方法参数提取
    describe = "上传文档",
    sensitiveParams = {"content"},    // 额外屏蔽的敏感字段
    logRequest = true,                // 记录请求（默认true）
    logResponse = false,              // 不记录响应（大对象时建议关闭）
    async = true                      // 异步保存（推荐）
)

// ❌ 避免：缺少必要参数
@Audited(action = AuditAction.USER_LOGIN, resourceType = "User")
```

### 2. 敏感数据处理

系统会自动处理常见敏感字段，但对于业务特定的敏感数据：

```java
// 方式1：使用 sensitiveParams 声明额外敏感字段
@Audited(
    action = AuditAction.USER_UPDATE,
    resourceType = "User",
    sensitiveParams = {"idCard", "bankAccount"}  // 业务特定敏感字段
)

// 方式2：在 JSON 中避免传输敏感数据
// 控制器层应提前脱敏，而非依赖审计层的过滤
```

### 3. 异步 vs 同步选择

| 场景                  | 推荐设置 | 理由                    |
| ------------------- | ---- | --------------------- |
| 一般CRUD操作            | async=true | 不影响主流程性能            |
| 关键安全操作（如删除、权限变更） | async=false | 确保审计日志实时持久化         |
| 高并发写操作             | async=true | 避免阻塞请求线程             |
| 需要立即查询的审计日志        | async=false | 保证数据一致性               |

### 4. 性能优化建议

1. **控制 requestData 大小**
   ```java
   // 对于大对象，关闭请求记录或仅记录关键字段
   @Audited(logRequest = false)  // 或在excludeParams中排除大字段
   ```

2. **合理设置分页参数**
   - 默认 pageSize=20，最大建议不超过100
   - 避免一次查询过多数据导致内存压力

3. **监控线程池状态**
   - 关注 `audit.log.thread-pool.*` 配置项
   - 生产环境建议根据QPS调整线程池参数

---

## 故障排查

### 常见问题

**Q1: 审计日志未记录？**

检查清单：
1. 方法是否添加了 `@Audited` 注解？
2. 方法是否为 public？（AOP只能拦截public方法）
3. 是否为同类内部调用？（AOP代理失效，需通过Spring Bean调用）
4. 检查 `logs/docmind-audit.log` 是否有输出

**Q2: 敏感数据未被过滤？**

可能原因：
1. 字段名不在 DEFAULT_SENSITIVE_PARAMS 列表中
2. 未匹配到正则模式（如自定义格式的手机号）
3. 解决：在 `sensitiveParams` 中显式声明

**Q3: 异步写入失败？**

排查步骤：
1. 检查数据库连接是否正常
2. 查看 `logs/audit-fallback/` 目录是否有降级文件
3. 检查线程池是否耗尽（调整 `max-size` 和 `queue-capacity`）
4. 查看应用日志中的 "Failed to save audit log" 错误信息

**Q4: TraceId 未传递？**

验证步骤：
1. 确认 `TraceIdFilter` 在 Filter Chain 中优先级最高
2. 检查请求/响应头是否包含 `X-Trace-Id`
3. 确认 MDC 在日志 Pattern 中配置正确：`%X{traceId:-}`

---

## 扩展指南

### 自定义数据收集策略

如需扩展 `AuditDataCollector` 的数据采集能力：

```java
// 方案1：直接修改 AuditDataCollector（简单场景）
public class AuditDataCollector {
    // 添加自定义方法
    public String extractCustomData(HttpServletRequest request) {
        // 你的逻辑
    }
}

// 方案2：提取为接口（复杂场景，符合OCP）
public interface IAuditDataCollector {
    HttpServletRequest getRequest();
    Map<String, Object> buildRequestData(...);
    // ... 其他方法
}

@Component
public class CustomAuditDataCollector implements IAuditDataCollector {
    // 自定义实现
}
```

### 添加新的敏感数据模式

在 `SensitiveDataFilter` 中新增正则模式：

```java
private static final Pattern CUSTOM_PATTERN = Pattern.compile("你的正则");

private String maskSensitivePatterns(String text, boolean maskIp) {
    // ... 现有逻辑
    masked = CUSTOM_PATTERN.matcher(masked).replaceAll("***CUSTOM***");
    return masked;
}
```

### 审计事件驱动架构（未来方向）

考虑引入 Spring Event 机制：

```java
// 发布审计事件
applicationEventPublisher.publishEvent(new AuditEvent(auditLog));

// 异步监听并处理
@EventListener
@Async("auditLogExecutor")
public void handleAuditEvent(AuditEvent event) {
    auditLogPersistenceService.saveAsync(event.getAuditLog());
}
```

优势：完全解耦审计逻辑与业务代码，支持多个消费者（如实时告警、数据分析等）
