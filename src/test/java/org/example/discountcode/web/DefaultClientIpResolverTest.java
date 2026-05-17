package org.example.discountcode.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultClientIpResolverTest {

    private final DefaultClientIpResolver resolver = new DefaultClientIpResolver();

    @Test
    void usesFirstForwardedIpWhenHeaderHasMultipleValues() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 198.51.100.20");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRemoteAddressWhenForwardedHeaderIsMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("198.51.100.20");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("198.51.100.20");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void fallsBackToRemoteAddressWhenForwardedHeaderIsBlank(String forwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        when(request.getRemoteAddr()).thenReturn("198.51.100.20");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void trimsForwardedIpBeforeReturningIt() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" 203.0.113.10 , 198.51.100.20");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("203.0.113.10");
    }
}
