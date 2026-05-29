# CI 集成测试故障排查指南

## 问题概述

本文档记录了 GitHub Actions 集成测试工作流中常见的问题及其解决方案。

## 已修复的问题

### 1. 测试过滤器不匹配

**问题描述**：
- CI 工作流使用 `--tests "*IntegrationTest*"` 过滤测试
- 但部分测试类命名为 `*IntegrationTests`（注意末尾多了 `s`）
- 导致这些测试在 CI 中被跳过

**影响的测试类**：
- `ControllerApiIntegrationTests`
- `PostIntegrationTests`

**解决方案**：
在 `.github/workflows/integration-test.yaml` 中添加额外的测试过滤器：

```yaml
./gradlew :application:test \
  --tests "*IntegrationTest*" \
  --tests "*IntegrationTests*" \  # 新增：匹配以 Tests 结尾的类
  --configuration-cache \
  --configuration-cache-problems=warn \
  --no-daemon
```

### 2. 数据库配置未生效

**问题描述**：
- CI 环境设置了 MySQL/PostgreSQL 环境变量
- 但测试配置文件 `application.yaml` 没有读取这些环境变量
- 导致测试仍然使用默认的 H2 数据库

**解决方案**：
修改 `application/src/test/resources/application.yaml`，支持环境变量覆盖：

```yaml
spring:
  r2dbc:
    # 支持通过环境变量覆盖数据库配置（用于 CI 环境）
    url: ${SPRING_R2DBC_URL:}
    username: ${SPRING_R2DBC_USERNAME:}
    password: ${SPRING_R2DBC_PASSWORD:}
    name: halo-test
    generate-unique-name: true
  sql:
    init:
      mode: always
      platform: ${SPRING_SQL_INIT_PLATFORM:h2}  # 默认 H2，可通过环境变量覆盖
```

**环境变量说明**：

|            环境变量            |      用途      |                   示例值                    |
|----------------------------|--------------|------------------------------------------|
| `SPRING_R2DBC_URL`         | R2DBC 连接 URL | `r2dbc:mysql://localhost:3306/halo_test` |
| `SPRING_R2DBC_USERNAME`    | 数据库用户名       | `root`                                   |
| `SPRING_R2DBC_PASSWORD`    | 数据库密码        | `test_password`                          |
| `SPRING_SQL_INIT_PLATFORM` | SQL 方言       | `mysql`, `postgresql`, `h2`              |

### 3. 代码格式检查失败

**问题描述**：
- Spotless 检测到代码格式不符合规范
- 主要是行尾符（CRLF vs LF）和 Markdown 表格格式问题

**解决方案**：
运行自动格式化命令：

```bash
./gradlew spotlessApply
```

**预防措施**：
在提交前运行格式检查：

```bash
./gradlew spotlessCheck
```

或配置 Git 钩子自动格式化（已在 `ui/.husky/pre-commit` 中配置）。

## 本地测试与 CI 环境差异

### 为什么本地测试通过，CI 失败？

|   差异点    |  本地环境   |      CI 环境       |      影响       |
|----------|---------|------------------|---------------|
| **数据库**  | H2（内存）  | MySQL/PostgreSQL | SQL 方言差异、并发行为 |
| **操作系统** | Windows | Ubuntu Linux     | 文件路径、行尾符      |
| **并发度**  | 单线程     | 可能并行执行           | 竞态条件          |
| **网络**   | 本地回环    | Docker 网络        | 连接延迟          |
| **文件系统** | NTFS    | ext4             | 大小写敏感性        |

### 如何在本地复现 CI 环境

#### 1. 使用 Docker 运行数据库

**MySQL**：

```bash
docker run -d \
  --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=test_password \
  -e MYSQL_DATABASE=halo_test \
  -p 3306:3306 \
  mysql:8.0
```

**PostgreSQL**：

```bash
docker run -d \
  --name postgres-test \
  -e POSTGRES_PASSWORD=test_password \
  -e POSTGRES_DB=halo_test \
  -p 5432:5432 \
  postgres:15
```

#### 2. 设置环境变量运行测试

**MySQL**：

```bash
export SPRING_R2DBC_URL=r2dbc:mysql://localhost:3306/halo_test
export SPRING_R2DBC_USERNAME=root
export SPRING_R2DBC_PASSWORD=test_password
export SPRING_SQL_INIT_PLATFORM=mysql

./gradlew :application:test --tests "*IntegrationTest*"
```

**PostgreSQL**：

```bash
export SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/halo_test
export SPRING_R2DBC_USERNAME=postgres
export SPRING_R2DBC_PASSWORD=test_password
export SPRING_SQL_INIT_PLATFORM=postgresql

./gradlew :application:test --tests "*IntegrationTest*"
```

#### 3. 使用 Act 在本地运行 GitHub Actions

