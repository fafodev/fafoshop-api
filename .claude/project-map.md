# Bản Đồ Dự Án

Điểm đã xác minh:

- Package gốc: `fafoshop`.
- Hạ tầng dùng chung: `src/main/java/fafoshop/common`.
- Module nghiệp vụ: `src/main/java/fafoshop/pos/<module>` (`auth`, `product`,
  `category`, `supplier`, `saleorder`, `inboundreceipt`, `bankaccount`,
  `report`), mỗi module có `dto/`, `process/`, `webservice/` — xem vai trò
  từng module trong `retail-domain.md`. `report` (mới) chỉ có 1 action tổng
  hợp `dashboardSummary` cho màn Tổng quan `fafoshop` — xem
  `../../docs/pos-tong-quan-dashboard.md`.
- Bootstrap: `web.xml` (Jersey `ServletContainer`, quét package `fafoshop`,
  mount tại `/api/*`). Lúc dev cũng có thể chạy thẳng
  `fafoshop.FafoshopApplication` (Spring Boot embedded Tomcat, xem
  `task-workflows.md`).
- DB: MySQL **8.4** `fafoshop_pos` (`localhost:3307`), user riêng `fafoshop`
  (chỉ quyền SELECT/INSERT/UPDATE/DELETE trên `fafoshop_pos.*`). LƯU Ý: máy
  dev này có NHIỀU instance MySQL cài song song (5.7 ở cổng 3306 mặc định,
  8.4 ở cổng 3307) — luôn trỏ đúng cổng 3307 cho project này, không dùng
  3306.
- Cấu hình DB: `src/main/resources/db.properties` (mật khẩu mã hoá bằng
  `AES128AndBase64`, khoá tĩnh riêng của dự án).
- Bootstrap DB (tạo database/user lần đầu) dùng tài khoản admin chung của
  server MySQL 8.4 — thông tin nằm trong `db/mysql8.info` (đã gitignore,
  KHÔNG commit vì đây là quyền admin toàn server, không riêng project này).
- Schema DDL tham khảo: `db/schema.sql` (nguồn chuẩn khi cần tạo lại DB).
- Package manager: Maven (`pom.xml`), `mvn -o compile` để build offline.
- Frontend tương ứng: `D:\00.SOURCE_WEB\fafoshop` (Angular, gọi API này qua
  `/api/pos/...`) — ĐÃ nối dây thật cho POS (`pos.saleorder`, gồm cả màn tra
  cứu bán hàng `pos.saleorder.search`/`detail`/`export` — xem
  `docs/pos-tra-cuu-ban-hang.md`), Sản phẩm, Danh mục, Nhà cung cấp, Nhập
  hàng (`pos.inboundreceipt`), Tổng quan (`pos.report`); không còn dùng
  `alert()`/state cục bộ cho các màn này.
- `pos.saleorder.create` (`SaleOrderCreateProcess`) trừ tồn kho (`stock`)
  ngay khi bán — floor tại 0 nếu tồn chưa ghi nhận đủ, KHÔNG chặn bán hàng
  khi thiếu dữ liệu tồn kho lịch sử. Xem chi tiết + lý do
  `docs/pos-tong-quan-dashboard.md` mục 5.

UNKNOWN:

- Quy tắc thuế, làm tròn tiền.
- Quy tắc khuyến mãi chồng nhau.
- Công thức báo cáo doanh thu chi tiết (theo ca, theo nhân viên...).
- Giá vốn tồn kho (giá vốn bình quân/FIFO...).
