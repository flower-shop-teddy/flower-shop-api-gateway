package com.flowershop.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lớp bảo vệ ở cửa vào. Không bật Eureka/identity: request hợp lệ đi tiếp sẽ nhận 503 (không có service đích),
 * nhưng thế là đủ để biết nó ĐÃ QUA được lớp security.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/jwks.json"
})
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    WebTestClient client;

    @Test
    void protectedRoute_withoutToken_returns401Json_withCorrelationId() {
        client.post().uri("/api/v1/flowers").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists("X-Correlation-Id")
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectBody()
                .jsonPath("$.code").isEqualTo("COM-401-001")
                .jsonPath("$.correlationId").exists();
    }

    @Test
    void malformedToken_returns401() {
        client.get().uri("/api/v1/customers/me").header("Authorization", "Bearer abc.def.ghi").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("COM-401-001");
    }

    @Test
    void publicRoutes_passSecurity() {
        assertPassesSecurity(client.get().uri("/api/v1/flowers").exchange().returnResult(Void.class).getStatus());
        assertPassesSecurity(client.post().uri("/api/v1/auth/login").exchange().returnResult(Void.class).getStatus());
        assertPassesSecurity(client.get().uri("/.well-known/jwks.json").exchange().returnResult(Void.class).getStatus());
    }

    private static void assertPassesSecurity(HttpStatusCode status) {
        assertThat(status.value()).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }
}
