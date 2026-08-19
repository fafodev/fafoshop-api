-- ============================================================================
-- Migration TỔNG HỢP — gom các migration đã có sẵn từng file riêng nhưng CHƯA
-- từng được áp vào 1 số môi trường (phát hiện thật trên DB dev
-- localhost:3307 ngày 2026-08-19: có 447 sản phẩm dữ liệu thật nhưng thiếu
-- hẳn bảng product_unit + nhiều cột/seed khác — nghi do DB bị restore lại từ
-- bản cũ hơn các phiên làm tính năng đa đơn vị tính/sửa-huỷ đơn).
--
-- Mục đích: 1 file DUY NHẤT để đưa BẤT KỲ môi trường nào (dev bị thiếu, hoặc
-- production lúc triển khai cho khách lần đầu) lên khớp đúng baseline hiện
-- tại — dùng cùng lúc với `schema.sql`/`seed_dev.sql` khi setup máy MỚI (chưa
-- có các cột/bảng này), hoặc chạy riêng để vá môi trường cũ đã thiếu. LƯU Ý:
-- các câu `ALTER TABLE ... ADD COLUMN` KHÔNG dùng `IF NOT EXISTS` (MySQL
-- không hỗ trợ cú pháp này cho ADD COLUMN, khác MariaDB — đã xác nhận thật
-- trên MySQL 8.4.9) — nếu chạy lại trên môi trường ĐÃ có sẵn 1 phần cột này
-- (vd đã áp permission_catchup 1 lần rồi), câu ALTER tương ứng sẽ báo lỗi
-- "Duplicate column name", BỎ QUA câu đó và chạy tiếp các câu còn lại (mỗi
-- ALTER/CREATE TABLE độc lập, không nằm chung 1 transaction). `CREATE TABLE
-- IF NOT EXISTS` và `INSERT ... ON DUPLICATE KEY UPDATE` vẫn chạy lại được an
-- toàn nhiều lần như bình thường. Gộp nội dung từ:
--   - migration_add_product_unit.sql
--   - migration_add_order_item_unit_columns.sql
--   - migration_add_inbound_receipt_item_pricing.sql
--   - migration_add_inbound_receipt_void_flg.sql
--   - migration_add_product_cost.sql (giá vốn cấu hình trong Product Master,
--     xem docs/pos-dong-bo-gia.md)
--   - phần seed function_code sửa/huỷ đơn còn thiếu (docs/pos-sua-huy-don.md)
-- KHÔNG xoá/sửa dữ liệu nghiệp vụ đã có — chỉ CREATE TABLE/ADD COLUMN/INSERT
-- seed, an toàn với dữ liệu sản phẩm/đơn hàng thật đang có sẵn.
-- ============================================================================

USE fafoshop_pos;

-- ----------------------------------------------------------------------------
-- 1) product.cost — giá vốn hiện hành đơn vị lẻ (xem docs/pos-dong-bo-gia.md)
-- ----------------------------------------------------------------------------
ALTER TABLE product
  ADD COLUMN cost DECIMAL(12,2) NULL
  COMMENT 'Giá vốn hiện hành của đơn vị lẻ - cấu hình/cập nhật trực tiếp trong Product Master (Nhập hàng đổi giá thì hỏi xác nhận rồi ghi đè cột này), KHÔNG tính bình quân gia quyền từ inbound_receipt_item. NULL = chưa từng cấu hình/chưa nhập hàng lần nào.'
  AFTER price;

-- ----------------------------------------------------------------------------
-- 2) product_unit — bảng đơn vị đóng gói (Lốc/Thùng...), xem
--    docs/pos-da-don-vi-tinh.md — kèm sẵn unit_cost (giá vốn theo đơn vị này,
--    docs/pos-dong-bo-gia.md, KHÔNG có ở bản migration_add_product_unit.sql
--    gốc vì lúc đó chưa có tính năng giá vốn theo đơn vị).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_unit (
  product_code      VARCHAR(100)  NOT NULL COMMENT 'Mã sản phẩm (1 phần khoá chính)',
  unit_name         VARCHAR(20)   NOT NULL COMMENT 'Tên đơn vị đóng gói (Lốc, Thùng...) - 1 phần khoá chính',
  conversion_qty    INT(9)        NOT NULL COMMENT 'Số lượng đơn vị NHỎ NHẤT (lẻ) quy đổi ra 1 đơn vị này, vd Lốc=4',
  unit_price        DECIMAL(12,2) NOT NULL COMMENT 'Giá bán khi bán theo đơn vị này - nhập tay riêng, KHÔNG ép theo tỉ lệ product.price*conversion_qty (thường có chiết khấu mua sỉ)',
  unit_cost         DECIMAL(12,2) NULL COMMENT 'Giá vốn khi nhập theo đơn vị này - nhập tay riêng, KHÔNG ép theo tỉ lệ conversion_qty (thường có chiết khấu mua sỉ, giống unit_price). NULL = chưa cấu hình.',
  entry_user_code   VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program     VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (product_code, unit_name),
  CONSTRAINT fk_productunit_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);

