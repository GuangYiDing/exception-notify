package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for GitLabService
 * This test performs real API calls to GitLab
 *
 * To run these tests, you need to set up valid GitLab credentials:
 * - Set environment variable GITLAB_TOKEN with your GitLab personal access token
 * - Set environment variable GITLAB_PROJECT_ID with your project ID
 * - Or update the test configuration with valid credentials
 */
@Tag("integration")
public class GitLabServiceTest {

    private GitLabService gitLabService;
    private ExceptionNotifyProperties properties;

    private static final String TEST_FILE = "src/main/java/com/nolimit35/springkit/service/GitLabService.java";
    private static final int TEST_LINE = 50;

    @BeforeEach
    public void setUp() {
        properties = new ExceptionNotifyProperties();

        // 配置 GitLab 属性
        ExceptionNotifyProperties.GitLab gitlabConfig = new ExceptionNotifyProperties.GitLab();

        // 从环境变量获取配置，或使用默认测试配置
        String token = System.getenv("GITLAB_TOKEN");
        String projectId = System.getenv("GITLAB_PROJECT_ID");
        String branch = System.getenv("GITLAB_BRANCH");
        String baseUrl = System.getenv("GITLAB_BASE_URL");

        // 如果环境变量未设置，使用默认值（测试会被跳过）
        gitlabConfig.setToken(token != null ? token : "your_gitlab_token_here");
        gitlabConfig.setProjectId(projectId != null ? projectId : "your_project_id");
        gitlabConfig.setBranch(branch != null ? branch : "main");
        gitlabConfig.setBaseUrl(baseUrl != null ? baseUrl : "https://gitlab.com/api/v4");

        properties.setGitlab(gitlabConfig);

        gitLabService = new GitLabService(properties);
    }

    @Test
    public void testGetCodeContext_Success() {
        // 检查配置是否有效
        boolean hasValidConfig = properties.getGitlab().getToken() != null
                && !properties.getGitlab().getToken().equals("your_gitlab_token_here")
                && !properties.getGitlab().getProjectId().equals("your_project_id");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitLab token and project ID not configured. " +
                "Set GITLAB_TOKEN and GITLAB_PROJECT_ID environment variables to run this test.");

        // 测试获取代码上下文
        String context = gitLabService.getCodeContext(TEST_FILE, TEST_LINE, 3);

        // 验证结果
        assertNotNull(context, "Code context should not be null");
        assertTrue(context.contains(">>>"), "Context should contain the target line marker");
        assertTrue(context.contains(String.valueOf(TEST_LINE)), "Context should contain the line number");

        // 打印结果以便查看
        System.out.println("Code context retrieved from GitLab:");
        System.out.println(context);
    }

    @Test
    public void testGetCodeContext_MissingConfiguration() {
        // 创建一个配置不完整的实例
        ExceptionNotifyProperties emptyProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.GitLab emptyGitlabConfig = new ExceptionNotifyProperties.GitLab();
        emptyGitlabConfig.setToken(null);
        emptyProps.setGitlab(emptyGitlabConfig);

        GitLabService serviceWithNoConfig = new GitLabService(emptyProps);

        // 配置缺失时应该返回 null
        String context = serviceWithNoConfig.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when configuration is missing");
    }

    @Test
    public void testGetCodeContext_InvalidFile() {
        boolean hasValidConfig = properties.getGitlab().getToken() != null
                && !properties.getGitlab().getToken().equals("your_gitlab_token_here")
                && !properties.getGitlab().getProjectId().equals("your_project_id");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitLab token not configured");

        // 测试不存在的文件
        String context = gitLabService.getCodeContext("non-existent-file.txt", 1, 2);

        assertNull(context, "Should return null for non-existent file");
    }

    @Test
    public void testGetCodeContext_EdgeCases() {
        boolean hasValidConfig = properties.getGitlab().getToken() != null
                && !properties.getGitlab().getToken().equals("your_gitlab_token_here")
                && !properties.getGitlab().getProjectId().equals("your_project_id");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitLab token not configured");

        // 测试第一行
        String context = gitLabService.getCodeContext(TEST_FILE, 1, 5);

        if (context != null) {
            assertTrue(context.contains(">>> 1:"), "Context should highlight line 1");
            assertFalse(context.contains("    0:"), "Context should not include line 0");

            System.out.println("Edge case - first line:");
            System.out.println(context);
        }
    }

    @Test
    public void testGetCodeContext_LargeContextLines() {
        boolean hasValidConfig = properties.getGitlab().getToken() != null
                && !properties.getGitlab().getToken().equals("your_gitlab_token_here")
                && !properties.getGitlab().getProjectId().equals("your_project_id");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitLab token not configured");

        // 测试大范围的上下文行
        String context = gitLabService.getCodeContext(TEST_FILE, TEST_LINE, 10);

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
    public void testGetCodeContext_MissingProjectId() {
        // 创建缺少 projectId 的配置
        ExceptionNotifyProperties testProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.GitLab gitlabConfig = new ExceptionNotifyProperties.GitLab();
        gitlabConfig.setToken("test_token");
        gitlabConfig.setProjectId(null);
        gitlabConfig.setBranch("main");
        gitlabConfig.setBaseUrl("https://gitlab.com/api/v4");
        testProps.setGitlab(gitlabConfig);

        GitLabService service = new GitLabService(testProps);

        String context = service.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when projectId is missing");
    }

    @Test
    public void testGetCodeContext_MissingBranch() {
        // 创建缺少 branch 的配置
        ExceptionNotifyProperties testProps = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.GitLab gitlabConfig = new ExceptionNotifyProperties.GitLab();
        gitlabConfig.setToken("test_token");
        gitlabConfig.setProjectId("123456");
        gitlabConfig.setBranch(null);
        gitlabConfig.setBaseUrl("https://gitlab.com/api/v4");
        testProps.setGitlab(gitlabConfig);

        GitLabService service = new GitLabService(testProps);

        String context = service.getCodeContext(TEST_FILE, TEST_LINE, 2);
        assertNull(context, "Should return null when branch is missing");
    }

    @Test
    public void testGetCodeContext_LastLineContext() {
        boolean hasValidConfig = properties.getGitlab().getToken() != null
                && !properties.getGitlab().getToken().equals("your_gitlab_token_here")
                && !properties.getGitlab().getProjectId().equals("your_project_id");

        assumeTrue(hasValidConfig, "Skipping test: Valid GitLab token not configured");

        // 测试获取靠近文件末尾的代码上下文
        String context = gitLabService.getCodeContext(TEST_FILE, 200, 5);

        if (context != null) {
            assertTrue(context.contains(">>>"), "Context should contain the target line marker");
            // 确保没有超出文件范围
            assertFalse(context.contains("    0:"), "Context should not contain invalid line numbers");

            System.out.println("Context near end of file:");
            System.out.println(context);
        }
    }
}
