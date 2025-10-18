'use client';

import {FormEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {ungzip} from 'pako';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css';

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
  additionalInfo?: string;
  author?: {
    name?: string;
    email?: string;
    lastCommitTime?: string;
    fileName?: string;
    lineNumber?: number;
    commitMessage?: string;
  };
}

type ToolCall = {
  id?: string;
  type?: string;
  mcp?: {
    name?: string;
    arguments?: unknown;
    [key: string]: unknown;
  };
  function?: {
    name?: string;
    arguments?: unknown;
  };
};

type ChatMessage = {
  role: 'system' | 'user' | 'assistant' | 'tool';
  content: string;
  tool_calls?: ToolCall[];
  reasoning?: string;
  tool_call_id?: string;
};

type ClientSettings = {
  endpoint: string;
  apiKey: string;
  model: string;
  temperature: number;
  systemPrompt: string;
};

type McpTransportType = 'streamable-http' | 'sse';

type McpTool = {
  name: string;
  title: string;
  description: string;
  inputSchema?: unknown;
  outputSchema?: unknown;
};

type McpServerInfo = {
  name?: string;
  version?: string;
  description?: string;
};

type McpServerData = {
  name?: string;
  info?: unknown;
  tools?: McpTool[];
  error?: string;
};

const extractServerInfo = (server?: McpServerData, fallbackName?: string): McpServerInfo | null => {
  const fallback = typeof fallbackName === 'string' && fallbackName.trim().length > 0 ? fallbackName.trim() : undefined;

  if (!server) {
    return fallback ? { name: fallback } : null;
  }

  const info =
    server.info && typeof server.info === 'object' && !Array.isArray(server.info)
      ? (server.info as Record<string, unknown>)
      : {};

  const infoName = typeof info.name === 'string' && info.name.trim().length > 0 ? info.name.trim() : undefined;
  const infoVersion =
    typeof info.version === 'string' && info.version.trim().length > 0 ? info.version.trim() : undefined;
  const infoDescription =
    typeof info.description === 'string' && info.description.trim().length > 0 ? info.description.trim() : undefined;

  const resolvedName =
    (typeof server.name === 'string' && server.name.trim().length > 0 ? server.name.trim() : undefined) ||
    infoName ||
    fallback;

  if (!resolvedName && !infoVersion && !infoDescription) {
    return fallback ? { name: fallback } : null;
  }

  const result: McpServerInfo = {};
  if (resolvedName) {
    result.name = resolvedName;
  }
  if (infoVersion) {
    result.version = infoVersion;
  }
  if (infoDescription) {
    result.description = infoDescription;
  }

  return result;
};

type McpExecutionResult = {
  toolName: string;
  toolTitle: string;
  plainText: string | null;
  structuredContent: unknown | null;
  content: unknown[];
  isError: boolean;
  executedAt: string;
  server?: McpServerInfo | null;
};

type McpServer = {
  id: string;
  name: string;
  baseUrl: string;
  headersText: string;
  transportType: McpTransportType;
  isActive: boolean;
};

type McpSettings = {
  servers: McpServer[];
};

type McpServerFormState = {
  name: string;
  baseUrl: string;
  headersText: string;
  transportType: McpTransportType;
};

const DEFAULT_SERVER_FORM: McpServerFormState = {
  name: '',
  baseUrl: '',
  headersText: '{"Authorization": "Bearer token"}',
  transportType: 'streamable-http'
};

const SETTINGS_KEY = 'exception-notify-ai-settings';
const MCP_SETTINGS_KEY = 'exception-notify-mcp-settings';

const defaultSystemPrompt =
  '你是一个资深 Java/Spring 工程师，擅长分析异常堆栈并提供修复建议。请结合提供的上下文，输出简洁明确、可执行的建议。';

const defaultSettings: ClientSettings = {
  endpoint: 'https://api.openai.com/v1/chat/completions',
  apiKey: '',
  model: 'gpt-4o-mini',
  temperature: 0.2,
  systemPrompt: defaultSystemPrompt
};

const defaultMcpSettings: McpSettings = {
  servers: []
};

const DEMO_PAYLOAD = 'H4sIAAAAAAAAAK1UTW8TMRD9K8OeUqnZ7KZJPxa1UBWQeuiHINxycbyT1NRrL7Y3tKp658IRceAENy4gLgjK36HQn8HYu6GhpUJCnHY98-bpjf1mTiJWlruswCiLCiZlW5scTduimQqO0WKEaiqMVgUqR5DS6JyCmvPKGFQcByKUdpNuv52k7bQ_SJMs6Wdp19cecSyd0GpwXHrUEzZlsWRqEu9WUu5roRya-zPQfMEOWssmvub8-evzr2cX79-ev3j5_dWn-uf8w5eLj2--fT778e6MyqTmLDBkEddF7PuIQx9x00e850-PmgM3yByGUOu3hNeXpb3VBeK0jvFDZxj_u_AMtphS2oFQU32IMIwCazxBt-2wsK2FYQQj5KyyPhmUUURYUMQ2VEPHHPwf4X8mGzPO8obrQf1vq1Eh3BxVEw9M_d6MyFYqNjiWyF28S3c8xR10Bzrf5JxeSJvtopRx3XbSqgFQI_6BoXVjPqha7l5r7xmOYq6VM1pK6jM0snV5LiU931yPc6nAuLriX5rrHH0Gj7zD014XIKAhXB6s19-H-LRC63ZoXKjc6fvKCXfcMnV44fZQpb0lgCmTIv_1SKGyzvUAOJO8kpTcN7rQ3jnzgD6A_ywDdDow2Lu3l8EBU7nE4JJGDDIjBbmHgCsAUk_iHEfVpDWMNke6cuA0kDwrrKsLMjg5HUaLTfWt9ZrrTn0OBs1bC5CRK4MVIy9lY2MDyEsw67vUVjhtjmPLpjiveA3AOs0PZy6kMTxs6SvWD9B-AmDQVUbVpBSjiw_TtZ3TpYe_tl8u7aXxWjfnLO0ma6MZ5rGRBDpwrrRZpxNCMR4xcgbNhC46Iu_czMAqspOJspNI1WtuU5JY2DrAsHEKJjw5k2HAKHh3jtnvFmbdli5oWK6uumSQrGVL3SxNCTYWEps1em0yPYtQuFsVIyQhdLfedJ7ycs2NxVHWvFJjIbJHY4B6T0Bjtej09CfuTiRZtQUAAA';
const DEMO_SHORT_CODE = '6abdc76d4ea8a70a08420300b52ff8d3483bb2633c185c2dc709cd5851d8144c';
const DEMO_KV: Record<string, string> = {
  [DEMO_SHORT_CODE]: DEMO_PAYLOAD
};
const textDecoder = new TextDecoder();

const repositoryUrl = 'https://github.com/GuangYiDing/exception-notify';
const rawBuildSha = (process.env.NEXT_PUBLIC_BUILD_SHA ?? '').trim();
const buildSha = rawBuildSha || 'dev';
const buildShaDisplay = buildSha.length > 7 ? buildSha.slice(0, 7) : buildSha;
const isDevBuild = buildSha === 'dev';
const buildShaUrl = isDevBuild ? repositoryUrl : `${repositoryUrl}/commit/${buildSha}`;

