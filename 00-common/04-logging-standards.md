# 日志规范 (Logging Standards)

## 适用范围

生成任意 Java 代码前必读。规范日志框架使用、级别选择、格式与内容。

## 强制规则

### 1. 框架与声明

- 统一 SLF4J API，具体实现（Logback/Log4j2）由依赖决定
- 禁止直接使用 `System.out.println`、`System.err.println` 输出日志

```java
// 正例：Lombok 或手动声明
@Slf4j
public class OrderService {
    // 直接使用 log 对象
}
```

### 2. 日志级别选择

| 级别 | 用途 | 示例 |
|---|---|---|
| ERROR | 系统无法继续/业务失败需告警 | 数据库异常、第三方调用失败、业务处理失败 |
| WARN | 可继续但需关注 | 参数异常被降级处理、重试成功、缓存穿透回源 |
| INFO | 关键业务节点 | 请求入口、订单创建/状态变更、定时任务完成 |
| DEBUG | 调试细节 | 中间结果、循环内数据 |
| TRACE | 极细追踪 | 少见，默认关闭 |

- 线上默认 INFO，DEBUG/TRACE 不输出业务关键信息（丢失不可恢复）
- 业务关键节点用 INFO，不用 DEBUG

### 3. 格式规范

- 使用占位符 `{}`，禁止字符串拼接

```java
// 反例
log.info("order created, orderId=" + orderId + ", userId=" + userId);

// 正例
log.info("order created, orderId={}, userId={}", orderId, userId);
```

- 占位符性能优势：参数不匹配时零开销；拼接则必然执行

### 4. 异常日志

- 异常对象作为最后一个参数传入，保留堆栈

```java
// 反例：只记 message，丢堆栈
log.error("order submit failed: {}", e.getMessage());

// 正例
log.error("order submit failed, orderId={}", orderId, e);
```

- 捕获异常必须记录堆栈；向上抛出时可只在上抛点记录一次，避免重复打
- 日志信息写业务含义，不写框架堆栈复制

### 5. 日志内容要求

- 关键日志带上下文 ID：订单号、用户 ID、traceId
- ❌ 不记录敏感信息：密码、token、身份证、手机号明文（脱敏后记）
- 日志长度限制：大对象（如整表数据）只记关键字段

```java
// 正例：脱敏
log.info("user login, userId={}, mobile={}", userId, MaskUtil.mask(mobile));
```

## 反例 / 正例

```java
// 反例
System.out.println("创建订单：" + order);
log.info("查询订单");  // 无上下文，无法定位

// 正例
log.info("order create start, orderId={}, userId={}", orderId, userId);
```

## 最佳实践

- 请求入口（Controller/网关）记录入参摘要 + 耗时，出口记录结果
- 定时任务开始/结束/失败各记一条，含批次信息
- 日志上下文：MDC 放入 traceId，链路追踪贯通调用链
- 高频循环内避免 INFO（改 DEBUG 或聚合统计）
- 日志内容与异常体系一致：ERROR 日志与 `SystemException` 抛出点一一对应

## 性能优化建议

- 使用占位符而非拼接（已列强制）
- 判断 `log.isDebugEnabled()`：DEBUG 参数构造昂贵时（如序列化对象）

```java
// 正例
if (log.isDebugEnabled()) {
    log.debug("order detail: {}", JsonUtil.toJson(order));
}
```

- 异步日志（AsyncAppender）降低 I/O 阻塞，注意丢日志风险与背压
- 避免循环内打日志：10 万次循环打 10 万条

## 自检清单

- [ ] 使用 SLF4J，无 System.out 日志
- [ ] 级别选择正确（业务节点 INFO、异常 ERROR）
- [ ] 占位符替代字符串拼接
- [ ] 异常日志带堆栈
- [ ] 关键日志含业务上下文 ID
- [ ] 无敏感信息
- [ ] 高频路径无逐条日志
