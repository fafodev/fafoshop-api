package fafoshop.pos.supplier.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fafoshop.pos.supplier.dto.SupplierRowDto;

/**
 * Phần dùng CHUNG giữa SupplierSearchProcess (có phân trang) và
 * SupplierExportProcess (lấy toàn bộ, không phân trang) — cùng 1 bộ lọc/sort
 * trên bảng supplier nên tách ra đây để không lặp SQL/row-mapping ở 2 nơi.
 */
final class SupplierQueryHelper {

	private SupplierQueryHelper() {
	}

	static final String SELECT_COLUMNS_SQL = "s.supplier_code, s.name, s.short_name, s.zip_code, s.address1, "
			+ "s.address2, s.address3, s.tel, s.fax, s.contact_name, s.email, s.note, s.del_flg, s.update_datetime ";

	static final String FROM_JOIN_SQL = "FROM supplier s ";

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Whitelist cứng tên cột sort — KHÔNG được ghép thẳng sortField của client
	 * vào SQL vì đây là identifier động (xem coding-rules.md).
	 */
	private static final Map<String, String> SORT_COLUMN_MAP = new HashMap<>();
	static {
		SORT_COLUMN_MAP.put("supplierCode", "s.supplier_code");
		SORT_COLUMN_MAP.put("name", "s.name");
		SORT_COLUMN_MAP.put("updateDatetime", "s.update_datetime");
	}

	static void buildWhereClause(String keyword, String statusFilter, StringBuilder where, List<String> params) {
		where.append("WHERE 1=1 ");

		if ("DELETED".equals(statusFilter)) {
			where.append("AND s.del_flg = '1' ");
		} else if (!"ALL".equals(statusFilter)) {
			where.append("AND s.del_flg = '0' ");
		}

		if (keyword != null && !keyword.trim().isEmpty()) {
			where.append("AND (s.name LIKE ? OR s.short_name LIKE ? OR s.tel = ?) ");
			String likeKeyword = "%" + keyword.trim() + "%";
			params.add(likeKeyword);
			params.add(likeKeyword);
			params.add(keyword.trim());
		}
	}

	static String resolveSortColumn(String sortField) {
		return SORT_COLUMN_MAP.getOrDefault(sortField, "s.name");
	}

	static String resolveSortDirection(String sortDirection) {
		return "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
	}

	static SupplierRowDto mapRow(ResultSet rs) throws SQLException {
		SupplierRowDto row = new SupplierRowDto();
		row.supplierCode = rs.getString("supplier_code");
		row.name = rs.getString("name");
		row.shortName = rs.getString("short_name");
		row.zipCode = rs.getString("zip_code");
		row.address1 = rs.getString("address1");
		row.address2 = rs.getString("address2");
		row.address3 = rs.getString("address3");
		row.tel = rs.getString("tel");
		row.fax = rs.getString("fax");
		row.contactName = rs.getString("contact_name");
		row.email = rs.getString("email");
		row.note = rs.getString("note");
		row.delFlg = rs.getString("del_flg");
		Timestamp updateDatetime = rs.getTimestamp("update_datetime");
		row.updateDatetime = updateDatetime != null ? updateDatetime.toLocalDateTime().format(DATETIME_FMT) : null;
		return row;
	}
}
