# Chỉ Dẫn Cho Claude Code

Đây là dự án Java (Jersey JAX-RS + JDBC thuần, không ORM) làm API cho hệ thống
bán lẻ/tạp hoá `fafoshop` (frontend Angular ở `D:\00.SOURCE_WEB\fafoshop`).

Kiến trúc theo hướng vertical-slice (`dto/process/webservice` trong 1 package
con), JDBC thuần qua `DBAccessor`/`DBStatement`, không dùng Spring MVC kiểu
Controller/Service/DAO thông thường — xem chi tiết trong `architecture.md`.
Xem `retail-domain.md` để biết vai trò từng bảng nghiệp vụ và những quy tắc
còn `UNKNOWN`.

## Luật Không Được Phá Vỡ

- **[ƯU TIÊN CAO NHẤT] Bảo mật không được thoả hiệp.** TUYỆT ĐỐI KHÔNG viết
  hoặc để lại code có lỗ hổng bảo mật đã biết — đặc biệt với backend JDBC
  thuần (không ORM) như dự án này:
  - **SQL Injection**: LUÔN dùng `PreparedStatement` qua `DBStatement` với
    placeholder `?` cho MỌI giá trị input (kể cả giá trị tưởng chừng "an
    toàn" như ID số). TUYỆT ĐỐI KHÔNG nối chuỗi giá trị input trực tiếp vào
    câu SQL bằng `+`/`String.format`. Nếu bắt buộc phải tham số hoá tên
    cột/bảng động (không thể dùng `?` cho identifier), PHẢI whitelist cứng
    (so khớp với danh sách hằng số cho phép trong code), không được ghép
    thẳng input người dùng vào identifier.
  - Không lưu/so sánh mật khẩu plaintext — LUÔN qua `PasswordUtility`.
  - Không bypass `AuthTokenFilter` bằng `@NoAuth` trừ khi thực sự là endpoint
    công khai.
  - Không trả lộ thông tin nhạy cảm (stack trace, cấu trúc DB, token nội bộ)
    trong response lỗi trả về client.
  - Không hardcode secret/credential DB vào code commit lên repo (đã có cơ
    chế `db.properties` + `AES128AndBase64` cho việc này).

  Phát hiện lỗ hổng ở code CŨ ngoài phạm vi nhiệm vụ đang làm cũng PHẢI báo
  ngay cho người dùng, không được im lặng bỏ qua vì "không nằm trong yêu cầu
  ban đầu". Dự án dự định deploy lên cloud — mọi lỗ hổng để lại đều có rủi ro
  thật, không phải rủi ro lý thuyết.
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
- `.claude/seqno-convention.md` — BẮT BUỘC đọc trước khi tạo module mới cần
  mã tự sinh (category_code/supplier_code/product_code và mọi mã tương
  tự sau này) — dùng `SeqNoUtility`, không tự chế cơ chế sinh mã riêng.

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

## Lỗi Encoding (Mojibake) — Kinh Nghiệm Thực Tế, Claude THƯỜNG MẮC

Sự cố thật: comment cột DB tiếng Việt (`product.reduced_tax_rate_flg`,
`promotion.discount_type`, `sale_order.void_flg`...) bị lưu sai thành
mojibake (`quy t?c c? th?...`) dù `db/schema.sql` nguồn vẫn đúng UTF-8 — do
lúc chạy DDL trước đây, charset kết nối MySQL client không phải UTF-8.

Khi viết/chạy 1 chương trình Java tạm (`javac`/`java`) trên máy dev này
(Windows + Git Bash + JDK 21) để thao tác dữ liệu tiếng Việt (đọc/ghi DB qua
JDBC, in ra để review...), PHẢI làm ĐỦ 3 việc, thiếu 1 là dữ liệu/hiển thị
sai:

1. **Biên dịch**: `javac -encoding UTF-8 ...` — dù JDK 18+ mặc định đọc
   source UTF-8, vẫn khai rõ để chắc chắn không phụ thuộc codepage hệ thống.
2. **Kết nối JDBC**: URL đã có sẵn `useUnicode=true&characterEncoding=UTF-8`
   trong `db.properties`/`DBAccessor` — giữ nguyên, không được bỏ khi tự viết
   script JDBC tạm ngoài `DBAccessor`.
3. **In ra console để review**: `java -Dstdout.encoding=UTF-8
   -Dstderr.encoding=UTF-8 ...` — trên Windows, `System.out` mặc định theo
   codepage console (không phải UTF-8) dù `-Dfile.encoding=UTF-8` đã bật,
   nên chữ có dấu in ra Git Bash sẽ thành `?`/ký tự vỡ dù dữ liệu Java/DB bên
   dưới HOÀN TOÀN đúng. Đây là lỗi hiển thị stream, KHÔNG được kết luận dữ
   liệu sai chỉ vì nhìn thấy `?` trên terminal — phải xác minh lại bằng cách
   ghi output ra file rồi đọc file đó bằng tool đọc UTF-8 đúng (vd `Read`)
   trước khi kết luận.

Ngược lại, cũng KHÔNG được mặc định coi mọi ký tự `?`/lỗi hiển thị là "chỉ
do terminal" rồi bỏ qua — như sự cố mojibake ở DB thật đã chứng minh, đôi
khi dữ liệu lưu thật sự sai. Luôn xác minh lại tận nguồn (query DB thật qua
kênh đảm bảo UTF-8, hoặc đọc lại file nguồn) thay vì tin vào 1 lần in ra
console.
