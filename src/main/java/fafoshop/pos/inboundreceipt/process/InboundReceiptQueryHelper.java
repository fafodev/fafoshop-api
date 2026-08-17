package fafoshop.pos.inboundreceipt.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
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
import fafoshop.pos.inboundreceipt.dto.InboundReceiptRowDto;

/**
 * Phần dùng CHUNG giữa InboundReceiptSearchProcess/InboundReceiptDetailProcess
 * — mirror SaleOrderQueryHelper y hệt (cùng bộ lọc/row-mapping/resolveBranchCode).
 */
final class InboundReceiptQueryHelper {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private InboundReceiptQueryHelper() {
	}

	/**
	 * total_amount/item_count lấy qua SUBQUERY vô hướng (không phải JOIN
	 * thường) — 1 phiếu nhập có NHIỀU dòng hàng, JOIN thường sẽ nhân dòng và
	 * làm sai COUNT(*)/phân trang, cùng kỹ thuật SaleOrderQueryHelper.
	 */
	static final String SELECT_COLUMNS_SQL = "ir.receipt_no, ir.branch_code, ir.supplier_code, s.name AS supplier_name, "
			+ "ir.receipt_date, ir.note, ir.receipt_user_code, u.name AS receipt_user_name, ir.void_flg, "
			+ "(SELECT COALESCE(SUM(iri2.unit_cost * iri2.actual_qty), 0) FROM inbound_receipt_item iri2 "
			+ " WHERE iri2.branch_code = ir.branch_code AND iri2.receipt_no = ir.receipt_no) AS total_amount, "
			+ "(SELECT COUNT(*) FROM inbound_receipt_item iri2 "
			+ " WHERE iri2.branch_code = ir.branch_code AND iri2.receipt_no = ir.receipt_no) AS item_count ";

	static final String FROM_JOIN_SQL = "FROM inbound_receipt ir "
			+ "LEFT JOIN supplier s ON s.supplier_code = ir.supplier_code "
			+ "LEFT JOIN app_user u ON u.user_code = ir.receipt_user_code ";

	private static final Map<String, String> SORT_COLUMN_MAP = new HashMap<>();
	static {
		SORT_COLUMN_MAP.put("receiptNo", "ir.receipt_no");
		SORT_COLUMN_MAP.put("receiptDate", "ir.receipt_date");
		SORT_COLUMN_MAP.put("totalAmount", "total_amount");
		SORT_COLUMN_MAP.put("itemCount", "item_count");
	}

	/** Chi nhánh của người xem = main_branch_code của user đăng nhập (KHÔNG nhận từ client) — mirror SaleOrderQueryHelper.resolveBranchCode. */
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

	static String resolveSortColumn(String sortField) {
		return SORT_COLUMN_MAP.getOrDefault(sortField, "ir.receipt_date");
	}

	/** Mặc định DESC (phiếu mới nhất trước), giống SaleOrderQueryHelper. */
	static String resolveSortDirection(String sortDirection) {
		return "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
	}

	static void buildWhereClause(String branchCode, String keyword, String dateFrom, String dateTo,
			String statusFilter, StringBuilder where, List<String> params) throws ProcessCheckErrorException {

		where.append("WHERE ir.branch_code = ? ");
		params.add(branchCode);

		if (keyword != null && !keyword.trim().isEmpty()) {
			where.append("AND (ir.receipt_no LIKE ? OR s.name LIKE ?) ");
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
			where.append("AND ir.receipt_date >= ? ");
			params.add(from.toString());
		}
		if (to != null) {
			where.append("AND ir.receipt_date <= ? ");
			params.add(to.toString());
		}

		if ("VALID".equals(statusFilter)) {
			where.append("AND ir.void_flg = '0' ");
		} else if ("VOID".equals(statusFilter)) {
			where.append("AND ir.void_flg = '1' ");
		}
		// "ALL" hoặc rỗng/giá trị lạ: không lọc theo void_flg.
	}

	static LocalDate parseDate(String value) throws ProcessCheckErrorException {
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

	static InboundReceiptRowDto mapRow(ResultSet rs) throws SQLException {
		InboundReceiptRowDto row = new InboundReceiptRowDto();
		row.receiptNo = rs.getString("receipt_no");
		row.branchCode = rs.getString("branch_code");
		row.supplierCode = rs.getString("supplier_code");
		row.supplierName = rs.getString("supplier_name");
		Date receiptDate = rs.getDate("receipt_date");
		row.receiptDate = receiptDate != null ? receiptDate.toLocalDate().format(DATE_FMT) : null;
		row.note = rs.getString("note");
		row.receiptUserCode = rs.getString("receipt_user_code");
		row.receiptUserName = rs.getString("receipt_user_name");
		row.voidFlg = rs.getString("void_flg");
		row.totalAmount = rs.getBigDecimal("total_amount");
		row.itemCount = rs.getInt("item_count");
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

	static void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}
}
