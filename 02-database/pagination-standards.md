# 分页查询规范 (Pagination Standards)

## 适用范围

实现列表分页查询时加载。定义分页方式选择、实现与深分页处理。

## 强制规则

### 1. 分页方式选择

| 数据量 | 推荐方式 |
|---|---|
| < 100 万 | MyBatis-Plus 分页插件（`Page`） |
| 100 万 ~ 1000 万 | 分页插件 + 索引条件约束 |
| > 1000 万 / 深分页 | 游标/键集分页 |

- 统一用 MyBatis-Plus `PaginationInnerInterceptor`，禁止手写 `LIMIT offset, size`

### 2. 分页插件用法

- Mapper 方法第一参数 `Page<T>`，插件自动 count + 数据查询

```java
// Service
public PageResult<OrderVO> queryPage(OrderQueryDTO orderQuery) {
    Page<Order> page = new Page<>(orderQuery.getPageNum(), orderQuery.getPageSize());
    IPage<Order> result = orderMapper.selectOrderPage(page, orderQuery);
    return PageResult.of(result.getRecords(), result.getTotal());
}

// Mapper
Page<Order> selectOrderPage(Page<Order> page, @Param("query") OrderQueryDTO orderQuery);
```

```xml
<select id="selectOrderPage" resultType="com.example.order.entity.Order">
    SELECT <include refid="orderColumns"/>
    FROM t_order
    <where>
        <if test="query.userId != null">AND user_id = #{query.userId}</if>
        <if test="query.status != null">AND status = #{query.status}</if>
    </where>
    ORDER BY create_time DESC, id DESC
</select>
```

- 插件配置 `maxLimit` 防止一次性拉全表（见 config 规范示例）

### 3. 分页参数

- `pageNum` 从 1 开始，`pageSize` 上限校验（默认 10，最大 100）
- 非法页码防御：pageNum ≤ 0 归一为 1，pageSize > 上限钳制
- 前端传参统一 `PageQuery` 基类（见 controller 规范）

### 4. 深分页处理（offset 大）

`LIMIT 100000, 20` 慢因：MySQL 先扫 100020 行再丢前 100000。方案：

**键集分页（推荐）**：记住上一页最后一条的排序键

```java
// 按 id 降序翻页：每次带上 lastId
public List<Order> queryByCursor(Long lastId, int size) {
    return orderMapper.selectList(new LambdaQueryWrapper<Order>()
            .lt(Order::getId, lastId)      // 第一页 lastId = null → 不加条件
            .orderByDesc(Order::getId)
            .last("LIMIT " + size));
}
```

```xml
<!-- 等价的 XML -->
<select id="selectPageByCursor" resultType="com.example.order.entity.Order">
    SELECT <include refid="orderColumns"/>
    FROM t_order
    <where>
        <if test="lastId != null">AND id &lt; #{lastId}</if>
        <if test="query.userId != null">AND user_id = #{query.userId}</if>
    </where>
    ORDER BY id DESC
    LIMIT #{size}
</select>
```

- 键集分页前提：排序键唯一稳定（id 天然满足）；多列排序时建对应复合索引
- 前端体验差异：无页码跳转，用「加载更多 / 上一页下一页游标」

### 5. 排序稳定性

- `ORDER BY` 加唯一字段（id）兜底，避免同排序值导致翻页重复/遗漏

```sql
-- 反例：create_time 相同的大量行，翻页错乱
ORDER BY create_time DESC
-- 正例
ORDER BY create_time DESC, id DESC
```

### 6. count 查询优化

- 分页插件自动 count；条件复杂（join + 多条件）时 count 昂贵
- 无需总数场景（加载更多）用键集分页，天然免 count
- count 结果缓存：列表条件不变时缓存总数（可选）

## 反例 / 正例

```java
// 反例：手写 offset 深分页 + 无上限
int offset = (pageNum - 1) * pageSize;
orderMapper.selectList(new LambdaQueryWrapper<Order>()
        .last("LIMIT " + offset + ", " + pageSize));   // 深分页 + 字符串拼接

// 正例：游标
queryByCursor(lastId, pageSize);
```

## 自检清单

- [ ] 分页插件统一，无手写 LIMIT offset
- [ ] pageSize 有上限校验
- [ ] 深分页用键集分页
- [ ] ORDER BY 含唯一字段
- [ ] 排序字段走索引
- [ ] 游标条件绑定参数，无拼接
