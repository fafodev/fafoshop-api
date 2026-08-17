package fafoshop.pos.inboundreceipt.process;

import java.sql.ResultSet;
import java.sql.SQLException;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.ProcessCheckErrorException;

/**
 * Kiểm tra quyền sửa/huỷ phiếu nhập — mirror SaleOrderEditGuard y hệt (đơn
 * còn hiệu lực + đúng người tạo/trong 15 phút, HOẶC có quyền `INBND_MGR`).
 * Không đạt → ME000125 (TÁI DÙNG đúng mã "không tìm thấy phiếu nhập", cùng
 * ý nghĩa "không tìm thấy hoặc không có quyền", không lộ chi tiết lý do).
 */
final class InboundReceiptEditGuard {

	private static final int EDIT_WINDOW_MINUTES = 15;

	private static final String MGR_FUNC_ID = "INBND_MGR";

	private InboundReceiptEditGuard() {
	}

	/** Trả về branch_code của phiếu nếu đủ điều kiện sửa/huỷ, ném lỗi nghiệp vụ nếu không. */
	static String resolveEligibleBranchCode(DBAccessor dba, String receiptNo, String userCode)
			throws DBException, ProcessCheckErrorException {

		boolean isManager = hasFunctionCode(dba, userCode, MGR_FUNC_ID);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT branch_code FROM inbound_receipt WHERE receipt_no = ? AND void_flg = '0' ");
			if (!isManager) {
				sql.append("AND receipt_user_code = ? ");
				sql.append("AND entry_datetime >= (NOW() - INTERVAL ").append(EDIT_WINDOW_MINUTES).append(" MINUTE) ");
			}

			ps = dba.prepareStatement(sql.toString());
			ps.setString(1, receiptNo);
			if (!isManager) {
				ps.setString(2, userCode);
			}
			rs = ps.executeQuery();

			if (!rs.next()) {
				InboundReceiptQueryHelper.throwError("ME000125");
			}
			return rs.getString("branch_code");

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}

	private static boolean hasFunctionCode(DBAccessor dba, String userCode, String functionCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COUNT(*) AS cnt FROM function_permission "
					+ "WHERE user_code = ? AND function_code = ? AND auth_type = '1'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			ps.setString(2, functionCode);
			rs = ps.executeQuery();
			return rs.next() && rs.getInt("cnt") > 0;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}
}
