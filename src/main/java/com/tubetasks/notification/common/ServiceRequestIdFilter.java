package com.tubetasks.notification.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceRequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Service-Request-ID";
    public static final String MDC_KEY = "serviceRequestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String serviceRequestId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(serviceRequestId)) {
            serviceRequestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, serviceRequestId);
        response.setHeader(HEADER_NAME, serviceRequestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
