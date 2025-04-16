# Exception-Notify

[![Maven Central](https://img.shields.io/maven-central/v/com.nolimit35.springkit/exception-notify.svg)](https://search.maven.org/search?q=g:com.nolimit35.springkit%20AND%20a:exception-notify)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

English | [简体中文](README_CN.md)

## Introduction

Exception-Notify is a Spring Boot Starter component designed to capture unhandled exceptions in Spring Boot applications and send real-time alerts through DingTalk, Feishu or WeChat Work. It automatically analyzes exception stack traces, pinpoints the source code file and line number where the exception occurred, and retrieves code committer information through GitHub, GitLab or Gitee APIs. Finally, it sends exception details, TraceID, and responsible person information to a DingTalk, Feishu or WeChat Work group, enabling real-time exception reporting and full-chain tracking.

## Features

- Automatic capture of unhandled exceptions in Spring Boot applications
- Stack trace analysis to precisely locate exception source (file name and line number)
- Retrieval of code committer information via GitHub API, GitLab API or Gitee API's Git Blame feature
- Integration with distributed tracing systems to correlate TraceID
- Support for real-time exception alerts via DingTalk robot, Feishu robot and WeChat Work robot
- Support for Tencent Cloud Log Service (CLS) trace linking
- Zero-intrusion design, requiring only dependency addition and simple configuration
- Support for custom alert templates and rules

## Quick Start

### 1. Add Dependency

Add the following dependency to your Spring Boot project's `pom.xml` file:

```xml
<dependency>
    <groupId>com.nolimit35.springkit</groupId>
    <artifactId>exception-notify</artifactId>
    <version>1.2.1-RELEASE</version>
</dependency>
```

### 2. Configure Parameters

Add the following configuration to your `application.yml` or `application.properties`:

```yaml
exception:
  notify:
    enabled: true                                # Enable exception notification
    dingtalk:
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx  # DingTalk robot webhook URL
    feishu:
      webhook: https://open.feishu.cn/open-apis/bot/v2/hook/xxx       # Feishu robot webhook URL
    wechatwork:
      webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx  # WeChat Work robot webhook URL
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
    package-filter:
      enabled: false                                                 # Enable package name filtering
      include-packages:                                              # List of packages to include in analysis
        - com.example.app
        - com.example.service
    notification:
      title-template: "【${appName}】Exception Alert"                # Alert title template
      include-stacktrace: true                                       # Include full stack trace
      max-stacktrace-lines: 10                                       # Maximum number of stack trace lines
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

### 3. Start the Application

Start your Spring Boot application, and Exception-Notify will automatically register a global exception handler to capture unhandled exceptions and send alerts.

## Alert Example

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
Stack Trace:
java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
    at com.example.service.UserService.processData(UserService.java:42)
    at com.example.controller.UserController.getData(UserController.java:28)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    ...
```

## Advanced Configuration

### Environment Configuration

You can specify which environments should report exceptions by configuring the `exception.notify.environment.report-from` property:

```yaml
exception:
  notify:
    environment:
      report-from: dev,test,prod  # Report exceptions from development, test, and production environments
```

By default, the component only reports exceptions from test and prod environments, but not from the dev environment. The current environment is automatically read from Spring's `spring.profiles.active` property.

### Package Filter Configuration

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

### Tencent Cloud Log Service (CLS) Integration

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

### Code Committer Information Integration

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

### Custom Exception Filtering

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

### Custom Alert Content

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

## Customization

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

### Custom Notification Channel

You can add custom notification channels by implementing the `NotificationProvider` interface:

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

Or the recommended way is to extend the `AbstractNotificationProvider` abstract class:

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

## How It Works

1. Captures unhandled exceptions using Spring AOP's `@AfterThrowing` annotation mechanism
2. Analyzes exception stack trace information to extract the source code file and line number where the exception occurred
3. Calls the GitHub API or Gitee API's Git Blame interface to retrieve committer information for the corresponding code line
4. Extracts TraceID from the current request context (if trace linking is enabled)
5. Assembles exception information, code committer information, and TraceID into an alert message
6. Sends the alert message to the specified group via DingTalk robot or WeChat Work robot Webhook interface

## Important Notes

- Ensure the application has network permission to access GitHub API or Gitee API
- GitHub Token or Gitee Token must have read permission for the repository
- DingTalk robot and WeChat Work robot need to be correctly configured with security settings
- To get accurate code committer information, ensure the code repository version matches the deployed code version

## Contribution Guidelines

Issues and Pull Requests are welcome to help improve this project.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
