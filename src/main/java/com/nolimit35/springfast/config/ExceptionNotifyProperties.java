package com.nolimit35.springfast.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.HashSet;
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
     * DingTalk configuration properties
     */
    @Data
    public static class DingTalk {
        /**
         * DingTalk webhook URL
         */
        private String webhook;

        /**
         * DingTalk secret for signature
         */
        private String secret;
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
} 