安装 [Act](https://github.com/nektos/act)：

```bash
# Windows (使用 Chocolatey)
choco install act-cli

# macOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
```

运行工作流：

```bash
# 运行所有工作流
act

# 运行特定工作流
act -W .github/workflows/integration-test.yaml

# 运行特定 job
act -j integration-tests-mysql
```

## 常见问题排查

### 问题 1：MySQL 连接超时

**症状**：

```
java.net.ConnectException: Connection refused
```

**排查步骤**：
1. 检查 MySQL 容器是否启动：

```bash
docker ps | grep mysql
```

2. 检查健康状态：

   ```bash
   docker exec mysql-test mysqladmin ping -h localhost -u root -ptest_password
   ```
3. 查看容器日志：

   ```bash
   docker logs mysql-test
   ```

**解决方案**：
- 增加健康检查重试次数
- 在测试前添加等待逻辑

### 问题 2：PostgreSQL 权限错误

**症状**：

```
org.postgresql.util.PSQLException: FATAL: password authentication failed
```

**排查步骤**：
1. 验证环境变量：

```bash
echo $SPRING_R2DBC_USERNAME
echo $SPRING_R2DBC_PASSWORD
```

2. 测试连接：

   ```bash
   docker exec postgres-test psql -U postgres -d halo_test -c "SELECT 1"
   ```

**解决方案**：
- 确保环境变量正确设置
- 检查 PostgreSQL 配置文件 `pg_hba.conf`

### 问题 3：测试数据未清理

**症状**：
- 测试在本地通过，在 CI 中失败
- 错误信息提示数据已存在

**原因**：
- 测试之间共享数据库状态
- 缺少 `@DirtiesContext` 注解

**解决方案**：
在测试类上添加注解：

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MyIntegrationTest {
    // ...
}
```

或在每个测试方法后清理：

```java
@AfterEach
void tearDown() {
    // 清理测试数据
    extensionClient.delete(Post.class, postName).block();
}
```

### 问题 4：并发测试失败

**症状**：
- 单独运行测试通过
- 批量运行时失败

**原因**：
- 测试之间存在资源竞争
- 共享可变状态

**解决方案**：
1. 使用唯一的测试数据标识：

```java
String uniqueSlug = "test-post-" + UUID.randomUUID();
```

2. 避免使用静态变量

3. 使用 `@DirtiesContext` 隔离测试

## 最佳实践

### 1. 测试命名规范

**推荐**：统一使用 `*IntegrationTest` 后缀

```java
// ✅ 推荐
public class PostFullStackIntegrationTest { }
public class UserEndpointIntegrationTest { }

// ❌ 不推荐（会导致过滤器不匹配）
public class PostFullStackIntegrationTests { }  // 注意末尾的 s
```

### 2. 数据库兼容性

编写跨数据库兼容的测试：

```java
// ✅ 使用标准 SQL
@Test
void shouldQueryPosts() {
    String sql = "SELECT * FROM posts WHERE title = ?";
    // ...
}

// ❌ 避免数据库特定语法
@Test
void shouldQueryPosts() {
    String sql = "SELECT * FROM posts WHERE title LIKE '%test%' LIMIT 10";  // MySQL 特定
    // ...
}
```

### 3. 测试隔离

确保测试之间相互独立：

```java
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MyIntegrationTest {
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
    }
}
```

### 4. 环境变量配置

在 `application.yaml` 中使用环境变量：

```yaml
spring:
  r2dbc:
    url: ${SPRING_R2DBC_URL:r2dbc:h2:mem:///halo-test}  # 默认值
    username: ${SPRING_R2DBC_USERNAME:sa}
    password: ${SPRING_R2DBC_PASSWORD:}
```

### 5. CI 工作流优化

```yaml
# 使用缓存加速构建
- name: 设置 Gradle 缓存
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

# 并行运行测试
- name: 运行测试
  run: |
    ./gradlew test --parallel --max-workers=4
```

## 监控和调试

### 查看 CI 日志

1. 进入 GitHub Actions 页面
2. 选择失败的工作流运行
3. 点击失败的 job
4. 展开失败的步骤查看详细日志

### 下载测试报告

CI 会上传测试报告作为 Artifacts：

1. 在工作流运行页面找到 "Artifacts" 部分
2. 下载对应的测试报告（如 `mysql-integration-test-reports`）
3. 解压后在浏览器中打开 `index.html`

### 启用调试日志

在测试配置中启用详细日志：

```yaml
logging:
  level:
    run.halo.app: DEBUG
    org.springframework.r2dbc: DEBUG
    io.r2dbc.pool: DEBUG
```

## 相关文档

- [集成测试快速入门](integration-testing-quickstart.md)
- [集成测试完整指南](integration-testing-guide.md)
- [CI 集成指南](ci-integration-guide.md)
- [如何触发 CI](how-to-trigger-ci.md)

## 总结

通过以上修复，CI 集成测试应该能够正常运行：

✅ 测试过滤器匹配所有集成测试类  
✅ 支持通过环境变量配置数据库  
✅ 代码格式符合规范  
✅ 本地可以复现 CI 环境

如果仍然遇到问题，请参考本文档的"常见问题排查"部分，或查看相关文档。
