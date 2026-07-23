-- ============================================================================
-- Migration: thêm comment tiếng Việt cho toàn bộ cột (đa số đang trống, 4 cột
-- bị lỗi encoding/mojibake được sửa lại) + thêm cột product.min_stock_qty
-- (định mức tồn tối thiểu, dùng sau này xác định sản phẩm "dưới định mức
-- tồn"). User 'fafoshop' chỉ có quyền SELECT/INSERT/UPDATE/DELETE, không có
-- ALTER — chạy file này bằng tài khoản admin MySQL 8.4 (cổng 3307).
--
-- Đã kiểm chứng qua dry-run bằng JDBC (đọc information_schema, giữ nguyên
-- type/nullable/default/on-update từng cột, chỉ thêm COMMENT) — an toàn,
-- không đổi cấu trúc cột hiện có ngoài việc thêm comment. VIEW
-- (v_daily_revenue, v_item_revenue) không có trong file này vì MySQL không
-- hỗ trợ COMMENT cho cột của VIEW.
-- ============================================================================

USE fafoshop_pos;

-- 1) Thêm cột min_stock_qty vào product.
ALTER TABLE product ADD COLUMN min_stock_qty INT(9) NOT NULL DEFAULT 0
  COMMENT 'Định mức tồn tối thiểu (số lượng) - dùng để xác định sản phẩm dưới định mức tồn (cần nhập thêm); 0 = chưa cấu hình định mức. Áp dụng chung mọi chi nhánh, chưa hỗ trợ định mức riêng theo chi nhánh.'
  AFTER price;

