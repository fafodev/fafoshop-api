package fafoshop.pos.inboundreceipt.process;

import java.util.Map;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;

/**
 * Điều chỉnh tồn kho theo DELTA khi sửa/huỷ phiếu nhập — mirror
 * SaleOrderStockAdjuster nhưng CHIỀU NGƯỢC LẠI (nhập hàng CỘNG tồn, không
 * phải TRỪ như bán hàng):
 *
 * - delta &gt; 0 (số lượng nhập TĂNG so với trước, hoặc phiếu mới) — CỘNG
 *   thêm tồn, phép cộng đơn giản, luôn hợp lệ.
 * - delta &lt; 0 (số lượng nhập GIẢM, hoặc huỷ cả phiếu) — TRỪ lại tồn,
 *   dùng floor-về-0 (KHÔNG chặn dù tồn đã bị bán bớt sau khi nhập — xem
 *   docs/pos-sua-huy-don.md quyết định #5, cùng lý luận đã áp dụng cho
 *   sale_order).
 * - delta == 0 — bỏ qua.
 */
final class InboundReceiptStockAdjuster {

	private InboundReceiptStockAdjuster() {
	}

	static void applyDelta(DBAccessor dba, String branchCode, Map<String, Integer> deltaByProduct, String userCode,
			String programCode) throws DBException {

		DBStatement incrementPs = null;
		DBStatement decrementPs = null;
		try {
			String incrementSql = "INSERT INTO stock "
					+ "(branch_code, product_code, quality_code, expiry_date, stock_qty, available_qty, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, '01', NULL, 0, 0, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE "
					+ "stock_qty = stock_qty + ?, "
					+ "available_qty = available_qty + ?, "
					+ "update_user_code = VALUES(update_user_code), "
					+ "update_program = VALUES(update_program)";

			String decrementSql = "INSERT INTO stock "
					+ "(branch_code, product_code, quality_code, expiry_date, stock_qty, available_qty, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, '01', NULL, 0, 0, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE "
					+ "stock_qty = GREATEST(stock_qty - ?, 0), "
					+ "available_qty = GREATEST(available_qty - ?, 0), "
					+ "update_user_code = VALUES(update_user_code), "
					+ "update_program = VALUES(update_program)";

			incrementPs = dba.prepareStatement(incrementSql);
			decrementPs = dba.prepareStatement(decrementSql);

			for (Map.Entry<String, Integer> entry : deltaByProduct.entrySet()) {
				String productCode = entry.getKey();
				int delta = entry.getValue();
				if (delta == 0) {
					continue;
				}

				DBStatement ps = delta > 0 ? incrementPs : decrementPs;
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
			if (incrementPs != null) {
				incrementPs.close();
			}
			if (decrementPs != null) {
				decrementPs.close();
			}
		}
	}
}
