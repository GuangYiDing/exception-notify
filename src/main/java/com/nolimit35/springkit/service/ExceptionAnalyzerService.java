package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.AiAnalysisPayload;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.trace.TraceInfoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for analyzing exceptions
 */
@Slf4j
@Service
public class ExceptionAnalyzerService {
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final List<GitSourceControlService> gitSourceControlServices;
    private final ExceptionNotifyProperties properties;
    private final TraceInfoProvider traceInfoProvider;

    @Autowired(required = false)
    private AiAnalysisLinkService aiAnalysisLinkService;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public ExceptionAnalyzerService(List<GitSourceControlService> gitSourceControlServices,
                                  ExceptionNotifyProperties properties,
                                  TraceInfoProvider traceInfoProvider) {
        this.gitSourceControlServices = gitSourceControlServices;
        this.properties = properties;
        this.traceInfoProvider = traceInfoProvider;
    }

    /**
     * Analyze exception and create ExceptionInfo
     *
     * @param throwable the exception to analyze
     * @param traceId the trace ID (optional)
     * @return exception information
     */
    public ExceptionInfo analyzeException(Throwable throwable, String traceId) {
        LocalDateTime occurrenceTime = LocalDateTime.now();

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

        // Generate trace URL if trace is enabled and traceId is available
        String traceUrl = null;
        if (properties.getTrace().isEnabled() && traceId != null && !traceId.isEmpty()) {
            traceUrl = traceInfoProvider.generateTraceUrl(traceId);
        }

        // Capture code context if configured
        String codeContext = null;
        if (properties.getAi().isEnabled()
                && properties.getAi().isIncludeCodeContext()
                && firstAppElement != null) {
            String fileName = convertClassNameToFilePath(firstAppElement.getClassName()) + ".java";
            int contextLines = properties.getAi().getCodeContextLines();
            codeContext = getCodeContext(fileName, firstAppElement.getLineNumber(), contextLines);
        }

        // Build AI analysis link if enabled
        String aiAnalysisUrl = null;
        if (properties.getAi().isEnabled() && aiAnalysisLinkService != null && aiAnalysisLinkService.isAvailable()) {
            try {
                AiAnalysisPayload.AiAnalysisPayloadBuilder payloadBuilder = AiAnalysisPayload.builder()
                        .appName(applicationName)
                        .environment(properties.getEnvironment().getCurrent())
                        .occurrenceTime(occurrenceTime.format(ISO_DATE_TIME))
                        .exceptionType(exceptionType)
                        .exceptionMessage(message != null ? message : "No message")
                        .location(location)
                        .stacktrace(stacktrace)
                        .traceId(traceId)
                        .traceUrl(traceUrl);

                if (codeContext != null && !codeContext.isEmpty()) {
                    payloadBuilder.codeContext(codeContext);
                }

                if (authorInfo != null) {
                    payloadBuilder.author(AiAnalysisPayload.Author.builder()
                            .name(authorInfo.getName())
                            .email(authorInfo.getEmail())
                            .lastCommitTime(authorInfo.getLastCommitTime() != null
                                    ? authorInfo.getLastCommitTime().format(ISO_DATE_TIME)
                                    : null)
                            .fileName(authorInfo.getFileName())
                            .lineNumber(authorInfo.getLineNumber())
                            .commitMessage(authorInfo.getCommitMessage())
                            .build());
                }

                aiAnalysisUrl = aiAnalysisLinkService.buildAnalysisLink(payloadBuilder.build());
            } catch (Exception e) {
                log.error("Error building AI analysis link", e);
            }
        }

        String formattedMessage = message != null ? message : "No message";

        // Build exception info
        return ExceptionInfo.builder()
                .time(occurrenceTime)
                .type(exceptionType)
                .message(formattedMessage)
                .location(location)
                .stacktrace(stacktrace)
                .traceId(traceId)
                .appName(applicationName)
                .environment(properties.getEnvironment().getCurrent())
                .authorInfo(authorInfo)
                .traceUrl(traceUrl)
                .aiAnalysisUrl(aiAnalysisUrl)
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
     * Get code context by trying all available git source control services
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @param contextLines number of lines before and after to include
     * @return code context or null if not found
     */
    private String getCodeContext(String fileName, int lineNumber, int contextLines) {
        for (GitSourceControlService service : gitSourceControlServices) {
            String codeContext = service.getCodeContext(fileName, lineNumber, contextLines);
            if (codeContext != null) {
                return codeContext;
            }
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