-- 2) app_function
ALTER TABLE app_function MODIFY COLUMN function_code varchar(10) NOT NULL COMMENT 'Mã chức năng (khoá chính)';
ALTER TABLE app_function MODIFY COLUMN name varchar(40) NOT NULL COMMENT 'Tên chức năng';
ALTER TABLE app_function MODIFY COLUMN short_name varchar(20) NOT NULL COMMENT 'Tên rút gọn chức năng';
ALTER TABLE app_function MODIFY COLUMN menu_show_flg varchar(1) NOT NULL DEFAULT '1' COMMENT 'Cờ hiển thị trên menu: 1=hiện, 0=ẩn';
ALTER TABLE app_function MODIFY COLUMN auth_required_flg varchar(1) NOT NULL DEFAULT '1' COMMENT 'Cờ yêu cầu kiểm tra quyền: 1=có yêu cầu, 0=không yêu cầu';
ALTER TABLE app_function MODIFY COLUMN note varchar(200) NULL COMMENT 'Ghi chú';
ALTER TABLE app_function MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE app_function MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE app_function MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE app_function MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE app_function MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE app_function MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE app_function MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 3) app_user
ALTER TABLE app_user MODIFY COLUMN user_code varchar(8) NOT NULL COMMENT 'Mã người dùng (khoá chính)';
ALTER TABLE app_user MODIFY COLUMN name varchar(20) NOT NULL COMMENT 'Họ tên người dùng';
ALTER TABLE app_user MODIFY COLUMN password_hash varchar(255) NOT NULL COMMENT 'Mật khẩu đã băm PBKDF2WithHmacSHA256 (xem PasswordUtility), không lưu plaintext';
ALTER TABLE app_user MODIFY COLUMN main_branch_code varchar(6) NULL COMMENT 'Mã chi nhánh làm việc chính';
ALTER TABLE app_user MODIFY COLUMN note varchar(100) NULL COMMENT 'Ghi chú';
ALTER TABLE app_user MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE app_user MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE app_user MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE app_user MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE app_user MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE app_user MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE app_user MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 4) branch
ALTER TABLE branch MODIFY COLUMN branch_code varchar(6) NOT NULL COMMENT 'Mã chi nhánh/cửa hàng (khoá chính)';
ALTER TABLE branch MODIFY COLUMN name varchar(40) NOT NULL COMMENT 'Tên chi nhánh';
ALTER TABLE branch MODIFY COLUMN short_name varchar(20) NULL COMMENT 'Tên rút gọn chi nhánh';
ALTER TABLE branch MODIFY COLUMN zip_code varchar(8) NOT NULL COMMENT 'Mã bưu chính';
ALTER TABLE branch MODIFY COLUMN address1 varchar(100) NOT NULL COMMENT 'Địa chỉ - dòng 1';
ALTER TABLE branch MODIFY COLUMN address2 varchar(100) NOT NULL COMMENT 'Địa chỉ - dòng 2';
ALTER TABLE branch MODIFY COLUMN address3 varchar(100) NULL COMMENT 'Địa chỉ - dòng 3';
ALTER TABLE branch MODIFY COLUMN tel varchar(14) NULL COMMENT 'Số điện thoại';
ALTER TABLE branch MODIFY COLUMN fax varchar(14) NULL COMMENT 'Số fax';
ALTER TABLE branch MODIFY COLUMN manager_name varchar(20) NULL COMMENT 'Tên quản lý chi nhánh';
ALTER TABLE branch MODIFY COLUMN contact_name varchar(20) NULL COMMENT 'Tên người liên hệ';
ALTER TABLE branch MODIFY COLUMN note varchar(100) NULL COMMENT 'Ghi chú';
ALTER TABLE branch MODIFY COLUMN start_date varchar(8) NULL COMMENT 'Ngày bắt đầu hoạt động (định dạng YYYYMMDD)';
ALTER TABLE branch MODIFY COLUMN end_date varchar(8) NULL COMMENT 'Ngày ngừng hoạt động (định dạng YYYYMMDD)';
ALTER TABLE branch MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE branch MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE branch MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE branch MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE branch MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE branch MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE branch MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 5) customer
ALTER TABLE customer MODIFY COLUMN customer_code varchar(20) NOT NULL COMMENT 'Mã khách hàng (khoá chính)';
ALTER TABLE customer MODIFY COLUMN name varchar(100) NOT NULL COMMENT 'Tên khách hàng';
ALTER TABLE customer MODIFY COLUMN phone varchar(20) NULL COMMENT 'Số điện thoại';
ALTER TABLE customer MODIFY COLUMN email varchar(100) NULL COMMENT 'Địa chỉ email';
ALTER TABLE customer MODIFY COLUMN note varchar(255) NULL COMMENT 'Ghi chú';
ALTER TABLE customer MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE customer MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE customer MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE customer MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE customer MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE customer MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE customer MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 6) function_permission
ALTER TABLE function_permission MODIFY COLUMN user_code varchar(8) NOT NULL COMMENT 'Mã người dùng (một phần khoá chính)';
ALTER TABLE function_permission MODIFY COLUMN function_code varchar(10) NOT NULL COMMENT 'Mã chức năng (một phần khoá chính)';
ALTER TABLE function_permission MODIFY COLUMN auth_type varchar(1) NOT NULL DEFAULT '1' COMMENT '1=được phép, 0=không được phép';
ALTER TABLE function_permission MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE function_permission MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE function_permission MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE function_permission MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE function_permission MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE function_permission MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 7) inbound_receipt
ALTER TABLE inbound_receipt MODIFY COLUMN branch_code varchar(6) NOT NULL COMMENT 'Mã chi nhánh nhận hàng (một phần khoá chính)';
ALTER TABLE inbound_receipt MODIFY COLUMN receipt_no varchar(16) NOT NULL COMMENT 'Số phiếu nhập hàng (một phần khoá chính)';
ALTER TABLE inbound_receipt MODIFY COLUMN supplier_code varchar(20) NULL COMMENT 'Mã nhà cung cấp';
ALTER TABLE inbound_receipt MODIFY COLUMN planned_arrival_date date NULL COMMENT 'Ngày dự kiến hàng về';
ALTER TABLE inbound_receipt MODIFY COLUMN actual_arrival_date date NULL COMMENT 'Ngày hàng thực tế về';
ALTER TABLE inbound_receipt MODIFY COLUMN receipt_date date NULL COMMENT 'Ngày lập phiếu nhập';
ALTER TABLE inbound_receipt MODIFY COLUMN receipt_user_code varchar(8) NULL COMMENT 'Mã người dùng lập phiếu nhập';
ALTER TABLE inbound_receipt MODIFY COLUMN note varchar(200) NULL COMMENT 'Ghi chú';
ALTER TABLE inbound_receipt MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE inbound_receipt MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE inbound_receipt MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE inbound_receipt MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE inbound_receipt MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE inbound_receipt MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE inbound_receipt MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 8) inbound_receipt_item
ALTER TABLE inbound_receipt_item MODIFY COLUMN branch_code varchar(6) NOT NULL COMMENT 'Mã chi nhánh (một phần khoá chính, tham chiếu phiếu nhập)';
ALTER TABLE inbound_receipt_item MODIFY COLUMN receipt_no varchar(16) NOT NULL COMMENT 'Số phiếu nhập hàng (một phần khoá chính, tham chiếu phiếu nhập)';
ALTER TABLE inbound_receipt_item MODIFY COLUMN line_no int NOT NULL COMMENT 'Số thứ tự dòng trong phiếu nhập (một phần khoá chính)';
ALTER TABLE inbound_receipt_item MODIFY COLUMN product_code varchar(100) NOT NULL COMMENT 'Mã sản phẩm';
ALTER TABLE inbound_receipt_item MODIFY COLUMN quality_code varchar(2) NOT NULL DEFAULT '01' COMMENT 'Mã phẩm cấp/tình trạng hàng (01=hàng thường)';
ALTER TABLE inbound_receipt_item MODIFY COLUMN expiry_date date NULL COMMENT 'Hạn sử dụng (nếu có)';
ALTER TABLE inbound_receipt_item MODIFY COLUMN planned_qty int NOT NULL DEFAULT 0 COMMENT 'Số lượng dự kiến nhập';
ALTER TABLE inbound_receipt_item MODIFY COLUMN actual_qty int NOT NULL DEFAULT 0 COMMENT 'Số lượng thực nhận';
ALTER TABLE inbound_receipt_item MODIFY COLUMN unit_cost decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Đơn giá vốn nhập hàng';
ALTER TABLE inbound_receipt_item MODIFY COLUMN note varchar(200) NULL COMMENT 'Ghi chú';
ALTER TABLE inbound_receipt_item MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE inbound_receipt_item MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE inbound_receipt_item MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE inbound_receipt_item MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE inbound_receipt_item MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE inbound_receipt_item MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 9) product
ALTER TABLE product MODIFY COLUMN product_code varchar(100) NOT NULL COMMENT 'Mã sản phẩm (khoá chính)';
ALTER TABLE product MODIFY COLUMN name varchar(100) NOT NULL COMMENT 'Tên sản phẩm';
ALTER TABLE product MODIFY COLUMN short_name varchar(50) NULL COMMENT 'Tên rút gọn sản phẩm';
ALTER TABLE product MODIFY COLUMN barcode varchar(14) NULL COMMENT 'Mã vạch sản phẩm';
ALTER TABLE product MODIFY COLUMN category_code varchar(4) NULL COMMENT 'Mã danh mục sản phẩm';
ALTER TABLE product MODIFY COLUMN supplier_code varchar(20) NULL COMMENT 'Mã nhà cung cấp';
ALTER TABLE product MODIFY COLUMN unit_name varchar(20) NULL DEFAULT '0' COMMENT 'Đơn vị tính (cái, kg, thùng...)';
ALTER TABLE product MODIFY COLUMN reduced_tax_rate_flg varchar(1) NULL DEFAULT '0' COMMENT 'Cờ áp dụng thuế suất ưu đãi: 1=có, 0=không; quy tắc cụ thể: UNKNOWN';
ALTER TABLE product MODIFY COLUMN price decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Giá bán cố định của sản phẩm';
ALTER TABLE product MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE product MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE product MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE product MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE product MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE product MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE product MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 10) promotion
ALTER TABLE promotion MODIFY COLUMN promotion_code varchar(20) NOT NULL COMMENT 'Mã khuyến mãi (khoá chính)';
ALTER TABLE promotion MODIFY COLUMN name varchar(100) NOT NULL COMMENT 'Tên chương trình khuyến mãi';
ALTER TABLE promotion MODIFY COLUMN discount_type varchar(20) NOT NULL COMMENT 'Loại giảm giá: percent (theo %) | amount (theo số tiền cố định); quy tắc áp dụng cụ thể: UNKNOWN';
ALTER TABLE promotion MODIFY COLUMN discount_value decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Giá trị giảm giá (theo % hoặc số tiền tuỳ discount_type)';
ALTER TABLE promotion MODIFY COLUMN start_date date NULL COMMENT 'Ngày bắt đầu áp dụng';
ALTER TABLE promotion MODIFY COLUMN end_date date NULL COMMENT 'Ngày kết thúc áp dụng';
ALTER TABLE promotion MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE promotion MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE promotion MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE promotion MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE promotion MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE promotion MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE promotion MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 11) sale_order
ALTER TABLE sale_order MODIFY COLUMN sale_order_no varchar(20) NOT NULL COMMENT 'Số đơn bán hàng (khoá chính)';
ALTER TABLE sale_order MODIFY COLUMN branch_code varchar(6) NOT NULL COMMENT 'Mã chi nhánh bán hàng';
ALTER TABLE sale_order MODIFY COLUMN customer_code varchar(20) NULL COMMENT 'Mã khách hàng (có thể trống nếu khách lẻ không lưu thông tin)';
ALTER TABLE sale_order MODIFY COLUMN sale_datetime datetime NOT NULL COMMENT 'Thời điểm bán hàng';
ALTER TABLE sale_order MODIFY COLUMN paid_amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Số tiền khách thanh toán';
ALTER TABLE sale_order MODIFY COLUMN change_amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Số tiền thối lại cho khách';
ALTER TABLE sale_order MODIFY COLUMN cashier_user_code varchar(8) NOT NULL COMMENT 'Mã thu ngân thực hiện đơn';
ALTER TABLE sale_order MODIFY COLUMN void_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ đơn bị huỷ (thay cho xoá cứng): 1=đã huỷ, 0=còn hiệu lực';
ALTER TABLE sale_order MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE sale_order MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE sale_order MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE sale_order MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE sale_order MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE sale_order MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 12) sale_order_item
ALTER TABLE sale_order_item MODIFY COLUMN sale_order_no varchar(20) NOT NULL COMMENT 'Số đơn bán hàng (một phần khoá chính, tham chiếu đơn bán)';
ALTER TABLE sale_order_item MODIFY COLUMN line_no int NOT NULL COMMENT 'Số thứ tự dòng trong đơn bán (một phần khoá chính)';
ALTER TABLE sale_order_item MODIFY COLUMN product_code varchar(100) NOT NULL COMMENT 'Mã sản phẩm';
ALTER TABLE sale_order_item MODIFY COLUMN unit_price decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Đơn giá bán tại thời điểm giao dịch';
ALTER TABLE sale_order_item MODIFY COLUMN quantity int NOT NULL DEFAULT 1 COMMENT 'Số lượng bán';
ALTER TABLE sale_order_item MODIFY COLUMN line_amount decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Thành tiền của dòng (đơn giá × số lượng)';
ALTER TABLE sale_order_item MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE sale_order_item MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE sale_order_item MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE sale_order_item MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE sale_order_item MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE sale_order_item MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 13) session_token
ALTER TABLE session_token MODIFY COLUMN token varchar(255) NOT NULL COMMENT 'Chuỗi token phiên đăng nhập (khoá chính)';
ALTER TABLE session_token MODIFY COLUMN user_code varchar(8) NOT NULL COMMENT 'Mã người dùng sở hữu phiên đăng nhập';
ALTER TABLE session_token MODIFY COLUMN expire_datetime datetime NOT NULL COMMENT 'Thời điểm token hết hạn';
ALTER TABLE session_token MODIFY COLUMN created_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm phát hành token';

