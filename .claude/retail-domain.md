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
| `stock` | Tồn kho | Theo dõi theo (`branch_code`, `product_code`) — không theo dõi vị trí kho vật lý chi tiết theo lô/hạn dùng riêng (1 dòng duy nhất mỗi sản phẩm/chi nhánh), phù hợp quy mô cửa hàng nhỏ. `expiry_date` bị GHI ĐÈ bằng lô nhập gần nhất mỗi lần nhập thêm (`InboundReceiptCreateProcess.upsertStock`) — không phải FEFO thật theo từng lô, là giới hạn đã biết của thiết kế đơn giản hoá này. |
| `inbound_receipt` + `inbound_receipt_item` | Nhập hàng (header + detail) — màn hình Nhập hàng, `pos.inboundreceipt` | Chỉ ghi nhận thực nhận, không có luồng lập kế hoạch nhập hàng (`planned_qty` = `actual_qty`). Lưu phiếu cộng thẳng vào `stock` + ghi đè `product.price` (giá bán sửa ngay trên lưới) trong CÙNG transaction, không qua bước duyệt riêng. Header có 5 cột `einvoice_*` lưu THAM CHIẾU hoá đơn điện tử NCC cung cấp (số hoá đơn/ký hiệu/ngày phát hành/mã tra cứu/link tra cứu) — tất cả optional, CHỈ lưu link, KHÔNG lưu file (chưa có hạ tầng lưu file). |
| `app_user` | Người dùng | `password_hash` lưu HASH (PBKDF2WithHmacSHA256), không lưu plaintext. |
| `app_function` + `function_permission` | Phân quyền theo chức năng | Mỗi Process tự khai `function_code` qua `getFuncId()`, không qua bảng trung gian nào. |
| `customer` | Khách hàng mua lẻ tại quầy | |
| `promotion` | Khuyến mãi | Chỉ khung — quy tắc áp dụng/chồng khuyến mãi: `UNKNOWN`. |
| `sale_order` + `sale_order_item` | Đơn bán tại quầy (checkout POS) | `sale_order.payment_method` (`CASH`/`TRANSFER`) ghi nhận phương thức thanh toán — xem `../../docs/pos-in-hoa-don.md`. |
| `bank_account` | Tài khoản NH nhận tiền theo chi nhánh, dùng build QR chuyển khoản lúc in bill | PK = `branch_code` (1 chi nhánh 1 TK). Xem `../../docs/pos-in-hoa-don.md`. |
| `session_token` | Lưu token phiên đăng nhập | Hạ tầng cho `AuthTokenFilter`. |
| `v_daily_revenue`, `v_item_revenue` | Báo cáo doanh thu | Chỉ khung tổng hợp cơ bản (tổng tiền, số lượng theo ngày/sản phẩm) — công thức chi tiết hơn: `UNKNOWN`. |

**Module đã có code Java hoàn chỉnh**: `pos.auth` (đăng nhập), `pos.product`
(Sản phẩm), `pos.category` (Danh mục), `pos.supplier` (Nhà cung cấp),
`pos.saleorder` (checkout POS), `pos.inboundreceipt` (Nhập hàng — chỉ có
action `create`, chưa có màn xem lại lịch sử phiếu nhập đã lập).

**Module CHƯA code**: xem, sửa, xoá phiếu nhập đã lập (`inboundreceipt` mới
chỉ có `create`); màn hình xem tồn kho (`stock`) riêng — hiện chỉ cộng dồn
ngầm qua Nhập hàng, chưa có API/màn hình đọc lại; báo cáo doanh thu chi tiết;
quản lý khách hàng/khuyến mãi/chi nhánh.

## UNKNOWN — không được tự phát minh

- Quy tắc thuế (VAT, hàng miễn thuế...).
- Quy tắc làm tròn tiền khi thanh toán.
- Giá vốn tồn kho (bình quân gia quyền, FIFO...).
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
