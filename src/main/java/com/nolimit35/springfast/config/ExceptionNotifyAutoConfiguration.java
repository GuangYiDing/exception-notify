package com.nolimit35.springfast.config;

import com.nolimit35.springfast.filter.DefaultExceptionFilter;
import com.nolimit35.springfast.filter.ExceptionFilter;
import com.nolimit35.springfast.formatter.DefaultNotificationFormatter;
import com.nolimit35.springfast.formatter.NotificationFormatter;
import com.nolimit35.springfast.handler.GlobalExceptionHandler;
import com.nolimit35.springfast.service.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for Exception-Notify
 */
@Configuration
@EnableConfigurationProperties(ExceptionNotifyProperties.class)
@ConditionalOnProperty(prefix = "exception.notify", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    DefaultExceptionFilter.class,
    DefaultNotificationFormatter.class,
    GlobalExceptionHandler.class
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
    public DingTalkService dingTalkService(ExceptionNotifyProperties properties) {
        return new DingTalkService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public WeChatWorkService weChatWorkService(ExceptionNotifyProperties properties) {
        return new WeChatWorkService(properties);
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
    public ExceptionNotificationService exceptionNotificationService(
            ExceptionNotifyProperties properties,
            ExceptionAnalyzerService analyzerService,
            DingTalkService dingTalkService,
            WeChatWorkService weChatWorkService,
            NotificationFormatter formatter,
            ExceptionFilter filter,
            EnvironmentProvider environmentProvider) {
        ExceptionNotificationService notificationService = new ExceptionNotificationService(properties, analyzerService, dingTalkService, weChatWorkService, formatter, filter, environmentProvider);
        log.info("异常通知组件已注入 :) ");
        return notificationService;
    }
} 