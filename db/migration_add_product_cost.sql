-- ============================================================================
-- migration: thêm product.cost + product_unit.unit_cost — chuyển giá vốn từ
-- "tính bình quân gia quyền theo inbound_receipt_item" (cách cũ) sang "cấu
-- hình trực tiếp trong Product Master, Nhập hàng đổi giá thì hỏi xác nhận rồi
-- ghi đè" (xem docs/pos-dong-bo-gia.md).
--
-- NULLABLE (không NOT NULL DEFAULT 0) — giữ đúng triết lý đã dùng xuyên suốt
-- dự án: NULL = "chưa cấu hình/chưa xác định", KHÔNG PHẢI "giá vốn = 0" (mirror
-- sale_order_item.unit_cost, ProductRowDto.currentAvgCost cũ). Sản phẩm mới
-- tạo/chưa từng nhập hàng lần nào hợp lệ ở trạng thái NULL.
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE product
  ADD COLUMN cost DECIMAL(12,2) NULL
  COMMENT 'Giá vốn hiện hành của đơn vị lẻ - cấu hình/cập nhật trực tiếp trong Product Master (Nhập hàng đổi giá thì hỏi xác nhận rồi ghi đè cột này), KHÔNG còn tính bình quân gia quyền từ inbound_receipt_item. NULL = chưa từng cấu hình/chưa nhập hàng lần nào.'
  AFTER price;

ALTER TABLE product_unit
  ADD COLUMN unit_cost DECIMAL(12,2) NULL
  COMMENT 'Giá vốn khi nhập theo đơn vị này - nhập tay riêng, KHÔNG ép theo tỉ lệ conversion_qty (thường có chiết khấu mua sỉ, giống unit_price). NULL = chưa cấu hình.'
  AFTER unit_price;
