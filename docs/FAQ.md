# Frequently Asked Questions (FAQ)

This document answers common questions about Exception-Notify.

## 📋 Table of Contents

- [General Questions](#general-questions)
- [Configuration Questions](#configuration-questions)
- [Integration Questions](#integration-questions)
- [Performance Questions](#performance-questions)
- [Deployment Questions](#deployment-questions)

## General Questions

### What is Exception-Notify?

Exception-Notify is a Spring Boot Starter that automatically captures unhandled exceptions in your application and sends real-time alerts through messaging platforms like DingTalk, Feishu, or WeChat Work. It enriches exception information with Git blame data, TraceID, and code context.

### Which Spring Boot versions are supported?

Exception-Notify requires Spring Boot 2.7 or higher and Java 8 or higher.

### Does Exception-Notify work with Spring Boot 3.x?

While Exception-Notify is primarily designed for Spring Boot 2.7, many users have successfully used it with Spring Boot 3.x. However, full compatibility testing with Spring Boot 3.x is still in progress.

### Will using Exception-Notify affect my application's performance?

Exception-Notify has minimal performance impact:
- Exception capture uses Spring AOP which has negligible overhead
- Git API calls are made asynchronously and only when exceptions occur
- Notification sending is non-blocking
- The deduplication feature prevents repeated processing

For high-frequency exception scenarios, consider:
- Enabling deduplication to reduce redundant notifications
- Using package filtering to limit scope
- Disabling Git blame if not needed

### Can I use Exception-Notify in production?

Yes! Exception-Notify is designed for production use. The `environment.report-from` configuration allows you to control which environments send notifications. By default, only test and prod environments send alerts.

## Configuration Questions

### Do I need to configure all three messaging platforms?

No, you only need to configure one. Choose DingTalk, Feishu, or WeChat Work based on your team's preference.

### Can I use multiple notification channels simultaneously?

Yes! You can configure multiple channels (e.g., both DingTalk and Feishu) and Exception-Notify will send notifications to all configured channels.

### How do I get a webhook URL for my messaging platform?

**DingTalk:**
1. Open your DingTalk group
2. Go to Group Settings → Group Assistant → Add Robot
3. Choose "Custom" robot
4. Configure security settings and copy the webhook URL

**Feishu:**
1. Open your Feishu group
2. Settings → Bots → Add Bot
3. Choose "Custom Bot"
4. Copy the webhook URL

**WeChat Work:**
1. Open your WeChat Work group
2. Right-click → Add Group Bot
3. Copy the webhook URL

### Which Git platform should I choose?

Choose the platform where your source code is hosted:
- **GitHub**: Use if your code is on GitHub.com or GitHub Enterprise
- **GitLab**: Use if your code is on GitLab.com or self-hosted GitLab
- **Gitee**: Use if your code is on Gitee.com

You can only configure one Git platform at a time.

### How do I create a token for Git integration?

**GitHub:**
1. Go to Settings → Developer settings → Personal access tokens
2. Generate new token with `repo` scope
3. Copy the token (starts with `ghp_`)

**GitLab:**
1. User Settings → Access Tokens
2. Create token with `read_api` scope
3. Copy the token (starts with `glpat-`)

**Gitee:**
1. Settings → Private Tokens
2. Create token with `projects` scope
3. Copy the token

### Can I use Exception-Notify without Git integration?

Yes! Git integration is optional. Without it, notifications will still include exception details, stack traces, and TraceID, but won't show code committer information.

### How do I configure @mentions?

Map your Git commit emails to platform user IDs:

```yaml
exception:
  notify:
    dingtalk:
      at:
        enabled: true
        userIdMappingGitEmail:
          dingtalk-user-id: ['user@company.com', 'user@gmail.com']
```

To find user IDs:
- **DingTalk**: Check user profile in DingTalk app
- **Feishu**: Use OpenID from Feishu admin console
- **WeChat Work**: Use user ID from admin console (replace `@` with `[@]`)

### How do I disable notifications for specific environments?

Configure the `environment.report-from` property:

```yaml
exception:
  notify:
    environment:
      report-from: prod  # Only notify in production
```

The environment is automatically detected from `spring.profiles.active`.

## Integration Questions

### Can I use Exception-Notify with existing exception handlers?

Yes, but be careful:
- Exception-Notify captures exceptions using `@AfterThrowing` aspect
- If your `@ControllerAdvice` or `@ExceptionHandler` catches and handles exceptions, they won't reach Exception-Notify
- Consider rethrowing exceptions after handling, or use the `Monitor` utility for manual notifications

### How do I exclude specific exception types?

Implement a custom `ExceptionFilter`:

```java
@Component
public class MyExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        // Don't notify for these exceptions
        if (throwable instanceof ResourceNotFoundException ||
            throwable instanceof ValidationException) {
            return false;
        }
        return true;
    }
}
```

### Can I use Exception-Notify with reactive Spring WebFlux?

Exception-Notify is designed for Spring MVC (servlet-based) applications. Support for WebFlux is not yet available.

### How do I integrate with custom tracing systems?

Implement `TraceInfoProvider`:

```java
@Component
public class CustomTraceProvider implements TraceInfoProvider {
    @Override
    public String getTraceId() {
        // Get trace ID from your system
        return MyTracingSystem.getCurrentTraceId();
    }
    
    @Override
    public String generateTraceUrl(String traceId) {
        return "https://tracing.company.com/trace/" + traceId;
    }
}
```

### Can I send notifications to Slack or other platforms?

Yes! Implement `NotificationProvider` or extend `AbstractNotificationProvider`:

```java
@Component
public class SlackNotificationProvider extends AbstractNotificationProvider {
    
    public SlackNotificationProvider(ExceptionNotifyProperties properties) {
        super(properties);
    }
    
    @Override
    protected boolean doSendNotification(ExceptionInfo info) {
        // Send to Slack webhook
        return slackClient.sendMessage(formatMessage(info));
    }
    
    @Override
    public boolean isEnabled() {
        return slackWebhook != null;
    }
}
```

## Performance Questions

### Will Exception-Notify slow down my application?

No, Exception-Notify has minimal performance impact:
- AOP overhead is negligible (microseconds)
- Git API calls don't block the main thread
- Notification sending is asynchronous
- Exceptions typically indicate an error state where minor delays are acceptable

### How does deduplication work?

Deduplication prevents repeated notifications for the same exception:
- Exceptions are identified by type + message + location (file:line)
- Within the time window (default 3 minutes), duplicate exceptions are suppressed
- After the time window, the same exception will trigger a new notification
- Expired entries are automatically cleaned up

### How much memory does deduplication cache use?

Memory usage is minimal:
- Each cached exception entry is ~200 bytes
- Cache is automatically cleaned based on `cleanup-interval-minutes`
- Even with 1000 different exceptions cached, memory usage is ~200KB

### Can high-frequency exceptions cause problems?

Exception-Notify includes protection mechanisms:
- Deduplication prevents notification spam
- Asynchronous processing prevents blocking
- Failed notifications don't affect application functionality

For extreme cases, consider:
- Reducing `time-window-minutes` for faster cache expiry
- Enabling package filtering to reduce processing
- Using custom `ExceptionFilter` to skip certain exceptions

## Deployment Questions

### How do I deploy the AI analysis workspace?

**Option 1: Cloudflare Workers (Recommended)**
1. Fork the repository
2. Add `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` to GitHub secrets
3. Push to main branch - automatic deployment via GitHub Actions

**Option 2: Vercel**
```bash
cd web
npm install
npm run build
vercel --prod
```

**Option 3: Netlify**
```bash
cd web
npm install
npm run build
netlify deploy --prod --dir=.next
```

### Do I need to deploy the web workspace?

No, the web workspace is optional. It's only needed if you want to use the AI analysis feature. All other Exception-Notify features work without it.

### How do I configure the AI workspace?

1. Deploy the workspace to any hosting platform
2. Configure the backend:
   ```yaml
   exception:
     notify:
       ai:
         enabled: true
         analysis-page-url: https://your-workspace.pages.dev
   ```
3. Users configure their AI provider and API key in the browser

### Can I use my own AI service?

Yes! The workspace supports:
- OpenAI (GPT-3.5, GPT-4)
- Anthropic (Claude)
- Any OpenAI-compatible API

Configure your API endpoint and key in the workspace UI. All settings are stored locally in your browser.

### How secure is the AI workspace?

Security features:
- API keys are stored only in browser LocalStorage
- No server-side storage of credentials
- Payload compression uses standard Base64URL + GZIP
- HTTPS recommended for all deployments
- Cloudflare KV storage is ephemeral (auto-expire)

### Can I self-host everything?

Yes! All components can be self-hosted:
- Exception-Notify library: Already in your application
- Web workspace: Deploy to your infrastructure
- `/api/compress` endpoint: Included in web workspace
- KV storage: Use Redis or any key-value store (requires code modification)

### What if my application can't access external Git APIs?

Options:
1. **Don't configure Git integration**: Notifications work without it
2. **Use a proxy**: Configure proxy for OkHttp client (requires custom configuration)
3. **Use GitLab self-hosted**: Configure `gitlab.base-url` to your internal GitLab

### How do I test my configuration?

Create a test endpoint:

```java
@RestController
public class TestController {
    @GetMapping("/test-exception-notify")
    public void test() {
        throw new RuntimeException("Test notification from Exception-Notify");
    }
}
```

Visit the endpoint and check if notification arrives in your messaging platform.

### Can I test notifications without throwing exceptions?

Yes, use the `Monitor` utility:

```java
Monitor.error("This is a test notification");
```

Or manually trigger a notification programmatically:

```java
@Autowired
private NotificationProviderManager providerManager;

public void testNotification() {
    ExceptionInfo info = ExceptionInfo.builder()
        .type("TestException")
        .message("Test message")
        .build();
    
    providerManager.sendNotification(info);
}
```

## Still Have Questions?

- Check the [Troubleshooting Guide](./TROUBLESHOOTING.md)
- Search [GitHub Issues](https://github.com/GuangYiDing/exception-notify/issues)
- Create a new issue with the `question` label
- Read the full [README](../README.md)
