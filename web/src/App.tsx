import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { ungzip } from 'pako';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github-dark.css';

interface AiAnalysisPayload {
  appName?: string;
  environment?: string;
  occurrenceTime?: string;
  exceptionType?: string;
  exceptionMessage?: string;
  location?: string;
  stacktrace?: string;
  codeContext?: string;
  traceId?: string;
  traceUrl?: string;
  author?: {
    name?: string;
    email?: string;
    lastCommitTime?: string;
    fileName?: string;
    lineNumber?: number;
    commitMessage?: string;
  };
}

type ChatMessage = {
  role: 'system' | 'user' | 'assistant';
  content: string;
  reasoning?: string;
};

type ClientSettings = {
  endpoint: string;
  apiKey: string;
  model: string;
  temperature: number;
};

const SETTINGS_KEY = 'exception-notify-ai-settings';

const defaultSettings: ClientSettings = {
  endpoint: 'https://api.openai.com/v1/chat/completions',
  apiKey: '',
  model: 'gpt-4o-mini',
  temperature: 0.2
};

const defaultSystemPrompt =
  '你是一个资深 Java/Spring 工程师，擅长分析异常堆栈并提供修复建议。请结合提供的上下文，输出简洁明确、可执行的建议。';

const textDecoder = new TextDecoder();

