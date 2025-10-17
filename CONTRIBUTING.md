# Contributing to Exception-Notify

Thank you for your interest in contributing to Exception-Notify! This document provides guidelines and instructions for contributing to the project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)

## 🤝 Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for everyone. Please be kind and considerate in all interactions.

## 🚀 Getting Started

### Prerequisites

- JDK 8 or higher
- Maven 3.6+
- Git
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code, etc.)

### Development Setup

1. **Fork the repository**
   
   Fork the repository to your GitHub account by clicking the "Fork" button.

2. **Clone your fork**
   
   ```bash
   git clone https://github.com/YOUR_USERNAME/exception-notify.git
   cd exception-notify
   ```

3. **Add upstream remote**
   
   ```bash
   git remote add upstream https://github.com/GuangYiDing/exception-notify.git
   ```

4. **Build the project**
   
   ```bash
   mvn clean install
   ```

5. **Run tests**
   
   ```bash
   mvn test
   ```

## 💡 How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:

- Clear, descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Your environment (Java version, Spring Boot version, etc.)
- Relevant code snippets or error messages

### Suggesting Enhancements

We welcome feature requests! Please create an issue with:

- Clear description of the proposed feature
- Use cases and benefits
- Possible implementation approach (if you have ideas)

### Contributing Code

1. Check existing issues or create a new one to discuss your changes
2. Create a feature branch from `main`
3. Make your changes following our coding standards
4. Add/update tests as needed
5. Ensure all tests pass
6. Submit a pull request

## 📝 Coding Standards

### Java Code Style

- Use 4 spaces for indentation (no tabs)
- Follow standard Java naming conventions:
  - Classes: UpperCamelCase
  - Methods/variables: lowerCamelCase
  - Constants: UPPER_SNAKE_CASE
- Maximum line length: 120 characters
- Use meaningful variable and method names

### Lombok Usage

- Prefer existing Lombok annotations (`@Slf4j`, `@Data`, `@Builder`, etc.)
- Avoid manual getters/setters when Lombok can generate them

### Package Structure

- Follow the existing package structure under `com.nolimit35.springkit`
- Group related classes by functionality (e.g., `notification`, `service`, `config`)

### Code Organization

```
src/main/java/com/nolimit35/springkit/
├── config/           # Configuration classes
├── notification/     # Notification providers
├── service/          # Business logic services
├── model/            # Data models
├── util/             # Utility classes
└── aspect/           # AOP aspects
```

### Comments and Documentation

- Add Javadoc for public APIs
- Use inline comments sparingly and only when necessary
- Keep comments up-to-date with code changes
- Document complex algorithms or business logic

## 🧪 Testing Guidelines

### Test Structure

- Mirror production package structure in `src/test/java`
- Name test classes with `*Test` suffix
- Use JUnit 5 for writing tests
- Use Mockito for mocking dependencies

### Test Coverage

- Aim to maintain or improve existing test coverage
- Write unit tests for new functionality
- Include integration tests for complex features
- Test edge cases and error scenarios

### Test Configuration

- Place test-specific configuration in `src/test/resources/application-test.yml`
- Keep test data minimal and focused

### Example Test

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationProvider notificationProvider;
    
    @InjectMocks
    private NotificationService notificationService;
    
    @Test
    void shouldSendNotification() {
        // Arrange
        ExceptionInfo exceptionInfo = ExceptionInfo.builder()
            .type("NullPointerException")
            .message("Test exception")
            .build();
        
        when(notificationProvider.sendNotification(any())).thenReturn(true);
        
        // Act
        boolean result = notificationService.notify(exceptionInfo);
        
        // Assert
        assertTrue(result);
        verify(notificationProvider).sendNotification(exceptionInfo);
    }
}
```

## 📜 Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code refactoring without changing functionality
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks, dependency updates

### Scope

The scope should indicate the affected module:
- `notification` - Notification providers (DingTalk, Feishu, WeChat)
- `git` - Git integration (GitHub, GitLab, Gitee)
- `trace` - Tracing functionality
- `config` - Configuration
- `web` - Web workspace
- `core` - Core functionality

### Examples

```
feat(notification): add Slack notification provider
fix(git): correct GitLab blame API endpoint
docs(readme): update configuration examples
refactor(trace): simplify TraceID extraction logic
test(notification): add tests for deduplication feature
```

## 🔄 Pull Request Process

### Before Submitting

1. **Update from upstream**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run tests**
   ```bash
   mvn test
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Format your code**
   - Organize imports
   - Remove unused imports
   - Format according to project style

### Submitting the PR

1. **Push to your fork**
   ```bash
   git push origin your-feature-branch
   ```

2. **Create Pull Request**
   - Use a clear, descriptive title
   - Reference related issues (e.g., "Fixes #123")
   - Provide detailed description of changes
   - Include screenshots/examples if applicable

### PR Description Template

```markdown
## Description
Brief description of the changes

## Motivation
Why is this change needed?

## Changes Made
- Change 1
- Change 2

## Testing
How has this been tested?

## Screenshots (if applicable)
Add screenshots here

## Checklist
- [ ] Tests pass locally
- [ ] Code follows project style guidelines
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow convention
```

### Review Process

- Maintainers will review your PR
- Address any requested changes
- Once approved, a maintainer will merge your PR

## 🎨 Web Workspace Contributions

For contributions to the web workspace (`web/` directory):

### Setup

```bash
cd web
npm install
npm run dev
```

### Technology Stack

- Next.js 14
- React
- TypeScript
- Tailwind CSS
- Cloudflare Workers

### Code Style

- Use TypeScript with strict mode
- Follow React best practices
- Use functional components with hooks
- Format code with Prettier

## 📚 Documentation Contributions

Documentation improvements are always welcome!

- Fix typos or unclear explanations
- Add examples and use cases
- Improve formatting and organization
- Translate documentation to other languages

## ❓ Questions?

If you have questions about contributing, feel free to:

- Open an issue with the `question` label
- Join our discussions
- Contact the maintainers

## 🙏 Thank You!

Your contributions help make Exception-Notify better for everyone. Thank you for taking the time to contribute!
