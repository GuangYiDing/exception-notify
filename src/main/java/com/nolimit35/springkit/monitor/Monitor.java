package com.nolimit35.springkit.monitor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.NotificationProviderManager;
import com.nolimit35.springkit.trace.TraceInfoProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * 监控工具，用于日志记录和发送通知
 * <p>
 * 该类提供类似于SLF4J日志的方法，同时通过exception-notify中配置的通知渠道发送通知
 */
@Slf4j
@Component
public class Monitor {

    private static NotificationProviderManager notificationManager;
    private static ExceptionNotifyProperties properties;
    private static TraceInfoProvider traceInfoProvider;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    private static String appName = "unknown";

    @Autowired
    public Monitor(NotificationProviderManager notificationManager,
                  @Value("${spring.application.name:unknown}") String applicationName,
                  ExceptionNotifyProperties properties,
                  TraceInfoProvider traceInfoProvider) {
        Monitor.notificationManager = notificationManager;
        Monitor.appName = applicationName;
        Monitor.properties = properties;
        Monitor.traceInfoProvider = traceInfoProvider;
    }

    /**
     * 记录信息消息并发送通知
     *
     * @param message 信息消息
     */
    public static void info(String message) {
        log.info(message);
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2通常是调用者（0是getStackTrace，1是当前方法）
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        sendNotification("INFO: " + message, null, caller);
    }

    /**
     * 使用自定义日志记录器记录信息消息并发送通知
     *
     * @param logger  要使用的日志记录器
     * @param message 信息消息
     */
    public static void info(Logger logger, String message) {
        logger.info(message);
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2通常是调用者（0是getStackTrace，1是当前方法）
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        sendNotification("INFO: " + message, null, caller);
    }

    /**
     * 记录带有异常的信息消息并发送通知
     *
     * @param message   信息消息
     * @param throwable 异常
     */
    public static void info(String message, Throwable throwable) {
        log.info(message, throwable);
        // 异常情况下，优先使用异常的堆栈信息，不需要额外获取调用者位置
        sendNotification("INFO: " + message, throwable);
    }

    /**
     * 使用自定义日志记录器记录带有异常的信息消息并发送通知
     *
     * @param logger    要使用的日志记录器
     * @param message   信息消息
     * @param throwable 异常
     */
    public static void info(Logger logger, String message, Throwable throwable) {
        logger.info(message, throwable);
        // 异常情况下，优先使用异常的堆栈信息，不需要额外获取调用者位置
        sendNotification("INFO: " + message, throwable);
    }

    /**
     * 记录警告消息并发送通知
     *
     * @param message 警告消息
     */
    public static void warn(String message) {
        log.warn(message);
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2通常是调用者（0是getStackTrace，1是当前方法）
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        sendNotification("WARN: " + message, null, caller);
    }

    /**
     * 记录带有异常的警告消息并发送通知
     *
     * @param message   警告消息
     * @param throwable 异常
     */
    public static void warn(String message, Throwable throwable) {
        log.warn(message, throwable);
        // 异常情况下，优先使用异常的堆栈信息，不需要额外获取调用者位置
        sendNotification("WARN: " + message, throwable);
    }

    /**
     * 使用自定义日志记录器记录警告消息并发送通知
     *
     * @param logger  要使用的日志记录器
     * @param message 警告消息
     */
    public static void warn(Logger logger, String message) {
        logger.warn(message);
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2通常是调用者（0是getStackTrace，1是当前方法）
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        sendNotification("WARN: " + message, null, caller);
    }

    /**
     * 使用自定义日志记录器记录带有异常的警告消息并发送通知
     *
     * @param logger    要使用的日志记录器
     * @param message   警告消息
     * @param throwable 异常
     */
    public static void warn(Logger logger, String message, Throwable throwable) {
        logger.warn(message, throwable);
        // 异常情况下，优先使用异常的堆栈信息，不需要额外获取调用者位置
        sendNotification("WARN: " + message, throwable);
    }

