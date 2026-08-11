-- ============================================================================
-- Migration: thêm 5 cột lưu THÔNG TIN THAM CHIẾU hoá đơn điện tử (HĐĐT) do
-- NCC cung cấp vào inbound_receipt — CHỈ lưu link tra cứu + định danh hoá
-- đơn, KHÔNG lưu file (chưa có hạ tầng lưu file, xem trao đổi thiết kế màn
-- Nhập hàng). Tất cả NULL được vì không phải phiếu nhập nào cũng có HĐĐT
-- (vd phiếu nạp tồn kho đầu kỳ). User 'fafoshop' chỉ có quyền
-- SELECT/INSERT/UPDATE/DELETE, không có ALTER — chạy file này bằng tài khoản
-- admin MySQL 8.4 (cổng 3307).
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE inbound_receipt
  ADD COLUMN einvoice_no VARCHAR(20) NULL
    COMMENT 'Số hoá đơn điện tử do NCC cung cấp (nếu có)'
    AFTER note,
  ADD COLUMN einvoice_series VARCHAR(20) NULL
    COMMENT 'Ký hiệu mẫu số hoá đơn điện tử (nếu có)'
    AFTER einvoice_no,
  ADD COLUMN einvoice_issue_date DATE NULL
    COMMENT 'Ngày phát hành hoá đơn điện tử (nếu có)'
    AFTER einvoice_series,
  ADD COLUMN einvoice_lookup_code VARCHAR(50) NULL
    COMMENT 'Mã tra cứu hoá đơn điện tử trên cổng NCC/Tổng cục Thuế (nếu có)'
    AFTER einvoice_issue_date,
  ADD COLUMN einvoice_url VARCHAR(500) NULL
    COMMENT 'Đường dẫn tra cứu/xem hoá đơn điện tử trên cổng NCC/Tổng cục Thuế (nếu có) - KHÔNG phải file upload, chỉ lưu link tham chiếu'
    AFTER einvoice_lookup_code;
