-- ============================================================================
-- migration: thêm line_amount + price cho inbound_receipt_item.
--
-- 1) line_amount — mirror ĐÚNG cơ chế đã có ở sale_order_item.line_amount
--    (xem migration_add_sale_order_item_unit_cost.sql/SaleOrderCreateProcess):
--    sửa lỗi làm tròn tiền khi nhập theo đơn vị đóng gói KHÔNG chia hết cho
--    số lượng lẻ quy đổi ra (vd Thùng x24, 500.000đ → unit_cost làm tròn
--    20.833đ/cái → 20.833×24=499.992đ, LỆCH 8đ so với 500.000đ đã thoả thuận
--    với NCC) — tech-debt-backlog.md mục 3, đã sửa bên sale_order/POS, CHƯA
--    sửa bên inbound_receipt. NULL = "Thành tiền" tính bình thường
--    unit_cost × quantity (dòng cũ trước migration, hoặc dòng KHÔNG qua dialog
--    chọn đơn vị).
--
-- 2) price — giá bán ÁP DỤNG lúc lập phiếu nhập (ghi đè product.price cùng
--    lúc, xem InboundReceiptCreateProcess.updateProductPrices) — TRƯỚC ĐÂY
--    hoàn toàn không lưu lại trên phiếu (chỉ là side-effect UPDATE product,
--    không có bản ghi nào giữ lại "lúc lập phiếu này giá bán là bao nhiêu"),
--    khiến dialog "Chi tiết phiếu nhập" không thể hiển thị được — khách hàng
--    phản ánh đúng chỗ này (fafomart_bug_report.md sheet BUG2 "Phiếu nhập đã
--    có nhập giá bán nhưng đây lại miss"). NULL = phiếu tạo TRƯỚC migration
--    này (không backfill — không có cơ sở biết giá bán áp dụng lúc đó).
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE inbound_receipt_item
  ADD COLUMN line_amount DECIMAL(12,2) NULL
  COMMENT 'Thành tiền CHÍNH XÁC của dòng (đơn giá vốn × số lượng, làm tròn ĐÚNG 1 lần khi thêm qua đơn vị đóng gói không chia hết) — NULL = tính bình thường unit_cost × quantity, mirror sale_order_item.line_amount.'
  AFTER unit_qty,
  ADD COLUMN price DECIMAL(12,2) NULL
  COMMENT 'Giá bán ĐÃ ÁP DỤNG lúc lập phiếu này (đồng thời ghi vào product.price) — lưu lại để xem lại đúng ở Chi tiết phiếu nhập, KHÔNG chỉ là side-effect thoáng qua như trước. NULL = phiếu tạo trước migration này, không backfill.'
  AFTER line_amount;
