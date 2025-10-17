# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with Exception-Notify.

## 📋 Table of Contents

- [Common Issues](#common-issues)
  - [No Notifications Received](#no-notifications-received)
  - [Git Blame Information Not Working](#git-blame-information-not-working)
  - [TraceID Not Captured](#traceid-not-captured)
  - [AI Analysis Link Not Working](#ai-analysis-link-not-working)
  - [Duplicate Notifications](#duplicate-notifications)
  - [@Mention Not Working](#mention-not-working)
- [Configuration Issues](#configuration-issues)
- [Performance Issues](#performance-issues)
- [Debugging](#debugging)

## 🔧 Common Issues

### No Notifications Received

**Symptoms:**
- Exceptions occur but no notifications are sent
- No errors in logs

**Possible Causes & Solutions:**

1. **Exception-Notify is disabled**
   ```yaml
   exception:
     notify:
       enabled: true  # Ensure this is set to true
   ```

2. **Wrong environment configuration**
   ```yaml
   exception:
     notify:
       environment:
         report-from: test,prod  # Add your current environment
   
   spring:
     profiles:
       active: prod  # Ensure this matches report-from
   ```

3. **Webhook URL not configured**
   - Check that at least one notification provider webhook is configured
   - Verify the webhook URL is correct and accessible
   
   ```yaml
   exception:
     notify:
       dingtalk:
         webhook: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
   ```

4. **Exception not caught by aspect**
   - Only exceptions in classes annotated with `@Controller`, `@RestController`, or `@ExceptionNotify` are captured
   - Add `@ExceptionNotify` annotation to your service classes if needed
   
   ```java
   @Service
   @ExceptionNotify  // Add this annotation
   public class YourService {
       // ...
   }
   ```

5. **Exception is handled before reaching aspect**
   - Check if you have other exception handlers catching exceptions first
   - Ensure `@ControllerAdvice` doesn't intercept all exceptions

6. **Custom ExceptionFilter blocking notifications**
   - If you have a custom `ExceptionFilter`, verify it returns `true` for the exception types you want to notify

### Git Blame Information Not Working

**Symptoms:**
- Notifications don't include committer information
- Notifications show "Unknown" for committer

**Possible Causes & Solutions:**

1. **Git provider not configured**
   ```yaml
   exception:
     notify:
       github:  # Or gitlab/gitee
         token: YOUR_TOKEN
         repo-owner: YOUR_USERNAME
         repo-name: YOUR_REPO
         branch: main
   ```

2. **Invalid token**
   - Verify your token has read permissions
   - For GitHub: Generate a new Personal Access Token with `repo` scope
   - For GitLab: Use a token with `read_api` scope
   - For Gitee: Use a token with `projects` scope

3. **Repository path mismatch**
   - Ensure the code running matches the repository configuration
   - Check that branch name is correct

4. **Network connectivity issues**
   - Verify your application can reach the Git API endpoints
   - Check firewall rules and proxy settings
   - Test API connectivity:
     ```bash
     curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/user
     ```

5. **File not found in repository**
   - The exception might occur in a file not tracked in the repository
   - Check if the file path matches what's in your repository

### TraceID Not Captured

**Symptoms:**
- Notifications don't include TraceID
- Cloud log links not generated

**Possible Causes & Solutions:**

1. **Trace feature disabled**
   ```yaml
   exception:
     notify:
       trace:
         enabled: true  # Enable trace
   ```

2. **TraceID not in MDC or request headers**
   - Exception-Notify looks for TraceID in MDC first, then request headers
   - Ensure your tracing system is properly configured
   
   **For MDC:**
   ```java
   MDC.put("traceId", yourTraceId);
   ```
   
   **For custom header:**
   ```yaml
   exception:
     notify:
       trace:
         header-name: X-Your-Trace-Header  # Match your header name
   ```

3. **Custom TraceInfoProvider not working**
   - If you implemented custom `TraceInfoProvider`, verify the implementation
   - Check logs for any errors in trace extraction

### AI Analysis Link Not Working

**Symptoms:**
- No AI analysis link in notifications
- Link leads to error page
- Payload cannot be decoded

**Possible Causes & Solutions:**

1. **AI feature disabled**
   ```yaml
   exception:
     notify:
       ai:
         enabled: true
   ```

2. **Analysis page URL not configured**
   ```yaml
   exception:
     notify:
       ai:
         analysis-page-url: https://your-workspace.pages.dev
   ```

3. **Compress API not accessible**
   - Verify `/api/compress` endpoint is deployed and accessible
   - Test the endpoint:
     ```bash
     curl -X POST https://your-workspace.pages.dev/api/compress \
       -H "Content-Type: application/json" \
       -d '{"payload":"test"}'
     ```

4. **CORS issues**
   - Ensure CORS is properly configured on your workspace
   - Check browser console for CORS errors

5. **Cloudflare KV not configured**
   - If using Cloudflare Workers, ensure KV namespace is bound
   - Check Cloudflare Workers logs for errors

### Duplicate Notifications

**Symptoms:**
- Same exception triggers multiple notifications
- Notification spam

**Possible Causes & Solutions:**

1. **Deduplication disabled**
   ```yaml
   exception:
     notify:
       notification:
         deduplication:
           enabled: true
           time-window-minutes: 3
   ```

2. **Multiple exception handlers**
   - Check if you have multiple aspects or exception handlers
   - Ensure only one handler processes each exception

3. **Retry mechanism**
   - If using retry logic, exceptions might be thrown multiple times
   - Configure retry behavior appropriately

### @Mention Not Working

**Symptoms:**
- Notifications don't @mention users
- Wrong users are mentioned

**Possible Causes & Solutions:**

1. **@Mention feature disabled**
   ```yaml
   exception:
     notify:
       dingtalk:  # or feishu/wechatwork
         at:
           enabled: true
   ```

2. **Incorrect user mapping**
   ```yaml
   exception:
     notify:
       dingtalk:
         at:
           userIdMappingGitEmail:
             dingtalk-user-id: ['git-email@example.com']
   ```
   - Verify DingTalk/Feishu user IDs are correct
   - Check Git emails match those in commit history
   - For WeChat Work, ensure `@` in user IDs is replaced with `[@]`

3. **Git committer email doesn't match**
   - The email in Git blame must match the mapped email
   - Check actual commit emails:
     ```bash
     git log --format='%ae' | sort -u
     ```

## ⚙️ Configuration Issues

### Property Not Being Recognized

**Check:**
1. Correct YAML indentation (use spaces, not tabs)
2. Property names match exactly (case-sensitive)
3. Spring Boot version compatibility
4. Configuration is in correct profile

**Validation:**
```bash
# Check actual configuration being used
mvn spring-boot:run -Ddebug
```

### Auto-Configuration Not Working

**Check:**
1. Dependency is included in `pom.xml`
2. No conflicting auto-configuration exclusions
3. Spring Boot version >= 2.7

**Debug:**
```yaml
logging:
  level:
    com.nolimit35.springkit: DEBUG
```

## 📊 Performance Issues

### High Memory Usage

**Possible Causes:**
1. Deduplication cache growing too large
2. Too many exceptions being tracked

**Solutions:**
```yaml
exception:
  notify:
    notification:
      deduplication:
        cleanup-interval-minutes: 30  # Clean up more frequently
        time-window-minutes: 2  # Reduce window size
```

### Slow Notification Delivery

**Possible Causes:**
1. Git API calls are slow
2. Network latency to webhook endpoints

**Solutions:**
1. Consider disabling Git blame for better performance:
   ```yaml
   # Don't configure github/gitlab/gitee sections
   ```

2. Increase timeouts if needed (requires custom HTTP client configuration)

### High API Rate Limiting

**Symptoms:**
- Git API calls failing
- 403 or 429 HTTP errors

**Solutions:**
1. Reduce notification frequency with deduplication
2. Use a different token or increase rate limits
3. Cache Git blame results (requires custom implementation)

## 🐛 Debugging

### Enable Debug Logging

```yaml
logging:
  level:
    com.nolimit35.springkit: DEBUG
    # For HTTP client debugging
    okhttp3: DEBUG
```

### Check Bean Registration

Add this to a configuration class:
```java
@Component
@Slf4j
public class BeanChecker implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        log.info("Notification providers: {}", 
            ctx.getBeansOfType(NotificationProvider.class));
        log.info("Exception filters: {}", 
            ctx.getBeansOfType(ExceptionFilter.class));
    }
}
```

### Test Notification Manually

```java
@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private NotificationProviderManager providerManager;
    
    @GetMapping("/notify")
    public String testNotification() {
        ExceptionInfo info = ExceptionInfo.builder()
            .type("TestException")
            .message("This is a test")
            .build();
        
        boolean sent = providerManager.sendNotification(info);
        return "Notification sent: " + sent;
    }
    
    @GetMapping("/exception")
    public String testException() {
        throw new RuntimeException("Test exception");
    }
}
```

### Verify Aspect Registration

```java
@Component
@Slf4j
public class AspectChecker {
    
    @Autowired
    private ApplicationContext context;
    
    @PostConstruct
    public void checkAspects() {
        String[] aspects = context.getBeanNamesForType(
            com.nolimit35.springkit.aspect.ExceptionNotifyAspect.class
        );
        log.info("Exception notify aspects: {}", Arrays.toString(aspects));
    }
}
```

## 📞 Getting Help

If you still can't resolve the issue:

1. **Check existing issues**: [GitHub Issues](https://github.com/GuangYiDing/exception-notify/issues)
2. **Create a new issue** with:
   - Exception-Notify version
   - Spring Boot version
   - Java version
   - Configuration (sanitize sensitive data)
   - Full error logs
   - Steps to reproduce

3. **Provide context**:
   - What you expected to happen
   - What actually happened
   - Any workarounds you've tried

## 🔍 Additional Resources

- [Configuration Guide](../README_EN.md#-configuration-overview) · [简体中文](../README.md#-配置总览)
- [Quick Start](../README_EN.md#-quick-start) · [简体中文](../README.md#-快速开始)
- [Contributing Guide](../CONTRIBUTING.md)
- [FAQ](./FAQ.md)
