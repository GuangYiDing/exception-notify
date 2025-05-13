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

import java.util.HashMap;
import java.util.Map;

/**
 * WeChat Work implementation of NotificationProvider
 */
@Slf4j
@Component
public class WeChatWorkNotificationProvider extends AbstractNotificationProvider {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final NotificationFormatter formatter;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public WeChatWorkNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        super(properties);
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.formatter = formatter;
    }

    @Override
    protected boolean doSendNotification(ExceptionInfo exceptionInfo) throws Exception {
        String webhook = properties.getWechatwork().getWebhook();

        // Format the exception info into a notification
        String content = formatter.format(exceptionInfo);

        // 添加处理人信息
        if (exceptionInfo.getAuthorInfo() != null && StringUtils.hasText(exceptionInfo.getAuthorInfo().getEmail()) &&
                properties.getWechatwork().getAt() != null && properties.getWechatwork().getAt().isEnabled()) {

            if (properties.getWechatwork().getAt().getUserIdMappingGitEmail() != null
                    && !properties.getWechatwork().getAt().getUserIdMappingGitEmail().isEmpty()) {
                // at 具体用户
                String qwUserId = properties.getWechatwork().getAt().getUserIdMappingGitEmail().entrySet().stream()
                        // 根据邮箱匹配对应的企微用户id
                        .filter(entry -> entry.getValue().contains(exceptionInfo.getAuthorInfo().getEmail()))
                        .map(entryKey -> {
                            // 处理 yaml 配置中 key 为 [@] 会序列化为 .@. 的情况
                            if (entryKey.getKey().contains(".@.")) {
                                return entryKey.getKey().replace(".@.", "@");
                            }
                            return entryKey.getKey();
                        })
                        .findFirst()
                        .orElse(null);

                if (StringUtils.hasText(qwUserId)) {
                    content += new StringBuilder("\n**处理人：** <@").append(qwUserId).append(">\n");
                }
            }
        }

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
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() &&
                properties.getWechatwork().getWebhook() != null &&
                !properties.getWechatwork().getWebhook().isEmpty();
    }
}