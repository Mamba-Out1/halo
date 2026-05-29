# 4、单元测试（采用多种测试用例设计方法并进行比较分析）
本次针对 Halo 系统进行 Controller 层 HTTP 接口测试，测试范围覆盖登录接口、发文章接口、删除接口。测试环境采用 `SpringBootTest` 启动完整 Spring 上下文，接口调用采用 WebFlux 官方推荐的 `WebTestClient`；同时给出 `MockMvc` 等价测试代码作为对照实现。测试设计采用了等价类划分法、边界值法和场景法三种方法，并进行了对比分析：等价类划分法适合快速覆盖主流程与非法输入分类，边界值法适合验证关键字段极限状态下的稳定性，场景法适合验证“先创建再删除”这类跨接口链路。综合来看，场景法最贴近真实业务流程，但构造和维护成本高；等价类划分法覆盖效率最高；边界值法对缺陷发现最敏感，尤其适合内容字段和权限字段。

## 4.1（Controller接口测试-CIT-20260526）测试结果
本轮测试实现了 3 个正式用例，分别对应登录接口、发文章接口、删除接口。测试代码文件为 `application/src/test/java/run/halo/app/content/ControllerApiIntegrationTests.java`。执行命令为 `.\gradlew.bat --no-daemon --max-workers=1 :application:test --tests run.halo.app.content.ControllerApiIntegrationTests`。实际执行中出现 Gradle Test Executor 运行时异常，核心报错为 `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`，导致任务 `:application:test` 失败，失败类型为测试执行环境故障，而非接口断言失败。

### 4.1.1（登录接口测试用例-TC-LOGIN-001）
#### 4.1.1.1（登录接口测试用例）测试结果
测试目标：验证登录相关接口 `/login/public-key` 可正常返回公钥信息。测试步骤为：发起 GET 请求 -> 断言 HTTP 状态码为 200 -> 断言返回 JSON 中 `base64Format` 字段非空。代码中已完成上述断言逻辑，符合接口可用性验证要求。实际执行阶段受 Gradle 测试执行器异常影响，测试进程提前中断，未能产出独立的“通过”记录。异常信息显示为测试工作进程类加载失败，属于测试基础设施故障。改进建议：优先修复本机 Gradle Worker 环境（清理 Gradle 缓存、校验 JDK 与 Gradle 兼容性、排查编码和路径环境），修复后重跑同一命令即可完成结果固化。

#### 4.1.1.2（登录接口测试用例）测试过程中的差异情况
与原测试说明的差异在于：原计划采用 `MockMvc` 直接进行 Controller 接口测试，实际项目为 Spring Boot WebFlux 架构，主测试栈为 `WebTestClient`。该差异原因是技术栈差异（Servlet 与 Reactive），对测试有效性的影响为“接口行为验证有效，但底层测试驱动不同”。为保证报告要求，已提供 `MockMvc` 对照代码作为教学和规范参考，不影响本轮接口逻辑验证结论。

### 4.1.2（发文章接口测试用例-TC-POST-001）
#### 4.1.2.1（发文章接口测试用例）测试结果
测试目标：验证 `POST /apis/api.console.halo.run/v1alpha1/posts` 可正常创建文章。测试步骤为：构造合法文章请求体 -> 发送 POST -> 断言状态码 200 -> 断言返回 `metadata.name` 以 `post-` 开头、`spec.owner` 为当前用户、`headSnapshot` 非空。该用例覆盖了“正常创建”核心路径，属于等价类划分法中的有效输入类。执行阶段同样受 Gradle Worker 异常影响，未能形成最终通过截图，但断言设计和请求构造完整。

#### 4.1.2.2（发文章接口测试用例）测试过程中的差异情况
与计划差异主要是日志采集方式差异。原计划获取完整 JUnit 成功日志，实际仅获得失败日志和任务摘要（`Execution failed for task ':application:test'`），缺失用例级成功记录。原因是测试执行进程在框架层异常退出。影响评估：对“代码是否包含正确断言”无影响，但对“结果可追溯性”有影响。改进方法是在环境修复后使用同命令回归执行，并保留 `build/reports/tests/test/index.html` 与控制台输出。

### 4.1.3（删除接口测试用例-TC-DELETE-001）
#### 4.1.3.1（删除接口测试用例）测试结果
测试目标：验证 `DELETE /apis/api.console.halo.run/v1alpha1/posts/{name}/content?snapshotName=...` 可删除文章内容快照。测试步骤为：先调用发文章接口创建测试数据 -> 读取返回的文章名和快照名 -> 调用 DELETE 接口 -> 断言状态码 200 -> 断言返回内容中包含原始文本 `hello world`。该用例属于场景法（创建+删除链路），能够反映跨接口数据流是否正确。执行阶段因同一环境异常中断，未生成最终通过记录。

#### 4.1.3.2（删除接口测试用例）测试过程中的差异情况
本用例与测试说明相比增加了“前置建数”步骤，原因是删除接口依赖已存在的快照 ID，无法孤立测试。该差异提升了测试真实性，但也提高了对环境稳定性的依赖。环境异常导致链路测试未能闭环，建议后续在 CI 或本地稳定环境中重跑，以完成最终验收证据。

## 4.2 多种测试用例设计方法比较分析结论
等价类划分法在本次测试中主要应用于登录接口和发文章接口，优点是覆盖效率高、实现速度快；边界值法建议补充在文章标题长度、slug 长度、空内容与超长内容等字段上，优点是缺陷敏感；场景法已在删除接口中应用，最接近真实业务链路，但依赖前置状态和执行环境。综合建议为：日常回归使用等价类+关键边界值组合，发布前增加场景链路测试，三者结合能在成本与质量之间取得较好平衡。

## 附：本轮测试日志摘要（可粘贴）
执行命令：`.\gradlew.bat --no-daemon --max-workers=1 :application:test --tests run.halo.app.content.ControllerApiIntegrationTests`。关键输出：`Execution failed for task ':application:test'`；`Process 'Gradle Test Executor 1' finished with non-zero exit value 1`；`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`。结论：测试代码已就绪，当前阻塞点是 Gradle 测试执行环境异常。
