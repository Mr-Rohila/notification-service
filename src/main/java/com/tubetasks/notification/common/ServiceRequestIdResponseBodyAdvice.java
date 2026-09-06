package com.tubetasks.notification.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ServiceRequestIdResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ServiceRequestIdResponseBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        String serviceRequestId = MDC.get(ServiceRequestIdFilter.SERVICE_REQUEST_ID);
        if (!StringUtils.hasText(serviceRequestId) || body == null) {
            return body;
        }
        if (body instanceof CharSequence || body instanceof byte[] || body instanceof Resource) {
            return body;
        }
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            copy.put(ServiceRequestIdFilter.SERVICE_REQUEST_ID, serviceRequestId);
            return copy;
        }
        if (body instanceof Collection<?> || body.getClass().isArray()) {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("data", body);
            envelope.put(ServiceRequestIdFilter.SERVICE_REQUEST_ID, serviceRequestId);
            return envelope;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> asMap = objectMapper.convertValue(body, Map.class);
            Map<String, Object> copy = new LinkedHashMap<>(asMap);
            copy.put(ServiceRequestIdFilter.SERVICE_REQUEST_ID, serviceRequestId);
            return copy;
        } catch (IllegalArgumentException ex) {
            return body;
        }
    }
}
