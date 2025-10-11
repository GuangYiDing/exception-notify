package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for AI suggestion service
 */
@SpringBootTest
class AiSuggestionServiceTest {

    private ExceptionNotifyProperties properties;
    private OpenAiSuggestionService aiSuggestionService;

    @BeforeEach
    void setUp() {
        properties = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.AI aiConfig = new ExceptionNotifyProperties.AI();
        aiConfig.setEnabled(true);
        aiConfig.setApiKey("test-api-key");
        aiConfig.setApiUrl("https://api.openai.com/v1/chat/completions");
        aiConfig.setModel("gpt-3.5-turbo");
        properties.setAi(aiConfig);

        aiSuggestionService = new OpenAiSuggestionService(properties);
    }

    @Test
    void testIsAvailable_WhenConfigured() {
        assertTrue(aiSuggestionService.isAvailable());
    }

    @Test
    void testIsAvailable_WhenNotConfigured() {
        ExceptionNotifyProperties.AI aiConfig = new ExceptionNotifyProperties.AI();
        aiConfig.setEnabled(false);
        properties.setAi(aiConfig);

        OpenAiSuggestionService service = new OpenAiSuggestionService(properties);
        assertFalse(service.isAvailable());
    }

    @Test
    void testIsAvailable_WhenApiKeyMissing() {
        ExceptionNotifyProperties.AI aiConfig = new ExceptionNotifyProperties.AI();
        aiConfig.setEnabled(true);
        aiConfig.setApiKey(null);
        properties.setAi(aiConfig);

        OpenAiSuggestionService service = new OpenAiSuggestionService(properties);
        assertFalse(service.isAvailable());
    }

    @Test
    void testGetSuggestion_WithoutCodeContext() {
        // Note: This test will fail without a real API key
        // In production, you would mock the RestTemplate
        String exceptionType = "java.lang.NullPointerException";
        String message = "Cannot invoke method on null object";
        String stacktrace = "at com.example.MyClass.myMethod(MyClass.java:42)\n" +
                           "at com.example.Main.main(Main.java:10)";

        // This will return null because we don't have a real API key
        String suggestion = aiSuggestionService.getSuggestion(
            exceptionType,
            message,
            stacktrace,
            null
        );

        // With a mock API key, this should return null
        // In a real test with mocking, you would verify the behavior
        assertNull(suggestion);
    }
}
