-- ============================================================================
-- fafoshop_pos — schema nền tảng cho API bán lẻ (POS)
--
-- Toàn bộ bảng dùng CHUNG 1 quy ước đặt tên snake_case dễ đọc (tên bảng/cột
-- tiếng Anh, không viết HOA). Mọi bảng có cột audit
-- (entry_user_code/entry_datetime/entry_program,
-- update_user_code/update_datetime/update_program) và cột xoá mềm (del_flg)
-- nếu bảng đó cần xoá mềm (bảng dữ liệu giao dịch như stock,
-- inbound_receipt_item không cần del_flg).
-- ============================================================================

CREATE DATABASE IF NOT EXISTS fafoshop_pos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fafoshop_pos;

-- ----------------------------------------------------------------------------
-- seq_no — sinh mã quản lý tự động CHUẨN CHUNG toàn hệ thống (xem
-- .claude/seqno-convention.md) cho MỌI mã tự sinh (category_code,
-- supplier_code, product_code...). Tạo TRƯỚC category/supplier/product vì
-- các Process tạo mới của những bảng đó đều gọi SeqNoUtility (đọc bảng này)
-- ngay trong transaction INSERT.
-- ----------------------------------------------------------------------------
CREATE TABLE seq_no (
  prefix           VARCHAR(4)    NOT NULL COMMENT 'Tiền tố nhận diện loại mã (khoá chính) - vd NCC, DM, SP',
  seq_no           BIGINT        NOT NULL DEFAULT 0 COMMENT 'Số thứ tự đã cấp gần nhất cho prefix này - tăng dần liên tục, KHÔNG reset theo ngày',
  max_digit        INT           NOT NULL DEFAULT 4 COMMENT 'Số chữ số đệm 0 bên trái khi ghép mã (vd 4 -> 0001); số vượt quá vẫn in đủ chữ số, không cắt bớt',
  description      VARCHAR(100)  NULL COMMENT 'Mô tả mục đích dùng của prefix này',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất (mỗi lần cấp số mới cũng tính 1 lần update)',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (prefix)
);

-- ----------------------------------------------------------------------------
-- category — bảng mã DÙNG CHUNG nhiều nghiệp vụ (không riêng cho product).
-- category_code là khoá chính DUY NHẤT (1 cột); category_type phân biệt
-- bảng này đang phục vụ nghiệp vụ nào (PRODUCT = danh mục sản phẩm) để sau
-- này tái dùng cho nghiệp vụ khác mà không cần tạo thêm bảng mã mới. Phải
-- tạo trước product vì product.category_code tham chiếu khoá ngoại tới đây.
-- ----------------------------------------------------------------------------
CREATE TABLE category (
  category_code    VARCHAR(20)   NOT NULL COMMENT 'Mã danh mục (khoá chính, bảng mã dùng chung nhiều nghiệp vụ) - tự sinh dạng DM+yyyyMMdd+4 số (xem seq_no/SeqNoUtility)',
  category_type    VARCHAR(20)   NOT NULL COMMENT 'Loại danh mục - phân biệt bảng này đang phục vụ nghiệp vụ nào (vd PRODUCT = danh mục sản phẩm)',
  name             VARCHAR(100)  NOT NULL COMMENT 'Tên danh mục',
  display_order    INT(9)        NOT NULL DEFAULT 0 COMMENT 'Thứ tự hiển thị',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (category_code),
  KEY idx_category_type (category_type)
);

