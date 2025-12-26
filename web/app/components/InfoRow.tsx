'use client';

type InfoRowProps = {
  label: string;
  value?: string | number | null;
  onCopySuccess?: () => void;
};

export function InfoRow({ label, value, onCopySuccess }: InfoRowProps) {
  if (value === undefined || value === null || value === '') {
    return null;
  }

  const text = String(value);

  const handleDoubleClick = async () => {
    try {
      await navigator.clipboard.writeText(text);
      onCopySuccess?.();
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <div className="info-row">
      <span className="info-label">{label}</span>
      <span
        className="info-value"
        title={`${text}\n\n💡 双击复制`}
        onDoubleClick={handleDoubleClick}
      >
        {text}
      </span>
    </div>
  );
}
