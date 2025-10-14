package com.nolimit35.springkit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for Exception-Notify
 */
@Data
@ConfigurationProperties(prefix = "exception.notify")
public class ExceptionNotifyProperties {
    /**
     * Whether to enable exception notification
     */
    private boolean enabled = true;

    /**
     * DingTalk configuration
     */
    private DingTalk dingtalk = new DingTalk();

    /**
     * Feishu configuration
     */
    private Feishu feishu = new Feishu();

    /**
     * WeChat Work configuration
     */
    private WeChatWork wechatwork = new WeChatWork();

    /**
     * GitHub configuration
     */
    private GitHub github = new GitHub();

    /**
     * Gitee configuration
     */
    private Gitee gitee = new Gitee();

    /**
     * GitLab configuration
     */
    private GitLab gitlab = new GitLab();

    /**
     * Trace configuration
     */
    private Trace trace = new Trace();

    /**
     * Notification configuration
     */
    private Notification notification = new Notification();

    /**
     * Environment configuration
     */
    private Environment environment = new Environment();

    /**
     * Tencent CLS configuration
     */
    private TencentCls tencentcls = new TencentCls();

    /**
     * Package filter configuration
     */
    private PackageFilter packageFilter = new PackageFilter();

    /**
     * AI suggestion configuration
     */
    private AI ai = new AI();

    /**
     * DingTalk configuration properties
     */
    @Data
    public static class DingTalk {
        /**
         * DingTalk webhook URL
         */
        private String webhook;
        
        /**
         * DingTalk @ configuration
         */
        private At at = new At();
        
        /**
         * DingTalk @ configuration properties
         */
        @Data
        public static class At {
            /**
             * Whether to enable @ functionality
             */
            private boolean enabled = true;

            
            /**
             * Mapping of DingTalk user IDs to Git email addresses
             */
            private Map<String, List<String>> userIdMappingGitEmail;
        }
    }

    /**
     * Feishu configuration properties
     */
    @Data
    public static class Feishu {
        /**
         * Feishu webhook URL
         */
        private String webhook;
        
        /**
         * Feishu @ configuration
         */
        private At at = new At();
        
        /**
         * Feishu @ configuration properties
         */
        @Data
        public static class At {
            /**
             * Whether to enable @ functionality
             */
            private boolean enabled = true;

            /**
             * Mapping of Feishu open IDs to Git email addresses
             */
            private Map<String, List<String>> openIdMappingGitEmail;
        }
    }

    /**
     * WeChat Work configuration properties
     */
    @Data
    public static class WeChatWork {
        /**
         * WeChat Work webhook URL
         */
        private String webhook;
        
        /**
         * WeChat Work @ configuration
         */
        private At at = new At();
        
        /**
         * WeChat Work @ configuration properties
         */
        @Data
        public static class At {
            /**
             * Whether to enable @ functionality
             */
            private boolean enabled = true;
            
            /**
             * Mapping of WeChat Work user IDs to Git email addresses
             */
            private Map<String, List<String>> userIdMappingGitEmail;
        }
    }

    /**
     * Tencent CLS configuration properties
     */
    @Data
    public static class TencentCls {
        /**
         * Tencent CLS region
         */
        private String region;

        /**
         * Tencent CLS topic ID
         */
        private String topicId;
    }

    /**
     * GitHub configuration properties
     */
    @Data
    public static class GitHub {
        /**
         * GitHub access token
         */
        private String token;

        /**
         * GitHub repository owner
         */
        private String repoOwner;

        /**
         * GitHub repository name
         */
        private String repoName;
        
        /**
         * GitHub repository branch
         */
        private String branch = "master";
    }

    /**
     * Gitee configuration properties
     */
    @Data
    public static class Gitee {
        /**
         * Gitee access token
         */
        private String token;

        /**
         * Gitee repository owner
         */
        private String repoOwner;

        /**
         * Gitee repository name
         */
        private String repoName;
        
        /**
         * Gitee repository branch
         */
        private String branch = "master";
    }

