-- ============================================================================
-- migration: thêm cột sale_order.customer_name — trường "Khách hàng" trên
-- POS chỉ là ô nhập tự do (không phải mã khách hàng thật, chưa có màn hình
-- quản lý khách hàng), trong khi customer_code là FK bắt buộc trỏ đúng bảng
-- customer có sẵn. Thêm cột free-text riêng để lưu đúng tên khách gõ tay mà
-- không cần xây form quản lý khách hàng ngay bây giờ.
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE sale_order
  ADD COLUMN customer_name VARCHAR(100) NULL
  COMMENT 'Tên khách hàng ghi tự do lúc bán (chưa có màn quản lý khách hàng nên KHÔNG qua customer_code)'
  AFTER customer_code;
