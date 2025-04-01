package com.nolimit35.springfast.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springfast.config.ExceptionNotifyProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending notifications to WeChat Work
 */
@Slf4j
@Service
public class WeChatWorkService {
    private final ExceptionNotifyProperties properties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public WeChatWorkService(ExceptionNotifyProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send notification to WeChat Work
     *
     * @param title the notification title
     * @param content the notification content
     * @return true if successful, false otherwise
     */
    public boolean sendNotification(String title, String content) {
        if (properties.getWechatwork().getWebhook() == null) {
            log.warn("WeChat Work webhook URL is not configured. Cannot send notification.");
            return false;
        }

        try {
            String webhook = properties.getWechatwork().getWebhook();

            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "markdown");
            
            Map<String, String> markdown = new HashMap<>();
            markdown.put("content", content);
            requestBody.put("markdown", markdown);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(webhook)
                .post(RequestBody.create(jsonBody, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to send WeChat Work notification: {}", response.code());
                    return false;
                }
                
                String responseBody = response.body().string();
                log.debug("WeChat Work response: {}", responseBody);
                return true;
            }
        } catch (Exception e) {
            log.error("Error sending WeChat Work notification", e);
            return false;
        }
    }
} 