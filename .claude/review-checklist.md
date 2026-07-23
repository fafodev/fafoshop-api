# Checklist Review

- **Không có lỗ hổng bảo mật đã biết** — ưu tiên kiểm tra mục này TRƯỚC các
  mục còn lại: mọi câu SQL dùng `?` placeholder qua `DBStatement` (không nối
  chuỗi input), không lưu/so sánh mật khẩu plaintext, không bypass
  `AuthTokenFilter` bằng `@NoAuth` khi không thực sự cần, không lộ thông tin
  nhạy cảm trong response lỗi, không hardcode secret. Xem "Luật Không Được
  Phá Vỡ" đầu `CLAUDE.md`.
- Nội dung mới dùng tiếng Việt (comment, message lỗi, Javadoc).
- Bảng/cột dùng thống nhất `snake_case`, có đủ cột audit
  (`entry_user_code/entry_datetime/entry_program`,
  `update_user_code/update_datetime/update_program`) và `del_flg` nếu bảng
  cần xoá mềm.
- Process mới kế thừa `AbstractProcess`, WebService mới kế thừa
  `AbstractWebService`.
- `getFuncId()` khai đúng, đã seed `app_function`/`function_permission`
  tương ứng (nếu không, mọi request sẽ bị chặn quyền).
- `ResultSet`/`DBStatement` đóng trong `finally`.
- Không so sánh/lưu mật khẩu plaintext — dùng `PasswordUtility`.
- Không bypass `AuthTokenFilter` bằng `@NoAuth` trừ khi thực sự là endpoint
  công khai (đăng nhập).
- `entry_program`/`update_program` không vượt quá `VARCHAR(10)`.
- Không phát minh nghiệp vụ đang đánh dấu `UNKNOWN` trong
  `retail-domain.md`.
- Không thêm dependency Maven không cần thiết.
- Đã chạy `mvn -o compile` (hoặc `package`) và báo cáo kết quả.
