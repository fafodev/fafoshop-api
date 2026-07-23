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
-- product
-- ----------------------------------------------------------------------------
CREATE TABLE product (
  product_code          VARCHAR(100)   NOT NULL,
  name                  VARCHAR(100)   NOT NULL,
  short_name            VARCHAR(50)    NULL,
  barcode               VARCHAR(14)    NULL,
  category_code         VARCHAR(4)     NULL,
  supplier_code         VARCHAR(20)    NULL,
  unit_name             VARCHAR(20)    NULL DEFAULT '0',
  reduced_tax_rate_flg  VARCHAR(1)     NULL DEFAULT '0' COMMENT 'quy tắc cụ thể: UNKNOWN',
  price                 DECIMAL(12,2)  NOT NULL DEFAULT 0 COMMENT 'giá bán',
  del_flg               VARCHAR(1)     NOT NULL DEFAULT '0',
  entry_user_code       VARCHAR(8)     NOT NULL,
  entry_datetime        TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program         VARCHAR(10)    NOT NULL,
  update_user_code      VARCHAR(8)     NOT NULL,
  update_datetime       TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program        VARCHAR(10)    NOT NULL,
  PRIMARY KEY (product_code),
  KEY idx_product_barcode (barcode),
  KEY idx_product_supplier_code (supplier_code)
);

-- ----------------------------------------------------------------------------
-- supplier
-- ----------------------------------------------------------------------------
CREATE TABLE supplier (
  supplier_code    VARCHAR(20)   NOT NULL,
  name             VARCHAR(80)   NOT NULL,
  short_name       VARCHAR(40)   NOT NULL,
  zip_code         VARCHAR(8)    NULL,
  address1         VARCHAR(100)  NULL,
  address2         VARCHAR(100)  NULL,
  address3         VARCHAR(100)  NULL,
  tel              VARCHAR(14)   NULL,
  fax              VARCHAR(14)   NULL,
  contact_name     VARCHAR(20)   NULL,
  email            VARCHAR(100)  NULL,
  note             VARCHAR(100)  NULL,
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (supplier_code)
);

-- ----------------------------------------------------------------------------
-- branch
-- ----------------------------------------------------------------------------
CREATE TABLE branch (
  branch_code      VARCHAR(6)    NOT NULL,
  name             VARCHAR(40)   NOT NULL,
  short_name       VARCHAR(20)   NULL,
  zip_code         VARCHAR(8)    NOT NULL,
  address1         VARCHAR(100)  NOT NULL,
  address2         VARCHAR(100)  NOT NULL,
  address3         VARCHAR(100)  NULL,
  tel              VARCHAR(14)   NULL,
  fax              VARCHAR(14)   NULL,
  manager_name     VARCHAR(20)   NULL,
  contact_name     VARCHAR(20)   NULL,
  note             VARCHAR(100)  NULL,
  start_date       VARCHAR(8)    NULL,
  end_date         VARCHAR(8)    NULL,
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (branch_code)
);

-- ----------------------------------------------------------------------------
-- stock — theo dõi tồn kho theo (branch_code, product_code), không theo dõi
-- vị trí kho vật lý chi tiết (đơn giản hoá cho quy mô 1 cửa hàng nhỏ).
-- ----------------------------------------------------------------------------
CREATE TABLE stock (
  branch_code      VARCHAR(6)    NOT NULL,
  product_code     VARCHAR(100)  NOT NULL,
  quality_code     VARCHAR(2)    NOT NULL DEFAULT '01',
  expiry_date      DATE          NULL,
  stock_qty        INT(9)        NOT NULL DEFAULT 0,
  available_qty    INT(9)        NOT NULL DEFAULT 0,
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (branch_code, product_code),
  CONSTRAINT fk_stock_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code),
  CONSTRAINT fk_stock_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);

