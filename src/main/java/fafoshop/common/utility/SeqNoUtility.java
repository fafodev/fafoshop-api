package fafoshop.common.utility;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;

/**
 * Sinh mã quản lý tự động THEO CHUẨN CHUNG toàn hệ thống — xem
 * <code>.claude/seqno-convention.md</code> để biết đầy đủ quy ước, danh sách
 * prefix đã đăng ký (NCC/DM/SP...) và lý do thiết kế. MỌI mã tự sinh mới
 * (category_code, supplier_code, product_code...) PHẢI dùng utility này,
 * không tự chế cơ chế sinh mã riêng (timestamp, UUID...).
 *
 * Định dạng: PREFIX + ngày hiện tại (yyyyMMdd) + số thứ tự đệm '0' bên trái
 * đủ <code>max_digit</code> chữ số — vd "NCC202607240001". Số thứ tự lấy từ
 * bảng <code>seq_no</code>, TĂNG DẦN LIÊN TỤC theo prefix — KHÔNG reset mỗi
 * ngày (ngày chỉ in kèm cho dễ đọc/tra cứu, không phải điều kiện tăng số).
 *
 * Cấp số AN TOÀN dưới tải đồng thời bằng 1 câu UPDATE dùng thủ thuật
 * <code>LAST_INSERT_ID(expr)</code> của MySQL (gán giá trị session-scope,
 * đọc lại được qua <code>SELECT LAST_INSERT_ID()</code> trên CÙNG kết nối)
 * thay vì SELECT rồi UPDATE riêng — tránh race condition mà không cần
 * SELECT ... FOR UPDATE thủ công. Chạy trong CÙNG transaction/connection
 * (tham số dba) của Process gọi tới, nên UPDATE khoá đúng 1 dòng (theo
 * prefix) tới khi Process đó commit/rollback — đi đúng khung retry-deadlock
 * có sẵn của AbstractProcess (xem architecture.md).
 */
public final class SeqNoUtility {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private SeqNoUtility() {
	}

	/**
	 * @param dba         kết nối/transaction hiện tại của Process gọi tới (KHÔNG mở connection riêng)
	 * @param prefix      mã tiền tố đã đăng ký sẵn trong bảng seq_no (vd "NCC", "DM", "SP")
	 * @param userCode    accessInfo.userCode của Process gọi tới — ghi vào update_user_code
	 * @param programCode mã chương trình gọi tới (PRG_CD của Process) — ghi vào update_program
	 * @return mã đã ghép đủ: PREFIX + yyyyMMdd + số thứ tự đệm 0
	 */
	public static String generate(DBAccessor dba, String prefix, String userCode, String programCode)
			throws DBException, FatalException {

		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("UPDATE seq_no SET seq_no = LAST_INSERT_ID(seq_no + 1), "
					+ "update_user_code = ?, update_program = ? WHERE prefix = ?");
			ps.setString(1, userCode);
			ps.setString(2, programCode);
			ps.setString(3, prefix);
			int affected = ps.executeUpdate();

			if (affected == 0) {
				throw new FatalException(new IllegalStateException("Chưa cấu hình seq_no cho prefix '" + prefix
						+ "' - xem .claude/seqno-convention.md để thêm dòng cấu hình trước khi dùng."));
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}

		return fetchGeneratedCode(dba, prefix);
	}

	private static String fetchGeneratedCode(DBAccessor dba, String prefix) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT LAST_INSERT_ID() AS next_seq_no, max_digit FROM seq_no WHERE prefix = ?");
			ps.setString(1, prefix);
			rs = ps.executeQuery();
			rs.next();

			long nextSeqNo = rs.getLong("next_seq_no");
			int maxDigit = rs.getInt("max_digit");

			String datePart = LocalDate.now().format(DATE_FMT);
			String seqPart = String.format("%0" + maxDigit + "d", nextSeqNo);

			return prefix + datePart + seqPart;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private static void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
