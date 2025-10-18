import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { SSEClientTransport } from '@modelcontextprotocol/sdk/client/sse.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';

const CLIENT_NAME = 'exception-notify-web';
const CLIENT_VERSION = (process.env.NEXT_PUBLIC_BUILD_SHA || 'dev').slice(0, 32);
const DEFAULT_BASE_URL =
  process.env.MCP_SERVER_URL ||
  process.env.MCP_BASE_URL ||
  process.env.MCP_ENDPOINT ||
  process.env.NEXT_PUBLIC_MCP_BASE_URL ||
  '';
const DEFAULT_HEADERS = sanitizeHeaders({
  Authorization:
    process.env.MCP_AUTHORIZATION ||
    (process.env.MCP_BEARER_TOKEN ? `Bearer ${process.env.MCP_BEARER_TOKEN}` : undefined)
});

export type McpConnectionOptions = {
  baseUrl: string;
  headers?: Record<string, string | undefined>;
};

export function resolveMcpConnectionOptions(
  input?: Partial<McpConnectionOptions>
): McpConnectionOptions {
  const baseUrl = input?.baseUrl || DEFAULT_BASE_URL;
  if (!baseUrl) {
    throw new Error('MCP baseUrl is required');
  }
  const headers = sanitizeHeaders({
    ...(DEFAULT_HEADERS || {}),
    ...(input?.headers || {})
  });
  return { baseUrl, headers };
}

export async function withMcpClient<T>(
  options: McpConnectionOptions,
  handler: (client: Client) => Promise<T>
): Promise<T> {
  if (!options.baseUrl) {
    throw new Error('MCP baseUrl is required');
  }

  const url = new URL(options.baseUrl);
  const headers = sanitizeHeaders(options.headers);
  const requestInit = headers ? { headers } : undefined;

  const connect = async () => {
    try {
      const client = createClient();
      const transport = new StreamableHTTPClientTransport(url, requestInit ? { requestInit } : undefined);
      await client.connect(transport);
      return { client, transport };
    } catch (error) {
      // Attempt SSE fallback for servers that still use the legacy transport
      const client = createClient();
      const transport = new SSEClientTransport(
        url,
        requestInit
          ? {
              requestInit
            }
          : undefined
      );
      await client.connect(transport);
      return { client, transport };
    }
  };

  const { client, transport } = await connect();

  try {
    return await handler(client);
  } finally {
    await client.close();
    if ('terminateSession' in transport && typeof transport.terminateSession === 'function') {
      try {
        await transport.terminateSession();
      } catch {
        // Ignore session termination failures
      }
    }
    await transport.close();
  }
}

function sanitizeHeaders(
  headers?: Record<string, string | undefined>
): Record<string, string> | undefined {
  if (!headers) {
    return undefined;
  }
  const entries = Object.entries(headers).filter(
    ([key, value]) => Boolean(key?.trim()) && typeof value === 'string' && value.trim().length > 0
  );
  if (entries.length === 0) {
    return undefined;
  }
  return Object.fromEntries(entries.map(([key, value]) => [key.trim(), value!.trim()]));
}

function createClient() {
  return new Client({
    name: CLIENT_NAME,
    version: CLIENT_VERSION
  });
}
