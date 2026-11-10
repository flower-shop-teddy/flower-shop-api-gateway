package com.flowershop.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Lớp kiểm tra thứ NHẤT: token sai chữ ký / hết hạn / sai iss, aud bị chặn ngay ở gateway,
 * không tốn tài nguyên của service phía sau.
 *
 * Gateway chỉ kiểm tra "đã đăng nhập hợp lệ chưa". Quyền chi tiết (flower:write, customer:read...)
 * do từng service tự kiểm tra, vì chỉ service mới hiểu nghiệp vụ của nó.
 * Header Authorization được chuyển nguyên vẹn xuống service (token relay).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler();
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(ex -> ex
                        // API nội bộ giữa các service (vd /internal/v1/service-token): không bao giờ đi qua cửa vào,
                        // kể cả có token hợp lệ. Hiện cũng không có route, luật này chặn trước nếu sau này lỡ thêm route rộng.
                        .pathMatchers("/internal/**").denyAll()
                        // Đăng nhập, đăng ký... thì làm gì đã có token
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout", "/api/v1/auth/register").permitAll()
                        .pathMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                        // Khách chưa đăng nhập vẫn xem được danh mục hoa
                        .pathMatchers(HttpMethod.GET, "/api/v1/flowers", "/api/v1/flowers/**").permitAll()
                        // ...và xem sản phẩm còn hàng không (chỉ còn/hết, không lộ số lượng)
                        .pathMatchers(HttpMethod.GET, "/api/v1/inventory/availability").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> { })
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }
}
