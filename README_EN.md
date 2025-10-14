# Exception-Notify

[![Maven Central](https://img.shields.io/maven-central/v/com.nolimit35.springkit/exception-notify.svg)](https://search.maven.org/search?q=g:com.nolimit35.springkit%20AND%20a:exception-notify)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[English](README_EN.md) | [简体中文](README.md)

## 📖 Introduction

Exception-Notify is a Spring Boot Starter component designed to capture unhandled exceptions in Spring Boot applications and send real-time alerts through DingTalk, Feishu or WeChat Work. It automatically analyzes exception stack traces, pinpoints the source code file and line number where the exception occurred, and retrieves code committer information through GitHub, GitLab or Gitee APIs. Finally, it sends exception details, TraceID, and responsible person information to a DingTalk, Feishu or WeChat Work group, enabling real-time exception reporting and full-chain tracking.

## ✨ Features

- 🎯 Basing on `@AfterThrowing` to automatically capture unhandled exceptions in Spring Boot applications
- 🔍 Stack trace analysis to precisely locate exception source (file name and line number)
- 👤 Retrieval of code committer information via GitHub API, GitLab API or Gitee API's Git Blame feature
- 🤖 Generate AI analysis links for rich troubleshooting via an external workspace
- 🔗 Integration with distributed tracing systems to correlate TraceID
- 📢 Support for real-time exception alerts via DingTalk robot, Feishu robot and WeChat Work robot
- ☁️ Support for Tencent Cloud Log Service (CLS) trace linking
- 🛡️ Exception deduplication to prevent repeated alerts for the same exception within a time window
- 💡 Zero-intrusion design, requiring only dependency addition and simple configuration
- 🎨 Support for custom alert templates and rules

## 🚀 Quick Start

### 1️⃣ Add Dependency

Add the following dependency to your Spring Boot project's `pom.xml` file:

```xml
<dependency>
    <groupId>com.nolimit35.springkit</groupId>
    <artifactId>exception-notify</artifactId>
    <version>1.3.1-RELEASE</version>
</dependency>
```

### 2️⃣ Configure Parameters

Add the following configuration to your `application.yml` or `application.properties`:

```yaml
exception:
  notify:
    enabled: true                                # Enable exception notification
    dingtalk:
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx  # DingTalk robot webhook URL
      at:
        enabled: true                                                 # Enable @ mention feature
        userIdMappingGitEmail:                                        # Mapping between DingTalk user ID and Git email
          xxx: ['xxx@xx.com','xxxx@xx.com']
    feishu:
      webhook: https://open.feishu.cn/open-apis/bot/v2/hook/xxx       # Feishu robot webhook URL
      at:
        enabled: true                                                 # Enable @ mention feature
        openIdMappingGitEmail:                                        # Mapping between Feishu openID and Git email
          ou_xxxxxxxx: ['xxx@xx.com','xxxx@xx.com']
    wechatwork:
      webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx  # WeChat Work robot webhook URL
      at:
        enabled: true                                                    # Enable @ mention feature
        userIdMappingGitEmail:                                           # Mapping between WeChat Work user ID and Git email
          # For WeChat Work user IDs containing @ symbol, replace with [@]
          'xxx[@]xx.com': ['xxx@xx.com','xxxx@xx.com']
    # GitHub configuration (choose one of GitHub, GitLab, or Gitee)
    github:
      token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx                # GitHub access token
      repo-owner: your-github-username                               # GitHub repository owner
      repo-name: your-repo-name                                      # GitHub repository name
      branch: master                                                 # GitHub repository branch
    # GitLab configuration (choose one of GitHub, GitLab, or Gitee)
    gitlab:
      token: glpat-xxxxxxxxxxxxxxxxxxxx                              # GitLab access token
      project-id: your-project-id-or-path                            # GitLab project ID or path
      base-url: https://gitlab.com/api/v4                            # GitLab API base URL
      branch: master                                                 # GitLab repository branch
    # Gitee configuration (choose one of GitHub, GitLab, or Gitee)
    gitee:
      token: xxxxxxxxxxxxxxxxxxxxxxx                                 # Gitee access token
      repo-owner: your-gitee-username                                # Gitee repository owner
      repo-name: your-repo-name                                      # Gitee repository name
      branch: master                                                 # Gitee repository branch
    tencentcls:
      region: ap-guangzhou                                           # Tencent Cloud Log Service (CLS) region
      topic-id: xxx-xxx-xxx                                          # Tencent Cloud Log Service (CLS) topic ID
    trace:
      enabled: true                                                  # Enable trace linking
      header-name: X-Trace-Id                                        # Trace ID request header name
    # AI analysis link configuration (optional)
    ai:
      enabled: true                                                  # Enable AI analysis link
      include-code-context: true                                     # Include code context
      code-context-lines: 5                                          # Number of context lines to capture
      analysis-page-url: http://localhost:5173                       # AI workspace URL
      payload-param: payload                                         # Query parameter name for the compressed payload
    package-filter:
      enabled: false                                                 # Enable package name filtering
      include-packages:                                              # List of packages to include in analysis
        - com.example.app
        - com.example.service
    notification:
      title-template: "【${appName}】Exception Alert"                # Alert title template
      include-stacktrace: true                                       # Include full stack trace
      max-stacktrace-lines: 10                                       # Maximum number of stack trace lines
      deduplication:
        enabled: true                                                # Enable exception deduplication
        time-window-minutes: 3                                       # Deduplication time window in minutes, default 3 minutes
        cleanup-interval-minutes: 60                                 # Cache cleanup interval in minutes, default 60 minutes
    environment:
      report-from: test,prod                                         # List of environments to report exceptions from

# Spring configuration
spring:
  # Application name, used in alert title
  application:
    name: YourApplicationName
  # Current environment configuration, used to determine exception notification environment
  profiles:
    active: dev                                                      # Current active environment
```

> **Note**: The current environment is automatically read from Spring's `spring.profiles.active` property, so manual setting is not required. Exceptions are only reported from environments listed in `exception.notify.environment.report-from`. By default, exceptions are only reported from test and prod environments.

### 3️⃣ Start the Application

Start your Spring Boot application, and Exception-Notify will automatically register a global exception handler to capture unhandled exceptions in classes marked with @Controller, @RestController, or @ExceptionNotify and send alerts.

## 📮 Alert Example

When an unhandled exception occurs in the application, a message like the following will be sent to the DingTalk or WeChat Work group:

```
【Service Name】Exception Alert
-------------------------------
Exception Time: 2023-03-17 14:30:45
Exception Type: java.lang.NullPointerException
Exception Message: Cannot invoke "String.length()" because "str" is null
Exception Location: com.example.service.UserService.processData(UserService.java:42)
Current Environment: prod
Code Committer: John Doe (johndoe@example.com)
Last Commit Time: 2023-03-15 10:23:18
TraceID: 7b2d1e8f9c3a5b4d6e8f9c3a5b4d6e8f
Cloud Log Link: https://console.cloud.tencent.com/cls/search?region=ap-guangzhou&topic_id=xxx-xxx-xxx&interactiveQueryBase64=xxxx
-------------------------------
### AI Analysis:

[Open AI Analysis](https://ai.example.com/analysis?payload=xxxxxx)

(The link carries the compressed exception context so you can inspect details and continue the conversation inside the workspace.)

-------------------------------
Stack Trace:
java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
    at com.example.service.UserService.processData(UserService.java:42)
    at com.example.controller.UserController.getData(UserController.java:28)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    ...
mention: @John Doe
```

> **Note**: The AI analysis link appears only when `exception.notify.ai.enabled` is `true` and `analysis-page-url` is configured.

## ⚙️ Advanced Configuration

### 🌍 Environment Configuration

You can specify which environments should report exceptions by configuring the `exception.notify.environment.report-from` property:

```yaml
exception:
  notify:
    environment:
      report-from: dev,test,prod  # Report exceptions from development, test, and production environments
```

By default, the component only reports exceptions from test and prod environments, but not from the dev environment. The current environment is automatically read from Spring's `spring.profiles.active` property.

### 🛡️ Exception Deduplication Configuration

To avoid repeated alerts for the same exception within a short time period, Exception-Notify provides exception deduplication functionality. You can enable and customize the deduplication strategy with the following configuration:

```yaml
exception:
  notify:
    notification:
      deduplication:
        enabled: true                            # Enable exception deduplication (default: true)
        time-window-minutes: 3                   # Deduplication time window in minutes, default 3 minutes
        cleanup-interval-minutes: 60             # Cache cleanup interval in minutes, default 60 minutes
```

Configuration details:

- **enabled**: Whether to enable exception deduplication, defaults to `true`. Set to `false` to disable deduplication
- **time-window-minutes**: Deduplication time window in minutes. Within this time window, the same exception will only trigger one notification. Default value is 3 minutes
- **cleanup-interval-minutes**: Cache cleanup interval in minutes. The system periodically cleans up expired cache data to prevent memory usage. Default value is 60 minutes (1 hour)

**Deduplication Mechanism**:

1. Exception uniqueness is determined by: exception type, exception message, and exception location (file name and line number)
2. When the same exception occurs again within the time window, it will be filtered out and no duplicate notification will be sent
3. After the time window expires, the same exception will trigger a notification again
4. The system automatically cleans up expired cache data to prevent memory leaks

**Use Cases**:

- In high-concurrency scenarios, the same issue may trigger many exceptions in a short time, avoiding alert bombardment
- When scheduled tasks fail, avoid sending duplicate alerts on every execution
- The time window can be adjusted according to actual needs, such as 5 or 10 minutes

### 🧠 AI Analysis Workspace

Exception-Notify compresses exception details and code context into a Base64URL + GZIP string and appends it to the configured `analysis-page-url`. The project ships a sample Vite + React workspace under the repository root (`web/`) which decodes the payload in the browser and allows interactive conversations with your AI provider.

#### 🎨 Workspace Features

- 📡 **Streaming Response**: AI responses use Server-Sent Events (SSE) for real-time streaming display
- 📝 **Markdown Rendering**: Full Markdown syntax support including headings, lists, code blocks, etc.
- 🎨 **Code Highlighting**: Syntax highlighting based on highlight.js, supporting multiple programming languages
- 💬 **Interactive Dialogue**: Supports multi-turn conversations for in-depth exception analysis
- 🔄 **Regenerate**: Not satisfied with AI's answer? Regenerate with one click
- 📋 **Quick Copy**: Quickly copy message content and code snippets
- ✏️ **Context Editing**: Support online editing of code context, stack trace and additional notes
- 🌙 **Dark Theme**: Eye-friendly dark interface design
- 🔐 **Local Storage**: API keys stored only in browser LocalStorage for privacy protection

#### 🚀 Quick Deploy

**Deploy to Vercel with one click:**

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FYOUR_USERNAME%2Fexception-notify&project-name=exception-notify-workspace&repository-name=exception-notify&root-directory=web)

> **Note**: Replace `YOUR_USERNAME` in the link above with your GitHub username

**Deploy to Cloudflare Pages:**

Method 1: Manual deployment via Cloudflare Dashboard

1. Login to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Select Pages → Create a project
3. Connect to your GitHub repository
4. Configure build settings:
   - **Build command**: `cd web && npm install && npm run build`
   - **Build output directory**: `web/dist`
   - **Root directory**: `/` (keep default)

Method 2: Auto-deploy via GitHub Actions

The repository is configured with Cloudflare Pages automatic deployment workflow. Just:

1. Add the following Secrets in your GitHub repository settings:
   - `CLOUDFLARE_API_TOKEN`: Your Cloudflare API Token
   - `CLOUDFLARE_ACCOUNT_ID`: Your Cloudflare Account ID

2. Push code to main branch or modify files in `web/` directory to trigger deployment automatically

#### 📦 Local Development

1. Install dependencies and start local development:
   ```bash
   cd web
   npm install
   npm run dev
   ```

2. Build for production:
   ```bash
   npm run build
   ```

3. Preview production build:
   ```bash
   npm run preview
   ```

#### ⚙️ Configuration

1. Point `analysis-page-url` to your deployed workspace (e.g., `https://your-workspace.vercel.app` or `https://your-workspace.pages.dev`)
2. The workspace prompts users to enter API keys and model information in the browser. All sensitive configurations are stored only in browser LocalStorage
3. Can be replaced with internal proxy service as needed
4. If you need to customize the query parameter name, configure `exception.notify.ai.payload-param` to keep frontend and backend consistent

#### 🔧 Custom Deployment

Deploy to other platforms (such as Netlify, GitHub Pages, etc.):

```bash
cd web
npm install
npm run build
# Deploy the dist directory to your static hosting service
```


### 📦 Package Filter Configuration

You can control which package names to focus on during exception stack trace analysis by configuring `exception.notify.package-filter`:

```yaml
exception:
  notify:
    package-filter:
      enabled: true                              # Enable package filter
      include-packages:                          # List of packages to analyze
        - com.example.app
        - com.example.service
```

When package filtering is enabled, the exception analyzer will prioritize finding stack trace information from the specified package list, which is particularly useful for locating problems in business code. If no matching stack information is found, the original filtering logic will be used.

### ☁️ Tencent Cloud Log Service (CLS) Integration

If you use Tencent Cloud Log Service (CLS), you can configure the relevant parameters to add cloud log links to exception alerts:

```yaml
exception:
  notify:
    tencentcls:
      region: ap-guangzhou             # Tencent Cloud Log Service (CLS) region
      topic-id: xxx-xxx-xxx            # Tencent Cloud Log Service (CLS) topic ID
    trace:
      enabled: true                    # Enable trace linking
```

When both CLS parameters and trace linking are configured, the exception alert message will include a link to the cloud logs, making it easy to quickly view the complete log context.

### 👤 Code Committer Information Integration

Exception-Notify supports retrieving code committer information via GitHub API, GitLab API, or Gitee API. You need to choose one of these methods for configuration, as they cannot be used simultaneously:

```yaml
exception:
  notify:
    # Use GitHub API to get code committer information
    github:
      token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # GitHub access token
      repo-owner: your-github-username                 # GitHub repository owner
      repo-name: your-repo-name                        # GitHub repository name
      branch: master                                   # GitHub repository branch
```

Or:

```yaml
exception:
  notify:
    # Use GitLab API to get code committer information
    gitlab:
      token: glpat-xxxxxxxxxxxxxxxxxxxx                # GitLab access token
      project-id: your-project-id-or-path              # GitLab project ID or path
      base-url: https://gitlab.com/api/v4              # GitLab API base URL
      branch: master                                   # GitLab repository branch
```

Or:

```yaml
exception:
  notify:
    # Use Gitee API to get code committer information
    gitee:
      token: xxxxxxxxxxxxxxxxxxxxxxx                   # Gitee access token
      repo-owner: your-gitee-username                  # Gitee repository owner
      repo-name: your-repo-name                        # Gitee repository name
      branch: master                                   # Gitee repository branch
```

> **Note**: GitHub, GitLab, and Gitee configurations are mutually exclusive; the system can only read commit information from one code hosting platform. If multiple are configured, preference order is Gitee, then GitLab, then GitHub.


### 📣 Notification @ Mention Configuration

Exception notifications support @mentioning responsible persons in DingTalk, Feishu, or WeChat Work groups to draw attention more quickly. You can enable and customize the @ mention feature with the following configuration:

```yaml
exception:
  notify:
    dingtalk:
      at:
        enabled: true                                                 # Enable @ mention feature
        userIdMappingGitEmail:                                        # Mapping between DingTalk user ID and Git email
          xxx: ['xxx@xx.com','xxxx@xx.com']
    feishu:
      at:
        enabled: true                                                 # Enable @ mention feature
        openIdMappingGitEmail:                                        # Mapping between Feishu openID and Git email
          ou_xxxxxxxx: ['xxx@xx.com','xxxx@xx.com']
    wechatwork:
      at:
        enabled: true                                                 # Enable @ mention feature
        userIdMappingGitEmail:                                        # Mapping between WeChat Work user ID and Git email
          # For WeChat Work user IDs containing @ symbol, replace with [@]
          'xxx[@]xx.com': ['xxx@xx.com','xxxx@xx.com']
```

Configuration details:

1. **DingTalk @ Mention**:
   - `enabled`: Whether to enable DingTalk @ mentions
   - `userIdMappingGitEmail`: Mapping between DingTalk user IDs and Git commit emails, supporting multiple Git emails for one user ID

2. **Feishu @ Mention**:
   - `enabled`: Whether to enable Feishu @ mentions
   - `openIdMappingGitEmail`: Mapping between Feishu openIDs and Git commit emails, supporting multiple Git emails for one openID

3. **WeChat Work @ Mention**:
   - `enabled`: Whether to enable WeChat Work @ mentions
   - `userIdMappingGitEmail`: Mapping between WeChat Work user IDs and Git commit emails, supporting multiple Git emails for one user ID
   - Note: For WeChat Work user IDs containing `@` symbol, replace with `[@]`

When the @ mention feature is enabled, the system will identify the responsible person based on Git commit information and mention them in the alert message, improving the timeliness and accuracy of exception handling.

### 🤖 AI Intelligent Suggestion Configuration

Exception-Notify can bundle stack traces, code context, trace identifiers, and author information into a compressed payload that is appended to an external AI workspace URL. The workspace (for example the sample app under `web/`) can then decode the payload, render the details, and let you drive the conversation with your preferred AI provider.

```yaml
exception:
  notify:
    ai:
      enabled: true                                          # Enable AI analysis link
      include-code-context: true                             # Capture code context around the failure
      code-context-lines: 5                                  # Number of lines before/after the target line
      analysis-page-url: https://ai.example.com/analysis     # Hosted AI workspace URL
      payload-param: payload                                 # Query parameter that carries the compressed payload
```

**Configuration Details**:

1. **enabled**: Turns on payload generation and the AI analysis link block.
2. **include-code-context**: Captures surrounding source code when repository integrations can provide it.
3. **code-context-lines**: Controls how many lines before and after the error line are included.
4. **analysis-page-url**: Where the notification should point users for deep-dive analysis; typically a self-hosted web workspace.
5. **payload-param**: Query parameter name expected by the workspace; defaults to `payload` and must stay in sync with the frontend.

**Important Notes**:

- The backend no longer calls external AI providers directly; conversations happen inside the workspace.
- Payloads are encoded with Base64URL + GZIP. The sample workspace demonstrates how to decode them client-side.
- Users provide their API credentials within the workspace UI (stored locally by default). Consider proxying through an internal service if stricter control is required.
- If workspace configuration is incomplete, the notification will omit the AI analysis link but still deliver the rest of the alert normally.


**Workspace Features**:

- 📡 **Streaming Response**: AI responses use Server-Sent Events (SSE) for real-time streaming display
- 📝 **Markdown Rendering**: Full Markdown syntax support including headings, lists, code blocks, etc.
- 🎨 **Code Highlighting**: Syntax highlighting based on highlight.js, supporting multiple programming languages
- 💬 **Interactive Dialogue**: Supports multi-turn conversations for in-depth exception analysis
### 🔧 Custom Exception Filtering

You can customize which exceptions should trigger alerts by implementing the `ExceptionFilter` interface and registering it as a Spring Bean:

```java
@Component
public class CustomExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        // Ignore specific types of exceptions
        if (throwable instanceof ResourceNotFoundException) {
            return false;
        }
        return true;
    }
}
```

### 🎨 Custom Alert Content

You can customize the alert content format by implementing the `NotificationFormatter` interface and registering it as a Spring Bean:

```java
@Component
public class CustomNotificationFormatter implements NotificationFormatter {
    @Override
    public String format(ExceptionInfo exceptionInfo) {
        // Custom alert content format
        return "Custom alert content";
    }
}
```

### 🔗 Custom Trace Information

You can customize how TraceID is retrieved and trace URLs are generated by implementing the `TraceInfoProvider` interface and registering it as a Spring Bean:

```java
@Component
public class CustomTraceInfoProvider implements TraceInfoProvider {
    @Override
    public String getTraceId() {
        // Custom logic to retrieve TraceID
        return "custom-trace-id";
    }

    @Override
    public String generateTraceUrl(String traceId) {
        // Custom logic to generate trace URL
        return "https://your-log-system.com/trace?id=" + traceId;
    }
}
```

The default implementation `DefaultTraceInfoProvider` retrieves TraceID from MDC or request headers and generates Tencent Cloud Log Service (CLS) trace URLs.

## 🎨 Customization

### Custom Notification Format

You can customize the format of exception notifications by implementing the `NotificationFormatter` interface:

```java
@Component
public class CustomNotificationFormatter implements NotificationFormatter {
    @Override
    public String format(ExceptionInfo exceptionInfo) {
        // Custom notification format
        return "Custom alert content";
    }
}
```

### Custom Exception Filter

You can customize which exceptions should trigger notifications by implementing the `ExceptionFilter` interface:

```java
@Component
public class CustomExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        // Custom filtering logic to determine if a notification should be sent
        return throwable instanceof RuntimeException;
    }
}
```

### 📱 Custom Notification Channel

You can add a custom notification channel by implementing the `NotificationProvider` interface:

```java
@Component
public class CustomNotificationProvider implements NotificationProvider {
    @Override
    public boolean sendNotification(ExceptionInfo exceptionInfo) {
        // Implement custom notification channel logic
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

Or more preferably by extending the `AbstractNotificationProvider` abstract class:

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

## 📊 Monitor Utility

Monitor is a simple utility class that allows you to record logs and send messages through notification channels configured in Exception-Notify (such as DingTalk, Feishu, or WeChat Work).

### Features

- Simple API similar to SLF4J, integrates with existing logging systems
- Automatically sends messages through configured notification channels
- Supports multiple logging levels (info, warn, error)
- Supports messages with or without exceptions
- Supports custom Logger instances
- Automatically captures TraceID from MDC or request headers when trace is enabled
- Generates links to Tencent Cloud Log Service (CLS) for easy log tracking
- Includes caller location information for better debugging

### Usage

#### Basic Usage

Call static methods directly to record logs and send notifications:

```java
// Info level notification
Monitor.info("User registration completed successfully");

// Warning level notification
Monitor.warn("Payment processing delayed");

// Error level notification
Monitor.error("Database connection failed");

// Notifications with exceptions
try {
    // Business logic
} catch (Exception e) {
    Monitor.info("Order received with issues", e);
    Monitor.warn("Order processing partially failed", e);
    Monitor.error("Order processing failed", e);
}
```

#### Using Custom Logger

You can get an SLF4J Logger instance through the `Monitor.getLogger()` method and use it for logging:

```java
// Get Logger
Logger logger = Monitor.getLogger(YourService.class);

// Use custom Logger to send notifications
Monitor.info(logger, "Payment processing started");
Monitor.warn(logger, "Payment processing delayed");
Monitor.error(logger, "Payment processing failed");

// With exceptions
Monitor.info(logger, "Third-party service responded with warnings", exception);
Monitor.warn(logger, "Third-party service responded with errors", exception);
Monitor.error(logger, "Third-party service call failed", exception);
```

### Configuration

Monitor utility uses the same configuration as Exception-Notify, no additional configuration is needed. As long as Exception-Notify is configured in `application.yml` or `application.properties`, Monitor will automatically use these configurations.

### Common Use Cases

#### Database Operation Failure

```java
try {
    repository.save(entity);
} catch (DataAccessException e) {
    Monitor.error("Failed to save entity with id: " + entity.getId(), e);
}
```

#### Third-Party Service Call Failure

```java
try {
    String response = thirdPartyApiClient.call();
    if (response == null || response.isEmpty()) {
        Monitor.error("Third-party API returned empty response");
    }
} catch (Exception e) {
    Monitor.error("Third-party API call failed", e);
}
```

#### Business Rule Violation

```java
if (withdrawAmount > dailyLimit) {
    Monitor.error("Business rule violation: attempted to withdraw " +
                  withdrawAmount + " exceeding daily limit of " + dailyLimit);
    throw new BusinessRuleException("Withdrawal amount exceeds daily limit");
}
```

#### Important Business Process State Change

```java
Monitor.error("Order #12345 status changed from PENDING to FAILED");
```

### Notes

- Monitor is primarily used for important errors and business events that require immediate notification
- Avoid overuse to prevent notification channel overload
- Notifications are only sent in configured environments (typically test and prod environments)
- TraceID is automatically captured from MDC or request headers when trace is enabled
- If Tencent CLS is configured, clickable log links will be included in notifications

## 🔧 How It Works

1. Captures unhandled exceptions through Spring AOP's `@AfterThrowing` annotation mechanism
2. Analyzes exception stack trace information to extract the source code file and line number where the exception occurred
3. Calls the GitHub API, GitLab API, or Gitee API's Git Blame interface to get committer information for the corresponding line of code
4. Extracts TraceID from the current request context (if trace linking is enabled)
5. Assembles exception information, code committer information, and TraceID into an alert message
6. Sends the alert message to the specified group through DingTalk or WeChat Work robot Webhook interface

## ⚠️ Precautions

- Ensure the application has network permissions to access GitHub API, GitLab API, or Gitee API
- GitHub Token, GitLab Token, or Gitee Token needs repository read permission
- DingTalk and WeChat Work robots need to be correctly configured with security settings
- To get accurate code committer information, ensure the code repository is consistent with the deployed code version

## 🤝 Contribution Guidelines

Issues and Pull Requests are welcome to help improve this project.

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).
