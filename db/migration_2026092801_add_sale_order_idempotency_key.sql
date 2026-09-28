-- ============================================================================
-- Migration: thêm sale_order.idempotency_key (chống ghi trùng đơn bán).
--
-- Bối cảnh: khách báo lúc máy lag, bấm "Thanh toán"/"Thanh toán & in" 2 lần
-- ghi nhận ra 2 hoá đơn. Frontend gửi kèm 1 khoá ngẫu nhiên (UUID) cho mỗi
-- lần thanh toán 1 giỏ hàng; server gặp lại khoá đã ghi nhận thì trả lại
-- ĐÚNG đơn cũ thay vì tạo đơn mới (xem SaleOrderCreateProcess).
--
-- Cột NULL được (đơn cũ trước migration, hoặc client cũ không gửi khoá) —
-- UNIQUE KEY của MySQL cho phép nhiều dòng NULL nên không ảnh hưởng dữ liệu
-- đã có.
--
-- BẮT BUỘC chạy file này TRƯỚC KHI deploy backend bản mới — backend mới ghi
-- vào cột này ở mọi đơn bán, thiếu cột thì KHÔNG bán được hàng.
--
-- Chạy TAY 1 lần vào DB fafoshop_pos đã có sẵn (đã đồng bộ vào db/schema.sql
-- cho lần dựng DB mới từ đầu). Cần tài khoản có quyền ALTER (user fafoshop
-- chỉ có SELECT/INSERT/UPDATE/DELETE).
--
-- Cách chạy: mysql -h 127.0.0.1 -P 3307 -u <user> -p fafoshop_pos < db/migration_2026092801_add_sale_order_idempotency_key.sql
-- ============================================================================

USE fafoshop_pos;

ALTER TABLE sale_order
  ADD COLUMN idempotency_key VARCHAR(64) NULL
    COMMENT 'Khoá chống ghi trùng do POS gửi kèm mỗi lần thanh toán (UUID) — gặp lại khoá thì trả đơn cũ, không tạo đơn mới'
    AFTER void_flg,
  ADD UNIQUE KEY uk_saleorder_idempotency_key (idempotency_key);
