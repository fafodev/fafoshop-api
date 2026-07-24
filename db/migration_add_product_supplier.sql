-- ============================================================================
-- migration: bảng product_supplier (quan hệ NHIỀU-NHIỀU sản phẩm <-> nhà
-- cung cấp, mỗi cặp có mã hàng riêng của NCC + giá mua riêng) + xoá cột
-- product.supplier_code (FK đơn cũ, không còn đúng nghiệp vụ vì 1 sản
-- phẩm có thể lấy từ nhiều NCC).
--
-- Thiết kế product_supplier theo ĐÚNG tiền lệ bảng quan hệ nhiều-nhiều đã
-- có trong schema (function_permission): khoá chính GHÉP, đủ audit
-- column, KHÔNG có del_flg (xoá quan hệ = xoá thẳng dòng).
-- ============================================================================

USE fafoshop_pos;

-- product.supplier_code cũ KHÔNG có FK constraint thật (chỉ có index) nên
-- không cần tắt FOREIGN_KEY_CHECKS khi xoá cột này.
ALTER TABLE product DROP INDEX idx_product_supplier_code;
ALTER TABLE product DROP COLUMN supplier_code;

-- ----------------------------------------------------------------------------
-- product_supplier — 1 sản phẩm có thể lấy từ nhiều NCC, mỗi NCC có mã
-- hàng riêng (mã trong catalog/hoá đơn của NCC, khác product_code nội bộ
-- của fafoshop) và giá mua riêng. KHÔNG có khái niệm "NCC chính" — danh
-- sách ngang hàng.
-- ----------------------------------------------------------------------------
CREATE TABLE product_supplier (
  product_code           VARCHAR(100)  NOT NULL COMMENT 'Mã sản phẩm (1 phần khoá chính)',
  supplier_code          VARCHAR(20)   NOT NULL COMMENT 'Mã nhà cung cấp (1 phần khoá chính)',
  supplier_product_code  VARCHAR(50)   NULL COMMENT 'Mã hàng riêng của NCC cho sản phẩm này (khác product_code nội bộ)',
  purchase_price         DECIMAL(12,2) NULL COMMENT 'Giá mua từ NCC này - quy tắc thuế/làm tròn khi mua: UNKNOWN',
  entry_user_code        VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program          VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code       VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program         VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (product_code, supplier_code),
  CONSTRAINT fk_prodsup_product FOREIGN KEY (product_code) REFERENCES product (product_code),
  CONSTRAINT fk_prodsup_supplier FOREIGN KEY (supplier_code) REFERENCES supplier (supplier_code)
);
