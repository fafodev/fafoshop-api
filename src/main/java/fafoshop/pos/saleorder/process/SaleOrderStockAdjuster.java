package fafoshop.pos.saleorder.process;

import java.util.Map;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;

/**
 * Điều chỉnh tồn kho theo DELTA khi sửa/huỷ đơn bán — dùng CHUNG cho
 * SaleOrderUpdateProcess (delta = số lượng MỚI - số lượng CŨ từng sản phẩm)
 * và SaleOrderVoidProcess (delta = -số lượng gốc, hoàn tác toàn bộ). Xem
 * quyết định #5 docs/pos-sua-huy-don.md — KHÔNG cần validate/chặn tồn kho
 * "không khớp thực tế", chỉ cần cộng/trừ đúng delta:
 *
 * - delta &gt; 0 (số lượng bán TĂNG, cần trừ THÊM tồn) — dùng lại đúng
 *   pattern floor-về-0 đã có ở SaleOrderCreateProcess.decrementStock
 *   (KHÔNG chặn dù tồn không đủ, nhất quán với lúc tạo đơn).
 * - delta &lt; 0 (số lượng bán GIẢM, hoặc huỷ cả đơn — cần CỘNG LẠI tồn) —
 *   phép cộng đơn giản, luôn hợp lệ, không có rủi ro âm.
 * - delta == 0 — bỏ qua, không đụng gì tới dòng stock đó.
 */
final class SaleOrderStockAdjuster {

	private SaleOrderStockAdjuster() {
	}

	static void applyDelta(DBAccessor dba, String branchCode, Map<String, Integer> deltaByProduct, String userCode,
			String programCode) throws DBException {

		DBStatement decrementPs = null;
		DBStatement incrementPs = null;
		try {
			String decrementSql = "INSERT INTO stock "
					+ "(branch_code, product_code, quality_code, expiry_date, stock_qty, available_qty, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, '01', NULL, 0, 0, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE "
					+ "stock_qty = GREATEST(stock_qty - ?, 0), "
					+ "available_qty = GREATEST(available_qty - ?, 0), "
					+ "update_user_code = VALUES(update_user_code), "
					+ "update_program = VALUES(update_program)";

			String incrementSql = "INSERT INTO stock "
					+ "(branch_code, product_code, quality_code, expiry_date, stock_qty, available_qty, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, '01', NULL, 0, 0, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE "
					+ "stock_qty = stock_qty + ?, "
					+ "available_qty = available_qty + ?, "
					+ "update_user_code = VALUES(update_user_code), "
					+ "update_program = VALUES(update_program)";

			decrementPs = dba.prepareStatement(decrementSql);
			incrementPs = dba.prepareStatement(incrementSql);

			for (Map.Entry<String, Integer> entry : deltaByProduct.entrySet()) {
				String productCode = entry.getKey();
				int delta = entry.getValue();
				if (delta == 0) {
					continue;
				}

				DBStatement ps = delta > 0 ? decrementPs : incrementPs;
				int amount = Math.abs(delta);
				ps.setString(1, branchCode);
				ps.setString(2, productCode);
				ps.setString(3, userCode);
				ps.setString(4, programCode);
				ps.setString(5, userCode);
				ps.setString(6, programCode);
				ps.setInt(7, amount);
				ps.setInt(8, amount);
				ps.executeUpdate();
			}
		} finally {
			if (decrementPs != null) {
				decrementPs.close();
			}
			if (incrementPs != null) {
				incrementPs.close();
			}
		}
	}
}
