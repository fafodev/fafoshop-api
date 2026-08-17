package fafoshop.pos.product.process;

import java.util.List;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;
import fafoshop.pos.product.dto.ProductUnitDto;

/**
 * Ghi danh sách đơn vị đóng gói gắn với sản phẩm vào product_unit — dùng
 * CHUNG cho ProductCreateProcess (INSERT lần đầu) và ProductUpdateProcess
 * (DELETE hết dòng cũ rồi INSERT lại toàn bộ danh sách mới), mirror
 * ProductSupplierWriter y hệt.
 */
final class ProductUnitWriter {

	private ProductUnitWriter() {
	}

	static void insertAll(DBAccessor dba, String productCode, List<ProductUnitDto> units, String userCode,
			String programCode) throws DBException {
		if (units == null || units.isEmpty()) {
			return;
		}

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO product_unit "
					+ "(product_code, unit_name, conversion_qty, unit_price, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

			ps = dba.prepareStatement(sql);
			for (ProductUnitDto item : units) {
				ps.setString(1, productCode);
				ps.setString(2, item.unitName);
				ps.setInt(3, item.conversionQty);
				ps.setBigDecimal(4, item.unitPrice);
				ps.setString(5, userCode);
				ps.setString(6, programCode);
				ps.setString(7, userCode);
				ps.setString(8, programCode);
				ps.executeUpdate();
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
