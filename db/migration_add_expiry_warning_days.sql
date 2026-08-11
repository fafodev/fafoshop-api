-- ============================================================================
-- Migration: thêm cột product.expiry_warning_days (số ngày cảnh báo trước hạn
-- sử dụng, phục vụ màn hình Nhập hàng — form "Thêm nhanh sản phẩm" mặc định
-- 90 ngày, xem ProductCreateRequest.expiryWarningDays). User 'fafoshop' chỉ
-- có quyền SELECT/INSERT/UPDATE/DELETE, không có ALTER — chạy file này bằng
-- tài khoản admin MySQL 8.4 (cổng 3307).
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE product ADD COLUMN expiry_warning_days INT(9) NOT NULL DEFAULT 90
  COMMENT 'Số ngày cảnh báo trước hạn sử dụng (dùng cho cảnh báo hàng sắp hết hạn sau này) - mặc định 90 ngày.'
  AFTER min_stock_qty;
