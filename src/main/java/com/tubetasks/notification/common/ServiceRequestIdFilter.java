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

/**
 * Resolves {@code X-Service-Request-ID} once per request for inter-service correlation.
 * Prefer an inbound header (from API Gateway or a caller); generate only when missing.
 * Echoes the same id on the response header for downstream service callers.
 * The API Gateway strips this header for end clients and keeps the id in the JSON body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceRequestIdFilter extends OncePerRequestFilter {

    public static final String SERVICE_REQUEST_ID_HEADER = "X-Service-Request-ID";
    public static final String SERVICE_REQUEST_ID = "serviceRequestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String serviceRequestId = request.getHeader(SERVICE_REQUEST_ID_HEADER);
        if (!StringUtils.hasText(serviceRequestId)) {
            serviceRequestId = UUID.randomUUID().toString();
        }
        MDC.put(SERVICE_REQUEST_ID, serviceRequestId);
        response.setHeader(SERVICE_REQUEST_ID_HEADER, serviceRequestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(SERVICE_REQUEST_ID);
        }
    }
}
