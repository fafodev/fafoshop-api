# Nghiệp Vụ Bán Lẻ/Tạp Hoá

Toàn bộ bảng dùng CHUNG 1 quy ước đặt tên `snake_case` dễ đọc (xem
`coding-rules.md`).

## Bảng nghiệp vụ

| Bảng | Vai trò | Ghi chú |
|---|---|---|
| `product` | Sản phẩm | Có cột `price` (giá bán) cố định trên sản phẩm. |
| `supplier` | Nhà cung cấp | |
| `branch` | Cửa hàng/chi nhánh | |
| `stock` | Tồn kho | Theo dõi theo (`branch_code`, `product_code`) — không theo dõi vị trí kho vật lý chi tiết, phù hợp quy mô cửa hàng nhỏ. |
| `inbound_receipt` + `inbound_receipt_item` | Nhập hàng (header + detail) | Chỉ ghi nhận thực nhận, không có luồng lập kế hoạch nhập hàng. |
| `app_user` | Người dùng | `password_hash` lưu HASH (PBKDF2WithHmacSHA256), không lưu plaintext. |
| `app_function` + `function_permission` | Phân quyền theo chức năng | Mỗi Process tự khai `function_code` qua `getFuncId()`, không qua bảng trung gian nào. |
| `customer` | Khách hàng mua lẻ tại quầy | |
| `promotion` | Khuyến mãi | Chỉ khung — quy tắc áp dụng/chồng khuyến mãi: `UNKNOWN`. |
| `sale_order` + `sale_order_item` | Đơn bán tại quầy (checkout POS) | |
| `session_token` | Lưu token phiên đăng nhập | Hạ tầng cho `AuthTokenFilter`. |
| `v_daily_revenue`, `v_item_revenue` | Báo cáo doanh thu | Chỉ khung tổng hợp cơ bản (tổng tiền, số lượng theo ngày/sản phẩm) — công thức chi tiết hơn: `UNKNOWN`. |

**Module CHƯA code (Đợt sau)**: Nhập hàng, Nhà cung cấp mới chỉ có DB schema,
chưa có Process/WebService — chỉ mới `pos.product` (Sản phẩm) và `pos.auth`
(đăng nhập) có code Java hoàn chỉnh.

## UNKNOWN — không được tự phát minh

- Quy tắc thuế (VAT, hàng miễn thuế...).
- Quy tắc làm tròn tiền khi thanh toán.
- Giá vốn tồn kho (bình quân gia quyền, FIFO...).
- Quy tắc khuyến mãi chồng nhau, điều kiện áp dụng khuyến mãi.
- Công thức báo cáo doanh thu chi tiết (theo ca làm việc, theo nhân viên,
  trừ hàng trả lại...) — `v_daily_revenue`/`v_item_revenue` chỉ là khung
  tổng hợp cơ bản (tổng tiền, số lượng theo ngày/sản phẩm).
- Mẫu hoá đơn in cho khách.
- Ma trận phân quyền chi tiết (ngoài 2 mã chức năng mẫu `PRDCT_VIEW`/
  `PRDCT_EDIT` đã seed để demo).

Các phần trên phải ghi rõ `UNKNOWN` trong code/tài liệu mới cho tới khi có
yêu cầu nghiệp vụ cụ thể.
