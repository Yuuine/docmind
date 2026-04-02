# DocMindRAG 审计日志系统 - 完整技术设计文档

## 目录

- [1. 系统概述](#1-系统概述)
- [2. 系统架构](#2-系统架构)
- [3. 文件结构与职责说明](#3-文件结构与职责说明)
- [4. 组件关联关系与数据流向](#4-组件关联关系与数据流向)
- [5. 核心模块实现细节](#5-核心模块实现细节)
  - [5.1 注解层：@Audited](#51-注解层audited)
  - [5.2 切面层：AuditLogAspect](#52-切面层auditlogaspect)
  - [5.3 数据收集器：AuditDataCollector](#53-数据收集器auditdatacollector)
  - [5.4 敏感数据过滤：SensitiveDataFilter](#54-敏感数据过滤sensitivedatafilter)
  - [5.5 持久化服务：AuditLogPersistenceService](#55-持久化服务auditlogpersistenceservice)
  - [5.6 查询服务：AuditService / AuditServiceImpl](#56-查询服务auditservice--auditserviceimpl)
  - [5.7 链路追踪：TraceIdFilter](#57-链路追踪traceidfilter)
  - [5.8 配置层：AuditLogAsyncConfig + logback-spring.xml](#58-配置层auditlogasyncconfig--logback-springxml)
- [6. 数据模型设计](#6-数据模型设计)
- [7. 日志输出策略](#7-日志输出策略)
- [8. 性能优化机制](#8-性能优化机制)
- [9. 异常处理与容错机制](#9-异常处理与容错机制)

---

## 1. 系统概述

### 1.1 设计目标

审计日志系统旨在为 DocMindRAG 应用提供**零侵入式**、**自动化**的审计日志记录能力，实现以下核心目标：

| 目标 | 说明 |
|------|------|
| **零侵入** | 业务代码只需添加 `@Audited` 注解，无需手写任何日志记录逻辑 |
| **自动化** | 自动采集 HTTP 上下文、方法参数、执行时间、异常信息等 |
| **安全性** | 内置敏感数据脱敏机制（密码、手机号、身份证等） |
| **可追溯** | 基于 TraceId 的全链路追踪能力 |
| **高可用** | 异步写入 + 重试机制 + 本地文件降级 |
| **可查询** | 提供多条件分页查询 API |

### 1.2 核心设计原则

```
┌─────────────────────────────────────────────────────────────┐
│                     设计原则                                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  SRP (单一职责)                                             │
│  ├── AuditLogAspect → 仅负责 AOP 流程控制                    │
│  ├── AuditDataCollector → 仅负责数据收集                     │
│  ├── SensitiveDataFilter → 仅负责敏感数据过滤                 │
│  └── AuditLogPersistenceService → 仅负责持久化               │
│                                                              │
│  OCP (开闭原则)                                              │
│  ├── 通过 @Audited 注解扩展审计点，无需修改切面               │
│  ├── 通过配置外部化调整线程池参数                              │
│  └── 敏感数据规则可扩展正则模式                                │
│                                                              │
│  DIP (依赖倒置)                                              │
│  ├── Aspect 依赖抽象的数据收集器                               │
│  ├── Service 依赖抽象的 Repository                           │
│  └── 配置通过 @Value 注入                                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 系统架构

### 2.1 分层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          HTTP Request                                  │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ┌──────────────────────┐                            │
│                        │   TraceIdFilter      │                            │
│                        │   (Filter Chain)     │                            │
│                        │                      │                            │
│                        │ • 生成/传递 TraceId  │                            │
│                        │ • 设置 MDC 上下文     │                            │
│                        │ • 响应头注入 TraceId │                            │
│                        └──────────┬───────────┘                            │
└───────────────────────────────────────┼──────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ┌──────────────────────┐                            │
│                        │    Controller        │                            │
│                        │   (@Audited 注解)     │                            │
│                        └──────────┬───────────┘                            │
└───────────────────────────────────────┼──────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ┌──────────────────────────────────────────────┐    │
│                        │            AuditLogAspect (117行)              │    │
│                        │         AOP 拦截器 - 流程控制中心             │    │
│                        │                                               │    │
│                        │  ┌─────────────┐  ┌────────────────────┐    │    │
│                        │  │ AuditData    │  │ SensitiveData     │    │    │
│                        │  │ Collector    │  │ Filter            │    │    │
│                        │  │ (数据收集)    │  │ (敏感数据过滤)     │    │    │
│                        │  └─────────────┘  └────────────────────┘    │    │
│                        │                                               │    │
│                        │  ┌──────────────────────────────────────┐   │    │
│                        │  │     AuditLogPersistenceService       │   │    │
│                        │  │     (异步/同步持久化)                 │   │    │
│                        │  └───────────────────┬──────────────────┘   │    │
│                        └──────────────────────┼────────────────────────┘    │
└───────────────────────────────────────────┼──────────────────────────────┘
                                            │
                         ┌──────────────────┴──────────────────┐
                         ▼                                     ▼
                ┌─────────────────┐                  ┌─────────────┐
                │ AUDIT_FILE      │                  │ Database    │
                │ (审计日志文件)    │                  │ audit_logs  │
                └─────────────────┘                  │ 表          │
                                                    └──────┬──────┘
                                                           │
                                                           ▼
                                                  ┌─────────────────┐
                                                  │ Fallback File   │
                                                  │ (降级本地文件)    │
                                                  └─────────────────┘

侧边服务：
┌─────────────────────────────────────────────────────────────────────────┐
│  AuditService / AuditServiceImpl                                       │
│  ├─ queryAuditLogs() → PageResponse<AuditLogResponse> (分页查询)       │
│  └─ getAuditLog() → AuditLogResponse (单条查询)                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 组件职责矩阵

| 层级 | 组件 | 职责 | 代码行数 |
|------|------|------|---------|
| **入口层** | TraceIdFilter | 请求链路追踪 ID 生成与传递 | 45 |
| **注解层** | @Audited | 定义审计元数据（操作类型、资源类型等） | 19 |
| **切面层** | AuditLogAspect | AOP 拦截器，流程控制与异常处理 | 117 |
| **辅助层** | AuditDataCollector | 数据收集（HTTP上下文、参数解析、用户ID提取） | 170 |
| **过滤层** | SensitiveDataFilter | 敏感数据脱敏（JSON + 正则模式） | 138 |
| **持久化层** | AuditLogPersistenceService | 审计日志数据库写入（异步/同步） | 74 |
| **查询层** | AuditServiceImpl | 审计日志分页查询与单条查询 | 100 |
| **配置层** | AuditLogAsyncConfig | 异步线程池配置（支持外部化） | 45 |
| **配置层** | logback-spring.xml | Logback 日志框架配置 | 72 |
| **模型层** | AuditLog | 审计日志实体类（MyBatis-Plus） | 79 |
| **DTO层** | PageResponse / QueryRequest / Response 等 | 数据传输对象 | ~150 |

---

## 3. 文件结构与职责说明

### 3.1 完整文件清单

```
src/main/java/yuuine/docmind/
├── common/
│   └── filter/
│       └── TraceIdFilter.java                          # 链路追踪过滤器
│
├── controller/
│   └── AuditController.java                             # 审计日志查询接口
│
└── core/
    └── audit/
        ├── annotation/
        │   └── Audited.java                              # 审计注解定义
        │
        ├── aspect/
        │   ├── AuditLogAspect.java                        # AOP 切面（核心拦截器）
        │   └── AuditDataCollector.java                   # 数据收集辅助类
        │
        ├── config/
        │   └── AuditLogAsyncConfig.java                   # 异步线程池配置
        │
        ├── dto/
        │   ├── AuditLogQueryRequest.java                 # 查询请求 DTO
        │   ├── AuditLogResponse.java                     # 审计日志响应 DTO
        │   └── PageResponse.java                          # 通用分页响应 DTO
        │
        ├── filter/
        │   └── SensitiveDataFilter.java                  # 敏感数据过滤器
        │
        ├── model/
        │   └── AuditLog.java                              # 审计日志实体类
        │
        ├── repository/
        │   └── AuditLogRepository.java                    # MyBatis-Plus Mapper 接口
        │
        ├── service/
        │   ├── AuditService.java                          # 查询服务接口
        │   ├── AuditLogPersistenceService.java            # 持久化服务（异步/同步）
        │   └── impl/
        │       └── AuditServiceImpl.java                 # 查询服务实现
        │
        └── valueobject/
            ├── AuditAction.java                           # 操作类型枚举
            └── AuditStatus.java                           # 审计状态枚举

src/main/resources/
    └── logback-spring.xml                                # Logback 日志配置
```

### 3.2 各文件详细说明

#### **annotation/Audited.java**

```java
@Target(ElementType.METHOD)           // 只能标注在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，AOP 可读取
@Documented
public @interface Audited {
    AuditAction action();                    // 操作类型（必填）
    String resourceType();                  // 资源类型（必填）
    String resourceIdParam() default "";    // 从哪个方法参数提取资源ID
    String resourceIdFromPath() default "";// 从 URL 路径变量提取资源ID
    String describe() default "";           // 操作描述
    boolean logRequest() default true;      // 是否记录请求数据
    boolean logResponse() default true;     // 是否记录响应数据
    boolean async() default true;           // 是否异步保存
    String[] excludeParams() default {};    // 排除不记录的参数名
    String[] sensitiveParams() default {};// 额外敏感字段
}
```

**作用**：作为审计日志的"开关"和"元数据声明"，开发者通过此注解告诉系统"这个方法需要被审计"以及"如何审计"。

---

#### **aspect/AuditLogAspect.java**

```java
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogPersistenceService auditLogPersistenceService;
    private final SensitiveDataFilter sensitiveDataFilter;
    private final ObjectMapper objectMapper;
    private final AuditDataCollector dataCollector;

    @Around("@annotation(yuuine.docmind.core.audit.annotation.Audited)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 使用 dataCollector 收集上下文
        var request = dataCollector.getRequest();
        Map<String, Object> requestData = dataCollector.buildRequestData(...);

        // 2. 构建 AuditLog 对象
        AuditLog auditLog = buildAuditLog(...);

        // 3. 执行目标方法
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 4. 记录失败状态和错误信息
        } finally {
            // 5. 输出审计日志到文件
            logAudit(auditLog);

            // 6. 异步或同步持久化到数据库
            if (async) {
                auditLogPersistenceService.saveAsync(auditLog);
            } else {
                auditLogPersistenceService.saveSync(auditLog);
            }
        }
    }
}
```

**核心职责**：
- 拦截所有带 `@Audited` 注解的方法
- 协调 `AuditDataCollector` 收集数据
- 协调 `SensitiveDataFilter` 过滤敏感信息
- 统一处理成功/失败状态记录
- 委托 `AuditLogPersistenceService` 进行持久化

---

#### **aspect/AuditDataCollector.java**

```java
@Component
public class AuditDataCollector {

    public HttpServletRequest getRequest() { ... }
    
    public Map<String, Object> buildRequestData(
        ProceedingJoinPoint joinPoint, 
        HttpServletRequest request, 
        Audited annotation) { ... }
        
    public Object resolveResourceId(ProceedingJoinPoint joinPoint, Audited annotation) { ... }
    
    public Long resolveUserId(HttpServletRequest request) { ... }
    
    public static Long toLong(Object value) { ... }
    
    public String getServerHost() { ... }
    
    public String getTraceId() { ... }
}
```

**核心职责**：
- 从 HTTP 请求中提取标准化的审计数据
- 解析方法参数中的资源 ID
- 从请求头或 Principal 中提取用户 ID
- 获取服务器主机名和链路追踪 ID

---

#### **filter/SensitiveDataFilter.java**

```java
@Component
@RequiredArgsConstructor
public class SensitiveDataFilter {

    private final ObjectMapper objectMapper;

    // 默认敏感字段集合
    private static final Set<String> DEFAULT_SENSITIVE_PARAMS = Set.of(
        "password", "passwd", "secret", "token", "apiKey", ...
    );

    // 预编译的正则表达式模式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("...");
    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("\\b(?:\\+86)?1[3-9]\\d{9}\\b");
    private static final Pattern IP_V4_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    // ... 更多模式

    public String filterSensitiveData(String jsonString, String[] additionalParams) { ... }
}
```

**核心职责**：
- **关键字段匹配**：对 JSON 中 key 匹配 DEFAULT_SENSITIVE_PARAMS 的值进行替换
- **正则模式匹配**：对字符串值进行邮箱、手机号、身份证、IP 等模式的检测和替换
- **递归处理**：支持嵌套 Map 和 List 结构的深度遍历

**脱敏算法流程**：

```
输入: {"password":"123456","email":"test@example.com","phone":"13800138000"}
       │
       ▼
  1️⃣ JSON → Map 解析
       │
       ▼
  2️⃣ 遍历 Key: "password" ∈ DEFAULT_SENSITIVE_PARAMS?
       ├── 是 → 替换值为 "***SENSITIVE***"
       └── 否 → 检查 Value 是否匹配正则
              ├── email 匹配 → "***EMAIL***"
              ├── phone 匹配 → "***CN_MOBILE***"
              └── 其他 → 保持原值
       │
       ▼
  3️⃣ Map → JSON 序列化输出
       
输出: {"password":"***SENSITIVE***","email":"***EMAIL***","phone":"***CN_MOBILE***"}
```

---

#### **service/AuditLogPersistenceService.java**

```java
@Service
@RequiredArgsConstructor
public class AuditLogPersistenceService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;
    private static final String FALLBACK_DIR = "logs/audit-fallback";

    @Async("auditLogExecutor")
    public void saveAsync(AuditLog auditLog) {
        saveWithRetries(auditLog);  // 提交到异步线程池
    }

    public void saveSync(AuditLog auditLog) {
        saveWithRetries(auditLog);  // 同步执行
    }

    private void saveWithRetries(AuditLog auditLog) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                auditLogRepository.insert(auditLog);
                return;  // 成功则返回
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);  // 递增延迟重试
                }
            }
        }
        writeToFallbackFile(auditLog);  // 全部失败后降级写入文件
    }

    private synchronized void writeToFallbackFile(AuditLog auditLog) {
        // 写入 logs/audit-fallback/audit-fallback-YYYY-MM-DD.log
    }
}
```

**核心职责**：
- 提供异步 (`saveAsync`) 和同步 (`saveSync`) 两种持久化方式
- 内置 **3 次重试机制**（间隔递增：100ms → 200ms → 300ms）
- **降级保障**：数据库不可用时写入本地文件，确保数据不丢失

**重试与降级流程**：

```
saveWithRetries()
    │
    ├── 第1次尝试 → 成功? ✅ 返回
    │              失败? → sleep(100ms)
    │
    ├── 第2次尝试 → 成功? ✅ 返回
    │              失败? → sleep(200ms)
    │
    ├── 第3次尝试 → 成功? ✅ 返回
    │              失败? → 写入 Fallback 文件
    │
    └── 最终: 数据永不丢失 ✅
```

---

#### **service/AuditService.java & impl/AuditServiceImpl.java**

```java
// 接口
public interface AuditService {
    PageResponse<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest request);
    AuditLogResponse getAuditLog(Long auditLogId);
}

// 实现
@Service
public class AuditServiceImpl implements AuditService {

    @Override
    public PageResponse<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest request) {
        LambdaQueryWrapper<AuditLog> queryWrapper = buildQueryWrapper(request);
        Page<AuditLog> page = new Page<>(page, pageSize);
        Page<AuditLog> result = auditLogRepository.selectPage(page, queryWrapper);
        
        return PageResponse.of(
            result.getRecords().stream().map(this::toAuditLogResponse).toList(),
            result.getTotal(), page, pageSize
        );
    }
}
```

**核心职责**：
- 提供**只读查询**能力（写入统一由 AOP 切面处理）
- 支持多条件筛选（userId、action、resourceType、status、日期范围）
- 返回完整的分页元信息（total、totalPages）

---

#### **config/AuditLogAsyncConfig.java**

```java
@Configuration
@EnableAsync
public class AuditLogAsyncConfig {

    @Value("${audit.log.thread-pool.core-size:4}")
    private int corePoolSize;

    @Value("${audit.log.thread-pool.max-size:10}")
    private int maxPoolSize;

    @Value("${audit.log.thread-pool.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.error("任务被拒绝...");
            throw new RejectedExecutionException();
        });
        return executor;
    }
}
```

**核心职责**：
- 创建专用的异步线程池，隔离审计日志写入对业务线程的影响
- 支持通过配置文件动态调整线程池参数

---

#### **common/filter/TraceIdFilter.java**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 最高优先级，最先执行
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // 1. 尝试从请求头获取 TraceId
        String traceId = httpRequest.getHeader("X-Trace-Id");
        
        // 2. 如果没有，生成新的 UUID（去除连字符）
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // 3. 放入 MDC（Logback 日志上下文）
        MDC.put("traceId", traceId);

        // 4. 响应头回传，方便前端排查
        httpResponse.setHeader("X-Trace-Id", traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");  // 清理 ThreadLocal
        }
    }
}
```

**核心职责**：
- 为每个 HTTP 请求分配唯一的 **TraceId**
- 将 TraceId 存入 **MDC**，使所有自动日志都携带该标识
- 在响应头中回传 TraceId，方便前后端联调排查

---

## 4. 组件关联关系与数据流向

### 4.1 完整调用链路图

```
HTTP Request
    │
    │ Headers:
    │   X-Trace-Id: [可选]
    │   X-User-Id: [认证后]
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ ① TraceIdFilter.doFilter()                                   │
│     │                                                          │
│     ├── 生成/获取 TraceId                                      │
│     ├── MDC.put("traceId", ...)                                │
│     └── response.setHeader("X-Trace-Id", ...)                  │
│                                                                 │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│ ② Controller (@Audited 方法)                                  │
│     │                                                          │
│     └── 业务方法执行                                           │
│                                                                 │
└──────────────────────────────┬──────────────────────────────────┘
                               │ AOP 拦截
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│ ③ AuditLogAspect.around() [核心调度]                           │
│     │                                                          │
│     ├── ③a. dataCollector.getRequest()                          │
│     │       └── 从 RequestContextHolder 获取当前请求              │
│     │                                                          │
│     ├── ③b. dataCollector.buildRequestData(joinPoint, req, anno)│
│     │       ├── 提取 method, uri, queryString                  │
│     │       ├── 反射获取方法参数（排除 excludeParams）            │
│     │       └── 区分简单类型 vs 复杂对象                         │
│     │                                                          │
│     ├── ③c. dataCollector.resolveResourceId(...)                 │
│     │       ├── 优先从参数提取 (resourceIdParam)                 │
│     │       └── 其次从路径变量提取 (resourceIdFromPath)          │
│     │                                                          │
│     ├── ③d. dataCollector.resolveUserId(request)                │
│     │       ├── 优先从 X-User-Id header 获取                   │
│     │       └── 其次从 UserPrincipal.getName() 获取            │
│     │                                                          │
│     ├── ③e. 构建 AuditLog 对象 (Builder 模式)                   │
│     │       ├── action, resourceType, httpMethod              │
│     │       ├── requestPath, ipAddress, userAgent             │
│     │       ├── serverHost, traceId, createdAt                │
│     │       └── status = SUCCESS (初始状态)                    │
│     │                                                          │
│     ├── ③f. toJson(requestData, sensitiveParams)               │
│     │       └── objectMapper.writeValueAsString()               │
│     │           └── sensitiveDataFilter.filterSensitiveData()  │
│     │               ├── JSON → Map 解析                        │
│     │               ├── Key 匹配 DEFAULT_SENSITIVE_PARAMS       │
│     │               ├── Value 正则模式匹配                       │
│     │               └── Map → JSON 序列化                       │
│     │                                                          │
│     ├── ③g. joinPoint.proceed() 执行目标方法                    │
│     │       ├── 成功 → responseData = toJson(result)           │
│     │       └── 失败 → status=FAILURE, errorMessage=e.getMessage()│
│     │                                                          │
│     └── ③h. finally 块                                         │
│           ├── executionTime = System.currentTimeMillis() - startTime│
│           ├── logAudit(auditLog) → AUDIT_FILE appender          │
│           └── persistenceService.saveAsync/Sync(auditLog)       │
│                                                                 │
└──────────────────────────────┬──────────────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ AUDIT_FILE     │ │ PersistenceSvc  │ │ MDC Context    │
│ (logback logger)│ │ (async thread)  │ │ (所有日志共享)  │
│                 │ │                 │ │                 │
│ [AUDIT] USER.. │ │ insert() → DB   │ │ [%X{traceId}]  │
│ - UserLogin    │ │ retry x3        │ │ 所有 log.info  │
│ - 5ms          │ │ fallback file   │ │ 都会带上 traceId│
└─────────────────┘ └────────┬────────┘ └─────────────────┘
                             │
                             ▼
                   ┌─────────────────┐
                   │  Database        │
                   │  audit_logs 表   │
                   └─────────────────┘
```

### 4.2 依赖注入关系图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Spring IoC Container                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐                                                │
│  │ AuditLogAspect  │ ← @Autowired                                  │
│  │   ├─ depends → │                                                 │
│  │   ├─ AuditLogPersistenceService  (持久化服务)                   │
│  │   ├─ SensitiveDataFilter         (敏感数据过滤器)               │
│  │   ├─ ObjectMapper                (JSON序列化)                  │
│  │   └─ AuditDataCollector           (数据收集器)                   │
│  └─────────────────┘                                                │
│                                                                      │
│  ┌─────────────────┐                                                │
│  │ AuditDataColl.  │ ← @Component (Spring Bean)                   │
│  │   └─ uses →    │ 无依赖注入（纯工具类）                              │
│  │   (无外部依赖)  │                                                 │
│  └─────────────────┘                                                │
│                                                                      │
│  ┌─────────────────┐                                                │
│  │ SensitivFilter  │ ← @Autowired                                  │
│  │   └─ depends → │                                                 │
│  │   └─ ObjectMapper                                                │
│  └─────────────────┘                                                │
│                                                                      │
│  ┌─────────────────────────┐                                        │
│  │ AuditLogPersistenceSvc│ ← @Autowired                            │
│  │   └─ depends →          │                                        │
│  │   └─ AuditLogRepository  (MyBatis-Plus Mapper)                │
│  └─────────────────────────┘                                        │
│                                                                      │
│  ┌─────────────────────────┐                                        │
│  │ AuditServiceImpl      │ ← @Autowired                            │
│  │   └─ depends →          │                                        │
│  │   └─ AuditLogRepository                                          │
│  └─────────────────────────┘                                        │
│                                                                      │
│  ┌─────────────────────────┐                                        │
│  │ TraceIdFilter         │ ← @Component (Servlet Filter)          │
│  │   └─ uses →             │                                        │
│  │   └─ MDC (SLF4J)       (ThreadLocal 上下文)                      │
│  └─────────────────────────┘                                        │
│                                                                      │
│  ┌─────────────────────────┐                                        │
│  │ AuditLogAsyncConfig    │ ← @Configuration                      │
│  │   └─ produces →         │                                        │
│  │   └─ ThreadPoolTaskExecutor ("auditLogExecutor")              │
│  └─────────────────────────┘                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. 核心模块实现细节

### 5.1 注解层：@Audited

**设计意图**：使用声明式编程，将审计关注点从业务逻辑中解耦。

**运行时行为**：
- Spring AOP 在启动时扫描所有带 `@Audited` 注解的方法
- 动态代理在方法执行前后插入审计逻辑
- 注解属性作为元数据指导审计行为

**关键参数决策树**：

```
@Audited(async=true)  ──→ 异步提交到线程池（默认，推荐）
@Audited(async=false) ──→ 同步阻塞等待写入完成（关键操作用）

@Audited(logRequest=true)  ──→ 记录方法参数（默认）
@Audited(logRequest=false) ──→ 不记录参数（大对象/隐私场景）

@Audited(sensitiveParams={"customField"}) ──→ 额外标记自定义字段为敏感

@Audited(excludeParams={"fileContent"}) ──→ 排除大字段不记录
```

---

### 5.2 切面层：AuditLogAspect

**核心算法**：环绕通知（Around Advice）+ try-catch-finally 模式

```java
@Around("@annotation(Audited)")
public Object around(ProceedingJoinPoint pjp) {
    // Phase 1: 前置处理（数据收集）
    Object targetResult;
    try {
        // Phase 2: 执行目标方法
        targetResult = pjp.proceed();
        // Phase 3a: 后置处理（成功路径）
        return targetResult;
    } catch (Throwable e) {
        // Phase 3b: 异常处理（失败路径）
        throw e;  // 重新抛出，不影响原异常传播
    } finally {
        // Phase 4: 必定执行的清理和持久化
        persistAndLog();
    }
}
```

**设计亮点**：
- **finally 块保证**：无论成功还是失败，审计日志都会记录
- **异常透明**：catch 后重新抛出，不影响原有异常处理链
- **性能优化**：数据收集在前置阶段完成，finally 只做轻量操作

---

### 5.3 数据收集器：AuditDataCollector

**resolveResourceId 算法**：

```java
public Object resolveResourceId(JoinPoint jp, Audited anno) {
    // 策略1: 从方法参数中按名称查找
    String paramName = anno.resourceIdParam();
    if (!paramName.isBlank()) {
        return resolveFromParameter(jp, paramName);  // 遍历参数列表匹配名称
    }
    
    // 策略2: 从URL路径变量中查找
    String pathVar = anno.resourceIdFromPath();
    if (!pathVar.isBlank()) {
        return resolveFromPath(pathVar);  // 从 HandlerMapping.URI_TEMPLATE_VARIABLES 获取
    }
    
    return null;  // 两种都没配置则返回null
}
```

**resolveUserId 算法**：

```java
public Long resolveUserId(HttpServletRequest request) {
    // 优先级1: 自定义请求头（微服务网关场景）
    String userIdHeader = request.getHeader("X-User-Id");
    if (userIdHeader != null) return Long.parseLong(userIdHeader);
    
    // 优先级2: Spring Security Principal（单体应用场景）
    Principal principal = request.getUserPrincipal();
    if (principal != null) return Long.parseLong(principal.getName());
    
    return null;  // 未登录返回null
}
```

**toLong 类型转换**（兼容多种输入）：

```java
public static Long toLong(Object value) {
    return switch (value) {
        case null -> null;
        case Long l -> l;
        case Number n -> n.longValue();
        case String s -> Long.parseLong(s);  // 可能抛NumberFormatException
        default -> null;
    };
}
```

---

### 5.4 敏感数据过滤：SensitiveDataFilter

**双层过滤机制**：

```
Layer 1: Key-Level Filtering（字段名匹配）
┌─────────────────────────────────────────────┐
│ Input Map:                                  │
│   {"password": "123456", "username": "admin"} │
│                                              │
│ 检查每个 Key 是否 ∈ DEFAULT_SENSITIVE_PARAMS   │
│   "password" ✓ → 替换为 "***SENSITIVE***"     │
│   "username" ✗ → 保留原值                      │
└─────────────────────────────────────────────┘
         │
         ▼
Layer 2: Value-Level Regex Matching（正则模式匹配）
┌─────────────────────────────────────────────┐
│ Input String Values:                        │
│   "test@example.com"                        │
│   "13800138000"                             │
│   "hello world"                             │
│                                              │
│ 逐一应用预编译正则:                           │
│   EMAIL_PATTERN ✓ → "***EMAIL***"           │
│   CN_MOBILE_PATTERN ✓ → "***CN_MOBILE***"    │
│   (无匹配) → 保留原值 "hello world"          │
└─────────────────────────────────────────────┘
```

**递归深度优先遍历算法**：

```java
private void filterMap(Map<String, Object> map, ...) {
    for (Entry<String, Object> entry : map.entrySet()) {
        Object value = entry.getValue();
        
        if (value instanceof String str) {
            entry.setValue(maskSensitivePatterns(str));  // 叶子节点：直接处理
        } else if (value instanceof Map mapVal) {
            filterMap(mapVal, ...);  // 递归进入子Map
        } else if (value instanceof List listVal) {
            filterList(listVal, ...);  // 递归进入List
        }
    }
}
```

**支持的敏感数据类型**：

| 类别 | 正则模式 | 示例 | 替换值 |
|------|---------|------|--------|
| 密码类 | Key 匹配 | password, token, secret | ***SENSITIVE*** |
| 邮箱 | `\S+@\S+\.\S+` | test@example.com | ***EMAIL*** |
| 中国手机号 | `\b(?:\+86)?1[3-9]\d{9}\b` | 13800138000 | ***CN_MOBILE*** |
| 身份证15位 | `\b\d{15}\b` | 110101199001010 | ***ID_CARD_15*** |
| 身份证18位 | `\b\d{17}[\dXx]\b` | 11010119900101001X | ***ID_CARD_18*** |
| 银行卡号 | `\b\d{4}[- ]?\d{4}[- ]?\d{4}[- ]?\d{4}\b` | 6222021234567890 | ***CREDIT_CARD*** |
| IPv4地址 | `\b(?:\d{1,3}\.){3}\d{1,3}\b` | 192.168.1.1 | ***IP*** |

---

### 5.5 持久化服务：AuditLogPersistenceService

**异步执行原理**：

```java
@Async("auditLogExecutor")
public void saveAsync(AuditLog auditLog) {
    saveWithRetries(auditLog);
    // 方法返回后，实际执行由线程池中的线程完成
    // 主线程立即继续，不阻塞响应
}
```

**重试策略详解**：

```java
private void saveWithRetries(AuditLog auditLog) {
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            auditLogRepository.insert(auditLog);
            return;  // 成功退出循环
        } catch (Exception e) {
            log.warn("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
            
            if (attempt < MAX_RETRIES) {
                // 指数退避：每次等待时间递增
                Thread.sleep(RETRY_DELAY_MS * attempt);
                // 第1次: 100ms, 第2次: 200ms, 第3次: 不再sleep（最后一次）
            }
        }
    }
    // 全部重试失败 → 降级
    writeToFallbackFile(auditLog);
}
```

**Fallback 文件格式**：

```
logs/audit-fallback/audit-fallback-2026-04-02.log

2026-04-02T10:30:00.123 | AuditLog{id=null, userId=1001, action='USER_LOGIN', ...}
2026-04-02T10:30:00.456 | AuditLog{id=null, userId=1002, action='DOCUMENT_UPLOAD', ...}
```

---

### 5.6 查询服务：AuditService / AuditServiceImpl

**分页查询构建算法**：

```java
private LambdaQueryWrapper<AuditLog> buildQueryWrapper(AuditLogQueryRequest req) {
    LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
    
    // 动态条件组装（只添加非空条件）
    Optional.ofNullable(req.getUserId())
        .ifPresent(id -> wrapper.eq(AuditLog::getUserId, id));
    
    Optional.ofNullable(req.getAction())
        .ifPresent(action -> wrapper.eq(AuditLog::getAction, action.name()));
    
    // 日期范围处理
    Optional.ofNullable(req.getStartDate())
        .ifPresent(start -> wrapper.ge(
            AuditLog::getCreatedAt, 
            LocalDateTime.parse(start + " 00:00:00", DATE_TIME_FORMATTER)
        ));
    
    // 排序
    wrapper.orderByDesc(AuditLog::getCreatedAt);
    
    return wrapper;
}
```

**PageResponse 工厂方法**：

```java
public class PageResponse<T> {
    public static <T> PageResponse<T> of(List<T> records, long total, int page, int pageSize) {
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();
    }
}
```

---

### 5.7 链路追踪：TraceIdFilter

**TraceId 生命周期**：

```
Request In
    │
    ▼
[检查 Header] ── X-Trace-Id 存在?
    │
    ├── YES → 直接使用（保持链路连续性）
    │
    └── NO  → 生成新 UUID (32位无连字符)
                │
                ▼
        [MDC.put("traceId", value)]
                │  ← ThreadLocal 绑定当前线程
                ▼
        [response.setHeader("X-Trace-Id", value)]
                │  ← 回传给调用方
                ▼
        chain.doFilter(request, response)
                │  ← 执行后续 Filter + Controller
                ▼
        [MDC.remove("traceId")]
                │  ← finally 清理，防止内存泄漏
                ▼
Request Out
```

**UUID 生成细节**：

```java
private String generateTraceId() {
    // 标准 UUID: 550e8400-e29b-41d4-a716-446655440000 (36字符)
    // 去除连字符:  550e8400e29b41d4a716446655440000 (32字符)
    return UUID.randomUUID().toString().replace("-", "");
}
```

**为什么选择 UUID？**
- 全局唯一性保证（碰撞概率极低）
- 无需中心化协调（分布式友好）
- 32位紧凑格式适合日志展示

---

### 5.8 配置层：AuditLogAsyncConfig + logback-spring.xml

**线程池参数调优指南**：

| 参数 | 默认值 | 低流量 | 中流量 | 高流量 |
|------|-------|--------|--------|--------|
| core-size | 4 | 2 | 4 | 8 |
| max-size | 10 | 5 | 10 | 20 |
| queue-capacity | 500 | 200 | 500 | 1000 |
| keep-alive | 60s | 30s | 60s | 60s |

**Logback 日志 Pattern 解析**：

```
%d{yyyy-MM-dd HH:mm:ss.SSS}  → 时间戳精确到毫秒
[%thread]                    → 线程名称
[%X{traceId:-}]             → MDC 中的 traceId（不存在显示 "-"）
%highlight(%-5level)         → 日志级别（彩色高亮）
%cyan(%logger{36})           → Logger 名称（青色，截断36字符）
%-msg%n                      → 日志消息内容
```

**Logger 层级配置**：

```xml
<root level="INFO">                          <!-- 全局根级别 -->
    <appender-ref ref="CONSOLE"/>              <!-- 控制台 -->
    <appender-ref ref="FILE"/>                  <!-- 应用日志文件 -->
    <appender-ref ref="ERROR_FILE"/>           <!-- 错误日志文件 -->
</root>

<logger name="yuuine.docmind" level="DEBUG"/>   <!-- 项目代码 DEBUG -->

<logger name="yuuine.docmind.core.audit" level="INFO">
    <appender-ref ref="AUDIT_FILE"/>            <!-- 审计日志独立文件 -->
</logger>
```

---

## 6. 数据模型设计

### 6.1 AuditLog 实体类

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_logs")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;                    // 主键
    
    @TableField("user_id")
    private Long userId;                 // 用户ID
    
    @TableField("action")
    private String action;               // 操作类型 (USER_LOGIN, DOCUMENT_UPLOAD...)
    
    @TableField("resource_type")
    private String resourceType;          // 资源类型 (User, Document, ChatSession...)
    
    @TableField("resource_id")
    private Long resourceId;             // 资源ID
    
    @TableField("http_method")
    private String httpMethod;            // HTTP方法 (GET, POST, PUT, DELETE)
    
    @TableField("request_path")
    private String requestPath;            // 请求路径 (/api/v1/users/login)
    
    @TableField("query_string")
    private String queryString;           // 查询参数 (?page=1&size=20)
    
    @TableField("ip_address")
    private String ipAddress;             // 客户端IP
    
    @TableField("user_agent")
    private String userAgent;             // 浏览器/客户端标识
    
    @TableField("request_data")
    private String requestData;           // 请求数据 (JSON, 已脱敏)
    
    @TableField("response_data")
    private String responseData;          // 响应数据 (JSON, 已脱敏)
    
    @TableField("status")
    private String status;                // 状态 (SUCCESS / FAILURE)
    
    @TableField("error_message")
    private String errorMessage;          // 错误信息 (仅失败时有值)
    
    @TableField("execution_time")
    private Long executionTime;           // 执行耗时 (毫秒)
    
    @TableField("operation_description")
    private String operationDescription;  // 操作描述 (@Audited.describe())
    
    @TableField("server_host")
    private String serverHost;             // 服务器主机名
    
    @TableField("trace_id")
    private String traceId;                // 链路追踪ID
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;        // 创建时间 (MyBatis-Plus 自动填充)
}
```

### 6.2 数据库表结构

```sql
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- 用户与操作信息
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id BIGINT,
    
    -- HTTP 请求信息
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    query_string VARCHAR(1000),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    
    -- 业务数据 (JSON 格式，已脱敏)
    request_data TEXT,
    response_data TEXT,
    
    -- 执行结果
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    execution_time BIGINT,
    operation_description VARCHAR(500),
    
    -- 追踪信息
    server_host VARCHAR(100),
    trace_id VARCHAR(50),
    
    -- 时间戳
    created_at DATETIME NOT NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at),
    INDEX idx_trace_id (trace_id)
);
```

**索引设计**：
- `idx_user_id`: 按用户查询其操作历史
- `idx_action`: 按操作类型统计
- `idx_created_at`: 按时间范围查询（最常用）
- `idx_trace_id`: 按 TraceId 追踪单次请求全链路

---

## 7. 日志输出策略

### 7.1 四通道日志分离

| Appender | 文件 | 用途 | 保留周期 | 级别过滤 |
|----------|------|------|---------|---------|
| **CONSOLE** | stdout | 开发调试控制台输出 | 即时 | INFO |
| **FILE** | docmind.log | 全量应用日志 | 7天 | INFO |
| **ERROR_FILE** | docmind-error.log | 错误日志 | 30天 | ERROR only |
| **AUDIT_FILE** | docmind-audit.log | 审计操作日志 | 30天 | INFO |

### 7.2 审计日志格式

```
[AUDIT] {action} - {resourceType} - {executionTime}ms - {status} - {requestPath} - {ipAddress}

示例:
[AUDIT] USER_LOGIN - User - 5ms - SUCCESS - /api/login - 192.168.1.100
[AUDIT] DOCUMENT_UPLOAD - Document - 150ms - SUCCESS - /api/documents - 10.0.0.5
[AUDIT] USER_DELETE - User - 12ms - FAILURE - /api/users/100 - 192.168.1.100
```

### 7.3 日志文件滚动策略

```
docmind-audit.log (当前活跃文件)
    │
    ├── 达到 100MB 或 跨天
    │
    ▼
docmind-audit.2026-04-02.0.log  (滚动归档)
docmind-audit.2026-04-02.1.log  (滚动归档)
...
docmind-audit.2026-03-25.29.log (30天后删除)
```

---

## 8. 性能优化机制

### 8.1 异步写入（主性能优化点）

```
主线程（HTTP请求线程）          异步线程池（audit-log-*）
    │                                │
    │ 1. 数据收集 (~2ms)              │
    │ 2. JSON序列化 (~1ms)            │
    │ 3. 提交任务到队列 (~0.05ms)      │
    │ 4. 返回响应给客户端 ✅           │
    │                                │
    │                           5. 数据库插入 (~10ms)
    │                           6. 如失败: 重试 + 降级
```

**收益**：主线程减少约 10ms 阻塞时间。

### 8.2 正则表达式预编译

```java
// ✅ 预编译一次，多次复用（Pattern 是线程安全的）
private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

// ❌ 每次调用都编译（性能杀手）
Pattern.compile(regex).matcher(text).replaceAll(replacement);
```

### 8.3 短路策略

```java
// ✅ 快速路径：空值直接返回
if (text == null || text.isBlank()) {
    return text;  // 零开销
}

// ✅ 条件性 IP 过滤
if (maskIp) {
    masked = IP_V4_PATTERN.matcher(masked).replaceAll("***IP***");
}
```

### 8.4 ObjectMapper 复用

```java
// ✅ Spring 注入的单例 ObjectMapper（已配置好序列化特性）
@RequiredArgsConstructor
public class SensitiveDataFilter {
    private final ObjectMapper objectMapper;  // 单例，线程安全
}

// ❌ 每次 new ObjectMapper()（浪费资源）
ObjectMapper mapper = new ObjectMapper();
```

---

## 9. 异常处理与容错机制

### 9.1 异常分类处理

| 异常类型 | 处理方式 | 记录内容 |
|---------|---------|---------|
| **业务异常** (BusinessException) | 记录 FAILURE + errorMessage | 正常记录 |
| **系统异常** (RuntimeException) | 记录 FAILURE + errorMessage + stacktrace | 正常记录 |
| **序列化异常** (JsonProcessingException) | 降级为 obj.toString() | warn 级别日志 |
| **数据库异常** (DataAccessException) | 重试 3 次 → 降级文件 | error 级别日志 |

### 9.2 容错保障层级

```
Level 1: AOP 切面自身异常防护
├── try-catch 包裹整个 around() 方法
├── 确保 Aspect 异常不影响业务方法执行
└── finally 保证审计日志一定输出

Level 2: JSON 序列化异常降级
├── ObjectMapper 序列化失败
└── 降级为 object.toString()

Level 3: 数据库写入重试机制
├── 最多重试 3 次（指数退避）
└── 全部失败 → 写入本地 Fallback 文件

Level 4: Fallback 文件兜底
├── synchronized 保证并发安全
├── 按日期分片存储
└── 即使文件写入失败也仅记录 CRITICAL 日志（不抛出）
```

### 9.3 线程池拒绝策略

```java
executor.setRejectedExecutionHandler((r, e) -> {
    // 记录拒绝事件（监控告警）
    log.error("审计日志任务被拒绝: pending={}, active={}", 
             e.getQueue().size(), e.getActiveCount());
    // 抛出异常让调用方感知（可选：也可改为丢弃并记录日志）
    throw new RejectedExecutionException();
});
```

---

## 附录：快速接入指南

### 步骤 1：添加注解

```java
@Audited(
    action = AuditAction.YOUR_ACTION,
    resourceType = "YourResource",
    describe = "操作描述"
)
@PostMapping("/your-endpoint")
public Result<YourResponse> yourMethod(@RequestBody YourRequest request) {
    // 业务逻辑，无需手写任何日志
    return Result.success(yourService.doSomething(request));
}
```

### 步骤 2：（可选）调整敏感参数

```java
@Audited(
    action = AuditAction.USER_UPDATE,
    resourceType = "User",
    sensitiveParams = {"idCard", "bankAccount"},  // 额外敏感字段
    excludeParams = {"avatar"}  // 排除大字段
)
```

### 步骤 3：（可选）控制异步/同步

```java
@Audited(async = false)  // 关键操作建议同步，确保实时落库
@DeleteMapping("/users/{id}")
public Result<Void> deleteUser(@PathVariable Long id) { ... }
```

---

*文档版本: v2.0 | 最后更新: 2026-04-02*
