// 存储测试脚本 - 用于验证 KV 和 D1 的切换功能

import { D1Storage, HybridStorage } from './cloudflare-env';
import { getStorageConfig } from './storage-config';

// 模拟测试数据
const TEST_KEY = 'test-hash-key';
const TEST_VALUE = 'test-payload-value';
const TEST_TTL = 3600; // 1小时

const formatError = (error: unknown): string =>
  error instanceof Error ? error.message : String(error);

// 模拟 KV 和 D1 接口
class MockKVNamespace {
  private storage = new Map<string, string>();
  private shouldFail = false;
  private failureType: 'get' | 'put' | 'delete' | 'all' = 'all';

  setFailure(shouldFail: boolean, type: 'get' | 'put' | 'delete' | 'all' = 'all') {
    this.shouldFail = shouldFail;
    this.failureType = type;
  }

  async get(key: string): Promise<string | null> {
    if (this.shouldFail && (this.failureType === 'get' || this.failureType === 'all')) {
      throw new Error('Mock KV get failure');
    }
    return this.storage.get(key) || null;
  }

  async put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void> {
    if (this.shouldFail && (this.failureType === 'put' || this.failureType === 'all')) {
      throw new Error('Mock KV put failure');
    }
    this.storage.set(key, value);
    // 模拟 TTL（在实际实现中，KV 会自动处理）
    if (options?.expirationTtl) {
      setTimeout(() => {
        this.storage.delete(key);
      }, options.expirationTtl * 1000);
    }
  }

  async delete(key: string): Promise<void> {
    if (this.shouldFail && (this.failureType === 'delete' || this.failureType === 'all')) {
      throw new Error('Mock KV delete failure');
    }
    this.storage.delete(key);
  }
}

class MockD1Database {
  private storage: Array<{ hash_key: string; payload: string; expires_at: number }> = [];
  private shouldFail = false;

  setFailure(shouldFail: boolean) {
    this.shouldFail = shouldFail;
  }

  private prepare(sql: string) {
    return {
      bind: (...values: any[]) => ({
        first: async <T = any>() => {
          if (this.shouldFail) {
            throw new Error('Mock D1 query failure');
          }

          if (sql.includes('SELECT')) {
            const matches = this.storage.filter(row =>
              row.hash_key === values[0] && row.expires_at > Math.floor(Date.now() / 1000)
            );
            return matches[0] as T | undefined;
          }
          return undefined;
        },
        all: async <T = any>() => {
          if (this.shouldFail) {
            throw new Error('Mock D1 query failure');
          }

          if (sql.includes('SELECT')) {
            const matches = this.storage.filter(row => row.expires_at > Math.floor(Date.now() / 1000));
            return { results: matches } as { results: T[] };
          }
          return { results: [] };
        },
        run: async () => {
          if (this.shouldFail) {
            throw new Error('Mock D1 run failure');
          }

          if (sql.includes('INSERT OR REPLACE')) {
            const existingIndex = this.storage.findIndex(row => row.hash_key === values[0]);
            if (existingIndex >= 0) {
              this.storage[existingIndex] = {
                hash_key: values[0],
                payload: values[1],
                expires_at: values[2]
              };
            } else {
              this.storage.push({
                hash_key: values[0],
                payload: values[1],
                expires_at: values[2]
              });
            }
          } else if (sql.includes('DELETE')) {
            this.storage = this.storage.filter(row => row.hash_key !== values[0]);
          }
        }
      })
    };
  }

  async getStoredValue(key: string): Promise<string | null> {
    const now = Math.floor(Date.now() / 1000);
    const record = this.storage.find(row => row.hash_key === key && row.expires_at > now);
    return record ? record.payload : null;
  }
}

// 测试套件
class StorageTests {
  private mockKV = new MockKVNamespace();
  private mockD1 = new MockD1Database();
  private results: Array<{ test: string; passed: boolean; message: string }> = [];

  async runAllTests() {
    console.log('🧪 开始存储测试...\n');

    // 清理测试数据
    await this.cleanup();

    // 测试 1: 正常的 KV 操作
    await this.testKVNormalOperations();

    // 测试 2: KV 失败时回退到 D1
    await this.testKVFailureFallback();

    // 测试 3: D1 失败时回退到 KV
    await this.testD1FailureFallback();

    // 测试 4: 两者都失败的情况
    await this.testBothFailures();

    // 测试 5: 双重写入功能
    await this.testDualWrite();

    console.log('\n📊 测试结果:');
    this.results.forEach(result => {
      const icon = result.passed ? '✅' : '❌';
      console.log(`${icon} ${result.test}: ${result.message}`);
    });

    const passed = this.results.filter(r => r.passed).length;
    const total = this.results.length;
    console.log(`\n总计: ${passed}/${total} 测试通过`);
  }

  private async cleanup() {
    this.mockKV.setFailure(false);
    this.mockD1.setFailure(false);
    this.mockKV = new MockKVNamespace();
    this.mockD1 = new MockD1Database();
  }

