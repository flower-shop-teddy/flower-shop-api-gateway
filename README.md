# flower-shop-api-gateway

Cửa vào duy nhất của hệ thống (Spring Cloud Gateway, WebFlux). Định tuyến qua Eureka, gắn X-Correlation-Id cho mọi request.

Cổng local: `8080`

## Chạy

Cách khuyên dùng: chạy cả hệ thống từ repo `flower-shop-workspace` (`./clone-all.sh` rồi `./run-local.sh`).

Build riêng repo này:

```bash
mvn verify
```

Cần parent `com.flowershop:flower-shop-parent` từ repo `flower-shop-platform`: nằm sẵn ở `../platform` trong workspace,
hoặc đã `mvn install` vào `~/.m2`.
