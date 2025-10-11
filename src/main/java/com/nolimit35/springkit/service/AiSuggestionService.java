package com.nolimit35.springkit.service;

/**
 * AI 建议服务接口
 * 根据异常信息和代码上下文提供 AI 建议
 */
public interface AiSuggestionService {

    /**
     * 根据异常信息获取 AI 建议
     *
     * @param exceptionType 异常类型
     * @param exceptionMessage 异常消息
     * @param stacktrace 堆栈信息
     * @param codeContext 异常位置的代码上下文（可选）
     * @return AI 建议，如果获取失败或未配置则返回 null
     */
    String getSuggestion(String exceptionType, String exceptionMessage, String stacktrace, String codeContext);

    /**
     * 检查 AI 服务是否可用
     *
     * @return true 如果服务已配置且可用
     */
    boolean isAvailable();
}