-- 14) stock
ALTER TABLE stock MODIFY COLUMN branch_code varchar(6) NOT NULL COMMENT 'Mã chi nhánh (một phần khoá chính)';
ALTER TABLE stock MODIFY COLUMN product_code varchar(100) NOT NULL COMMENT 'Mã sản phẩm (một phần khoá chính)';
ALTER TABLE stock MODIFY COLUMN quality_code varchar(2) NOT NULL DEFAULT '01' COMMENT 'Mã phẩm cấp/tình trạng hàng (01=hàng thường)';
ALTER TABLE stock MODIFY COLUMN expiry_date date NULL COMMENT 'Hạn sử dụng (nếu có)';
ALTER TABLE stock MODIFY COLUMN stock_qty int NOT NULL DEFAULT 0 COMMENT 'Số lượng tồn kho thực tế';
ALTER TABLE stock MODIFY COLUMN available_qty int NOT NULL DEFAULT 0 COMMENT 'Số lượng tồn khả dụng (có thể bán)';
ALTER TABLE stock MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE stock MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE stock MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE stock MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE stock MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE stock MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';

-- 15) supplier
ALTER TABLE supplier MODIFY COLUMN supplier_code varchar(20) NOT NULL COMMENT 'Mã nhà cung cấp (khoá chính)';
ALTER TABLE supplier MODIFY COLUMN name varchar(80) NOT NULL COMMENT 'Tên đầy đủ nhà cung cấp';
ALTER TABLE supplier MODIFY COLUMN short_name varchar(40) NOT NULL COMMENT 'Tên rút gọn nhà cung cấp';
ALTER TABLE supplier MODIFY COLUMN zip_code varchar(8) NULL COMMENT 'Mã bưu chính';
ALTER TABLE supplier MODIFY COLUMN address1 varchar(100) NULL COMMENT 'Địa chỉ - dòng 1';
ALTER TABLE supplier MODIFY COLUMN address2 varchar(100) NULL COMMENT 'Địa chỉ - dòng 2';
ALTER TABLE supplier MODIFY COLUMN address3 varchar(100) NULL COMMENT 'Địa chỉ - dòng 3';
ALTER TABLE supplier MODIFY COLUMN tel varchar(14) NULL COMMENT 'Số điện thoại';
ALTER TABLE supplier MODIFY COLUMN fax varchar(14) NULL COMMENT 'Số fax';
ALTER TABLE supplier MODIFY COLUMN contact_name varchar(20) NULL COMMENT 'Tên người liên hệ';
ALTER TABLE supplier MODIFY COLUMN email varchar(100) NULL COMMENT 'Địa chỉ email';
ALTER TABLE supplier MODIFY COLUMN note varchar(100) NULL COMMENT 'Ghi chú';
ALTER TABLE supplier MODIFY COLUMN del_flg varchar(1) NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực';
ALTER TABLE supplier MODIFY COLUMN entry_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng tạo bản ghi';
ALTER TABLE supplier MODIFY COLUMN entry_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi';
ALTER TABLE supplier MODIFY COLUMN entry_program varchar(10) NOT NULL COMMENT 'Mã chương trình tạo bản ghi';
ALTER TABLE supplier MODIFY COLUMN update_user_code varchar(8) NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất';
ALTER TABLE supplier MODIFY COLUMN update_datetime timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất';
ALTER TABLE supplier MODIFY COLUMN update_program varchar(10) NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất';
