# 配置文件规范 (Application Config Standards)

## 适用范围

编写/审查 Spring Boot 配置文件（`application.yml` / `application-{env}.yml`）时加载。覆盖：多环境 profile、配置注释、数据库连接池、Redis 连接池、MQ 配置、生产环境敏感配置。

## 强制规则

### 1. 多环境 profile（dev / test / online 三件套）

配置文件按环境拆分，**至少三份**：

| 文件 | 环境 | 内容 |
|------|------|------|
| `application.yml` | 公共 | 公共配置 + profile 激活（**不写死环境**，见下） |
| `application-dev.yml` | 开发 | 本地连接（本机 MySQL/Redis） |
| `application-test.yml` | 测试 | 测试库连接（CI/契约测试用，数据可重建） |
| `application-online.yml` | 生产 | 上线环境（敏感值用环境变量占位符，不写死） |

**profile 激活方式**（禁止写死具体环境）：

```yaml
# application.yml —— 默认 dev，生产/测试显式注入，不写死 test/online
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

- 禁止 `active: test` / `active: online` 写死（生产忘指定 profile 会连错库）
- 契约测试：测试类加 `@ActiveProfiles("test")` **显式隔离**，不依赖默认值
- 生产：部署环境注入 `SPRING_PROFILES_ACTIVE=online`（漏设启动连 dev 库，由部署校验兜底）
- 本地开发：默认 dev 即可，或 `--spring.profiles.active=dev`

- 禁止把环境差异写在代码里（if dev / if prod 分支），一律走 profile 文件
- dev/test 允许写死本地凭据；**online 禁止写死密码/密钥**，用 `${ENV_VAR:默认值}` 占位（敏感项无默认值，缺变量启动失败）
- 环境相关公共项（如 `mybatis-plus`、`jackson`）放 `application.yml`，环境差异项放各 profile 文件

### 2. 配置项逐条注释（要知道配的是什么）

每个配置项必须带注释：**这个配置是干什么的、取值范围、环境差异、为什么这么设**。

```yaml
spring:
  datasource:
    # 数据库连接串：测试环境使用本地 VM 的 MySQL（NAT 3308 → VM 3306），库名 permission_test
    url: jdbc:mysql://127.0.0.1:3308/permission_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: mysql-admin      # 数据库账号，仅测试环境使用
    password: Mysql123456!     # 数据库密码，测试环境明文（online 必须环境变量）
    hikari:
      maximum-pool-size: 10    # 连接池上限：测试环境并发低，10 足够；单实例建议 10~20
      minimum-idle: 2          # 最小空闲连接，避免频繁建连
```

- 禁止裸配置无注释（`password: xxxxx` 无说明）
- 分组配置（datasource/redis/mq）组头注释说明该模块用途
- 环境差异值注释标明「仅 dev/test」「online 用环境变量」

### 3. 数据库连接池：HikariCP（Spring Boot 默认，必须显式配池参数）

Spring Boot 3 默认 HikariCP，**必须显式配置池参数**（默认值不适用于多数场景）：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # 最大连接数：按并发评估，单实例 10~20；写多可适当上调
      minimum-idle: 2             # 最小空闲连接
      connection-timeout: 30000   # 获取连接超时 ms（默认 30000）
      idle-timeout: 600000        # 空闲回收 ms（默认 600000）
      max-lifetime: 1800000       # 连接最大存活 ms（默认 1800000，须小于数据库 wait_timeout）
      pool-name: PermissionHikariPool  # 连接池名，便于监控日志区分
```

- 禁止依赖 HikariCP 默认值裸奔（不写 = 用默认，大流量下连接不足/泄漏难排查）
- `max-lifetime` 必须小于数据库 `wait_timeout`，否则连接被 DB 掐断后池内仍复用

### 4. Redis 连接池：Lettuce + commons-pool2

Spring Data Redis 默认 Lettuce 客户端，**连接池需加 `commons-pool2` 依赖 + 显式池配置**（不加依赖则池配置不生效）：

```yaml
# pom.xml 增加
# <dependency>
#   <groupId>org.apache.commons</groupId>
#   <artifactId>commons-pool2</artifactId>
# </dependency>

spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: redis123456      # 密码，online 用环境变量
      lettuce:
        pool:
          max-active: 16         # 最大连接数（默认 8，按并发上调）
          max-idle: 8            # 最大空闲连接
          min-idle: 2            # 最小空闲连接
          max-wait: 3000         # 获取连接最大等待 ms（-1 无限等待）
```

- 有 Redis 使用（缓存/黑名单/分布式锁）必须配连接池，禁止裸 host/port
- 无 redis 依赖的项目不配（配了也不生效，属无效配置）

### 5. MQ（RabbitMQ 示例）：用到才配，配置项齐全

项目引入 MQ 依赖时，配置项必须齐全（禁止只配 host/port 裸奔）：

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1             # MQ 地址，online 用环境变量
    port: 5672
    username: admin
    password: admin123456
    virtual-host: /             # vhost 隔离环境
    publisher-confirm-type: correlated   # 生产者确认（correlated：回调确认）
    publisher-returns: true              # 消息不可达回退
    listener:
      simple:
        acknowledge-mode: manual         # 消费者手动 ACK（业务处理成功才确认）
        prefetch: 10                     # 预取条数，防积压
        retry:
          enabled: true                  # 消费失败重试
          max-attempts: 3                # 最大重试次数
        default-requeue-rejected: false  # 重试耗尽不进死信则丢弃（防无限 requeue）
```

- 项目无 MQ 依赖 → 不配置（无意义配置禁止）
- 消费者必须手动 ACK + 重试上限，禁止自动 ACK 丢消息

### 6. 生产环境（online）敏感配置

```yaml
spring:
  datasource:
    # 生产库地址/账号/密码全部走环境变量，禁止写死
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

- 密码/密钥/token：online 一律 `${ENV_VAR}`，无默认值（缺变量直接启动失败，防误用）
- 本地开发允许 `:默认值` 形式（`${DB_HOST:127.0.0.1}`）

## 反例 / 正例

```yaml
# 反例：无注释、无连接池、无 profile 分离
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/app
    username: root
    password: 123456

# 正例：注释齐全 + HikariCP 池 + 环境分离（application-online.yml 用环境变量）
spring:
  datasource:
    # 应用库连接：dev 本地 MySQL
    url: jdbc:mysql://127.0.0.1:3306/app?serverTimezone=Asia/Shanghai
    username: app_dev          # 仅 dev 用
    password: dev123456        # 仅 dev 用；online 走 ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10    # 连接池上限
      minimum-idle: 2
      pool-name: AppHikariPool
```

## 自检清单

- [ ] 三件套齐全：application.yml + application-dev.yml + application-test.yml + application-online.yml
- [ ] 每个配置项有注释（用途/取值/环境差异）
- [ ] HikariCP 池参数显式配置（非默认裸奔）
- [ ] 有 Redis 使用 → commons-pool2 依赖 + lettuce.pool 配置齐全
- [ ] 有 MQ → 生产者确认 + 手动 ACK + 重试上限齐全
- [ ] online 无写死密码/密钥（全环境变量）
- [ ] 无无效配置（未用的中间件不配置）
