# 🚀 如何触发 GitHub Actions CI 测试

> 完整的 CI 触发指南 - 从推送代码到查看测试结果

## 📋 前提条件

1. ✅ 你已经创建了 Halo 的 GitHub 仓库
2. ✅ 代码已经在本地准备好
3. ✅ CI 配置文件已存在（`.github/workflows/` 目录）

## 🎯 触发 CI 的三种方式

### 方式 1: 推送代码到分支（最常用）

这是最常用的触发方式，每次推送代码到 `main` 或 `release-*` 分支时自动触发。

#### 步骤 1: 确认你在正确的分支

```bash
# 查看当前分支
git branch

# 如果不在 main 分支，切换到 main
git checkout main
```

#### 步骤 2: 添加并提交代码

```bash
# 查看修改的文件
git status

# 添加所有修改的文件
git add .

# 或者只添加特定文件
git add application/src/test/java/run/halo/app/integration/PostFullStackIntegrationTest.java
git add .github/workflows/integration-test.yaml
git add docs/

# 提交代码
git commit -m "feat: 添加集成测试和 CI 配置"
```

#### 步骤 3: 推送到 GitHub

```bash
# 第一次推送（如果还没有设置远程仓库）
git remote add origin https://github.com/你的用户名/halo.git
git push -u origin main

# 后续推送
git push
```

#### 步骤 4: 查看 CI 运行

1. 打开浏览器，访问你的 GitHub 仓库
2. 点击顶部的 **Actions** 标签
3. 你会看到正在运行的工作流

```
🟡 Integration Tests #1
   main branch • just now • Running
```

---

### 方式 2: 创建 Pull Request

创建 PR 时会自动触发 CI 测试。

#### 步骤 1: 创建新分支

```bash
# 创建并切换到新分支
git checkout -b feature/integration-tests

# 或者分开执行
git branch feature/integration-tests
git checkout feature/integration-tests
```

#### 步骤 2: 提交并推送

```bash
# 添加并提交代码
git add .
git commit -m "feat: 添加集成测试"

# 推送到远程
git push -u origin feature/integration-tests
```

#### 步骤 3: 在 GitHub 上创建 PR

1. 访问你的 GitHub 仓库
2. 点击 **Pull requests** 标签
3. 点击 **New pull request** 按钮
4. 选择：
   - **base**: `main`
   - **compare**: `feature/integration-tests`
5. 点击 **Create pull request**
6. 填写 PR 标题和描述
7. 点击 **Create pull request**

#### 步骤 4: 查看 CI 状态

在 PR 页面底部，你会看到 CI 检查状态：

```
✅ All checks have passed
   ✓ Integration Tests
   ✓ Halo Workflow
```

---

### 方式 3: 手动触发（workflow_dispatch）

如果你想手动触发 CI，需要先修改工作流配置。

#### 步骤 1: 修改 CI 配置文件

编辑 `.github/workflows/integration-test.yaml`，在 `on:` 部分添加 `workflow_dispatch:`

```yaml
on:
  pull_request:
    branches:
      - main
      - release-*
  push:
    branches:
      - main
      - release-*
  workflow_dispatch:  # 添加这一行，允许手动触发
```

#### 步骤 2: 提交并推送修改

```bash
git add .github/workflows/integration-test.yaml
git commit -m "ci: 允许手动触发集成测试"
git push
```

#### 步骤 3: 在 GitHub 上手动触发

1. 访问你的 GitHub 仓库
2. 点击 **Actions** 标签
3. 在左侧选择 **Integration Tests** 工作流
4. 点击右侧的 **Run workflow** 按钮
5. 选择分支（通常是 `main`）
6. 点击绿色的 **Run workflow** 按钮

---

## 📊 查看 CI 测试结果

### 在 Actions 页面查看

1. **访问 Actions 页面**

   ```
   https://github.com/你的用户名/halo/actions
   ```