export default function App() {
  const [payload, setPayload] = useState<AiAnalysisPayload | null>(null);
  const [payloadError, setPayloadError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('根据以上信息,深入分析解决这个问题');
  const [isSending, setIsSending] = useState(false);
  const [collapsedMessages, setCollapsedMessages] = useState<Record<number, boolean>>({});
  const [settings, setSettings] = useState<ClientSettings>(() => {
    if (typeof window === 'undefined') {
      return defaultSettings;
    }
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
              : defaultSettings.temperature,
          systemPrompt: parsed.systemPrompt ?? defaultSettings.systemPrompt
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
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [showCopyToast, setShowCopyToast] = useState(false);
  const [editingCodeContext, setEditingCodeContext] = useState(false);
  const [editingStacktrace, setEditingStacktrace] = useState(false);
  const [editingAdditionalInfo, setEditingAdditionalInfo] = useState(false);
  const [codeContextDraft, setCodeContextDraft] = useState('');
  const [stacktraceDraft, setStacktraceDraft] = useState('');
  const [additionalInfoDraft, setAdditionalInfoDraft] = useState('');
  const [mcpSettings, setMcpSettings] = useState<McpSettings>(() => {
    if (typeof window === 'undefined') {
      return defaultMcpSettings;
    }
    try {
      const raw = localStorage.getItem(MCP_SETTINGS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        // Handle migration from old format
        if (parsed.baseUrl || parsed.headersText) {
          // Migrate old single server format to new array format
          return {
            servers: parsed.baseUrl
              ? [
                  {
                    id: 'migrated-server',
                    name: 'Migrated Server',
                    baseUrl: parsed.baseUrl,
                    headersText: parsed.headersText || '',
                    transportType: 'streamable-http' as McpTransportType,
                    isActive: true
                  }
                ]
              : []
          };
        }
        // Handle new array format
        if (Array.isArray(parsed.servers)) {
          return {
            servers: parsed.servers.map((server: any) => ({
              id: typeof server.id === 'string' ? server.id : Date.now().toString(),
              name: typeof server.name === 'string' ? server.name : 'Unnamed Server',
              baseUrl: typeof server.baseUrl === 'string' ? server.baseUrl : '',
              headersText: typeof server.headersText === 'string' ? server.headersText : '',
              transportType:
                server.transportType === 'sse' ? 'sse' : 'streamable-http',
              isActive: Boolean(server.isActive)
            }))
          };
        }
      }
    } catch (error) {
      console.warn('Failed to load MCP settings', error);
    }
    return defaultMcpSettings;
  });
  const [mcpPanelOpen, setMcpPanelOpen] = useState(false);
  const [mcpLoading, setMcpLoading] = useState(false);
  const [mcpExecuting, setMcpExecuting] = useState(false);
  const [mcpTools, setMcpTools] = useState<McpTool[]>([]);
  const [mcpServerInfo, setMcpServerInfo] = useState<McpServerInfo | null>(null);
  const [mcpSelectedTool, setMcpSelectedTool] = useState('');
  const [mcpArgumentsText, setMcpArgumentsText] = useState('{}');
  const [mcpResult, setMcpResult] = useState<McpExecutionResult | null>(null);
  const [mcpError, setMcpError] = useState<string | null>(null);
  const [mcpHeadersError, setMcpHeadersError] = useState<string | null>(null);
  const [mcpArgumentError, setMcpArgumentError] = useState<string | null>(null);

  // Server connection status tracking
  const [serverConnectionStatus, setServerConnectionStatus] = useState<Record<string, 'connected' | 'disconnected' | 'loading'>>({});

  // Server management states
  const [editingServer, setEditingServer] = useState<string | null>(null);
  const [serverForm, setServerForm] = useState<McpServerFormState>(() => ({ ...DEFAULT_SERVER_FORM }));
  const [showAddServer, setShowAddServer] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<{ show: boolean; serverId: string; serverName: string }>({
    show: false,
    serverId: '',
    serverName: ''
  });

  useEffect(() => {
    const resolvePayload = async () => {
      try {
        const url = new URL(window.location.href);
        const searchParams = url.searchParams;
        const payloadParamName = searchParams.get('payloadParam') || 'payload';
        const encoded = searchParams.get(payloadParamName);
        if (!encoded) {
          setPayloadError('missing-payload');
          return;
        }

        let compressed = DEMO_KV[encoded];
        if (!compressed) {
          const response = await fetch(`/api/decompress?payload=${encodeURIComponent(encoded)}`);
          if (!response.ok) {
            throw new Error(`Failed to fetch payload: ${response.status}`);
          }
          const body: { code?: number; data?: string; message?: string } = await response.json();
          if (body.code !== 0 || typeof body.data !== 'string') {
            throw new Error(body.message || 'Invalid payload response');
          }
          compressed = body.data;
        }

        const bytes = decodeBase64Url(compressed);
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
    };

    resolvePayload();
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    
    // Update system message when system prompt changes
    setMessages(prev => {
      const systemIndex = prev.findIndex(msg => msg.role === 'system');
      if (systemIndex === -1) {
        // Add system message if it doesn't exist
        return [{ role: 'system', content: settings.systemPrompt }, ...prev];
      }
      // Update existing system message
      const newMessages = [...prev];
      newMessages[systemIndex] = {
        ...newMessages[systemIndex],
        content: settings.systemPrompt
      };
      return newMessages;
    });
  }, [settings]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }
    localStorage.setItem(MCP_SETTINGS_KEY, JSON.stringify(mcpSettings));
  }, [mcpSettings]);

  // Auto load MCP tools when settings change and panel is open
  const toggleCollapsed = (index: number, current: boolean) => {
    setCollapsedMessages(prev => ({
      ...prev,
      [index]: !current
    }));
  };

  const copyToClipboard = async (content: string, index: number) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedIndex(index);
      setTimeout(() => setCopiedIndex(null), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const sendMessage = async (userMessage: string, baseMessages?: ChatMessage[], skipAddUserMessage = false) => {
    if (!settings.apiKey.trim()) {
      setSendError('请先在设置中填写 API Key。');
      setSettingsOpen(true);
      return;
    }

    setSendError(null);
    const currentMessages = baseMessages || messages;
    const newMessages: ChatMessage[] = skipAddUserMessage
      ? currentMessages
      : [
          ...currentMessages,
          { role: 'user', content: userMessage }
        ];
    if (!skipAddUserMessage) {
      setMessages(newMessages);
    }
    setIsSending(true);
    setStreamingContent('');
    setStreamingReasoning('');
    setStreamingReasoningCollapsed(false);

    try {
      // Check if model is GLM and has available MCP services
      const isGlmModel = settings.model.toLowerCase().startsWith('glm');
      const activeServer = mcpSettings.servers.find(server => server.isActive);
      const hasMcpService = isGlmModel && activeServer && activeServer.baseUrl.trim();

      // Prepare request body
      const requestBody: any = {
        model: settings.model,
        temperature: settings.temperature,
        stream: true,
        messages: newMessages.map(message => {
          const payload: Record<string, unknown> = {
            role: message.role,
            content: message.content
          };
          if (message.tool_call_id) {
            payload.tool_call_id = message.tool_call_id;
          }
          if (message.tool_calls && message.tool_calls.length > 0) {
            payload.tool_calls = message.tool_calls;
          }
          return payload;
        })
      };

      // Add MCP tools if GLM model with MCP service is available
      if (hasMcpService) {
        try {
          // Get available MCP tools
          let headers: any;
          if (activeServer.headersText.trim()) {
            headers = JSON.parse(activeServer.headersText);
          }

          const mcpResponse = await fetch('/api/mcp/tools/list', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              servers: [
                {
                  baseUrl: activeServer.baseUrl,
                  headers,
                  name: activeServer.name
                }
              ]
            })
          });

          const mcpResult = await mcpResponse.json() as {
            code: number;
            message?: string;
            data?: { servers?: McpServerData[] };
          };

          const serverDataList = mcpResult.data?.servers ?? [];
          const serverData =
            serverDataList.find(
              item => item.name && activeServer.name && item.name === activeServer.name
            ) ?? serverDataList[0];
          if (serverData?.error) {
            console.warn('MCP server responded with an error:', serverData.error);
          }

          if (mcpResult.code === 0 && serverData && !serverData.error && serverData.tools && serverData.tools.length > 0) {
            console.log('🛠️ Found MCP tools:', serverData.tools.map(t => t.name));

            // Transform MCP tools to GLM MCP tool schema
            const transportType: McpTransportType = activeServer.transportType ?? 'streamable-http';
            const toolHeaders =
              headers && typeof headers === 'object' && Object.keys(headers).length > 0
                ? headers
                : undefined;
            const mcpTools = serverData.tools.map(tool => ({
              type: 'mcp',
              mcp: {
                server_label: serverData.name ?? activeServer.name,
                server_url: activeServer.baseUrl,
                transport_type: transportType,
                allowed_tools: [tool.name],
                ...(toolHeaders ? { headers: toolHeaders } : {})
              }
            }));

            console.log('🔧 Transformed MCP tools for GLM:', JSON.stringify(mcpTools, null, 2));

            // Add MCP tools to request
            requestBody.tools = mcpTools;
            requestBody.tool_choice = 'auto';

            console.log('📤 Enhanced GLM request with MCP tools');
          }
        } catch (error) {
          console.warn('Failed to load MCP tools for GLM request:', error);
          // Continue without MCP tools if loading fails
        }
      }

      const response = await fetch(settings.endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${settings.apiKey}`
        },
        body: JSON.stringify(requestBody)
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
      let accumulatedToolCalls: any[] = [];

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

            // Handle tool calls for GLM models
            if (delta?.tool_calls) {
              console.log('Received tool_calls delta:', delta.tool_calls);
              delta.tool_calls.forEach((toolCall: any, index: number) => {
                console.log(`Processing tool call ${index}:`, toolCall);
                if (!accumulatedToolCalls[index]) {
                  accumulatedToolCalls[index] = {
                    id: toolCall.id || '',
                    type: toolCall.type || 'mcp',
                    mcp: toolCall.mcp || {},
                    function: toolCall.function || null
                  };
                } else {
                  // Update existing tool call
                  if (toolCall.id) accumulatedToolCalls[index].id = toolCall.id;
                  if (toolCall.type) accumulatedToolCalls[index].type = toolCall.type;
                  if (toolCall.mcp) {
                    accumulatedToolCalls[index].mcp = {
                      ...accumulatedToolCalls[index].mcp,
                      ...toolCall.mcp
                    };
                  }
                  if (toolCall.function) {
                    accumulatedToolCalls[index].function = {
                      ...accumulatedToolCalls[index].function,
                      ...toolCall.function
                    };
                  }
                }
              });
            }
          } catch (err) {
            console.warn('Failed to parse SSE line', trimmed, err);
          }
        }
      }

      // Handle tool calls if present
      if (accumulatedToolCalls.length > 0) {
        // Execute MCP tools and continue conversation
        await handleToolCalls(accumulatedToolCalls, newMessages, accumulatedContent, accumulatedReasoning);
      } else {
        // Regular response without tools
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
      }
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

  const handleToolCalls = async (
    toolCalls: any[],
    conversationMessages: ChatMessage[],
    assistantContent: string,
    assistantReasoning?: string
  ) => {
    const activeServer = mcpSettings.servers.find(server => server.isActive);
    if (!activeServer) {
      setSendError('没有激活的 MCP 服务器来处理工具调用');
      return;
    }

    const toolCallsForHistory: ToolCall[] = toolCalls.map((toolCall: any) => {
      const sanitized: ToolCall = {
        id: typeof toolCall.id === 'string' ? toolCall.id : undefined,
        type: typeof toolCall.type === 'string' ? toolCall.type : undefined
      };
      if (toolCall.mcp && typeof toolCall.mcp === 'object') {
        sanitized.mcp = { ...(toolCall.mcp as Record<string, unknown>) };
      }
      if (toolCall.function && typeof toolCall.function === 'object') {
        sanitized.function = { ...(toolCall.function as Record<string, unknown>) };
      }
      return sanitized;
    });

    // Add assistant message with tool calls
    const assistantMessage: ChatMessage = {
      role: 'assistant',
      content: assistantContent || '我正在使用工具来帮助分析这个问题...',
      reasoning: assistantReasoning,
      tool_calls: toolCallsForHistory
    };
    setMessages(prev => [...prev, assistantMessage]);

    // Execute each tool call
    const toolResults: any[] = [];
    for (const toolCall of toolCalls) {
      try {
        console.log('Executing tool call:', JSON.stringify(toolCall, null, 2));

        let headers: any;
        if (activeServer.headersText.trim()) {
          headers = JSON.parse(activeServer.headersText);
        }

        // Extract tool name and arguments based on GLM's response format
        let toolName: string | undefined;
        let toolArgs: any;

        if (toolCall.mcp) {
          // GLM MCP format
          toolName = toolCall.mcp.name;
          toolArgs = toolCall.mcp.arguments;
        } else if (toolCall.function) {
          // OpenAI function format
          toolName = toolCall.function.name;
          toolArgs = toolCall.function.arguments;
        }

        console.log('Extracted tool name:', toolName);
        console.log('Extracted tool args:', toolArgs);

        if (!toolName) {
          throw new Error('Tool name not found in tool call response');
        }

        const requestBody = {
          baseUrl: activeServer.baseUrl,
          headers,
          name: toolName,
          arguments: toolArgs
        };

        console.log('MCP execute request body:', JSON.stringify(requestBody, null, 2));

        const response = await fetch('/api/mcp/tools/execute', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
          const errorText = await response.text();
          console.error('MCP execute API error:', response.status, errorText);
          throw new Error(`MCP API error ${response.status}: ${errorText}`);
        }

        const result = await response.json() as { code: number; message?: string; data?: { content: unknown[]; plainText: string | null; structuredContent: unknown | null; isError: boolean; server: McpServerInfo } };

        console.log('MCP execute response:', JSON.stringify(result, null, 2));

        if (result.code === 0) {
          toolResults.push({
            tool_call_id: toolCall.id,
            role: 'tool',
            content: JSON.stringify({
              success: true,
              result: result.data?.plainText || result.data?.structuredContent,
              server: result.data?.server?.name
            })
          });
        } else {
          console.error('MCP tool execution failed:', result.message);
          toolResults.push({
            tool_call_id: toolCall.id,
            role: 'tool',
            content: JSON.stringify({
              success: false,
              error: result.message || '工具执行失败'
            })
          });
        }
      } catch (error) {
        console.error('Failed to execute MCP tool:', error);
        toolResults.push({
          tool_call_id: toolCall.id,
          role: 'tool',
          content: JSON.stringify({
            success: false,
            error: error instanceof Error ? error.message : '工具执行出现异常'
          })
        });
      }
    }

    // Continue conversation with tool results
    const messagesWithTools = [
      ...conversationMessages,
      assistantMessage,
      ...toolResults.map(result => ({
        role: result.role as 'tool',
        content: result.content,
        tool_call_id: result.tool_call_id
      }))
    ];

    console.log('🔄 Sending tool results back to GLM:', messagesWithTools.length, 'messages');

    // Send messages with tool results back to GLM model
    await sendMessage('', messagesWithTools, true);
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

  const handleRegenerate = async (index: number) => {
    if (isSending) return;
    
    // Find the user message before this assistant message
    const userMessageIndex = index - 1;
    if (userMessageIndex < 0 || messages[userMessageIndex].role !== 'user') {
      return;
    }
    
    // Remove the assistant message we want to regenerate
    const messagesBeforeAssistant = messages.slice(0, index);
    setMessages(messagesBeforeAssistant);
    
    // Resend the user message (don't add it again since it's already in messagesBeforeAssistant)
    const userMessage = messages[userMessageIndex].content;
    await sendMessage(userMessage, messagesBeforeAssistant, true);
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

  const handleCopySuccess = () => {
    setShowCopyToast(true);
    setTimeout(() => setShowCopyToast(false), 2000);
  };

  const handleEditCodeContext = () => {
    setCodeContextDraft(payload?.codeContext || '');
    setEditingCodeContext(true);
  };

  const handleSaveCodeContext = () => {
    if (payload) {
      setPayload({ ...payload, codeContext: codeContextDraft });
    }
    setEditingCodeContext(false);
  };

  const handleCancelCodeContext = () => {
    setEditingCodeContext(false);
    setCodeContextDraft('');
  };

  const handleEditStacktrace = () => {
    setStacktraceDraft(payload?.stacktrace || '');
    setEditingStacktrace(true);
  };

  const handleSaveStacktrace = () => {
    if (payload) {
      setPayload({ ...payload, stacktrace: stacktraceDraft });
    }
    setEditingStacktrace(false);
  };

  const handleCancelStacktrace = () => {
    setEditingStacktrace(false);
    setStacktraceDraft('');
  };

  const handleEditAdditionalInfo = () => {
    setAdditionalInfoDraft(payload?.additionalInfo || '');
    setEditingAdditionalInfo(true);
  };

  const handleSaveAdditionalInfo = () => {
    if (payload) {
      setPayload({ ...payload, additionalInfo: additionalInfoDraft });
    }
    setEditingAdditionalInfo(false);
  };

  const handleCancelAdditionalInfo = () => {
    setEditingAdditionalInfo(false);
    setAdditionalInfoDraft('');
  };

  const loadMcpTools = useCallback(async () => {
    const activeServer = mcpSettings.servers.find(server => server.isActive);
    if (!activeServer || !activeServer.baseUrl.trim()) {
      setMcpError('请先配置并激活一个 MCP 服务器');
      return;
    }

    setMcpLoading(true);
    setMcpError(null);

    // Set server status to loading
    setServerConnectionStatus(prev => ({
      ...prev,
      [activeServer.id]: 'loading'
    }));

    try {
      let headers;
      if (activeServer.headersText.trim()) {
        try {
          headers = JSON.parse(activeServer.headersText);
        } catch (error) {
          setMcpError('请求头格式错误，请输入有效的 JSON');
          setServerConnectionStatus(prev => ({
            ...prev,
            [activeServer.id]: 'disconnected'
          }));
          return;
        }
      }

      const response = await fetch('/api/mcp/tools/list', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          servers: [
            {
              baseUrl: activeServer.baseUrl,
              headers,
              name: activeServer.name
            }
          ]
        })
      });

      const result = await response.json() as {
        code: number;
        message?: string;
        data?: { servers?: McpServerData[] };
      };
      const serverDataList = result.data?.servers ?? [];
      const serverData =
        serverDataList.find(
          item => item.name && activeServer.name && item.name === activeServer.name
        ) ?? serverDataList[0];
      if (serverData?.error) {
        throw new Error(serverData.error);
      }

      if (result.code !== 0 || !serverData) {
        throw new Error(result.message || 'Failed to load MCP tools');
      }

      setMcpTools(serverData.tools ?? []);
      setMcpServerInfo(extractServerInfo(serverData, activeServer.name));

      // Set server status to connected when tools are successfully loaded
      setServerConnectionStatus(prev => ({
        ...prev,
        [activeServer.id]: 'connected'
      }));
    } catch (error) {
      console.error('Failed to load MCP tools', error);
      setMcpError(error instanceof Error ? error.message : '加载 MCP 工具失败');

      // Set server status to disconnected on error
      setServerConnectionStatus(prev => ({
        ...prev,
        [activeServer.id]: 'disconnected'
      }));
    } finally {
      setMcpLoading(false);
    }
  }, [mcpSettings]);

  useEffect(() => {
    if (mcpPanelOpen) {
      const activeServer = mcpSettings.servers.find(server => server.isActive);
      if (activeServer && activeServer.baseUrl.trim()) {
        loadMcpTools();
      }
    }
  }, [mcpPanelOpen, mcpSettings.servers, loadMcpTools]);

  // Sync payload changes to messages
  useEffect(() => {
    if (!payload) return;

    const summaryMessage = buildSummaryPrompt(payload);
    if (summaryMessage) {
      setMessages(prev => {
        const summaryIndex = prev.findIndex(
          msg => msg.role === 'user' && msg.content.startsWith('[异常概览]')
        );
        if (summaryIndex === -1) {
          return prev;
        }
        // Update existing summary message
        const newMessages = [...prev];
        newMessages[summaryIndex] = {
          ...newMessages[summaryIndex],
          content: summaryMessage
        };
        return newMessages;
      });
    }
  }, [payload]);

  // Auto scroll to bottom when streaming or new messages arrive
  useEffect(() => {
    if (chatWindowRef.current) {
      chatWindowRef.current.scrollTop = chatWindowRef.current.scrollHeight;
    }
  }, [messages, streamingContent]);

  const executeMcpTool = async () => {
    if (!mcpSelectedTool) {
      setMcpError('请选择要执行的工具');
      return;
    }

    const activeServer = mcpSettings.servers.find(server => server.isActive);
    if (!activeServer) {
      setMcpError('没有激活的 MCP 服务器');
      return;
    }

    setMcpExecuting(true);
    setMcpError(null);

    try {
      let headers;
      if (activeServer.headersText.trim()) {
        headers = JSON.parse(activeServer.headersText);
      }

      let argumentsObj;
      if (mcpArgumentsText.trim()) {
        try {
          argumentsObj = JSON.parse(mcpArgumentsText);
        } catch (error) {
          setMcpArgumentError('参数格式错误，请输入有效的 JSON');
          return;
        }
      }

      const response = await fetch('/api/mcp/tools/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          baseUrl: activeServer.baseUrl,
          headers,
          name: mcpSelectedTool,
          arguments: argumentsObj
        })
      });

      const result = await response.json() as { code: number; message?: string; data?: { content: unknown[]; plainText: string | null; structuredContent: unknown | null; isError: boolean; server: McpServerInfo } };
      if (result.code !== 0) {
        throw new Error(result.message || 'Failed to execute MCP tool');
      }

      const executionResult: McpExecutionResult = {
        toolName: mcpSelectedTool,
        toolTitle: mcpTools.find(t => t.name === mcpSelectedTool)?.title || mcpSelectedTool,
        plainText: result.data!.plainText,
        structuredContent: result.data!.structuredContent,
        content: result.data!.content,
        isError: result.data!.isError,
        executedAt: new Date().toISOString(),
        server: result.data!.server
      };

      setMcpResult(executionResult);
      setMcpArgumentError(null);
    } catch (error) {
      console.error('Failed to execute MCP tool', error);
      setMcpError(error instanceof Error ? error.message : '执行 MCP 工具失败');
    } finally {
      setMcpExecuting(false);
    }
  };

  // Server management functions
  const addServer = () => {
    if (!serverForm.name.trim() || !serverForm.baseUrl.trim()) {
      return;
    }

    const newServerId = Date.now().toString();
    const newServer: McpServer = {
      id: newServerId,
      name: serverForm.name.trim(),
      baseUrl: serverForm.baseUrl.trim(),
      headersText: serverForm.headersText.trim(),
      transportType: serverForm.transportType,
      isActive: true // Always activate new server by default
    };

    setMcpSettings(prev => {
      // Deactivate all existing servers and activate the new one
      const updatedServers = prev.servers.map(server => ({
        ...server,
        isActive: false
      }));
      return {
        ...prev,
        servers: [...updatedServers, newServer]
      };
    });

    // Initialize connection status for new server
    setServerConnectionStatus(prev => ({
      ...prev,
      [newServerId]: 'disconnected'
    }));

    // Clear MCP related states when adding new server
    setMcpServerInfo(null);
    setMcpTools([]);
    setMcpSelectedTool('');
    setMcpResult(null);
    setMcpError(null);

    setServerForm({ ...DEFAULT_SERVER_FORM });
    setShowAddServer(false);

    // Auto load tools for the new server after a short delay
    setTimeout(() => {
      loadMcpTools();
    }, 100);
  };

  const updateServer = (serverId: string) => {
    if (!serverForm.name.trim() || !serverForm.baseUrl.trim()) {
      return;
    }

    setMcpSettings(prev => ({
      ...prev,
      servers: prev.servers.map(server =>
        server.id === serverId
          ? {
              ...server,
              name: serverForm.name.trim(),
              baseUrl: serverForm.baseUrl.trim(),
              headersText: serverForm.headersText.trim(),
              transportType: serverForm.transportType
            }
          : server
      )
    }));

    setServerForm({ ...DEFAULT_SERVER_FORM });
    setEditingServer(null);
  };

  const deleteServer = (serverId: string) => {
    const serverToDelete = mcpSettings.servers.find(s => s.id === serverId);
    const remainingServers = mcpSettings.servers.filter(s => s.id !== serverId);

    // Clear connection status for the deleted server
    setServerConnectionStatus(prev => {
      const newStatus = { ...prev };
      delete newStatus[serverId];
      return newStatus;
    });

    // Clear MCP related states when deleting a server
    setMcpServerInfo(null);
    setMcpTools([]);
    setMcpSelectedTool('');
    setMcpResult(null);
    setMcpError(null);

    if (remainingServers.length === 0) {
      // If no servers left, just remove the deleted one
      setMcpSettings(prev => ({
        ...prev,
        servers: []
      }));
    } else {
      // If deleting active server, activate another one
      if (serverToDelete?.isActive) {
        const newActiveServer = remainingServers[0];
        setMcpSettings(prev => ({
          ...prev,
          servers: remainingServers.map((server, index) => ({
            ...server,
            isActive: index === 0 // First remaining server becomes active
          }))
        }));

        // Reset connection status for the new active server
        setServerConnectionStatus(prev => ({
          ...prev,
          [newActiveServer.id]: 'disconnected'
        }));
      } else {
        // Just remove the server
        setMcpSettings(prev => ({
          ...prev,
          servers: prev.servers.filter(server => server.id !== serverId)
        }));
      }
    }
  };

  const activateServer = (serverId: string) => {
    setMcpSettings(prev => ({
      ...prev,
      servers: prev.servers.map(server => ({
        ...server,
        isActive: server.id === serverId
      }))
    }));

    // Reset connection status for the newly activated server
    setServerConnectionStatus(prev => ({
      ...prev,
      [serverId]: 'disconnected'
    }));

    // Clear MCP related states when switching servers
    setMcpServerInfo(null);
    setMcpTools([]);
    setMcpSelectedTool('');
    setMcpResult(null);
    setMcpError(null);
  };

  const startEditServer = (server: McpServer) => {
    setEditingServer(server.id);
    setServerForm({
      name: server.name,
      baseUrl: server.baseUrl,
      headersText: server.headersText,
      transportType: server.transportType
    });
  };

  const cancelEdit = () => {
    setEditingServer(null);
    setServerForm({ ...DEFAULT_SERVER_FORM });
    setShowAddServer(false);
  };

  const loadDemoPayload = () => {
    const url = new URL(window.location.href);
      url.searchParams.set('payload', DEMO_SHORT_CODE);
    window.location.href = url.toString();
  };

  return (
    <div className="app-container">
      {showCopyToast && (
        <div className="copy-toast">
          <span className="toast-icon">✓</span>
          <span className="toast-text">已复制到剪贴板</span>
        </div>
      )}
      {settingsOpen && (
        <>
          <div className="modal-overlay" onClick={() => setSettingsOpen(false)} />
          <dialog className="settings-modal" open>
            <div className="modal-header">
              <h2>⚙️ AI 接口设置</h2>
              <button
                type="button"
                className="modal-close"
                onClick={() => setSettingsOpen(false)}
              >
                ✕
              </button>
            </div>
            <p className="hint">
              API Key 仅保存在当前浏览器 LocalStorage 中。若使用公共环境，请谨慎输入密钥。
            </p>
            {sendError && sendError.includes('API Key') && (
              <div className="modal-error-banner">
                <span className="error-icon">⚠️</span>
                <span>{sendError}</span>
              </div>
            )}
            <form className="settings-form" onSubmit={event => event.preventDefault()}>
              <label className="system-prompt-label">
                系统提示词
                <textarea
                  className="system-prompt-input"
                  value={settings.systemPrompt}
                  onChange={event =>
                    setSettings(prev => ({ ...prev, systemPrompt: event.target.value }))
                  }
                  placeholder="输入系统提示词，定义 AI 的角色和行为..."
                  rows={4}
                />
              </label>
              
              <div className="settings-grid">
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
              </div>
            </form>
            <p className="modal-footer">
              © Nolimit35-不限进步 · 构建 SHA：{' '}
              {isDevBuild ? (
                <span className="build-sha">{buildShaDisplay}</span>
              ) : (
                <a
                  className="build-sha"
                  href={buildShaUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  title={`查看提交 ${buildSha}`}
                >
                  {buildShaDisplay}
                </a>
              )}
            </p>
          </dialog>
        </>
      )}

      {payloadError && payloadError !== 'missing-payload' && (
        <div className="error-banner">{payloadError}</div>
      )}

      <main className="content">
        {payloadError === 'missing-payload' ? (
          <section className="card welcome-card">
            <h2>欢迎使用异常 AI 分析工作台</h2>
            <p className="welcome-text">
              通常情况下，您会通过异常通知中的链接直接访问带有异常数据的页面。
              如果您想先体验功能，可以点击下方按钮加载示例数据。
            </p>
            <button className="demo-button" onClick={loadDemoPayload}>
              <span className="demo-icon">🚀</span>
              <span>体验示例</span>
            </button>
            <p className="welcome-subtitle">
              基于异常上下文快速梳理问题并联动对话式分析。
            </p>
          </section>
        ) : payload ? (
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
                  🔗 查看链路
                </a>
              )}
            </header>

            <div className="card-grid">
              <InfoRow label="🐛 异常类型" value={payload.exceptionType} onCopySuccess={handleCopySuccess} />
              <InfoRow label="🔍 Trace ID" value={payload.traceId} onCopySuccess={handleCopySuccess} />
              <InfoRow label="📍 异常位置" value={payload.location} onCopySuccess={handleCopySuccess} />
              <InfoRow label="💬 异常描述" value={payload.exceptionMessage} onCopySuccess={handleCopySuccess} />
            </div>

            {payload.author && (
              <section className="sub-card">
                <h3>👤 代码提交者</h3>
                <div className="card-grid">
                  <InfoRow label="👨‍💻 姓名" value={payload.author.name} onCopySuccess={handleCopySuccess} />
                  <InfoRow label="📧 邮箱" value={payload.author.email} onCopySuccess={handleCopySuccess} />
                  <InfoRow label="⏰ 最后提交时间" value={formatDate(payload.author.lastCommitTime)} onCopySuccess={handleCopySuccess} />
                  <InfoRow label="📁 文件位置" value={formatFileLocation(payload.author)} onCopySuccess={handleCopySuccess} />
                  <InfoRow label="💡 提交信息" value={payload.author.commitMessage} onCopySuccess={handleCopySuccess} />
                </div>
              </section>
            )}

            {payload.codeContext && (
              <section className="sub-card">
                <div className="editable-header">
                  <h3>📝 代码上下文</h3>
                  <div className="edit-actions">
                    {editingCodeContext ? (
                      <>
                        <button
                          type="button"
                          className="edit-button save"
                          onClick={handleSaveCodeContext}
                        >
                          ✅ 保存
                        </button>
                        <button
                          type="button"
                          className="edit-button cancel"
                          onClick={handleCancelCodeContext}
                        >
                          ❌ 取消
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="edit-button"
                        onClick={handleEditCodeContext}
                      >
                        ✏️ 编辑
                      </button>
                    )}
                  </div>
                </div>
                {editingCodeContext ? (
                  <textarea
                    className="code-editor"
                    value={codeContextDraft}
                    onChange={e => setCodeContextDraft(e.target.value)}
                  />
                ) : (
                  <pre className="code-block">
                    <code>{payload.codeContext}</code>
                  </pre>
                )}
              </section>
            )}

            {payload.stacktrace && (
              <section className="sub-card">
                <div className="editable-header">
                  <h3>📚 堆栈信息</h3>
                  <div className="edit-actions">
                    {editingStacktrace ? (
                      <>
                        <button
                          type="button"
                          className="edit-button save"
                          onClick={handleSaveStacktrace}
                        >
                          ✅ 保存
                        </button>
                        <button
                          type="button"
                          className="edit-button cancel"
                          onClick={handleCancelStacktrace}
                        >
                          ❌ 取消
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="edit-button"
                        onClick={handleEditStacktrace}
                      >
                        ✏️ 编辑
                      </button>
                    )}
                  </div>
                </div>
                {editingStacktrace ? (
                  <textarea
                    className="code-editor"
                    value={stacktraceDraft}
                    onChange={e => setStacktraceDraft(e.target.value)}
                  />
                ) : (
                  <pre className="code-block">
                    <code>{payload.stacktrace}</code>
                  </pre>
                )}
              </section>
            )}

            <section className="sub-card">
              <div className="editable-header">
                <h3>📌 其他补充</h3>
                <div className="edit-actions">
                  {editingAdditionalInfo ? (
                    <>
                      <button
                        type="button"
                        className="edit-button save"
                        onClick={handleSaveAdditionalInfo}
                      >
                        ✅ 保存
                      </button>
                      <button
                        type="button"
                        className="edit-button cancel"
                        onClick={handleCancelAdditionalInfo}
                      >
                        ❌ 取消
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="edit-button"
                      onClick={handleEditAdditionalInfo}
                    >
                      {payload?.additionalInfo ? '✏️ 编辑' : '➕ 添加'}
                    </button>
                  )}
                </div>
              </div>
              {editingAdditionalInfo ? (
                <textarea
                  className="code-editor"
                  value={additionalInfoDraft}
                  onChange={e => setAdditionalInfoDraft(e.target.value)}
                  placeholder="在此添加其他补充信息，例如：&#10;- pom.xml 依赖配置&#10;- application.yml 配置&#10;- 环境变量&#10;- 相关日志&#10;- 其他上下文信息"
                />
              ) : payload?.additionalInfo ? (
                <pre className="code-block">
                  <code>{payload.additionalInfo}</code>
                </pre>
              ) : (
                <p className="empty-hint">
                  点击&nbsp;&quot;添加&quot;&nbsp;按钮补充其他信息（如 pom.xml 依赖、配置文件等），帮助 AI 更准确地分析问题。
                </p>
              )}
            </section>
          </section>
        ) : null}

        <section className="card chat-panel">
          <header className="card-header">
            <div>
              <h2>💬 对话分析</h2>
              <p className="hint">
                根据异常上下文向 AI 提问，获取进一步的定位与修复建议。
              </p>
            </div>
            <div className="header-actions">
              <a
                className="github-link"
                href="https://github.com/GuangYiDing/exception-notify"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="查看 GitHub 仓库"
                title="GitHub"
              >
                <svg viewBox="0 0 24 24" role="img" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M12 0a12 12 0 0 0-3.79 23.4c.6.11.82-.26.82-.58v-2c-3.34.73-4.04-1.61-4.04-1.61a3.18 3.18 0 0 0-1.34-1.75c-1.1-.76.08-.75.08-.75a2.5 2.5 0 0 1 1.84 1.24 2.54 2.54 0 0 0 3.46 1 2.52 2.52 0 0 1 .76-1.6c-2.67-.3-5.47-1.34-5.47-5.95a4.67 4.67 0 0 1 1.24-3.24 4.3 4.3 0 0 1 .12-3.2s1-.32 3.28 1.24a11.29 11.29 0 0 1 6 0c2.28-1.56 3.27-1.24 3.27-1.24a4.3 4.3 0 0 1 .12 3.2 4.67 4.67 0 0 1 1.24 3.24c0 4.62-2.81 5.64-5.49 5.94a2.83 2.83 0 0 1 .81 2.2v3.27c0 .32.22.7.82.58A12 12 0 0 0 12 0Z"
                  />
                </svg>
                </a>
                <button
                className={`mcp-button ${mcpPanelOpen ? 'active' : ''}`}
                  onClick={() => setMcpPanelOpen(v => !v)}
                title={mcpPanelOpen ? '关闭 MCP 工具' : '打开 MCP 工具'}
              >
                🔧 {mcpPanelOpen ? '关闭 MCP' : 'MCP 工具'}
              </button>
              <button className="settings-button" onClick={() => setSettingsOpen(v => !v)}>
                {settingsOpen ? '❌ 关闭设置' : '⚙️ 打开设置'}
              </button>
            </div>
          </header>
          <div className="chat-window" ref={chatWindowRef}>
            {messages.map((message, index) => {
              const collapsible = isCollapsibleMessage(message);
              const collapsed =
                collapsedMessages[index] !== undefined
                  ? collapsedMessages[index]
                  : (message.role === 'system' ? true : collapsible);
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
                    <div className="message-actions">
                      <button
                        type="button"
                        className="copy-button"
                        onClick={() => copyToClipboard(message.content, index)}
                        title="复制内容"
                      >
                        {copiedIndex === index ? '✅ 已复制' : '📋 复制'}
                      </button>
                      {message.role === 'assistant' && !isSending && (
                        <button
                          type="button"
                          className="regenerate-button"
                          onClick={() => handleRegenerate(index)}
                          title="重新生成回答"
                        >
                          🔄 重新生成
                        </button>
                      )}
                      {collapsible && (
                        <button
                          type="button"
                          className="collapse-button"
                          onClick={() => toggleCollapsed(index, collapsed)}
                        >
                          {collapsed ? '📂 展开' : '📁 收起'}
                        </button>
                      )}
                    </div>
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
                  <span className="role-label">🤖 AI</span>
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
            <div className="input-wrapper">
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
              <button type="submit" disabled={isSending} className="send-button">
                {isSending ? '🔄 发送中...' : '🚀 发送'}
              </button>
            </div>
          </form>
          {sendError && <div className="error-banner">{sendError}</div>}
          </section>

      {mcpPanelOpen && (
        <>
          <div className="modal-overlay" onClick={() => setMcpPanelOpen(false)} />
          <dialog className="settings-modal mcp-modal" open>
            <div className="modal-header">
              <h2>🔧 MCP 工具</h2>
              <button
                type="button"
                className="modal-close"
                onClick={() => setMcpPanelOpen(false)}
              >
                ✕
              </button>
            </div>
            <p className="hint">
              使用外部工具扩展 AI 分析能力。连接到 MCP 服务器以获取可用工具。
              <span className="hint-compatibility">
                仅支持 Streamable HTTP 传输协议
                <a
                  href="https://modelcontextprotocol.io/specification/2025-03-26/basic/transports"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="help-link"
                  title="查看 MCP 传输协议规范"
                >
                  ⚠️
                </a>
              </span>
            </p>
            {mcpError && (
              <div className="modal-error-banner">
                <span className="error-icon">⚠️</span>
                <span>{mcpError}</span>
              </div>
            )}

            {/* Server Management Section */}
            <div className="server-management">
              <div className="section-header">
                <h3>MCP 服务器</h3>
                <button
                  type="button"
                  className="add-server-button"
                  onClick={() => setShowAddServer(true)}
                >
                  ➕ 添加服务器
                </button>
              </div>

              {mcpSettings.servers.length === 0 ? (
                <div className="empty-servers">
                  <p>
                    尚未配置任何 MCP 服务器。点击&nbsp;&quot;添加服务器&quot;&nbsp;开始配置。
                  </p>
                </div>
              ) : (
                <div className="servers-list">
                  {mcpSettings.servers.map((server) => (
                    <div key={server.id} className={`server-item ${server.isActive ? 'active' : ''}`}>
                      <div className="server-info">
                        <div className="server-header">
                          <span className="server-name">{server.name}</span>
                          <div className="server-actions">
                            <button
                              type="button"
                              className={`activate-button ${server.isActive ? 'active' : ''}`}
                              onClick={() => activateServer(server.id)}
                              title={server.isActive ? '当前激活' : '激活此服务器'}
                            >
                              {server.isActive ? (
                                serverConnectionStatus[server.id] === 'connected' ? '✅ 已连接' :
                                serverConnectionStatus[server.id] === 'loading' ? '🔄 连接中' :
                                '✅ 已激活'
                              ) : '🔘 激活'}
                            </button>
                            <button
                              type="button"
                              className="edit-button"
                              onClick={() => startEditServer(server)}
                              title="编辑服务器"
                            >
                              ✏️
                            </button>
                            {mcpSettings.servers.length > 1 && (
                              <button
                                type="button"
                                className="delete-button"
                                onClick={() => deleteServer(server.id)}
                                title="删除服务器"
                              >
                                🗑️
                              </button>
                            )}
                          </div>
                        </div>
                        <div className="server-details">
                          <span className="server-url">{server.baseUrl}</span>
                          <span className="server-transport">传输方式：{server.transportType}</span>
                        </div>
                      </div>

                      {editingServer === server.id && (
                        <div className="edit-server-form">
                          <div className="form-grid">
                            <label>
                              服务器名称
                              <input
                                type="text"
                                value={serverForm.name}
                                onChange={(e) => setServerForm(prev => ({ ...prev, name: e.target.value }))}
                                placeholder="输入服务器名称"
                              />
                            </label>
                            <label>
                              Base URL
                              <input
                                type="text"
                                value={serverForm.baseUrl}
                                onChange={(e) => setServerForm(prev => ({ ...prev, baseUrl: e.target.value }))}
                                placeholder="https://your-mcp-server.com"
                              />
                            </label>
                            <label>
                              传输方式
                              <select
                                value={serverForm.transportType}
                                onChange={(e) =>
                                  setServerForm(prev => ({
                                    ...prev,
                                    transportType: (e.target.value as McpTransportType)
                                  }))
                                }
                              >
                                <option value="streamable-http">streamable-http</option>
                                <option value="sse">sse</option>
                              </select>
                            </label>
                            <label className="full-width">
                              请求头（JSON 格式）
                              <textarea
                                value={serverForm.headersText}
                                onChange={(e) => setServerForm(prev => ({ ...prev, headersText: e.target.value }))}
                                placeholder='{"Authorization": "Bearer token"}'
                                rows={2}
                              />
                            </label>
                          </div>
                          <div className="form-actions">
                            <button
                              type="button"
                              className="save-button"
                              onClick={() => updateServer(server.id)}
                            >
                              💾 保存
                            </button>
                            <button
                              type="button"
                              className="cancel-button"
                              onClick={cancelEdit}
                            >
                              ❌ 取消
                            </button>
                            <button
                              type="button"
                              className="delete-button-form"
                              onClick={() => {
                                setDeleteConfirm({
                                  show: true,
                                  serverId: server.id,
                                  serverName: server.name
                                });
                              }}
                              title="删除此服务器"
                            >
                              🗑️ 删除
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {showAddServer && (
                <div className="add-server-form">
                  <h4>添加新服务器</h4>
                  <div className="form-grid">
                    <label>
                      服务器名称
                      <input
                        type="text"
                        value={serverForm.name}
                        onChange={(e) => setServerForm(prev => ({ ...prev, name: e.target.value }))}
                        placeholder="输入服务器名称"
                      />
                    </label>
                    <label>
                      Base URL
                      <input
                        type="text"
                        value={serverForm.baseUrl}
                        onChange={(e) => setServerForm(prev => ({ ...prev, baseUrl: e.target.value }))}
                        placeholder="https://your-mcp-server.com"
                      />
                    </label>
                    <label>
                      传输方式
                      <select
                        value={serverForm.transportType}
                        onChange={(e) =>
                          setServerForm(prev => ({
                            ...prev,
                            transportType: (e.target.value as McpTransportType)
                          }))
                        }
                      >
                        <option value="streamable-http">streamable-http</option>
                        <option value="sse">sse</option>
                      </select>
                    </label>
                    <label className="full-width">
                      请求头（JSON 格式）
                      <textarea
                        value={serverForm.headersText}
                        onChange={(e) => setServerForm(prev => ({ ...prev, headersText: e.target.value }))}
                        placeholder='{"Authorization": "Bearer token"}'
                        rows={2}
                      />
                    </label>
                  </div>
                  <div className="form-actions">
                    <button
                      type="button"
                      className="save-button"
                      onClick={addServer}
                      disabled={!serverForm.name.trim() || !serverForm.baseUrl.trim()}
                    >
                      ➕ 添加
                    </button>
                    <button
                      type="button"
                      className="cancel-button"
                      onClick={cancelEdit}
                    >
                      ❌ 取消
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="mcp-content">
              {mcpServerInfo && (
                <div className="server-info">
                  <div className="server-info-header">
                    <h3>服务器信息</h3>
                    <button
                      type="button"
                      className="clear-server-button"
                      onClick={() => {
                        setMcpServerInfo(null);
                        setMcpTools([]);
                        setMcpSelectedTool('');
                        setMcpResult(null);
                        setMcpError(null);
                      }}
                      title="清除当前服务器信息"
                    >
                      🗑️ 清除
                    </button>
                  </div>
                  <div className="info-grid">
                    <InfoRow label="名称" value={mcpServerInfo.name} onCopySuccess={handleCopySuccess} />
                    <InfoRow label="版本" value={mcpServerInfo.version} onCopySuccess={handleCopySuccess} />
                    <InfoRow label="描述" value={mcpServerInfo.description} onCopySuccess={handleCopySuccess} />
                  </div>
                </div>
              )}

              <div className="tools-section">
                <div className="tools-header">
                  <h3>可用工具</h3>
                  <button
                    type="button"
                    className="refresh-button"
                    onClick={loadMcpTools}
                    disabled={mcpLoading}
                  >
                    {mcpLoading ? '🔄 加载中...' : '🔄 刷新'}
                  </button>
                </div>

                {mcpLoading && (
                  <div className="loading-indicator">
                    <span>正在连接 MCP 服务器...</span>
                  </div>
                )}

                {!mcpLoading && mcpTools.length === 0 && !mcpError && (
                  <div className="empty-state">
                    <p>未找到可用工具。请检查 MCP 服务器配置。</p>
                  </div>
                )}

                <div className="tools-list">
                  {mcpTools.map((tool) => (
                    <div key={tool.name} className="tool-item">
                      <label className="tool-label">
                        <input
                          type="radio"
                          name="mcp-tool"
                          value={tool.name}
                          checked={mcpSelectedTool === tool.name}
                          onChange={(e) => {
                            setMcpSelectedTool(e.target.value);
                            setMcpResult(null);
                            setMcpError(null);
                          }}
                        />
                        <div className="tool-info">
                          <span className="tool-title">{tool.title}</span>
                          <span className="tool-description">{tool.description}</span>
                        </div>
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              {mcpSelectedTool && (
                <div className="execution-section">
                  <h3>执行工具</h3>
                  <div className="execution-form">
                    <label>
                      参数（JSON 格式）
                      <textarea
                        className="arguments-input"
                        value={mcpArgumentsText}
                        onChange={(e) => setMcpArgumentsText(e.target.value)}
                        placeholder="{}"
                        rows={3}
                      />
                    </label>
                    {mcpArgumentError && (
                      <div className="modal-error-banner">
                        <span className="error-icon">⚠️</span>
                        <span>{mcpArgumentError}</span>
                      </div>
                    )}
                    <button
                      type="button"
                      className="execute-button"
                      onClick={executeMcpTool}
                      disabled={mcpExecuting}
                    >
                      {mcpExecuting ? '🔄 执行中...' : '🚀 执行工具'}
                    </button>
                  </div>
                </div>
              )}

              {mcpResult && (
                <div className="result-section">
                  <h3>执行结果</h3>
                  <div className="result-content">
                    <div className="result-meta">
                      <span className="result-tool">{mcpResult.toolTitle}</span>
                      <span className="result-time">{formatDate(mcpResult.executedAt)}</span>
                      {mcpResult.server && (
                        <span className="result-server">服务器: {mcpResult.server.name || '未知'}</span>
                      )}
                    </div>
                    {mcpResult.isError && (
                      <div className="error-indicator">
                        ⚠️ 执行出现错误
                      </div>
                    )}
                    {mcpResult.plainText && (
                      <pre className="result-text">
                        <code>{mcpResult.plainText}</code>
                      </pre>
                    )}
                    {mcpResult.structuredContent != null && (
                      <pre className="result-json">
                        <code>{JSON.stringify(mcpResult.structuredContent, null, 2)}</code>
                      </pre>
                    )}
                  </div>
                </div>
              )}
            </div>
          </dialog>

          {/* Custom Delete Confirmation Dialog */}
          {deleteConfirm.show && (
            <>
              <div className="modal-overlay" />
              <dialog className="confirm-dialog" open>
                <div className="confirm-content">
                  <div className="confirm-header">
                    <h3>⚠️ 确认删除</h3>
                  </div>
                  <div className="confirm-body">
                    <p>
                      确定要删除服务器{' '}
                      <strong>&quot;{deleteConfirm.serverName}&quot;</strong> 吗？
                    </p>
                    <p className="confirm-warning">此操作不可恢复，请谨慎操作。</p>
                  </div>
                  <div className="confirm-actions">
                    <button
                      type="button"
                      className="confirm-cancel"
                      onClick={() => setDeleteConfirm({ show: false, serverId: '', serverName: '' })}
                    >
                      取消
                    </button>
                    <button
                      type="button"
                      className="confirm-delete"
                      onClick={() => {
                        deleteServer(deleteConfirm.serverId);
                        setDeleteConfirm({ show: false, serverId: '', serverName: '' });
                        cancelEdit();
                      }}
                    >
                      🗑️ 确认删除
                    </button>
                  </div>
                </div>
              </dialog>
            </>
          )}
        </>
      )}
      </main>
    </div>
  );
}

type InfoRowProps = {
  label: string;
  value?: string | number | null;
  onCopySuccess?: () => void;
};

function InfoRow({ label, value, onCopySuccess }: InfoRowProps) {
  if (value === undefined || value === null || value === '') {
    return null;
  }

  const text = String(value);

  const handleDoubleClick = async () => {
    try {
      await navigator.clipboard.writeText(text);
      onCopySuccess?.();
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <div className="info-row">
      <span className="info-label">{label}</span>
      <span
        className="info-value"
        title={`${text}\n\n💡 双击复制`}
        onDoubleClick={handleDoubleClick}
      >
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
  if (payload.additionalInfo) {
    lines.push('\n[其他补充]', payload.additionalInfo);
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
      return '🤖 AI';
    case 'user':
      return '👤 你';
    case 'system':
      return '⚙️ 系统';
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
