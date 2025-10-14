package com.nolimit35.springkit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.AiAnalysisPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class CompressedPayloadAiAnalysisLinkServiceTest {

    private ExceptionNotifyProperties properties;
    private CompressedPayloadAiAnalysisLinkService service;

    @BeforeEach
    void setUp() {
        properties = new ExceptionNotifyProperties();
        ExceptionNotifyProperties.AI aiConfig = new ExceptionNotifyProperties.AI();
        aiConfig.setEnabled(true);
        aiConfig.setAnalysisPageUrl("http://localhost:5173");
        aiConfig.setPayloadParam("payload");
        properties.setAi(aiConfig);

        service = new CompressedPayloadAiAnalysisLinkService(properties);
    }

    @Test
    void isAvailableReturnsTrueWhenConfigured() {
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailableReturnsFalseWithoutUrl() {
        properties.getAi().setAnalysisPageUrl(null);
        CompressedPayloadAiAnalysisLinkService unavailable = new CompressedPayloadAiAnalysisLinkService(properties);
        assertFalse(unavailable.isAvailable());
    }

    @Test
    void buildAnalysisLinkReturnsNullWhenServiceUnavailable() {
        ExceptionNotifyProperties.AI aiConfig = new ExceptionNotifyProperties.AI();
        aiConfig.setEnabled(false);
        properties.setAi(aiConfig);
        CompressedPayloadAiAnalysisLinkService unavailable = new CompressedPayloadAiAnalysisLinkService(properties);

        String link = unavailable.buildAnalysisLink(AiAnalysisPayload.builder().build());
        assertNull(link);
    }

    @Test
    void buildAnalysisLinkCompressesPayload() throws IOException {
        AiAnalysisPayload payload = createSamplePayload("NullPointer in OrderService");

        String link = service.buildAnalysisLink(payload);
        assertNotNull(link);
        assertTrue(link.startsWith("http://localhost:5173"));

        Map<String, Object> result = decodePayloadFromLink(link);
        assertEquals("mall-order-service", result.get("appName"));
        assertEquals("创建订单时订单对象为空", result.get("exceptionMessage"));
        assertEquals("com.mall.order.service.OrderService.createOrder(OrderService.java:148)", result.get("location"));

        Map<?, ?> author = (Map<?, ?>) result.get("author");
        assertNotNull(author);
        assertEquals("Alice Chen", author.get("name"));
    }

    @Test
    void buildAnalysisLinkForMultipleRealisticScenarios() throws IOException {
        List<String> scenarios = Arrays.asList(
                "NullPointer in OrderService",
                "SQL Timeout in CustomerRepository",
                "Feign BadRequest while calling Payment",
                "MessageQueue Deserialization Failure"
        );

        Map<String, Map<String, Object>> decodedPayloads = new HashMap<>();

        for (String scenario : scenarios) {
            AiAnalysisPayload payload = createSamplePayload(scenario);
            String link = service.buildAnalysisLink(payload);
            assertNotNull(link, "Link should not be null for scenario " + scenario);

            Map<String, Object> result = decodePayloadFromLink(link);
            decodedPayloads.put(scenario, result);

            System.out.println("AIanalysis sample [" + scenario + "]: " + link);
            assertEquals(payload.getExceptionType(), result.get("exceptionType"));
            assertEquals(payload.getExceptionMessage(), result.get("exceptionMessage"));
            assertEquals(payload.getAppName(), result.get("appName"));
            assertEquals(payload.getLocation(), result.get("location"));
        }

        Map<String, Object> feignPayload = decodedPayloads.get("Feign BadRequest while calling Payment");
        assertNotNull(feignPayload);
        assertTrue(((String) feignPayload.get("stacktrace")).contains("feign.FeignException$BadRequest"));
    }

    private AiAnalysisPayload createSamplePayload(String scenario) {
        switch (scenario) {
            case "NullPointer in OrderService":
                return AiAnalysisPayload.builder()
                        .appName("mall-order-service")
                        .environment("prod")
                        .occurrenceTime("2025-01-15T10:05:12")
                        .exceptionType("java.lang.NullPointerException")
                        .exceptionMessage("创建订单时订单对象为空")
                        .location("com.mall.order.service.OrderService.createOrder(OrderService.java:148)")
                        .stacktrace("java.lang.NullPointerException: Cannot invoke \"Order.getItems()\" because \"order\" is null\n" +
                                "\tat com.mall.order.service.OrderService.createOrder(OrderService.java:148)\n" +
                                "\tat com.mall.order.facade.OrderFacade.submitOrder(OrderFacade.java:54)\n" +
                                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                                "\tat com.mall.web.controller.OrderController.placeOrder(OrderController.java:87)")
                        .codeContext("142  Order order = orderRequestMapper.toEntity(request);\n" +
                                "143  validateOrder(order);\n" +
                                "144  calculatePromotion(order);\n" +
                                "145  \n" +
                                "146  // TODO: handle null order earlier\n" +
                                "147  log.debug(\"About to persist order: {}\", order != null ? order.getId() : \"null\");\n" +
                                ">>> 148  orderRepository.save(order);\n" +
                                "149  stockService.lock(order.getItems());\n" +
                                "150  return order;\n")
                        .traceId("trace-prod-3f92dca1209b")
                        .traceUrl("https://trace.example.com/id/trace-prod-3f92dca1209b")
                        .author(AiAnalysisPayload.Author.builder()
                                .name("Alice Chen")
                                .email("alice.chen@example.com")
                                .lastCommitTime("2025-01-10T09:32:11")
                                .fileName("OrderService.java")
                                .lineNumber(148)
                                .commitMessage("fix: order validation handles null request")
                                .build())
                        .build();
            case "SQL Timeout in CustomerRepository":
                return AiAnalysisPayload.builder()
                        .appName("crm-customer-service")
                        .environment("prod")
                        .occurrenceTime("2025-01-14T22:41:03")
                        .exceptionType("org.springframework.dao.QueryTimeoutException")
                        .exceptionMessage("JDBC query timeout for SQL [select * from customer where phone = ?]")
                        .location("com.crm.customer.repository.CustomerRepository.findByPhone(CustomerRepository.java:92)")
                        .stacktrace("org.springframework.dao.QueryTimeoutException: Timeout executing query for table \"customer\"\n" +
                                "\tat org.springframework.jdbc.core.JdbcTemplate.execute(JdbcTemplate.java:370)\n" +
                                "\tat com.crm.customer.repository.CustomerRepository.findByPhone(CustomerRepository.java:92)\n" +
                                "\tat com.crm.customer.service.CustomerLookupService.loadCustomer(CustomerLookupService.java:51)\n" +
                                "\tat com.crm.api.CustomerController.lookup(CustomerController.java:44)")
                        .codeContext("86   public Optional<CustomerEntity> findByPhone(String phone) {\n" +
                                "87       long start = System.currentTimeMillis();\n" +
                                "88       try {\n" +
                                "89           return jdbcTemplate.query(sqlByPhone, customerRowMapper, phone)\n" +
                                "90               .stream()\n" +
                                "91               .findFirst();\n" +
                                ">>> 92       } finally {\n" +
                                "93           metrics.timer(\"customer.lookup.by-phone\").record(System.currentTimeMillis() - start);\n" +
                                "94       }\n" +
                                "95   }\n")
                        .traceId("trace-prod-b4e9e231761b")
                        .traceUrl("https://trace.example.com/id/trace-prod-b4e9e231761b")
                        .author(AiAnalysisPayload.Author.builder()
                                .name("Wei Sun")
                                .email("wei.sun@example.com")
                                .lastCommitTime("2025-01-12T17:46:40")
                                .fileName("CustomerRepository.java")
                                .lineNumber(92)
                                .commitMessage("perf: add query metrics for phone lookup")
                                .build())
                        .build();
            case "Feign BadRequest while calling Payment":
                return AiAnalysisPayload.builder()
                        .appName("mall-order-service")
                        .environment("prod")
                        .occurrenceTime("2025-01-15T08:12:08")
                        .exceptionType("feign.FeignException$BadRequest")
                        .exceptionMessage("status 400 reading PaymentClient#createPayment(PaymentRequest)")
                        .location("com.mall.order.integration.PaymentClientProxy.createPayment(PaymentClientProxy.java:58)")
                        .stacktrace("feign.FeignException$BadRequest: [400] during [POST] to [https://payment.example.com/api/create] [PaymentClient#createPayment(PaymentRequest)]: [{\"code\":\"PAYMENT-001\",\"message\":\"payload missing customerId\"}]\n" +
                                "\tat feign.FeignException.errorStatus(FeignException.java:204)\n" +
                                "\tat feign.FeignException$ErrorStatus.decode(FeignException.java:283)\n" +
                                "\tat feign.codec.ErrorDecoder$Default.decode(ErrorDecoder.java:92)\n" +
                                "\tat com.mall.order.integration.PaymentClientProxy.createPayment(PaymentClientProxy.java:58)\n" +
                                "\tat com.mall.order.service.PaymentService.processPayment(PaymentService.java:112)")
                        .codeContext("53   public PaymentResponse createPayment(PaymentRequest request) {\n" +
                                "54       try {\n" +
                                "55           log.debug(\"Invoke payment, orderId={}\", request.getOrderId());\n" +
                                "56           return paymentClient.createPayment(request);\n" +
                                "57       } catch (FeignException ex) {\n" +
                                ">>> 58           throw new PaymentGatewayException(\"payment gateway returned error\", ex);\n" +
                                "59       }\n" +
                                "60   }\n")
                        .traceId("trace-prod-9afbc9917162")
                        .author(AiAnalysisPayload.Author.builder()
                                .name("Yun Zhao")
                                .email("yun.zhao@example.com")
                                .lastCommitTime("2025-01-09T13:27:55")
                                .fileName("PaymentClientProxy.java")
                                .lineNumber(58)
                                .commitMessage("feat: wrap feign exceptions with domain specific error")
                                .build())
                        .build();
            case "MessageQueue Deserialization Failure":
                return AiAnalysisPayload.builder()
                        .appName("recommendation-consumer")
                        .environment("staging")
                        .occurrenceTime("2025-01-13T19:21:44")
                        .exceptionType("com.fasterxml.jackson.databind.exc.ValueInstantiationException")
                        .exceptionMessage("Cannot construct instance of `RecommendationEvent` (missing creator property)")
                        .location("com.mall.recommendation.consumer.EventListener.onMessage(EventListener.java:71)")
                        .stacktrace("com.fasterxml.jackson.databind.exc.ValueInstantiationException: Cannot construct instance of `RecommendationEvent` (missing creator property 'userId')\n" +
                                "\tat com.fasterxml.jackson.databind.exc.ValueInstantiationException.from(ValueInstantiationException.java:47)\n" +
                                "\tat com.fasterxml.jackson.databind.DeserializationContext.handleMissingInstantiator(DeserializationContext.java:1353)\n" +
                                "\tat com.fasterxml.jackson.databind.deser.BeanDeserializerBase.deserializeFromObjectUsingNonDefault(BeanDeserializerBase.java:1413)\n" +
                                "\tat com.mall.recommendation.consumer.EventListener.onMessage(EventListener.java:71)")
                        .codeContext("65   public void onMessage(Message message, byte[] pattern) {\n" +
                                "66       String payload = new String(message.getBody(), StandardCharsets.UTF_8);\n" +
                                "67       log.debug(\"Received MQ message: {}\", payload);\n" +
                                "68       try {\n" +
                                "69           RecommendationEvent event = objectMapper.readValue(payload, RecommendationEvent.class);\n" +
                                "70           handler.handle(event);\n" +
                                ">>> 71       } catch (JsonProcessingException ex) {\n" +
                                "72           deadLetterPublisher.publish(payload, ex.getMessage());\n" +
                                "73       }\n" +
                                "74   }\n")
                        .traceId("trace-staging-1ce8a47f8c21")
                        .author(AiAnalysisPayload.Author.builder()
                                .name("Martin Lee")
                                .email("martin.lee@example.com")
                                .lastCommitTime("2025-01-08T18:05:03")
                                .fileName("EventListener.java")
                                .lineNumber(71)
                                .commitMessage("chore: add dead letter queue for parse failures")
                                .build())
                        .build();
            default:
                throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
    }

    private Map<String, Object> decodePayloadFromLink(String link) throws IOException {
        String encodedPayload = UriComponentsBuilder.fromUriString(link)
                .build()
                .getQueryParams()
                .getFirst(properties.getAi().getPayloadParam());
        assertNotNull(encodedPayload);
        return decodePayload(encodedPayload);
    }

    private Map<String, Object> decodePayload(String encodedPayload) throws IOException {
        byte[] compressed = Base64.getUrlDecoder().decode(encodedPayload);
        String json = decompressToString(compressed);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Map.class);
    }

    private String decompressToString(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
