-- ============================================================================
-- Dữ liệu seed CHỈ DÙNG DEV/TEST — không phải dữ liệu production.
-- Tạo 1 user demo + 3 mã chức năng mẫu (PRDCT_VIEW/PRDCT_EDIT/PRDCT_DEL) để
-- test luồng đăng nhập + phân quyền end-to-end, kèm vài dòng category mẫu
-- (category_type='PRODUCT') để có dữ liệu chạy thử màn Product Master.
--
-- Tài khoản test: user_code=admin, password=Fafoshop@123
-- (password_hash bên dưới sinh bằng đúng PasswordUtility.hash() của project —
-- PBKDF2WithHmacSHA256, không phải plaintext).
-- ============================================================================

USE fafoshop_pos;

INSERT INTO app_function
  (function_code, name, short_name, menu_show_flg, auth_required_flg, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('PRDCT_VIEW', 'Xem sản phẩm', 'Xem SP', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('PRDCT_EDIT', 'Sửa sản phẩm', 'Sửa SP', '1', '1', '0', 'system', 'SEED', 'system', 'SEED'),
  ('PRDCT_DEL', 'Xoá sản phẩm', 'Xoá SP', '1', '1', '0', 'system', 'SEED', 'system', 'SEED')
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
  ('admin', 'PRDCT_DEL', '1', 'system', 'SEED', 'system', 'SEED')
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
