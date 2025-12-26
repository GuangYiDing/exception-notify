'use client';

import { FormEvent, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css';

import { ChatMessageItem } from './ChatMessage';
import type { ChatMessage, ClientSettings } from '../lib/types';
import { repositoryUrl } from '../lib/constants';

type ChatPanelProps = {
  messages: ChatMessage[];
  input: string;
  setInput: (input: string) => void;
  isSending: boolean;
  sendError: string | null;
  chatWindowRef: React.RefObject<HTMLDivElement>;
  copiedIndex: number | null;
  collapsedMessages: Record<number, boolean>;
  reasoningCollapsed: Record<number, boolean>;
  streamingContent: string;
  streamingReasoning: string;
  streamingReasoningCollapsed: boolean;
  settingsOpen: boolean;
  onSubmit: (event: FormEvent) => void;
  onCopy: (content: string, index: number) => void;
  onRegenerate: (index: number) => void;
  onToggleCollapse: (index: number, current: boolean) => void;
  onToggleReasoningCollapse: (index: number, current: boolean) => void;
  onToggleSettings: () => void;
  onToggleStreamingReasoningCollapse: () => void;
};

export function ChatPanel({
  messages,
  input,
  setInput,
  isSending,
  sendError,
  chatWindowRef,
  copiedIndex,
  collapsedMessages,
  reasoningCollapsed,
  streamingContent,
  streamingReasoning,
  streamingReasoningCollapsed,
  settingsOpen,
  onSubmit,
  onCopy,
  onRegenerate,
  onToggleCollapse,
  onToggleReasoningCollapse,
  onToggleSettings,
  onToggleStreamingReasoningCollapse
}: ChatPanelProps) {
  return (
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
            href={repositoryUrl}
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
          <button className="settings-button" onClick={onToggleSettings}>
            {settingsOpen ? '❌ 关闭设置' : '⚙️ 打开设置'}
          </button>
        </div>
      </header>
      <div className="chat-window" ref={chatWindowRef}>
        {messages.map((message, index) => (
          <ChatMessageItem
            key={index}
            message={message}
            index={index}
            copiedIndex={copiedIndex}
            collapsedMessages={collapsedMessages}
            reasoningCollapsed={reasoningCollapsed}
            isSending={isSending}
            onCopy={onCopy}
            onRegenerate={onRegenerate}
            onToggleCollapse={onToggleCollapse}
            onToggleReasoningCollapse={onToggleReasoningCollapse}
          />
        ))}
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
                  onClick={onToggleStreamingReasoningCollapse}
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
      <form className="chat-form" onSubmit={onSubmit}>
        <div className="input-wrapper">
          <textarea
            placeholder="描述你想了解的问题，按 Ctrl+Enter 发送"
            value={input}
            onChange={event => setInput(event.target.value)}
            onKeyDown={event => {
              if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
                event.preventDefault();
                onSubmit(event);
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
  );
}
