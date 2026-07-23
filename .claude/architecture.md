# Kiến Trúc

Jersey JAX-RS + JDBC thuần, vertical-slice theo module — xem phần "Quyết
Định Thiết Kế" cuối file cho các lựa chọn bảo mật/kiến trúc quan trọng.

## Luồng 1 request

```
Client (Angular POS)
  → AuthTokenFilter (@Provider, kiểm tra Bearer token, trừ @NoAuth)
  → XxxWebService (@Path, JAX-RS resource) — gọi executeProcess(request)
  → AbstractWebService.executeProcess() — gán accessInfo.userCode từ token đã
    xác thực, gọi process.execute(request)
  → AbstractProcess.execute() — mở DBAccessor, checkAuth() theo function_code,
    beforeProcess()/process()/afterProcess(), commit hoặc rollback
  → XxxProcess.process() — SQL thuần qua DBStatement, map ResultSet → DTO
  → trả AbstractResponse (JSON) — lỗi nghiệp vụ nằm trong
    lstNormalError/lstFatalError của body, lỗi hệ thống thật trả HTTP 500
    qua SystemExceptionMapper
```

## Cấu trúc 1 module (vertical-slice)

```
pos/<module>/
  dto/        — *Request, *Response, *RowDto (extends AbstractDto)
  process/    — *Process extends AbstractProcess, override process() +
                getFuncId() (permission) + createNewResponse()
  webservice/ — *WebService extends AbstractWebService, @Path, override
                getProcess()
```

Mỗi hành động nghiệp vụ (tìm kiếm, tạo, sửa...) là 1 bộ ba DTO/Process
riêng, thường tương ứng 1 WebService riêng (xem `pos/product` — có
`ProductSearchWebService` và `ProductCreateWebService` tách biệt, không gộp
chung 1 class).

## Hạ tầng dùng chung (`common/`)

- `database/DBAccessor` — kết nối MySQL, đọc `db.properties`, giải mã mật
  khẩu bằng `AES128AndBase64`, tự quản lý transaction (autoCommit=false).
- `database/DBStatement`/`DBCallableStatement` — wrapper PreparedStatement/
  CallableStatement, tự phát hiện deadlock (mã lỗi 1213) → `LoopException`.
- `process/AbstractProcess` — khung transaction + retry deadlock (tối đa 5
  lần) + `checkAuth()` theo `function_code` + gom lỗi vào response.
- `process/CheckAuthProcess` — kiểm tra `function_permission` (`user_code` ×
  `function_code`).
- `webservice/AbstractWebService` — cầu nối JAX-RS ↔ Process, gán
  `accessInfo.userCode` từ token đã xác thực.
- `auth/AuthTokenFilter` — `ContainerRequestFilter` kiểm tra token cho MỌI
  request (trừ `@NoAuth`), đọc từ cookie phiên (xem `SessionCookieUtility`),
  tra bảng `session_token`.
- `auth/SessionCookieUtility` — dựng chuỗi header `Set-Cookie` cho cookie
  phiên (`fafoshop_session`, `HttpOnly`+`Secure`+`SameSite=Strict`) — gom 1
  chỗ DUY NHẤT cho `AuthTokenFilter` (đọc), `AuthWebService` (set lúc
  login), `AuthLogoutWebService` (xoá lúc logout).
- `filter/CorsFilter` — cấu hình CORS cho frontend Angular (origin dev mặc
  định `http://localhost:4200`), CÓ bật
  `Access-Control-Allow-Credentials: true` (bắt buộc vì auth dùng cookie).
- `utility/PasswordUtility` — băm/kiểm mật khẩu (PBKDF2WithHmacSHA256).
- `utility/IdTokenUtility` — phát hành/kiểm tra/huỷ token (`generate()`/
  `verify()`/`revoke()`), lưu `session_token`.

## Xác thực (auth) — chi tiết cookie phiên

