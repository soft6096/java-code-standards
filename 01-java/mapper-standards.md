# Mapper 规范 (Mapper Standards)

## 适用范围

生成 MyBatis-Plus Mapper 接口时加载。本规范只覆盖 **Java 接口结构**（继承/命名/参数/返回类型）；Wrapper 选择、XML、分页、批量等 MyBatis-Plus 数据访问规则见 **database-standards** skill（`mybatis-plus/mapper-standards.md`）。

## 强制规则

### 1. 接口结构

- 继承 `BaseMapper<T>` 获得通用 CRUD，接口内只写自定义方法
- 一个 Mapper 对应一个 Entity 类，不跨表混写（跨表查询收敛到主表 Mapper）
- `@Mapper` 注解或启动类 `@MapperScan` 二选一，团队统一

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    Page<Order> selectOrderPage(Page<Order> page, @Param("query") OrderQueryDTO orderQuery);
}
```

### 2. 方法命名

- 查询 `select` 前缀：`selectById`、`selectByUserId`、`selectPage`
- 插入 `insert` 前缀：`insertBatch`
- 更新 `update` 前缀：`updateStatusById`
- 删除 `delete` 前缀（物理删）或 `remove`（逻辑删）
- 自定义 SQL 方法名与 SQL 行为一致：`updateStatusById` 不叫 `modifyOrder`

### 3. 参数传递

- 多参数必须 `@Param` 命名，禁止裸 `#{0}` `#{1}`

```java
// 反例
List<Order> selectByStatus(Integer status, Long shopId);

// 正例
List<Order> selectByStatus(@Param("status") Integer status,
                           @Param("shopId") Long shopId);
```

- 复杂查询条件封装 DTO 作为单个参数，内部字段用 `query.userId` 引用

### 4. 返回类型

- 禁止 `Map<String, Object>` 作为主结果（类型不安全），用 VO/DTO 接收
- 列表页查询列裁剪：定义专用 VO，只查列表所需列（性能 + 序列化冗余）
- 返回类型与 SQL 列对齐：多表 join 用 VO，单表直接 Entity

### 5. 禁止事项

- ❌ 在 Mapper 里写业务逻辑（if 状态判断等）
- ❌ 接口方法无实现且无注解/XML（运行期炸，编译不报错）——自查
- ❌ 自定义方法重复实现 BaseMapper 已有能力

## 反例 / 正例

```java
// 反例：裸 #{0} + Map 返回
@Select("SELECT * FROM t_order WHERE status = #{0} AND shop_id = #{1}")
List<Map<String, Object>> list(Integer status, Long shopId);

// 正例：@Param 命名 + VO 接收
List<OrderVO> selectOrderVOList(@Param("status") Integer status,
                                @Param("shopId") Long shopId);
```

## 自检清单

- [ ] 继承 BaseMapper，无重复通用 CRUD 定义
- [ ] 方法命名 select/insert/update/delete 前缀
- [ ] 多参数全部 @Param
- [ ] 无 Map 返回主结果
- [ ] 无业务逻辑在 Mapper
- [ ] 接口方法均有实现（注解/XML/BaseMapper）
