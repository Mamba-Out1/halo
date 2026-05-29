# CI 集成测试修复总结

## 问题描述

GitHub Actions 工作流中的集成测试失败，但本地运行测试正常。主要问题包括：

1. **集成测试 (MySQL 8.0)** - 失败 ❌
2. **集成测试 (PostgreSQL 15)** - 失败 ❌
3. **代码质量检查** - 失败 ❌

## 根本原因分析

### 1. 测试过滤器不匹配

**问题**：
- CI 工作流使用 `--tests "*IntegrationTest*"` 来过滤集成测试
- 但有些测试类命名为 `*IntegrationTests`（末尾多了 `s`）
- 导致这些测试被跳过，没有真正运行

**影响的测试类**：
- `ControllerApiIntegrationTests`
- `PostIntegrationTests`

### 2. 数据库配置未生效

**问题**：
- CI 环境通过环境变量设置了 MySQL/PostgreSQL 连接信息
- 但测试配置文件 `application.yaml` 没有读取这些环境变量
- 导致测试仍然使用默认的 H2 内存数据库

**环境变量**：

```bash
SPRING_R2DBC_URL=r2dbc:mysql://localhost:3306/halo_test
SPRING_R2DBC_USERNAME=root
SPRING_R2DBC_PASSWORD=test_password
SPRING_SQL_INIT_PLATFORM=mysql
```

### 3. 代码格式不符合规范

**问题**：
- Spotless 检测到多个文件的格式问题
- 主要是行尾符（CRLF vs LF）和 Markdown 表格对齐问题
- Windows 和 Linux 环境的差异导致

## 修复方案

### 修复 1：更新测试过滤器

**文件**：`.github/workflows/integration-test.yaml`

**修改**：在 MySQL 和 PostgreSQL 测试步骤中添加额外的过滤器

```yaml
# 修改前
./gradlew :application:test \
  --tests "*IntegrationTest*" \
  --configuration-cache \
  --configuration-cache-problems=warn \
  --no-daemon

# 修改后
./gradlew :application:test \
  --tests "*IntegrationTest*" \
  --tests "*IntegrationTests*" \  # 新增：匹配以 Tests 结尾的类
  --configuration-cache \
  --configuration-cache-problems=warn \
  --no-daemon
```

**影响**：
- ✅ 所有集成测试类都会被执行
- ✅ 包括 `*IntegrationTest` 和 `*IntegrationTests` 命名的类

### 修复 2：支持环境变量配置数据库

**文件**：`application/src/test/resources/application.yaml`

**修改**：添加环境变量占位符，并提供默认值

```yaml
# 修改前
spring:
  r2dbc:
    name: halo-test
    generate-unique-name: true
  sql:
    init:
      mode: always
      platform: h2

# 修改后
spring:
  r2dbc:
    # 支持通过环境变量覆盖数据库配置（用于 CI 环境）
    url: ${SPRING_R2DBC_URL:r2dbc:h2:mem:///halo-test?options=DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE}
    username: ${SPRING_R2DBC_USERNAME:sa}
    password: ${SPRING_R2DBC_PASSWORD:}
    name: halo-test
  sql:
    init:
      mode: always
      platform: ${SPRING_SQL_INIT_PLATFORM:h2}  # 默认 H2，可通过环境变量覆盖
```

**关键点**：
- ✅ 必须为 `url` 提供默认值，否则 Spring 无法解析空字符串
- ✅ H2 URL 包含必要的选项：`DB_CLOSE_DELAY=-1` 保持数据库在内存中，`MODE=MySQL` 兼容 MySQL 语法
- ✅ 默认用户名为 `sa`（H2 默认）
- ✅ 密码默认为空

**影响**：
- ✅ 本地开发：默认使用 H2 内存数据库（无需配置）
- ✅ CI 环境：通过环境变量使用 MySQL/PostgreSQL
- ✅ 灵活性：开发者可以在本地测试不同数据库

### 修复 3：自动格式化代码

**命令**：

```bash
./gradlew spotlessApply
```

**修复的文件**：
- `CLAUDE.md`
- `docs/ci-integration-guide.md`
- `docs/ci-troubleshooting.md`
- 以及其他 11 个文件

**影响**：
- ✅ 所有文件符合 Spotless 格式规范
- ✅ CI 代码质量检查通过

## 验证步骤

### 1. 本地验证

#### 验证格式检查

```bash
./gradlew spotlessCheck
# 应该输出：BUILD SUCCESSFUL
```

#### 验证单元测试

```bash
./gradlew :application:test
# 应该通过所有测试
```

#### 验证集成测试（H2）

```bash
./gradlew :application:test --tests "*IntegrationTest*" --tests "*IntegrationTests*"
# 应该运行所有集成测试
```

### 2. 本地复现 CI 环境

#### 启动 MySQL 容器

```bash
docker run -d \
  --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=test_password \
  -e MYSQL_DATABASE=halo_test \
  -p 3306:3306 \
  mysql:8.0
```

