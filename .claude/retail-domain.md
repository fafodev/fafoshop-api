# Nghiệp Vụ Bán Lẻ/Tạp Hoá

Toàn bộ bảng dùng CHUNG 1 quy ước đặt tên `snake_case` dễ đọc (xem
`coding-rules.md`).

## Bảng nghiệp vụ

| Bảng | Vai trò | Ghi chú |
|---|---|---|
| `product` | Sản phẩm | Có cột `price` (giá bán) cố định trên sản phẩm và `expiry_warning_days` (số ngày cảnh báo trước hạn sử dụng, mặc định 90 — dùng ở màn Nhập hàng để cảnh báo hạn dùng nhập vào quá gần). |
| `category` | Danh mục sản phẩm | Bảng mã dùng chung nhiều nghiệp vụ (`category_type` phân biệt, hiện chỉ có `PRODUCT`). |
| `supplier` | Nhà cung cấp | |
| `branch` | Cửa hàng/chi nhánh | |
| `stock` | Tồn kho | Theo dõi theo (`branch_code`, `product_code`) — không theo dõi vị trí kho vật lý chi tiết theo lô/hạn dùng riêng (1 dòng duy nhất mỗi sản phẩm/chi nhánh), phù hợp quy mô cửa hàng nhỏ. `expiry_date` bị GHI ĐÈ bằng lô nhập gần nhất mỗi lần nhập thêm (`InboundReceiptCreateProcess.upsertStock`) — không phải FEFO thật theo từng lô, là giới hạn đã biết của thiết kế đơn giản hoá này. CỘNG khi nhập (`InboundReceiptCreateProcess`), TRỪ khi bán (`SaleOrderCreateProcess.decrementStock` — floor tại 0, không chặn bán khi thiếu dữ liệu tồn kho, xem `../../docs/pos-tong-quan-dashboard.md` mục 5). POS KHÔNG kiểm tra tồn khả dụng trước khi cho bán. |
| `inbound_receipt` + `inbound_receipt_item` | Nhập hàng (header + detail) — màn hình Nhập hàng, `pos.inboundreceipt` | Chỉ ghi nhận thực nhận, không có luồng lập kế hoạch nhập hàng (`planned_qty` = `actual_qty`). Lưu phiếu cộng thẳng vào `stock` + ghi đè `product.price` (giá bán sửa ngay trên lưới) trong CÙNG transaction, không qua bước duyệt riêng. Header có 5 cột `einvoice_*` lưu THAM CHIẾU hoá đơn điện tử NCC cung cấp (số hoá đơn/ký hiệu/ngày phát hành/mã tra cứu/link tra cứu) — tất cả optional, CHỈ lưu link, KHÔNG lưu file (chưa có hạ tầng lưu file). |
| `app_user` | Người dùng | `password_hash` lưu HASH (PBKDF2WithHmacSHA256), không lưu plaintext. |
| `app_function` + `function_permission` | Phân quyền theo chức năng | Mỗi Process tự khai `function_code` qua `getFuncId()`, không qua bảng trung gian nào. |
| `customer` | Khách hàng mua lẻ tại quầy | |
| `promotion` | Khuyến mãi | Chỉ khung — quy tắc áp dụng/chồng khuyến mãi: `UNKNOWN`. |
| `sale_order` + `sale_order_item` | Đơn bán tại quầy (checkout POS) | `sale_order.payment_method` (`CASH`/`TRANSFER`) ghi nhận phương thức thanh toán — xem `../../docs/pos-in-hoa-don.md`. Có màn tra cứu (`pos.saleorder.search`/`detail`/`export`) — xem `../../docs/pos-tra-cuu-ban-hang.md`. `sale_order_item.unit_cost` (mới, xem mục Giá vốn bên dưới) chụp giá vốn TẠI THỜI ĐIỂM bán, dùng tính "tiền lãi" trên màn tra cứu. |
| `bank_account` | Tài khoản NH nhận tiền theo chi nhánh, dùng build QR chuyển khoản lúc in bill | PK = `branch_code` (1 chi nhánh 1 TK). Xem `../../docs/pos-in-hoa-don.md`. |
| `session_token` | Lưu token phiên đăng nhập | Hạ tầng cho `AuthTokenFilter`. |
| `v_daily_revenue`, `v_item_revenue` | Báo cáo doanh thu | Chỉ khung tổng hợp cơ bản (tổng tiền, số lượng theo ngày/sản phẩm) — công thức chi tiết hơn: `UNKNOWN`. Từ `pos.report` (mới), 2 view này được `DashboardSummaryProcess` đọc cho màn Tổng quan — trước đó tồn tại trong schema nhưng chưa có process/webservice nào dùng tới. |

