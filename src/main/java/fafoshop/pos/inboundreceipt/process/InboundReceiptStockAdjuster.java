package fafoshop.pos.inboundreceipt.process;

import java.sql.Date;
import java.util.Map;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.pos.stock.process.StockLotHelper;

/**
 * Điều chỉnh tồn theo (sản phẩm + hạn dùng). delta &gt; 0 = nhập thêm
 * (cộng), delta &lt; 0 = trừ floor 0. Xem docs/pos-lo-ton-kho.md.
 */
final class InboundReceiptStockAdjuster {

	private InboundReceiptStockAdjuster() {
	}

	/**
	 * {@code deltaByLotKey} khoá = {@link #lotKey(String, Date)}.
	 */
	static void applyDelta(DBAccessor dba, String branchCode, Map<String, Integer> deltaByLotKey,
			Map<String, String> productByLotKey, Map<String, Date> expiryByLotKey, String userCode, String programCode)
			throws DBException, FatalException {

		for (Map.Entry<String, Integer> entry : deltaByLotKey.entrySet()) {
			int delta = entry.getValue();
			if (delta == 0) {
				continue;
			}
			String key = entry.getKey();
			StockLotHelper.applyDeltaByExpiry(dba, branchCode, productByLotKey.get(key), expiryByLotKey.get(key), delta,
					userCode, programCode);
		}
	}

	static String lotKey(String productCode, Date expiryDate) {
		return productCode + "\t" + (expiryDate == null ? "" : expiryDate.toString());
	}

	static void acc(Map<String, Integer> deltaByLotKey, Map<String, String> productByLotKey,
			Map<String, Date> expiryByLotKey, String productCode, Date expiryDate, int qty) {
		String key = lotKey(productCode, expiryDate);
		deltaByLotKey.merge(key, qty, Integer::sum);
		productByLotKey.put(key, productCode);
		expiryByLotKey.put(key, expiryDate);
	}
}
