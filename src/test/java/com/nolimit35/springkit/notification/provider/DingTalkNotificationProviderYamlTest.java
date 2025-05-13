package com.nolimit35.springkit.notification.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基于 YAML 配置的钉钉通知提供者测试
 * 需要设置 DINGTALK_WEBHOOK 环境变量才能运行
 */
@TestPropertySource(properties = {
        "spring.config.location=classpath:application-test.yml"
})
class DingTalkNotificationProviderYamlTest extends YamlConfigNotificationProviderTest {

    private DingTalkNotificationProvider provider;

    @BeforeEach
    void setUpProvider() {
        // 创建提供者实例
        provider = new DingTalkNotificationProvider(properties, formatter);
    }

    @Test
    void testIsEnabled() {
        // 如果环境变量存在，应该是启用的
        assertTrue(provider.isEnabled());
        
        // 验证配置是否正确加载
        assertEquals(System.getenv("DINGTALK_WEBHOOK"), properties.getDingtalk().getWebhook());
    }

    @Test
    void testSendNotification() throws Exception {
        // 发送真实的通知请求
        boolean result = provider.doSendNotification(exceptionInfo);

        // 断言
        assertTrue(result, "通知应该成功发送");
    }
} 