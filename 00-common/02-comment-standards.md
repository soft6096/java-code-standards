# 注释规范 (Comment Standards)

## 适用范围

生成任意 Java 代码前必读。规范 Javadoc 与行内注释的覆盖范围、格式与写法。

## 强制规则

### 1. Javadoc 覆盖范围

以下必须写 Javadoc：
- 公开类 / 接口
- 公开方法（含参数、返回值、异常说明）
- 公开字段（尤其含义不直观的）
- 枚举值
- 常量（语义不直观时）

以下不强制：
- private 方法（逻辑复杂时可写）
- getter / setter（字段有注释即可，Lombok 场景可省）
- 简单重写方法（`@Override` 已有接口注释）

### 2. 类注释格式

```java
/**
 * 订单查询服务。
 *
 * <p>提供订单的查询、分页、状态流转查询能力。</p>
 *
 * @author zhangsan
 * @since 1.0.0
 */
public class OrderQueryService {
```

- 第一句为概述句，以句号结尾
- `<p>` 分段
- `@since` 记版本；`@author` 团队有要求才写，否则省略

### 3. 方法注释格式

```java
/**
 * 分页查询订单列表。
 *
 * @param userId    用户 ID，不可为空
 * @param status    订单状态，可空（空表示全部）
 * @param pageNum   页码，从 1 开始
 * @param pageSize  每页条数，最大 100
 * @return 订单分页结果，不含已删除订单
 * @throws BusinessException 用户不存在时抛出
 */
PageResult<OrderVO> queryPage(Long userId, String status, int pageNum, int pageSize);
```

- 参数逐个说明，空值约束写清
- 返回值说明业务含义，非类型复述（写「订单分页结果」，不写「PageResult 对象」）
- 异常条件写清

### 4. 行内注释

- 说明「为什么」，不说明「是什么」
- 反例：`int i = 0; // 将 i 设为 0`
- 正例：`// 状态为待提交时不允许删除，防止已产生支付记录的订单被误删`
- 魔法值必须注释或提取常量

### 5. 禁止事项

- ❌ 注释掉的代码（用 git 历史管理）
- ❌ 一整块代码全行注释（改为提取方法）
- ❌ 无意义注释：`// TODO 优化`（无具体事项）、`// 处理中` 等
- ❌ 与代码不一致的过时注释（改代码必改注释）

## 反例 / 正例

```java
// 反例
public List<Order> getOrders(Long id) {  // 获取订单
    List<Order> list = orderMapper.selectList(  // 查询列表
        new LambdaQueryWrapper<Order>().eq(Order::getUserId, id));
    return list;
}

// 正例
/**
 * 查询用户的全部订单。
 *
 * @param userId 用户 ID
 * @return 该用户的订单列表，按创建时间倒序
 */
public List<Order> getOrders(Long userId) {
    // 仅查未删除订单，软删除数据不返回
    return orderMapper.selectList(new LambdaQueryWrapper<Order>()
            .eq(Order::getUserId, userId)
            .eq(Order::getDeleted, 0)
            .orderByDesc(Order::getCreateTime));
}
```

## 最佳实践

- 注释解释意图与约束，代码本身表达实现
- 复杂算法（如分库分表路由、状态机）必须注释设计思路
- 特殊业务规则（如「金额单位为分」「时间存 UTC」）注释在字段/常量处
- 团队约定写入统一 Javadoc 头模板（版权、作者、日期按团队要求）

## 自检清单

- [ ] 所有公开类/方法/字段有 Javadoc
- [ ] Javadoc 参数逐个说明，含空值约束
- [ ] 返回值和异常条件已说明
- [ ] 无注释掉的代码
- [ ] 行内注释解释「为什么」
- [ ] 注释与代码一致
- [ ] 无 TODO 无实义注释
