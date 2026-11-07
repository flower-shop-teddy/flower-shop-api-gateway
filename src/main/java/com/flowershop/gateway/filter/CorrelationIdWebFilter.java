package com.flowershop.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cửa vào duy nhất của hệ thống:
 * 1. Gắn X-Correlation-Id cho mọi request (sinh mới nếu client không gửi hoặc gửi sai định dạng).
 * 2. Ghi access log: method, path, status, thời gian xử lý.
 *    KHÔNG log body, query string hay header Authorization vì có thể chứa dữ liệu nhạy cảm.
 *
 * Là WebFilter (không phải GlobalFilter của Gateway) và chạy TRƯỚC Spring Security:
 * GlobalFilter chỉ chạy khi request đã qua security và khớp route, nên response 401/403
 * do security trả về sẽ không có correlation id và không được ghi access log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (incoming != null && VALID_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, id))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, id);
        exchange.getAttributes().put(HEADER, id);

        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("[{}] {} {} -> {} ({} ms)",
                        id, method, path,
                        exchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - start));
    }
}
