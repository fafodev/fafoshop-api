package fafoshop.pos.saleorder.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fafoshop.common.ConstantValue;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;
import fafoshop.pos.saleorder.dto.PaymentMethod;
import fafoshop.pos.saleorder.dto.SaleOrderRowDto;

/**
 * Phần dùng CHUNG giữa SaleOrderSearchProcess (có phân trang), SaleOrderExportProcess
 * (lấy toàn bộ, không phân trang) và SaleOrderDetailProcess (xác định chi nhánh) —
 * cùng bộ lọc trên bảng sale_order nên tách ra đây để không lặp SQL/row-mapping ở
 * nhiều nơi, giống đúng mẫu ProductQueryHelper bên pos/product.
 */
final class SaleOrderQueryHelper {

	private SaleOrderQueryHelper() {
	}

	/**
	 * total_amount/item_count lấy qua SUBQUERY vô hướng (không phải JOIN
	 * thường) — 1 đơn bán có NHIỀU dòng hàng (sale_order_item), JOIN thường sẽ
	 * nhân dòng và làm sai COUNT(*)/phân trang. Subquery giữ nguyên 1 dòng/đơn
	 * bán, cùng kỹ thuật ProductQueryHelper dùng cho supplier_names.
	 */
	static final String SELECT_COLUMNS_SQL = "so.sale_order_no, so.branch_code, so.customer_name, "
			+ "so.sale_datetime, so.payment_method, so.paid_amount, so.change_amount, "
			+ "so.cashier_user_code, u.name AS cashier_name, so.void_flg, "
			+ "(SELECT COALESCE(SUM(soi2.line_amount), 0) FROM sale_order_item soi2 "
			+ " WHERE soi2.sale_order_no = so.sale_order_no) AS total_amount, "
			+ "(SELECT COUNT(*) FROM sale_order_item soi2 WHERE soi2.sale_order_no = so.sale_order_no) AS item_count ";

	static final String FROM_JOIN_SQL = "FROM sale_order so "
			+ "LEFT JOIN app_user u ON u.user_code = so.cashier_user_code ";

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final Map<String, String> SORT_COLUMN_MAP = new HashMap<>();
	static {
		SORT_COLUMN_MAP.put("saleOrderNo", "so.sale_order_no");
		SORT_COLUMN_MAP.put("saleDatetime", "so.sale_datetime");
		SORT_COLUMN_MAP.put("paidAmount", "so.paid_amount");
		SORT_COLUMN_MAP.put("totalAmount", "total_amount");
		SORT_COLUMN_MAP.put("itemCount", "item_count");
	}

	/**
	 * Chi nhánh của người xem = main_branch_code của user đang đăng nhập
	 * (KHÔNG nhận từ client) — cùng quy tắc đã áp dụng ở
	 * SaleOrderCreateProcess.getCashierBranchCode/DashboardSummaryProcess.getUserBranchCode.
	 * Trùng lặp có chủ đích (không tách utility dùng chung ngoài module) để
	 * theo đúng phong cách hiện có của 2 Process kia.
	 */
	static String resolveBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT main_branch_code FROM app_user WHERE user_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			String branchCode = rs.next() ? rs.getString("main_branch_code") : null;
			if (branchCode == null || branchCode.trim().isEmpty()) {
				throwError("ME000088");
			}
			return branchCode;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/**
	 * Whitelist cứng tên cột sort — KHÔNG được ghép thẳng sortField của client
	 * vào SQL vì đây là identifier động (xem coding-rules.md).
	 */
	static String resolveSortColumn(String sortField) {
		return SORT_COLUMN_MAP.getOrDefault(sortField, "so.sale_datetime");
	}

	/** Mặc định DESC (đơn mới nhất trước) — khác mặc định ASC của ProductQueryHelper, phù hợp hơn cho màn tra cứu theo thời gian. */
	static String resolveSortDirection(String sortDirection) {
		return "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
	}

