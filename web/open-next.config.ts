import { defineCloudflareConfig } from "@opennextjs/cloudflare";
// import R2IncrementalCache from "@opennextjs/cloudflare/overrides/incremental-cache/r2-incremental-cache";

export default defineCloudflareConfig({
    // incrementalCache: R2IncrementalCache,
});


// import cloudflare from '@opennextjs/cloudflare';
// export default {
//   outDir: '.open-next',
//   adapter: cloudflare({
//     wranglerConfigPath: './wrangler.toml'
//   })
// };
