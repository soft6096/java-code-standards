# Validator 规范 (Custom Validator Standards)

## 适用范围

生成自定义 Bean Validation 校验器时加载。定义校验器结构、注册、与 DTO 校验注解配合。

## 强制规则

### 1. 校验器定位

- 自定义校验器处理框架校验覆盖不了的场景：跨字段、正则、业务字典、数值范围规则
- 结构：注解（`@Constraint`）+ 实现（`ConstraintValidator`）

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {

    String message() default "手机号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

```java
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;  // 空值由 @NotBlank 负责，校验器不重复报错
        }
        return MOBILE_PATTERN.matcher(value).matches();
    }
}
```

### 2. 空值约定

- 校验器内 null 返回 true（null 合法性由 `@NotNull` 声明），避免重复校验职责
- 正则 Pattern 编译为 static final，禁止方法内重复编译

### 3. 注解设计

- `message` 提供默认文案，使用处可覆盖
- `groups` / `payload` 必须声明（Bean Validation 规范要求）
- 校验器尽量无状态（可复用实例），不持有线程不安全字段

### 4. 跨字段校验

- 跨字段校验用类级注解（`ElementType.TYPE`），如时间段 start ≤ end

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeRangeValidator.class)
public @interface TimeRange {
    String message() default "开始时间不能晚于结束时间";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 使用
@TimeRange
@Data
public class OrderQueryDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

## 反例 / 正例

```java
// 反例：校验逻辑散落 Service + 空值重复报错
// Service 里：
if (!mobile.matches("^1[3-9]\\d{9}$")) {
    throw new BusinessException("手机号格式不正确");
}
// 每个接口重复一遍

// 正例
// DTO 字段：
@Mobile
private String mobile;
// 框架统一校验，Service 不感知
```

## 最佳实践

- 先查框架自带注解能否覆盖（@Pattern、@Size、@Email），不够再自定义
- 常用校验（手机号、身份证、金额范围）沉淀团队公共校验注解，避免各处重复写
- 校验失败信息面向用户：`"手机号格式不正确"`，不抛内部技术描述
- 批量校验失败：分组校验（group）控制更新/新增不同约束

## 自检清单

- [ ] 注解含 @Constraint + message/groups/payload
- [ ] null 返回 true，不重复 @NotNull 职责
- [ ] Pattern 为 static final
- [ ] 校验器无状态
- [ ] 跨字段校验用类级注解
- [ ] 团队公共校验器优先复用