**Lịch sử quyết định (quan trọng)**: thiết kế ban đầu trả token qua JSON
body (`AuthLoginResponse.token`), client (Angular) tự lưu `localStorage` rồi
tự đính `Authorization: Bearer <token>`. Đây là lỗ hổng bảo mật — token lưu
`localStorage` bị JavaScript độc hại (XSS) đọc trộm được. Trước khi deploy
lên cloud, đã sửa lại thành cookie `HttpOnly`:

- `POST /pos/auth/login` (`AuthWebService`) — `AuthLoginProcess` vẫn phát
  hành token như cũ (`IdTokenUtility.generate()`), nhưng `AuthWebService`
  gắn token vào response qua header `Set-Cookie`
  (`SessionCookieUtility.buildSessionCookie()`, `Max-Age` = phút cấu hình
  trong `session.properties` × 60) rồi **gán `response.token = null` TRƯỚC
  KHI trả JSON** — token KHÔNG BAO GIỜ xuất hiện trong response body thật.
  Client xác định đăng nhập thành công bằng `fatalError` rỗng.
- `POST /pos/auth/logout` (`AuthLogoutWebService`, `@NoAuth`, tách WebService
  riêng theo đúng quy ước 1 action = 1 WebService) — đọc token từ
  `@CookieParam`, gọi `IdTokenUtility.revoke()` xoá `session_token` tương
  ứng (best-effort, token không hợp lệ vẫn coi là thành công), LUÔN trả
  `Set-Cookie` hết hạn ngay (`Max-Age=0`) để trình duyệt xoá cookie — kể cả
  khi cookie đã hết hạn/không có, vẫn phải trả cookie hết hạn (idempotent).
- `AuthTokenFilter` đọc token từ `Cookie: fafoshop_session=...` (JAX-RS
  `requestContext.getCookies()`), KHÔNG còn đọc header `Authorization`.
- `CorsFilter` bật `Access-Control-Allow-Credentials: true` — bắt buộc để
  trình duyệt gửi/nhận cookie cross-origin (dev: Angular `4200` gọi API
  `8080`). Khi bật credentials, `Access-Control-Allow-Origin` PHẢI là origin
  cụ thể (không được `*`) — `ALLOWED_ORIGIN` đã sẵn là chuỗi cụ thể.
- Giới hạn đã biết (không phải lỗ hổng, cần nhớ khi deploy): `SameSite=Strict`
  yêu cầu frontend + backend production cùng site (cùng eTLD+1) — domain
  thật vẫn `UNKNOWN`. `Secure` yêu cầu HTTPS trừ `localhost`.

## Quyết Định Thiết Kế

1. **Token xác thực được kiểm tra tự động qua `AuthTokenFilter`** (JAX-RS
   `ContainerRequestFilter`, áp dụng cho MỌI endpoint trừ `@NoAuth`) — đặt ở
   tầng infrastructure thay vì gọi tay trong từng Process, tránh trường hợp
   process mới quên gọi kiểm tra token.
2. **Mật khẩu luôn băm PBKDF2WithHmacSHA256** (`PasswordUtility`) — không
   bao giờ lưu/so sánh plaintext.
3. **Lỗi hệ thống thật trả HTTP 500** qua `SystemExceptionMapper`. CHỈ lỗi
   NGHIỆP VỤ (validate, thiếu quyền...) mới dùng cơ chế
   `lstNormalError`/`lstFatalError` (vẫn HTTP 200, vì client Angular POS cần
   hiển thị thông báo cụ thể) — lỗi hệ thống dùng status code chuẩn.
4. **Không có bảng trung gian ánh xạ PRCSID → FUNCID** — mỗi Process tự khai
   `getFuncId()` (trả về `function_code`), đơn giản, phù hợp quy mô 1 cửa
   hàng.
5. **Không đa tenant** — không có khái niệm nhiều khách hàng thuê hệ thống,
   fafoshop chỉ có 1 công ty/1 hệ thống nên không cần mã phân biệt tenant ở
   bất kỳ bảng/DTO nào.
6. **Đặt tên bảng/cột thống nhất `snake_case`** cho TOÀN BỘ schema — xem
   `retail-domain.md` để biết vai trò từng bảng.
