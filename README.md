# flower-shop-api-gateway

Cửa vào duy nhất của hệ thống (Spring Cloud Gateway, WebFlux). Định tuyến qua Eureka, gắn X-Correlation-Id cho mọi request,
kiểm tra JWT (lớp bảo vệ thứ nhất).

Cổng local: `8080`

## Bảo mật

- Public (không cần token): `POST /api/v1/auth/{login,refresh,logout,register}`, `GET /api/v1/flowers/**`, `GET /.well-known/jwks.json`, health.
- Còn lại phải có `Authorization: Bearer <JWT>` hợp lệ: đúng chữ ký (public key lấy từ JWKS của identity-service), còn hạn, đúng `iss`, `aud`.
- Gateway chỉ kiểm tra "đã đăng nhập chưa". Quyền chi tiết do từng service tự kiểm tra (zero trust), token được chuyển nguyên xuống service.
- 401/403 trả JSON cùng format `ApiResponse` (`COM-401-001`, `COM-403-001`), có `X-Correlation-Id`.

Biến môi trường: `IDENTITY_JWKS_URI` (mặc định `http://localhost:8083/.well-known/jwks.json`).

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