-- ----------------------------------------------------------------------------
-- product
-- ----------------------------------------------------------------------------
CREATE TABLE product (
  product_code          VARCHAR(100)   NOT NULL COMMENT 'Mã sản phẩm (khoá chính)',
  name                  VARCHAR(100)   NOT NULL COMMENT 'Tên sản phẩm',
  short_name            VARCHAR(50)    NULL COMMENT 'Tên rút gọn sản phẩm',
  barcode               VARCHAR(14)    NULL COMMENT 'Mã vạch sản phẩm',
  category_code         VARCHAR(20)    NULL COMMENT 'Mã danh mục sản phẩm',
  unit_name             VARCHAR(20)    NULL DEFAULT '0' COMMENT 'Đơn vị tính (cái, kg, thùng...)',
  reduced_tax_rate_flg  VARCHAR(1)     NULL DEFAULT '0' COMMENT 'Cờ áp dụng thuế suất ưu đãi: 1=có, 0=không; quy tắc cụ thể: UNKNOWN',
  price                 DECIMAL(12,2)  NOT NULL DEFAULT 0 COMMENT 'Giá bán cố định của sản phẩm',
  min_stock_qty         INT(9)         NOT NULL DEFAULT 0 COMMENT 'Định mức tồn tối thiểu (số lượng) - dùng để xác định sản phẩm dưới định mức tồn (cần nhập thêm); 0 = chưa cấu hình định mức. Áp dụng chung mọi chi nhánh, chưa hỗ trợ định mức riêng theo chi nhánh.',
  expiry_warning_days   INT(9)         NOT NULL DEFAULT 90 COMMENT 'Số ngày cảnh báo trước hạn sử dụng (dùng cho cảnh báo hàng sắp hết hạn sau này) - mặc định 90 ngày.',
  del_flg               VARCHAR(1)     NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code       VARCHAR(8)     NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime        TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program         VARCHAR(10)    NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code      VARCHAR(8)     NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime       TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program        VARCHAR(10)    NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (product_code),
  KEY idx_product_barcode (barcode),
  CONSTRAINT fk_product_category FOREIGN KEY (category_code) REFERENCES category (category_code)
);

-- ----------------------------------------------------------------------------
-- supplier
-- ----------------------------------------------------------------------------
CREATE TABLE supplier (
  supplier_code    VARCHAR(20)   NOT NULL COMMENT 'Mã nhà cung cấp (khoá chính)',
  name             VARCHAR(80)   NOT NULL COMMENT 'Tên đầy đủ nhà cung cấp',
  short_name       VARCHAR(40)   NOT NULL COMMENT 'Tên rút gọn nhà cung cấp',
  zip_code         VARCHAR(8)    NULL COMMENT 'Mã bưu chính',
  address1         VARCHAR(100)  NULL COMMENT 'Địa chỉ - dòng 1',
  address2         VARCHAR(100)  NULL COMMENT 'Địa chỉ - dòng 2',
  address3         VARCHAR(100)  NULL COMMENT 'Địa chỉ - dòng 3',
  tel              VARCHAR(14)   NULL COMMENT 'Số điện thoại',
  fax              VARCHAR(14)   NULL COMMENT 'Số fax',
  contact_name     VARCHAR(20)   NULL COMMENT 'Tên người liên hệ',
  email            VARCHAR(100)  NULL COMMENT 'Địa chỉ email',
  note             VARCHAR(100)  NULL COMMENT 'Ghi chú',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (supplier_code)
);

-- ----------------------------------------------------------------------------
-- product_supplier — quan hệ NHIỀU-NHIỀU sản phẩm <-> nhà cung cấp (1 sản
-- phẩm có thể lấy từ nhiều NCC khác nhau, mỗi NCC có mã hàng riêng + giá
-- mua riêng). KHÔNG có khái niệm "NCC chính" — danh sách ngang hàng. Theo
-- đúng tiền lệ bảng quan hệ nhiều-nhiều đã có (function_permission): khoá
-- chính GHÉP, đủ audit column, KHÔNG có del_flg (xoá quan hệ = xoá thẳng
-- dòng, không xoá mềm).
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

