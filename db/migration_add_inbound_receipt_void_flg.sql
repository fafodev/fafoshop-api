-- Migration: thêm inbound_receipt.void_flg (huỷ phiếu nhập, THAY cho xoá
-- cứng — mirror sale_order.void_flg đã có sẵn) — xem docs/pos-sua-huy-don.md
-- (gốc workspace). Áp bằng tài khoản admin (db/mysql8.info) vì user
-- fafoshop không có quyền ALTER TABLE.

USE fafoshop_pos;

ALTER TABLE inbound_receipt
  ADD COLUMN void_flg VARCHAR(1) NOT NULL DEFAULT '0'
  COMMENT 'Cờ phiếu bị huỷ (thay cho xoá cứng): 1=đã huỷ, 0=còn hiệu lực'
  AFTER einvoice_url;