-- ----------------------------------------------------------------------------
-- inbound_receipt + inbound_receipt_item — nhập hàng (chỉ ghi nhận thực
-- nhận, không có luồng lập kế hoạch nhập hàng).
-- ----------------------------------------------------------------------------
CREATE TABLE inbound_receipt (
  branch_code           VARCHAR(6)    NOT NULL,
  receipt_no            VARCHAR(16)   NOT NULL,
  supplier_code         VARCHAR(20)   NULL,
  planned_arrival_date  DATE          NULL,
  actual_arrival_date   DATE          NULL,
  receipt_date          DATE          NULL,
  receipt_user_code     VARCHAR(8)    NULL,
  note                  VARCHAR(200)  NULL,
  del_flg               VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code       VARCHAR(8)    NOT NULL,
  entry_datetime        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program         VARCHAR(10)   NOT NULL,
  update_user_code      VARCHAR(8)    NOT NULL,
  update_datetime       TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program        VARCHAR(10)   NOT NULL,
  PRIMARY KEY (branch_code, receipt_no),
  CONSTRAINT fk_inboundreceipt_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code),
  CONSTRAINT fk_inboundreceipt_supplier FOREIGN KEY (supplier_code) REFERENCES supplier (supplier_code)
);

CREATE TABLE inbound_receipt_item (
  branch_code      VARCHAR(6)    NOT NULL,
  receipt_no       VARCHAR(16)   NOT NULL,
  line_no          INT(3)        NOT NULL,
  product_code     VARCHAR(100)  NOT NULL,
  quality_code     VARCHAR(2)    NOT NULL DEFAULT '01',
  expiry_date      DATE          NULL,
  planned_qty      INT(9)        NOT NULL DEFAULT 0,
  actual_qty       INT(9)        NOT NULL DEFAULT 0,
  unit_cost        DECIMAL(10,2) NOT NULL DEFAULT 0,
  note             VARCHAR(200)  NULL,
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (branch_code, receipt_no, line_no),
  CONSTRAINT fk_inboundreceiptitem_hed FOREIGN KEY (branch_code, receipt_no) REFERENCES inbound_receipt (branch_code, receipt_no),
  CONSTRAINT fk_inboundreceiptitem_product FOREIGN KEY (product_code) REFERENCES product (product_code)
);

-- ----------------------------------------------------------------------------
-- app_user
-- ----------------------------------------------------------------------------
CREATE TABLE app_user (
  user_code         VARCHAR(8)    NOT NULL,
  name              VARCHAR(20)   NOT NULL,
  password_hash     VARCHAR(255)  NOT NULL COMMENT 'băm PBKDF2WithHmacSHA256 (xem PasswordUtility), không lưu plaintext',
  main_branch_code  VARCHAR(6)    NULL,
  note              VARCHAR(100)  NULL,
  del_flg           VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code   VARCHAR(8)    NOT NULL,
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program     VARCHAR(10)   NOT NULL,
  update_user_code  VARCHAR(8)    NOT NULL,
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program    VARCHAR(10)   NOT NULL,
  PRIMARY KEY (user_code)
);

-- ----------------------------------------------------------------------------
-- app_function + function_permission — phân quyền theo chức năng, không đa
-- ngôn ngữ, không qua bảng trung gian nào (mỗi Process tự khai function_code
-- qua getFuncId()).
-- ----------------------------------------------------------------------------
CREATE TABLE app_function (
  function_code     VARCHAR(10)   NOT NULL,
  name              VARCHAR(40)   NOT NULL,
  short_name        VARCHAR(20)   NOT NULL,
  menu_show_flg     VARCHAR(1)    NOT NULL DEFAULT '1',
  auth_required_flg VARCHAR(1)    NOT NULL DEFAULT '1',
  note              VARCHAR(200)  NULL,
  del_flg           VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code   VARCHAR(8)    NOT NULL,
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program     VARCHAR(10)   NOT NULL,
  update_user_code  VARCHAR(8)    NOT NULL,
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program    VARCHAR(10)   NOT NULL,
  PRIMARY KEY (function_code)
);

CREATE TABLE function_permission (
  user_code        VARCHAR(8)    NOT NULL,
  function_code    VARCHAR(10)   NOT NULL,
  auth_type        VARCHAR(1)    NOT NULL DEFAULT '1' COMMENT '1=được phép, 0=không',
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (user_code, function_code),
  CONSTRAINT fk_funcperm_user FOREIGN KEY (user_code) REFERENCES app_user (user_code),
  CONSTRAINT fk_funcperm_function FOREIGN KEY (function_code) REFERENCES app_function (function_code)
);

