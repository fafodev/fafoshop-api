# Quy Tắc Code

- Tiếng Việt bắt buộc cho nội dung mới (comment, message lỗi, Javadoc).
- Giữ kiến trúc vertical-slice (`dto/process/webservice`) — không tự ý tách
  Controller/Service/DAO kiểu Spring MVC thông thường.
- JDBC thuần qua `DBAccessor`/`DBStatement` — không thêm ORM (Hibernate,
  MyBatis, JPA...).
- **1 quy ước đặt tên DUY NHẤT cho toàn bộ schema**: bảng/cột DB
  `snake_case` (`product_code`, `sale_order_no`...), field Java camelCase
  tương ứng (`productCode`, `saleOrderNo`...).
- Mọi Process mới kế thừa `AbstractProcess`, khai `getFuncId()` nếu cần
  kiểm tra quyền (trả `null` nếu không cần) — trả về `function_code` tương
  ứng trong bảng `app_function`/`function_permission`.
- Mọi WebService mới kế thừa `AbstractWebService`, đặt `@Path` theo dạng
  `pos/<module>`, method con `@Path("/<action>")`.
- Đóng `ResultSet`/`DBStatement` trong `finally` — theo đúng pattern các
  Process mẫu (`ProductSearchProcess`, `ProductCreateProcess`).
- Không thêm `console.log`-tương-đương (`System.out.println` production) —
  dùng `logSend(LogLevel.xxx, ...)`.
- Không thêm dependency Maven khi chưa cần; đặc biệt không thêm
  JasperReports/EDI/Azure/POI/PDFBox hay các thư viện report/tích hợp nặng
  khác trừ khi được yêu cầu rõ ràng cho tính năng cụ thể.
- Cột `entry_program`/`update_program` chỉ rộng `VARCHAR(10)` — dùng mã
  chương trình rút gọn (hằng số `PRG_CD` trong Process), KHÔNG dùng
  `getClass().getSimpleName()` trực tiếp nếu tên class dài hơn 10 ký tự (đã
  từng gây lỗi `Data too long for column`).
- Mật khẩu luôn qua `PasswordUtility.hash()`/`verify()` — không lưu/so sánh
  plaintext.
- **Field DTO kế thừa `AbstractDto` (`@JsonInclude(Include.NON_NULL)`) mà
  giá trị Java là `null` sẽ bị LƯỢC HẲN khỏi JSON trả về, KHÔNG gửi
  `"field": null`.** Frontend nhận được `undefined` cho field đó, không phải
  `null` — nếu FE chỉ check `=== null` (không check cả `undefined`) sẽ crash
  khi gọi tiếp method trên giá trị (vd `.toLocaleString()`). Đã xảy ra thật 2
  lần với `SaleOrderRowDto.profitAmount`/`SaleOrderDetailItemDto.lineProfit`
  (màn Tra cứu bán hàng, xem `fafoshop/.claude/coding-rules.md` mục tương
  ứng). Thêm field nullable mới vào DTO kế thừa `AbstractDto` → PHẢI báo rõ
  cho phía frontend (hoặc tự kiểm tra code FE dùng field đó) là giá trị có
  thể `undefined`, không chỉ `null`.
