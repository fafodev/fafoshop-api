-- ============================================================================
-- Migration: tạo lại bảng hkd_info (thông tin hộ kinh doanh in ở đầu Sổ
-- S1a-HKD khi xuất báo cáo doanh thu phục vụ kê khai thuế) — bản này thêm 2
-- cột phone/email so với lần tạo đầu (nếu đã chạy migration cũ, chạy lại
-- file này sẽ XOÁ VÀ TẠO LẠI bảng, mất dữ liệu cũ đã sửa tay nếu có — sửa
-- lại INSERT bên dưới cho đúng trước khi chạy nếu cần).
--
-- Chạy TAY 1 lần vào DB fafoshop_pos đã có sẵn (không cần chạy lại toàn bộ
-- schema.sql/seed_dev.sql) — nội dung bên dưới đã được đồng bộ vào
-- db/schema.sql (định nghĩa bảng) và db/seed_dev.sql (dữ liệu) để lần setup
-- DB mới từ đầu (mysql < schema.sql rồi < seed_dev.sql) tự có luôn, không
-- cần chạy riêng file này nữa.
--
-- Cách chạy: mysql -h 127.0.0.1 -P 3307 -u <user> -p fafoshop_pos < db/migration_2026082001_add_hkd_info.sql
-- ============================================================================

USE fafoshop_pos;

DROP TABLE IF EXISTS hkd_info;

CREATE TABLE hkd_info (
  branch_code      VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh (khoá chính)',
  hkd_name         VARCHAR(100)  NOT NULL COMMENT 'Tên hộ, cá nhân kinh doanh (in ở đầu Sổ S1a-HKD)',
  address          VARCHAR(200)  NOT NULL COMMENT 'Địa chỉ kinh doanh (in ở đầu Sổ S1a-HKD)',
  tax_code         VARCHAR(14)   NOT NULL COMMENT 'Mã số thuế hộ kinh doanh',
  phone            VARCHAR(14)   NULL COMMENT 'Số điện thoại liên hệ của hộ kinh doanh (không bắt buộc)',
  email            VARCHAR(100)  NULL COMMENT 'Email liên hệ của hộ kinh doanh (không bắt buộc)',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (branch_code),
  CONSTRAINT fk_hkdinfo_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code)
);

-- Thông tin thật của hộ kinh doanh (đối chiếu
-- Thue/KY_KHAI_BAO_THUE_HKD_DUOI_1_TY.md ở gốc workspace) — sửa lại nếu đổi
-- tên/địa chỉ/MST/điện thoại/email, hoặc đổi branch_code nếu chi nhánh của
-- bạn không phải CN001. phone/email CHƯA có thông tin thật, đang để NULL —
-- tự sửa UPDATE bên dưới khi có.
INSERT INTO hkd_info
  (branch_code, hkd_name, address, tax_code, phone, email, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('CN001', 'fafo', '333 Đống Đa, TP Huế', '046985092925', NULL, NULL, '0',
   'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE hkd_name = VALUES(hkd_name), address = VALUES(address), tax_code = VALUES(tax_code);

-- Nếu muốn điền luôn số điện thoại/email ngay khi chạy migration, sửa 2 dòng
-- NULL ở INSERT bên trên, hoặc chạy thêm UPDATE riêng sau, ví dụ:
-- UPDATE hkd_info SET phone = '0901234567', email = 'contact@fafoshop.vn',
--   update_user_code = 'system', update_program = 'SEED' WHERE branch_code = 'CN001';
