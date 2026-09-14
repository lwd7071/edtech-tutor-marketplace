package com.edtech.platform.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    @Test
    void addsRequestIdToEveryApplicationResponse() throws Exception {
        var filter = new RequestLoggingFilter();
        var request = new MockHttpServletRequest("GET", "/api/public/subjects");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    void preservesSafeIdAndRegeneratesUnsafeId() throws Exception {
        var filter = new RequestLoggingFilter();
        var request = new MockHttpServletRequest("GET", "/api/public/subjects");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "REQ-123:abc");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("REQ-123:abc");

        var unsafe = new MockHttpServletRequest("GET", "/api/public/subjects");
        unsafe.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "bad value");
        var unsafeResponse = new MockHttpServletResponse();
        filter.doFilter(unsafe, unsafeResponse, new MockFilterChain());
        assertThat(unsafeResponse.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).matches("[A-Za-z0-9-]{36}");
    }
}
