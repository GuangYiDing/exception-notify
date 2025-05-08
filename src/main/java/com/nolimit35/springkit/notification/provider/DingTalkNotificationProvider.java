package com.nolimit35.springkit.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.AbstractNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
        
        Map<String, String> text = new HashMap<>();
        text.put("text", formattedContent);
        text.put("title", "异常告警");
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