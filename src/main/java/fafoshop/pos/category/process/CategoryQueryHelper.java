package fafoshop.pos.category.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fafoshop.pos.category.dto.CategoryFullRowDto;

/**
 * Phần dùng CHUNG giữa CategorySearchProcess (có phân trang) và
 * CategoryExportProcess (lấy toàn bộ, không phân trang) — cùng 1 bộ lọc/sort
 * trên bảng category nên tách ra đây để không lặp SQL/row-mapping ở 2 nơi.
 * Đây là bộ API MỚI cho màn Category Master, tách biệt với
 * CategoryListProcess (chỉ đọc, phục vụ dropdown Product Master).
 */
final class CategoryQueryHelper {

	private CategoryQueryHelper() {
	}

	static final String SELECT_COLUMNS_SQL = "c.category_code, c.name, c.category_type, c.display_order, "
			+ "c.del_flg, c.update_datetime ";

	static final String FROM_JOIN_SQL = "FROM category c ";

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Whitelist cứng tên cột sort — KHÔNG được ghép thẳng sortField của client
	 * vào SQL vì đây là identifier động (xem coding-rules.md).
	 */
	private static final Map<String, String> SORT_COLUMN_MAP = new HashMap<>();
	static {
		SORT_COLUMN_MAP.put("categoryCode", "c.category_code");
		SORT_COLUMN_MAP.put("name", "c.name");
		SORT_COLUMN_MAP.put("categoryType", "c.category_type");
		SORT_COLUMN_MAP.put("displayOrder", "c.display_order");
		SORT_COLUMN_MAP.put("updateDatetime", "c.update_datetime");
	}

	static void buildWhereClause(String keyword, String categoryType, String statusFilter, StringBuilder where,
			List<String> params) {
		where.append("WHERE 1=1 ");

		if ("DELETED".equals(statusFilter)) {
			where.append("AND c.del_flg = '1' ");
		} else if (!"ALL".equals(statusFilter)) {
			where.append("AND c.del_flg = '0' ");
		}

		if (keyword != null && !keyword.trim().isEmpty()) {
			where.append("AND (c.name LIKE ? OR c.category_code = ?) ");
			params.add("%" + keyword.trim() + "%");
			params.add(keyword.trim());
		}

		if (categoryType != null && !categoryType.trim().isEmpty()) {
			where.append("AND c.category_type = ? ");
			params.add(categoryType.trim());
		}
	}

	static String resolveSortColumn(String sortField) {
		return SORT_COLUMN_MAP.getOrDefault(sortField, "c.display_order");
	}

	static String resolveSortDirection(String sortDirection) {
		return "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
	}

	static CategoryFullRowDto mapRow(ResultSet rs) throws SQLException {
		CategoryFullRowDto row = new CategoryFullRowDto();
		row.categoryCode = rs.getString("category_code");
		row.name = rs.getString("name");
		row.categoryType = rs.getString("category_type");
		row.displayOrder = rs.getInt("display_order");
		row.delFlg = rs.getString("del_flg");
		Timestamp updateDatetime = rs.getTimestamp("update_datetime");
		row.updateDatetime = updateDatetime != null ? updateDatetime.toLocalDateTime().format(DATETIME_FMT) : null;
		return row;
	}
}
