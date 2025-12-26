'use client';

import { DEMO_SHORT_CODE } from '../lib/constants';

export function WelcomeCard() {
  const loadDemoPayload = () => {
    const url = new URL(window.location.href);
    url.searchParams.set('payload', DEMO_SHORT_CODE);
    window.location.href = url.toString();
  };

  return (
    <section className="card welcome-card">
      <h2>欢迎使用异常 AI 分析工作台</h2>
      <p className="welcome-text">
        通常情况下，您会通过异常通知中的链接直接访问带有异常数据的页面。
        如果您想先体验功能，可以点击下方按钮加载示例数据。
      </p>
      <button className="demo-button" onClick={loadDemoPayload}>
        <span className="demo-icon">🚀</span>
        <span>体验示例</span>
      </button>
      <p className="welcome-subtitle">
        基于异常上下文快速梳理问题并联动对话式分析。
      </p>
    </section>
  );
}
