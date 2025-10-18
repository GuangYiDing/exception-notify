import { NextRequest, NextResponse } from 'next/server';

import { resolveMcpConnectionOptions, withMcpClient } from '@/lib/mcp-client';

type ExecuteToolRequest = {
  baseUrl?: string;
  headers?: Record<string, string | undefined>;
  name?: string;
  arguments?: unknown;
};

export async function POST(request: NextRequest) {
  let body: ExecuteToolRequest;
  try {
    body = (await request.json()) as ExecuteToolRequest;
  } catch {
    return jsonResponse({ code: 1, message: 'Invalid JSON payload' }, 400);
  }

  if (!body?.name) {
    return jsonResponse({ code: 1, message: 'name is required' }, 400);
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
        const callResult = await client.callTool({
          name: body.name!,
          arguments: body.arguments as Record<string, unknown> | undefined
        });

        const serverInfo = client.getServerVersion();
        return {
          content: callResult.content ?? [],
          plainText: stringifyContent(callResult.content),
          structuredContent: callResult.structuredContent ?? null,
          isError: Boolean((callResult as { isError?: boolean }).isError),
          server: serverInfo
        };
      }
    );

    return jsonResponse({ code: 0, data: result });
  } catch (error) {
    return jsonResponse(
      {
        code: 1,
        message: error instanceof Error ? error.message : 'Failed to execute MCP tool'
      },
      500
    );
  }
}

function stringifyContent(content: unknown): string | null {
  if (!Array.isArray(content)) {
    return null;
  }

  const parts: string[] = [];

  for (const item of content) {
    if (!item || typeof item !== 'object') {
      continue;
    }

    const record = item as Record<string, unknown>;
    const type = typeof record.type === 'string' ? record.type : undefined;

    if (type === 'text' && typeof record.text === 'string') {
      parts.push(record.text);
      continue;
    }

    if (type === 'resource' && record.resource && typeof record.resource === 'object') {
      const resource = record.resource as Record<string, unknown>;
      const uri = typeof resource.uri === 'string' ? resource.uri : '未知资源';
      parts.push(`资源引用：${uri}`);
      continue;
    }

    if (type === 'resource_reference' && typeof record.resourceUri === 'string') {
      parts.push(`资源引用：${record.resourceUri}`);
      continue;
    }

    if ((type === 'image' || type === 'audio') && typeof record.mimeType === 'string') {
      parts.push(`${type === 'image' ? '图像' : '音频'}返回（MIME：${record.mimeType}）`);
      continue;
    }

    try {
      parts.push(JSON.stringify(item, null, 2));
    } catch {
      parts.push(String(item));
    }
  }

  if (parts.length === 0) {
    return null;
  }

  return parts.join('\n\n');
}

function jsonResponse(body: unknown, status: number = 200) {
  return NextResponse.json(body, {
    status,
    headers: {
      'cache-control': 'no-store, max-age=0'
    }
  });
}
