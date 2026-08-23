-- Quyền màn Kiểm kê tồn kho (docs/pos-kiem-ke.md).
-- ON DUPLICATE KEY UPDATE — chạy lại an toàn, không đụng mật khẩu admin.

USE fafoshop_pos;

INSERT INTO app_function
  (function_code, name, short_name, menu_show_flg, auth_required_flg, del_flg,
   entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('STK_EDIT', 'Kiểm kê tồn kho', 'Kiểm kê', '1', '1', '0', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE name = VALUES(name), short_name = VALUES(short_name);

INSERT INTO function_permission
  (user_code, function_code, auth_type, entry_user_code, entry_program, update_user_code, update_program)
VALUES
  ('admin', 'STK_EDIT', '1', 'system', 'SEED', 'system', 'SEED')
ON DUPLICATE KEY UPDATE auth_type = VALUES(auth_type);
