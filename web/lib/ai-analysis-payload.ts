export interface AiAnalysisPayload {
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
