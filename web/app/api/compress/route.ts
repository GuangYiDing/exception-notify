import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

import { getCloudflareEnv } from '@/lib/cloudflare-env';


type CompressRequestBody = {
  payload?: unknown;
};

const jsonResponse = (body: unknown, status = 200) =>
  NextResponse.json(body, {
    status,
    headers: { 'content-type': 'application/json; charset=UTF-8' }
  });

export async function POST(request: NextRequest) {
  const env = await getCloudflareEnv();
  let payload: unknown;

  try {
    const body = (await request.json()) as CompressRequestBody;
    payload = body.payload;
  } catch {
    return jsonResponse({ code: 1, message: 'Invalid JSON body' }, 400);
  }

  if (typeof payload !== 'string' || payload.length === 0) {
    return jsonResponse({ code: 1, message: 'payload must be a non-empty string' }, 400);
  }

  const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(payload));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(byte => byte.toString(16).padStart(2, '0')).join('');

  const existingKey = await env.CODE_MAP.get(hashHex);
  if (existingKey) {
    return jsonResponse({ code: 0, data: hashHex });
  }

  await env.CODE_MAP.put(hashHex, payload);
  return jsonResponse({ code: 0, data: hashHex });
}
