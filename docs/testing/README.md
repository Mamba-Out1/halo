# Halo 测试文档中心

> 📚 完整的测试指南和最佳实践

## 📖 文档导航

### 🚀 快速开始

|                         文档                         |   适合人群    | 预计时间  |        描述         |
|----------------------------------------------------|-----------|-------|-------------------|
| [**快速入门指南**](../integration-testing-quickstart.md) | 🔰 初学者    | 30 分钟 | 从零开始学习集成测试，包含完整示例 |
| [**集成测试指南**](../integration-testing-guide.md)      | 👨‍💻 开发者 | 1 小时  | 深入学习测试模式和最佳实践     |
| [**CI/CD 集成指南**](../ci-integration-guide.md)       | 🔧 CI 负责人 | 45 分钟 | 了解 CI/CD 流程和配置    |

### 📚 详细文档

#### 1. 快速入门指南

**适合**: 完全不了解集成测试的开发者

**内容**:
- ✅ 什么是集成测试
- ✅ 环境准备
- ✅ 运行第一个测试
- ✅ 编写第一个测试
- ✅ 使用 MySQL 测试
- ✅ 查看测试报告
- ✅ 配置 CI

**链接**: [integration-testing-quickstart.md](../integration-testing-quickstart.md)

#### 2. 集成测试指南

**适合**: 有一定测试基础的开发者

**内容**:
- 📊 测试架构设计
- 🧪 Controller 层测试
- 💼 Service 层测试
- 🗄️ Repository 层测试
- 🔄 完整流程测试
- 🎯 最佳实践
- 🐛 故障排查

**链接**: [integration-testing-guide.md](../integration-testing-guide.md)

#### 3. CI/CD 集成指南

**适合**: CI/CD 负责人和 DevOps 工程师

**内容**:
- 🔄 CI 流程图
- ⚙️ GitHub Actions 配置
- 📊 测试报告查看
- 🐛 故障排查
- 🚀 性能优化
- 📈 监控和告警

**链接**: [ci-integration-guide.md](../ci-integration-guide.md)

## 🎯 学习路径

### 路径 1: 初学者路径

```
1. 快速入门指南 (30 分钟)
   ↓
2. 运行示例测试 (15 分钟)
   ↓
3. 编写第一个测试 (30 分钟)
   ↓
4. 学习集成测试指南 (1 小时)
```

### 路径 2: 开发者路径

```
1. 集成测试指南 (1 小时)
   ↓
2. 学习测试模式 (30 分钟)
   ↓
3. 实践编写测试 (2 小时)
   ↓
4. 学习最佳实践 (30 分钟)
```

### 路径 3: CI 负责人路径

```
1. CI/CD 集成指南 (45 分钟)
   ↓
2. 配置 GitHub Actions (30 分钟)
   ↓
3. 优化 CI 流程 (1 小时)
   ↓
4. 监控和维护 (持续)
```

## 📁 代码示例

### 完整示例

|                                                               文件                                                                |    描述     |                  测试类型                  |
|---------------------------------------------------------------------------------------------------------------------------------|-----------|----------------------------------------|
| [PostFullStackIntegrationTest.java](../../application/src/test/java/run/halo/app/integration/PostFullStackIntegrationTest.java) | 完整的集成测试示例 | Controller → Service → Repository → DB |
| [PostIntegrationTests.java](../../application/src/test/java/run/halo/app/content/PostIntegrationTests.java)                     | 文章集成测试    | Service + Repository                   |
| [ControllerApiIntegrationTests.java](../../application/src/test/java/run/halo/app/content/ControllerApiIntegrationTests.java)   | API 集成测试  | Controller + Service                   |

### 测试配置

|                                               文件                                                |       描述        |
|-------------------------------------------------------------------------------------------------|-----------------|
| [application.yaml](../../application/src/test/resources/application.yaml)                       | 测试配置（H2）        |
| [application-mysql.yaml](../../application/src/test/resources/application-mysql.yaml)           | MySQL 测试配置      |
| [application-postgresql.yaml](../../application/src/test/resources/application-postgresql.yaml) | PostgreSQL 测试配置 |

### CI 配置

|                                   文件                                   |    描述    |
|------------------------------------------------------------------------|----------|
| [halo.yaml](../../.github/workflows/halo.yaml)                         | 主 CI 工作流 |
| [integration-test.yaml](../../.github/workflows/integration-test.yaml) | 集成测试工作流  |

## 🛠️ 常用命令

### 测试命令

```bash
# 运行所有测试
./gradlew :application:test

# 运行集成测试
./gradlew :application:test --tests "*IntegrationTest*"

# 运行特定测试类
./gradlew :application:test --tests PostFullStackIntegrationTest

# 运行特定测试方法
./gradlew :application:test --tests PostFullStackIntegrationTest.shouldCompletePostLifecycle

# 生成覆盖率报告
./gradlew :application:test jacocoTestReport

# 查看测试报告
open application/build/reports/tests/test/index.html
open application/build/reports/jacoco/test/html/index.html
```

### 代码质量命令

```bash
# 检查代码格式
./gradlew spotlessCheck

# 自动修复代码格式
./gradlew spotlessApply

# 运行静态代码分析
./gradlew check -x test
```

### Docker 命令

