# Controller 规范 (Controller Standards)

## 适用范围

生成 Controller 类时加载。定义 RESTful 设计、参数校验、响应封装、分层边界。

## 强制规则

### 1. 类结构

- `@RestController` + `@RequestMapping`，类注释说明业务模块
- 只做 3 件事：接收参数 → 调 Service → 返回结果
- ❌ Controller 内写业务逻辑、SQL、事务

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> create(@Validated @RequestBody OrderCreateDTO createInfo) {
        return Result.success(orderService.create(createInfo));
    }
}
```

### 2. RESTful 设计

| 操作 | 方法 | 路径示例 |
|---|---|---|
| 查询单条 | GET | `/api/orders/{id}` |
| 分页列表 | GET | `/api/orders/page` |
| 创建 | POST | `/api/orders` |
| 更新 | PUT | `/api/orders/{id}` |
| 部分更新 | PATCH | `/api/orders/{id}/status` |
| 删除 | DELETE | `/api/orders/{id}` |

- 路径全小写，复数名词，禁动词：`/api/orders` 非 `/api/getOrders`
- **多词路径用连字符（kebab-case）**：`/api/current-user-info` 非 `/api/currentUserInfo` 非 `/api/currentuserinfo`
- **团队约定：弃用缩写惯例（如 REST 生态的 `/me`），统一业务语义路径**：`/me`（反例）→ `/current-user-info`
- 嵌套资源：`/api/users/{userId}/orders`
- 幂等语义：GET/PUT/DELETE 幂等；POST 不幂等
- **Controller 方法名与路径对应业务语义**（呼应命名规范反直译）：

```java
// 反例：直译缩写 / 泛化方法名
@GetMapping("/me")
public Result<MeVO> me() { ... }

@GetMapping("/role/list")
public Result<PageResult<RoleVO>> list(RoleQueryDTO query) { ... }

// 正例：动词前缀 + 业务语义 + 语义化路径变量与参数名
@GetMapping("/current-user-info")
public Result<CurrentUserInfoVO> getCurrentUserInfo() { ... }

@GetMapping("/role/list")
public Result<PageResult<RoleVO>> queryRoleList(RoleQueryDTO roleQueryDTO) { ... }

@PostMapping("/role")
public Result<RoleVO> saveRole(@Valid @RequestBody RoleSaveDTO roleSaveDTO) { ... }

@PutMapping("/role")
public Result<Void> updateRole(@Valid @RequestBody RoleEditDTO roleEditDTO) { ... }

@DeleteMapping("/role/{roleId}")
public Result<Void> deleteRole(@PathVariable Long roleId) { ... }
```

- **方法名禁止泛化**：`list` / `create` / `update` / `delete` / `tree`（无业务对象）→ `queryRoleList` / `saveRole` / `updateRole` / `deleteRole` / `getPermissionTreeList`
- **路径变量语义化**：`{id}` → `{roleId}` / `{userId}`（带领域前缀）
- **返回集合资源的路径加 `-list`**：`GET /permission/tree` → `GET /permission/tree-list`（与返回 List 方法名 xxxList 一致）

### 3. 参数校验

- 入参对象用 `@Validated` + Bean Validation 注解
- 简单参数用 `@RequestParam` / `@PathVariable` + `@NotNull` 等，Controller 类加 `@Validated` 生效
- 校验注解写在 DTO 字段上，不写在 Controller 方法里做 if 判断

```java
public class OrderCreateDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "商品ID不能为空")
    private String skuId;

    @Min(value = 1, message = "数量必须大于0")
    @Max(value = 999, message = "数量过大")
    private Integer quantity;
}
```

### 4. 响应封装

- 统一返回 `Result<T>`（code/message/data），业务异常由全局处理器转为 `Result`
- 方法签名不写 `throws`，异常全部上抛

### 5. 分页入参

- 分页参数封装 `PageQuery` 基类（pageNum/pageSize），校验 pageSize 上限

```java
public class PageQuery {
    @Min(value = 1, message = "页码从1开始")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大100")
    private Integer pageSize = 10;
}
```

## 反例 / 正例

```java
// 反例：Controller 写业务
@GetMapping("/getOrderInfo")
public Result<OrderVO> get(String id, String type) {
    if (id == null || id.isEmpty()) {
        return Result.error("参数错误");
    }
    Order order = orderMapper.selectById(id);       // 直接操作 Mapper
    OrderVO orderVO = new OrderVO();
    orderVO.setStatus(order.getStatus() == 1 ? "已提交" : "处理中"); // 业务判断
    return Result.success(orderVO);
}

// 正例
@GetMapping("/{id}")
public Result<OrderVO> getById(@PathVariable Long id) {
    return Result.success(orderService.getById(id));
}
```

## 最佳实践

- 每接口一个 Service 方法，不做跨接口复用编排
- 文件上传用 MultipartFile 单独接口，不塞进业务 DTO
- 敏感操作（删除、改状态）加权限注解（`@PreAuthorize`），权限在 Service 二次校验
- 超时重试、幂等令牌（防重复提交）在网关/拦截器层处理，不进 Controller
- 下载接口设置响应头 Content-Disposition，文件名 URL 编码

## 性能优化建议

- 大列表接口不返回全量，强制分页或游标
- 响应 VO 只含前端需要字段，不返回整个 Entity（减少序列化体积）
- 高频读接口加缓存注解（见 caching 规范）

## 自检清单

- [ ] 只做参数接收 + Service 调用 + 返回
- [ ] RESTful 路径规范（kebab-case 连字符、弃用 /me 类缩写惯例）
- [ ] 方法名业务语义（动词前缀 + 领域术语，无 me/info 类直译）
- [ ] 入参 @Validated + 校验注解
- [ ] 统一 Result 返回
- [ ] 无 try/catch 堆积
- [ ] 无业务逻辑
- [ ] 分页参数带上限校验
