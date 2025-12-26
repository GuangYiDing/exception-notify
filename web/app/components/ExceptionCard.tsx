'use client';

import { useState } from 'react';
import type { AiAnalysisPayload } from '@/lib/ai-analysis-payload';
import { InfoRow } from './InfoRow';
import { formatDate, formatFileLocation } from '../lib/utils';

type ExceptionCardProps = {
  payload: AiAnalysisPayload | null;
  onCopySuccess: () => void;
};

export function ExceptionCard({ payload, onCopySuccess }: ExceptionCardProps) {
  const [editingCodeContext, setEditingCodeContext] = useState(false);
  const [editingStacktrace, setEditingStacktrace] = useState(false);
  const [editingAdditionalInfo, setEditingAdditionalInfo] = useState(false);
  const [codeContextDraft, setCodeContextDraft] = useState('');
  const [stacktraceDraft, setStacktraceDraft] = useState('');
  const [additionalInfoDraft, setAdditionalInfoDraft] = useState('');

  if (!payload) return null;

  const handleEditCodeContext = () => {
    setCodeContextDraft(payload.codeContext || '');
    setEditingCodeContext(true);
  };

  const handleSaveCodeContext = () => {
    // In a real implementation, this would update the payload through a callback
    setEditingCodeContext(false);
  };

  const handleCancelCodeContext = () => {
    setEditingCodeContext(false);
    setCodeContextDraft('');
  };

  const handleEditStacktrace = () => {
    setStacktraceDraft(payload.stacktrace || '');
    setEditingStacktrace(true);
  };

  const handleSaveStacktrace = () => {
    // In a real implementation, this would update the payload through a callback
    setEditingStacktrace(false);
  };

  const handleCancelStacktrace = () => {
    setEditingStacktrace(false);
    setStacktraceDraft('');
  };

  const handleEditAdditionalInfo = () => {
    setAdditionalInfoDraft(payload.additionalInfo || '');
    setEditingAdditionalInfo(true);
  };

  const handleSaveAdditionalInfo = () => {
    // In a real implementation, this would update the payload through a callback
    setEditingAdditionalInfo(false);
  };

  const handleCancelAdditionalInfo = () => {
    setEditingAdditionalInfo(false);
    setAdditionalInfoDraft('');
  };

  const title = (() => {
    const parts = [
      payload.appName || '应用',
      payload.environment ? `环境 ${payload.environment}` : undefined
    ].filter(Boolean);
    return parts.join(' · ') || '异常详情';
  })();

  return (
    <section className="card">
      <header className="card-header">
        <div>
          <h2>{title}</h2>
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
        <InfoRow label="🐛 异常类型" value={payload.exceptionType} onCopySuccess={onCopySuccess} />
        <InfoRow label="🔍 Trace ID" value={payload.traceId} onCopySuccess={onCopySuccess} />
        <InfoRow label="📍 异常位置" value={payload.location} onCopySuccess={onCopySuccess} />
        <InfoRow label="💬 异常描述" value={payload.exceptionMessage} onCopySuccess={onCopySuccess} />
      </div>

      {payload.author && (
        <section className="sub-card">
          <h3>👤 代码提交者</h3>
          <div className="card-grid">
            <InfoRow label="👨‍💻 姓名" value={payload.author.name} onCopySuccess={onCopySuccess} />
            <InfoRow label="📧 邮箱" value={payload.author.email} onCopySuccess={onCopySuccess} />
            <InfoRow label="⏰ 最后提交时间" value={formatDate(payload.author.lastCommitTime)} onCopySuccess={onCopySuccess} />
            <InfoRow label="📁 文件位置" value={formatFileLocation(payload.author)} onCopySuccess={onCopySuccess} />
            <InfoRow label="💡 提交信息" value={payload.author.commitMessage} onCopySuccess={onCopySuccess} />
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
                {payload.additionalInfo ? '✏️ 编辑' : '➕ 添加'}
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
        ) : payload.additionalInfo ? (
          <pre className="code-block">
            <code>{payload.additionalInfo}</code>
          </pre>
        ) : (
          <p className="empty-hint">
            点击&quot;添加&quot;按钮补充其他信息（如 pom.xml 依赖、配置文件等），帮助 AI 更准确地分析问题。
          </p>
        )}
      </section>
    </section>
  );
}