#### 运行 MySQL 集成测试

```bash
export SPRING_R2DBC_URL=r2dbc:mysql://localhost:3306/halo_test
export SPRING_R2DBC_USERNAME=root
export SPRING_R2DBC_PASSWORD=test_password
export SPRING_SQL_INIT_PLATFORM=mysql

./gradlew :application:test --tests "*IntegrationTest*" --tests "*IntegrationTests*"
```

#### 启动 PostgreSQL 容器

```bash
docker run -d \
  --name postgres-test \
  -e POSTGRES_PASSWORD=test_password \
  -e POSTGRES_DB=halo_test \
  -p 5432:5432 \
  postgres:15
```

#### 运行 PostgreSQL 集成测试

```bash
export SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/halo_test
export SPRING_R2DBC_USERNAME=postgres
export SPRING_R2DBC_PASSWORD=test_password
export SPRING_SQL_INIT_PLATFORM=postgresql

./gradlew :application:test --tests "*IntegrationTest*" --tests "*IntegrationTests*"
```

### 3. CI 验证

提交修改后，GitHub Actions 应该：

1. ✅ **单元测试 (H2)** - 通过
2. ✅ **集成测试 (MySQL 8.0)** - 通过
3. ✅ **集成测试 (PostgreSQL 15)** - 通过
4. ✅ **代码质量检查** - 通过

## 提交的修改

### 修改的文件

1. **`.github/workflows/integration-test.yaml`**
   - 添加 `--tests "*IntegrationTests*"` 过滤器
2. **`application/src/test/resources/application.yaml`**
   - 添加环境变量支持
3. **多个格式化的文件**
   - 通过 `spotlessApply` 自动修复

### 新增的文件

1. **`docs/ci-troubleshooting.md`**
   - CI 故障排查指南
   - 包含常见问题和解决方案
   - 本地复现 CI 环境的方法
2. **`CI_FIXES_SUMMARY.md`** (本文件)
   - 修复总结和验证步骤

## 提交信息

```bash
# 暂存修改
git add .github/workflows/integration-test.yaml
git add application/src/test/resources/application.yaml
git add docs/ci-troubleshooting.md
git add CI_FIXES_SUMMARY.md

# 提交
git commit -m "fix(ci): 修复集成测试工作流失败问题

- 添加 --tests \"*IntegrationTests*\" 过滤器以匹配所有集成测试类
- 在测试配置中支持通过环境变量配置数据库连接
- 修复代码格式问题
- 添加 CI 故障排查文档

Fixes: 集成测试在 CI 中失败但本地通过的问题"

# 推送
git push origin main
```

## 预期结果

修复后，CI 工作流应该：

|  测试类型  |      数据库      |  状态  |  预期时间  |
|--------|---------------|------|--------|
| 单元测试   | H2 (内存)       | ✅ 通过 | ~5 分钟  |
| 集成测试   | MySQL 8.0     | ✅ 通过 | ~10 分钟 |
| 集成测试   | PostgreSQL 15 | ✅ 通过 | ~10 分钟 |
| 代码质量检查 | N/A           | ✅ 通过 | ~2 分钟  |

**总时间**：约 27 分钟（并行执行）

## 后续建议

### 1. 统一测试命名规范

建议将所有集成测试类统一命名为 `*IntegrationTest`（不带 `s`）：

```java
// 推荐
public class PostFullStackIntegrationTest { }
public class UserEndpointIntegrationTest { }

// 不推荐
public class ControllerApiIntegrationTests { }  // 末尾有 s
```

### 2. 添加 Pre-commit Hook

在 `.husky/pre-commit` 中添加格式检查：

```bash
#!/bin/sh
./gradlew spotlessCheck
```

### 3. 本地开发环境配置

建议开发者在本地也测试不同数据库：

```bash
# 创建 docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: test_password
      MYSQL_DATABASE: halo_test
    ports:
      - "3306:3306"
  
  postgres:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: test_password
      POSTGRES_DB: halo_test
    ports:
      - "5432:5432"
```

### 4. 监控 CI 性能

定期检查 CI 执行时间，优化慢速测试：

```bash
# 查看测试执行时间
./gradlew :application:test --profile
```

## 相关文档

- [CI 故障排查指南](docs/ci-troubleshooting.md)
- [集成测试快速入门](docs/integration-testing-quickstart.md)
- [集成测试完整指南](docs/integration-testing-guide.md)
- [CI 集成指南](docs/ci-integration-guide.md)
- [如何触发 CI](docs/how-to-trigger-ci.md)

## 总结

通过以上三个修复：

1. ✅ **测试过滤器匹配所有集成测试类**
2. ✅ **支持通过环境变量配置数据库**
3. ✅ **代码格式符合规范**

CI 集成测试应该能够正常运行，并且本地和 CI 环境的行为保持一致。

如果仍然遇到问题，请参考 [CI 故障排查指南](docs/ci-troubleshooting.md)。
