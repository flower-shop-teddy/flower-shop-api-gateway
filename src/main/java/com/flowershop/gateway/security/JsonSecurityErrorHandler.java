package com.flowershop.gateway.security;

import com.flowershop.gateway.filter.CorrelationIdWebFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Trả 401/403 cùng format ApiResponse như các service ({"code":"COM-401-001",...}).
 * Gateway chạy WebFlux nên không dùng được common-lib (servlet); chuỗi JSON tự ghép, chỉ chứa giá trị cố định
 * và correlation id đã kiểm tra định dạng, nên không có nguy cơ chèn JSON.
 */
public class JsonSecurityErrorHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonSecurityErrorHandler.class);

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        log.warn("Unauthenticated {} {}: {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(), ex.getClass().getSimpleName());
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return write(exchange, HttpStatus.UNAUTHORIZED, "COM-401-001",
                "Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return write(exchange, HttpStatus.FORBIDDEN, "COM-403-001", "Bạn không có quyền thực hiện thao tác này");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getAttribute(CorrelationIdWebFilter.HEADER);
        String json = "{\"code\":\"%s\",\"message\":\"%s\"%s,\"timestamp\":\"%s\"}".formatted(
                code, message,
                correlationId == null ? "" : ",\"correlationId\":\"" + correlationId + "\"",
                Instant.now());
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