export default function App() {
  const [payload, setPayload] = useState<AiAnalysisPayload | null>(null);
  const [payloadError, setPayloadError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([
    { role: 'system', content: defaultSystemPrompt }
  ]);
  const [input, setInput] = useState('根据以上信息,分析解决这个问题');
  const [isSending, setIsSending] = useState(false);
  const [collapsedMessages, setCollapsedMessages] = useState<Record<number, boolean>>({});
  const [settings, setSettings] = useState<ClientSettings>(() => {
    try {
      const raw = localStorage.getItem(SETTINGS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        return {
          endpoint: parsed.endpoint ?? defaultSettings.endpoint,
          apiKey: parsed.apiKey ?? defaultSettings.apiKey,
          model: parsed.model ?? defaultSettings.model,
          temperature:
            typeof parsed.temperature === 'number'
              ? parsed.temperature
              : defaultSettings.temperature
        };
      }
    } catch (err) {
      console.warn('Failed to load settings', err);
    }
    return defaultSettings;
  });
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [streamingContent, setStreamingContent] = useState<string>('');
  const [streamingReasoning, setStreamingReasoning] = useState<string>('');
  const [reasoningCollapsed, setReasoningCollapsed] = useState<Record<number, boolean>>({});
  const [streamingReasoningCollapsed, setStreamingReasoningCollapsed] = useState(false);
  const chatWindowRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    try {
      const url = new URL(window.location.href);
      const searchParams = url.searchParams;
      const payloadParamName = searchParams.get('payloadParam') || 'payload';
      const encoded = searchParams.get(payloadParamName);
      if (!encoded) {
        setPayloadError('链接缺少压缩后的异常数据参数。');
        return;
      }

      const bytes = decodeBase64Url(encoded);
      const decompressed = ungzip(bytes);
      const json = textDecoder.decode(decompressed);
      const parsed: AiAnalysisPayload = JSON.parse(json);
      setPayload(parsed);
      setPayloadError(null);

      const summaryMessage = buildSummaryPrompt(parsed);
      if (summaryMessage) {
        setMessages(prev => {
          const hasSummary = prev.some(msg => msg.role === 'user' && msg.content.startsWith('[异常概览]'));
          if (hasSummary) {
            return prev;
          }
          return [...prev, { role: 'user', content: summaryMessage }];
        });
      }
    } catch (error) {
      console.error('Failed to decode payload', error);
      setPayloadError('无法解析 AI 分析参数，请确认链接未被篡改。');
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  }, [settings]);

  // Auto scroll to bottom when streaming or new messages arrive
  useEffect(() => {
    if (chatWindowRef.current) {
      chatWindowRef.current.scrollTop = chatWindowRef.current.scrollHeight;
    }
  }, [messages, streamingContent]);

  const toggleCollapsed = (index: number, current: boolean) => {
    setCollapsedMessages(prev => ({
      ...prev,
      [index]: !current
    }));
  };

  const sendMessage = async (userMessage: string, baseMessages?: ChatMessage[]) => {
    if (!settings.apiKey.trim()) {
      setSendError('请先在设置中填写 API Key。');
      setSettingsOpen(true);
      return;
    }

    setSendError(null);
    const currentMessages = baseMessages || messages;
    const newMessages: ChatMessage[] = [
      ...currentMessages,
      { role: 'user', content: userMessage }
    ];
    setMessages(newMessages);
    setIsSending(true);
    setStreamingContent('');
    setStreamingReasoning('');
    setStreamingReasoningCollapsed(false);

    try {
      const response = await fetch(settings.endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${settings.apiKey}`
        },
        body: JSON.stringify({
          model: settings.model,
          temperature: settings.temperature,
          stream: true,
          messages: newMessages.map(({ role, content }) => ({ role, content }))
        })
      });

      if (!response.ok) {
        const bodyText = await response.text();
        throw new Error(formatApiError(response.status, response.statusText, bodyText));
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('无法获取响应流');
      }

      const decoder = new TextDecoder();
      let buffer = '';
      let accumulatedContent = '';
      let accumulatedReasoning = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed || trimmed === 'data: [DONE]') continue;
          if (!trimmed.startsWith('data: ')) continue;

          try {
            const jsonStr = trimmed.slice(6);
            const parsed = JSON.parse(jsonStr);
            const delta = parsed?.choices?.[0]?.delta;
            
            if (delta?.content) {
              accumulatedContent += delta.content;
              setStreamingContent(accumulatedContent);
            }
            
            if (delta?.reasoning_content) {
              accumulatedReasoning += delta.reasoning_content;
              setStreamingReasoning(accumulatedReasoning);
            }
          } catch (err) {
            console.warn('Failed to parse SSE line', trimmed, err);
          }
        }
      }

      if (!accumulatedContent) {
        throw new Error('接口返回内容为空，请检查模型与消息体。');
      }

      const newMessage: ChatMessage = {
        role: 'assistant',
        content: accumulatedContent
      };
      if (accumulatedReasoning) {
        newMessage.reasoning = accumulatedReasoning;
      }

      setMessages(prev => [...prev, newMessage]);
      setCollapsedMessages(prev => {
        const next = { ...prev };
        next[newMessages.length] = false;
        return next;
      });
      if (accumulatedReasoning) {
        setReasoningCollapsed(prev => {
          const next = { ...prev };
          next[newMessages.length] = true; // Default to collapsed
          return next;
        });
      }
      setStreamingContent('');
      setStreamingReasoning('');
    } catch (error) {
      console.error('Failed to call AI endpoint', error);
      setSendError(
        error instanceof Error ? error.message : '调用 AI 接口失败，请检查网络或配置。'
      );
      setStreamingContent('');
      setStreamingReasoning('');
    } finally {
      setIsSending(false);
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!input.trim()) {
      return;
    }
    const userMessage = input.trim();
    setInput('');
    await sendMessage(userMessage);
  };

  const exceptionTitle = useMemo(() => {
    if (!payload) {
      return '异常详情';
    }
    const parts = [
      payload.appName || '应用',
      payload.environment ? `环境 ${payload.environment}` : undefined
    ].filter(Boolean);
    return parts.join(' · ') || '异常详情';
  }, [payload]);

  return (
    <div className="app-container">
      <header className="app-header">
        <div>
          <h1>异常 AI 分析工作台</h1>
          <p>基于异常上下文快速梳理问题并联动对话式分析。</p>
        </div>
        <button className="settings-button" onClick={() => setSettingsOpen(v => !v)}>
          {settingsOpen ? '关闭设置' : '打开设置'}
        </button>
      </header>

      {settingsOpen && (
        <section className="settings-panel">
          <h2>AI 接口设置</h2>
          <p className="hint">
            API Key 仅保存在当前浏览器 LocalStorage 中。若使用公共环境，请谨慎输入密钥。
          </p>
          <form className="settings-grid" onSubmit={event => event.preventDefault()}>
            <label>
              Endpoint
              <input
                type="text"
                value={settings.endpoint}
                onChange={event =>
                  setSettings(prev => ({ ...prev, endpoint: event.target.value }))
                }
              />
            </label>
            <label>
              Model
              <input
                type="text"
                value={settings.model}
                onChange={event =>
                  setSettings(prev => ({ ...prev, model: event.target.value }))
                }
              />
            </label>
            <label>
              Temperature
              <input
                type="number"
                min={0}
                max={2}
                step={0.1}
                value={settings.temperature}
                onChange={event =>
                  setSettings(prev => ({
                    ...prev,
                    temperature: Number(event.target.value)
                  }))
                }
              />
            </label>
            <label>
              API Key
              <input
                type="password"
                value={settings.apiKey}
                onChange={event =>
                  setSettings(prev => ({ ...prev, apiKey: event.target.value }))
                }
              />
            </label>
          </form>
        </section>
      )}

      {payloadError && <div className="error-banner">{payloadError}</div>}

      <main className="content">
        {payload && (
          <section className="card">
            <header className="card-header">
              <div>
                <h2>{exceptionTitle}</h2>
                {payload.occurrenceTime && (
                  <span className="time">{formatDate(payload.occurrenceTime)}</span>
                )}
              </div>
              {payload.traceUrl && (
                <a
                  className="primary-link"
                  href={payload.traceUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  查看链路
                </a>
              )}
            </header>

            <div className="card-grid">
              <InfoRow label="异常类型" value={payload.exceptionType} />
              <InfoRow label="Trace ID" value={payload.traceId} />
              <InfoRow label="异常位置" value={payload.location} />
              <InfoRow label="异常描述" value={payload.exceptionMessage} />
            </div>

            {payload.author && (
              <section className="sub-card">
                <h3>代码提交者</h3>
                <div className="card-grid">
                  <InfoRow label="姓名" value={payload.author.name} />
                  <InfoRow label="邮箱" value={payload.author.email} />
                  <InfoRow label="最后提交时间" value={formatDate(payload.author.lastCommitTime)} />
                  <InfoRow label="文件位置" value={formatFileLocation(payload.author)} />
                  <InfoRow label="提交信息" value={payload.author.commitMessage} />
                </div>
              </section>
            )}

            {payload.codeContext && (
              <section className="sub-card">
                <h3>代码上下文</h3>
                <pre className="code-block">
                  <code>{payload.codeContext}</code>
                </pre>
              </section>
            )}

            {payload.stacktrace && (
              <section className="sub-card">
                <h3>堆栈信息</h3>
                <pre className="code-block">
                  <code>{payload.stacktrace}</code>
                </pre>
              </section>
            )}
          </section>
        )}

        <section className="card chat-panel">
          <header className="card-header">
            <div>
              <h2>对话分析</h2>
              <p className="hint">
                根据异常上下文向 AI 提问，获取进一步的定位与修复建议。
              </p>
            </div>
          </header>
          <div className="chat-window" ref={chatWindowRef}>
            {messages.map((message, index) => {
              const collapsible = isCollapsibleMessage(message);
              const collapsed =
                collapsible && collapsedMessages[index] !== undefined
                  ? collapsedMessages[index]
                  : collapsible;
              const preview = collapsible ? buildPreview(message.content) : null;

              return (
                <article
                  key={index}
                  className={`chat-message ${message.role}${collapsible ? ' collapsible' : ''}${
                    collapsed ? ' collapsed' : ''
                  }`}
                >
                  <div className="message-header">
                    <span className="role-label">{roleLabel(message.role)}</span>
                    {collapsible && (
                      <button
                        type="button"
                        className="collapse-button"
                        onClick={() => toggleCollapsed(index, collapsed)}
                      >
                        {collapsed ? '展开' : '收起'}
                      </button>
                    )}
                  </div>
                  {message.reasoning && (
                    <div className="reasoning-section">
                      <button
                        type="button"
                        className="reasoning-toggle"
                        onClick={() => {
                          setReasoningCollapsed(prev => ({
                            ...prev,
                            [index]: !prev[index]
                          }));
                        }}
                      >
                        <span className="reasoning-icon">🧠</span>
                        <span>思考过程</span>
                        <span className="toggle-arrow">
                          {reasoningCollapsed[index] ? '▼' : '▲'}
                        </span>
                      </button>
                      {!reasoningCollapsed[index] && (
                        <div className="reasoning-content">
                          <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            rehypePlugins={[rehypeHighlight]}
                          >
                            {message.reasoning}
                          </ReactMarkdown>
                        </div>
                      )}
                    </div>
                  )}
                  <div className="message-content">
                    {collapsible && collapsed ? (
                      <p className="collapsed-preview">{preview}</p>
                    ) : message.role === 'assistant' ? (
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        rehypePlugins={[rehypeHighlight]}
                      >
                        {message.content}
                      </ReactMarkdown>
                    ) : (
                      message.content.split('\n').map((line, lineIndex) => (
                        <p key={lineIndex}>{line}</p>
                      ))
                    )}
                  </div>
                </article>
              );
            })}
            {(streamingContent || streamingReasoning) && (
              <article className="chat-message assistant streaming">
                <div className="message-header">
                  <span className="role-label">AI</span>
                  <span className="streaming-indicator">正在生成...</span>
                </div>
                {streamingReasoning && (
                  <div className="reasoning-section">
                    <button
                      type="button"
                      className="reasoning-toggle"
                      onClick={() => setStreamingReasoningCollapsed(!streamingReasoningCollapsed)}
                    >
                      <span className="reasoning-icon">🧠</span>
                      <span>思考中...</span>
                      <span className="toggle-arrow">
                        {streamingReasoningCollapsed ? '▼' : '▲'}
                      </span>
                    </button>
                    {!streamingReasoningCollapsed && (
                      <div className="reasoning-content">
                        <ReactMarkdown
                          remarkPlugins={[remarkGfm]}
                          rehypePlugins={[rehypeHighlight]}
                        >
                          {streamingReasoning}
                        </ReactMarkdown>
                      </div>
                    )}
                  </div>
                )}
                {streamingContent && (
                  <div className="message-content">
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      rehypePlugins={[rehypeHighlight]}
                    >
                      {streamingContent}
                    </ReactMarkdown>
                  </div>
                )}
              </article>
            )}
          </div>
          <form className="chat-form" onSubmit={handleSubmit}>
            <textarea
              placeholder="描述你想了解的问题，按 Ctrl+Enter 发送"
              value={input}
              onChange={event => setInput(event.target.value)}
              onKeyDown={event => {
                if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
                  event.preventDefault();
                  handleSubmit(event);
                }
              }}
            />
            <button type="submit" disabled={isSending}>
              {isSending ? '发送中...' : '发送'}
            </button>
          </form>
          {sendError && <div className="error-banner">{sendError}</div>}
        </section>
      </main>
    </div>
  );
}

type InfoRowProps = {
  label: string;
  value?: string | number | null;
};

function InfoRow({ label, value }: InfoRowProps) {
  if (value === undefined || value === null || value === '') {
    return null;
  }
  const text = String(value);
  return (
    <div className="info-row">
      <span className="info-label">{label}</span>
      <span className="info-value" title={text}>
        {text}
      </span>
    </div>
  );
}

function decodeBase64Url(input: string): Uint8Array {
  let base64 = input.replace(/-/g, '+').replace(/_/g, '/');
  const pad = base64.length % 4;
  if (pad) {
    base64 += '='.repeat(4 - pad);
  }
  const binary = window.atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function buildSummaryPrompt(payload: AiAnalysisPayload): string | null {
  if (!payload) {
    return null;
  }
  const lines: string[] = ['[异常概览]', `应用：${payload.appName ?? '未知'}`];
  if (payload.environment) {
    lines.push(`环境：${payload.environment}`);
  }
  if (payload.exceptionType) {
    lines.push(`类型：${payload.exceptionType}`);
  }
  if (payload.exceptionMessage) {
    lines.push(`描述：${payload.exceptionMessage}`);
  }
  if (payload.location) {
    lines.push(`位置：${payload.location}`);
  }
  if (payload.traceId) {
    lines.push(`Trace ID：${payload.traceId}`);
  }
  if (payload.traceUrl) {
    lines.push(`Trace URL：${payload.traceUrl}`);
  }

  if (payload.codeContext) {
    lines.push('\n[代码上下文]', payload.codeContext);
  }
  if (payload.stacktrace) {
    lines.push('\n[堆栈信息]', limitLines(payload.stacktrace, 40));
  }
  if (payload.author) {
    lines.push(
      '\n[代码作者]',
      [
        payload.author.name && `姓名：${payload.author.name}`,
        payload.author.email && `邮箱：${payload.author.email}`,
        payload.author.commitMessage && `提交：${payload.author.commitMessage}`
      ]
        .filter(Boolean)
        .join('；')
    );
  }
  return lines.filter(Boolean).join('\n');
}

function limitLines(text: string, maxLines: number): string {
  const lines = text.split('\n');
  if (lines.length <= maxLines) {
    return text;
  }
  return `${lines.slice(0, maxLines).join('\n')}\n...（后续合计 ${lines.length - maxLines} 行已省略）`;
}

function formatDate(value?: string | null): string | undefined {
  if (!value) {
    return undefined;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function formatFileLocation(author?: AiAnalysisPayload['author']): string | undefined {
  if (!author) {
    return undefined;
  }
  if (!author.fileName) {
    return undefined;
  }
  if (typeof author.lineNumber === 'number' && author.lineNumber > 0) {
    return `${author.fileName}:${author.lineNumber}`;
  }
  return author.fileName;
}

function roleLabel(role: ChatMessage['role']): string {
  switch (role) {
    case 'assistant':
      return 'AI';
    case 'user':
      return '你';
    case 'system':
      return '系统';
    default:
      return role;
  }
}

function formatApiError(status: number, statusText: string, body: string): string {
  const snippet = body ? body.slice(0, 200) : '';
  if (status === 401) {
    return '401 未授权：请检查 API Key 是否填写正确。';
  }
  if (status === 404) {
    return '404 未找到接口：请确认 Endpoint / 模型路径配置是否正确。' + (snippet ? ` 服务器返回：${snippet}` : '');
  }
  if (status === 429) {
    return '429 频率受限：请稍后重试或降低调用频率。';
  }
  if (status >= 500) {
    return `服务端错误 ${status}：${statusText || ''}`.trim() + (snippet ? `，响应内容：${snippet}` : '');
  }
  return `调用失败 ${status}${statusText ? ' ' + statusText : ''}${snippet ? `：${snippet}` : ''}`;
}

function isCollapsibleMessage(message: ChatMessage): boolean {
  if (message.role === 'system') {
    return true;
  }
  if (message.role === 'user' && message.content.startsWith('[异常概览]')) {
    return true;
  }
  return false;
}

function buildPreview(content: string): string {
  const lines = content.split('\n').map(line => line.trim()).filter(Boolean);
  if (lines.length <= 2) {
    return lines.join(' ');
  }
  return `${lines.slice(0, 2).join(' ')} …`;
}
