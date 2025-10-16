import { getCloudflareContext } from '@opennextjs/cloudflare';

type EnvWithCodeMap = CloudflareEnv & {
  CODE_MAP: NonNullable<CloudflareEnv['CODE_MAP']>;
};

export async function getCloudflareEnv(): Promise<EnvWithCodeMap> {
  const { env } = await getCloudflareContext({ async: true });
  if (!env?.CODE_MAP) {
    throw new Error('Cloudflare binding "CODE_MAP" is not configured.');
  }
  return env as EnvWithCodeMap;
}
