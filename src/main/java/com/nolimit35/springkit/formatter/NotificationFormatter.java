package com.nolimit35.springkit.formatter;

import com.nolimit35.springkit.model.ExceptionInfo;

/**
 * Interface for formatting exception notifications
 */
public interface NotificationFormatter {
    /**
     * Format exception information into notification content
     *
     * @param exceptionInfo the exception information
     * @return formatted notification content
     */
    String format(ExceptionInfo exceptionInfo);
} 