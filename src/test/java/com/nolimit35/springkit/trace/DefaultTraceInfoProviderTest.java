package com.nolimit35.springkit.trace;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DefaultTraceInfoProviderTest {

    @Mock
    private ExceptionNotifyProperties properties;

    @Mock
    private ExceptionNotifyProperties.Trace trace;

    @Mock
    private ExceptionNotifyProperties.TencentCls tencentCls;

    private DefaultTraceInfoProvider traceInfoProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getTrace()).thenReturn(trace);
        when(properties.getTencentcls()).thenReturn(tencentCls);
        when(trace.isEnabled()).thenReturn(true);
        when(trace.getHeaderName()).thenReturn("X-Trace-Id");

        traceInfoProvider = new DefaultTraceInfoProvider(properties);

        // Clear MDC before each test
        MDC.clear();
    }

    @Test
    void getTraceId_fromMDC() {
        // Given
        String expectedTraceId = "test-trace-id-from-mdc";
        MDC.put("traceId", expectedTraceId);

        // When
        String traceId = traceInfoProvider.getTraceId();

        // Then
        assertEquals(expectedTraceId, traceId);
    }

    @Test
    void getTraceId_fromRequestHeader() {
        // Given
        String expectedTraceId = "test-trace-id-from-header";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", expectedTraceId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // When
        String traceId = traceInfoProvider.getTraceId();

        // Then
        assertEquals(expectedTraceId, traceId);

        // Clean up
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getTraceId_traceDisabled() {
        // Given
        when(trace.isEnabled()).thenReturn(false);

        // When
        String traceId = traceInfoProvider.getTraceId();

        // Then
        assertNull(traceId);
    }

    @Test
    void generateTraceUrl_withValidConfig() {
        // Given
        String traceId = "test-trace-id";
        when(tencentCls.getRegion()).thenReturn("ap-guangzhou");
        when(tencentCls.getTopicId()).thenReturn("test-topic-id");

        // When
        String traceUrl = traceInfoProvider.generateTraceUrl(traceId);

        // Then
        assertNotNull(traceUrl);
        assertTrue(traceUrl.contains("region=ap-guangzhou"));
        assertTrue(traceUrl.contains("topic_id=test-topic-id"));
        assertTrue(traceUrl.contains("traceId"));
        assertTrue(traceUrl.contains("test-trace-id"));
    }

    @Test
    void generateTraceUrl_withNullTraceId() {
        // When
        String traceUrl = traceInfoProvider.generateTraceUrl(null);

        // Then
        assertNull(traceUrl);
    }

    @Test
    void generateTraceUrl_withEmptyTraceId() {
        // When
        String traceUrl = traceInfoProvider.generateTraceUrl("");

        // Then
        assertNull(traceUrl);
    }

    @Test
    void generateTraceUrl_withMissingConfig() {
        // Given
        String traceId = "test-trace-id";
        when(tencentCls.getRegion()).thenReturn(null);
        when(tencentCls.getTopicId()).thenReturn(null);

        // When
        String traceUrl = traceInfoProvider.generateTraceUrl(traceId);

        // Then
        assertNull(traceUrl);
    }
}
