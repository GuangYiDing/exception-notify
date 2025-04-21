package com.nolimit35.springkit.monitor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.NotificationProviderManager;

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
    
    @Value("${spring.application.name:unknown}")
    private String applicationName;
    
    private static String appName = "unknown";

    @Autowired
    public Monitor(NotificationProviderManager notificationManager, 
                  @Value("${spring.application.name:unknown}") String applicationName,
                  ExceptionNotifyProperties properties) {
        Monitor.notificationManager = notificationManager;
        Monitor.appName = applicationName;
        Monitor.properties = properties;
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
        if (properties != null && properties.getTrace().isEnabled()) {
            // 首先从MDC中获取traceId
            traceId = MDC.get("traceId");
            
            // 如果MDC中没有traceId，尝试从请求头中获取
            if ((traceId == null || traceId.isEmpty()) && properties.getTrace().getHeaderName() != null) {
                traceId = getTraceIdFromHeader(properties.getTrace().getHeaderName());
            }
            
            if (traceId != null && !traceId.isEmpty()) {
                builder.traceId(traceId);
                
                // 生成腾讯云日志CLS的链接
                String clsTraceUrl = generateClsTraceUrl(traceId);
                if (clsTraceUrl != null) {
                    builder.clsTraceUrl(clsTraceUrl);
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
    
    /**
     * 从请求头中获取traceId
     *
     * @param headerName 头部名称
     * @return traceId或null
     */
    private static String getTraceIdFromHeader(String headerName) {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                String traceId = request.getHeader(headerName);
                
                if (traceId != null && !traceId.isEmpty()) {
                    return traceId;
                }
            }
        } catch (Exception e) {
            log.debug("从请求头获取traceId失败", e);
        }
        return null;
    }
    
    /**
     * 生成腾讯云日志CLS链接
     *
     * @param traceId 追踪ID
     * @return CLS链接或null如果未配置
     */
    private static String generateClsTraceUrl(String traceId) {
        if (properties == null || properties.getTencentcls() == null) {
            return null;
        }
        
        String region = properties.getTencentcls().getRegion();
        String topicId = properties.getTencentcls().getTopicId();
        
        if (region != null && !region.isEmpty() && topicId != null && !topicId.isEmpty()) {
            // 构建查询JSON
            String interactiveQuery = String.format(
                "{\"filters\":[{\"key\":\"traceId\",\"grammarName\":\"INCLUDE\",\"values\":[{\"values\":[{\"value\":\"%s\",\"isPartialEscape\":true}],\"isOpen\":false}],\"alias_name\":\"traceId\",\"cnName\":\"\"}],\"sql\":{\"quotas\":[],\"dimensions\":[],\"sequences\":[],\"limit\":1000,\"samplingRate\":1},\"sqlStr\":\"\"}",
                traceId
            );
            
            // Base64编码查询参数
            String interactiveQueryBase64 = Base64.getEncoder().encodeToString(
                interactiveQuery.getBytes(StandardCharsets.UTF_8)
            );
            
            // 构建完整链接
            return String.format(
                "https://console.cloud.tencent.com/cls/search?region=%s&topic_id=%s&interactiveQueryBase64=%s",
                region, topicId, interactiveQueryBase64
            ) + "&time=now%2Fd,now%2Fd";
        }
        
        return null;
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