    /**
     * 记录错误消息并发送通知
     *
     * @param message 错误消息
     */
    public static void error(String message) {
        log.error(message);
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2通常是调用者（0是getStackTrace，1是当前方法）
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        sendNotification(message, null, caller);
    }

    /**
     * 记录带有异常的错误消息并发送通知
     *
     * @param message   错误消息
     * @param throwable 异常
     */
    public static void error(String message, Throwable throwable) {
        log.error(message, throwable);
        // 异常情况下，优先使用异常的堆栈信息，不需要额外获取调用者位置
        sendNotification(message, throwable);
    }

    /**
     * 使用自定义日志记录器记录错误消息并发送通知
     *
     * @param logger  要使用的日志记录器
     * @param message 错误消息
     */
    public static void error(Logger logger, String message) {
        logger.error(message);
        // 获取调用堆栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 索引2通常是调用者（0是getStackTrace，1是当前方法）
        StackTraceElement caller = stackTrace.length > 2 ? stackTrace[2] : null;
        sendNotification(message, null, caller);
    }

    /**
     * 使用自定义日志记录器记录带有异常的错误消息并发送通知
     *
     * @param logger    要使用的日志记录器
     * @param message   错误消息
     * @param throwable 异常
     */
    public static void error(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
        // 异常情况下，优先使用异常的堆栈信息，不需要额外获取调用者位置
        sendNotification(message, throwable);
    }

    /**
     * 获取指定类的日志记录器
     *
     * @param clazz 要获取日志记录器的类
     * @return 日志记录器
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 获取指定名称的日志记录器
     *
     * @param name 要获取日志记录器的名称
     * @return 日志记录器
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    private static void sendNotification(String message, Throwable throwable) {
        sendNotification(message, throwable, null);
    }

    private static void sendNotification(String message, Throwable throwable, StackTraceElement caller) {
        if (notificationManager == null) {
            log.warn("Monitor尚未完全初始化，通知将不会被发送");
            return;
        }

        try {
            ExceptionInfo exceptionInfo = buildExceptionInfo(message, throwable, caller);

            // 通过通知管理器发送通知
            notificationManager.sendNotification(exceptionInfo);
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }

    private static ExceptionInfo buildExceptionInfo(String message, Throwable throwable) {
        return buildExceptionInfo(message, throwable, null);
    }

    private static ExceptionInfo buildExceptionInfo(String message, Throwable throwable, StackTraceElement caller) {
        ExceptionInfo.ExceptionInfoBuilder builder = ExceptionInfo.builder()
                .time(LocalDateTime.now())
                .appName(appName)
                .message(message);

        // 如果配置中启用了trace，获取traceId
        String traceId = null;
        if (properties != null && properties.getTrace().isEnabled() && traceInfoProvider != null) {
            // 使用TraceInfoProvider获取traceId
            traceId = traceInfoProvider.getTraceId();

            if (traceId != null && !traceId.isEmpty()) {
                builder.traceId(traceId);

                // 使用TraceInfoProvider生成trace URL
                String traceUrl = traceInfoProvider.generateTraceUrl(traceId);
                if (traceUrl != null) {
                    builder.traceUrl(traceUrl);
                }
            }
        }

        if (throwable != null) {
            builder.type(throwable.getClass().getName())
                   .stacktrace(getStackTraceAsString(throwable));

            // 如果有异常堆栈信息，查找可用的第一个应用程序元素
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            if (stackTraceElements != null && stackTraceElements.length > 0) {
                // 仅使用堆栈跟踪中的第一个元素
                StackTraceElement element = stackTraceElements[0];
                builder.location(element.getClassName() + "." + element.getMethodName() +
                        "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
        } else {
            builder.type("MonitoredMessage");

            // 如果没有异常但有调用者信息，使用调用者信息
            if (caller != null) {
                builder.location(caller.getClassName() + "." + caller.getMethodName() +
                        "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")");
            }
        }

        return builder.build();
    }



    private static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        return Arrays.stream(throwable.getStackTrace())
                .limit(10) // 限制堆栈深度
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
    }
}