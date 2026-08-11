# Chuẩn Sinh Mã Quản Lý Tự Động (seq_no / SeqNoUtility)

Đây là CHUẨN CHUNG bắt buộc cho MỌI mã quản lý tự sinh trong hệ thống
(category_code, supplier_code, product_code, và mọi mã tự sinh khác sẽ có
sau này) — không được tự chế cơ chế sinh mã riêng (timestamp, UUID, đếm tay
trong code Java...) cho tính năng mới.

## Bối cảnh quyết định

Trước khi có chuẩn này, mỗi module tự sinh mã một kiểu khác nhau:
`product_code` = `"PRD" + CommonUtility.compactTimestamp()` (timestamp
`yyyyMMddHHmmssSSS`), `supplier_code` tương tự với prefix `"SUP"`,
`category_code` thì NHẬP TAY (cột chỉ `VARCHAR(4)`, không đủ chỗ cho mã tự
sinh). Người dùng cung cấp 1 bảng sinh số tham khảo từ hệ thống khác
(`PREFIX`/`SEQNO`/`MAXDIGIT`/audit columns kiểu Hungarian-notation) và yêu
cầu áp dụng thống nhất — bảng `seq_no` trong `db/schema.sql` viết LẠI theo
đúng quy ước đặt tên/audit column của `fafoshop_pos` (snake_case), KHÔNG
copy nguyên bảng tham khảo (đã bỏ các cột `NUMRSRV1-10` không dùng tới).

## Định dạng mã

```
<PREFIX><yyyyMMdd><SEQNO đệm 0 bên trái đủ max_digit chữ số>
```

Ví dụ: `NCC202607240001`, `DM202607240001`, `SP202607240001`.

- `PREFIX` — tiền tố đăng ký sẵn trong bảng `seq_no` (khoá chính, tối đa 4
  ký tự), nhận diện loại mã.
- Ngày ghép vào mã là ngày **lúc sinh mã** (`LocalDate.now()`), chỉ để dễ
  đọc/tra cứu bằng mắt — **KHÔNG** phải điều kiện reset số. `seq_no` (cột
  `seq_no.seq_no`) tăng dần LIÊN TỤC theo prefix, không bao giờ reset về 0
  theo ngày/tháng/năm — khớp đúng hành vi của bảng tham khảo gốc.
- `max_digit` (mặc định 4) — số chữ số đệm `0` bên trái. Nếu `seq_no` vượt
  quá số chữ số này (vd sinh tới lần thứ 10000 cho 1 prefix có
  `max_digit=4`), mã vẫn in ĐỦ chữ số thật (`10000`), không cắt bớt — chấp
  nhận mã dài hơn dự kiến còn hơn mất tính duy nhất.

## Bảng `seq_no`

`db/schema.sql` (bảng gốc cho cài đặt mới) + migration
`db/migration_add_seqno_and_widen_category_code.sql` (áp cho DB dev đã có
sẵn dữ liệu) — cột: `prefix` (PK), `seq_no`, `max_digit`, `description`, đủ
6 cột audit chuẩn (`entry_user_code/entry_datetime/entry_program/
update_user_code/update_datetime/update_program`).

**Prefix đã đăng ký** (seed ở `db/seed_dev.sql`, xem trước khi thêm prefix
mới để tránh trùng):

| Prefix | Dùng cho | max_digit |
|---|---|---|
| `NCC` | `supplier.supplier_code` | 4 |
| `DM` | `category.category_code` | 4 |
| `SP` | `product.product_code` | 4 |
| `HD` | `sale_order.sale_order_no` | 4 |
| `PN` | `inbound_receipt.receipt_no` | 4 |

Thêm module mới cần mã tự sinh → thêm 1 dòng `INSERT ... INTO seq_no` mới
(prefix ngắn gọn viết HOA, dễ nhận diện, tối đa 4 ký tự) vào
`db/seed_dev.sql`, KHÔNG tái dùng prefix đã có cho mục đích khác.

## Dùng trong Process — `SeqNoUtility`

`fafoshop.common.utility.SeqNoUtility` (`src/main/java/fafoshop/common/
utility/SeqNoUtility.java`):

```java
String categoryCode = SeqNoUtility.generate(dba, "DM", req.accessInfo.userCode, PRG_CD);
```

- Tham số `dba` là `DBAccessor` CỦA Process gọi tới (không tự mở connection
  riêng) — việc cấp số nằm CHUNG transaction với câu `INSERT` tạo bản ghi
  chính, nên nếu transaction rollback thì số vừa cấp cũng rollback theo
  (chấp nhận có khoảng trống số nếu process fail sau khi đã cấp số nhưng
  trước khi rollback commit — đây là đánh đổi bình thường của kiểu sequence
  generator này, không phải lỗi).
- An toàn dưới tải đồng thời nhờ thủ thuật MySQL
  `UPDATE seq_no SET seq_no = LAST_INSERT_ID(seq_no + 1) WHERE prefix = ?`
  rồi `SELECT LAST_INSERT_ID()` trên CÙNG connection — UPDATE khoá đúng 1
  dòng (theo prefix) tới khi Process gọi tới commit/rollback, đi đúng khung
  retry-deadlock có sẵn của `AbstractProcess` (xem `architecture.md`).
- Nếu gọi với prefix CHƯA seed trong `seq_no` → ném `FatalException` (lỗi
  cấu hình, không phải lỗi nghiệp vụ người dùng — không dùng
  `ProcessCheckErrorException`).

## Khi thêm module mới cần mã tự sinh

1. Kiểm tra độ rộng cột lưu mã đủ chỗ: `<prefix ngắn nhất/dài nhất>` + 8 số
   ngày + `max_digit` số — vd prefix 3 ký tự + `max_digit=4` cần tối thiểu
   `VARCHAR(15)`, nên dùng `VARCHAR(20)` cho dư (khớp `supplier_code`/
   `category_code` hiện tại).
2. Thêm 1 dòng seed vào `seq_no` trong `db/seed_dev.sql` (và
   `db/schema.sql` nếu ảnh hưởng bảng gốc).
3. Trong `XxxCreateProcess.process()`, gọi `SeqNoUtility.generate(dba,
   "PREFIX", req.accessInfo.userCode, PRG_CD)` để lấy mã, KHÔNG nhận mã từ
   client request.
4. KHÔNG cần validate uniqueness/format cho mã ở tầng validator — mã đã
   đảm bảo duy nhất bởi cơ chế sinh số, không có input người dùng nào để
   validate nữa.

## Ngoại lệ đã biết

- Dữ liệu mẫu cũ nhập tay trước khi có chuẩn này (`category_code` như
  `TP01`, `DGD1`...) vẫn giữ nguyên, không migrate lại — cột đã nới rộng
  vẫn chấp nhận cả mã ngắn kiểu cũ lẫn mã tự sinh kiểu mới cùng tồn tại
  trong 1 bảng.