```bash
# 启动 MySQL 测试容器
docker run -d --name halo-test-mysql \
  -e MYSQL_ROOT_PASSWORD=test_password \
  -e MYSQL_DATABASE=halo_test \
  -p 3306:3306 \
  mysql:8.0

# 启动 PostgreSQL 测试容器
docker run -d --name halo-test-postgres \
  -e POSTGRES_PASSWORD=test_password \
  -e POSTGRES_DB=halo_test \
  -p 5432:5432 \
  postgres:15

# 清理容器
docker stop halo-test-mysql halo-test-postgres
docker rm halo-test-mysql halo-test-postgres
```

## 📊 测试架构

### 测试层次

```
┌─────────────────────────────────────────┐
│         Controller 层测试                │
│  • HTTP 请求/响应                        │
│  • 路由和认证                            │
│  • API 契约验证                          │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Service 层测试                   │
│  • 业务逻辑                              │
│  • 事务管理                              │
│  • 数据转换                              │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Repository 层测试                │
│  • CRUD 操作                             │
│  • 复杂查询                              │
│  • 数据一致性                            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Database                         │
│  • H2 (测试)                             │
│  • MySQL (集成测试)                      │
│  • PostgreSQL (集成测试)                 │
└─────────────────────────────────────────┘
```

### CI/CD 流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│ 代码提交  │ --> │ 代码检查  │ --> │   编译   │
└──────────┘     └──────────┘     └──────────┘
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        │                               │                               │
┌───────▼────────┐         ┌───────────▼────────┐         ┌───────────▼────────┐
│  单元测试 (H2)  │         │ 集成测试 (MySQL)    │         │集成测试 (PostgreSQL)│
└───────┬────────┘         └───────────┬────────┘         └───────────┬────────┘
        │                               │                               │
        └───────────────────────────────┼───────────────────────────────┘
                                        │
                                ┌───────▼────────┐
                                │   测试报告     │
                                └───────┬────────┘
                                        │
                                ┌───────▼────────┐
                                │  构建 & 发布   │
                                └────────────────┘
```

## 📈 测试指标

### 覆盖率目标

|     层次     | 目标覆盖率 |   当前覆盖率    |
|------------|-------|------------|
| Controller | > 80% | 查看 Codecov |
| Service    | > 90% | 查看 Codecov |
| Repository | > 85% | 查看 Codecov |
| 整体         | > 80% | 查看 Codecov |

### 性能基准

|    指标    |   目标    |        说明        |
|----------|---------|------------------|
| 单元测试执行时间 | < 5 分钟  | H2 内存数据库         |
| 集成测试执行时间 | < 15 分钟 | MySQL/PostgreSQL |
| 总 CI 时间  | < 30 分钟 | 包括构建和测试          |
| 测试成功率    | > 99%   | 排除不稳定测试          |

## 🔗 相关资源

### 官方文档

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Reactor Test](https://projectreactor.io/docs/core/release/reference/#testing)
- [GitHub Actions](https://docs.github.com/en/actions)

### 工具和服务

- [Codecov](https://codecov.io/gh/halo-dev/halo) - 代码覆盖率
- [GitHub Actions](https://github.com/halo-dev/halo/actions) - CI/CD
- [Gradle](https://gradle.org/) - 构建工具

### 社区资源

- [Halo 官网](https://halo.run)
- [Halo 文档](https://docs.halo.run)
- [GitHub 仓库](https://github.com/halo-dev/halo)
- [社区论坛](https://bbs.halo.run)

## ❓ 常见问题

### Q1: 我应该从哪里开始？

**A**: 如果你是初学者，从 [快速入门指南](../integration-testing-quickstart.md) 开始。如果你有测试经验，直接阅读 [集成测试指南](../integration-testing-guide.md)。

### Q2: 测试需要多长时间？

**A**:
- 单元测试（H2）: ~5 分钟
- 集成测试（MySQL）: ~10 分钟
- 完整 CI 流程: ~30 分钟

### Q3: 如何查看测试报告？

**A**:
1. 本地: `application/build/reports/tests/test/index.html`
2. CI: GitHub Actions 页面的 Artifacts
3. 覆盖率: https://codecov.io/gh/halo-dev/halo

### Q4: 测试失败了怎么办？

**A**:
1. 查看错误信息和堆栈跟踪
2. 单独运行失败的测试
3. 使用 IDE 调试器
4. 查看 [故障排查](../ci-integration-guide.md#故障排查) 章节

### Q5: 如何贡献测试代码？

**A**:
1. Fork 项目
2. 创建新分支
3. 编写测试
4. 提交 Pull Request
5. 等待 CI 检查通过

## 📞 获取帮助

如果你遇到问题或有疑问：

1. 📖 查看相关文档
2. 🔍 搜索 [GitHub Issues](https://github.com/halo-dev/halo/issues)
3. 💬 在 [社区论坛](https://bbs.halo.run) 提问
4. 📧 联系项目维护者

## 🤝 贡献

欢迎贡献测试代码和文档！

1. 阅读 [贡献指南](../../CONTRIBUTING.md)
2. 遵循 [代码规范](../../CLAUDE.md)
3. 编写测试和文档
4. 提交 Pull Request

## 📝 更新日志

|     日期     |  版本   |       更新内容        |
|------------|-------|-------------------|
| 2024-05-29 | 1.0.0 | 初始版本，包含完整的测试文档和示例 |

---

**Happy Testing!** 🎉

如果这些文档对你有帮助，请给项目一个 ⭐️！
