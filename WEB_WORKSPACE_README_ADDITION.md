# README 补充内容 - Web 工作台描述

## 使用说明

请将以下内容替换到 `README.md` 和 `README_EN.md` 中的 "AI 分析工作台" 章节。

---

## 中文版本（README.md）

在 README.md 中找到 `### 🧠 AI 分析工作台` 章节，将原有内容替换为：

```markdown
### 🧠 AI 分析工作台

组件会将异常信息、代码上下文等内容压缩为 Base64URL + GZIP 字符串，并拼接到 `analysis-page-url` 的查询参数中。仓库根目录提供了一个基于 Vite + React 的示例 Web 项目（`web/`），用于在浏览器侧解析数据并与 AI 服务交互。

#### 🎨 工作台特性

- 📡 **流式响应**：采用 Server-Sent Events (SSE) 流式展示 AI 回答，实时显示生成内容
- 📝 **Markdown 渲染**：完整支持 Markdown 语法，包括标题、列表、代码块等
- 🎨 **代码高亮**：基于 highlight.js 的语法高亮，支持多种编程语言
- 💬 **对话式交互**：支持多轮对话，可针对异常继续追问和深入分析
- 🔄 **重新生成**：对 AI 回答不满意？一键重新生成获得新答案
- 📋 **一键复制**：快速复制消息内容和代码片段
- ✏️ **上下文编辑**：支持在线编辑代码上下文、堆栈信息和补充说明
- 🌙 **暗色主题**：护眼的暗色界面设计
- 🔐 **本地存储**：API Key 仅保存在浏览器 LocalStorage，保护隐私安全

#### 🚀 快速部署

**一键部署到 Vercel：**

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FYOUR_USERNAME%2Fexception-notify&project-name=exception-notify-workspace&repository-name=exception-notify&root-directory=web)

> **注意**：请将上面链接中的 `YOUR_USERNAME` 替换为你的 GitHub 用户名

**一键部署到 Cloudflare Pages：**

[![Deploy to Cloudflare Pages](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/YOUR_USERNAME/exception-notify)

> **注意**：请将上面链接中的 `YOUR_USERNAME` 替换为你的 GitHub 用户名

**通过 GitHub Actions 自动部署：**

仓库已配置 Cloudflare Pages 自动部署 workflow，只需：

1. 在 GitHub 仓库设置中添加以下 Secrets：
   - `CLOUDFLARE_API_TOKEN`：Cloudflare API Token
   - `CLOUDFLARE_ACCOUNT_ID`：Cloudflare Account ID

2. 推送代码到 main 分支或修改 `web/` 目录下的文件，将自动触发部署

#### 📦 本地开发

1. 安装依赖并启动本地调试：
   ```bash
   cd web
   npm install
   npm run dev
   ```

2. 生产环境构建：
   ```bash
   npm run build
   ```

3. 预览生产构建：
   ```bash
   npm run preview
   ```

#### ⚙️ 配置说明

1. 将 `analysis-page-url` 指向部署后的地址（例如 `https://your-workspace.vercel.app` 或 `https://your-workspace.pages.dev`）
2. Web 工作台会提示用户在本地浏览器输入 API Key 与模型信息，所有敏感配置仅保存在浏览器 LocalStorage 中
3. 可根据需要替换为企业内部的中转服务
4. 如果需要自定义查询参数名称，可通过 `exception.notify.ai.payload-param` 配置保持前后端一致

#### 🔧 自定义部署

部署到其他平台（如 Netlify、GitHub Pages 等）：

```bash
cd web
npm install
npm run build
# 将 dist 目录部署到你的静态托管服务
```
```

---

## 英文版本（README_EN.md）

在 README_EN.md 中找到 `### 🧠 AI Analysis Workspace` 章节，将原有内容替换为：

```markdown
### 🧠 AI Analysis Workspace

Exception-Notify compresses exception details and code context into a Base64URL + GZIP string and appends it to the configured `analysis-page-url`. The project ships a sample Vite + React workspace under the repository root (`web/`) which decodes the payload in the browser and allows interactive conversations with your AI provider.

#### 🎨 Workspace Features

- 📡 **Streaming Response**: AI responses use Server-Sent Events (SSE) for real-time streaming display
- 📝 **Markdown Rendering**: Full Markdown syntax support including headings, lists, code blocks, etc.
- 🎨 **Code Highlighting**: Syntax highlighting based on highlight.js, supporting multiple programming languages
- 💬 **Interactive Dialogue**: Supports multi-turn conversations for in-depth exception analysis
- 🔄 **Regenerate**: Not satisfied with AI's answer? Regenerate with one click
- 📋 **Quick Copy**: Quickly copy message content and code snippets
- ✏️ **Context Editing**: Support online editing of code context, stack trace and additional notes
- 🌙 **Dark Theme**: Eye-friendly dark interface design
- 🔐 **Local Storage**: API keys stored only in browser LocalStorage for privacy protection

#### 🚀 Quick Deploy

**Deploy to Vercel with one click:**

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2FYOUR_USERNAME%2Fexception-notify&project-name=exception-notify-workspace&repository-name=exception-notify&root-directory=web)

> **Note**: Replace `YOUR_USERNAME` in the link above with your GitHub username

**Deploy to Cloudflare Pages with one click:**

[![Deploy to Cloudflare Pages](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/YOUR_USERNAME/exception-notify)

> **Note**: Replace `YOUR_USERNAME` in the link above with your GitHub username

**Auto-deploy via GitHub Actions:**

The repository is configured with Cloudflare Pages automatic deployment workflow. Just:

1. Add the following Secrets in your GitHub repository settings:
   - `CLOUDFLARE_API_TOKEN`: Your Cloudflare API Token
   - `CLOUDFLARE_ACCOUNT_ID`: Your Cloudflare Account ID

2. Push code to main branch or modify files in `web/` directory to trigger deployment automatically

#### 📦 Local Development

1. Install dependencies and start local development:
   ```bash
   cd web
   npm install
   npm run dev
   ```

2. Build for production:
   ```bash
   npm run build
   ```

3. Preview production build:
   ```bash
   npm run preview
   ```

#### ⚙️ Configuration

1. Point `analysis-page-url` to your deployed workspace (e.g., `https://your-workspace.vercel.app` or `https://your-workspace.pages.dev`)
2. The workspace prompts users to enter API keys and model information in the browser. All sensitive configurations are stored only in browser LocalStorage
3. Can be replaced with internal proxy service as needed
4. If you need to customize the query parameter name, configure `exception.notify.ai.payload-param` to keep frontend and backend consistent

#### 🔧 Custom Deployment

Deploy to other platforms (such as Netlify, GitHub Pages, etc.):

```bash
cd web
npm install
npm run build
# Deploy the dist directory to your static hosting service
```
```

---

## 部署配置说明

### Cloudflare Pages

1. **配置文件**：`web/wrangler.toml`
2. **GitHub Actions**：`.github/workflows/cloudflare-pages.yml`
3. **所需 Secrets**：
   - `CLOUDFLARE_API_TOKEN`
   - `CLOUDFLARE_ACCOUNT_ID`

### Vercel

1. **配置文件**：`web/vercel.json`
2. **部署方式**：通过一键部署按钮或 Vercel CLI

## 注意事项

- 请确保将示例中的 `YOUR_USERNAME` 替换为实际的 GitHub 用户名
- 部署后记得在 Spring Boot 配置中更新 `exception.notify.ai.analysis-page-url`
- Cloudflare Pages 自动部署需要先配置 GitHub Secrets
