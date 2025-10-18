import { NextRequest, NextResponse } from 'next/server';

import { resolveMcpConnectionOptions, withMcpClient } from '@/lib/mcp-client';

type McpServerRequest = {
  baseUrl?: string;
  headers?: Record<string, string | undefined>;
  name?: string;
};

type ListToolsRequest = McpServerRequest & {
  servers?: McpServerRequest[];
};

type McpToolSummary = {
  name: string;
  title: string;
  description: string;
  inputSchema?: unknown;
  outputSchema?: unknown;
};

type McpServerResult = {
  name?: string;
  info?: unknown;
  tools: McpToolSummary[];
  error?: string;
};

const normalizeServerRequests = (body: ListToolsRequest): McpServerRequest[] => {
  if (Array.isArray(body.servers) && body.servers.length > 0) {
    return body.servers.filter(server => server && typeof server === 'object');
  }

  return [
    {
      baseUrl: body.baseUrl,
      headers: body.headers,
      name: body.name
    }
  ];
};

export async function POST(request: NextRequest) {
  let body: ListToolsRequest;
  try {
    body = (await request.json()) as ListToolsRequest;
  } catch {
    return jsonResponse({ code: 1, message: 'Invalid JSON payload' }, 400);
  }

  const serverRequests = normalizeServerRequests(body);
  if (serverRequests.length === 0) {
    return jsonResponse({ code: 1, message: 'No MCP server provided' }, 400);
  }

  const results = await Promise.all(
    serverRequests.map(async serverRequest => {
      const trimmedName =
        typeof serverRequest.name === 'string' && serverRequest.name.trim().length > 0
          ? serverRequest.name.trim()
          : undefined;

      try {
        const connection = resolveMcpConnectionOptions({
          baseUrl: serverRequest.baseUrl,
          headers: serverRequest.headers
        });

        const serverResult = await withMcpClient(connection, async client => {
          const [toolsResult, serverInfo] = await Promise.all([
            client.listTools(),
            Promise.resolve(client.getServerVersion())
          ]);
          const resolvedName =
            trimmedName ||
            (serverInfo &&
              typeof serverInfo === 'object' &&
              'name' in serverInfo &&
              typeof (serverInfo as Record<string, unknown>).name === 'string'
                ? ((serverInfo as Record<string, unknown>).name as string)
                : undefined);

          return {
            name: resolvedName,
            info: serverInfo,
            tools: toolsResult.tools.map(tool => ({
              name: tool.name,
              title: tool.title ?? tool.name,
              description: tool.description ?? '',
              inputSchema: tool.inputSchema,
              outputSchema: tool.outputSchema
            }))
          } satisfies McpServerResult;
        });

        return serverResult;
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Failed to list MCP tools';
        return {
          name: trimmedName,
          info: undefined,
          tools: [],
          error: message
        } as McpServerResult;
      }
    })
  );

  if (results.every(server => server.error)) {
    const firstError = results[0]?.error ?? 'Failed to list MCP tools';
    return jsonResponse(
      {
        code: 1,
        message: firstError,
        data: { servers: results }
      },
      500
    );
  }

  return jsonResponse({ code: 0, data: { servers: results } });
}

function jsonResponse(body: unknown, status: number = 200) {
  return NextResponse.json(body, {
    status,
    headers: {
      'cache-control': 'no-store, max-age=0'
    }
  });
}
