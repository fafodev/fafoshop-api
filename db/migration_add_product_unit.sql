-- Migration: thêm bảng product_unit (đơn vị đóng gói lớn hơn đơn vị lẻ, vd
-- Lốc/Thùng) — xem docs/pos-da-don-vi-tinh.md (gốc workspace). Áp bằng tài
-- khoản admin (db/mysql8.info) vì user fafoshop không có quyền CREATE TABLE.

USE fafoshop_pos;

CREATE TABLE product_unit (
  product_code      VARCHAR(100)  NOT NULL COMMENT 'Mã sản phẩm (1 phần khoá chính)',
  unit_name         VARCHAR(20)   NOT NULL COMMENT 'Tên đơn vị đóng gói (Lốc, Thùng...) - 1 phần khoá chính',
  conversion_qty    INT(9)        NOT NULL COMMENT 'Số lượng đơn vị NHỎ NHẤT (lẻ) quy đổi ra 1 đơn vị này, vd Lốc=4',
  unit_price        DECIMAL(12,2) NOT NULL COMMENT 'Giá bán khi bán theo đơn vị này - nhập tay riêng, KHÔNG ép theo tỉ lệ product.price*conversion_qty (thường có chiết khấu mua sỉ)',
  entry_user_code   VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program     VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (product_code, unit_name),
  CONSTRAINT fk_productunit_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);
