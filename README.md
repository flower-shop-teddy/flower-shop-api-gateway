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

## Release và Docker image

Merge PR `dev → production` thì GitHub Actions publish image `ghcr.io/flower-shop-teddy/flower-shop-api-gateway:X.Y.Z` và tạo tag `vX.Y.Z`.
Chi tiết quy trình: README của repo `flower-shop-workspace`, mục "CI/CD và release".

```bash
docker run --rm -p 8080:8080 ghcr.io/flower-shop-teddy/flower-shop-api-gateway:<version>
```
