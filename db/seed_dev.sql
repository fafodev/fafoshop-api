-- ============================================================================
-- Dữ liệu seed CHỈ DÙNG DEV/TEST — không phải dữ liệu production.
-- Tạo 1 user demo + mã chức năng mẫu (PRDCT_*/SPLR_*/CTGR_*) để test luồng
-- đăng nhập + phân quyền end-to-end, kèm vài dòng category/supplier mẫu để
-- có dữ liệu chạy thử màn Product/Supplier/Category Master.
--
-- Tài khoản test: user_code=admin, password=Fafoshop@123
-- (password_hash bên dưới sinh bằng đúng PasswordUtility.hash() của project —
-- PBKDF2WithHmacSHA256, không phải plaintext).
-- ============================================================================

USE fafoshop_pos;

-- seq_no KHÔNG phải dữ liệu dev/test — đây là cấu hình BẮT BUỘC để
-- SeqNoUtility hoạt động (category/supplier/product Create Process đều gọi
-- tới ngay trong transaction INSERT, thiếu dòng nào là FatalException ngay
-- khi tạo mới). Xem .claude/seqno-convention.md.
INSERT INTO seq_no
  (prefix, seq_no, max_digit, description, entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('NCC', 0, 4, 'Mã nhà cung cấp (supplier.supplier_code)', 'system', 'SEED', 'system', 'SEED'),
  ('DM', 0, 4, 'Mã danh mục (category.category_code)', 'system', 'SEED', 'system', 'SEED'),
  ('SP', 0, 4, 'Mã sản phẩm (product.product_code)', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO app_function
  (function_code, name, short_name, menu_show_flg, auth_required_flg, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('PRDCT_VIEW', 'Xem sản phẩm', 'Xem SP', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('PRDCT_EDIT', 'Sửa sản phẩm', 'Sửa SP', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('PRDCT_DEL', 'Xoá sản phẩm', 'Xoá SP', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('SPLR_VIEW', 'Xem nhà cung cấp', 'Xem NCC', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('SPLR_EDIT', 'Sửa nhà cung cấp', 'Sửa NCC', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('SPLR_DEL', 'Xoá nhà cung cấp', 'Xoá NCC', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('CTGR_VIEW', 'Xem danh mục', 'Xem DM', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('CTGR_EDIT', 'Sửa danh mục', 'Sửa DM', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('CTGR_DEL', 'Xoá danh mục', 'Xoá DM', '1', '1', '0', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO app_user
  (user_code, name, password_hash, main_branch_code, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('admin', 'Quản trị viên', '120000:VBdHrv022mSjEYe1Ad0VzQ==:m6gbk4Idl7Va/V8tPhVHTanX6AI/VfH2oso9XeXLB0U=',
   NULL, '0', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

INSERT INTO function_permission
  (user_code, function_code, auth_type, entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('admin', 'PRDCT_VIEW', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'PRDCT_EDIT', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'PRDCT_DEL', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'SPLR_VIEW', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'SPLR_EDIT', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'SPLR_DEL', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'CTGR_VIEW', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'CTGR_EDIT', '1', 'system', 'SEED', 'system', 'SEED'),
  ('admin', 'CTGR_DEL', '1', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE auth_type = VALUES(auth_type);

INSERT INTO category
  (category_code, category_type, name, display_order, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('TP01', 'PRODUCT', 'Thực phẩm', 1, '0', 'system', 'SEED', 'system', 'SEED'),
  ('DGD1', 'PRODUCT', 'Đồ gia dụng', 2, '0', 'system', 'SEED', 'system', 'SEED'),
  ('HMP1', 'PRODUCT', 'Hoá mỹ phẩm', 3, '0', 'system', 'SEED', 'system', 'SEED'),
  ('VPP1', 'PRODUCT', 'Văn phòng phẩm', 4, '0', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO supplier
  (supplier_code, name, short_name, zip_code, address1, tel, contact_name, email, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('SUP0000000000000001', 'Công ty TNHH Thực phẩm An Bình', 'An Bình', '700000',
   '12 Nguyễn Huệ, Q1, TP.HCM', '0281234567', 'Nguyễn Văn A', 'anbinh@example.com', '0',
   'system', 'SEED', 'system', 'SEED'),
  ('SUP0000000000000002', 'Công ty CP Hoá mỹ phẩm Sạch Đẹp', 'Sạch Đẹp', '700000',
   '45 Lê Lợi, Q1, TP.HCM', '0287654321', 'Trần Thị B', 'sachdep@example.com', '0',
   'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE name = VALUES(name);
