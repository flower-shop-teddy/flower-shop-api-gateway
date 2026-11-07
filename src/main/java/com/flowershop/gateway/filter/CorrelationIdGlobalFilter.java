package com.flowershop.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cửa vào duy nhất của hệ thống:
 * 1. Gắn X-Correlation-Id cho mọi request (sinh mới nếu client không gửi hoặc gửi sai định dạng).
 * 2. Ghi access log: method, path, status, thời gian xử lý.
 *    KHÔNG log body hay query string vì có thể chứa dữ liệu nhạy cảm.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (incoming != null && VALID_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, id))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, id);

        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("[{}] {} {} -> {} ({} ms)",
                        id, method, path,
                        exchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - start));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
