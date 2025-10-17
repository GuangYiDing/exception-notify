# Exception-Notify AI Workspace

A Next.js-based web workspace for viewing and analyzing exceptions with AI assistance.

## 🎨 Features

- **Real-time AI Conversations**: Stream responses from your preferred AI provider
- **Markdown & Code Highlighting**: Beautiful rendering with syntax highlighting
- **Multi-turn Dialogue**: Continue conversations about exceptions
- **Context Editing**: Edit code context and stack traces inline
- **Dark Theme**: Eye-friendly dark interface
- **Privacy-First**: API keys stored only in browser LocalStorage
- **One-Click Copy**: Quick copy for messages and code snippets

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ and npm
- A Cloudflare account (for Workers deployment) or other hosting platform

### Local Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Open http://localhost:3000
```

### Build for Production

```bash
# Build the application
npm run build

# Preview production build
npm run preview
```

## 📦 Deployment

### Cloudflare Workers (Recommended)

**Automatic Deployment via GitHub Actions:**

1. Fork the repository
2. Add GitHub Secrets:
   - `CLOUDFLARE_API_TOKEN`: Your Cloudflare API token
   - `CLOUDFLARE_ACCOUNT_ID`: Your Cloudflare account ID

3. Push to main branch or modify files in `web/` directory

**Manual Deployment:**

```bash
# Install Wrangler CLI
npm install -g wrangler

# Login to Cloudflare
wrangler login

# Deploy
npm run deploy
```

### Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Deploy
vercel --prod
```

### Netlify

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Build and deploy
npm run build
netlify deploy --prod --dir=.next
```

### Docker

```bash
# Build image
docker build -t exception-notify-workspace .

# Run container
docker run -p 3000:3000 exception-notify-workspace
```

## ⚙️ Configuration

### Backend Configuration

Configure Exception-Notify backend to point to your deployed workspace:

```yaml
exception:
  notify:
    ai:
      enabled: true
      include-code-context: true
      code-context-lines: 5
      analysis-page-url: https://your-workspace.pages.dev
```

### Cloudflare KV Setup (for /api/compress)

The `/api/compress` endpoint requires Cloudflare KV:

1. Create a KV namespace in Cloudflare dashboard
2. Bind it to your Worker:
   ```toml
   # wrangler.toml
   kv_namespaces = [
     { binding = "EXCEPTION_PAYLOADS", id = "your-kv-namespace-id" }
   ]
   ```

### Environment Variables

Create `.env.local` for local development:

```env
# Optional: Default API configuration
NEXT_PUBLIC_DEFAULT_API_ENDPOINT=https://api.openai.com/v1
NEXT_PUBLIC_DEFAULT_MODEL=gpt-4
```

## 🎯 Usage

### For Users

1. Open an AI analysis link from Exception-Notify
2. Configure your AI provider on first visit:
   - API Endpoint (e.g., `https://api.openai.com/v1`)
   - API Key
   - Model (e.g., `gpt-4`, `claude-3-opus`)
3. Review exception details
4. Chat with AI for analysis and solutions
5. Edit context or add notes as needed

### For Developers

**API Endpoints:**

- `GET /` - Main workspace interface
- `POST /api/compress` - Compress and store payload, return short code
- `GET /api/decompress?code=xxx` - Retrieve and decompress payload
- `POST /api/chat` - Proxy to AI provider with streaming support

## 🔧 Architecture

```
web/
├── src/
│   ├── app/              # Next.js app directory
│   │   ├── page.tsx      # Main workspace page
│   │   ├── api/          # API routes
│   │   │   ├── compress/ # Payload compression endpoint
│   │   │   ├── decompress/ # Payload decompression endpoint
│   │   │   └── chat/     # AI chat streaming endpoint
│   │   └── layout.tsx    # Root layout
│   ├── components/       # React components
│   ├── lib/              # Utilities and helpers
│   └── styles/           # Global styles
├── public/               # Static assets
├── package.json
├── next.config.js
├── tsconfig.json
└── wrangler.toml         # Cloudflare Workers config
```

## 🔐 Security

- API keys never leave the browser
- No server-side credential storage
- HTTPS recommended for all deployments
- Payload compression uses standard algorithms
- Short codes expire automatically (configurable)

## 🛠️ Development

### Technology Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Syntax Highlighting**: highlight.js
- **Markdown**: Custom renderer with code blocks
- **Hosting**: Cloudflare Workers / Vercel / Netlify

### Code Structure

```typescript
// Payload compression/decompression
import { compress, decompress } from '@/lib/compression'

// AI chat streaming
import { streamChat } from '@/lib/ai'

// Exception data types
interface ExceptionPayload {
  type: string
  message: string
  stackTrace: string
  codeContext?: string
  traceId?: string
  environment?: string
  // ...
}
```

### Adding Custom AI Providers

Edit `src/lib/ai.ts` to add support for new providers:

```typescript
export async function streamChat(
  messages: Message[],
  config: AIConfig
): Promise<ReadableStream> {
  // Add your provider logic here
}
```

## 📝 Customization

### Styling

Edit `src/app/globals.css` or Tailwind config:

```css
/* Custom theme colors */
:root {
  --background: #0a0a0a;
  --foreground: #ededed;
  --primary: #3b82f6;
}
```

### Code Highlighting Themes

Change in `src/components/CodeBlock.tsx`:

```typescript
import 'highlight.js/styles/github-dark.css'  // Or other themes
```

### Default Models

Update `src/lib/constants.ts`:

```typescript
export const DEFAULT_MODELS = [
  'gpt-4',
  'gpt-3.5-turbo',
  'claude-3-opus',
  'your-custom-model'
]
```

## 🧪 Testing

```bash
# Run type checking
npm run type-check

# Run linting
npm run lint

# Run tests (if configured)
npm test
```

## 🐛 Troubleshooting

### Payload not decompressing

- Check if KV namespace is bound correctly
- Verify short code hasn't expired
- Check browser console for errors

### AI streaming not working

- Verify API endpoint and key are correct
- Check CORS configuration
- Ensure streaming is supported by provider

### Build errors

```bash
# Clear cache and rebuild
rm -rf .next node_modules
npm install
npm run build
```

## 📚 Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [Cloudflare Workers Docs](https://developers.cloudflare.com/workers)
- [Exception-Notify Main Docs](../README.md)

## 🤝 Contributing

Contributions welcome! Please see [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## 📄 License

Apache License 2.0 - see [LICENSE](../LICENSE)
