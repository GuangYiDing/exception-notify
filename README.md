# Exception-Notify

[![Maven Central](https://img.shields.io/maven-central/v/com.nolimit35.springkit/exception-notify.svg)](https://search.maven.org/search?q=g:com.nolimit35.springkit%20AND%20a:exception-notify)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[English](README_EN.md) | [简体中文](README.md)

## 📖 简介

Exception-Notify 是一个 Spring Boot Starter 组件，用于捕获 Spring Boot 应用中未处理的异常，并通过钉钉、飞书或企业微信实时告警通知。它能够自动分析异常堆栈信息，定位到异常发生的源代码文件和行号，并通过 GitHub、GitLab 或 Gitee API 获取代码提交者信息，最终将异常详情、TraceID 以及责任人信息发送到钉钉群、飞书群或企业微信群，实现异常的实时上报与全链路追踪。

## ✨ 功能特点

- 🎯 基于 @AfterThrowing 自动捕获 Spring Boot 应用中未处理的异常
- 🔍 分析异常堆栈，精确定位异常源码位置（文件名和行号）
- 👤 通过 GitHub API、GitLab API 或 Gitee API 的 Git Blame 功能获取代码提交者信息
- 🤖 支持生成 AI 分析链接，结合外部工作台快捷获取修复建议
- 🔗 支持与分布式链路追踪系统集成，关联 TraceID
- 📢 支持通过钉钉机器人、飞书机器人和企业微信机器人实时推送异常告警
- ☁️ 支持腾讯云日志服务(CLS)的链路追踪
- 🛡️ 支持异常去重功能，避免短时间内相同异常重复告警
- 💡 零侵入式设计，仅需添加依赖和简单配置即可使用
- 🎨 支持自定义告警模板和告警规则

## 🚀 快速开始

### 1️⃣ 添加依赖

在你的 Spring Boot 项目的 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>com.nolimit35.springkit</groupId>
    <artifactId>exception-notify</artifactId>
    <version>1.3.1-RELEASE</version>
</dependency>
```

### 2️⃣ 配置参数

在 `application.yml` 或 `application.properties` 中添加以下配置：

```yaml
exception:
  notify:
    enabled: true                                # 是否启用异常通知功能
    dingtalk:
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx  # 钉钉机器人 Webhook 地址
      at:
        enabled: true                                                 # 是否启用@功能
        userIdMappingGitEmail:                                        # 钉钉 用户id 与 git 提交邮箱的映射关系
          xxx: ['xxx@xx.com','xxxx@xx.com']
    feishu:
      webhook: https://open.feishu.cn/open-apis/bot/v2/hook/xxx       # 飞书机器人 Webhook 地址
      at:
        enabled: true                                                 # 是否启用@功能
        openIdMappingGitEmail:                                        # 飞书 openid 与 git 提交邮箱的映射关系
          ou_xxxxxxxx: ['xxx@xx.com','xxxx@xx.com']
    wechatwork:
      webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx  # 企业微信机器人 Webhook 地址
      at:
        enabled: true                                                    # 是否启用@功能
        userIdMappingGitEmail:                                           # 企微 用户id 与 git 提交邮箱的映射关系
          # 企微用户 id 带有 @ 符号时,需要手动特殊处理成 [@]
          'xxx[@]xx.com': ['xxx@xx.com','xxxx@xx.com']
    # GitHub 配置 (与 GitLab、Gitee 配置互斥，只能选择其中一种)
    github:
      token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx                # GitHub 访问令牌
      repo-owner: your-github-username                               # GitHub 仓库所有者
      repo-name: your-repo-name                                      # GitHub 仓库名称
      branch: master                                                 # GitHub 仓库分支
    # GitLab 配置 (与 GitHub、Gitee 配置互斥，只能选择其中一种)
    gitlab:
      token: glpat-xxxxxxxxxxxxxxxxxxxx                              # GitLab 访问令牌
      project-id: your-project-id-or-path                            # GitLab 项目 ID 或路径
      base-url: https://gitlab.com/api/v4                            # GitLab API 基础 URL
      branch: master                                                 # GitLab 仓库分支
    # Gitee 配置 (与 GitHub、GitLab 配置互斥，只能选择其中一种)
    gitee:
      token: xxxxxxxxxxxxxxxxxxxxxxx                                 # Gitee 访问令牌
      repo-owner: your-gitee-username                                # Gitee 仓库所有者
      repo-name: your-repo-name                                      # Gitee 仓库名称
      branch: master                                                 # Gitee 仓库分支
    tencentcls:
      region: ap-guangzhou                                           # 腾讯云日志服务(CLS)的地域
      topic-id: xxx-xxx-xxx                                          # 腾讯云日志服务(CLS)的主题ID
    trace:
      enabled: true                                                  # 是否启用链路追踪
      header-name: X-Trace-Id                                        # 链路追踪 ID 的请求头名称
    ai:
      enabled: true                                                  # 是否启用 AI 分析链接
      include-code-context: true                                     # 是否采集代码上下文
      code-context-lines: 5                                          # 代码上下文行数
      analysis-page-url: https://fixit.nolimit35.com                 # AI 分析工作台地址
    package-filter:
      enabled: false                                                 # 是否启用包名过滤
      include-packages:                                              # 需要解析的包名列表
        - com.example.app
        - com.example.service
    notification:
      title-template: "【${appName}】异常告警"                        # 告警标题模板
      include-stacktrace: true                                       # 是否包含完整堆栈信息
      max-stacktrace-lines: 10                                       # 堆栈信息最大行数
      deduplication:
        enabled: true                                                # 是否启用异常去重功能
        time-window-minutes: 3                                       # 去重时间窗口（分钟），默认3分钟内相同异常只通知一次
        cleanup-interval-minutes: 60                                 # 缓存清理周期（分钟），默认60分钟清理一次过期缓存
    environment:
      report-from: test,prod                                         # 需要上报异常的环境列表，多个环境用逗号分隔

# Spring 配置
spring:
  # 应用名称，用于告警标题
  application:
    name: YourApplicationName
  # 当前环境配置，会自动用于确定异常通知的当前环境
  profiles:
    active: dev                                                      # 当前激活的环境配置
```

> **注意**：当前环境会自动从 Spring 的 `spring.profiles.active` 属性中读取，无需手动设置。只有在 `exception.notify.environment.report-from` 列表中的环境才会上报异常，默认只上报 test 和 prod 环境的异常。

### 3️⃣ 启动应用

启动你的 Spring Boot 应用，Exception-Notify 将自动注册全局异常处理器，捕获所有被 @Controller 或 @RestController 或 @ExceptionNotify 标记的类中未处理的异常并发送告警。

## 📮 告警示例

当应用发生未处理的异常时，钉钉群或企业微信群将收到类似以下格式的告警消息：

```
【服务名称】异常告警
-------------------------------
异常时间：2023-03-17 14:30:45
异常类型：java.lang.NullPointerException
异常描述：Cannot invoke "String.length()" because "str" is null
异常位置：com.example.service.UserService.processData(UserService.java:42)
当前环境：prod
代码提交者：张三 (zhangsan@example.com)
最后提交时间：2023-03-15 10:23:18
TraceID：7b2d1e8f9c3a5b4d6e8f9c3a5b4d6e8f
云日志链路：https://console.cloud.tencent.com/cls/search?region=ap-guangzhou&topic_id=xxx-xxx-xxx&interactiveQueryBase64=xxxx
-------------------------------
### AI 分析：

[点击 AI 分析](https://fixit.nolimit35.com/analysis?payload=xxxxxx)

（链接会携带压缩后的异常上下文，可在工作台查看详情并继续与 AI 对话）

-------------------------------
堆栈信息：
java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
    at com.example.service.UserService.processData(UserService.java:42)
    at com.example.controller.UserController.getData(UserController.java:28)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    ...
处理人: @张三
```

> **注意**：AI 分析链接需要启用 `exception.notify.ai.enabled`，并配置可访问的 `analysis-page-url`。

## ⚙️ 高级配置

### 🧠 AI 分析工作台

组件会将异常信息、代码上下文等内容压缩为 Base64URL + GZIP 字符串，并拼接到 `analysis-page-url` 的查询参数中。仓库根目录提供了一个基于 Vite + React 的示例 Web 项目（`web/`），用于在浏览器侧解析数据并与 AI 服务交互。

#### 🎨 工作台特性

- 📡 **流式响应**：采用 Server-Sent Events (SSE) 流式展示 AI 回答，实时显示生成内容
- 📝 **Markdown 渲染**：完整支持 Markdown 语法，包括标题、列表、代码块等
- 🎨 **代码高亮**：基于 highlight.js 的语法高亮，支持多种编程语言
- 💬 **对话式交互**：支持多轮对话，可针对异常继续追问和深入分析
- 🔄 **重新生成**：对 AI 回答不满意？一键重新生成获得新答案
- 📋 **一键复制**：快速复制消息内容和代码片段
- ✏️ **上下文编辑**：支持在线编辑代码上下文、堆栈信息和补充说明
- 🌙 **暗色主题**：护眼的暗色界面设计
- 🔐 **本地存储**：API Key 仅保存在浏览器 LocalStorage，保护隐私安全

#### 🚀 快速部署

**一键部署到 Vercel：**

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https://github.com/GuangYiDing/exception-notify&project-name=exception-notify-workspace&repository-name=exception-notify&root-directory=web)

**一键部署到 Cloudflare Pages：**

[![Deploy to Cloudflare Pages](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/GuangYiDing/exception-notify)


**通过 GitHub Actions 自动部署：**

仓库已配置 Cloudflare Pages 自动部署 workflow，只需：

1. 在 GitHub 仓库设置中添加以下 Secrets：
   - `CLOUDFLARE_API_TOKEN`：Cloudflare API Token
   - `CLOUDFLARE_ACCOUNT_ID`：Cloudflare Account ID

2. 推送代码到 main 分支或修改 `web/` 目录下的文件，将自动触发部署

#### 📦 本地开发

1. 安装依赖并启动本地调试：
   ```bash
   cd web
   npm install
   npm run dev
   ```

2. 生产环境构建：
   ```bash
   npm run build
   ```

3. 预览生产构建：
   ```bash
   npm run preview
   ```

#### ⚙️ 配置说明

1. 将 `analysis-page-url` 指向部署后的地址（例如 `https://your-workspace.vercel.app` 或 `https://your-workspace.pages.dev`）
2. Web 工作台会提示用户在本地浏览器输入 API Key 与模型信息，所有敏感配置仅保存在浏览器 LocalStorage 中
3. 可根据需要替换为企业内部的中转服务

#### 🔧 自定义部署

部署到其他平台（如 Netlify、GitHub Pages 等）：

```bash
cd web
npm install
npm run build
# 将 dist 目录部署到你的静态托管服务
```


### 🌍 环境配置

你可以通过配置 `exception.notify.environment.report-from` 属性来指定哪些环境需要上报异常：

```yaml
exception:
  notify:
    environment:
      report-from: dev,test,prod  # 在开发、测试和生产环境都上报异常
```

默认情况下，组件只会在 test 和 prod 环境上报异常，而在 dev 环境不上报。当前环境会自动从 Spring 的 `spring.profiles.active` 属性中读取。

### 🛡️ 异常去重配置

为了避免短时间内相同异常重复告警，Exception-Notify 提供了异常去重功能。你可以通过以下配置来启用和自定义去重策略：

```yaml
exception:
  notify:
    notification:
      deduplication:
        enabled: true                            # 是否启用异常去重功能（默认：true）
        time-window-minutes: 3                   # 去重时间窗口（分钟），默认3分钟内相同异常只通知一次
        cleanup-interval-minutes: 60             # 缓存清理周期（分钟），默认60分钟清理一次过期缓存
```

配置说明：

- **enabled**: 是否启用异常去重功能，默认为 `true`。设置为 `false` 可以关闭去重功能
- **time-window-minutes**: 去重时间窗口，单位为分钟。在该时间窗口内，相同的异常只会通知一次。默认值为 3 分钟
- **cleanup-interval-minutes**: 缓存清理周期，单位为分钟。系统会定期清理过期的缓存数据，避免内存占用。默认值为 60 分钟（1 小时）

**去重机制说明**：

1. 异常唯一性判断基于：异常类型、异常消息和异常位置（文件名和行号）
2. 当相同异常在时间窗口内再次发生时，会被过滤掉，不会重复发送通知
3. 超过时间窗口后，相同的异常会重新触发通知
4. 系统会自动清理过期的缓存数据，避免内存泄漏

**使用场景**：

- 高并发场景下，同一个问题可能在短时间内触发大量异常，避免告警轰炸
- 定时任务失败时，避免每次执行都发送重复告警

### 📦 包名过滤配置

你可以通过配置 `exception.notify.package-filter` 来控制异常堆栈分析时只关注特定包名下的代码：

```yaml
exception:
  notify:
    package-filter:
      enabled: true                              # 启用包名过滤功能
      include-packages:                          # 需要解析的包名列表
        - com.example.app
        - com.example.service
```

当启用包名过滤功能后，异常分析器会优先从指定的包名列表中寻找异常堆栈信息，这对于定位业务代码中的问题特别有用。如果没有找到匹配的堆栈信息，会使用原始的过滤逻辑。

### ☁️ 腾讯云日志服务(CLS)集成

如果你使用了腾讯云日志服务(CLS)，可以配置相关参数来在异常告警中添加云日志链路：

```yaml
exception:
  notify:
    tencentcls:
      region: ap-guangzhou             # 腾讯云日志服务(CLS)的地域
      topic-id: xxx-xxx-xxx            # 腾讯云日志服务(CLS)的主题ID
    trace:
      enabled: true                    # 启用链路追踪
```

当同时配置了 CLS 参数和链路追踪，异常告警消息中会包含指向云日志的链接，方便快速查看完整的日志上下文。

### 👤 代码提交者信息集成

Exception-Notify 支持通过 GitHub API、GitLab API 或 Gitee API 获取代码提交者信息。你需要选择其中一种方式进行配置，不能同时配置多者：

```yaml
exception:
  notify:
    # 使用 GitHub API 获取代码提交者信息
    github:
      token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # GitHub 访问令牌
      repo-owner: your-github-username                 # GitHub 仓库所有者
      repo-name: your-repo-name                        # GitHub 仓库名称
      branch: master                                   # GitHub 仓库分支
```

或者：

```yaml
exception:
  notify:
    # 使用 GitLab API 获取代码提交者信息
    gitlab:
      token: glpat-xxxxxxxxxxxxxxxxxxxx                # GitLab 访问令牌
      project-id: your-project-id-or-path              # GitLab 项目 ID 或路径
      base-url: https://gitlab.com/api/v4              # GitLab API 基础 URL
      branch: master                                   # GitLab 仓库分支
```

或者：

```yaml
exception:
  notify:
    # 使用 Gitee API 获取代码提交者信息
    gitee:
      token: xxxxxxxxxxxxxxxxxxxxxxx                   # Gitee 访问令牌
      repo-owner: your-gitee-username                  # Gitee 仓库所有者
      repo-name: your-repo-name                        # Gitee 仓库名称
      branch: master                                   # Gitee 仓库分支
```

> **注意**：GitHub、GitLab 和 Gitee 配置是互斥的，系统只能从一个代码托管平台读取提交信息。如果同时配置了多个，将按照 Gitee、GitLab、GitHub 的优先顺序选择。


### 📣 通知@功能配置

异常通知支持在钉钉、飞书或企业微信群中@相关责任人，以便更快地引起注意。你可以通过以下配置来启用和自定义@功能：

```yaml
exception:
  notify:
    dingtalk:
      at:
        enabled: true                                                 # 是否启用@功能
        userIdMappingGitEmail:                                        # 钉钉 用户id 与 git 提交邮箱的映射关系
          xxx: ['xxx@xx.com','xxxx@xx.com']
    feishu:
      at:
        enabled: true                                                 # 是否启用@功能
        openIdMappingGitEmail:                                        # 飞书 openid 与 git 提交邮箱的映射关系
          ou_xxxxxxxx: ['xxx@xx.com','xxxx@xx.com']
    wechatwork:
      at:
        enabled: true                                                 # 是否启用@功能
        userIdMappingGitEmail:                                        # 企微 用户id 与 git 提交邮箱的映射关系
          # 企微用户 id 带有 @ 符号时,需要手动特殊处理成 [@]
          'xxx[@]xx.com': ['xxx@xx.com','xxxx@xx.com']
```

配置说明：

1. **钉钉@功能**：
    - `enabled`: 是否启用钉钉@功能
    - `userIdMappingGitEmail`: 钉钉用户ID与Git提交邮箱的映射关系，支持一个用户ID对应多个Git邮箱

2. **飞书@功能**：
    - `enabled`: 是否启用飞书@功能
    - `openIdMappingGitEmail`: 飞书用户openID与Git提交邮箱的映射关系，支持一个openID对应多个Git邮箱

3. **企业微信@功能**：
    - `enabled`: 是否启用企业微信@功能
    - `userIdMappingGitEmail`: 企业微信用户ID与Git提交邮箱的映射关系，支持一个用户ID对应多个Git邮箱
    - 注意：企业微信用户ID如果包含`@`符号，需要特殊处理成`[@]`

启用@功能后，当异常发生时，系统会根据Git提交信息找到对应的责任人，并在告警消息中@相关人员，提高异常处理的及时性和准确性。

### 🤖 AI 分析链接配置

Exception-Notify 会将堆栈信息、代码上下文、Trace ID、提交人等数据压缩后拼接到指定的 AI 分析页面。通过自建的 Web 工作台（仓库自带示例项目 `web/`），即可在浏览器端解压并与 AI 服务进行对话。

```yaml
exception:
  notify:
    ai:
      enabled: true                                          # 是否启用 AI 分析链接
      include-code-context: true                             # 是否采集异常位置的代码上下文
      code-context-lines: 5                                  # 异常行上下文的前后行数
      analysis-page-url: https://fixit.nolimit35.com         # AI 工作台地址
```

**配置说明**：

1. **enabled**：开启后，通知中会携带 AI 分析链接并生成压缩负载。
2. **include-code-context**：当成功获取代码上下文时一并压缩进负载。
3. **code-context-lines**：控制采集的上下文行数。
4. **analysis-page-url**：指向已部署的分析工作台页面（可自建或使用示例项目）。

**注意事项**：

- 后端不再直接调用外部 AI 接口，所有对话在工作台中进行。
- 负载采用 Base64URL + GZIP 编码，示例工作台提供了解码实现，可按需扩展。
- 用户在工作台中输入的 API Key 默认只保存在浏览器 LocalStorage，如需集中管理可改为企业内代理服务。
- 若配置不完整（例如缺少工作台地址），通知会自动省略 AI 分析链接，但不会影响其他信息的发送。


**工作台特性**：

- 📡 **流式响应**：AI 回答采用 Server-Sent Events (SSE) 流式展示，实时显示生成内容
- 📝 **Markdown 渲染**：支持完整的 Markdown 语法，包括标题、列表、代码块等
- 🎨 **代码高亮**：基于 highlight.js 的语法高亮，支持多种编程语言
- 💬 **对话式交互**：支持多轮对话，可针对异常继续追问和深入分析
### 🔧 自定义异常过滤

你可以通过实现 `ExceptionFilter` 接口并注册为 Spring Bean 来自定义哪些异常需要告警：

```java
@Component
public class CustomExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        // 忽略特定类型的异常
        if (throwable instanceof ResourceNotFoundException) {
            return false;
        }
        return true;
    }
}
```

### 🎨 自定义告警内容

通过实现 `NotificationFormatter` 接口并注册为 Spring Bean 来自定义告警内容格式：

```java
@Component
public class CustomNotificationFormatter implements NotificationFormatter {
    @Override
    public String format(ExceptionInfo exceptionInfo) {
        // 自定义告警内容格式
        return "自定义告警内容";
    }
}
```

### 🔗 自定义链路追踪

你可以通过实现 `TraceInfoProvider` 接口并注册为 Spring Bean 来自定义如何获取 TraceID 和生成链路追踪 URL：

```java
@Component
public class CustomTraceInfoProvider implements TraceInfoProvider {
    @Override
    public String getTraceId() {
        // 自定义获取 TraceID 的逻辑
        return "custom-trace-id";
    }

    @Override
    public String generateTraceUrl(String traceId) {
        // 自定义生成链路追踪 URL 的逻辑
        return "https://your-log-system.com/trace?id=" + traceId;
    }
}
```

默认实现 `DefaultTraceInfoProvider` 会从 MDC 或请求头中获取 TraceID，并生成腾讯云日志服务(CLS)的链路追踪 URL。

### 📱 自定义通知渠道

您可以通过实现 `NotificationProvider` 接口来添加自定义通知渠道：

```java
@Component
public class CustomNotificationProvider implements NotificationProvider {
    @Override
    public boolean sendNotification(ExceptionInfo exceptionInfo) {
        // 实现自定义通知渠道的发送逻辑
        // exceptionInfo 包含了异常的所有相关信息，如：类型、消息、堆栈跟踪、环境、代码提交者等
        System.out.println("发送通知: " + exceptionInfo.getType());
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 决定此通知渠道是否启用
        return true;
    }
}
```

或者使用更推荐的方式继承 `AbstractNotificationProvider` 抽象类:

```java
@Component
public class CustomNotificationProvider extends AbstractNotificationProvider {

    public CustomNotificationProvider(ExceptionNotifyProperties properties) {
        super(properties);
    }

    @Override
    protected boolean doSendNotification(ExceptionInfo exceptionInfo) throws Exception {
        // Implement actual notification sending logic
        // exceptionInfo contains all related information about the exception
        // such as: type, message, stacktrace, environment, code committer, etc.
        System.out.println("Sending notification for: " + exceptionInfo.getType());
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Determine if this notification channel is enabled
        return true;
    }
}
```

## 📊 Monitor 工具类

Monitor 是一个简单易用的工具类，可以在记录日志的同时，将消息通过 Exception-Notify 配置的通知渠道（如钉钉、飞书或企业微信）发送出去。

### 特点

- 类似 SLF4J 的简单 API，与现有日志系统集成
- 自动通过配置的通知渠道发送消息
- 支持多种日志级别（info、warn、error）
- 支持带异常和不带异常的消息推送
- 支持自定义 Logger 实例
- 自动从 MDC 或请求头中捕获 TraceID（当链路追踪功能启用时）
- 生成腾讯云日志服务(CLS)的可点击链接，便于日志追踪
- 包含调用者位置信息，便于更好的调试

### 使用方法

#### 基本用法

直接调用静态方法记录日志并发送通知：

```java
// 信息级别通知
Monitor.info("用户注册成功完成");

// 警告级别通知
Monitor.warn("支付处理延迟");

// 错误级别通知
Monitor.error("数据库连接失败");

// 带异常的通知
try {
    // 业务逻辑
} catch (Exception e) {
    Monitor.info("订单接收存在问题", e);
    Monitor.warn("订单处理部分失败", e);
    Monitor.error("订单处理失败", e);
}
```

#### 使用自定义 Logger

您可以通过 `Monitor.getLogger()` 方法获取一个 SLF4J Logger 实例，然后使用该实例进行日志记录：

```java
// 获取 Logger
Logger logger = Monitor.getLogger(YourService.class);

// 使用自定义 Logger 发送通知
Monitor.info(logger, "支付处理开始");
Monitor.warn(logger, "支付处理延迟");
Monitor.error(logger, "支付处理失败");

// 带异常的通知
Monitor.info(logger, "第三方服务返回警告信息", exception);
Monitor.warn(logger, "第三方服务返回错误信息", exception);
Monitor.error(logger, "第三方服务调用失败", exception);
```

### 配置

Monitor 工具类使用与 Exception-Notify 相同的配置，无需额外配置。只要已经在 `application.yml` 或 `application.properties` 中配置了 Exception-Notify 组件，Monitor 就会自动使用这些配置。

### 常见使用场景

#### 数据库操作失败

```java
try {
    repository.save(entity);
} catch (DataAccessException e) {
    Monitor.error("保存实体失败，ID: " + entity.getId(), e);
}
```

#### 第三方服务调用失败

```java
try {
    String response = thirdPartyApiClient.call();
    if (response == null || response.isEmpty()) {
        Monitor.error("第三方 API 返回空响应");
    }
} catch (Exception e) {
    Monitor.error("第三方 API 调用失败", e);
}
```

#### 业务规则违反

```java
if (withdrawAmount > dailyLimit) {
    Monitor.error("业务规则违反: 尝试提取 " + withdrawAmount +
                  " 超过每日限额 " + dailyLimit);
    throw new BusinessRuleException("提款金额超过每日限额");
}
```

#### 重要业务流程状态变更

```java
Monitor.error("订单 #12345 状态从 PENDING 变更为 FAILED");
```

### 注意事项

- Monitor 主要用于需要即时通知的重要错误和业务事件
- 避免过度使用，以免通知渠道被大量消息淹没
- 通知仅在配置的环境中发送（通常是 test 和 prod 环境）
- 当链路追踪功能启用时，TraceID 会自动从 MDC 或请求头中获取
- 如果配置了腾讯云日志服务(CLS)，通知中将包含可点击的日志链接

## 🔧 工作原理

1. 通过 Spring AOP 的 `@AfterThrowing` 注解机制捕获未处理的异常
2. 分析异常堆栈信息，提取出异常发生的源代码文件和行号
3. 调用 GitHub API、GitLab API 或 Gitee API 的 Git Blame 接口，获取对应代码行的提交者信息
4. 从当前请求上下文中提取 TraceID（如果启用了链路追踪）
5. 将异常信息、代码提交者信息和 TraceID 组装成告警消息
6. 通过钉钉机器人或企业微信机器人 Webhook 接口发送告警消息到指定群组

## ⚠️ 注意事项

- 需要确保应用有访问 GitHub API、GitLab API 或 Gitee API 的网络权限
- GitHub Token、GitLab Token 或 Gitee Token 需要有仓库的读取权限
- 钉钉机器人和企业微信机器人需要正确配置安全设置
- 为了获取准确的代码提交者信息，确保代码仓库与实际部署的代码版本一致

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目。

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。
