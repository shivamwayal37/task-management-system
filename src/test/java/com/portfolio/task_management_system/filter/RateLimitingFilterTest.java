package com.portfolio.task_management_system.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RateLimitingFilterTest {

    @Test
    void returnsTooManyRequestsWhenIpExceedsBucketCapacity() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(1, Duration.ofMinutes(1), Duration.ofMinutes(15));
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest firstRequest = requestFrom("203.0.113.10");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        filter.doFilter(firstRequest, firstResponse, filterChain);

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(firstResponse.getHeader("X-Rate-Limit-Remaining")).isEqualTo("0");
        verify(filterChain, times(1)).doFilter(firstRequest, firstResponse);

        MockHttpServletRequest secondRequest = requestFrom("203.0.113.10");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        filter.doFilter(secondRequest, secondResponse, filterChain);

        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentType()).isEqualTo("application/json");
        assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
        assertThat(secondResponse.getContentAsString()).contains("Rate limit exceeded. Try again later.");
        verifyNoMoreInteractions(filterChain);
    }

    private MockHttpServletRequest requestFrom(String ipAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        request.setRemoteAddr(ipAddress);
        return request;
    }
}
