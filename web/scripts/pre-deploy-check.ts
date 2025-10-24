// 部署前检查脚本
// 用于验证 D1 配置和部署前的各项检查

import { execSync } from 'child_process';
import { readFileSync, existsSync } from 'fs';
import { join } from 'path';

interface CheckResult {
  name: string;
  passed: boolean;
  message: string;
}

class PreDeployChecker {
  private results: CheckResult[] = [];
  private projectRoot = process.cwd();

  async runAllChecks(): Promise<void> {
    console.log('🚀 开始部署前检查...\n');

    // 检查 1: 验证必要的文件存在
    this.checkRequiredFiles();

    // 检查 2: 验证环境配置
    this.checkEnvironmentConfig();

    // 检查 3: 验证 TypeScript 编译
    await this.checkTypeScriptCompilation();

    // 检查 4: 验证依赖安装
    this.checkDependencies();

    console.log('\n📋 检查结果:');
    this.results.forEach(result => {
      const icon = result.passed ? '✅' : '❌';
      console.log(`${icon} ${result.name}: ${result.message}`);
    });

    const passed = this.results.filter(r => r.passed).length;
    const total = this.results.length;
    console.log(`\n总计: ${passed}/${total} 检查通过`);

    if (passed !== total) {
      console.log('\n❌ 部署前检查失败，请修复上述问题后重试。');
      process.exit(1);
    }

    console.log('\n✅ 所有检查通过，可以开始部署！');
  }

  private checkRequiredFiles(): void {
    const requiredFiles = [
      'wrangler.toml',
      'migrations/0001_create_code_map.sql',
      'lib/cloudflare-env.ts',
      'lib/storage-config.ts',
    ];

    for (const file of requiredFiles) {
      const filePath = join(this.projectRoot, file);
      const exists = existsSync(filePath);

      this.results.push({
        name: `必要文件检查: ${file}`,
        passed: exists,
        message: exists ? '文件存在' : `文件不存在: ${filePath}`
      });
    }
  }

  private checkEnvironmentConfig(): void {
    // 检查 wrangler.toml 配置
    try {
      const wranglerConfig = readFileSync(join(this.projectRoot, 'wrangler.toml'), 'utf8');

      const hasKVNamespace = wranglerConfig.includes('[[kv_namespaces]]');
      const hasD1Database = wranglerConfig.includes('[[d1_databases]]');
      const hasCodeMapBinding = wranglerConfig.includes('binding = "CODE_MAP"');
      const hasDBBinding = wranglerConfig.includes('binding = "DB"');

      this.results.push({
        name: 'KV 命名空间配置',
        passed: hasKVNamespace && hasCodeMapBinding,
        message: hasKVNamespace && hasCodeMapBinding ? '配置正确' : '缺少 KV 命名空间配置'
      });

      this.results.push({
        name: 'D1 数据库配置',
        passed: hasD1Database && hasDBBinding,
        message: hasD1Database && hasDBBinding ? '配置正确' : '缺少 D1 数据库配置'
      });

      // 检查环境变量（可选）
      const hasKVNamespaceId = wranglerConfig.includes('${KV_NAMESPACE_ID}');
      const hasD1DatabaseId = wranglerConfig.includes('${D1_DATABASE_ID}');

      this.results.push({
        name: '环境变量占位符',
        passed: hasKVNamespaceId && hasD1DatabaseId,
        message: hasKVNamespaceId && hasD1DatabaseId ? '环境变量配置正确' : '缺少环境变量占位符'
      });

    } catch (error) {
      this.results.push({
        name: 'wrangler.toml 配置检查',
        passed: false,
        message: `读取配置文件失败: ${error}`
      });
    }
  }

  private async checkTypeScriptCompilation(): Promise<void> {
    try {
      // 尝试编译 TypeScript 代码
      execSync('npx tsc --noEmit', { cwd: this.projectRoot, stdio: 'pipe' });

      this.results.push({
        name: 'TypeScript 编译检查',
        passed: true,
        message: '编译通过'
      });
    } catch (error: any) {
      this.results.push({
        name: 'TypeScript 编译检查',
        passed: false,
        message: `编译失败: ${error.message?.split('\n')[0] || error}`
      });
    }
  }

  private checkDependencies(): void {
    try {
      const packageJson = JSON.parse(
        readFileSync(join(this.projectRoot, 'package.json'), 'utf8')
      );

      const requiredDeps = ['@opennextjs/cloudflare', 'next', 'react', 'react-dom'];
      const requiredDevDeps = ['@cloudflare/workers-types', 'wrangler', 'tsx'];

      const allDeps = { ...packageJson.dependencies, ...packageJson.devDependencies };
      const missingDeps = [...requiredDeps, ...requiredDevDeps].filter(
        dep => !allDeps[dep]
      );

      this.results.push({
        name: '依赖检查',
        passed: missingDeps.length === 0,
        message: missingDeps.length === 0
          ? '所有依赖都已安装'
          : `缺少依赖: ${missingDeps.join(', ')}`
      });

    } catch (error) {
      this.results.push({
        name: '依赖检查',
        passed: false,
        message: `检查依赖失败: ${error}`
      });
    }
  }
}

// 导出检查器类
export { PreDeployChecker };

// 如果直接运行此文件
if (typeof require !== 'undefined' && require.main === module) {
  const checker = new PreDeployChecker();
  checker.runAllChecks().catch(error => {
    console.error('检查过程出错:', error);
    process.exit(1);
  });
}