# 命名规范 (Naming Standards)

## 适用范围

生成任意 Java 代码前必读。适用于类、方法、变量、常量、包名、枚举、泛型等全部标识符。

## 强制规则

### 1. 包名

- 全部小写，单词间无分隔符
- 反例：`com.Company.Order` / `com.company_name`
- 正例：`com.example.hotel.order`
- **DTO / VO 独立包，禁止挂在 controller 包下**：

| 层 | 包 | 说明 |
|---|---|---|
| Controller | `com.example.order.controller` | 接口层 |
| 请求/出参对象 | `com.example.order.dto` / `com.example.order.vo` | 与 controller **平级** |
| 实体 | `com.example.order.domain` | 数据库实体 |

- 反例：`com.example.order.controller.vo.MeVO`、`com.example.order.controller.dto.LoginDTO`
- 正例：`com.example.order.vo.CurrentUserInfoVO`、`com.example.order.dto.LoginDTO`

### 2. 类名 / 接口名

- UpperCamelCase（帕斯卡命名）
- 名词或名词短语，避免动词开头
- 接口不加 `I` 前缀（除历史遗留），实现类加 `Impl` 后缀
- 反例：`userService`、`getUserData`、`IUserService`
- 正例：`UserService`（接口）、`UserServiceImpl`（实现）、`OrderController`
- **类名表达业务语义，禁止中文直译/无领域含义缩写**：

```java
// 反例：中文直译（Me = 我）→ 无业务语义
public class MeVO { ... }
public class GetInfoDTO { ... }

// 正例：业务语义命名（当前用户信息）
public class CurrentUserInfoVO { ... }
```

- DTO/VO 类名 = 「领域概念 + 后缀」：`CurrentUserInfoVO`、`OrderQueryDTO`、`RoleAssignDTO`
- 禁止：`MeVO`（Me 无领域概念）、`InfoDTO`（Info 无领域概念）、`UserInfoDTO`（Info 冗余，若语义就是用户信息则 `UserDTO` 或 `UserVO`）
- 判据：类名去掉后缀（VO/DTO）后，剩余部分必须是**领域术语**（CurrentUserInfo / OrderQuery / RoleAssign），能独立表达业务概念

### 3. 方法名

- lowerCamelCase（驼峰命名）
- 动词或动词短语，语义清晰
- 分层约定：
  - Controller：`get/create/update/delete` 前缀，如 `getOrderById`
  - Service：`query/save/modify/remove` 或领域动词，如 `queryPage`、`submitOrder`
  - Mapper：`select/insert/update/delete` 前缀，如 `selectList`、`insertBatch`
- 布尔返回方法：`is/has/can/should` 前缀，如 `isValid`、`hasPermission`
- 反例：`handleData`、`doStuff`、`getOrderInfoByIdAndStatus`

### 4. 变量名

- lowerCamelCase，表达**语义角色**，见名知意
- **禁止无意义简写**：`dto` / `vo` / `query` / `result` / `info` 单独作变量名（作用域 ≤ 5 行可豁免）

```java
// 反例：无信息量
OrderVO vo;
OrderCreateDTO dto;
```

- **方法参数按角色命名**（领域词 + 角色词，不机械重复完整类型名）：

```java
public PageResult<OrderVO> queryPage(OrderQueryDTO orderQuery) { ... }   // 查询条件
public Long create(OrderCreateDTO createInfo) { ... }                    // 创建参数
public void update(Long id, OrderUpdateDTO updateInfo) { ... }           // 更新参数
public void cancelOrder(CancelOrderDTO cancelInfo) { ... }               // 取消参数
```

- **多个同类型变量并存时，加语义限定前缀区分**（谁是谁、用途）

```java
// 正例：同一方法两个 OrderVO
OrderVO cachedOrderVO = cache.get(key, OrderVO.class);  // 缓存中的
OrderVO dbOrderVO = orderService.getDetailFromDb(id);   // 库里的
// 转换场景
OrderVO sourceOrderVO;   // 源
OrderVO targetOrderVO;   // 目标
```

- 单变量 VO 可用「领域词 + 层后缀」：`OrderVO orderVO`（区别于 Entity `order`）
- 基础类型直接语义命名：`String name`、`Map<Long, Order> orderMap`
- **集合字段命名：统一 `xxxList` 后缀（方案 B，团队唯一约定，禁止复数命名）**：

```java
List<Long> permissionIdList;          // ✅ 一眼看出是 List
List<OrderVO> orderVOList;            // ✅ 统一 xxxList
List<Long> permissionIds;             // ❌ 复数命名（与团队约定冲突）
```

- **语义名豁免**：非"列表"语义的集合用业务名，`List<PermissionTreeVO> menuTree`（树结构不是列表集合，保持语义名）；判定：元素关系是「平级列表」→ xxxList；是「树/图/层级结构」→ 语义名
- 避免拼音、单字母（循环变量 `i/j/k` 除外）
- 反例：`OrderVO vo`、`OrderUpdateDTO dto`、`String s`、`int shuliang`、`List<Long> roleIds`
- 正例：`OrderVO orderVO`、`OrderQueryDTO orderQuery`、`String name`、`int quantity`、`List<Long> roleIdList`

### 5. 常量名

- 全大写 + 下划线分隔（`static final`）
- 反例：`static final int maxCount = 5`
- 正例：`static final int MAX_COUNT = 5`
- 枚举值同理：`DAY_STATUS_SUBMITTED`

### 6. 分层 DTO 命名后缀

| 类型 | 后缀 | 示例 |
|---|---|---|
| 请求入参 | DTO / Req | `OrderCreateDTO`、`OrderQueryDTO` |
| 出参 | VO | `OrderVO` |
| 数据库实体 | 无后缀 | `Order` |
| 业务接口 | Service | `OrderService` |
| 实现 | ServiceImpl | `OrderServiceImpl` |

### 7. 缩写规则

- 缩写视为单词：`getUserId` 而非 `getUserID`
- 类名中：`UserIdDTO` 而非 `UserIDDTO`

## 反例 / 正例

```java
// 反例：命名混乱
public class userInfoController {
    private String USERNAME;
    public void handleData(String id, String status) {
        String s = USERNAME + id;
    }
}

// 正例
public class UserInfoController {
    private static final int MAX_PAGE_SIZE = 100;

    public UserVO getUserById(Long userId) {
        return userService.getUserById(userId);
    }
}
```

## 最佳实践

- 命名自文档化：读名字即知含义，不依赖注释
- 领域术语优先：业务词用团队统一词汇（见领域模型），如 `order` 不用 `transaction`
- 布尔字段命名避免 `is` 前缀（Lombok 生成歧义）：用 `submitted` 而非 `isSubmitted`
- 方法名避免 2 个以上动词叠加：`queryAndValidateAndSubmit` → 拆多个方法

## 自检清单

- [ ] 包名全小写；DTO/VO 独立包（不在 controller 包下）
- [ ] 类 UpperCamelCase，接口无 `I` 前缀，实现带 `Impl`
- [ ] 类名业务语义（去后缀后是领域术语），无中文直译（MeVO 类反例）
- [ ] 方法 lowerCamelCase，动词开头，分层前缀正确
- [ ] 变量 lowerCamelCase，无语义不明缩写
- [ ] 集合命名统一 `xxxList` 后缀（树/层级结构豁免语义名）
- [ ] 常量全大写 + 下划线
- [ ] DTO/VO/Entity 后缀正确
- [ ] 无拼音命名