	/**
	 * Dựng mệnh đề WHERE + danh sách param theo đúng thứ tự bind — dùng CHUNG
	 * cho query đếm tổng, query lấy dòng (có phân trang) và query tổng tiền
	 * cộng dồn, đảm bảo cả 3 luôn khớp cùng 1 bộ lọc.
	 *
	 * dateFrom/dateTo validate định dạng "yyyy-MM-dd" (giống
	 * DashboardSummaryProcess.parseSelectedDate) — sai định dạng hoặc
	 * dateFrom sau dateTo đều ném lỗi nghiệp vụ, KHÔNG âm thầm bỏ qua bộ lọc
	 * ngày (khác cách xử lý im lặng của paymentMethod/statusFilter lạ, vì
	 * lỗi ngày nhiều khả năng là người dùng gõ sai, cần được biết).
	 */
	static void buildWhereClause(String branchCode, String keyword, String dateFrom, String dateTo,
			String paymentMethod, String statusFilter, String cashierKeyword, StringBuilder where,
			List<String> params) throws ProcessCheckErrorException {

		where.append("WHERE so.branch_code = ? ");
		params.add(branchCode);

		if (keyword != null && !keyword.trim().isEmpty()) {
			where.append("AND (so.sale_order_no LIKE ? OR so.customer_name LIKE ?) ");
			String kw = "%" + keyword.trim() + "%";
			params.add(kw);
			params.add(kw);
		}

		LocalDate from = parseDate(dateFrom);
		LocalDate to = parseDate(dateTo);
		if (from != null && to != null && from.isAfter(to)) {
			throwError("ME000118");
		}
		if (from != null) {
			where.append("AND so.sale_datetime >= ? ");
			params.add(from + " 00:00:00");
		}
		if (to != null) {
			where.append("AND so.sale_datetime <= ? ");
			params.add(to + " 23:59:59");
		}

		if (paymentMethod != null && PaymentMethod.isValid(paymentMethod)) {
			where.append("AND so.payment_method = ? ");
			params.add(paymentMethod);
		}

		if ("VALID".equals(statusFilter)) {
			where.append("AND so.void_flg = '0' ");
		} else if ("VOID".equals(statusFilter)) {
			where.append("AND so.void_flg = '1' ");
		}
		// "ALL" hoặc rỗng/giá trị lạ: không lọc theo void_flg — mặc định của màn tra cứu.

		if (cashierKeyword != null && !cashierKeyword.trim().isEmpty()) {
			where.append("AND u.name LIKE ? ");
			params.add("%" + cashierKeyword.trim() + "%");
		}
	}

	private static LocalDate parseDate(String value) throws ProcessCheckErrorException {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (Exception e) {
			throwError("ME000117");
			return null; // không bao giờ tới đây - throwError luôn ném exception
		}
	}

	static int bindParams(DBStatement ps, List<String> params) throws DBException {
		int idx = 1;
		for (String param : params) {
			ps.setString(idx++, param);
		}
		return idx;
	}

	static SaleOrderRowDto mapRow(ResultSet rs) throws SQLException {
		SaleOrderRowDto row = new SaleOrderRowDto();
		row.saleOrderNo = rs.getString("sale_order_no");
		row.branchCode = rs.getString("branch_code");
		row.customerName = rs.getString("customer_name");
		Timestamp saleDatetime = rs.getTimestamp("sale_datetime");
		row.saleDatetime = saleDatetime != null ? saleDatetime.toLocalDateTime().format(DATETIME_FMT) : null;
		row.paymentMethod = rs.getString("payment_method");
		row.paidAmount = rs.getBigDecimal("paid_amount");
		row.changeAmount = rs.getBigDecimal("change_amount");
		row.totalAmount = rs.getBigDecimal("total_amount");
		row.itemCount = rs.getInt("item_count");
		row.cashierUserCode = rs.getString("cashier_user_code");
		row.cashierName = rs.getString("cashier_name");
		row.voidFlg = rs.getString("void_flg");
		return row;
	}

	static void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	/**
	 * Dựng ErrorDto + ném ProcessCheckErrorException — bản static dùng CHUNG
	 * cho helper này, cùng nội dung với throwError() private mà từng Process
	 * (SaleOrderSearchProcess/SaleOrderDetailProcess/SaleOrderExportProcess)
	 * tự khai riêng theo đúng phong cách hiện có của module (xem
	 * SaleOrderCreateProcess.throwError).
	 */
	static void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}
}
