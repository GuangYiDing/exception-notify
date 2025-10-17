<p align="center">
  <img src="docs/assets/logo.png" alt="Exception-Notify logo" width="200">
</p>

[![Maven Central](https://img.shields.io/maven-central/v/com.nolimit35.springkit/exception-notify.svg)](https://search.maven.org/search?q=g:com.nolimit35.springkit%20AND%20a:exception-notify)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Deploy to Cloudflare Pages](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/GuangYiDing/exception-notify)

[English](README_EN.md) | [简体中文](README.md)

# Exception-Notify

> Spring Boot 应用的异常实时告警与 AI 排障工作台。

## 📚 目录

- [📖 简介](#-简介)
- [✨ 核心功能](#-核心功能)
- [🏗️ 架构概览](#-架构概览)
- [🚀 快速开始](#-快速开始)
- [⚙️ 配置总览](#-配置总览)
- [📢 通知渠道支持](#-通知渠道支持)
- [🧠 AI 分析工作台](#-ai-分析工作台)
- [📊 Monitor 工具](#-monitor-工具)
- [🧩 自定义扩展](#-自定义扩展)
- [❓ 常见问题](#-常见问题)
- [🔧 工作原理](#-工作原理)
- [⚠️ 注意事项](#-注意事项)
- [🤝 贡献指南](#-贡献指南)
- [📄 许可证](#-许可证)

## 📖 简介

Exception-Notify 是一个 Spring Boot Starter 组件，用于捕获应用中的未处理异常，并通过钉钉、飞书或企业微信进行即时告警。它自动解析异常堆栈、标记责任人、补充 TraceID 与云日志链接，把关键信息以统一模板推送到协作群。

除了后端告警之外，仓库还提供了一个基于 Next.js 的 Web 工作台，可解码压缩后的异常上下文，并与自选的 AI 模型进行对话式排障，帮助团队快速定位并修复问题。

## ✨ 核心功能

- **异常捕获与定位**
  - 基于 `@AfterThrowing` 自动捕获 Controller 层未处理异常
  - 精确定位源码文件与行号，支持包过滤确保聚焦业务模块
- **上下文丰富**
  - 集成 GitHub / GitLab / Gitee 的 Git Blame，自动获取代码提交者与最近提交时间
  - 支持 TraceID、腾讯云 CLS 链路、运行环境、代码上下文等附加信息
- **通知能力**
  - 内置钉钉、飞书、企业微信三大机器人，同时支持自定义通知渠道扩展
  - 异常去重、标题模板、自定义格式化等能力避免告警轰炸
- **AI 协作**
  - 生成 AI 分析链接，借助 Web 工作台查看异常详情并继续对话
  - 支持上下文采样、负载压缩、短链生成，兼顾易用性与安全性
- **可观测辅助**
  - 提供 `Monitor` 工具类，支持信息/警告/错误级别的即时通知
  - 自动从 MDC 或请求头中捕获 TraceID，补齐链路信息

## 🏗️ 架构概览

Exception-Notify 由以下模块组成：

1. **Starter 自动装配**：`ExceptionNotifyAutoConfiguration` 注册 AOP 切面、异常分析器、通知管理器等核心 Bean。
2. **异常分析器**：负责堆栈解析、包过滤、Git Blame、Trace 丰富以及异常去重判断。
3. **通知通道层**：通过 `NotificationProviderManager` 调度钉钉、飞书、企业微信或自定义渠道。
4. **AI 负载组件**：将异常数据压缩后请求 `/api/compress`，生成访问短链。
5. **Web 工作台（web/）**：基于 Next.js + React 的异常可视化与 AI 对话界面，可部署到 Cloudflare Workers、Vercel 等平台。

## 🚀 快速开始

### 1. 添加依赖

在 `pom.xml` 中引入依赖：

```xml
<dependency>
    <groupId>com.nolimit35.springkit</groupId>
    <artifactId>exception-notify</artifactId>
    <version>1.3.2-RELEASE</version>
</dependency>
```

### 2. 最小配置

在 `application.yml` 中完成基础配置（示例使用钉钉 + GitHub）：

```yaml
exception:
  notify:
    enabled: true
    dingtalk:
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
    github:
      token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
      repo-owner: your-github-username
      repo-name: your-repo-name
    environment:
      report-from: test,prod
spring:
  application:
    name: demo-service
  profiles:
    active: test
```

> **提示**：请根据实际需求选择钉钉 / 飞书 / 企业微信中的任意一种通知渠道，并配置对应的 GitHub / GitLab / Gitee 信息。更多属性详见下方「配置总览」章节或 `src/main/resources/application-example.yaml`。

### 3. 触发一次演示异常

创建一个示例 Controller 并故意抛出异常，验证告警是否送达：

```java
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/error")
    public String error() {
        throw new IllegalStateException("Demo exception from Exception-Notify");
    }
}
```

启动应用后访问 `GET /demo/error`，即可在群机器人中看到包含异常详情、责任人及 TraceID 的通知。

## ⚙️ 配置总览

下表列出了最常用的配置项：

| 分类             | 关键属性                                                                 | 说明                                                                                       |
| ---------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| 启用状态         | `exception.notify.enabled`                                                | 是否启用异常通知（默认 `true`）                                                            |
| 通知渠道         | `exception.notify.dingtalk/feishu/wechatwork.webhook`                     | 配置任意一个机器人 Webhook 即可生效                                                        |
| @提及映射        | `exception.notify.{channel}.at.enabled` & `userIdMappingGitEmail`         | 支持按 Git 提交邮箱自动 @ 责任人                                                           |
| Git 集成         | `exception.notify.github/gitlab/gitee.*`                                  | 选择单一平台获取 Git Blame 信息，三者互斥                                                  |
| Trace 配置       | `exception.notify.trace.enabled/header-name`                              | 启用链路追踪并指定请求头名称，默认读取 `X-Trace-Id` 或 MDC                                 |
| 去重策略         | `exception.notify.notification.deduplication.*`                           | 控制异常去重开关、时间窗口、清理周期                                                       |
| 运行环境过滤     | `exception.notify.environment.report-from`                                | 指定在哪些环境上报异常，默认 `test,prod`                                                   |
| AI 配置          | `exception.notify.ai.enabled/include-code-context/analysis-page-url`      | 生成 AI 分析链接并控制代码上下文采样                                                       |
| 包名过滤         | `exception.notify.package-filter.enabled/include-packages`                | 限定异常堆栈解析范围，更快定位业务代码                                                     |
| 通知模板         | `exception.notify.notification.title-template/include-stacktrace/...`     | 自定义标题、堆栈行数等模板内容                                                             |

<details>
<summary>查看更多配置示例</summary>

```yaml
exception:
  notify:
    enabled: true
    dingtalk:
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      at:
        enabled: true
        userIdMappingGitEmail:
          xxx: ['xxx@xx.com','xxxx@xx.com']
    feishu:
      webhook: https://open.feishu.cn/open-apis/bot/v2/hook/xxx
      at:
        enabled: true
        openIdMappingGitEmail:
          ou_xxxxxxxx: ['xxx@xx.com','xxxx@xx.com']
    wechatwork:
      webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
      at:
        enabled: true
        userIdMappingGitEmail:
          'xxx[@]xx.com': ['xxx@xx.com','xxxx@xx.com']
    github:
      token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
      repo-owner: your-github-username
      repo-name: your-repo-name
      branch: master
    gitlab:
      token: glpat-xxxxxxxxxxxxxxxxxxxx
      project-id: your-project-id-or-path
      base-url: https://gitlab.com/api/v4
      branch: master
    gitee:
      token: xxxxxxxxxxxxxxxxxxxxxxx
      repo-owner: your-gitee-username
      repo-name: your-repo-name
      branch: master
    tencentcls:
      region: ap-guangzhou
      topic-id: xxx-xxx-xxx
    trace:
      enabled: true
      header-name: X-Trace-Id
    ai:
      enabled: true
      include-code-context: true
      code-context-lines: 5
      analysis-page-url: https://fixit.nolimit35.com
    package-filter:
      enabled: false
      include-packages:
        - com.example.app
        - com.example.service
    notification:
      title-template: "【${appName}】异常告警"
      include-stacktrace: true
      max-stacktrace-lines: 10
      deduplication:
        enabled: true
        time-window-minutes: 3
        cleanup-interval-minutes: 60
    environment:
      report-from: test,prod

spring:
  application:
    name: YourApplicationName
  profiles:
    active: dev
```

</details>

更多配置场景可参考 [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md)。

## 📢 通知渠道支持

- **钉钉机器人**  
  配置 `exception.notify.dingtalk.webhook` 即可启用，可通过 `userIdMappingGitEmail` 实现按 Git 邮箱自动 @ 责任人。
- **飞书机器人**  
  使用 `openIdMappingGitEmail` 维护 Git 提交邮箱与飞书用户 OpenID 的映射，支持多邮箱指向同一用户。
- **企业微信机器人**  
  注意包含 `@` 的用户 ID 需写成 `[@]`，否则企业微信会拦截。其余配置与钉钉类似。
- **自定义通知渠道**  
  实现 `NotificationProvider` 或继承 `AbstractNotificationProvider` 即可扩展 Slack、短信等自定义通道。

## 🧠 AI 分析工作台

1. **功能特性**
   - Server-Sent Events 流式响应，实时展示 AI 回复
   - 完整 Markdown 渲染与代码高亮
   - 多轮对话、上下文编辑与一键复制
   - 暗色主题、LocalStorage 本地存储 API Key

2. **后端配置**

```yaml
exception:
  notify:
    ai:
      enabled: true
      include-code-context: true
      code-context-lines: 5
      analysis-page-url: https://your-workspace.pages.dev
```

3. **部署方式**
   - 一键部署到 Cloudflare Workers（仓库已内置 workflow）
   - 或者将 `web/` 目录构建后部署到 Vercel、Netlify 等平台

4. **本地开发**

```bash
cd web
npm install
npm run dev
```

5. **使用说明**
   - 后端生成压缩负载并请求 `/api/compress` 获取 16 位短码
   - Web 工作台从 URL 上的 `payload` 参数读取短码并解压
   - 用户可在页面中输入自定义模型与 API Key，默认仅存储在浏览器 LocalStorage

## 📊 Monitor 工具

`Monitor` 提供类 SLF4J 的调用体验，让你在业务代码中同时记录日志与推送通知：

```java
Monitor.info("用户注册成功完成");
Monitor.warn("支付处理延迟");
Monitor.error("数据库连接失败");
```

支持自定义 Logger：

```java
Logger logger = Monitor.getLogger(YourService.class);
Monitor.error(logger, "第三方服务调用失败", exception);
```

特点：

- 与告警通道共用配置，无需额外接入
- 自动携带 TraceID、CLS 链路与调用者位置信息
- 适用于数据库异常、第三方服务失败、重要状态变更等场景

## 🧩 自定义扩展

### 自定义异常过滤

```java
@Component
public class CustomExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        return !(throwable instanceof ResourceNotFoundException);
    }
}
```

### 自定义告警内容

```java
@Component
public class CustomNotificationFormatter implements NotificationFormatter {
    @Override
    public String format(ExceptionInfo exceptionInfo) {
        return "自定义告警内容";
    }
}
```

### 自定义链路追踪

```java
@Component
public class CustomTraceInfoProvider implements TraceInfoProvider {
    @Override
    public String getTraceId() {
        return "custom-trace-id";
    }

    @Override
    public String generateTraceUrl(String traceId) {
        return "https://your-log-system.com/trace?id=" + traceId;
    }
}
```

### 自定义通知渠道

```java
@Component
public class CustomNotificationProvider extends AbstractNotificationProvider {

    public CustomNotificationProvider(ExceptionNotifyProperties properties) {
        super(properties);
    }

    @Override
    protected boolean doSendNotification(ExceptionInfo exceptionInfo) {
        System.out.println("Sending notification for: " + exceptionInfo.getType());
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

## ❓ 常见问题

**Q: 已经抛出了异常，但群里没有收到通知？**  
A: 请确认 `exception.notify.enabled` 为 `true`，当前环境在 `environment.report-from` 列表中，并且至少配置了一个有效的机器人 Webhook。

**Q: Git 提交者信息为什么一直显示 Unknown？**  
A: 确认只配置了 GitHub / GitLab / Gitee 中的一种，并使用具有仓库读取权限的 Token。部署代码版本需要与配置的仓库分支保持一致。

**Q: AI 分析链接 404 或无响应？**  
A: 检查 `analysis-page-url` 是否指向可访问的工作台，并确保同域暴露 POST `/api/compress` 接口。更多排障建议见 [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md)。

## 🔧 工作原理

1. 通过 Spring AOP 的 `@AfterThrowing` 捕获未处理的异常
2. 解析堆栈信息，定位异常发生的源码文件与行号
3. 调用 Git 平台 Blame 接口获取责任人信息
4. 从 MDC 或请求头中提取 TraceID，并生成腾讯云 CLS 等链接
5. 将异常详情、责任人、TraceID、AI 链接等整合为消息体
6. 通过机器人 Webhook 推送至钉钉、飞书或企业微信

## ⚠️ 注意事项

- 需要确保应用能够访问 GitHub / GitLab / Gitee API
- 令牌需具备对应仓库的读取权限，注意妥善保管
- 钉钉 / 飞书 / 企业微信机器人需按照平台要求开启安全设置
- 异常去重基于「类型 + 消息 + 位置」判定，如需完全关闭可将 `deduplication.enabled` 置为 `false`

## 🤝 贡献指南

欢迎通过 Issue 或 Pull Request 来改进项目！在提交前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)，了解代码规范、提交规范与测试要求。

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE)。

[![Star History Chart](https://api.star-history.com/svg?repos=GuangYiDing/exception-notify&type=date&legend=top-left)](https://www.star-history.com/#GuangYiDing/exception-notify&type=date&legend=top-left)
