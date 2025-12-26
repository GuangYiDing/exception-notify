'use client';

import { FormEvent, useEffect, useRef, useState } from 'react';

import { CopyToast, WelcomeCard, SettingsModal, ChatPanel, ExceptionCard } from './components';
import { SETTINGS_KEY, defaultSettings } from './lib/constants';
import { buildSummaryPrompt } from './lib/utils';
import type { AiAnalysisPayload } from '@/lib/ai-analysis-payload';
import type { ChatMessage, ClientSettings } from './lib/types';

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

        let serialized: string | undefined;
        // Check demo KV first
        const { DEMO_KV } = await import('./lib/constants');
        serialized = DEMO_KV[encoded];
        if (!serialized) {
          const response = await fetch(`/api/decompress?payload=${encodeURIComponent(encoded)}`);
          if (!response.ok) {
            throw new Error(`Failed to fetch payload: ${response.status}`);
          }
          const body: { code?: number; data?: string; message?: string } = await response.json();
          if (body.code !== 0 || typeof body.data !== 'string') {
            throw new Error(body.message || 'Invalid payload response');
          }
          serialized = body.data;
        }

        if (!serialized || serialized.length === 0) {
          throw new Error('Empty payload content');
        }

        const parsed: AiAnalysisPayload = JSON.parse(serialized);
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

  const handleCopySuccess = () => {
    setShowCopyToast(true);
    setTimeout(() => setShowCopyToast(false), 2000);
  };

  const formatApiError = (status: number, statusText: string, body: string): string => {
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
  };

  return (
    <div className="app-container">
      {showCopyToast && <CopyToast />}

      <SettingsModal
        settings={settings}
        setSettings={setSettings}
        settingsOpen={settingsOpen}
        setSettingsOpen={setSettingsOpen}
        sendError={sendError}
      />

      {payloadError && payloadError !== 'missing-payload' && (
        <div className="error-banner">{payloadError}</div>
      )}

      <main className="content">
        {payloadError === 'missing-payload' ? (
          <WelcomeCard />
        ) : payload ? (
          <ExceptionCard payload={payload} onCopySuccess={handleCopySuccess} />
        ) : null}

        <ChatPanel
          messages={messages}
          input={input}
          setInput={setInput}
          isSending={isSending}
          sendError={sendError}
          chatWindowRef={chatWindowRef}
          copiedIndex={copiedIndex}
          collapsedMessages={collapsedMessages}
          reasoningCollapsed={reasoningCollapsed}
          streamingContent={streamingContent}
          streamingReasoning={streamingReasoning}
          streamingReasoningCollapsed={streamingReasoningCollapsed}
          settingsOpen={settingsOpen}
          onSubmit={handleSubmit}
          onCopy={copyToClipboard}
          onRegenerate={handleRegenerate}
          onToggleCollapse={toggleCollapsed}
          onToggleReasoningCollapse={(i: number, c: boolean) => setReasoningCollapsed(prev => ({ ...prev, [i]: !c }))}
          onToggleSettings={() => setSettingsOpen(v => !v)}
          onToggleStreamingReasoningCollapse={() => setStreamingReasoningCollapsed(v => !v)}
        />
      </main>
    </div>
  );
}
