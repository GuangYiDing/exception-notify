package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for GitHubService
 * This test performs real API calls to GitHub
 *
 * To run these tests, you need to set up valid GitHub credentials:
 * - Set environment variable GITHUB_TOKEN with your GitHub personal access token
 * - Or update the test configuration with valid credentials
 */
@Tag("integration")
public class GitHubServiceTest {

    private GitHubService gitHubService;
    private ExceptionNotifyProperties properties;

    private static final String TEST_FILE = "src/main/java/com/nolimit35/springkit/service/GitHubService.java";
    private static final int TEST_LINE = 50;

    @BeforeEach
    public void setUp() {
        properties = new ExceptionNotifyProperties();

        // 配置 GitHub 属性
        ExceptionNotifyProperties.GitHub githubConfig = new ExceptionNotifyProperties.GitHub();

        // 从环境变量获取配置，或使用默认测试配置
        String token = System.getenv("GITHUB_TOKEN");
        String repoOwner = System.getenv("GITHUB_REPO_OWNER");
        String repoName = System.getenv("GITHUB_REPO_NAME");
        String branch = System.getenv("GITHUB_BRANCH");

        // 如果环境变量未设置，使用默认值（测试会被跳过）
        githubConfig.setToken(token != null ? token : "your_github_token_here");
        githubConfig.setRepoOwner(repoOwner != null ? repoOwner : "GuangYiDing");
        githubConfig.setRepoName(repoName != null ? repoName : "exception-notify");
        githubConfig.setBranch(branch != null ? branch : "main");

        properties.setGithub(githubConfig);

        gitHubService = new GitHubService(properties);
    }

    @Test
    public void testGetCodeContext_Success() {
        // 检查配置是否有效
        boolean hasValidConfig = properties.getGithub().getToken() != null
                && !properties.getGithub().getToken().equals("your_github_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitHub token not configured. " +
                "Set GITHUB_TOKEN environment variable to run this test.");

        // 测试获取代码上下文
        String context = gitHubService.getCodeContext(TEST_FILE, TEST_LINE, 3);

        // 验证结果
        assertNotNull(context, "Code context should not be null");
        assertTrue(context.contains(">>>"), "Context should contain the target line marker");
        assertTrue(context.contains(String.valueOf(TEST_LINE)), "Context should contain the line number");

        // 打印结果以便查看
        System.out.println("Code context retrieved from GitHub:");
        System.out.println(context);
    }

    @Test
    public void testGetCodeContext_MissingConfiguration() {
        // 创建一个配置不完整的实例
        ExceptionNotifyProperties emptyProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.GitHub emptyGithubConfig = new ExceptionNotifyProperties.GitHub();
        emptyGithubConfig.setToken(null);
        emptyProps.setGithub(emptyGithubConfig);

        GitHubService serviceWithNoConfig = new GitHubService(emptyProps);

        // 配置缺失时应该返回 null
        String context = serviceWithNoConfig.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when configuration is missing");
    }

    @Test
    public void testGetCodeContext_InvalidFile() {
        boolean hasValidConfig = properties.getGithub().getToken() != null
                && !properties.getGithub().getToken().equals("your_github_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitHub token not configured");

        // 测试不存在的文件
        String context = gitHubService.getCodeContext("non-existent-file.txt", 1, 2);

        assertNull(context, "Should return null for non-existent file");
    }

    @Test
    public void testGetCodeContext_EdgeCases() {
        boolean hasValidConfig = properties.getGithub().getToken() != null
                && !properties.getGithub().getToken().equals("your_github_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitHub token not configured");

        // 测试第一行
        String context = gitHubService.getCodeContext(TEST_FILE, 1, 5);

        if (context != null) {
            assertTrue(context.contains(">>> 1:"), "Context should highlight line 1");
            assertFalse(context.contains("    0:"), "Context should not include line 0");

            System.out.println("Edge case - first line:");
            System.out.println(context);
        }
    }

    @Test
    public void testGetCodeContext_LargeContextLines() {
        boolean hasValidConfig = properties.getGithub().getToken() != null
                && !properties.getGithub().getToken().equals("your_github_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitHub token not configured");

        // 测试大范围的上下文行
        String context = gitHubService.getCodeContext(TEST_FILE, TEST_LINE, 10);

        if (context != null) {
            assertTrue(context.contains(">>>"), "Context should contain the target line marker");

            // 计算上下文行数
            String[] lines = context.split("\n");
            assertTrue(lines.length >= 3, "Context should contain multiple lines");

            System.out.println("Large context (10 lines before/after):");
            System.out.println(context);
        }
    }

    @Test
    public void testGetCodeContext_MissingRepoOwner() {
        // 创建缺少 repoOwner 的配置
        ExceptionNotifyProperties testProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.GitHub githubConfig = new ExceptionNotifyProperties.GitHub();
        githubConfig.setToken("test_token");
        githubConfig.setRepoOwner(null);
        githubConfig.setRepoName("test_repo");
        githubConfig.setBranch("main");
        testProps.setGithub(githubConfig);

        GitHubService service = new GitHubService(testProps);

        String context = service.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when repoOwner is missing");
    }

    @Test
    public void testGetCodeContext_MissingRepoName() {
        // 创建缺少 repoName 的配置
        ExceptionNotifyProperties testProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.GitHub githubConfig = new ExceptionNotifyProperties.GitHub();
        githubConfig.setToken("test_token");
        githubConfig.setRepoOwner("test_owner");
        githubConfig.setRepoName(null);
        githubConfig.setBranch("main");
        testProps.setGithub(githubConfig);

        GitHubService service = new GitHubService(testProps);

        String context = service.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when repoName is missing");
    }
}
