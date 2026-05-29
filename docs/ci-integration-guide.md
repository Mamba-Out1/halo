# Halo CI/CD 集成测试指南

## 📋 目录

1. [CI 流程概述](#ci-流程概述)
2. [CI 架构设计](#ci-架构设计)
3. [GitHub Actions 配置](#github-actions-配置)
4. [测试执行流程](#测试执行流程)
5. [查看测试报告](#查看测试报告)
6. [故障排查](#故障排查)
7. [最佳实践](#最佳实践)

## CI 流程概述

Halo 项目采用 GitHub Actions 作为 CI/CD 平台，实现自动化构建、测试和部署。

### CI 流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                         代码提交/PR                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    1. 代码检出 & 环境设置                        │
│  • 检出代码 (actions/checkout@v6)                               │
│  • 设置 JDK 21 (actions/setup-java@v4)                          │
│  • 设置 Node.js & pnpm                                          │
│  • 缓存依赖 (Gradle, pnpm)                                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    2. 代码质量检查                               │
│  • Spotless 格式检查 (./gradlew spotlessCheck)                  │
│  • 静态代码分析 (./gradlew check -x test)                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    3. 编译构建                                   │
│  • 后端编译 (./gradlew :application:build -x test)              │
│  • 前端构建 (pnpm -C ui build)                                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ├──────────────┬──────────────┬───────────┐
                         ▼              ▼              ▼           ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ 4a. 单元测试 │ │ 4b. 集成测试 │ │ 4c. 集成测试 │ │ 4d. 性能测试 │
│   (H2 DB)    │ │  (MySQL 8.0) │ │(PostgreSQL)  │ │   (可选)     │
│              │ │              │ │              │ │              │
│ • 快速执行   │ │ • 真实数据库 │ │ • 真实数据库 │ │ • 压力测试   │
│ • 基础功能   │ │ • 完整流程   │ │ • 完整流程   │ │ • 性能基准   │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │                │
       └────────────────┴────────────────┴────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    5. 测试报告生成                               │
│  • JUnit 测试报告                                               │
│  • JaCoCo 覆盖率报告                                            │
│  • 上传到 Codecov                                               │
│  • 上传 Artifacts                                               │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    6. 测试结果汇总                               │
│  • 生成测试摘要                                                 │
│  • 发送通知 (失败时)                                            │
│  • 更新 PR 状态                                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    7. 构建 & 发布 (仅主分支)                     │
│  • 构建 Docker 镜像                                             │
│  • 推送到 GHCR & Docker Hub                                     │
│  • 发布 Release (仅 tag)                                        │
└─────────────────────────────────────────────────────────────────┘
```

### 测试矩阵

| 测试类型 | 数据库 | 触发条件 | 执行时间 | 目的 |
|---------|--------|---------|---------|------|
| **单元测试** | H2 (内存) | 每次 PR/Push | ~5 分钟 | 快速验证基础功能 |
| **集成测试 (MySQL)** | MySQL 8.0 | 每次 PR/Push | ~10 分钟 | 验证 MySQL 兼容性 |
| **集成测试 (PostgreSQL)** | PostgreSQL 15 | 每次 PR/Push | ~10 分钟 | 验证 PostgreSQL 兼容性 |
| **性能测试** | H2 (内存) | 主分支 Push | ~15 分钟 | 性能基准测试 |
| **E2E 测试** | Docker | 每次 PR/Push | ~20 分钟 | 端到端场景测试 |

## CI 架构设计

### 1. 工作流文件结构

```
.github/workflows/
├── halo.yaml                    # 主工作流（构建、测试、发布）
├── integration-test.yaml        # 集成测试工作流
├── openapi-check.yaml           # OpenAPI 规范检查
├── packages-preview-release.yaml # 包预览发布
└── release-ui-packages.yaml     # UI 包发布
```

### 2. 测试环境配置

#### H2 内存数据库（单元测试）

```yaml
spring:
  r2dbc:
    name: halo-test
    generate-unique-name: true  # 每个测试独立数据库
  sql:
    init:
      mode: always
      platform: h2
```

#### MySQL 8.0（集成测试）

```yaml
services:
  mysql:
    image: mysql:8.0
    env:
      MYSQL_ROOT_PASSWORD: test_password
      MYSQL_DATABASE: halo_test
    ports:
      - 3306:3306
    options: >-
      --health-cmd="mysqladmin ping"
      --health-interval=10s
      --health-timeout=5s
      --health-retries=5
```

#### PostgreSQL 15（集成测试）

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_PASSWORD: test_password
      POSTGRES_DB: halo_test
    ports:
      - 5432:5432
    options: >-
      --health-cmd="pg_isready"
      --health-interval=10s
      --health-timeout=5s
      --health-retries=5
```

## GitHub Actions 配置

### 主工作流 (halo.yaml)

```yaml
name: Halo Workflow

on:
  pull_request:
    branches: [main, release-*]
  push:
    branches: [main, release-*]
  release:
    types: [published]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - name: Setup Environment
        uses: ./.github/actions/setup-env
      - name: Check Halo
        run: ./gradlew clean check
      - name: Upload coverage
        uses: codecov/codecov-action@v6

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - name: Build Halo
        run: ./gradlew build -x check
      - name: Upload Artifacts
        uses: actions/upload-artifact@v6
```

### 集成测试工作流 (integration-test.yaml)

详见 `.github/workflows/integration-test.yaml` 文件。

主要特点：

1. **并行执行**: 单元测试、MySQL 集成测试、PostgreSQL 集成测试并行运行
2. **服务容器**: 使用 GitHub Actions Services 启动数据库
3. **健康检查**: 等待数据库就绪后再运行测试
4. **报告上传**: 自动上传测试报告和覆盖率报告
5. **结果汇总**: 生成测试摘要并显示在 PR 中

## 测试执行流程

### 1. 本地运行测试

在提交代码前，建议先在本地运行测试：

```bash
# 运行所有测试
./gradlew :application:test

# 运行单元测试
./gradlew :application:test --tests "*Test"

# 运行集成测试
./gradlew :application:test --tests "*IntegrationTest*"

# 生成覆盖率报告
./gradlew :application:test jacocoTestReport

# 查看报告
open application/build/reports/tests/test/index.html
open application/build/reports/jacoco/test/html/index.html
```

### 2. 使用 Docker 运行集成测试

```bash
# 启动 MySQL
docker run -d --name halo-test-mysql \
  -e MYSQL_ROOT_PASSWORD=test_password \
  -e MYSQL_DATABASE=halo_test \
  -p 3306:3306 \
  mysql:8.0

# 等待 MySQL 就绪
until docker exec halo-test-mysql mysqladmin ping -h localhost --silent; do
  echo "Waiting for MySQL..."
  sleep 2
done

# 运行测试
SPRING_R2DBC_URL=r2dbc:mysql://localhost:3306/halo_test \
SPRING_R2DBC_USERNAME=root \
SPRING_R2DBC_PASSWORD=test_password \
SPRING_SQL_INIT_PLATFORM=mysql \
./gradlew :application:test --tests "*IntegrationTest*"

# 清理
docker stop halo-test-mysql && docker rm halo-test-mysql
```

### 3. CI 自动触发

CI 会在以下情况自动触发：

1. **Pull Request**: 创建或更新 PR 时
2. **Push**: 推送到 `main` 或 `release-*` 分支时
3. **Release**: 发布新版本时
4. **手动触发**: 在 Actions 页面手动运行

### 4. CI 执行步骤

```bash
# 1. 代码检出
git clone --depth=1 https://github.com/halo-dev/halo.git

# 2. 环境设置
# - 安装 JDK 21
# - 安装 Node.js 和 pnpm
# - 缓存依赖

# 3. 代码质量检查
./gradlew spotlessCheck
./gradlew check -x test

# 4. 编译构建
./gradlew :application:build -x test
pnpm -C ui build

# 5. 运行测试
./gradlew :application:test

# 6. 生成报告
./gradlew :application:jacocoTestReport

# 7. 上传报告
# - 上传到 Codecov
# - 上传 Artifacts
```

## 查看测试报告

### 1. GitHub Actions 界面

1. 打开项目的 GitHub 页面
2. 点击 **Actions** 标签
3. 选择对应的工作流运行记录
4. 查看各个 Job 的执行结果

![GitHub Actions](https://docs.github.com/assets/cb-27528/images/help/repository/actions-quickstart-logs.png)

### 2. 测试摘要

在工作流运行页面，可以看到自动生成的测试摘要：

```
🧪 测试报告摘要

## 测试结果

| 测试类型 | 状态 |
|---------|------|
| 单元测试 (H2) | ✅ 通过 |
| 集成测试 (MySQL) | ✅ 通过 |
| 集成测试 (PostgreSQL) | ✅ 通过 |

## 📁 测试报告

测试报告已上传为 Artifacts，可在 Actions 页面下载查看。
```

### 3. 下载测试报告

1. 在工作流运行页面，滚动到底部
2. 找到 **Artifacts** 部分
3. 下载对应的测试报告：
   - `unit-test-reports`: 单元测试报告
   - `mysql-integration-test-reports`: MySQL 集成测试报告
   - `postgresql-integration-test-reports`: PostgreSQL 集成测试报告

### 4. 查看覆盖率报告

#### Codecov

1. 访问 https://codecov.io/gh/halo-dev/halo
2. 查看整体覆盖率和趋势
3. 查看文件级别的覆盖率详情

#### 本地查看

```bash
# 生成覆盖率报告
./gradlew :application:test jacocoTestReport

# 打开 HTML 报告
open application/build/reports/jacoco/test/html/index.html
```

### 5. PR 状态检查

在 Pull Request 页面，可以看到 CI 检查状态：

- ✅ **All checks have passed**: 所有测试通过
- ❌ **Some checks were not successful**: 部分测试失败
- 🟡 **Some checks are still running**: 测试正在运行

点击 **Details** 可以查看详细的测试结果。

## 故障排查

### 常见问题

#### 1. 测试超时

**现象**: 测试运行超过 30 分钟后被取消

**原因**:
- 数据库连接问题
- 死锁或无限循环
- 资源泄漏

**解决方案**:
```bash
# 检查数据库连接
./gradlew :application:test --info

# 增加超时时间
./gradlew :application:test -Dtest.timeout=60m

# 单独运行失败的测试
./gradlew :application:test --tests FailingTest
```

#### 2. 数据库连接失败

**现象**: `Connection refused` 或 `Unknown database`

**原因**:
- 数据库服务未启动
- 连接配置错误
- 健康检查未通过

**解决方案**:
```yaml
# 增加健康检查重试次数
options: >-
  --health-cmd="mysqladmin ping"
  --health-interval=10s
  --health-timeout=5s
  --health-retries=10  # 增加到 10 次

# 添加等待脚本
- name: Wait for MySQL
  run: |
    for i in {1..60}; do
      if mysqladmin ping -h 127.0.0.1 --silent; then
        echo "MySQL is ready!"
        break
      fi
      sleep 1
    done
```

#### 3. 内存不足

**现象**: `OutOfMemoryError` 或测试被 OOM Killer 终止

**原因**:
- 测试数据过大
- 内存泄漏
- 并发测试过多

**解决方案**:
```gradle
// 在 build.gradle 中增加测试内存
test {
    maxHeapSize = '2G'  // 增加到 2GB
    jvmArgs '-XX:MaxMetaspaceSize=512m'
}
```

#### 4. 测试不稳定（Flaky Tests）

**现象**: 同样的测试有时通过，有时失败

**原因**:
- 时间依赖
- 并发竞态条件
- 外部依赖不稳定

**解决方案**:
```java
// 使用 @RepeatedTest 检测不稳定测试
@RepeatedTest(10)
void flakyTest() {
    // 测试代码
}

// 使用 StepVerifier 的超时机制
StepVerifier.create(publisher)
    .expectNext(expected)
    .expectComplete()
    .verify(Duration.ofSeconds(5));  // 设置超时

// 使用 @DirtiesContext 隔离测试
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class IsolatedTest {
    // 测试代码
}
```

#### 5. 覆盖率下降

**现象**: PR 导致覆盖率下降

**原因**:
- 新增代码未编写测试
- 删除了测试代码
- 测试未覆盖新分支

**解决方案**:
```bash
# 查看未覆盖的代码
./gradlew :application:jacocoTestReport
open application/build/reports/jacoco/test/html/index.html

# 为新代码添加测试
# 确保覆盖率 > 80%
```

### 调试技巧

#### 1. 启用详细日志

```bash
# Gradle 详细日志
./gradlew :application:test --info

# Spring Boot 详细日志
./gradlew :application:test -Dlogging.level.run.halo.app=DEBUG

# R2DBC SQL 日志
./gradlew :application:test -Dlogging.level.org.springframework.r2dbc=DEBUG
```

#### 2. 单独运行失败的测试

```bash
# 运行特定测试类
./gradlew :application:test --tests PostFullStackIntegrationTest

# 运行特定测试方法
./gradlew :application:test --tests PostFullStackIntegrationTest.shouldCompletePostLifecycle

# 运行匹配模式的测试
./gradlew :application:test --tests "*Integration*"
```

#### 3. 使用 IDE 调试

在 IntelliJ IDEA 中：

1. 右键点击测试类或方法
2. 选择 **Debug 'TestName'**
3. 设置断点进行调试

#### 4. 查看测试日志

```bash
# 查看最近的测试日志
cat application/build/test-results/test/TEST-*.xml

# 查看失败的测试
grep -r "FAILED" application/build/test-results/
```

## 最佳实践

### 1. 测试编写

- ✅ 每个 PR 都应包含相应的测试
- ✅ 测试应该快速、独立、可重复
- ✅ 使用有意义的测试名称
- ✅ 测试应该覆盖正常和异常情况
- ❌ 不要依赖测试执行顺序
- ❌ 不要在测试中使用 `Thread.sleep()`

### 2. CI 优化

- ✅ 使用缓存加速构建（Gradle、pnpm）
- ✅ 并行运行独立的测试
- ✅ 快速失败（fail-fast）策略
- ✅ 合理设置超时时间
- ❌ 不要在 CI 中运行长时间的测试
- ❌ 不要在 CI 中使用外部依赖

### 3. 测试维护

- ✅ 定期审查和更新测试
- ✅ 删除过时的测试
- ✅ 重构重复的测试代码
- ✅ 保持测试代码的可读性
- ❌ 不要忽略失败的测试
- ❌ 不要为了通过而修改测试

### 4. 覆盖率目标

| 层次 | 目标覆盖率 |
|------|-----------|
| **Controller** | > 80% |
| **Service** | > 90% |
| **Repository** | > 85% |
| **整体** | > 80% |

### 5. 性能基准

| 指标 | 目标 |
|------|------|
| **单元测试执行时间** | < 5 分钟 |
| **集成测试执行时间** | < 15 分钟 |
| **总 CI 时间** | < 30 分钟 |
| **测试成功率** | > 99% |

## 参考资源

### 官方文档

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Gradle 测试文档](https://docs.gradle.org/current/userguide/java_testing.html)
- [Spring Boot 测试文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 文档](https://junit.org/junit5/docs/current/user-guide/)

### 工具和服务

- [Codecov](https://codecov.io/) - 代码覆盖率服务
- [GitHub Actions Marketplace](https://github.com/marketplace?type=actions) - Actions 市场
- [Gradle Build Scans](https://scans.gradle.com/) - 构建分析

### 相关文档

- [集成测试指南](./integration-testing-guide.md)
- [贡献指南](../CONTRIBUTING.md)
- [开发者指南](./developer-guide/)

## 附录

### A. CI 配置文件完整示例

详见：
- `.github/workflows/halo.yaml`
- `.github/workflows/integration-test.yaml`

### B. 测试配置文件

详见：
- `application/src/test/resources/application.yaml`
- `application/src/test/resources/application-mysql.yaml`
- `application/src/test/resources/application-postgresql.yaml`

### C. 测试示例代码

详见：
- `application/src/test/java/run/halo/app/integration/PostFullStackIntegrationTest.java`
- `application/src/test/java/run/halo/app/content/PostIntegrationTests.java`
- `application/src/test/java/run/halo/app/content/ControllerApiIntegrationTests.java`
