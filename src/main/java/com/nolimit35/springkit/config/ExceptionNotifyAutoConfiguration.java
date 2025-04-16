package com.nolimit35.springkit.config;

import com.nolimit35.springkit.aspect.ExceptionNotificationAspect;
import com.nolimit35.springkit.filter.DefaultExceptionFilter;
import com.nolimit35.springkit.filter.ExceptionFilter;
import com.nolimit35.springkit.formatter.DefaultNotificationFormatter;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.notification.NotificationProviderManager;
import com.nolimit35.springkit.notification.provider.DingTalkNotificationProvider;
import com.nolimit35.springkit.notification.provider.FeishuNotificationProvider;
import com.nolimit35.springkit.notification.provider.WeChatWorkNotificationProvider;
import com.nolimit35.springkit.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Auto-configuration for Exception-Notify
 */
@Configuration
@EnableConfigurationProperties(ExceptionNotifyProperties.class)
@ConditionalOnProperty(prefix = "exception.notify", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    DefaultExceptionFilter.class,
    DefaultNotificationFormatter.class
})
@Slf4j
public class ExceptionNotifyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GitHubService gitHubService(ExceptionNotifyProperties properties) {
        return new GitHubService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public GiteeService giteeService(ExceptionNotifyProperties properties) {
        return new GiteeService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EnvironmentProvider environmentProvider(Environment environment) {
        return new EnvironmentProvider(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionAnalyzerService exceptionAnalyzerService(List<GitSourceControlService> gitSourceControlServices, ExceptionNotifyProperties properties) {
        return new ExceptionAnalyzerService(gitSourceControlServices, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationProviderManager notificationProviderManager(List<com.nolimit35.springkit.notification.NotificationProvider> providers) {
        return new NotificationProviderManager(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public DingTalkNotificationProvider dingTalkNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        return new DingTalkNotificationProvider(properties, formatter);
    }

    @Bean
    @ConditionalOnMissingBean
    public FeishuNotificationProvider feishuNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        return new FeishuNotificationProvider(properties, formatter);
    }

    @Bean
    @ConditionalOnMissingBean
    public WeChatWorkNotificationProvider weChatWorkNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        return new WeChatWorkNotificationProvider(properties, formatter);
    }


    @Bean
    @ConditionalOnMissingBean
    public ExceptionNotificationAspect exceptionNotificationAspect(ExceptionNotificationService notificationService) {
        return new ExceptionNotificationAspect(notificationService);
    }


    @Bean
    @ConditionalOnMissingBean
    public ExceptionNotificationService exceptionNotificationService(
            ExceptionNotifyProperties properties,
            ExceptionAnalyzerService analyzerService,
            NotificationProviderManager notificationManager,
            NotificationFormatter formatter,
            ExceptionFilter filter,
            EnvironmentProvider environmentProvider) {
        ExceptionNotificationService notificationService = new ExceptionNotificationService(
                properties, analyzerService, notificationManager, formatter, filter, environmentProvider);
        log.info("异常通知组件已注入 :) ");
        return notificationService;
    }
} 