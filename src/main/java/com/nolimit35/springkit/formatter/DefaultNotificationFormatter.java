package com.nolimit35.springkit.formatter;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import com.nolimit35.springkit.model.ExceptionInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Default implementation of NotificationFormatter
 */
@Component
@ConditionalOnMissingBean(NotificationFormatter.class)
public class DefaultNotificationFormatter implements NotificationFormatter {
    private final ExceptionNotifyProperties properties;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DefaultNotificationFormatter(ExceptionNotifyProperties properties) {
        this.properties = properties;
    }

    @Override
    public String format(ExceptionInfo exceptionInfo) {
        StringBuilder sb = new StringBuilder();
        
        // Format title as Markdown heading
        String title = properties.getNotification().getTitleTemplate()
                .replace("${appName}", exceptionInfo.getAppName());
        sb.append("# ").append(title).append("\n\n");
        
        // Horizontal rule in Markdown
        sb.append("---\n\n");
        
        // Format exception details in Markdown
        sb.append("**异常时间：** ").append(exceptionInfo.getTime().format(DATE_FORMATTER)).append("\n\n");
        sb.append("**异常类型：** ").append(exceptionInfo.getType()).append("\n\n");
        sb.append("**异常描述：** ").append(exceptionInfo.getMessage()).append("\n\n");
        sb.append("**异常位置：** ").append(exceptionInfo.getLocation()).append("\n\n");
        
        // Format environment if available
        if (exceptionInfo.getEnvironment() != null && !exceptionInfo.getEnvironment().isEmpty()) {
            sb.append("**当前环境：** ").append(exceptionInfo.getEnvironment()).append("\n\n");
        }
        
        // Add branch information from GitHub or Gitee configuration
        String branch = null;
        if (properties.getGithub() != null && properties.getGithub().getToken() != null && properties.getGithub().getBranch() != null) {
            branch = properties.getGithub().getBranch();
        } else if (properties.getGitee() != null && properties.getGitee().getToken() != null && properties.getGitee().getBranch() != null) {
            branch = properties.getGitee().getBranch();
        }
        
        if (branch != null && !branch.isEmpty()) {
            sb.append("**当前分支：** ").append(branch).append("\n\n");
        }
        
        // Format author info if available
        CodeAuthorInfo authorInfo = exceptionInfo.getAuthorInfo();
        if (authorInfo != null) {
            sb.append("**代码提交者：** ").append(authorInfo.getName())
                    .append(" (").append(authorInfo.getEmail()).append(")\n\n");
            
            if (authorInfo.getLastCommitTime() != null) {
                sb.append("**最后提交时间：** ").append(authorInfo.getLastCommitTime().format(DATE_FORMATTER)).append("\n\n");
            }

            if(authorInfo.getCommitMessage() != null){
                sb.append("**提交信息：** ").append(authorInfo.getCommitMessage());
            }
        }
        
        // Format trace ID if available
        if (exceptionInfo.getTraceId() != null && !exceptionInfo.getTraceId().isEmpty()) {
            sb.append("**TraceID：** ").append(exceptionInfo.getTraceId()).append("\n\n");

            // Include CLS trace URL as a clickable link if available
            if (exceptionInfo.getTraceUrl() != null && !exceptionInfo.getTraceUrl().isEmpty()) {
                sb.append("**云日志链路：** [点击查看日志](").append(exceptionInfo.getTraceUrl()).append(")\n\n");
            }
        }

        // Format AI suggestion if available
        if (exceptionInfo.getAiSuggestion() != null && !exceptionInfo.getAiSuggestion().isEmpty()) {
            sb.append("---\n\n");
            sb.append("### AI 建议：\n\n");
            sb.append(exceptionInfo.getAiSuggestion()).append("\n\n");
        }

        sb.append("---\n\n");
        
        // Format stacktrace if enabled
        if (properties.getNotification().isIncludeStacktrace() && exceptionInfo.getStacktrace() != null) {
            sb.append("### 堆栈信息：\n\n");
            sb.append("```java\n");
            
            // Limit stacktrace lines if configured
            int maxLines = properties.getNotification().getMaxStacktraceLines();
            if (maxLines > 0) {
                String[] lines = exceptionInfo.getStacktrace().split("\n");
                String limitedStacktrace = Arrays.stream(lines)
                        .limit(maxLines)
                        .collect(Collectors.joining("\n"));
                sb.append(limitedStacktrace);
                
                if (lines.length > maxLines) {
                    sb.append("\n... (").append(lines.length - maxLines).append(" more lines)");
                }
            } else {
                sb.append(exceptionInfo.getStacktrace());
            }
            
            sb.append("\n```");
        }
        
        return sb.toString();
    }
} 