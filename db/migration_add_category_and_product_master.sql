-- ============================================================================
-- Migration: thêm bảng category (bảng mã DÙNG CHUNG nhiều nghiệp vụ, không
-- riêng cho product — phân biệt bằng cột category_type) + ràng buộc khoá
-- ngoại product.category_code -> category.category_code, phục vụ màn hình
-- Product Master (lọc/hiển thị tên danh mục sản phẩm).
--
-- User 'fafoshop' chỉ có quyền SELECT/INSERT/UPDATE/DELETE, không có
-- CREATE/ALTER — chạy file này bằng tài khoản admin MySQL 8.4 (cổng 3307),
-- xem db/mysql8.info (đã gitignore).
-- ============================================================================

USE fafoshop_pos;

-- 1) Bảng category — bảng mã dùng chung: category_code là khoá chính DUY
-- NHẤT (1 cột), category_type phân biệt bảng này đang phục vụ nghiệp vụ nào
-- (PRODUCT = danh mục sản phẩm cho lần này) để sau này tái dùng cho nghiệp vụ
-- khác mà không cần tạo bảng mã mới.
CREATE TABLE category (
  category_code    VARCHAR(4)    NOT NULL COMMENT 'Mã danh mục (khoá chính, bảng mã dùng chung nhiều nghiệp vụ)',
  category_type    VARCHAR(20)   NOT NULL COMMENT 'Loại danh mục - phân biệt bảng này đang phục vụ nghiệp vụ nào (vd PRODUCT = danh mục sản phẩm)',
  name             VARCHAR(100)  NOT NULL COMMENT 'Tên danh mục',
  display_order    INT(9)        NOT NULL DEFAULT 0 COMMENT 'Thứ tự hiển thị',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (category_code),
  KEY idx_category_type (category_type)
);

-- 2) Ràng buộc khoá ngoại product.category_code -> category.category_code.
-- category_code trên product vốn nullable (chưa bắt buộc chọn danh mục) nên
-- FK này không ảnh hưởng dữ liệu product hiện có (NULL không vi phạm FK).
ALTER TABLE product
  ADD CONSTRAINT fk_product_category FOREIGN KEY (category_code) REFERENCES category (category_code);
