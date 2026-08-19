package fafoshop.pos.inboundreceipt.process;

import java.util.List;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptItemDto;

/**
 * Ghi đè giá vốn/giá bán cấu hình trong Product Master
 * (product.cost/product_unit.unit_cost/product_unit.unit_price) khi người
 * dùng ĐÃ XÁC NHẬN thay đổi lúc lập/sửa phiếu nhập — dùng CHUNG cho
 * InboundReceiptCreateProcess/UpdateProcess, mirror ProductUnitWriter. Xem
 * docs/pos-dong-bo-gia.md.
 *
 * CHỈ ghi cho dòng có {@code updateMasterCost}/{@code updateMasterPrice} =
 * true — dòng KHÔNG xác nhận (mặc định) hoàn toàn KHÔNG đụng tới Master, giá
 * nhập vào chỉ lưu lại làm bản ghi lịch sử trên chính phiếu.
 */
final class InboundReceiptCostWriter {

	private InboundReceiptCostWriter() {
	}

	static void updateMasterCosts(DBAccessor dba, List<InboundReceiptItemDto> items, String userCode,
			String programCode) throws DBException {

		DBStatement psProduct = null;
		DBStatement psUnit = null;
		try {
			String sqlProduct = "UPDATE product SET cost = ?, update_user_code = ?, update_program = ? "
					+ "WHERE product_code = ? AND del_flg = '0'";
			String sqlUnit = "UPDATE product_unit SET unit_cost = ?, update_user_code = ?, update_program = ? "
					+ "WHERE product_code = ? AND unit_name = ?";

			for (InboundReceiptItemDto item : items) {
				if (!Boolean.TRUE.equals(item.updateMasterCost) || item.masterUnitCost == null) {
					continue;
				}

				boolean isBaseUnit = item.unitName == null || item.unitName.trim().isEmpty();
				if (isBaseUnit) {
					if (psProduct == null) {
						psProduct = dba.prepareStatement(sqlProduct);
					}
					psProduct.setBigDecimal(1, item.masterUnitCost);
					psProduct.setString(2, userCode);
					psProduct.setString(3, programCode);
					psProduct.setString(4, item.productCode);
					psProduct.executeUpdate();
				} else {
					if (psUnit == null) {
						psUnit = dba.prepareStatement(sqlUnit);
					}
					psUnit.setBigDecimal(1, item.masterUnitCost);
					psUnit.setString(2, userCode);
					psUnit.setString(3, programCode);
					psUnit.setString(4, item.productCode);
					psUnit.setString(5, item.unitName);
					psUnit.executeUpdate();
				}
			}
		} finally {
			if (psProduct != null) {
				psProduct.close();
			}
			if (psUnit != null) {
				psUnit.close();
			}
		}
	}

	/**
	 * Ghi đè giá bán cấu hình trong `product_unit.unit_price` khi người dùng
	 * ĐÃ XÁC NHẬN thay đổi lúc lập/sửa phiếu nhập — CHỈ áp dụng cho dòng đơn
	 * vị đóng gói (`unitName` khác null); dòng đơn vị lẻ vẫn ghi thẳng
	 * `product.price` KHÔNG cần xác nhận qua `updateProductPrices()` sẵn có
	 * (hành vi CŨ giữ nguyên, xem Javadoc `InboundReceiptItemDto.price`) —
	 * BUG ĐÃ SỬA: trước đây `updateProductPrices()` ghi NHẦM giá của dòng đơn
	 * vị đóng gói thẳng vào `product.price` (đơn vị lẻ), xem
	 * docs/pos-dong-bo-gia.md.
	 */
	static void updateMasterPrices(DBAccessor dba, List<InboundReceiptItemDto> items, String userCode,
			String programCode) throws DBException {

		DBStatement psUnit = null;
		try {
			String sqlUnit = "UPDATE product_unit SET unit_price = ?, update_user_code = ?, update_program = ? "
					+ "WHERE product_code = ? AND unit_name = ?";

			for (InboundReceiptItemDto item : items) {
				boolean isPackUnit = item.unitName != null && !item.unitName.trim().isEmpty();
				if (!isPackUnit || !Boolean.TRUE.equals(item.updateMasterPrice) || item.masterUnitPrice == null) {
					continue;
				}

				if (psUnit == null) {
					psUnit = dba.prepareStatement(sqlUnit);
				}
				psUnit.setBigDecimal(1, item.masterUnitPrice);
				psUnit.setString(2, userCode);
				psUnit.setString(3, programCode);
				psUnit.setString(4, item.productCode);
				psUnit.setString(5, item.unitName);
				psUnit.executeUpdate();
			}
		} finally {
			if (psUnit != null) {
				psUnit.close();
			}
		}
	}
}
