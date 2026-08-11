-- ============================================================================
-- migration: thêm sale_order.payment_method (CASH/TRANSFER) + bảng
-- bank_account (tài khoản NH nhận tiền theo chi nhánh, dùng build QR chuyển
-- khoản chuẩn EMVCo/Napas247 lúc in hoá đơn POS). Xem đầy đủ thiết kế tại
-- docs/pos-in-hoa-don.md (gốc workspace).
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE sale_order
  ADD COLUMN payment_method VARCHAR(10) NOT NULL DEFAULT 'CASH'
  COMMENT 'Phương thức thanh toán: CASH=tiền mặt, TRANSFER=chuyển khoản'
  AFTER change_amount;

CREATE TABLE IF NOT EXISTS bank_account (
  branch_code      VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh nhận tiền (khoá chính)',
  bank_bin         VARCHAR(6)    NOT NULL COMMENT 'Mã BIN ngân hàng theo chuẩn Napas (vd 970436 = Vietcombank)',
  bank_name        VARCHAR(100)  NOT NULL COMMENT 'Tên ngân hàng hiển thị (vd Vietcombank)',
  account_no       VARCHAR(30)   NOT NULL COMMENT 'Số tài khoản nhận tiền',
  account_name     VARCHAR(100)  NOT NULL COMMENT 'Tên chủ tài khoản (không dấu, khớp thông tin ngân hàng)',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (branch_code),
  CONSTRAINT fk_bankaccount_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code)
);
