# Exception-Notify

[![Maven Central](https://img.shields.io/maven-central/v/com.nolimit35.springkit/exception-notify.svg)](https://search.maven.org/search?q=g:com.nolimit35.springkit%20AND%20a:exception-notify)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

[English](README.md) | 简体中文

## 简介

Exception-Notify 是一个 Spring Boot Starter 组件，用于捕获 Spring Boot 应用中未处理的异常，并通过钉钉或企业微信实时告警通知。它能够自动分析异常堆栈信息，定位到异常发生的源代码文件和行号，并通过 GitHub、GitLab 或 Gitee API 获取代码提交者信息，最终将异常详情、TraceID 以及责任人信息发送到钉钉群或企业微信群，实现异常的实时上报与全链路追踪。

## 功能特点

- 自动捕获 Spring Boot 应用中未处理的异常
- 分析异常堆栈，精确定位异常源码位置（文件名和行号）
- 通过 GitHub API、GitLab API 或 Gitee API 的 Git Blame 功能获取代码提交者信息
- 支持与分布式链路追踪系统集成，关联 TraceID
- 支持通过钉钉机器人和企业微信机器人实时推送异常告警
- 支持腾讯云日志服务(CLS)的链路追踪
- 零侵入式设计，仅需添加依赖和简单配置即可使用
- 支持自定义告警模板和告警规则

## 快速开始

### 1. 添加依赖

在你的 Spring Boot 项目的 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>com.nolimit35.springkit</groupId>
    <artifactId>exception-notify</artifactId>
    <version>1.1.0-RELEASE</version>
</dependency>
```

### 2. 配置参数

在 `application.yml` 或 `application.properties` 中添加以下配置：

```yaml
exception:
  notify:
    enabled: true                                # 是否启用异常通知功能
    dingtalk:
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx  # 钉钉机器人 Webhook 地址
      secret: SEC000000000000000000000000000000000000000000          # 钉钉机器人安全设置的签名
    wechatwork:
      webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx  # 企业微信机器人 Webhook 地址
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
    package-filter:
      enabled: false                                                 # 是否启用包名过滤
      include-packages:                                              # 需要解析的包名列表
        - com.example.app
        - com.example.service
    notification:
      title-template: "【${appName}】异常告警"                        # 告警标题模板
      include-stacktrace: true                                       # 是否包含完整堆栈信息
      max-stacktrace-lines: 10                                       # 堆栈信息最大行数
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

### 3. 启动应用

启动你的 Spring Boot 应用，Exception-Notify 将自动注册全局异常处理器，捕获未处理的异常并发送告警。

## 告警示例

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
堆栈信息：
java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
    at com.example.service.UserService.processData(UserService.java:42)
    at com.example.controller.UserController.getData(UserController.java:28)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    ...
```

## 高级配置

### 环境配置

你可以通过配置 `exception.notify.environment.report-from` 属性来指定哪些环境需要上报异常：

```yaml
exception:
  notify:
    environment:
      report-from: dev,test,prod  # 在开发、测试和生产环境都上报异常
```

默认情况下，组件只会在 test 和 prod 环境上报异常，而在 dev 环境不上报。当前环境会自动从 Spring 的 `spring.profiles.active` 属性中读取。

### 包名过滤配置

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

### 腾讯云日志服务(CLS)集成

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

### 代码提交者信息集成

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

### 自定义异常过滤

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

### 自定义告警内容

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

## 工作原理

1. 通过 Spring Boot 的 `@ControllerAdvice` 机制捕获未处理的异常
2. 分析异常堆栈信息，提取出异常发生的源代码文件和行号
3. 调用 GitHub API、GitLab API 或 Gitee API 的 Git Blame 接口，获取对应代码行的提交者信息
4. 从当前请求上下文中提取 TraceID（如果启用了链路追踪）
5. 将异常信息、代码提交者信息和 TraceID 组装成告警消息
6. 通过钉钉机器人或企业微信机器人 Webhook 接口发送告警消息到指定群组

## 注意事项

- 需要确保应用有访问 GitHub API、GitLab API 或 Gitee API 的网络权限
- GitHub Token、GitLab Token 或 Gitee Token 需要有仓库的读取权限
- 钉钉机器人和企业微信机器人需要正确配置安全设置
- 为了获取准确的代码提交者信息，确保代码仓库与实际部署的代码版本一致

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。 