  private async testKVNormalOperations() {
    const storage = new HybridStorage(
      this.mockKV as unknown as KVNamespace,
      this.mockD1 as unknown as D1Database
    );

    try {
      // 测试写入
      await storage.put(TEST_KEY, TEST_VALUE);
      const value = await storage.get(TEST_KEY);

      if (value === TEST_VALUE) {
        this.results.push({
          test: 'KV 正常操作',
          passed: true,
          message: '读写操作成功'
        });
      } else {
        this.results.push({
          test: 'KV 正常操作',
          passed: false,
          message: '读取的值与写入的值不匹配'
        });
      }
    } catch (error) {
      this.results.push({
        test: 'KV 正常操作',
        passed: false,
        message: `操作失败: ${formatError(error)}`
      });
    }
  }

  private async testKVFailureFallback() {
    const storage = new HybridStorage(
      this.mockKV as unknown as KVNamespace,
      this.mockD1 as unknown as D1Database
    );

    // 让 KV 失败
    this.mockKV.setFailure(true, 'put');

    try {
      // 写入应该成功（回退到 D1）
      await storage.put(TEST_KEY + '_fallback', TEST_VALUE);

      // 恢复 KV 用于读取
      this.mockKV.setFailure(false, 'put');
      this.mockKV.setFailure(true, 'get');

      // 读取应该成功（从 D1）
      const value = await storage.get(TEST_KEY + '_fallback');

      if (value === TEST_VALUE) {
        this.results.push({
          test: 'KV 失败回退到 D1',
          passed: true,
          message: '成功回退到 D1 存储'
        });
      } else {
        this.results.push({
          test: 'KV 失败回退到 D1',
          passed: false,
          message: '回退失败，读取值不正确'
        });
      }
    } catch (error) {
      this.results.push({
        test: 'KV 失败回退到 D1',
        passed: false,
        message: `回退失败: ${formatError(error)}`
      });
    }

    this.mockKV.setFailure(false);
  }

  private async testD1FailureFallback() {
    const storage = new HybridStorage(
      this.mockKV as unknown as KVNamespace,
      this.mockD1 as unknown as D1Database
    );

    // 让 D1 失败，但 KV 正常
    this.mockD1.setFailure(true);

    try {
      // 写入应该成功（使用 KV）
      await storage.put(TEST_KEY + '_kv_only', TEST_VALUE);

      // 读取应该成功（从 KV）
      const value = await storage.get(TEST_KEY + '_kv_only');

      if (value === TEST_VALUE) {
        this.results.push({
          test: 'D1 失败回退到 KV',
          passed: true,
          message: '成功使用 KV 存储'
        });
      } else {
        this.results.push({
          test: 'D1 失败回退到 KV',
          passed: false,
          message: 'KV 操作失败，读取值不正确'
        });
      }
    } catch (error) {
      this.results.push({
        test: 'D1 失败回退到 KV',
        passed: false,
        message: `KV 操作失败: ${formatError(error)}`
      });
    }

    this.mockD1.setFailure(false);
  }

  private async testBothFailures() {
    const storage = new HybridStorage(
      this.mockKV as unknown as KVNamespace,
      this.mockD1 as unknown as D1Database
    );

    // 让两个存储都失败
    this.mockKV.setFailure(true);
    this.mockD1.setFailure(true);

    try {
      await storage.put(TEST_KEY + '_fail', TEST_VALUE);
      this.results.push({
        test: '两者都失败',
        passed: false,
        message: '应该抛出错误但没有'
      });
    } catch (error) {
      const errorMessage = formatError(error);
      if (errorMessage.includes('Both KV and D1 storage are unavailable')) {
        this.results.push({
          test: '两者都失败',
          passed: true,
          message: '正确抛出错误'
        });
      } else {
        this.results.push({
          test: '两者都失败',
          passed: false,
          message: `抛出了错误的错误: ${errorMessage}`
        });
      }
    }

    this.mockKV.setFailure(false);
    this.mockD1.setFailure(false);
  }

  private async testDualWrite() {
    const config = getStorageConfig();
    const storage = new HybridStorage(
      this.mockKV as unknown as KVNamespace,
      this.mockD1 as unknown as D1Database,
      config
    );

    try {
      await storage.put(TEST_KEY + '_dual', TEST_VALUE);

      // 验证数据在两个存储中
      const kvValue = await this.mockKV.get(TEST_KEY + '_dual');
      const d1Value = await this.mockD1.getStoredValue(TEST_KEY + '_dual');

      if (kvValue === TEST_VALUE && d1Value === TEST_VALUE) {
        this.results.push({
          test: '双重写入功能',
          passed: true,
          message: '数据成功写入两个存储'
        });
      } else {
        this.results.push({
          test: '双重写入功能',
          passed: false,
          message: '双重写入失败'
        });
      }
    } catch (error) {
      this.results.push({
        test: '双重写入功能',
        passed: false,
        message: `双重写入失败: ${formatError(error)}`
      });
    }
  }
}

// 导出测试函数
export async function runStorageTests() {
  const tests = new StorageTests();
  await tests.runAllTests();
}

// 如果直接运行此文件
if (typeof require !== 'undefined' && require.main === module) {
  runStorageTests().catch(console.error);
}
