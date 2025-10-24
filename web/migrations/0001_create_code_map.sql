-- 创建 code_map 表用于存储 KV 的备用数据
CREATE TABLE IF NOT EXISTS code_map (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  hash_key TEXT NOT NULL UNIQUE,
  payload TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
  updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_code_map_hash_key ON code_map(hash_key);
CREATE INDEX IF NOT EXISTS idx_code_map_expires_at ON code_map(expires_at);

-- 创建自动清理过期数据的触发器（可选）
CREATE TRIGGER IF NOT EXISTS cleanup_expired_codes
AFTER INSERT ON code_map
BEGIN
  DELETE FROM code_map WHERE expires_at < strftime('%s', 'now');
END;