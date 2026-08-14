-- ============================================================================
-- migration: thêm cột sale_order_item.unit_cost — chụp lại giá vốn BÌNH QUÂN
-- GIA QUYỀN của sản phẩm tại THỜI ĐIỂM tạo đơn bán (tính từ
-- SUM(actual_qty*unit_cost)/SUM(actual_qty) trên inbound_receipt_item theo
-- branch_code, xem SaleOrderCreateProcess), phục vụ tính "tiền lãi" ở màn
-- Tra cứu bán hàng — xem docs/pos-tra-cuu-ban-hang.md. Trước đây "giá vốn
-- tồn kho" là UNKNOWN trong retail-domain.md; quyết định công thức này đã
-- CHỐT theo yêu cầu người dùng, ghi lại trong retail-domain.md.
--
-- NULL = sản phẩm CHƯA TỪNG có phiếu nhập nào tính đến lúc bán (không xác
-- định được giá vốn, KHÔNG phải giá vốn = 0) — đơn tạo TRƯỚC migration này
-- cũng NULL, không backfill tự động (dữ liệu lịch sử không có cơ sở tính lại
-- đúng giá vốn tại đúng thời điểm đã bán).
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE sale_order_item
  ADD COLUMN unit_cost DECIMAL(12,2) NULL
  COMMENT 'Giá vốn BÌNH QUÂN GIA QUYỀN của sản phẩm tại THỜI ĐIỂM bán (chụp lại lúc tạo đơn, xem SaleOrderCreateProcess). NULL = sản phẩm CHƯA TỪNG có phiếu nhập nào tính đến lúc bán, KHÔNG xác định được giá vốn (không phải giá vốn = 0) — đơn tạo TRƯỚC khi field này ra đời cũng NULL, không backfill tự động.'
  AFTER line_amount;
