<p align="center">
  <img src="docs/assets/logo.png" alt="Exception-Notify logo" width="200">
</p>

[![Maven Central](https://img.shields.io/maven-central/v/com.nolimit35.springkit/exception-notify.svg)](https://search.maven.org/search?q=g:com.nolimit35.springkit%20AND%20a:exception-notify)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Deploy to Cloudflare Pages](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/GuangYiDing/exception-notify)

[English](README_EN.md) | [简体中文](README.md)

# Exception-Notify

> Real-time exception alerting and AI-powered troubleshooting workspace for Spring Boot applications.

## 📚 Table of Contents

- [📖 Introduction](#-introduction)
- [✨ Key Features](#-key-features)
- [🏗️ Architecture Overview](#-architecture-overview)
- [🚀 Quick Start](#-quick-start)
- [⚙️ Configuration Overview](#-configuration-overview)
- [📢 Notification Channels](#-notification-channels)
- [🧠 AI Analysis Workspace](#-ai-analysis-workspace)
- [📊 Monitor Utility](#-monitor-utility)
- [🧩 Custom Extensions](#-custom-extensions)
- [❓ FAQ](#-faq)
- [🔧 How It Works](#-how-it-works)
- [⚠️ Important Notes](#-important-notes)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

## 📖 Introduction

Exception-Notify is a Spring Boot Starter component that captures unhandled exceptions in your application and sends instant alerts through DingTalk, Feishu, or WeChat Work. It automatically analyzes exception stack traces, identifies responsible developers, enriches context with TraceID and cloud log links, then delivers comprehensive alerts to collaboration groups.

Beyond backend alerting, the repository includes a Next.js-based web workspace that decodes compressed exception payloads and enables conversational troubleshooting with your preferred AI model, helping teams quickly identify and resolve issues.

## ✨ Key Features

- **Exception Capture & Location**
  - Automatic capture of unhandled Controller exceptions via `@AfterThrowing`
  - Precise source file and line number identification with package filtering for business code focus
- **Context Enrichment**
  - Integration with GitHub / GitLab / Gitee Git Blame to retrieve committer information and timestamps
  - Support for TraceID, Tencent Cloud CLS links, runtime environment, code context, and more
- **Notification Capabilities**
  - Built-in support for DingTalk, Feishu, and WeChat Work robots with custom channel extensibility
  - Deduplication, title templates, and custom formatting to prevent alert fatigue
- **AI Collaboration**
  - Generate AI analysis links to view exception details and continue conversations in the web workspace
  - Context sampling, payload compression, and short link generation balancing usability and security
- **Observability Assistance**
  - `Monitor` utility class for info/warn/error level instant notifications
  - Automatic TraceID capture from MDC or request headers to complete trace context

## 🏗️ Architecture Overview

Exception-Notify consists of the following modules:

1. **Starter Auto-configuration**: `ExceptionNotifyAutoConfiguration` registers AOP aspects, exception analyzers, notification managers, and other core beans.
2. **Exception Analyzer**: Handles stack parsing, package filtering, Git Blame, trace enrichment, and deduplication checks.
3. **Notification Channel Layer**: `NotificationProviderManager` orchestrates DingTalk, Feishu, WeChat Work, or custom channels.
4. **AI Payload Component**: Compresses exception data and requests `/api/compress` to generate access tokens.
5. **Web Workspace (web/)**: Next.js + React exception visualization and AI conversation interface, deployable to Cloudflare Workers, Vercel, and other platforms.

## 🚀 Quick Start

### 1. Add Dependency

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.nolimit35.springkit</groupId>
    <artifactId>exception-notify</artifactId>
    <version>1.3.2-RELEASE</version>
</dependency>
```

### 2. Minimal Configuration

Complete basic configuration in `application.yml` (example using DingTalk + GitHub):

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

> **Tip**: Choose one notification channel (DingTalk / Feishu / WeChat Work) and configure the corresponding GitHub / GitLab / Gitee information. See "Configuration Overview" below or `src/main/resources/application-example.yaml` for more options.

### 3. Trigger a Test Exception

Create a sample Controller that deliberately throws an exception to verify alert delivery:

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

After starting the application, visit `GET /demo/error` to see a notification in your robot group with exception details, responsible person, and TraceID.

## ⚙️ Configuration Overview

The table below lists the most commonly used configuration options:

| Category              | Key Properties                                                          | Description                                                                                |
| --------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Enable State          | `exception.notify.enabled`                                              | Whether to enable exception notifications (default `true`)                                 |
| Notification Channels | `exception.notify.dingtalk/feishu/wechatwork.webhook`                   | Configure any robot webhook to activate                                                    |
| @Mention Mapping      | `exception.notify.{channel}.at.enabled` & `userIdMappingGitEmail`       | Auto @mention responsible persons based on Git commit email                                |
| Git Integration       | `exception.notify.github/gitlab/gitee.*`                                | Choose single platform for Git Blame info, mutually exclusive                              |
| Trace Config          | `exception.notify.trace.enabled/header-name`                            | Enable trace linking and specify header name, defaults to `X-Trace-Id` or MDC              |
| Deduplication         | `exception.notify.notification.deduplication.*`                         | Control deduplication switch, time window, cleanup interval                                |
| Environment Filter    | `exception.notify.environment.report-from`                              | Specify which environments report exceptions, defaults to `test,prod`                      |
| AI Config             | `exception.notify.ai.enabled/include-code-context/analysis-page-url`    | Generate AI analysis links and control code context sampling                               |
| Package Filter        | `exception.notify.package-filter.enabled/include-packages`              | Limit stack parsing scope to focus on business code faster                                 |
| Notification Template | `exception.notify.notification.title-template/include-stacktrace/...`   | Customize title, stack line count, and other template content                              |

<details>
<summary>View Full Configuration Example</summary>

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
      title-template: "【${appName}】Exception Alert"
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

For more configuration scenarios, refer to [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md).

## 📢 Notification Channels

- **DingTalk Robot**  
  Configure `exception.notify.dingtalk.webhook` to enable. Use `userIdMappingGitEmail` to auto @mention based on Git email.
- **Feishu Robot**  
  Use `openIdMappingGitEmail` to maintain mapping between Git commit emails and Feishu user OpenIDs. Supports multiple emails per user.
- **WeChat Work Robot**  
  Note that user IDs containing `@` should be written as `[@]`, otherwise WeChat Work will block them. Other configuration is similar to DingTalk.
- **Custom Notification Channels**  
  Implement `NotificationProvider` or extend `AbstractNotificationProvider` to add Slack, SMS, or other custom channels.

## 🧠 AI Analysis Workspace

1. **Features**
   - Server-Sent Events streaming responses for real-time AI replies
   - Complete Markdown rendering and code highlighting
   - Multi-turn conversations, context editing, and one-click copy
   - Dark theme, LocalStorage for API key storage

2. **Backend Configuration**

```yaml
exception:
  notify:
    ai:
      enabled: true
      include-code-context: true
      code-context-lines: 5
      analysis-page-url: https://your-workspace.pages.dev
```

3. **Deployment Options**
   - One-click deploy to Cloudflare Workers (built-in workflow)
   - Or build the `web/` directory and deploy to Vercel, Netlify, etc.

4. **Local Development**

```bash
cd web
npm install
npm run dev
```

5. **Usage**
   - Backend generates compressed payload and requests `/api/compress` for 16-character token
   - Web workspace reads token from URL `payload` parameter and decompresses
   - Users can enter custom model and API Key in the page, stored only in browser LocalStorage by default

## 📊 Monitor Utility

`Monitor` provides an SLF4J-like calling experience, allowing you to log and push notifications simultaneously in business code:

```java
Monitor.info("User registration completed successfully");
Monitor.warn("Payment processing delayed");
Monitor.error("Database connection failed");
```

Supports custom Logger:

```java
Logger logger = Monitor.getLogger(YourService.class);
Monitor.error(logger, "Third-party service call failed", exception);
```

Features:

- Shares configuration with alert channels, no additional setup needed
- Automatically includes TraceID, CLS links, and caller location info
- Suitable for database exceptions, third-party service failures, important state changes, etc.

## 🧩 Custom Extensions

### Custom Exception Filter

```java
@Component
public class CustomExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        return !(throwable instanceof ResourceNotFoundException);
    }
}
```

### Custom Alert Content

```java
@Component
public class CustomNotificationFormatter implements NotificationFormatter {
    @Override
    public String format(ExceptionInfo exceptionInfo) {
        return "Custom alert content";
    }
}
```

### Custom Trace Information

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

### Custom Notification Channel

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

## ❓ FAQ

**Q: Exception occurred but no notification received in group?**  
A: Verify that `exception.notify.enabled` is `true`, current environment is in `environment.report-from` list, and at least one valid robot webhook is configured.

**Q: Why does Git committer info always show Unknown?**  
A: Confirm only one of GitHub / GitLab / Gitee is configured, using a token with repository read permissions. Deployed code version must match configured repository branch.

**Q: AI analysis link returns 404 or no response?**  
A: Check if `analysis-page-url` points to an accessible workspace and ensure POST `/api/compress` endpoint is exposed on the same domain. See [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) for more troubleshooting tips.

## 🔧 How It Works

1. Capture unhandled exceptions via Spring AOP's `@AfterThrowing`
2. Parse stack information to locate source file and line number
3. Call Git platform Blame API to retrieve committer information
4. Extract TraceID from MDC or request headers, generate Tencent Cloud CLS links
5. Integrate exception details, committer, TraceID, AI links into message body
6. Push to DingTalk, Feishu, or WeChat Work via robot webhook

## ⚠️ Important Notes

- Ensure application can access GitHub / GitLab / Gitee APIs
- Tokens must have read permissions for corresponding repositories, protect them carefully
- DingTalk / Feishu / WeChat Work robots must enable security settings per platform requirements
- Exception deduplication is based on "type + message + location", set `deduplication.enabled` to `false` to disable completely

## 🤝 Contributing

Welcome to improve the project through Issues or Pull Requests! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting to understand code standards, commit conventions, and testing requirements.

## 📄 License

This project is licensed under [Apache License 2.0](LICENSE).

[![Star History Chart](https://api.star-history.com/svg?repos=GuangYiDing/exception-notify&type=date&legend=top-left)](https://www.star-history.com/#GuangYiDing/exception-notify&type=date&legend=top-left)