-- ----------------------------------------------------------------------------
-- 3) unit_name/unit_qty trên sale_order_item + inbound_receipt_item — hiển
--    thị lại đúng đơn vị đã chọn lúc giao dịch (docs/pos-da-don-vi-tinh.md).
-- ----------------------------------------------------------------------------
ALTER TABLE sale_order_item
  ADD COLUMN unit_name VARCHAR(20) NULL
  COMMENT 'Tên đơn vị đóng gói đã chọn lúc bán (vd "Lốc") — NULL = đơn vị lẻ. CHỈ phục vụ hiển thị lại, KHÔNG dùng tính toán (quantity đã quy đổi ra lẻ mới là nguồn sự thật).'
  AFTER unit_cost,
  ADD COLUMN unit_qty INT(9) NULL
  COMMENT 'Số lượng theo ĐÚNG đơn vị đã chọn (vd 3, nếu unit_name="Lốc") — NULL khi unit_name NULL.'
  AFTER unit_name;

ALTER TABLE inbound_receipt_item
  ADD COLUMN unit_name VARCHAR(20) NULL
  COMMENT 'Tên đơn vị đóng gói đã chọn lúc nhập (vd "Lốc") — NULL = đơn vị lẻ. CHỈ phục vụ hiển thị lại, KHÔNG dùng tính toán (quantity đã quy đổi ra lẻ mới là nguồn sự thật).'
  AFTER unit_cost,
  ADD COLUMN unit_qty INT(9) NULL
  COMMENT 'Số lượng theo ĐÚNG đơn vị đã chọn (vd 3, nếu unit_name="Lốc") — NULL khi unit_name NULL.'
  AFTER unit_name,
  ADD COLUMN line_amount DECIMAL(12,2) NULL
  COMMENT 'Thành tiền CHÍNH XÁC của dòng (làm tròn ĐÚNG 1 lần khi thêm qua đơn vị đóng gói không chia hết) — NULL = tính bình thường unit_cost × quantity, mirror sale_order_item.line_amount.'
  AFTER unit_qty,
  ADD COLUMN price DECIMAL(12,2) NULL
  COMMENT 'Giá bán ĐÃ ÁP DỤNG lúc lập phiếu này (đồng thời ghi vào product.price) — lưu lại để xem lại đúng ở Chi tiết phiếu nhập.'
  AFTER line_amount;

-- ----------------------------------------------------------------------------
-- 4) inbound_receipt.void_flg — huỷ phiếu nhập (docs/pos-sua-huy-don.md)
-- ----------------------------------------------------------------------------
ALTER TABLE inbound_receipt
  ADD COLUMN void_flg VARCHAR(1) NOT NULL DEFAULT '0'
  COMMENT 'Cờ phiếu bị huỷ (thay cho xoá cứng): 1=đã huỷ, 0=còn hiệu lực'
  AFTER einvoice_url;

-- ----------------------------------------------------------------------------
-- 5) function_code sửa/huỷ đơn bán + tra cứu/sửa/huỷ phiếu nhập còn thiếu
--    (docs/pos-sua-huy-don.md) — ON DUPLICATE KEY UPDATE nên an toàn chạy lại,
--    KHÔNG đụng tới app_user (không reset mật khẩu admin thật).
-- ----------------------------------------------------------------------------
INSERT INTO app_function
  (function_code, name, short_name, menu_show_flg, auth_required_flg, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('SALE_EDIT', 'Sửa/huỷ đơn bán (tự tạo, 15 phút)', 'Sửa đơn bán', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('SALE_MGR', 'QL sửa/huỷ đơn bán (không giới hạn)', 'QL đơn bán', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('INBND_VIEW', 'Tra cứu phiếu nhập hàng', 'Tra cứu nhập', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('INBND_EDIT', 'Sửa/huỷ phiếu nhập (tự tạo, 15 phút)', 'Sửa phiếu nhập', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('INBND_MGR', 'QL sửa/huỷ phiếu nhập (không giới hạn)', 'QL phiếu nhập', '1', '1', '0', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO function_permission
  (user_code, function_code, auth_type, entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('admin', 'SALE_EDIT', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'SALE_MGR', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'INBND_VIEW', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'INBND_EDIT', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'INBND_MGR', '1', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE auth_type = VALUES(auth_type);
