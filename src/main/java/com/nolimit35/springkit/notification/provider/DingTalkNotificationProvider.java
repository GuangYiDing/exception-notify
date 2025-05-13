package com.nolimit35.springkit.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.AbstractNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DingTalk implementation of NotificationProvider
 */
@Slf4j
@Component
public class DingTalkNotificationProvider extends AbstractNotificationProvider {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final NotificationFormatter formatter;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public DingTalkNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        super(properties);
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.formatter = formatter;
    }

    @Override
    protected boolean doSendNotification(ExceptionInfo exceptionInfo) throws Exception {
        String webhook = properties.getDingtalk().getWebhook();
        
        // Format the exception info into a notification
        String content = formatter.format(exceptionInfo);
        
        // Extract title from the first line of content
        String title = content.split("\n")[0];

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("msgtype", "markdown");
        
        // Format content as markdown
        String formattedContent = "#### " + title + "\n" + content;

        Map<String, Object> text = new HashMap<>();
        text.put("text", formattedContent);
        text.put("title", "异常告警");


        // 添加处理人信息
        if (exceptionInfo.getAuthorInfo() != null && StringUtils.hasText(exceptionInfo.getAuthorInfo().getEmail()) &&
            properties.getDingtalk().getAt() != null && properties.getDingtalk().getAt().isEnabled()) {

            if (properties.getDingtalk().getAt().getUserIdMappingGitEmail() != null
                && !properties.getDingtalk().getAt().getUserIdMappingGitEmail().isEmpty()) {
                // at 具体用户
                String dingUserId = properties.getDingtalk().getAt().getUserIdMappingGitEmail().entrySet().stream()
                        // 根据邮箱匹配对应的企微用户id
                        .filter(entry -> entry.getValue().contains(exceptionInfo.getAuthorInfo().getEmail()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (StringUtils.hasText(dingUserId)) {
                    Map<String, List<String>> atUserId = new HashMap<>();
                    atUserId.put("atUserIds", Collections.singletonList(dingUserId));
                    requestBody.put("at", atUserId);
                }
            }
        }

        requestBody.put("markdown", text);


        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        Request request = new Request.Builder()
            .url(webhook)
            .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to send DingTalk notification: {}", response.code());
                return false;
            }

            String responseBody = response.body().string();
            log.debug("DingTalk response: {}", responseBody);
            return true;
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && 
               properties.getDingtalk().getWebhook() != null && 
               !properties.getDingtalk().getWebhook().isEmpty();
    }
} 