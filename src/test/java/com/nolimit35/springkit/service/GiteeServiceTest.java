package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for GiteeService
 * This test performs real API calls to Gitee
 *
 * To run these tests, you need to set up valid Gitee credentials:
 * - Set environment variable GITEE_TOKEN with your Gitee personal access token
 * - Or update the test configuration with valid credentials
 */
@Tag("integration")
public class GiteeServiceTest {

    private GiteeService giteeService;
    private ExceptionNotifyProperties properties;

	private static final String TEST_FILE = "src/main/java/com/nolimit35/springkit/service/GiteeService.java";
	private static final String SIMPLE_FILE_NAME = "GiteeService.java";
    private static final int TEST_LINE = 50;

    @BeforeEach
    public void setUp() {
        properties = new ExceptionNotifyProperties();

        // 配置 Gitee 属性
        ExceptionNotifyProperties.Gitee giteeConfig = new ExceptionNotifyProperties.Gitee();

        // 从环境变量获取配置，或使用默认测试配置
        String token = System.getenv("GITEE_TOKEN");
        String repoOwner = System.getenv("GITEE_REPO_OWNER");
        String repoName = System.getenv("GITEE_REPO_NAME");
        String branch = System.getenv("GITEE_BRANCH");


        // 如果环境变量未设置，使用默认值（测试会被跳过）
        giteeConfig.setToken(token != null ? token : "GITEE_TOKEN");
        giteeConfig.setRepoOwner(repoOwner != null ? repoOwner : "GITEE_REPO_OWNER");
        giteeConfig.setRepoName(repoName != null ? repoName : "GITEE_REPO_NAME");
        giteeConfig.setBranch(branch != null ? branch : "GITEE_BRANCH");

        properties.setGitee(giteeConfig);

        giteeService = new GiteeService(properties);
    }

    @Test
    public void testGetCodeContext_Success() {
        // 检查配置是否有效
        boolean hasValidConfig = properties.getGitee().getToken() != null
                && !properties.getGitee().getToken().equals("your_gitee_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid Gitee token not configured. " +
                "Set GITEE_TOKEN environment variable to run this test.");

        // 测试获取代码上下文
        String context = giteeService.getCodeContext(TEST_FILE, TEST_LINE, 3);

        // 验证结果
        assertNotNull(context, "Code context should not be null");
        assertTrue(context.contains(">>>"), "Context should contain the target line marker");
        assertTrue(context.contains(String.valueOf(TEST_LINE)), "Context should contain the line number");

        // 打印结果以便查看
        System.out.println("Code context retrieved:");
        System.out.println(context);
    }

    @Test
    public void testGetCodeContext_WithSimpleFileName() {
        boolean hasValidConfig = properties.getGitee().getToken() != null
                && !properties.getGitee().getToken().equals("your_gitee_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid Gitee token not configured");

        // 使用简单文件名测试
        String context = giteeService.getCodeContext(SIMPLE_FILE_NAME, 30, 2);

        assertNotNull(context, "Code context should not be null");
        assertTrue(context.contains(">>>"), "Context should contain the target line marker");

        System.out.println("Code context with simple filename:");
        System.out.println(context);
    }

    @Test
    public void testGetCodeContext_MissingConfiguration() {
        // 创建一个配置不完整的实例
        ExceptionNotifyProperties emptyProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.Gitee emptyGiteeConfig = new ExceptionNotifyProperties.Gitee();
        emptyGiteeConfig.setToken(null);
        emptyProps.setGitee(emptyGiteeConfig);

        GiteeService serviceWithNoConfig = new GiteeService(emptyProps);

        // 配置缺失时应该返回 null
        String context = serviceWithNoConfig.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when configuration is missing");
    }

    @Test
    public void testGetCodeContext_InvalidFile() {
        boolean hasValidConfig = properties.getGitee().getToken() != null
                && !properties.getGitee().getToken().equals("your_gitee_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid Gitee token not configured");

        // 测试不存在的文件
        String context = giteeService.getCodeContext("NonExistent.java", 1, 2);

        assertNull(context, "Should return null for non-existent file");
    }

    @Test
    public void testGetCodeContext_EdgeCases() {
        boolean hasValidConfig = properties.getGitee().getToken() != null
                && !properties.getGitee().getToken().equals("your_gitee_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid Gitee token not configured");

        // 测试第一行
        String context = giteeService.getCodeContext(TEST_FILE, 1, 5);

        if (context != null) {
            assertTrue(context.contains(">>> 1:"), "Context should highlight line 1");
            assertFalse(context.contains("    0:"), "Context should not include line 0");

            System.out.println("Edge case - first line:");
            System.out.println(context);
        }
    }

    @Test
    public void testGetCodeContext_LargeContextLines() {
        boolean hasValidConfig = properties.getGitee().getToken() != null
                && !properties.getGitee().getToken().equals("your_gitee_token_here");

        assumeTrue(hasValidConfig, "Skipping test: Valid Gitee token not configured");

        // 测试大范围的上下文行
        String context = giteeService.getCodeContext(TEST_FILE, TEST_LINE, 10);

        if (context != null) {
            assertTrue(context.contains(">>>"), "Context should contain the target line marker");

            // 计算上下文行数
            String[] lines = context.split("\n");
            assertTrue(lines.length >= 3, "Context should contain multiple lines");

            System.out.println("Large context (10 lines before/after):");
            System.out.println(context);
        }
    }
}
