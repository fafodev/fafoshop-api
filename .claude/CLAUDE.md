# Chỉ Dẫn Cho Claude Code

Đây là dự án Java (Jersey JAX-RS + JDBC thuần, không ORM) làm API cho hệ thống
bán lẻ/tạp hoá `fafoshop` (frontend Angular ở `D:\00.SOURCE_WEB\fafoshop`).

Kiến trúc theo hướng vertical-slice (`dto/process/webservice` trong 1 package
con), JDBC thuần qua `DBAccessor`/`DBStatement`, không dùng Spring MVC kiểu
Controller/Service/DAO thông thường — xem chi tiết trong `architecture.md`.
Xem `retail-domain.md` để biết vai trò từng bảng nghiệp vụ và những quy tắc
còn `UNKNOWN`.

## Luật Không Được Phá Vỡ

- Toàn bộ nội dung Claude tạo trong repository (comment, Javadoc, tài liệu,
  tên hiển thị, message lỗi trả về client qua `errMsg`/`systemerror.properties`)
  phải dùng tiếng Việt.
- **NGOẠI LỆ**: nội dung log runtime/audit — chuỗi truyền vào
  `logSend(LogLevel.xxx, "...")` và tham số message của `Log`
  (`log.debug/info/warn/error/fatal`) — dùng **tiếng Anh**, ngắn gọn kiểu
  `Module:Action` (vd `"Process:Start (...)"`, `"Process:Finish (...)"`,
  `"Commit:Success"`, `"Deadlock:Retry " + n`). Đây là log nội bộ, khác với
  message lỗi trả cho client (vẫn tiếng Việt).
- Thuật ngữ tiếng Anh phải mở ngoặc giải thích tiếng Việt, trừ định danh kỹ
  thuật (tên bảng, tên cột, tên class, tên package, lệnh Maven...).
- **Toàn bộ schema DB dùng 1 quy ước đặt tên DUY NHẤT**: tên bảng/cột
  `snake_case` tiếng Anh dễ đọc (`product`, `product_code`, `branch_code`...).
- Field Java tương ứng dùng camelCase (`productCode`, `branchCode`...).
- Không phát minh API/nghiệp vụ mà `retail-domain.md` đánh dấu `UNKNOWN`.

## File Phải Đọc Trước Khi Làm

- `.claude/project-map.md`
- `.claude/architecture.md`
- `.claude/coding-rules.md`
- `.claude/retail-domain.md`
- `.claude/task-workflows.md`
- `.claude/review-checklist.md`

## Quy Tắc Cốt Lõi

- Giữ kiến trúc **vertical-slice theo module** (`dto/process/webservice`
  trong 1 package con), KHÔNG tách Controller/Service/DAO như Spring MVC
  thông thường — đây là quyết định kiến trúc có chủ đích của dự án.
- JDBC thuần qua `DBAccessor`/`DBStatement` — không thêm Hibernate/MyBatis
  hay ORM nào khác.
- Mọi Process kế thừa `AbstractProcess`, mọi WebService kế thừa
  `AbstractWebService` — không tự viết luồng transaction/commit/rollback
  riêng ngoài khung có sẵn.
- Mọi bảng phải có cột audit (`entry_user_code/entry_datetime/entry_program`,
  `update_user_code/update_datetime/update_program`) và cột xoá mềm
  (`del_flg`) nếu bảng đó cần xoá mềm (bảng dữ liệu giao dịch như `stock`,
  `inbound_receipt_item` không cần `del_flg`).
- Không thêm dependency Maven khi chưa cần (đặc biệt: không thêm
  JasperReports/EDI/Azure/POI hay các thư viện report/tích hợp nặng khác trừ
  khi được yêu cầu rõ cho tính năng cụ thể).
- Mật khẩu người dùng LUÔN băm bằng `PasswordUtility` (PBKDF2), không bao
  giờ lưu/so sánh plaintext.
- Mọi endpoint mới (trừ đăng nhập) đều đi qua `AuthTokenFilter` — không tự
  bypass bằng cách thêm `@NoAuth` khi không thực sự cần.
- Cột `entry_program`/`update_program` chỉ rộng `VARCHAR(10)` — dùng mã
  chương trình rút gọn (hằng số `PRG_CD` trong Process khi tên class dài hơn
  10 ký tự), KHÔNG dùng `getClass().getSimpleName()` trực tiếp nếu không
  chắc độ dài.
- Ưu tiên `mvn -o compile` để verify (kiểm chứng) nhanh khi không cần tải
  dependency mới; bỏ `-o` khi cần dependency mới từ Maven Central.
