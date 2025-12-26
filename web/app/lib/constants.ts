import type { AiAnalysisPayload } from '@/lib/ai-analysis-payload';

export const SETTINGS_KEY = 'exception-notify-ai-settings';

export const defaultSystemPrompt =
  '你是一个资深 Java/Spring 工程师，擅长分析异常堆栈并提供修复建议。请结合提供的上下文，输出简洁明确、可执行的建议。';

export const defaultSettings: {
  endpoint: string;
  apiKey: string;
  model: string;
  temperature: number;
  systemPrompt: string;
} = {
  endpoint: 'https://api.openai.com/v1/chat/completions',
  apiKey: '',
  model: 'gpt-4o-mini',
  temperature: 0.2,
  systemPrompt: defaultSystemPrompt
};

export const DEMO_PAYLOAD_OBJECT: AiAnalysisPayload = {
  appName: 'mall-order-service',
  environment: 'prod',
  occurrenceTime: '2025-01-15T10:05:12',
  exceptionType: 'java.lang.NullPointerException',
  exceptionMessage: '创建订单时订单对象为空',
  location: 'com.mall.order.service.OrderService.createOrder(OrderService.java:148)',
  stacktrace:
    'java.lang.NullPointerException: Cannot invoke "Order.getItems()" because "order" is null\n' +
    '\tat com.mall.order.service.OrderService.createOrder(OrderService.java:148)\n' +
    '\tat com.mall.order.facade.OrderFacade.submitOrder(OrderFacade.java:54)\n' +
    '\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n' +
    '\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n' +
    '\tat com.mall.web.controller.OrderController.placeOrder(OrderController.java:87)',
  codeContext:
    '142  Order order = orderRequestMapper.toEntity(request);\n' +
    '143  validateOrder(order);\n' +
    '144  calculatePromotion(order);\n' +
    '145  \n' +
    '146  // TODO: handle null order earlier\n' +
    '147  log.debug("About to persist order: {}", order != null ? order.getId() : "null");\n' +
    '>>> 148  orderRepository.save(order);\n' +
    '149  stockService.lock(order.getItems());\n' +
    '150  return order;\n',
  traceId: 'trace-prod-3f92dca1209b',
  traceUrl: 'https://trace.example.com/id/trace-prod-3f92dca1209b',
  author: {
    name: 'Alice Chen',
    email: 'alice.chen@example.com',
    lastCommitTime: '2025-01-10T09:32:11',
    fileName: 'OrderService.java',
    lineNumber: 148,
    commitMessage: 'fix: order validation handles null request'
  }
};

export const DEMO_PAYLOAD = JSON.stringify(DEMO_PAYLOAD_OBJECT);
export const DEMO_SHORT_CODE = '70235bf91147d98283f6891a9b98f1734d42298b11e0a0c9bea66e661fb12837';
export const DEMO_KV: Record<string, string> = {
  [DEMO_SHORT_CODE]: DEMO_PAYLOAD
};

export const repositoryUrl = 'https://github.com/GuangYiDing/exception-notify';
export const rawBuildSha = (process.env.NEXT_PUBLIC_BUILD_SHA ?? '').trim();
export const buildSha = rawBuildSha || 'dev';
export const buildShaDisplay = buildSha.length > 7 ? buildSha.slice(0, 7) : buildSha;
export const isDevBuild = buildSha === 'dev';
export const buildShaUrl = isDevBuild ? repositoryUrl : `${repositoryUrl}/commit/${buildSha}`;
