package com.nolimit35.springkit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending notifications to DingTalk
 */
@Slf4j
@Service
public class DingTalkService {
    private final ExceptionNotifyProperties properties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public DingTalkService(ExceptionNotifyProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send notification to DingTalk
     *
     * @param title the notification title
     * @param content the notification content
     * @return true if successful, false otherwise
     */
    public boolean sendNotification(String title, String content) {
        if (properties.getDingtalk().getWebhook() == null) {
            log.warn("DingTalk webhook URL is not configured. Cannot send notification.");
            return false;
        }

        try {
            String webhook = properties.getDingtalk().getWebhook();
            
            // Add signature if secret is configured
            if (properties.getDingtalk().getSecret() != null && !properties.getDingtalk().getSecret().isEmpty()) {
                webhook = addSignature(webhook, properties.getDingtalk().getSecret());
            }

            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "markdown");
            
            Map<String, String> markdown = new HashMap<>();
            markdown.put("title", title);
            markdown.put("text", content);
            requestBody.put("markdown", markdown);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(webhook)
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
        } catch (Exception e) {
            log.error("Error sending DingTalk notification", e);
            return false;
        }
    }

    /**
     * Add signature to DingTalk webhook URL
     *
     * @param webhook the webhook URL
     * @param secret the secret for signature
     * @return webhook URL with signature
     */
    private String addSignature(String webhook, String secret) throws Exception {
        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
        
        return webhook + "&timestamp=" + timestamp + "&sign=" + sign;
    }
} 