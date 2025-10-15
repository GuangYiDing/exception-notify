const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const sourceDir = path.join(root, 'functions');
const targetDir = path.join(root, 'dist', 'functions');

if (!fs.existsSync(sourceDir)) {
  console.warn(`[copy-functions] Skipping: ${sourceDir} does not exist.`);
  process.exit(0);
}

fs.rmSync(targetDir, { recursive: true, force: true });
fs.mkdirSync(path.dirname(targetDir), { recursive: true });
fs.cpSync(sourceDir, targetDir, { recursive: true });
console.log(`[copy-functions] Copied to ${targetDir}`);