-- ----------------------------------------------------------------------------
-- branch
-- ----------------------------------------------------------------------------
CREATE TABLE branch (
  branch_code      VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh/cửa hàng (khoá chính)',
  name             VARCHAR(40)   NOT NULL COMMENT 'Tên chi nhánh',
  short_name       VARCHAR(20)   NULL COMMENT 'Tên rút gọn chi nhánh',
  zip_code         VARCHAR(8)    NOT NULL COMMENT 'Mã bưu chính',
  address1         VARCHAR(100)  NOT NULL COMMENT 'Địa chỉ - dòng 1',
  address2         VARCHAR(100)  NOT NULL COMMENT 'Địa chỉ - dòng 2',
  address3         VARCHAR(100)  NULL COMMENT 'Địa chỉ - dòng 3',
  tel              VARCHAR(14)   NULL COMMENT 'Số điện thoại',
  fax              VARCHAR(14)   NULL COMMENT 'Số fax',
  manager_name     VARCHAR(20)   NULL COMMENT 'Tên quản lý chi nhánh',
  contact_name     VARCHAR(20)   NULL COMMENT 'Tên người liên hệ',
  note             VARCHAR(100)  NULL COMMENT 'Ghi chú',
  start_date       VARCHAR(8)    NULL COMMENT 'Ngày bắt đầu hoạt động (định dạng YYYYMMDD)',
  end_date         VARCHAR(8)    NULL COMMENT 'Ngày ngừng hoạt động (định dạng YYYYMMDD)',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (branch_code)
);

-- ----------------------------------------------------------------------------
-- bank_account — tài khoản ngân hàng nhận tiền theo chi nhánh, dùng để build
-- mã QR chuyển khoản (chuẩn EMVCo/Napas247) lúc in hoá đơn POS. 1 chi nhánh
-- hiện chỉ có 1 tài khoản nhận tiền chính (PK = branch_code) — đủ dùng cho
-- quy mô hiện tại, mở rộng thêm cột/bảng phụ nếu sau này cần nhiều tài
-- khoản/chi nhánh. Xem docs/pos-in-hoa-don.md (gốc workspace) để biết đầy đủ
-- thiết kế luồng in.
-- ----------------------------------------------------------------------------
CREATE TABLE bank_account (
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

-- ----------------------------------------------------------------------------
-- stock — theo dõi tồn kho theo (branch_code, product_code), không theo dõi
-- vị trí kho vật lý chi tiết (đơn giản hoá cho quy mô 1 cửa hàng nhỏ).
-- ----------------------------------------------------------------------------
CREATE TABLE stock (
  branch_code      VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh (một phần khoá chính)',
  product_code     VARCHAR(100)  NOT NULL COMMENT 'Mã sản phẩm (một phần khoá chính)',
  quality_code     VARCHAR(2)    NOT NULL DEFAULT '01' COMMENT 'Mã phẩm cấp/tình trạng hàng (01=hàng thường)',
  expiry_date      DATE          NULL COMMENT 'Hạn sử dụng (nếu có)',
  stock_qty        INT(9)        NOT NULL DEFAULT 0 COMMENT 'Số lượng tồn kho thực tế',
  available_qty    INT(9)        NOT NULL DEFAULT 0 COMMENT 'Số lượng tồn khả dụng (có thể bán)',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (branch_code, product_code),
  CONSTRAINT fk_stock_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code),
  CONSTRAINT fk_stock_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);

-- ----------------------------------------------------------------------------
-- inbound_receipt + inbound_receipt_item — nhập hàng (chỉ ghi nhận thực
-- nhận, không có luồng lập kế hoạch nhập hàng).
-- ----------------------------------------------------------------------------
CREATE TABLE inbound_receipt (
  branch_code           VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh nhận hàng (một phần khoá chính)',
  receipt_no            VARCHAR(16)   NOT NULL COMMENT 'Số phiếu nhập hàng (một phần khoá chính)',
  supplier_code         VARCHAR(20)   NULL COMMENT 'Mã nhà cung cấp',
  planned_arrival_date  DATE          NULL COMMENT 'Ngày dự kiến hàng về',
  actual_arrival_date   DATE          NULL COMMENT 'Ngày hàng thực tế về',
  receipt_date          DATE          NULL COMMENT 'Ngày lập phiếu nhập',
  receipt_user_code     VARCHAR(8)    NULL COMMENT 'Mã người dùng lập phiếu nhập',
  note                  VARCHAR(200)  NULL COMMENT 'Ghi chú',
  einvoice_no           VARCHAR(20)   NULL COMMENT 'Số hoá đơn điện tử do NCC cung cấp (nếu có)',
  einvoice_series       VARCHAR(20)   NULL COMMENT 'Ký hiệu mẫu số hoá đơn điện tử (nếu có)',
  einvoice_issue_date   DATE          NULL COMMENT 'Ngày phát hành hoá đơn điện tử (nếu có)',
  einvoice_lookup_code  VARCHAR(50)   NULL COMMENT 'Mã tra cứu hoá đơn điện tử trên cổng NCC/Tổng cục Thuế (nếu có)',
  einvoice_url          VARCHAR(500)  NULL COMMENT 'Đường dẫn tra cứu/xem hoá đơn điện tử trên cổng NCC/Tổng cục Thuế (nếu có) - KHÔNG phải file upload, chỉ lưu link tham chiếu',
  del_flg               VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code       VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program         VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code      VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime       TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program        VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (branch_code, receipt_no),
  CONSTRAINT fk_inboundreceipt_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code),
  CONSTRAINT fk_inboundreceipt_supplier FOREIGN KEY (supplier_code) REFERENCES supplier (supplier_code)
);

