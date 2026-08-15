# Mapper 规范 (Mapper Standards)

## 适用范围

生成 MyBatis-Plus Mapper 接口时加载。定义接口设计、注解 SQL 与 XML 的选择、分页、批量操作。

## 强制规则

### 1. 接口结构

- 继承 `BaseMapper<T>` 获得通用 CRUD，接口内只写自定义方法

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    Page<Order> selectOrderPage(Page<Order> page, @Param("query") OrderQueryDTO orderQuery);
}
```

- 一个 Mapper 对应一个 Entity 类，不跨表混写（跨表查询收敛到主表 Mapper）
- `@Mapper` 注解或启动类 `@MapperScan` 二选一，团队统一

### 2. 方法命名

- 查询 `select` 前缀：`selectById`、`selectByUserId`、`selectPage`
- 插入 `insert` 前缀：`insertBatch`
- 更新 `update` 前缀：`updateStatusById`
- 删除 `delete` 前缀（物理删）或 `remove`（逻辑删）
- 自定义 SQL 方法名与 SQL 行为一致：`updateStatusById` 不叫 `modifyOrder`

### 3. SQL 两种写法选择

| 场景 | 方式 |
|---|---|
| 单表简单条件 | LambdaQueryWrapper / LambdaUpdateWrapper |
| 复杂查询（多表 join、子查询、动态条件多） | XML 或 @Select 注解 |
| 批量插入/更新 | XML `<foreach>` 或注解脚本 |

- 简单单表查询禁止写死 SQL 字符串，用 Wrapper 避免 SQL 注入与硬编码字段
- 动态 SQL（`<if>`）统一放 XML，注解内长脚本可读性差

### 4. 参数传递

- 多参数必须 `@Param` 命名，禁止裸 `#{0}` `#{1}`

```java
// 反例
List<Order> selectByStatus(Integer status, Long shopId);

// 正例
List<Order> selectByStatus(@Param("status") Integer status,
                           @Param("shopId") Long shopId);
```

- 复杂查询条件封装 DTO 作为单个参数，内部字段用 `query.userId` 引用

### 5. 分页

- 使用 MyBatis-Plus 分页插件 `Page<T>`，不手写 `LIMIT offset, size`

```java
Page<Order> selectOrderPage(Page<Order> page, @Param("query") OrderQueryDTO orderQuery);
```

- 分页方法第一参数必须是 `Page`，插件自动生成 count 查询

### 6. 禁止事项

- ❌ 返回 `Map<String, Object>` 作为主结果（类型不安全），用 VO/DTO 接收
- ❌ `select *`（XML 内），明确列出列
- ❌ 在 Mapper 里写业务逻辑（if 状态判断等）
- ❌ 返回全部列给不需要的字段（性能 + 序列化冗余）

## 反例 / 正例

```java
// 反例
@Select("SELECT * FROM t_order WHERE status = #{0} AND shop_id = #{1}")
List<Order> list(Integer status, Long shopId);

// 正例（简单条件用 Wrapper，Service 层组装）
// Mapper 无需定义，直接用：
List<Order> list = orderMapper.selectList(new LambdaQueryWrapper<Order>()
        .eq(Order::getStatus, status)
        .eq(Order::getShopId, shopId));
```

## 最佳实践

- 逻辑删除列（deleted）在全局配置开启，Entity 注解 `@TableLogic`，查询自动过滤
- 字段映射用驼峰自动转换（`map-underscore-to-camel-case: true`），XML 少写 `resultMap`；有别名/复杂映射才写 resultMap
- 批量插入用 `insertBatchSomeColumn` 自定义方法或 XML foreach，不用循环 `insert`
- 查询列裁剪：列表页只查列表所需列，定义专用 VO 接收

## 性能优化建议

- 大表查询强制走索引列条件（见 index 规范），Mapper 方法注释标明期望索引
- 深分页（offset 大）改游标/键集分页，见分页规范
- 批量操作用 foreach 一次提交，控制 batch 大小（500~1000）

## 自检清单

- [ ] 继承 BaseMapper，无重复通用 CRUD 定义
- [ ] 方法命名 select/insert/update/delete 前缀
- [ ] 多参数全部 @Param
- [ ] 复杂 SQL 在 XML，简单条件用 Wrapper
- [ ] 无 select *，列明确
- [ ] 分页用 Page 参数
- [ ] 无 Map 返回主结果
- [ ] 批量操作用批量方法
