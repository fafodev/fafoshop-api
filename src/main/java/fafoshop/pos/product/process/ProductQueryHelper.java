package fafoshop.pos.product.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fafoshop.pos.product.dto.ProductRowDto;

/**
 * Phần dùng CHUNG giữa ProductSearchProcess (có phân trang) và
 * ProductExportProcess (lấy toàn bộ, không phân trang) — cùng 1 bộ lọc/sort
 * trên bảng product nên tách ra đây để không lặp SQL/row-mapping ở 2 nơi.
 */
final class ProductQueryHelper {

	private ProductQueryHelper() {
	}

	static final String SELECT_COLUMNS_SQL = "p.product_code, p.name, p.short_name, p.barcode, p.category_code, "
			+ "c.name AS category_name, p.supplier_code, s.name AS supplier_name, p.unit_name, "
			+ "p.reduced_tax_rate_flg, p.price, p.min_stock_qty, p.del_flg, p.update_datetime ";

	static final String FROM_JOIN_SQL = "FROM product p "
			+ "LEFT JOIN category c ON c.category_code = p.category_code "
			+ "LEFT JOIN supplier s ON s.supplier_code = p.supplier_code ";

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Whitelist cứng tên cột sort — KHÔNG được ghép thẳng sortField của client
	 * vào SQL vì đây là identifier động (xem coding-rules.md).
	 */
	private static final Map<String, String> SORT_COLUMN_MAP = new HashMap<>();
	static {
		SORT_COLUMN_MAP.put("productCode", "p.product_code");
		SORT_COLUMN_MAP.put("name", "p.name");
		SORT_COLUMN_MAP.put("price", "p.price");
		SORT_COLUMN_MAP.put("categoryCode", "p.category_code");
		SORT_COLUMN_MAP.put("minStockQty", "p.min_stock_qty");
		SORT_COLUMN_MAP.put("updateDatetime", "p.update_datetime");
	}

	static void buildWhereClause(String keyword, String categoryCode, String statusFilter, StringBuilder where,
			List<String> params) {
		where.append("WHERE 1=1 ");

		if ("DELETED".equals(statusFilter)) {
			where.append("AND p.del_flg = '1' ");
		} else if (!"ALL".equals(statusFilter)) {
			where.append("AND p.del_flg = '0' ");
		}

		if (keyword != null && !keyword.trim().isEmpty()) {
			where.append("AND (p.name LIKE ? OR p.barcode = ?) ");
			params.add("%" + keyword.trim() + "%");
			params.add(keyword.trim());
		}

		if (categoryCode != null && !categoryCode.trim().isEmpty()) {
			where.append("AND p.category_code = ? ");
			params.add(categoryCode.trim());
		}
	}

	static String resolveSortColumn(String sortField) {
		return SORT_COLUMN_MAP.getOrDefault(sortField, "p.name");
	}

	static String resolveSortDirection(String sortDirection) {
		return "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
	}

	static ProductRowDto mapRow(ResultSet rs) throws SQLException {
		ProductRowDto row = new ProductRowDto();
		row.productCode = rs.getString("product_code");
		row.name = rs.getString("name");
		row.shortName = rs.getString("short_name");
		row.barcode = rs.getString("barcode");
		row.categoryCode = rs.getString("category_code");
		row.categoryName = rs.getString("category_name");
		row.supplierCode = rs.getString("supplier_code");
		row.supplierName = rs.getString("supplier_name");
		row.unitName = rs.getString("unit_name");
		row.reducedTaxRateFlg = rs.getString("reduced_tax_rate_flg");
		row.price = rs.getBigDecimal("price");
		row.minStockQty = rs.getInt("min_stock_qty");
		row.delFlg = rs.getString("del_flg");
		Timestamp updateDatetime = rs.getTimestamp("update_datetime");
		row.updateDatetime = updateDatetime != null ? updateDatetime.toLocalDateTime().format(DATETIME_FMT) : null;
		return row;
	}
}
