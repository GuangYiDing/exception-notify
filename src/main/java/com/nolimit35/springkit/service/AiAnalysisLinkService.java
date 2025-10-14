package com.nolimit35.springkit.service;

import com.nolimit35.springkit.model.AiAnalysisPayload;

/**
 * Generates analysis links that point to the external AI web application.
 */
public interface AiAnalysisLinkService {

    /**
     * Build an analysis link carrying the compressed payload.
     *
     * @param payload 聚合的异常、代码上下文等信息
     * @return 用于访问 AI 分析页面的链接；若生成失败则返回 null
     */
    String buildAnalysisLink(AiAnalysisPayload payload);

    /**
     * 检查 AI 链接服务是否可用
     *
     * @return true 如果服务已配置且可用
     */
    boolean isAvailable();
}
