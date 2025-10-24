import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

import { getCloudflareEnv, createStorage } from '@/lib/cloudflare-env';
import type { AiAnalysisPayload } from '@/lib/ai-analysis-payload';

type CompressRequestBody = {
  payload?: AiAnalysisPayload | null;
};

const jsonResponse = (body: unknown, status = 200) =>
  NextResponse.json(body, {
    status,
    headers: { 'content-type': 'application/json; charset=UTF-8' }
  });

export async function POST(request: NextRequest) {
  const env = await getCloudflareEnv();
  const storage = createStorage(env);
  let payload: AiAnalysisPayload | null = null;

  try {
    const body = (await request.json()) as CompressRequestBody;
    payload = body.payload ?? null;
  } catch {
    return jsonResponse({ code: 1, message: 'Invalid JSON body' }, 400);
  }

  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return jsonResponse({ code: 1, message: 'payload must be an object' }, 400);
  }

  const payloadJson = JSON.stringify(payload);
  if (!payloadJson || payloadJson.length === 0) {
    return jsonResponse({ code: 1, message: 'payload must not be empty' }, 400);
  }

  const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(payloadJson));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(byte => byte.toString(16).padStart(2, '0')).join('');

  const existingKey = await storage.get(hashHex);
  if (existingKey) {
    return jsonResponse({ code: 0, data: hashHex });
  }

  await storage.put(hashHex, payloadJson, { expirationTtl: 60 * 60 * 24 * 30 });
  return jsonResponse({ code: 0, data: hashHex });
}
