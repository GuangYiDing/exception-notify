package com.nolimit35.springkit.notification.provider;

import com.nolimit35.springkit.notification.NotificationProvider;
import com.nolimit35.springkit.notification.NotificationProviderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基于 YAML 配置的所有通知提供者组合测试
 */
@TestPropertySource(properties = {
		"spring.config.location=classpath:application-test.yml"
})
class AllProvidersYamlTest extends YamlConfigNotificationProviderTest {

	private DingTalkNotificationProvider dingTalkProvider;
	private FeishuNotificationProvider feishuProvider;
	private WeChatWorkNotificationProvider weChatWorkProvider;
	private NotificationProviderManager providerManager;

	@BeforeEach
	void setUpProviders() {
		// 创建提供者实例
		dingTalkProvider = new DingTalkNotificationProvider(properties, formatter);
		feishuProvider = new FeishuNotificationProvider(properties, formatter);
		weChatWorkProvider = new WeChatWorkNotificationProvider(properties, formatter);

		// 创建提供者管理器，包含所有提供者
		List<NotificationProvider> providers = Arrays.asList(
				dingTalkProvider,
				feishuProvider,
				weChatWorkProvider
		);
		providerManager = new NotificationProviderManager(providers);
	}

	@Test
	void testProviderEnabledStatus() {
		// 检查提供者是否根据 webhook 配置正确启用
		assertTrue(dingTalkProvider.isEnabled());

		assertTrue(feishuProvider.isEnabled());

		assertTrue(weChatWorkProvider.isEnabled());
	}

	@Test
	void testSendNotificationToAllEnabledProviders() {
		// 通过提供者管理器发送通知到所有启用的提供者
		boolean result = providerManager.sendNotification(exceptionInfo);

		// 如果任何提供者已启用，结果应该为 true
		assertEquals(isAnyProviderEnabled(), result);
	}

	private boolean hasEnvironmentVariable(String name) {
		String value = System.getenv(name);
		return value != null && !value.isEmpty();
	}

	private boolean isAnyProviderEnabled() {
		return dingTalkProvider.isEnabled() ||
		       feishuProvider.isEnabled() ||
		       weChatWorkProvider.isEnabled();
	}
} 