-- ----------------------------------------------------------------------------
-- session_token — lưu token phiên đăng nhập cho AuthTokenFilter.
-- ----------------------------------------------------------------------------
CREATE TABLE session_token (
  token            VARCHAR(255)  NOT NULL,
  user_code        VARCHAR(8)    NOT NULL,
  expire_datetime  DATETIME      NOT NULL,
  created_datetime TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (token),
  CONSTRAINT fk_sessiontoken_user FOREIGN KEY (user_code) REFERENCES app_user (user_code)
);

-- ============================================================================
-- Quy tắc nghiệp vụ cụ thể (thuế, làm tròn tiền, điều kiện khuyến mãi chồng
-- nhau, công thức doanh thu chi tiết) giữ UNKNOWN cho tới khi có yêu cầu rõ.
-- ============================================================================

-- Khách hàng mua lẻ tại quầy.
CREATE TABLE customer (
  customer_code    VARCHAR(20)   NOT NULL,
  name             VARCHAR(100)  NOT NULL,
  phone            VARCHAR(20)   NULL,
  email            VARCHAR(100)  NULL,
  note             VARCHAR(255)  NULL,
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (customer_code)
);

-- Khuyến mãi — CHỈ KHUNG. Quy tắc áp dụng/chồng khuyến mãi: UNKNOWN.
CREATE TABLE promotion (
  promotion_code   VARCHAR(20)   NOT NULL,
  name             VARCHAR(100)  NOT NULL,
  discount_type    VARCHAR(20)   NOT NULL COMMENT 'percent | amount — quy tắc cụ thể: UNKNOWN',
  discount_value   DECIMAL(12,2) NOT NULL DEFAULT 0,
  start_date       DATE          NULL,
  end_date         DATE          NULL,
  del_flg          VARCHAR(1)    NOT NULL DEFAULT '0',
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
  PRIMARY KEY (promotion_code)
);

-- Đơn bán tại quầy (checkout POS thật — thay cho alert() trong pos.component.ts)
CREATE TABLE sale_order (
  sale_order_no     VARCHAR(20)   NOT NULL,
  branch_code       VARCHAR(6)    NOT NULL,
  customer_code     VARCHAR(20)   NULL,
  sale_datetime     DATETIME      NOT NULL,
  paid_amount       DECIMAL(12,2) NOT NULL DEFAULT 0,
  change_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
  cashier_user_code VARCHAR(8)    NOT NULL,
  void_flg          VARCHAR(1)    NOT NULL DEFAULT '0' COMMENT 'Đơn bị huỷ (thay cho xoá cứng)',
  entry_user_code   VARCHAR(8)    NOT NULL,
  entry_datetime    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program     VARCHAR(10)   NOT NULL,
  update_user_code  VARCHAR(8)    NOT NULL,
  update_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program    VARCHAR(10)   NOT NULL,
  PRIMARY KEY (sale_order_no),
  CONSTRAINT fk_saleorder_branch FOREIGN KEY (branch_code) REFERENCES branch (branch_code),
  CONSTRAINT fk_saleorder_customer FOREIGN KEY (customer_code) REFERENCES customer (customer_code)
);

CREATE TABLE sale_order_item (
  sale_order_no    VARCHAR(20)   NOT NULL,
  line_no          INT(3)        NOT NULL,
  product_code     VARCHAR(100)  NOT NULL,
  unit_price       DECIMAL(12,2) NOT NULL DEFAULT 0,
  quantity         INT(9)        NOT NULL DEFAULT 1,
  line_amount      DECIMAL(12,2) NOT NULL DEFAULT 0,
  entry_user_code  VARCHAR(8)    NOT NULL,
  entry_datetime   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  entry_program    VARCHAR(10)   NOT NULL,
  update_user_code VARCHAR(8)    NOT NULL,
  update_datetime  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  update_program   VARCHAR(10)   NOT NULL,
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