2. **查看工作流运行列表**

   你会看到类似这样的列表：

   ```
   ✅ Integration Tests #3
      main branch • 5 minutes ago • Success

   🟡 Integration Tests #2
      main branch • 10 minutes ago • In progress

   ❌ Integration Tests #1
      main branch • 1 hour ago • Failed
   ```
3. **点击某个运行记录**

   查看详细信息：
   - 运行时间
   - 各个 Job 的状态
   - 日志输出

4. **查看具体的 Job**

   点击某个 Job（如 `unit-tests`）查看：
   - 每个步骤的执行情况
   - 详细的日志输出
   - 错误信息（如果有）

### 在 Pull Request 页面查看

如果是通过 PR 触发的，可以在 PR 页面底部看到：

```
✅ All checks have passed
   ✓ unit-tests (H2) — 5m 23s
   ✓ integration-tests-mysql — 9m 45s
   ✓ integration-tests-postgresql — 10m 12s
   ✓ code-quality — 3m 56s
```

点击 **Details** 可以查看详细日志。

---

## 🎨 CI 工作流说明

### 主工作流 (halo.yaml)

**触发条件**:
- Push 到 `main` 或 `release-*` 分支
- 创建或更新 PR
- 发布 Release

**执行内容**:
- 代码检查
- 编译构建
- 运行测试
- 上传覆盖率
- 构建 Docker 镜像（仅主分支）

### 集成测试工作流 (integration-test.yaml)

**触发条件**:
- Push 到 `main` 或 `release-*` 分支
- 创建或更新 PR
- 手动触发（如果配置了 `workflow_dispatch`）

**执行内容**:
- 单元测试（H2 数据库）
- 集成测试（MySQL 8.0）
- 集成测试（PostgreSQL 15）
- 代码质量检查
- 生成测试报告

---

## 📝 完整示例：从零开始触发 CI

### 场景：你刚创建了 GitHub 仓库

```bash
# 1. 克隆你的空仓库（或者初始化本地仓库）
git clone https://github.com/你的用户名/halo.git
cd halo

# 2. 如果是从现有代码开始，复制所有文件到这个目录
# （假设你已经有了所有代码和 CI 配置）

# 3. 查看状态
git status

# 4. 添加所有文件
git add .

# 5. 提交
git commit -m "feat: 初始提交 - 添加集成测试和 CI 配置"

# 6. 推送到 GitHub
git push -u origin main
```

### 推送后会发生什么？

1. **GitHub 检测到推送**
   - GitHub 收到你的推送
   - 检查 `.github/workflows/` 目录
   - 发现工作流配置文件
2. **触发工作流**
   - 根据 `on:` 配置判断是否触发
   - 创建工作流运行实例
   - 分配运行器（Ubuntu 虚拟机）
3. **执行测试**
   - 检出代码
   - 设置环境（JDK、Node.js 等）
   - 运行测试
   - 生成报告
4. **显示结果**
   - 在 Actions 页面显示状态
   - 发送通知（如果配置了）
   - 更新徽章（如果有）

---

## 🔍 查看测试报告

### 1. 在线查看（GitHub Actions）

**步骤**:
1. 进入 Actions 页面
2. 点击某个工作流运行
3. 点击 `test-summary` Job
4. 查看 Summary 部分

**你会看到**:

```markdown
# 🧪 测试报告摘要

## 测试结果

| 测试类型 | 状态 |
|---------|------|
| 单元测试 (H2) | ✅ 通过 |
| 集成测试 (MySQL) | ✅ 通过 |
| 集成测试 (PostgreSQL) | ✅ 通过 |
```

### 2. 下载测试报告

**步骤**:
1. 进入 Actions 页面
2. 点击某个工作流运行
3. 滚动到页面底部
4. 在 **Artifacts** 部分下载：
- `unit-test-reports`
- `mysql-integration-test-reports`
- `postgresql-integration-test-reports`

