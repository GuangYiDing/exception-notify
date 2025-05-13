package com.nolimit35.springkit.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.AbstractNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.nolimit35.springkit.formatter.DefaultNotificationFormatter.DATE_FORMATTER;

/**
 * Feishu implementation of NotificationProvider
 */
@Slf4j
@Component
public class FeishuNotificationProvider extends AbstractNotificationProvider {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final NotificationFormatter formatter;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public FeishuNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        super(properties);
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.formatter = formatter;
    }

    @Override
    protected boolean doSendNotification(ExceptionInfo exceptionInfo) throws Exception {
        String webhook = properties.getFeishu().getWebhook();

        // 飞书机器人不能直接支持 markdown 格式,不使用 formatter 格式化,直接使用原始的异常信息
        // String content = formatter.format(exceptionInfo);
        StringBuilder sb = new StringBuilder();

        // Format title as Markdown heading
        String title = properties.getNotification().getTitleTemplate()
                .replace("${appName}", exceptionInfo.getAppName());
        sb.append(title).append("\n");

        // Format exception details in Markdown
        sb.append("异常时间：").append(exceptionInfo.getTime().format(DATE_FORMATTER)).append("\n");
        sb.append("异常类型：").append(exceptionInfo.getType()).append("\n");
        sb.append("异常描述：").append(exceptionInfo.getMessage()).append("\n");
        sb.append("异常位置：").append(exceptionInfo.getLocation()).append("\n");

        // Format environment if available
        if (exceptionInfo.getEnvironment() != null && !exceptionInfo.getEnvironment().isEmpty()) {
            sb.append("当前环境：").append(exceptionInfo.getEnvironment()).append("\n");
        }

        // Add branch information from GitHub or Gitee configuration
        String branch = null;
        if (properties.getGithub() != null && properties.getGithub().getToken() != null && properties.getGithub().getBranch() != null) {
            branch = properties.getGithub().getBranch();
        } else if (properties.getGitee() != null && properties.getGitee().getToken() != null && properties.getGitee().getBranch() != null) {
            branch = properties.getGitee().getBranch();
        }

        if (branch != null && !branch.isEmpty()) {
            sb.append("当前分支：").append(branch).append("\n");
        }

        // Format author info if available
        CodeAuthorInfo authorInfo = exceptionInfo.getAuthorInfo();
        if (authorInfo != null) {
            sb.append("代码提交者：").append(authorInfo.getName())
                    .append(" (").append(authorInfo.getEmail()).append(")\n");

            if (authorInfo.getLastCommitTime() != null) {
                sb.append("最后提交时间：").append(authorInfo.getLastCommitTime().format(DATE_FORMATTER)).append("\n");
            }

            if(authorInfo.getCommitMessage() != null){
                sb.append("提交信息：").append(authorInfo.getCommitMessage());
            }
        }

        // Format trace ID if available
        if (exceptionInfo.getTraceId() != null && !exceptionInfo.getTraceId().isEmpty()) {
            sb.append("TraceID：").append(exceptionInfo.getTraceId()).append("\n");

            // Include CLS trace URL as a clickable link if available
            if (exceptionInfo.getClsTraceUrl() != null && !exceptionInfo.getClsTraceUrl().isEmpty()) {
                sb.append("云日志链路：").append(exceptionInfo.getClsTraceUrl()).append("\n");
            }
        }

        // Format stacktrace if enabled
        if (properties.getNotification().isIncludeStacktrace() && exceptionInfo.getStacktrace() != null) {
            sb.append("堆栈信息：\n");

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
        }

        // 添加处理人信息
        if (exceptionInfo.getAuthorInfo() != null && StringUtils.hasText(exceptionInfo.getAuthorInfo().getEmail()) &&
            properties.getFeishu().getAt() != null && properties.getFeishu().getAt().isEnabled()) {
            if (properties.getFeishu().getAt().getOpenIdMappingGitEmail() != null
                && !properties.getFeishu().getAt().getOpenIdMappingGitEmail().isEmpty()) {
                // at 具体用户
                String feishuOpenId = properties.getFeishu().getAt().getOpenIdMappingGitEmail().entrySet().stream()
                        // 根据邮箱匹配对应的企微用户id
                        .filter(entry -> entry.getValue().contains(exceptionInfo.getAuthorInfo().getEmail()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (StringUtils.hasText(feishuOpenId)) {
                   sb.append(String.format("\n处理人: <at user_id=\"%s\">名字</at>", feishuOpenId));
                }
            }
        }

        // Build request body according to Feishu bot API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("msg_type", "text");

        Map<String, String> contentMap = new HashMap<>();
        contentMap.put("text", sb.toString());
        requestBody.put("content", contentMap);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(webhook)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to send Feishu notification: {}", response.code());
                return false;
            }

            String responseBody = response.body().string();
            log.debug("Feishu response: {}", responseBody);
            return true;
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() &&
               properties.getFeishu().getWebhook() != null &&
               !properties.getFeishu().getWebhook().isEmpty();
    }
}