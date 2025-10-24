import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

import { getCloudflareEnv, createStorage } from '@/lib/cloudflare-env';


const jsonResponse = (body: unknown, status = 200) =>
  NextResponse.json(body, {
    status,
    headers: { 'content-type': 'application/json; charset=UTF-8' }
  });

export async function GET(request: NextRequest) {
  const env = await getCloudflareEnv();
  const storage = createStorage(env);
  const searchParams = request.nextUrl.searchParams;
  const payload = searchParams.get('payload');

  if (!payload) {
    return jsonResponse({ code: 1, message: 'Missing payload parameter' }, 400);
  }

  if (payload.length === 0) {
    return jsonResponse({ code: 1, message: 'payload must be a non-empty string' }, 400);
  }

  const original = await storage.get(payload);
  if (!original) {
    return jsonResponse({ code: 1, message: 'short code not found' }, 404);
  }

  return jsonResponse({ code: 0, data: original });
}
