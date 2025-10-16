import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: '异常通知 AI 工作台',
  description: '基于异常上下文的智能分析助手'
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
