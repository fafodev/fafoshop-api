package fafoshop.pos.saleorder.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
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

	/**
	 * true nếu đơn được tạo qua màn "Bổ sung đơn bán" (SaleOrderBackfillProcess)
	 * — đơn đó KHÔNG trừ tồn lúc tạo, nên huỷ/sửa cũng KHÔNG được hoàn/điều
	 * chỉnh tồn (nếu không, huỷ 1 đơn bổ sung sẽ cộng ảo tồn kho). Xem
	 * docs/pos-bo-sung-don-ban.md.
	 *
	 * Nhận diện qua cột audit CÓ SẴN `entry_program` (mã chương trình tạo bản
	 * ghi — sửa/huỷ chỉ đổi update_program, không đổi entry_program), KHÔNG
	 * dùng cột cờ riêng: người dùng chọn không phân loại đơn bổ sung/đơn
	 * thường. KHÔNG dựa vào stock_code NULL — đơn tạo trước khi theo dõi lô
	 * cũng NULL nhưng ĐÃ trừ tồn thật.
	 */
	static boolean isBackfillOrder(DBAccessor dba, String saleOrderNo) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT entry_program FROM sale_order WHERE sale_order_no = ?");
			ps.setString(1, saleOrderNo);
			rs = ps.executeQuery();
			return rs.next() && SaleOrderBackfillProcess.PRG_CD.equals(rs.getString("entry_program"));
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