CREATE TABLE inbound_receipt_item (
  branch_code      VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh (một phần khoá chính, tham chiếu phiếu nhập)',
  receipt_no       VARCHAR(16)   NOT NULL COMMENT 'Số phiếu nhập hàng (một phần khoá chính, tham chiếu phiếu nhập)',
  line_no          INT(3)        NOT NULL COMMENT 'Số thứ tự dòng trong phiếu nhập (một phần khoá chính)',
  product_code     VARCHAR(100)  NOT NULL COMMENT 'Mã sản phẩm',
  quality_code     VARCHAR(2)    NOT NULL DEFAULT '01' COMMENT 'Mã phẩm cấp/tình trạng hàng (01=hàng thường)',
  expiry_date      DATE          NULL COMMENT 'Hạn sử dụng (nếu có)',
  planned_qty      INT(9)        NOT NULL DEFAULT 0 COMMENT 'Số lượng dự kiến nhập',
  actual_qty       INT(9)        NOT NULL DEFAULT 0 COMMENT 'Số lượng thực nhận',
  unit_cost        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'Đơn giá vốn nhập hàng',
  note             VARCHAR(200)  NULL COMMENT 'Ghi chú',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (branch_code, receipt_no, line_no),
  CONSTRAINT fk_inboundreceiptitem_hed FOREIGN KEY (branch_code, receipt_no) REFERENCES inbound_receipt (branch_code, receipt_no),
  CONSTRAINT fk_inboundreceiptitem_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);

-- ----------------------------------------------------------------------------
-- app_user
-- ----------------------------------------------------------------------------
CREATE TABLE app_user (
  user_code         VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng (khoá chính)',
  name              VARCHAR(20)   NOT NULL COMMENT 'Họ tên người dùng',
  password_hash     VARCHAR(255)  NOT NULL COMMENT 'Mật khẩu đã băm PBKDF2WithHmacSHA256 (xem PasswordUtility), không lưu plaintext',
  main_branch_code  VARCHAR(6)    NULL COMMENT 'Mã chi nhánh làm việc chính',
  note              VARCHAR(100)  NULL COMMENT 'Ghi chú',
  del_flg           VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code   VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program     VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (user_code)
);

-- ----------------------------------------------------------------------------
-- app_function + function_permission — phân quyền theo chức năng, không đa
-- ngôn ngữ, không qua bảng trung gian nào (mỗi Process tự khai function_code
-- qua getFuncId()).
-- ----------------------------------------------------------------------------
CREATE TABLE app_function (
  function_code     VARCHAR(10)   NOT NULL COMMENT 'Mã chức năng (khoá chính)',
  name              VARCHAR(40)   NOT NULL COMMENT 'Tên chức năng',
  short_name        VARCHAR(20)   NOT NULL COMMENT 'Tên rút gọn chức năng',
  menu_show_flg     VARCHAR(1)    NOT NULL DEFAULT '1' COMMENT 'Cờ hiển thị trên menu: 1=hiện, 0=ẩn',
  auth_required_flg VARCHAR(1)    NOT NULL DEFAULT '1' COMMENT 'Cờ yêu cầu kiểm tra quyền: 1=có yêu cầu, 0=không yêu cầu',
  note              VARCHAR(200)  NULL COMMENT 'Ghi chú',
  del_flg           VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code   VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program     VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (function_code)
);

