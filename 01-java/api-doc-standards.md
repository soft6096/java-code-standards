# API 文档规范 (API Documentation Standards)

## 适用范围

生成接口文档注解（SpringDoc/OpenAPI/knife4j）、评审接口文档时加载。

## 强制规则

### 1. 注解使用

- 接口文档用 OpenAPI 注解（`@Tag`/`@Operation`/`@Schema`），与 Controller/DTO 同步维护
- 类级 `@Tag(name = "订单管理", description = "...")`，方法级 `@Operation(summary = "创建订单", description = "...")`
- DTO/VO 字段用 `@Schema(description = "...")`，与校验注解并存

```java
@Tag(name = "订单管理", description = "订单查询与操作")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Operation(summary = "创建订单", description = "创建订单，返回订单号")
    @PostMapping
    public Result<OrderVO> create(@Validated @RequestBody OrderCreateDTO createInfo) {
        return Result.success(orderService.create(createInfo));
    }
}
```

```java
public class OrderCreateDTO {
    @Schema(description = "用户ID", requiredMode = REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "商品SKU", requiredMode = REQUIRED)
    @NotBlank(message = "商品SKU不能为空")
    private String skuId;
}
```

### 2. 文档与代码一致

- 接口签名变更 → 同步改注解（文档是接口契约一部分，过期文档误导联调）
- 响应结构返回 `Result<T>` 泛型，让文档展示真实响应结构（禁 Map 裸返回，文档不可读）

### 3. 禁止事项

- ❌ 暴露 Entity 作为接口出入参（文档泄露表结构 + 多余字段；用 DTO/VO）
- ❌ 敏感字段进文档示例（示例值用脱敏假数据）
- ❌ 无注解裸接口（文档自动生成但无业务语义）
- ❌ 文档注解与校验逻辑冲突（@Schema required 与 @NotNull 不一致）

### 4. 环境

- 文档开关配置化：dev 环境开启，prod 按需关闭（防接口信息泄露）
- 离线文档导出（knife4j 导出 markdown/openapi json）进版本管理，随接口变更同步

## 自检清单

- [ ] 类/方法/字段有 OpenAPI 注解
- [ ] 出入参用 DTO/VO，无 Entity 暴露
- [ ] 注解与校验一致
- [ ] 示例无敏感数据
- [ ] 文档开关按环境配置
