import {Hono} from 'hono';
import {handle} from 'hono/cloudflare-pages';

type Bindings = {
    CODE_MAP: KVNamespace;
};

const app = new Hono<{ Bindings: Bindings }>();


app.post('/api/compress', async (c) => {
    let payload: unknown;

    try {
        const body = await c.req.json();
        payload = body?.payload;
    } catch {
        return c.json({code: 1, message: 'Invalid JSON body'}, 400);
    }

    if (typeof payload !== 'string' || payload.length === 0) {
        return c.json({code: 1, message: 'payload must be a non-empty string'}, 400);
    }

    // 使用 sha-256 作为 key
    const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(payload));
    const hashArray = Array.from(new Uint8Array(hash));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    // 检查 map 中是否已存在 value,已存在直接返回 key
    const existingKey = await c.env.CODE_MAP.get(hashHex);
    if (existingKey) {
        return c.json({code: 0, data: hashHex});
    }

    await c.env.CODE_MAP.put(hashHex, payload);

    return c.json({code: 0, data: hashHex});
});

app.get('/api/depress', async (c) => {
    const payload = c.req.query('payload');

    if (!payload) {
        return c.json({code: 1, message: 'Missing payload parameter'}, 400);
    }

    if (typeof payload !== 'string' || payload.length === 0) {
        return c.json({code: 1, message: 'payload must be a non-empty string'}, 400);
    }

    const original = await c.env.CODE_MAP.get(payload);

    if (!original) {
        return c.json({code: 1, message: 'short code not found'}, 404);
    }

    return c.json({code: 0, data: original});
});

export const onRequest = handle(app);