CREATE TABLE function_permission (
  user_code        VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng (một phần khoá chính)',
  function_code    VARCHAR(10)   NOT NULL COMMENT 'Mã chức năng (một phần khoá chính)',
  auth_type        VARCHAR(1)    NOT NULL DEFAULT '1' COMMENT '1=được phép, 0=không được phép',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (user_code, function_code),
  CONSTRAINT fk_funcperm_user FOREIGN KEY (user_code) REFERENCES app_user (user_code),
  CONSTRAINT fk_funcperm_function FOREIGN KEY (function_code) REFERENCES app_function (function_code)
);

-- ----------------------------------------------------------------------------
-- session_token — lưu token phiên đăng nhập cho AuthTokenFilter.
-- ----------------------------------------------------------------------------
CREATE TABLE session_token (
  token            VARCHAR(255)  NOT NULL COMMENT 'Chuỗi token phiên đăng nhập (khoá chính)',
  user_code        VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng sở hữu phiên đăng nhập',
  expire_datetime  DATETIME      NOT NULL COMMENT 'Thời điểm token hết hạn',
  created_datetime TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm phát hành token',
  PRIMARY KEY (token),
  CONSTRAINT fk_sessiontoken_user FOREIGN KEY (user_code) REFERENCES app_user (user_code)
);

-- ============================================================================
-- Quy tắc nghiệp vụ cụ thể (thuế, làm tròn tiền, điều kiện khuyến mãi chồng
-- nhau, công thức doanh thu chi tiết) giữ UNKNOWN cho tới khi có yêu cầu rõ.
-- ============================================================================

-- Khách hàng mua lẻ tại quầy.
CREATE TABLE customer (
  customer_code    VARCHAR(20)   NOT NULL COMMENT 'Mã khách hàng (khoá chính)',
  name             VARCHAR(100)  NOT NULL COMMENT 'Tên khách hàng',
  phone            VARCHAR(20)   NULL COMMENT 'Số điện thoại',
  email            VARCHAR(100)  NULL COMMENT 'Địa chỉ email',
  note             VARCHAR(255)  NULL COMMENT 'Ghi chú',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (customer_code)
);

-- Khuyến mãi — CHỈ KHUNG. Quy tắc áp dụng/chồng khuyến mãi: UNKNOWN.
CREATE TABLE promotion (
  promotion_code   VARCHAR(20)   NOT NULL COMMENT 'Mã khuyến mãi (khoá chính)',
  name             VARCHAR(100)  NOT NULL COMMENT 'Tên chương trình khuyến mãi',
  discount_type    VARCHAR(20)   NOT NULL COMMENT 'Loại giảm giá: percent (theo %) | amount (theo số tiền cố định); quy tắc áp dụng cụ thể: UNKNOWN',
  discount_value   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Giá trị giảm giá (theo % hoặc số tiền tuỳ discount_type)',
  start_date       DATE          NULL COMMENT 'Ngày bắt đầu áp dụng',
  end_date         DATE          NULL COMMENT 'Ngày kết thúc áp dụng',
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ xoá mềm: 1=đã xoá, 0=còn hiệu lực',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (promotion_code)
);

