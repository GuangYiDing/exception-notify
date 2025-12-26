'use client';

import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css';

import type { ChatMessage } from '../lib/types';
import { isCollapsibleMessage, buildPreview, roleLabel } from '../lib/utils';

type ChatMessageProps = {
  message: ChatMessage;
  index: number;
  copiedIndex: number | null;
  collapsedMessages: Record<number, boolean>;
  reasoningCollapsed: Record<number, boolean>;
  isSending: boolean;
  onCopy: (content: string, index: number) => void;
  onRegenerate: (index: number) => void;
  onToggleCollapse: (index: number, current: boolean) => void;
  onToggleReasoningCollapse: (index: number, current: boolean) => void;
};

export function ChatMessageItem({
  message,
  index,
  copiedIndex,
  collapsedMessages,
  reasoningCollapsed,
  isSending,
  onCopy,
  onRegenerate,
  onToggleCollapse,
  onToggleReasoningCollapse
}: ChatMessageProps) {
  const collapsible = isCollapsibleMessage(message);
  const collapsed =
    collapsedMessages[index] !== undefined
      ? collapsedMessages[index]
      : (message.role === 'system' ? true : collapsible);
  const preview = collapsible ? buildPreview(message.content) : null;

  return (
    <article
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
            onClick={() => onCopy(message.content, index)}
            title="复制内容"
          >
            {copiedIndex === index ? '✅ 已复制' : '📋 复制'}
          </button>
          {message.role === 'assistant' && !isSending && (
            <button
              type="button"
              className="regenerate-button"
              onClick={() => onRegenerate(index)}
              title="重新生成回答"
            >
              🔄 重新生成
            </button>
          )}
          {collapsible && (
            <button
              type="button"
              className="collapse-button"
              onClick={() => onToggleCollapse(index, collapsed)}
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
            onClick={() => onToggleReasoningCollapse(index, reasoningCollapsed[index] ?? false)}
          >
            <span className="reasoning-icon">🧠</span>
            <span>思考过程</span>
            <span className="toggle-arrow">
              {reasoningCollapsed[index] ? '▼' : '▲'}
            </span>
          </button>
          {!(reasoningCollapsed[index] ?? false) && (
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
          message.content.split('\n').map((line: string, lineIndex: number) => (
            <p key={lineIndex}>{line}</p>
          ))
        )}
      </div>
    </article>
  );
}
