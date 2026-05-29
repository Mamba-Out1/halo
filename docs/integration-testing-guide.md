# Halo 集成测试指南

## 目录

1. [集成测试概述](#集成测试概述)
2. [测试架构](#测试架构)
3. [环境准备](#环境准备)
4. [编写集成测试](#编写集成测试)
5. [运行测试](#运行测试)
6. [CI/CD 集成](#cicd-集成)
7. [最佳实践](#最佳实践)

## 集成测试概述

集成测试验证多个组件协同工作的能力。在 Halo 项目中，我们采用以下测试模式：

```
Controller → Service → Repository → Database (MySQL/H2)
```

### 测试层次

|       层次       |        职责         |        测试重点        |
|----------------|-------------------|--------------------|
| **Controller** | HTTP 请求处理、路由、认证授权 | API 端点、请求/响应格式、状态码 |
| **Service**    | 业务逻辑、事务管理         | 业务规则、数据转换、异常处理     |
| **Repository** | 数据持久化、查询          | CRUD 操作、复杂查询、数据一致性 |
| **Database**   | 数据存储              | 数据完整性、约束、事务        |

## 测试架构

### 技术栈

- **测试框架**: JUnit 5
- **Spring 测试**: Spring Boot Test, WebTestClient
- **数据库**: H2 (测试环境), MySQL (可选)
- **Mock 框架**: Mockito
- **断言库**: AssertJ
- **响应式测试**: Reactor Test

### 测试类型

1. **单元测试**: 测试单个组件（Service、Repository）
2. **集成测试**: 测试多个组件协同工作
3. **端到端测试**: 测试完整的用户场景

## 环境准备

### 1. 依赖配置

在 `application/build.gradle` 中已包含必要的测试依赖：

```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.boot:spring-boot-webtestclient'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'io.projectreactor:reactor-test'
```

### 2. 测试配置

测试配置文件位于 `application/src/test/resources/application.yaml`：

```yaml
spring:
  r2dbc:
    name: halo-test
    generate-unique-name: true  # 每次测试使用独立的 H2 数据库
  sql:
    init:
      mode: always
      platform: h2

halo:
  work-dir: ${user.home}/halo-next-test
  security:
    initializer:
      disabled: true  # 禁用初始化器以加快测试
```

### 3. MySQL 测试配置（可选）

创建 `application/src/test/resources/application-mysql.yaml`：

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/halo_test
    username: root
    password: password
  sql:
    init:
      mode: always
      platform: mysql
```

## 编写集成测试

### 1. Controller 集成测试

测试 HTTP 端点、请求处理和响应格式。

**示例**: `PostControllerIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureWebTestClient
@WithMockUser(username = "test-user", roles = "ADMIN")
class PostControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        // 设置 Mock 权限
        var role = createAdminRole();
        when(roleService.listDependenciesFlux(anySet()))
            .thenReturn(Flux.just(role));
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void shouldCreatePost() {
        webTestClient
            .post()
            .uri("/apis/api.console.halo.run/v1alpha1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPostRequest())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .value(post -> {
                assertThat(post.getMetadata().getName()).isNotBlank();
                assertThat(post.getSpec().getTitle()).isEqualTo("Test Post");
            });
    }

    @Test
    void shouldGetPostById() {
        // 先创建文章
        var createdPost = createTestPost();

        // 获取文章
        webTestClient
            .get()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .value(post -> {
                assertThat(post.getSpec().getTitle())
                    .isEqualTo(createdPost.getSpec().getTitle());
            });
    }

    @Test
    void shouldUpdatePost() {
        var createdPost = createTestPost();
        createdPost.getSpec().setTitle("Updated Title");

        webTestClient
            .put()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createdPost)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .value(post -> {
                assertThat(post.getSpec().getTitle()).isEqualTo("Updated Title");
            });
    }

    @Test
    void shouldDeletePost() {
        var createdPost = createTestPost();

        webTestClient
            .delete()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .exchange()
            .expectStatus().isOk();

        // 验证删除
        webTestClient
            .get()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .exchange()
            .expectStatus().isNotFound();
    }

    private Post createTestPost() {
        return webTestClient
            .post()
            .uri("/apis/api.console.halo.run/v1alpha1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPostRequest())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .returnResult()
            .getResponseBody();
    }
}
```

### 2. Service 集成测试

测试业务逻辑和事务管理。

**示例**: `PostServiceIntegrationTest.java`

```java
@SpringBootTest
@DirtiesContext
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private ReactiveExtensionClient client;

    @Test
    void shouldCreatePostWithContent() {
        var postRequest = createPostRequest();

        StepVerifier.create(postService.draftPost(postRequest))
            .assertNext(post -> {
                assertThat(post.getMetadata().getName()).isNotBlank();
                assertThat(post.getSpec().getHeadSnapshot()).isNotBlank();
                assertThat(post.getSpec().getOwner()).isEqualTo("test-user");
            })
            .verifyComplete();
    }

    @Test
    void shouldPublishPost() {
        // 创建草稿
        var draft = postService.draftPost(createPostRequest()).block();
        assertThat(draft).isNotNull();

        // 发布文章
        StepVerifier.create(postService.publishPost(draft.getMetadata().getName()))
            .assertNext(post -> {
                assertThat(post.getSpec().getReleaseSnapshot()).isNotBlank();
                assertThat(post.getStatus().getPhase()).isEqualTo("PUBLISHED");
            })
            .verifyComplete();
    }

    @Test
    void shouldUpdatePostContent() {
        var draft = postService.draftPost(createPostRequest()).block();
        assertThat(draft).isNotNull();

        var contentRequest = new ContentRequest();
        contentRequest.setRaw("<p>Updated content</p>");
        contentRequest.setContent("<p>Updated content</p>");
        contentRequest.setRawType("HTML");

        StepVerifier.create(
            postService.updateContent(draft.getMetadata().getName(), contentRequest)
        )
            .assertNext(post -> {
                assertThat(post.getSpec().getHeadSnapshot()).isNotBlank();
            })
            .verifyComplete();
    }

    @Test
    @Transactional
    void shouldRollbackOnError() {
        var postRequest = createPostRequest();
        postRequest.post().getSpec().setTitle(null); // 触发验证错误

        StepVerifier.create(postService.draftPost(postRequest))
            .expectError(ValidationException.class)
            .verify();

        // 验证数据库中没有创建记录
        StepVerifier.create(
            client.list(Post.class, null, null)
        )
            .expectNextCount(0)
            .verifyComplete();
    }
}
```

### 3. Repository 集成测试

测试数据访问层和数据库交互。

**示例**: `PostRepositoryIntegrationTest.java`

```java
@SpringBootTest
@DirtiesContext
class PostRepositoryIntegrationTest {

    @Autowired
    private ReactiveExtensionClient client;

    @Test
    void shouldSaveAndFindPost() {
        var post = createTestPost();

        StepVerifier.create(client.create(post))
            .assertNext(saved -> {
                assertThat(saved.getMetadata().getName()).isNotBlank();
                assertThat(saved.getMetadata().getCreationTimestamp()).isNotNull();
            })
            .verifyComplete();

        StepVerifier.create(client.get(Post.class, post.getMetadata().getName()))
            .assertNext(found -> {
                assertThat(found.getSpec().getTitle()).isEqualTo(post.getSpec().getTitle());
            })
            .verifyComplete();
    }

    @Test
    void shouldListPostsByOwner() {
        // 创建多个文章
        var post1 = createTestPost("user1", "Post 1");
        var post2 = createTestPost("user1", "Post 2");
        var post3 = createTestPost("user2", "Post 3");

        client.create(post1).block();
        client.create(post2).block();
        client.create(post3).block();

        // 查询 user1 的文章
        StepVerifier.create(
            client.list(Post.class, 
                post -> "user1".equals(post.getSpec().getOwner()), 
                null)
        )
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldUpdatePost() {
        var post = createTestPost();
        var saved = client.create(post).block();
        assertThat(saved).isNotNull();

        saved.getSpec().setTitle("Updated Title");

        StepVerifier.create(client.update(saved))
            .assertNext(updated -> {
                assertThat(updated.getSpec().getTitle()).isEqualTo("Updated Title");
                assertThat(updated.getMetadata().getVersion())
                    .isGreaterThan(saved.getMetadata().getVersion());
            })
            .verifyComplete();
    }

    @Test
    void shouldDeletePost() {
        var post = createTestPost();
        var saved = client.create(post).block();
        assertThat(saved).isNotNull();

        StepVerifier.create(
            client.delete(saved)
                .then(client.get(Post.class, saved.getMetadata().getName()))
        )
            .expectError(ExtensionNotFoundException.class)
            .verify();
    }

    @Test
    void shouldHandleConcurrentUpdates() {
        var post = createTestPost();
        var saved = client.create(post).block();
        assertThat(saved).isNotNull();

        // 模拟并发更新
        var update1 = saved.clone();
        var update2 = saved.clone();

        update1.getSpec().setTitle("Update 1");
        update2.getSpec().setTitle("Update 2");

        // 第一个更新应该成功
        var updated1 = client.update(update1).block();
        assertThat(updated1).isNotNull();

        // 第二个更新应该失败（版本冲突）
        StepVerifier.create(client.update(update2))
            .expectError(OptimisticLockingFailureException.class)
            .verify();
    }
}
```

### 4. 完整流程集成测试

测试从 Controller 到 Database 的完整流程。

**示例**: `PostFullStackIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureWebTestClient
@WithMockUser(username = "test-user", roles = "ADMIN")
@DirtiesContext
class PostFullStackIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveExtensionClient client;

    @MockitoBean
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        setupMockSecurity();
    }

    @Test
    void shouldCompletePostLifecycle() {
        // 1. 创建草稿
        var createdPost = webTestClient
            .post()
            .uri("/apis/api.console.halo.run/v1alpha1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPostRequest())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .returnResult()
            .getResponseBody();

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getStatus().getPhase()).isEqualTo("DRAFT");

        // 2. 验证数据库中的记录
        var postFromDb = client.get(Post.class, createdPost.getMetadata().getName())
            .block();
        assertThat(postFromDb).isNotNull();
        assertThat(postFromDb.getSpec().getTitle())
            .isEqualTo(createdPost.getSpec().getTitle());

        // 3. 更新内容
        webTestClient
            .put()
            .uri("/apis/api.console.halo.run/v1alpha1/posts/{name}/content", 
                 createdPost.getMetadata().getName())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "raw": "<p>Updated content</p>",
                  "content": "<p>Updated content</p>",
                  "rawType": "HTML"
                }
                """)
            .exchange()
            .expectStatus().isOk();

        // 4. 发布文章
        createdPost.getSpec().setPublish(true);
        var publishedPost = webTestClient
            .put()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createdPost)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .returnResult()
            .getResponseBody();

        assertThat(publishedPost).isNotNull();
        assertThat(publishedPost.getSpec().getReleaseSnapshot()).isNotBlank();

        // 5. 查询已发布的文章
        webTestClient
            .get()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Post.class)
            .value(post -> {
                assertThat(post.getSpec().getPublish()).isTrue();
            });

        // 6. 删除文章
        webTestClient
            .delete()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 createdPost.getMetadata().getName())
            .exchange()
            .expectStatus().isOk();

        // 7. 验证数据库中已删除
        StepVerifier.create(
            client.get(Post.class, createdPost.getMetadata().getName())
        )
            .expectError(ExtensionNotFoundException.class)
            .verify();
    }

    @Test
    void shouldHandleTransactionRollback() {
        // 创建一个会触发错误的请求
        var invalidRequest = createPostRequest();
        invalidRequest.post().getSpec().setTitle(""); // 空标题应该触发验证错误

        webTestClient
            .post()
            .uri("/apis/api.console.halo.run/v1alpha1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().is4xxClientError();

        // 验证数据库中没有创建任何记录
        StepVerifier.create(client.list(Post.class, null, null))
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void shouldEnforceAccessControl() {
        // 创建文章
        var post = createTestPost();
        var saved = client.create(post).block();
        assertThat(saved).isNotNull();

        // 尝试以未授权用户访问
        webTestClient
            .mutate()
            .build()
            .get()
            .uri("/apis/content.halo.run/v1alpha1/posts/{name}", 
                 saved.getMetadata().getName())
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
```

## 运行测试

### 本地运行

```bash
# 运行所有测试
./gradlew :application:test

# 运行特定测试类
./gradlew :application:test --tests PostControllerIntegrationTest

# 运行特定测试方法
./gradlew :application:test --tests PostControllerIntegrationTest.shouldCreatePost

# 生成测试报告
./gradlew :application:test jacocoTestReport

# 查看测试报告
open application/build/reports/tests/test/index.html
open application/build/reports/jacoco/test/html/index.html
```

### 使用 MySQL 运行测试

```bash
# 启动 MySQL 容器
docker run -d --name halo-test-mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=halo_test \
  -p 3306:3306 \
  mysql:8.0

# 运行测试
./gradlew :application:test -Dspring.profiles.active=mysql

# 停止并删除容器
docker stop halo-test-mysql && docker rm halo-test-mysql
```

## CI/CD 集成

详见 `.github/workflows/halo.yaml` 和 `.github/workflows/integration-test.yaml`

### GitHub Actions 工作流

CI 流程包括：

1. **代码检出**: 获取最新代码
2. **环境设置**: 安装 JDK 21、Node.js、pnpm
3. **依赖缓存**: 缓存 Gradle 和 pnpm 依赖
4. **代码格式检查**: 运行 Spotless
5. **编译**: 编译后端和前端代码
6. **单元测试**: 运行单元测试
7. **集成测试**: 运行集成测试（H2 和 MySQL）
8. **代码覆盖率**: 生成并上传覆盖率报告
9. **构建产物**: 生成 JAR 文件
10. **Docker 镜像**: 构建 Docker 镜像

### 测试报告

- **测试结果**: `application/build/reports/tests/test/index.html`
- **覆盖率报告**: `application/build/reports/jacoco/test/html/index.html`
- **Codecov**: https://codecov.io/gh/halo-dev/halo

## 最佳实践

### 1. 测试隔离

- 每个测试应该独立运行，不依赖其他测试
- 使用 `@DirtiesContext` 在测试后清理上下文
- 使用 `generate-unique-name: true` 为每个测试创建独立数据库

### 2. 测试数据

- 使用工厂方法创建测试数据
- 避免硬编码测试数据
- 使用有意义的测试数据名称

### 3. 断言

- 使用 AssertJ 的流式断言
- 验证关键字段，不要过度验证
- 使用描述性的断言消息

### 4. Mock 使用

- 只 Mock 外部依赖（如第三方服务）
- 不要 Mock 被测试的组件
- 使用 `@MockitoBean` 替换 Spring 容器中的 Bean

### 5. 性能

- 使用 H2 内存数据库加快测试速度
- 合理使用 `@DirtiesContext`（会重启上下文）
- 考虑使用测试套件分组运行测试

### 6. 可维护性

- 提取公共的测试工具方法
- 使用清晰的测试方法命名
- 添加必要的注释说明测试意图

### 7. 覆盖率

- 目标：代码覆盖率 > 80%
- 重点测试核心业务逻辑
- 不要为了覆盖率而写无意义的测试

## 常见问题

### 1. 测试运行缓慢

**原因**: 频繁重启 Spring 上下文

**解决方案**:
- 减少 `@DirtiesContext` 的使用
- 将相关测试放在同一个测试类中
- 使用 `@Nested` 组织测试

### 2. 数据库连接错误

**原因**: 数据库未启动或配置错误

**解决方案**:
- 检查 `application.yaml` 配置
- 确保 MySQL 容器正在运行
- 使用 H2 内存数据库进行快速测试

### 3. 权限测试失败

**原因**: Mock 的权限配置不正确

**解决方案**:
- 检查 `@WithMockUser` 配置
- 确保 Mock 的 `RoleService` 返回正确的权限
- 使用 `csrf()` 配置 CSRF 令牌

### 4. 并发测试不稳定

**原因**: 测试之间存在竞态条件

**解决方案**:
- 使用 `@DirtiesContext` 隔离测试
- 为每个测试使用唯一的数据
- 使用 `StepVerifier` 的超时机制

## 参考资源

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [WebTestClient](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#webtestclient)
- [Reactor Test](https://projectreactor.io/docs/core/release/reference/#testing)
- [AssertJ](https://assertj.github.io/doc/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)