    /**
     * GitLab configuration properties
     */
    @Data
    public static class GitLab {
        /**
         * GitLab access token
         */
        private String token;

        /**
         * GitLab project id or path
         */
        private String projectId;

        /**
         * GitLab API base URL
         */
        private String baseUrl = "https://gitlab.com/api/v4";
        
        /**
         * GitLab repository branch
         */
        private String branch = "master";
    }

    /**
     * Trace configuration properties
     */
    @Data
    public static class Trace {
        /**
         * Whether to enable trace
         */
        private boolean enabled = true;

        /**
         * Trace ID header name
         */
        private String headerName = "X-Trace-Id";
    }

    /**
     * Notification configuration properties
     */
    @Data
    public static class Notification {
        /**
         * Title template for notification
         */
        private String titleTemplate = "【${appName}】异常告警";

        /**
         * Whether to include stacktrace in notification
         */
        private boolean includeStacktrace = true;

        /**
         * Maximum number of stacktrace lines to include
         */
        private int maxStacktraceLines = 10;

        /**
         * Deduplication configuration
         */
        private Deduplication deduplication = new Deduplication();

        /**
         * Deduplication configuration properties
         */
        @Data
        public static class Deduplication {
            /**
             * Whether to enable exception deduplication
             */
            private boolean enabled = true;

            /**
             * Time window for deduplication in minutes
             * Default is 3 minutes
             */
            private long timeWindowMinutes = 3;

            /**
             * Cleanup interval for expired cache entries in minutes
             * Default is 60 minutes (1 hour)
             */
            private long cleanupIntervalMinutes = 60;
        }
    }

    /**
     * Environment configuration properties
     */
    @Data
    public static class Environment {
        /**
         * Current environment (automatically determined from spring.profiles.active)
         * This property is not meant to be set directly in most cases.
         * It will be automatically set based on spring.profiles.active.
         */
        private String current = "dev";

        /**
         * Environments to report exceptions from (comma-separated)
         */
        private Set<String> reportFrom = new HashSet<>(Arrays.asList("test", "prod"));

        /**
         * Check if exceptions should be reported from the current environment
         *
         * @return true if exceptions should be reported, false otherwise
         */
        public boolean shouldReportFromCurrentEnvironment() {
            return reportFrom.contains(current);
        }
    }

    /**
     * Package filter configuration properties
     */
    @Data
    public static class PackageFilter {
        /**
         * Whether to enable package filtering
         */
        private boolean enabled = false;

        /**
         * List of package names to include in exception analysis
         */
        private Set<String> includePackages = new HashSet<>();
    }

    /**
     * AI suggestion configuration properties
     */
    @Data
    public static class AI {
        /**
         * Whether to enable AI suggestions
         */
        private boolean enabled = false;

        /**
         * AI service provider (openai, azure-openai, etc.)
         */
        private String provider = "openai";

        /**
         * API key for the AI service
         */
        private String apiKey;

        /**
         * API base URL
         * For OpenAI: https://api.openai.com/v1/chat/completions
         * For Azure OpenAI or other compatible services: custom URL
         */
        private String apiUrl = "https://api.openai.com/v1/chat/completions";

        /**
         * AI model to use
         * For OpenAI: gpt-3.5-turbo, gpt-4, gpt-4-turbo, etc.
         */
        private String model = "gpt-3.5-turbo";

        /**
         * Maximum tokens in the response
         */
        private int maxTokens = 500;

        /**
         * Temperature for response generation (0.0 - 2.0)
         * Lower values make output more focused and deterministic
         */
        private double temperature = 0.7;

        /**
         * Request timeout in seconds
         */
        private int timeout = 30;

        /**
         * Whether to include code context when available
         */
        private boolean includeCodeContext = true;

        /**
         * Number of lines of code context to include (before and after the error line)
         */
        private int codeContextLines = 5;

        /**
         * URL of the AI analysis web page that will receive the compressed payload
         */
        private String analysisPageUrl;

        /**
         * Query parameter name used to pass the compressed payload to the analysis page
         */
        private String payloadParam = "payload";
    }
}
