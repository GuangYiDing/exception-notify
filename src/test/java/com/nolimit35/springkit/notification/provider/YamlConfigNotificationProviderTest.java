package com.nolimit35.springkit.notification.provider;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.formatter.DefaultNotificationFormatter;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import com.nolimit35.springkit.model.ExceptionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 从 YAML 配置文件加载 ExceptionNotifyProperties 的集成测试基类
 */
@SpringBootTest(classes = { TestApplication.class })
@EnableConfigurationProperties(ExceptionNotifyProperties.class)
@ActiveProfiles("test")
public abstract class YamlConfigNotificationProviderTest {

    @Autowired
    protected ExceptionNotifyProperties properties;

    protected NotificationFormatter formatter;
    protected ExceptionInfo exceptionInfo;

    @BeforeEach
    void setUp() {
        // 创建实际的 formatter
        formatter = new DefaultNotificationFormatter(properties);

        // 设置测试异常信息
        RuntimeException testException = new RuntimeException("测试异常，请忽略");

        exceptionInfo = ExceptionInfo.builder()
                .time(LocalDateTime.now())
                .type(testException.getClass().getName())
                .message(testException.getMessage())
                .location("com.nolimit35.springkit.test.IntegrationTest.testMethod(IntegrationTest.java:42)")
                .stacktrace(getStackTraceAsString(testException))
                .appName("springkit-test")
                .authorInfo(CodeAuthorInfo.builder().name("xxx").email("xxx@xx.com").build())
                .build();
    }

    /**
     * 辅助方法，将栈追踪转换为字符串
     */
    public String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }

        return sb.toString();
    }
}