**解压后查看**:

```bash
# 解压下载的文件
unzip unit-test-reports.zip

# 打开 HTML 报告
open index.html  # Mac
start index.html  # Windows
xdg-open index.html  # Linux
```

### 3. 查看覆盖率（Codecov）

如果配置了 Codecov，访问：

```
https://codecov.io/gh/你的用户名/halo
```

---

## ⚠️ 常见问题

### Q1: 推送后没有触发 CI

**可能原因**:
1. 工作流配置文件路径错误
2. 分支名称不匹配
3. 工作流被禁用

**解决方案**:

```bash
# 检查文件路径
ls -la .github/workflows/

# 应该看到：
# integration-test.yaml
# halo.yaml

# 检查分支名称
git branch

# 检查工作流配置
cat .github/workflows/integration-test.yaml | grep "branches:"
```

### Q2: CI 运行失败

**查看失败原因**:
1. 进入 Actions 页面
2. 点击失败的运行
3. 点击红色 ❌ 的 Job
4. 查看错误日志

**常见错误**:
- 编译错误：检查代码语法
- 测试失败：查看测试日志
- 依赖问题：检查 `build.gradle`

### Q3: 如何跳过 CI

**方法 1: 在提交信息中添加标记**

```bash
git commit -m "docs: 更新文档 [skip ci]"
```

**方法 2: 使用 `[ci skip]`**

```bash
git commit -m "chore: 更新配置 [ci skip]"
```

### Q4: CI 运行太慢

**优化建议**:
1. 使用缓存（已配置）
2. 并行运行测试（已配置）
3. 只在必要时运行完整测试

**修改触发条件**:

```yaml
on:
  push:
    branches:
      - main
    paths:
      - 'application/**'  # 只在 application 目录变化时触发
      - '.github/workflows/**'
```

---

## 🎯 最佳实践

### 1. 提交前本地测试

```bash
# 在推送前先本地运行测试
./gradlew :application:test

# 确保测试通过后再推送
git push
```

### 2. 使用有意义的提交信息

```bash
# 好的提交信息
git commit -m "feat: 添加文章集成测试"
git commit -m "fix: 修复删除测试的断言错误"
git commit -m "ci: 优化测试工作流配置"

# 不好的提交信息
git commit -m "update"
git commit -m "fix bug"
git commit -m "test"
```

### 3. 小步提交

```bash
# 不要一次提交太多改动
git add application/src/test/java/run/halo/app/integration/PostFullStackIntegrationTest.java
git commit -m "feat: 添加文章完整流程集成测试"

git add .github/workflows/integration-test.yaml
git commit -m "ci: 添加集成测试工作流"

git push
```

### 4. 使用分支开发

```bash
# 创建功能分支
git checkout -b feature/my-feature

# 开发和测试
# ...

# 推送并创建 PR
git push -u origin feature/my-feature

# 在 GitHub 上创建 PR，触发 CI
```

---

## 📚 相关文档

- [CI 集成指南](./ci-integration-guide.md) - 详细的 CI 配置说明
- [集成测试指南](./integration-testing-guide.md) - 如何编写集成测试
- [快速入门指南](./integration-testing-quickstart.md) - 30分钟上手
- [GitHub Actions 文档](https://docs.github.com/en/actions) - 官方文档

---

## 🎉 总结

触发 CI 测试很简单：

1. **最简单**: 直接推送到 `main` 分支

   ```bash
   git push
   ```
2. **推荐方式**: 创建 PR

   ```bash
   git checkout -b feature/my-feature
   git push -u origin feature/my-feature
   # 然后在 GitHub 上创建 PR
   ```
3. **手动触发**: 在 Actions 页面点击 "Run workflow"

**查看结果**:
- 访问 `https://github.com/你的用户名/halo/actions`
- 点击运行记录查看详情
- 下载 Artifacts 查看完整报告

现在就去试试吧！🚀