**Module đã có code Java hoàn chỉnh**: `pos.auth` (đăng nhập), `pos.product`
(Sản phẩm), `pos.category` (Danh mục), `pos.supplier` (Nhà cung cấp),
`pos.saleorder` (checkout POS, có trừ tồn kho, sửa PTTT, VÀ tra cứu
search/detail/export — xem `../../docs/pos-tra-cuu-ban-hang.md`),
`pos.inboundreceipt` (Nhập hàng — chỉ có action `create`, chưa có màn xem
lại lịch sử phiếu nhập đã lập), `pos.report` (Tổng quan — chỉ 1 action
`dashboardSummary`, xem `../../docs/pos-tong-quan-dashboard.md`).

**Module CHƯA code**: xem, sửa, xoá phiếu nhập đã lập (`inboundreceipt` mới
chỉ có `create`); xoá/huỷ đơn bán (`saleorder` có tra cứu nhưng CHƯA có
action huỷ — xem chi tiết `void_flg` ở `docs/pos-tra-cuu-ban-hang.md`); màn
hình xem/quản lý tồn kho (`stock`) ĐẦY ĐỦ —
`pos.report.dashboardSummary` chỉ trả rút gọn 5 dòng tồn thấp + 5 dòng sắp
hết hạn cho màn Tổng quan, chưa có màn liệt kê toàn bộ tồn kho; kiểm tra
tồn kho khả dụng lúc bán (POS hiện cho bán bất kể tồn kho); báo cáo doanh
thu chi tiết; quản lý khách hàng/khuyến mãi/chi nhánh.

## Giá vốn & tiền lãi — ĐÃ CHỐT (không còn UNKNOWN)

Trước đây "giá vốn tồn kho" là UNKNOWN. Người dùng đã CHỐT công thức khi
yêu cầu thêm cột "tiền lãi" ở màn tra cứu bán hàng:

- **Giá vốn = bình quân gia quyền TẤT CẢ phiếu nhập của sản phẩm tính đến
  hiện tại** (`SUM(actual_qty*unit_cost)/SUM(actual_qty)` trên
  `inbound_receipt_item`, lọc theo `branch_code`) — KHÔNG phải FIFO.
- **CHỤP LẠI (snapshot) NGAY LÚC TẠO ĐƠN BÁN**, lưu vào
  `sale_order_item.unit_cost` (xem
  `SaleOrderCreateProcess.queryWeightedAvgUnitCost`) — giống cách
  `unit_price` được chụp lại, KHÔNG tính lại giá vốn khi xem báo cáo sau
  này (giá nhập mới hơn KHÔNG làm thay đổi lãi của đơn đã bán trong quá
  khứ).
- `unit_cost` NULL (KHÔNG phải 0) nếu sản phẩm CHƯA TỪNG có phiếu nhập tính
  đến lúc bán. Đơn bán tạo TRƯỚC migration
  `db/migration_add_sale_order_item_unit_cost.sql` cũng NULL — KHÔNG
  backfill tự động (không có cơ sở tính đúng giá vốn tại đúng thời điểm đã
  bán trong quá khứ).
- Tiền lãi 1 đơn = NULL (không phải 0) nếu BẤT KỲ dòng hàng nào có
  `unit_cost` NULL — tránh hiển thị số liệu thiếu 1 phần chi phí mà không
  cảnh báo. Xem `SaleOrderQueryHelper.PROFIT_SUBQUERY_SQL`.
- Vẫn CHƯA làm "Tổng giá trị tồn kho" (giá vốn × tồn kho hiện tại) — khác
  mục đích với "lãi từng đơn bán", chưa có yêu cầu cụ thể.

Chi tiết đầy đủ: `../../docs/pos-tra-cuu-ban-hang.md` mục giá vốn/lãi.

## UNKNOWN — không được tự phát minh

- Quy tắc thuế (VAT, hàng miễn thuế...).
- Quy tắc làm tròn tiền khi thanh toán.
- Quy tắc khuyến mãi chồng nhau, điều kiện áp dụng khuyến mãi.
- Công thức báo cáo doanh thu chi tiết (theo ca làm việc, theo nhân viên,
  trừ hàng trả lại...) — `v_daily_revenue`/`v_item_revenue` chỉ là khung
  tổng hợp cơ bản (tổng tiền, số lượng theo ngày/sản phẩm).
- ~~Mẫu hoá đơn in cho khách~~ — ĐÃ CÓ thiết kế, xem `../../docs/pos-in-hoa-don.md`
  (không phải UNKNOWN nữa, nhưng vẫn phải đọc tài liệu đó trước khi đổi hành
  vi in ấn).
- Ma trận phân quyền chi tiết (ngoài 2 mã chức năng mẫu `PRDCT_VIEW`/
  `PRDCT_EDIT` đã seed để demo).

Các phần trên phải ghi rõ `UNKNOWN` trong code/tài liệu mới cho tới khi có
yêu cầu nghiệp vụ cụ thể.
