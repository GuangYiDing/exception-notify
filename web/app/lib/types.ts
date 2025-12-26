import type { AiAnalysisPayload } from '@/lib/ai-analysis-payload';

export type ChatMessage = {
  role: 'system' | 'user' | 'assistant';
  content: string;
  reasoning?: string;
};

export type ClientSettings = {
  endpoint: string;
  apiKey: string;
  model: string;
  temperature: number;
  systemPrompt: string;
};

export { type AiAnalysisPayload };
