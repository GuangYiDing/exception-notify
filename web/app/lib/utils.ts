import type { AiAnalysisPayload, ChatMessage } from './types';

export function buildSummaryPrompt(payload: AiAnalysisPayload): string | null {
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

export function limitLines(text: string, maxLines: number): string {
  const lines = text.split('\n');
  if (lines.length <= maxLines) {
    return text;
  }
  return `${lines.slice(0, maxLines).join('\n')}\n...（后续合计 ${lines.length - maxLines} 行已省略）`;
}

export function formatDate(value?: string | null): string | undefined {
  if (!value) {
    return undefined;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

export function formatFileLocation(author?: AiAnalysisPayload['author']): string | undefined {
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

export function roleLabel(role: ChatMessage['role']): string {
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

export function formatApiError(status: number, statusText: string, body: string): string {
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

export function isCollapsibleMessage(message: ChatMessage): boolean {
  if (message.role === 'system') {
    return true;
  }
  if (message.role === 'user' && message.content.startsWith('[异常概览]')) {
    return true;
  }
  return false;
}

export function buildPreview(content: string): string {
  const lines = content.split('\n').map(line => line.trim()).filter(Boolean);
  if (lines.length <= 2) {
    return lines.join(' ');
  }
  return `${lines.slice(0, 2).join(' ')} …`;
}
