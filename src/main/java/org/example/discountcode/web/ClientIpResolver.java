package org.example.discountcode.web;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpResolver {

    String resolveClientIp(HttpServletRequest request);
}
