package com.nolimit35.springfast.handler;

import com.nolimit35.springfast.service.ExceptionNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for catching unhandled exceptions
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication
public class GlobalExceptionHandler {
    private final ExceptionNotificationService notificationService;

    public GlobalExceptionHandler(ExceptionNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Handle all exceptions
     *
     * @param ex the exception
     * @throws Exception the original exception is rethrown after processing
     */
    @ExceptionHandler(Exception.class)
    public void handleException(Exception ex) throws Exception {
        try {
            // Process exception for notification
            notificationService.processException(ex);
        } catch (Exception e) {
            log.error("Error in exception handler", e);
        }
        
        // Rethrow the original exception to allow other handlers to process it
        throw ex;
    }
} 