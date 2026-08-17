-- ============================================================================
-- migration: thêm unit_name/unit_qty cho sale_order_item VÀ inbound_receipt_item
-- — lưu lại ĐÚNG đơn vị đóng gói (Lốc/Thùng...) người dùng đã chọn lúc thao
-- tác (qua UnitChooserDialogComponent, xem docs/pos-da-don-vi-tinh.md), thay
-- vì chỉ lưu số lượng đã quy đổi ra đơn vị lẻ như trước đây.
--
-- Bối cảnh: khách hàng phản ánh (fafomart_bug_report.md, sheet BUG1/BUG3)
-- chọn "3 Vỉ" ở màn Nhập hàng nhưng lưới/phiếu chỉ hiện số đã quy đổi ra lẻ
-- (vd "6"), kể cả khi xem lại phiếu/đơn ĐÃ LƯU — không có cách nào biết lại
-- đã dùng đơn vị gì. `quantity` (đã có sẵn) VẪN LUÔN là nguồn sự thật DUY
-- NHẤT cho tồn kho/tính toán (giữ đúng quyết định "1 kho duy nhất tính theo
-- đơn vị lẻ" đã chốt) — 2 cột mới CHỈ phục vụ hiển thị lại đúng lựa chọn ban
-- đầu của người dùng, không ảnh hưởng nghiệp vụ tồn kho/giá vốn/doanh thu.
--
-- NULL = dòng hàng dùng đơn vị LẺ (mặc định, không chọn đơn vị đóng gói nào)
-- — bao gồm mọi dòng hàng có sẵn TRƯỚC migration này, giữ nguyên hành vi
-- hiển thị cũ (chỉ hiện số lượng theo lẻ), không cần backfill.
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE sale_order_item
  ADD COLUMN unit_name VARCHAR(20) NULL
  COMMENT 'Tên đơn vị đóng gói đã chọn lúc bán (vd "Lốc") — NULL = đơn vị lẻ. CHỈ phục vụ hiển thị lại, KHÔNG dùng tính toán (quantity đã quy đổi ra lẻ mới là nguồn sự thật).'
  AFTER unit_cost,
  ADD COLUMN unit_qty INT(9) NULL
  COMMENT 'Số lượng theo ĐÚNG đơn vị đã chọn (vd 3, nếu unit_name="Lốc") — NULL khi unit_name NULL. CHỈ phục vụ hiển thị lại, quantity vẫn là số lượng LẺ thật dùng tính tồn kho.'
  AFTER unit_name;

ALTER TABLE inbound_receipt_item
  ADD COLUMN unit_name VARCHAR(20) NULL
  COMMENT 'Tên đơn vị đóng gói đã chọn lúc nhập (vd "Lốc") — NULL = đơn vị lẻ. CHỈ phục vụ hiển thị lại, KHÔNG dùng tính toán (quantity đã quy đổi ra lẻ mới là nguồn sự thật).'
  AFTER unit_cost,
  ADD COLUMN unit_qty INT(9) NULL
  COMMENT 'Số lượng theo ĐÚNG đơn vị đã chọn (vd 3, nếu unit_name="Lốc") — NULL khi unit_name NULL. CHỈ phục vụ hiển thị lại, quantity vẫn là số lượng LẺ thật dùng tính tồn kho.'
  AFTER unit_name;
