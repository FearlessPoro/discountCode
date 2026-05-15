package org.example.discountcode.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class DefaultClientIpResolver implements ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
