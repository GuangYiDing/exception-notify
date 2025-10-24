// 存储配置选项
export interface StorageConfig {
  // 是否启用 D1 作为备用存储
  enableD1Fallback: boolean;
  // 是否将数据同时写入 KV 和 D1（双重备份）
  enableDualWrite: boolean;
  // KV 操作超时时间（毫秒）
  kvTimeoutMs: number;
  // D1 操作超时时间（毫秒）
  d1TimeoutMs: number;
  // 是否启用详细日志记录
  enableDebugLogs: boolean;
  // 默认 TTL（秒）
  defaultTtl: number;
}

// 默认配置
export const defaultStorageConfig: StorageConfig = {
  enableD1Fallback: true,
  enableDualWrite: true,
  kvTimeoutMs: 5000,
  d1TimeoutMs: 3000,
  enableDebugLogs: false,
  defaultTtl: 60 * 60 * 24 * 30, // 30 days
};

// 从环境变量获取配置
export function getStorageConfig(): StorageConfig {
  return {
    enableD1Fallback: process.env.ENABLE_D1_FALLBACK !== 'false',
    enableDualWrite: process.env.ENABLE_DUAL_WRITE !== 'false',
    kvTimeoutMs: parseInt(process.env.KV_TIMEOUT_MS || '5000', 10),
    d1TimeoutMs: parseInt(process.env.D1_TIMEOUT_MS || '3000', 10),
    enableDebugLogs: process.env.ENABLE_STORAGE_DEBUG_LOGS === 'true',
    defaultTtl: parseInt(process.env.DEFAULT_TTL || String(defaultStorageConfig.defaultTtl), 10),
  };
}

// 日志记录器
export class StorageLogger {
  constructor(private enabled: boolean) {}

  log(message: string, data?: any) {
    if (this.enabled) {
      console.log(`[Storage] ${message}`, data || '');
    }
  }

  warn(message: string, data?: any) {
    console.warn(`[Storage] ${message}`, data || '');
  }

  error(message: string, error?: any) {
    console.error(`[Storage] ${message}`, error || '');
  }
}

// 创建带超时的 Promise
export function withTimeout<T>(promise: Promise<T>, timeoutMs: number, operation: string): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((_, reject) =>
      setTimeout(() => reject(new Error(`${operation} timed out after ${timeoutMs}ms`)), timeoutMs)
    ),
  ]);
}