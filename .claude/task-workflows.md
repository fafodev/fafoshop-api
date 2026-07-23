# Workflow Tác Vụ

## Đọc

Đọc `.claude/architecture.md` và `.claude/retail-domain.md` trước, sau đó
đọc module tương tự gần nhất trong `pos/` (ví dụ `pos/product`) làm mẫu
trước khi tạo module mới.

## Lập Kế Hoạch

Nêu: bảng nào cần dùng/thêm (đúng quy ước snake_case thống nhất), mã chức
năng (`function_code`) cần khai báo, có cần sửa `db/schema.sql` không, quy
tắc nghiệp vụ nào còn `UNKNOWN`.

## Triển Khai

Theo đúng khuôn 1 module = `dto/` + `process/` + `webservice/`. Copy cấu
trúc từ `pos/product` làm điểm khởi đầu. Nội dung mới (comment, message lỗi)
phải tiếng Việt.

## Review

Ưu tiên: đóng `ResultSet`/`DBStatement` đúng chỗ, `getFuncId()` khai đúng
`function_code` đã seed trong `function_permission`, không lộ mật khẩu
plaintext, cột `entry_program`/`update_program` không vượt quá độ dài cột,
không phát minh nghiệp vụ đang `UNKNOWN`.

## Verify

- `mvn -o compile` để build nhanh (offline, dùng cache Maven local).
- Bỏ `-o` khi thêm dependency mới cần tải từ Maven Central.
- `mvn -o package` để đóng gói WAR đầy đủ khi cần kiểm tra kỹ hơn.
- Chưa có test tự động (JUnit) — verify thủ công theo 1 trong 3 cách:
  1. Gọi trực tiếp `XxxProcess.execute()` từ 1 class `main()` tạm (nhanh,
     nhưng KHÔNG đi qua tầng Jersey/AuthTokenFilter/Jackson).
  2. **Nhanh nhất, khuyến nghị hàng ngày**: chạy thẳng
     `src/main/java/fafoshop/FafoshopApplication.java` (Run As > Java
     Application trong Eclipse) — Spring Boot dựng Tomcat nhúng (embedded)
     tại `http://localhost:8080/api/...` trong ~2 giây, không cần cấu hình
     "Run on Server" của Eclipse. Spring CHỈ đóng vai trò servlet container
     (xem `JerseyConfig`), không dùng Spring DI cho nghiệp vụ — vẫn đi đúng
     tầng Jersey/`AuthTokenFilter`/`CorsFilter`/Jackson như deploy WAR thật.
     Nhớ dừng process (`Get-NetTCPConnection -LocalPort 8080` rồi
     `Stop-Process`) sau khi xong — Ctrl+C không đủ nếu chạy nền/qua IDE.
  3. Kỹ hơn khi cần sát 100% hành vi WAR/Tomcat thật: dùng
     `src/test/java/.../devserver/DevServerMain.java`
     — chạy `mvn test-compile`, lấy classpath test
     (`mvn -o dependency:build-classpath -Dmdep.outputFile=test-cp.txt
     -Dmdep.includeScope=test`), rồi `java -cp
     "target/classes;target/test-classes;<classpath>"
     fafoshop.devserver.DevServerMain` để dựng HTTP server nhúng (Grizzly)
     tại `http://localhost:8089/api/`, gọi thử bằng `curl` — đi qua ĐÚNG tầng
     HTTP/Jersey routing/`AuthTokenFilter`/Jackson JSON, sát với hành vi thật
     khi deploy WAR lên Tomcat. Dependency `jersey-container-grizzly2-http`
     chỉ ở scope `test`, KHÔNG đóng gói vào WAR (`jersey-hk2` thì NGƯỢC LẠI —
     bắt buộc phải ở scope mặc định vì Jersey cần nó để tự bootstrap kể cả
     khi deploy WAR thật, xem comment trong `pom.xml`). Nhớ dừng process
     (`Stop-Process` theo PID lấy từ `Get-NetTCPConnection -LocalPort 8089`)
     và xoá dữ liệu/token test sau khi xong.
