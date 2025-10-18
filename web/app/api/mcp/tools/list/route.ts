import { NextRequest, NextResponse } from 'next/server';

import { resolveMcpConnectionOptions, withMcpClient } from '@/lib/mcp-client';

type ListToolsRequest = {
  baseUrl?: string;
  headers?: Record<string, string | undefined>;
};

export async function POST(request: NextRequest) {
  let body: ListToolsRequest;
  try {
    body = (await request.json()) as ListToolsRequest;
  } catch {
    return jsonResponse({ code: 1, message: 'Invalid JSON payload' }, 400);
  }

  let connection;
  try {
    connection = resolveMcpConnectionOptions(body);
  } catch (error) {
    const message = error instanceof Error ? error.message : 'baseUrl is required';
    return jsonResponse({ code: 1, message }, 400);
  }

  try {
    const result = await withMcpClient(
      connection,
      async client => {
        const [toolsResult, serverInfo] = await Promise.all([
          client.listTools(),
          Promise.resolve(client.getServerVersion())
        ]);
        return {
          tools: toolsResult.tools.map(tool => ({
            name: tool.name,
            title: tool.title ?? tool.name,
            description: tool.description ?? '',
            inputSchema: tool.inputSchema,
            outputSchema: tool.outputSchema
          })),
          server: serverInfo
        };
      }
    );

    return jsonResponse({ code: 0, data: result });
  } catch (error) {
    return jsonResponse(
      {
        code: 1,
        message: error instanceof Error ? error.message : 'Failed to list MCP tools'
      },
      500
    );
  }
}

function jsonResponse(body: unknown, status: number = 200) {
  return NextResponse.json(body, {
    status,
    headers: {
      'cache-control': 'no-store, max-age=0'
    }
  });
}
