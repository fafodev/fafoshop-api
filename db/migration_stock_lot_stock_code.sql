-- Khoá chính stock đổi sang stock_code (seq_no prefix IS) để 1 sản phẩm
-- có nhiều dòng hạn dùng. quality_code CHƯA đưa vào khoá.
-- Xem docs/pos-lo-ton-kho.md.

USE fafoshop_pos;

INSERT INTO seq_no
  (prefix, seq_no, max_digit, description, entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('IS', 0, 4, 'Mã lô tồn kho (stock.stock_code)', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE description = VALUES(description);

ALTER TABLE stock
  ADD COLUMN stock_code VARCHAR(20) NULL
  COMMENT 'Mã lô tồn kho (khoá chính) - tự sinh dạng IS+yyyyMMdd+4 số (seq_no prefix IS)'
  FIRST;

SET @seq := 0;
UPDATE stock
  SET stock_code = CONCAT('IS', DATE_FORMAT(CURDATE(), '%Y%m%d'), LPAD(@seq := @seq + 1, 4, '0'))
  WHERE stock_code IS NULL;

UPDATE seq_no s
  JOIN (SELECT COUNT(*) AS n FROM stock) t
  SET s.seq_no = GREATEST(s.seq_no, t.n)
  WHERE s.prefix = 'IS';

ALTER TABLE stock DROP FOREIGN KEY fk_stock_branch;
-- fk_stock_branch bám PRIMARY cũ (branch_code) — phải DROP FK trước khi đổi PK.
ALTER TABLE stock DROP PRIMARY KEY;
ALTER TABLE stock MODIFY stock_code VARCHAR(20) NOT NULL
  COMMENT 'Mã lô tồn kho (khoá chính) - tự sinh dạng IS+yyyyMMdd+4 số (seq_no prefix IS)';
ALTER TABLE stock ADD PRIMARY KEY (stock_code);
ALTER TABLE stock ADD KEY idx_stock_branch_product (branch_code, product_code);
ALTER TABLE stock ADD CONSTRAINT fk_stock_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code);

ALTER TABLE sale_order_item
  ADD COLUMN stock_code VARCHAR(20) NULL
  COMMENT 'Mã lô tồn kho đã trừ lúc bán (stock.stock_code). NULL = đơn tạo trước khi theo dõi lô.'
  AFTER product_code;

ALTER TABLE sale_order_item
  ADD COLUMN expiry_date DATE NULL
  COMMENT 'Hạn dùng của lô đã chọn lúc bán (chụp lại). NULL = không hạn / đơn cũ.'
  AFTER stock_code;

-- Đơn cũ: mỗi SP/chi nhánh lúc migrate còn 1 lô → gán stock_code + hạn dùng
-- hiện tại. Dòng bán không còn dòng stock thì để NULL.
UPDATE sale_order_item soi
JOIN sale_order so ON so.sale_order_no = soi.sale_order_no
JOIN stock s ON s.branch_code = so.branch_code AND s.product_code = soi.product_code
SET soi.stock_code = s.stock_code,
    soi.expiry_date = s.expiry_date
WHERE soi.stock_code IS NULL;
