package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI GPT 建议服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "exception.notify.ai", name = "enabled", havingValue = "true")
public class OpenAiSuggestionService implements AiSuggestionService {

    private final ExceptionNotifyProperties properties;
    private final RestTemplate restTemplate;

    public OpenAiSuggestionService(ExceptionNotifyProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();

        // 设置超时时间
        if (properties.getAi() != null && properties.getAi().getTimeout() > 0) {
            this.restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("Connection", "close");
                return execution.execute(request, body);
            });
        }
    }

    @Override
    public String getSuggestion(String exceptionType, String exceptionMessage, String stacktrace, String codeContext) {
        if (!isAvailable()) {
            log.debug("AI service is not available");
            return null;
        }

        try {
            ExceptionNotifyProperties.AI aiConfig = properties.getAi();

            // 构建提示词
            String prompt = buildPrompt(exceptionType, exceptionMessage, stacktrace, codeContext);

            // 调用 OpenAI API
            String suggestion = callOpenAiApi(prompt, aiConfig);

            return suggestion;
        } catch (Exception e) {
            log.error("Failed to get AI suggestion", e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        ExceptionNotifyProperties.AI aiConfig = properties.getAi();
        return aiConfig != null
            && aiConfig.isEnabled()
            && aiConfig.getApiKey() != null
            && !aiConfig.getApiKey().isEmpty();
    }

    /**
     * 构建发送给 AI 的提示词
     */
    private String buildPrompt(String exceptionType, String exceptionMessage, String stacktrace, String codeContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个 Java 异常分析专家。请根据以下异常信息，提供简洁的分析和修复建议：\n\n");

        prompt.append("异常类型：").append(exceptionType).append("\n");
        prompt.append("异常消息：").append(exceptionMessage).append("\n\n");

        prompt.append("堆栈信息：\n");
        prompt.append(limitStacktrace(stacktrace, 15)).append("\n\n");

        if (codeContext != null && !codeContext.isEmpty()) {
            prompt.append("异常位置代码上下文：\n");
            prompt.append(codeContext).append("\n\n");
        }

        prompt.append("请提供：\n");
        prompt.append("1. 异常原因分析（1-2句话）\n");
        prompt.append("2. 可能的修复建议（2-3个要点）\n");
        prompt.append("\n请用中文回复，保持简洁明了。");

        return prompt.toString();
    }

    /**
     * 限制堆栈信息的行数
     */
    private String limitStacktrace(String stacktrace, int maxLines) {
        if (stacktrace == null) {
            return "";
        }

        String[] lines = stacktrace.split("\n");
        if (lines.length <= maxLines) {
            return stacktrace;
        }

        StringBuilder limited = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            limited.append(lines[i]).append("\n");
        }
        limited.append("... (省略 ").append(lines.length - maxLines).append(" 行)");

        return limited.toString();
    }

    /**
     * 调用 OpenAI API
     */
    private String callOpenAiApi(String prompt, ExceptionNotifyProperties.AI aiConfig) {
        try {
            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);

            requestBody.put("max_tokens", aiConfig.getMaxTokens());
            requestBody.put("temperature", aiConfig.getTemperature());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                aiConfig.getApiUrl(),
                entity,
                Map.class
            );

            // 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> messageObj = (Map<String, String>) firstChoice.get("message");

                    if (messageObj != null) {
                        return messageObj.get("content");
                    }
                }
            }

            log.warn("Failed to parse AI response: {}", response);
            return null;

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return null;
        }
    }
}
