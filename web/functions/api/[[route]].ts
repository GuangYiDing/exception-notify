type PagesFunction<Env = unknown> = (context: { request: Request; env: Env }) => Response | Promise<Response>;

interface Env {
    CODE_MAP: KVNamespace;
}

const jsonResponse = (body: unknown, status = 200): Response => new Response(JSON.stringify(body), {
    status,
    headers: {'content-type': 'application/json; charset=UTF-8'}
});

const normalizePath = (pathname: string): string => {
    if (pathname.length > 1 && pathname.endsWith('/')) {
        return pathname.slice(0, -1);
    }
    return pathname;
};

export const onRequest: PagesFunction<Env> = async (context) => {
    const {request, env} = context;
    const url = new URL(request.url);
    if (!url.pathname.startsWith('/api')) {
        return new Response('Not Found', {status: 404});
    }

    const rawPath = url.pathname.slice('/api'.length) || '/';
    const path = normalizePath(rawPath || '/');

    if (request.method === 'POST' && path === '/compress') {
        return handleCompressRequest(request, env);
    }

    if (request.method === 'GET' && path === '/decompress') {
        return handleDecompressRequest(url, env);
    }

    return new Response('Not Found', {status: 404});
};

const handleCompressRequest = async (request: Request, env: Env): Promise<Response> => {
    let payload: unknown;

    try {
        const body = await request.json();
        payload = (body as { payload?: unknown })?.payload;
    } catch {
        return jsonResponse({code: 1, message: 'Invalid JSON body'}, 400);
    }

    if (typeof payload !== 'string' || payload.length === 0) {
        return jsonResponse({code: 1, message: 'payload must be a non-empty string'}, 400);
    }

    const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(payload));
    const hashArray = Array.from(new Uint8Array(hash));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    const existingKey = await env.CODE_MAP.get(hashHex);
    if (existingKey) {
        return jsonResponse({code: 0, data: hashHex});
    }

    await env.CODE_MAP.put(hashHex, payload);

    return jsonResponse({code: 0, data: hashHex});
};

const handleDecompressRequest = async (url: URL, env: Env): Promise<Response> => {
    const payload = url.searchParams.get('payload');

    if (!payload) {
        return jsonResponse({code: 1, message: 'Missing payload parameter'}, 400);
    }

    if (payload.length === 0) {
        return jsonResponse({code: 1, message: 'payload must be a non-empty string'}, 400);
    }

    const original = await env.CODE_MAP.get(payload);

    if (!original) {
        return jsonResponse({code: 1, message: 'short code not found'}, 404);
    }

    return jsonResponse({code: 0, data: original});
};
