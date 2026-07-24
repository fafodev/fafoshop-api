-- ============================================================================
-- migration: bảng seq_no (sinh mã quản lý tự động CHUẨN CHUNG toàn hệ
-- thống, xem fafoshop-api/.claude/seqno-convention.md) + nới rộng
-- category_code từ VARCHAR(4) lên VARCHAR(20) để chứa mã tự sinh dạng
-- PREFIX+yyyyMMdd+SEQNO (tối thiểu 14 ký tự, vd "DM202607240001").
--
-- Thiết kế seq_no THAM KHẢO 1 bảng sinh số cùng mục đích từ hệ thống khác
-- (PREFIX/SEQNO/MAXDIGIT/audit columns) do người dùng cung cấp, viết lại
-- theo ĐÚNG quy ước đặt tên/audit column của fafoshop_pos (snake_case, bỏ
-- các cột NUMRSRV1-10 không dùng tới).
-- ============================================================================

USE fafoshop_pos;

-- ----------------------------------------------------------------------------
-- seq_no — sinh số quản lý tăng dần theo prefix (KHÔNG reset theo ngày), dùng
-- CHUNG cho MỌI mã tự sinh trong hệ thống (category_code, supplier_code,
-- product_code...). Mã cuối cùng client thấy được ghép ở tầng ứng dụng
-- (SeqNoUtility.generate()): PREFIX + ngày hiện tại (yyyyMMdd) + seq_no đã
-- tăng, đệm '0' bên trái đủ max_digit chữ số.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seq_no (
  prefix           VARCHAR(4)    NOT NULL COMMENT 'Tiền tố nhận diện loại mã (khoá chính) - vd NCC, DM, SP',
  seq_no           BIGINT        NOT NULL DEFAULT 0 COMMENT 'Số thứ tự đã cấp gần nhất cho prefix này - tăng dần liên tục, KHÔNG reset theo ngày',
  max_digit        INT           NOT NULL DEFAULT 4 COMMENT 'Số chữ số đệm 0 bên trái khi ghép mã (vd 4 -> 0001); số vượt quá vẫn in đủ chữ số, không cắt bớt',
  description      VARCHAR(100)  NULL COMMENT 'Mô tả mục đích dùng của prefix này',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất (mỗi lần cấp số mới cũng tính 1 lần update)',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (prefix)
);

INSERT INTO seq_no
  (prefix, seq_no, max_digit, description, entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('NCC', 0, 4, 'Mã nhà cung cấp (supplier.supplier_code)', 'system', 'SEED', 'system', 'SEED'),
  ('DM', 0, 4, 'Mã danh mục (category.category_code)', 'system', 'SEED', 'system', 'SEED'),
  ('SP', 0, 4, 'Mã sản phẩm (product.product_code)', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ----------------------------------------------------------------------------
-- Nới rộng category_code: mã tự sinh mới dạng "DM"+yyyyMMdd+4 số tối thiểu
-- 14 ký tự, VARCHAR(4) cũ (dữ liệu mẫu nhập tay như "TP01") không đủ chỗ.
-- Phải tắt FOREIGN_KEY_CHECKS tạm thời để sửa cả 2 bảng cùng lúc
-- (category.category_code + product.category_code) vì có khoá ngoại
-- fk_product_category giữa 2 cột này — dữ liệu cũ (mã ngắn) vẫn hợp lệ sau
-- khi nới rộng, không cần migrate dữ liệu.
-- ----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE category MODIFY COLUMN category_code VARCHAR(20) NOT NULL
  COMMENT 'Mã danh mục (khoá chính, bảng mã dùng chung nhiều nghiệp vụ) - tự sinh dạng DM+yyyyMMdd+4 số (xem seq_no/SeqNoUtility)';
ALTER TABLE product MODIFY COLUMN category_code VARCHAR(20) NULL COMMENT 'Mã danh mục sản phẩm';
SET FOREIGN_KEY_CHECKS = 1;
