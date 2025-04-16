package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import com.nolimit35.springkit.model.ExceptionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analyzing exceptions
 */
@Slf4j
@Service
public class ExceptionAnalyzerService {
    private final List<GitSourceControlService> gitSourceControlServices;
    private final ExceptionNotifyProperties properties;
   
    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public ExceptionAnalyzerService(List<GitSourceControlService> gitSourceControlServices, ExceptionNotifyProperties properties) {
        this.gitSourceControlServices = gitSourceControlServices;
        this.properties = properties;
    }

    /**
     * Analyze exception and create ExceptionInfo
     *
     * @param throwable the exception to analyze
     * @param traceId the trace ID (optional)
     * @return exception information
     */
    public ExceptionInfo analyzeException(Throwable throwable, String traceId) {
        // Get exception details
        String exceptionType = throwable.getClass().getName();
        String message = throwable.getMessage();
        String stacktrace = getStackTraceAsString(throwable);
        
        // Find the first application-specific stack trace element
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        StackTraceElement firstAppElement = findFirstApplicationElement(stackTraceElements);

        String location = null;
        CodeAuthorInfo authorInfo = null;

        if (firstAppElement != null) {
            location = firstAppElement.getClassName() + "." + firstAppElement.getMethodName() +
                       "(" + firstAppElement.getFileName() + ":" + firstAppElement.getLineNumber() + ")";
            
            // Get author information from available git source control services
            try {
                String fileName = convertClassNameToFilePath(firstAppElement.getClassName()) + ".java";
                authorInfo = findAuthorInfo(fileName, firstAppElement.getLineNumber());
            } catch (Exception e) {
                log.error("Error getting author information", e);
            }
        }
        
        // Generate Tencent CLS trace URL if trace is enabled and CLS config is available
        String clsTraceUrl = null;
        if (properties.getTrace().isEnabled() && traceId != null && !traceId.isEmpty()) {
            clsTraceUrl = generateClsTraceUrl(traceId);
        }
        
        // Build exception info
        return ExceptionInfo.builder()
                .time(LocalDateTime.now())
                .type(exceptionType)
                .message(message != null ? message : "No message")
                .location(location)
                .stacktrace(stacktrace)
                .traceId(traceId)
                .appName(applicationName)
                .environment(properties.getEnvironment().getCurrent())
                .authorInfo(authorInfo)
                .clsTraceUrl(clsTraceUrl)
                .build();
    }
    
    /**
     * Find author information by trying all available git source control services
     * 
     * @param fileName the file name
     * @param lineNumber the line number
     * @return author information or null if not found
     */
    private CodeAuthorInfo findAuthorInfo(String fileName, int lineNumber) {
        for (GitSourceControlService service : gitSourceControlServices) {
            CodeAuthorInfo authorInfo = service.getAuthorInfo(fileName, lineNumber);
            if (authorInfo != null) {
                return authorInfo;
            }
        }
        return null;
    }
    
    /**
     * Generate Tencent CLS trace URL
     *
     * @param traceId the trace ID
     * @return the CLS trace URL or null if not configured
     */
    private String generateClsTraceUrl(String traceId) {
        String region = properties.getTencentcls().getRegion();
        String topicId = properties.getTencentcls().getTopicId();
        
        if (region != null && !region.isEmpty() && topicId != null && !topicId.isEmpty()) {

            String interactiveQuery =
                    String.format("{\"filters\":[{\"key\":\"traceId\",\"grammarName\":\"INCLUDE\",\"values\":[{\"values\":[{\"value\":\"%s\",\"isPartialEscape\":true}],\"isOpen\":false}],\"alias_name\":\"traceId\",\"cnName\":\"\"}],\"sql\":{\"quotas\":[],\"dimensions\":[],\"sequences\":[],\"limit\":1000,\"samplingRate\":1},\"sqlStr\":\"\"}",traceId);

            String interactiveQueryBase64 = Base64.getEncoder().encodeToString(interactiveQuery.getBytes(StandardCharsets.UTF_8));

            return String.format("https://console.cloud.tencent.com/cls/search?region=%s&topic_id=%s&interactiveQueryBase64=%s",
                                region, topicId, interactiveQueryBase64) + "&time=now%2Fd,now%2Fd";
        }
        
        return null;
    }
    
    /**
     * Find the first application-specific stack trace element
     *
     * @param stackTraceElements the stack trace elements
     * @return the first application-specific element or null if not found
     */
    private StackTraceElement findFirstApplicationElement(StackTraceElement[] stackTraceElements) {
        // Check if package filtering is enabled
        if (properties.getPackageFilter().isEnabled() && !properties.getPackageFilter().getIncludePackages().isEmpty()) {
            // Filter based on configured packages
            for (StackTraceElement element : stackTraceElements) {
                String className = element.getClassName();
                
                // Check if the class belongs to any of the configured packages
                for (String packageName : properties.getPackageFilter().getIncludePackages()) {
                    if (className.startsWith(packageName)) {
                        return element;
                    }
                }
            }
            
            // If no stack trace element matches the configured packages, return the first element
            return stackTraceElements.length > 0 ? stackTraceElements[0] : null;
        } else {
            // Original behavior if package filtering is not enabled
            for (StackTraceElement element : stackTraceElements) {
                String className = element.getClassName();
                
                // Skip common framework packages
                if (!className.startsWith("java.") && 
                    !className.startsWith("javax.") && 
                    !className.startsWith("sun.") && 
                    !className.startsWith("com.sun.") &&
                    !className.startsWith("org.springframework.") && 
                    !className.startsWith("org.apache.") &&
                    !className.startsWith("com.nolimit35.springkit")) {
                    return element;
                }
            }
            
            // If no application-specific element found, return the first element
            return stackTraceElements.length > 0 ? stackTraceElements[0] : null;
        }
    }
    
    /**
     * Convert class name to file path
     *
     * @param className the class name
     * @return file path
     */
    private String convertClassNameToFilePath(String className) {
        return className.replace('.', '/');
    }
    
    /**
     * Get stack trace as string
     *
     * @param throwable the exception
     * @return stack trace as string
     */
    private String getStackTraceAsString(Throwable throwable) {
        return Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
    }
} 