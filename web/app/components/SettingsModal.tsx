'use client';

import { buildShaDisplay, buildShaUrl, isDevBuild, repositoryUrl } from '../lib/constants';
import type { ClientSettings } from '../lib/types';

type SettingsModalProps = {
  settings: ClientSettings;
  setSettings: React.Dispatch<React.SetStateAction<ClientSettings>>;
  settingsOpen: boolean;
  setSettingsOpen: (open: boolean) => void;
  sendError: string | null;
};

export function SettingsModal({ settings, setSettings, settingsOpen, setSettingsOpen, sendError }: SettingsModalProps) {
  if (!settingsOpen) return null;

  return (
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
                setSettings((prev: ClientSettings) => ({ ...prev, systemPrompt: event.target.value }))
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
                  setSettings((prev: ClientSettings) => ({ ...prev, endpoint: event.target.value }))
                }
              />
            </label>
            <label>
              Model
              <input
                type="text"
                value={settings.model}
                onChange={event =>
                  setSettings((prev: ClientSettings) => ({ ...prev, model: event.target.value }))
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
                  setSettings((prev: ClientSettings) => ({
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
                  setSettings((prev: ClientSettings) => ({ ...prev, apiKey: event.target.value }))
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
              title={`查看提交 ${buildShaDisplay}`}
            >
              {buildShaDisplay}
            </a>
          )}
        </p>
      </dialog>
    </>
  );
}
