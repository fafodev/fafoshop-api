package fafoshop.pos.saleorder.process;

import java.util.Map;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.pos.stock.process.StockLotHelper;

/**
 * Điều chỉnh tồn theo MÃ LÔ (stock_code). delta &gt; 0 = bán thêm (trừ
 * floor 0), delta &lt; 0 = hoàn tồn (cộng). Xem docs/pos-lo-ton-kho.md.
 */
final class SaleOrderStockAdjuster {

	private SaleOrderStockAdjuster() {
	}

	static void applyDelta(DBAccessor dba, Map<String, Integer> deltaByStockCode, String userCode, String programCode)
			throws DBException, FatalException {

		for (Map.Entry<String, Integer> entry : deltaByStockCode.entrySet()) {
			String stockCode = entry.getKey();
			int delta = entry.getValue();
			if (delta == 0 || stockCode == null || stockCode.isEmpty()) {
				continue;
			}
			if (delta > 0) {
				StockLotHelper.subtractQtyFloor(dba, stockCode, delta, userCode, programCode);
			} else {
				StockLotHelper.addQty(dba, stockCode, -delta, userCode, programCode);
			}
		}
	}
}
