import { getCloudflareContext } from '@opennextjs/cloudflare';
import { getStorageConfig, StorageConfig, StorageLogger, withTimeout } from './storage-config';

// 扩展 CloudflareEnv 类型以包含 D1 数据库
type EnvWithCodeMap = CloudflareEnv & {
  CODE_MAP: NonNullable<CloudflareEnv['CODE_MAP']>;
  DB?: D1Database;
};

export async function getCloudflareEnv(): Promise<EnvWithCodeMap> {
  const { env } = await getCloudflareContext({ async: true });
  if (!env?.CODE_MAP) {
    throw new Error('Cloudflare binding "CODE_MAP" is not configured.');
  }
  return env as EnvWithCodeMap;
}

// D1 数据库操作接口
export interface D1CodeMapStorage {
  get(key: string): Promise<string | null>;
  put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void>;
  delete(key: string): Promise<void>;
  list(): Promise<Array<{ key: string; value: string }>>;
}

// D1 数据库存储实现
export class D1Storage implements D1CodeMapStorage {
  constructor(private db: D1Database) {}

  async get(key: string): Promise<string | null> {
    try {
      const result = await this.db
        .prepare('SELECT payload FROM code_map WHERE hash_key = ? AND expires_at > ?')
        .bind(key, Math.floor(Date.now() / 1000))
        .first<{ payload: string }>();

      return result?.payload || null;
    } catch (error) {
      console.error('D1 get error:', error);
      return null;
    }
  }

  async put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void> {
    try {
      const now = Math.floor(Date.now() / 1000);
      const expiresAt = options?.expirationTtl ? now + options.expirationTtl : now + (60 * 60 * 24 * 30); // 默认30天

      await this.db
        .prepare(`
          INSERT OR REPLACE INTO code_map (hash_key, payload, expires_at, updated_at)
          VALUES (?, ?, ?, ?)
        `)
        .bind(key, value, expiresAt, now)
        .run();
    } catch (error) {
      console.error('D1 put error:', error);
      throw error;
    }
  }

  async delete(key: string): Promise<void> {
    try {
      await this.db
        .prepare('DELETE FROM code_map WHERE hash_key = ?')
        .bind(key)
        .run();
    } catch (error) {
      console.error('D1 delete error:', error);
      throw error;
    }
  }

  async list(): Promise<Array<{ key: string; value: string }>> {
    try {
      const results = await this.db
        .prepare('SELECT hash_key, payload FROM code_map WHERE expires_at > ?')
        .bind(Math.floor(Date.now() / 1000))
        .all<{ hash_key: string; payload: string }>();

      return results.results.map(row => ({
        key: row.hash_key,
        value: row.payload
      }));
    } catch (error) {
      console.error('D1 list error:', error);
      return [];
    }
  }
}

// 混合存储类：优先使用 KV，失败时回退到 D1
export class HybridStorage implements D1CodeMapStorage {
  private d1Storage: D1Storage;
  private config: StorageConfig;
  private logger: StorageLogger;

  constructor(private kv: KVNamespace, private db?: D1Database, config?: StorageConfig) {
    this.d1Storage = db ? new D1Storage(db) : null as any;
    this.config = config || getStorageConfig();
    this.logger = new StorageLogger(this.config.enableDebugLogs);
  }

  async get(key: string): Promise<string | null> {
    this.logger.log('Getting key', { key });

    // 首先尝试从 KV 获取
    try {
      const kvResult = await withTimeout(
        this.kv.get(key),
        this.config.kvTimeoutMs,
        'KV get'
      );

      if (kvResult !== null) {
        this.logger.log('Found in KV', { key });
        return kvResult;
      }

      this.logger.log('Not found in KV, trying D1', { key });
    } catch (kvError) {
      this.logger.warn('KV get failed, falling back to D1', { key, error: kvError });
    }

    // KV 失败或未找到，尝试 D1
    if (this.config.enableD1Fallback && this.d1Storage) {
      try {
        const d1Result = await this.d1Storage.get(key);
        this.logger.log('D1 get result', { key, found: d1Result !== null });
        return d1Result;
      } catch (d1Error) {
        this.logger.error('D1 get failed', { key, error: d1Error });
      }
    }

    this.logger.log('Key not found in any storage', { key });
    return null;
  }

  async put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void> {
    this.logger.log('Putting key', { key, hasOptions: !!options });

    // 首先尝试写入 KV
    try {
      await withTimeout(
        this.kv.put(key, value, options),
        this.config.kvTimeoutMs,
        'KV put'
      );

      this.logger.log('Successfully wrote to KV', { key });

      // 如果启用了双重写入，也写入 D1 作为备份
      if (this.config.enableDualWrite && this.d1Storage) {
        try {
          await this.d1Storage.put(key, value, options);
          this.logger.log('Successfully wrote backup to D1', { key });
        } catch (d1Error) {
          this.logger.warn('D1 backup write failed', { key, error: d1Error });
          // 不抛出错误，因为 KV 写入成功了
        }
      }
      return;
    } catch (kvError) {
      this.logger.warn('KV put failed, using D1 only', { key, error: kvError });

      // KV 失败，写入 D1
      if (this.config.enableD1Fallback && this.d1Storage) {
        try {
          await this.d1Storage.put(key, value, options);
          this.logger.log('Successfully wrote to D1 as fallback', { key });
          return;
        } catch (d1Error) {
          this.logger.error('D1 fallback put failed', { key, error: d1Error });
        }
      }

      // 如果两者都不可用，抛出错误
      throw new Error('Both KV and D1 storage are unavailable');
    }
  }

  async delete(key: string): Promise<void> {
    this.logger.log('Deleting key', { key });

    // 尝试从 KV 删除
    try {
      await withTimeout(
        this.kv.delete(key),
        this.config.kvTimeoutMs,
        'KV delete'
      );
      this.logger.log('Successfully deleted from KV', { key });
    } catch (kvError) {
      this.logger.warn('KV delete failed', { key, error: kvError });
    }

    // 尝试从 D1 删除
    if (this.d1Storage) {
      try {
        await this.d1Storage.delete(key);
        this.logger.log('Successfully deleted from D1', { key });
      } catch (d1Error) {
        this.logger.warn('D1 delete failed', { key, error: d1Error });
      }
    }
  }

  async list(): Promise<Array<{ key: string; value: string }>> {
    this.logger.log('Listing keys from D1 (KV does not support efficient listing)');

    // KV 不支持高效的 list 操作，所以直接使用 D1
    if (this.d1Storage) {
      try {
        const results = await this.d1Storage.list();
        this.logger.log('Listed keys from D1', { count: results.length });
        return results;
      } catch (error) {
        this.logger.error('D1 list failed', { error });
      }
    }

    this.logger.log('No storage available for listing');
    return [];
  }
}

export function createStorage(env: EnvWithCodeMap, config?: StorageConfig): D1CodeMapStorage {
  return new HybridStorage(env.CODE_MAP, env.DB, config);
}