-- Đơn bán tại quầy (checkout POS thật — thay cho alert() trong pos.component.ts)
CREATE TABLE sale_order (
  sale_order_no     VARCHAR(20)   NOT NULL COMMENT 'Số đơn bán hàng (khoá chính)',
  branch_code       VARCHAR(6)    NOT NULL COMMENT 'Mã chi nhánh bán hàng',
  customer_code     VARCHAR(20)   NULL COMMENT 'Mã khách hàng (có thể trống nếu khách lẻ không lưu thông tin)',
  customer_name     VARCHAR(100)  NULL COMMENT 'Tên khách hàng ghi tự do lúc bán (chưa có màn quản lý khách hàng nên KHÔNG qua customer_code)',
  sale_datetime     DATETIME      NOT NULL COMMENT 'Thời điểm bán hàng',
  paid_amount       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Số tiền khách thanh toán',
  change_amount     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Số tiền thối lại cho khách',
  payment_method    VARCHAR(10)   NOT NULL DEFAULT 'CASH' COMMENT 'Phương thức thanh toán: CASH=tiền mặt, TRANSFER=chuyển khoản',
  cashier_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã thu ngân thực hiện đơn',
  void_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Cờ đơn bị huỷ (thay cho xoá cứng): 1=đã huỷ, 0=còn hiệu lực',
  entry_user_code   VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program     VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (sale_order_no),
  CONSTRAINT fk_saleorder_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code),
  CONSTRAINT fk_saleorder_customer FOREIGN KEY (customer_code) REFERENCES customer (customer_code)
);

CREATE TABLE sale_order_item (
  sale_order_no    VARCHAR(20)   NOT NULL COMMENT 'Số đơn bán hàng (một phần khoá chính, tham chiếu đơn bán)',
  line_no          INT(3)        NOT NULL COMMENT 'Số thứ tự dòng trong đơn bán (một phần khoá chính)',
  product_code     VARCHAR(100)  NOT NULL COMMENT 'Mã sản phẩm',
  unit_price       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Đơn giá bán tại thời điểm giao dịch',
  quantity         INT(9)        NOT NULL DEFAULT 1 COMMENT 'Số lượng bán',
  line_amount      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Thành tiền của dòng (đơn giá × số lượng)',
  unit_cost        DECIMAL(12,2) NULL COMMENT 'Giá vốn BÌNH QUÂN GIA QUYỀN của sản phẩm tại THỜI ĐIỂM bán (chụp lại lúc tạo đơn, tính từ SUM(actual_qty*unit_cost)/SUM(actual_qty) trên inbound_receipt_item theo branch_code — xem SaleOrderCreateProcess). NULL = sản phẩm CHƯA TỪNG có phiếu nhập nào tính đến lúc bán, KHÔNG xác định được giá vốn (không phải giá vốn = 0) — đơn tạo TRƯỚC khi field này ra đời cũng NULL, không backfill tự động.',
  entry_user_code  VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng tạo bản ghi',
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo bản ghi',
  entry_program    VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình tạo bản ghi',
  update_user_code VARCHAR(8)    NOT NULL COMMENT 'Mã người dùng cập nhật gần nhất',
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật gần nhất',
  update_program   VARCHAR(10)   NOT NULL COMMENT 'Mã chương trình cập nhật gần nhất',
  PRIMARY KEY (sale_order_no, line_no),
  CONSTRAINT fk_saleitem_hed FOREIGN KEY (sale_order_no) REFERENCES sale_order (sale_order_no),
  CONSTRAINT fk_saleitem_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);

-- Báo cáo doanh thu — khung tổng hợp cơ bản. Công thức chi tiết hơn (theo ca,
-- theo nhân viên, trừ khuyến mãi...) giữ UNKNOWN cho tới khi có yêu cầu.
CREATE OR REPLACE VIEW v_daily_revenue AS
SELECT
  so.branch_code,
  DATE(so.sale_datetime) AS sale_date,
  SUM(soi.line_amount)   AS total_revenue,
  COUNT(DISTINCT so.sale_order_no) AS order_count
FROM sale_order so
JOIN sale_order_item soi ON soi.sale_order_no = so.sale_order_no
WHERE so.void_flg = '0'
GROUP BY so.branch_code, DATE(so.sale_datetime);

CREATE OR REPLACE VIEW v_item_revenue AS
SELECT
  so.branch_code,
  soi.product_code,
  DATE(so.sale_datetime) AS sale_date,
  SUM(soi.quantity)      AS total_quantity,
  SUM(soi.line_amount)   AS total_revenue
FROM sale_order so
JOIN sale_order_item soi ON soi.sale_order_no = so.sale_order_no
WHERE so.void_flg = '0'
GROUP BY so.branch_code, soi.product_code, DATE(so.sale_datetime);
