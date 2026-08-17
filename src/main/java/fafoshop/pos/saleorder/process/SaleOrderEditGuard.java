package fafoshop.pos.saleorder.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;

/**
 * Kiểm tra quyền sửa/huỷ đơn bán — dùng CHUNG cho SaleOrderUpdateProcess và
 * SaleOrderVoidProcess (mirror SaleOrderUpdatePaymentMethodProcess đã có,
 * xem docs/pos-sua-huy-don.md quyết định #3):
 *
 * - Đơn còn hiệu lực (void_flg='0').
 * - VÀ (đúng người tạo + trong khung giờ ngắn, HOẶC user có quyền
 *   `SALE_MGR` — bỏ qua ràng buộc người tạo/thời gian).
 *
 * Không đạt → coi là "không tìm thấy đơn hợp lệ" (ME000099, TÁI DÙNG đúng
 * mã đã có ở SaleOrderUpdatePaymentMethodProcess — cùng ý nghĩa, không phải
 * lỗi trùng khoá kiểu ME000100 đã từng gặp) — không phân biệt lý do cụ thể
 * trong thông báo, tránh lộ thông tin đơn của người khác.
 */
final class SaleOrderEditGuard {

	/** Số phút tối đa cho phép tự sửa/huỷ đơn MÌNH tạo, KHÔNG áp dụng nếu có SALE_MGR — mirror SaleOrderUpdatePaymentMethodProcess.EDIT_WINDOW_MINUTES. */
	private static final int EDIT_WINDOW_MINUTES = 15;

	private static final String MGR_FUNC_ID = "SALE_MGR";

	private SaleOrderEditGuard() {
	}

	/** Trả về branch_code của đơn nếu đủ điều kiện sửa/huỷ, ném lỗi nghiệp vụ nếu không. */
	static String resolveEligibleBranchCode(DBAccessor dba, String saleOrderNo, String userCode)
			throws DBException, ProcessCheckErrorException {

		boolean isManager = hasFunctionCode(dba, userCode, MGR_FUNC_ID);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT branch_code FROM sale_order WHERE sale_order_no = ? AND void_flg = '0' ");
			if (!isManager) {
				sql.append("AND entry_user_code = ? ");
				sql.append("AND entry_datetime >= (NOW() - INTERVAL ").append(EDIT_WINDOW_MINUTES).append(" MINUTE) ");
			}

			ps = dba.prepareStatement(sql.toString());
			ps.setString(1, saleOrderNo);
			if (!isManager) {
				ps.setString(2, userCode);
			}
			rs = ps.executeQuery();

			if (!rs.next()) {
				throwError("ME000099");
			}
			return rs.getString("branch_code");

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				throw new DBException(e);
			}
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
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				throw new DBException(e);
			}
		}
	}

	private static void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}
}
