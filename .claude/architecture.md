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
- `auth/AuthTokenFilter` — `ContainerRequestFilter` kiểm tra Bearer token
  cho MỌI request (trừ `@NoAuth`), tra bảng `session_token`.
- `filter/CorsFilter` — cấu hình CORS cho frontend Angular (origin dev mặc
  định `http://localhost:4200`).
- `utility/PasswordUtility` — băm/kiểm mật khẩu (PBKDF2WithHmacSHA256).
- `utility/IdTokenUtility` — phát hành/kiểm tra token, lưu `session_token`.

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
