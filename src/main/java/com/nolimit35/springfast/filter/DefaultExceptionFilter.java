package com.nolimit35.springfast.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default implementation of ExceptionFilter
 */
@Component
@ConditionalOnMissingBean(ExceptionFilter.class)
public class DefaultExceptionFilter implements ExceptionFilter {
    @Override
    public boolean shouldNotify(Throwable throwable) {
        // By default, notify for all exceptions
        return true;
    }
} 