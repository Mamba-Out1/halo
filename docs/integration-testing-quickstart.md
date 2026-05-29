# 集成测试快速入门指南

> 🎯 **目标读者**: 完全不了解集成测试的开发者  
> ⏱️ **预计时间**: 30 分钟  
> 📚 **前置知识**: 基本的 Java 和 Git 知识

## 📖 什么是集成测试？

### 简单理解

想象你在组装一台电脑：

- **单元测试** = 测试每个零件（CPU、内存、硬盘）是否正常工作
- **集成测试** = 测试所有零件组装在一起后，电脑是否能正常开机和运行

在 Halo 项目中：

- **单元测试** = 测试单个方法或类
- **集成测试** = 测试 Controller → Service → Repository → Database 的完整流程

### 为什么需要集成测试？

1. ✅ **发现集成问题**: 单个组件可能正常，但组合在一起可能出错
2. ✅ **验证真实场景**: 使用真实数据库，模拟真实用户操作
3. ✅ **提高信心**: 确保代码在生产环境中能正常工作
4. ✅ **防止回归**: 避免新代码破坏现有功能

## 🚀 第一步：环境准备

### 1. 检查 Java 版本

```bash
java -version
# 应该显示 Java 21 或更高版本
```

如果没有安装 Java 21，请访问 [Adoptium](https://adoptium.net/) 下载安装。

### 2. 克隆项目

```bash
git clone https://github.com/halo-dev/halo.git
cd halo
```

### 3. 验证构建

```bash
# Windows
gradlew.bat build -x test

# Linux/Mac
./gradlew build -x test
```

如果构建成功，说明环境配置正确！

## 📝 第二步：理解测试结构

### 测试文件位置

```
halo/
└── application/
    └── src/
        └── test/
            └── java/
                └── run/
                    └── halo/
                        └── app/
                            ├── integration/          # 集成测试（新增）
                            │   └── PostFullStackIntegrationTest.java
                            ├── content/              # 内容模块测试
                            │   ├── PostIntegrationTests.java
                            │   └── ControllerApiIntegrationTests.java
                            └── ...
```

### 测试类命名规范

| 命名模式 | 用途 | 示例 |
|---------|------|------|
| `*Test.java` | 单元测试 | `PostServiceTest.java` |
| `*IntegrationTest.java` | 集成测试 | `PostIntegrationTest.java` |
| `*FullStackIntegrationTest.java` | 完整流程测试 | `PostFullStackIntegrationTest.java` |

## 🧪 第三步：运行第一个测试

### 1. 运行所有测试

```bash
# Windows
gradlew.bat :application:test

# Linux/Mac
./gradlew :application:test
```

### 2. 查看测试报告

测试完成后，打开浏览器访问：

```
file:///你的项目路径/halo/application/build/reports/tests/test/index.html
```

你会看到：

- ✅ **Tests**: 总测试数
- ✅ **Failures**: 失败数
- ✅ **Ignored**: 忽略数
- ✅ **Duration**: 执行时间
- ✅ **Success rate**: 成功率

### 3. 运行特定测试

```bash
# 只运行集成测试
./gradlew :application:test --tests "*IntegrationTest*"

# 运行特定测试类
./gradlew :application:test --tests PostFullStackIntegrationTest

# 运行特定测试方法
./gradlew :application:test --tests PostFullStackIntegrationTest.shouldCompletePostLifecycle
```

## 📚 第四步：理解测试代码

让我们看一个简单的集成测试示例：

```java
@SpringBootTest                              // 1. 启动完整的 Spring 应用
@AutoConfigureWebTestClient                  // 2. 配置 Web 测试客户端
@WithMockUser(username = "test-user")        // 3. 模拟登录用户
class SimpleIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;    // 4. 注入测试客户端

    @Test
    void shouldCreatePost() {                // 5. 测试方法
        webTestClient
            .post()                          // 6. 发送 POST 请求
            .uri("/apis/api.console.halo.run/v1alpha1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPostRequest())  // 7. 请求体
            .exchange()                      // 8. 执行请求
            .expectStatus().isOk()           // 9. 验证状态码
            .expectBody(Post.class)          // 10. 验证响应体
            .value(post -> {
                assertThat(post.getMetadata().getName()).isNotBlank();
                assertThat(post.getSpec().getTitle()).isEqualTo("Test Post");
            });
    }
}
```

### 代码解释

1. **@SpringBootTest**: 启动完整的 Spring Boot 应用上下文
2. **@AutoConfigureWebTestClient**: 自动配置 WebTestClient 用于测试 HTTP 请求
3. **@WithMockUser**: 模拟一个已登录的用户
4. **@Autowired**: 注入 Spring 管理的 Bean
5. **@Test**: 标记这是一个测试方法
6. **post()**: 发送 POST 请求
7. **bodyValue()**: 设置请求体
8. **exchange()**: 执行 HTTP 请求
9. **expectStatus()**: 验证 HTTP 状态码
10. **expectBody()**: 验证响应体内容

## ✍️ 第五步：编写你的第一个测试

### 场景：测试创建和查询文章

创建文件：`application/src/test/java/run/halo/app/integration/MyFirstIntegrationTest.java`

```java
package run.halo.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import run.halo.app.core.extension.Metadata;
import run.halo.app.core.extension.Role;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.infra.utils.JsonUtils;

/**
 * 我的第一个集成测试
 */
@SpringBootTest
@AutoConfigureWebTestClient
@WithMockUser(username = "my-test-user", roles = "ADMIN")
@DirtiesContext
public class MyFirstIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        // 设置管理员权限
        var rule = new Role.PolicyRule.Builder()
                .apiGroups("*")
                .resources("*")
                .verbs("*")
                .build();
        var role = new Role();
        role.setMetadata(new Metadata());
        role.getMetadata().setName("admin");
        role.setRules(List.of(rule));
        when(roleService.listDependenciesFlux(anySet())).thenReturn(Flux.just(role));
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void myFirstTest_shouldCreateAndGetPost() {
        // 步骤 1: 创建文章
        var createdPost = webTestClient
                .post()
                .uri("/apis/api.console.halo.run/v1alpha1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createSimplePostRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Post.class)
                .returnResult()
                .getResponseBody();

        // 验证创建成功
        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getSpec().getTitle()).isEqualTo("我的第一篇测试文章");
        System.out.println("✅ 文章创建成功: " + createdPost.getMetadata().getName());

        // 步骤 2: 查询文章
        webTestClient
                .get()
                .uri("/apis/content.halo.run/v1alpha1/posts/{name}",
                        createdPost.getMetadata().getName())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Post.class)
                .value(post -> {
                    assertThat(post.getSpec().getTitle()).isEqualTo("我的第一篇测试文章");
                    System.out.println("✅ 文章查询成功: " + post.getSpec().getTitle());
                });
    }

    /**
     * 创建简单的文章请求
     */
    private Object createSimplePostRequest() {
        var json = """
                {
                    "post": {
                        "spec": {
                            "title": "我的第一篇测试文章",
                            "slug": "my-first-test-post",
                            "template": "",
                            "cover": "",
                            "deleted": false,
                            "publish": false,
                            "pinned": false,
                            "allowComment": true,
                            "visible": "PUBLIC",
                            "priority": 0,
                            "excerpt": {
                                "autoGenerate": true,
                                "raw": ""
                            },
                            "categories": [],
                            "tags": [],
                            "htmlMetas": []
                        },
                        "apiVersion": "content.halo.run/v1alpha1",
                        "kind": "Post",
                        "metadata": {
                            "name": "",
                            "generateName": "post-"
                        }
                    },
                    "content": {
                        "raw": "<p>这是我的第一个集成测试</p>",
                        "content": "<p>这是我的第一个集成测试</p>",
                        "rawType": "HTML"
                    }
                }
                """;
        return JsonUtils.jsonToObject(json, Object.class);
    }
}
```

### 运行你的测试

```bash
./gradlew :application:test --tests MyFirstIntegrationTest
```

如果看到：

```
✅ 文章创建成功: post-xxxxx
✅ 文章查询成功: 我的第一篇测试文章

BUILD SUCCESSFUL
```

恭喜！你已经成功编写并运行了第一个集成测试！🎉

## 🔧 第六步：使用 MySQL 测试

### 1. 启动 MySQL 容器

```bash
docker run -d --name halo-test-mysql \
  -e MYSQL_ROOT_PASSWORD=test_password \
  -e MYSQL_DATABASE=halo_test \
  -p 3306:3306 \
  mysql:8.0
```

### 2. 等待 MySQL 就绪

```bash
# Windows (PowerShell)
while (!(Test-NetConnection -ComputerName localhost -Port 3306).TcpTestSucceeded) {
    Write-Host "Waiting for MySQL..."
    Start-Sleep -Seconds 2
}

# Linux/Mac
until docker exec halo-test-mysql mysqladmin ping -h localhost --silent; do
    echo "Waiting for MySQL..."
    sleep 2
done
```

### 3. 运行测试

```bash
# Windows (PowerShell)
$env:SPRING_R2DBC_URL="r2dbc:mysql://localhost:3306/halo_test"
$env:SPRING_R2DBC_USERNAME="root"
$env:SPRING_R2DBC_PASSWORD="test_password"
$env:SPRING_SQL_INIT_PLATFORM="mysql"
./gradlew :application:test --tests MyFirstIntegrationTest

# Linux/Mac
SPRING_R2DBC_URL=r2dbc:mysql://localhost:3306/halo_test \
SPRING_R2DBC_USERNAME=root \
SPRING_R2DBC_PASSWORD=test_password \
SPRING_SQL_INIT_PLATFORM=mysql \
./gradlew :application:test --tests MyFirstIntegrationTest
```

### 4. 清理

```bash
docker stop halo-test-mysql
docker rm halo-test-mysql
```

## 📊 第七步：查看测试报告

### 1. HTML 报告

打开浏览器访问：

```
application/build/reports/tests/test/index.html
```

你会看到：

- 📊 **测试统计**: 总数、成功、失败、跳过
- ⏱️ **执行时间**: 每个测试的耗时
- 📝 **测试详情**: 每个测试的输出和错误信息

### 2. 覆盖率报告

```bash
# 生成覆盖率报告
./gradlew :application:test jacocoTestReport

# 打开报告
# Windows
start application/build/reports/jacoco/test/html/index.html

# Linux
xdg-open application/build/reports/jacoco/test/html/index.html

# Mac
open application/build/reports/jacoco/test/html/index.html
```

覆盖率报告显示：

- 🟢 **绿色**: 代码已被测试覆盖
- 🟡 **黄色**: 部分分支被覆盖
- 🔴 **红色**: 代码未被测试覆盖

## 🚀 第八步：配置 CI

### 1. 查看 CI 配置

打开 `.github/workflows/integration-test.yaml` 查看 CI 配置。

### 2. 提交代码触发 CI

```bash
# 创建新分支
git checkout -b feature/my-first-test

# 添加测试文件
git add application/src/test/java/run/halo/app/integration/MyFirstIntegrationTest.java

# 提交
git commit -m "feat: 添加我的第一个集成测试"

# 推送到远程
git push origin feature/my-first-test
```

### 3. 创建 Pull Request

1. 访问 GitHub 项目页面
2. 点击 **Pull requests** → **New pull request**
3. 选择你的分支
4. 填写 PR 描述
5. 点击 **Create pull request**

### 4. 查看 CI 结果

在 PR 页面，你会看到 CI 检查状态：

- 🟡 **Pending**: 正在运行
- ✅ **Success**: 测试通过
- ❌ **Failed**: 测试失败

点击 **Details** 查看详细日志。

## 📚 第九步：学习更多

### 推荐阅读顺序

1. ✅ **你已完成**: 快速入门指南（本文档）
2. 📖 **下一步**: [集成测试指南](./integration-testing-guide.md) - 深入学习测试模式
3. 🔧 **进阶**: [CI 集成指南](./ci-integration-guide.md) - 了解 CI/CD 流程
4. 💡 **参考**: [完整测试示例](../application/src/test/java/run/halo/app/integration/PostFullStackIntegrationTest.java)

### 常用命令速查

```bash
# 运行所有测试
./gradlew :application:test

# 运行集成测试
./gradlew :application:test --tests "*IntegrationTest*"

# 运行特定测试
./gradlew :application:test --tests MyFirstIntegrationTest

# 生成覆盖率报告
./gradlew :application:test jacocoTestReport

# 检查代码格式
./gradlew spotlessCheck

# 自动修复代码格式
./gradlew spotlessApply

# 完整构建（包括测试）
./gradlew build

# 快速构建（跳过测试）
./gradlew build -x test
```

## ❓ 常见问题

### Q1: 测试运行很慢怎么办？

**A**: 使用 H2 内存数据库（默认配置）而不是 MySQL，可以大幅提升速度。

### Q2: 测试失败了怎么办？

**A**: 
1. 查看错误信息：`application/build/reports/tests/test/index.html`
2. 查看日志：`application/build/test-results/test/`
3. 单独运行失败的测试：`./gradlew :application:test --tests FailingTest`

### Q3: 如何调试测试？

**A**: 在 IntelliJ IDEA 中：
1. 右键点击测试方法
2. 选择 **Debug 'testMethod'**
3. 设置断点进行调试

### Q4: 测试数据会影响数据库吗？

**A**: 不会。测试使用独立的测试数据库（H2 内存数据库或独立的 MySQL 测试库），不会影响开发或生产数据库。

### Q5: 如何跳过测试？

**A**: 
```bash
# 跳过所有测试
./gradlew build -x test

# 跳过特定测试
./gradlew :application:test -x testClassName
```

## 🎯 下一步行动

现在你已经掌握了集成测试的基础，可以：

1. ✅ **练习**: 为其他功能编写集成测试
2. ✅ **探索**: 查看现有的测试代码学习最佳实践
3. ✅ **贡献**: 为 Halo 项目贡献测试代码
4. ✅ **分享**: 帮助其他团队成员学习集成测试

## 📞 获取帮助

如果遇到问题：

1. 📖 查看 [集成测试指南](./integration-testing-guide.md)
2. 🔍 搜索 [GitHub Issues](https://github.com/halo-dev/halo/issues)
3. 💬 在 [Halo 社区](https://halo.run) 提问
4. 📧 联系项目维护者

---

**恭喜你完成了集成测试快速入门！** 🎉

现在你已经具备了编写和运行集成测试的基本能力。继续学习和实践，你会成为测试专家！💪
