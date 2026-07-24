package fafoshop.pos.product.process;

import java.util.List;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;
import fafoshop.pos.product.dto.ProductSupplierDto;

/**
 * Ghi danh sách nhà cung cấp gắn với sản phẩm vào product_supplier — dùng
 * CHUNG cho ProductCreateProcess (INSERT lần đầu) và ProductUpdateProcess
 * (DELETE hết dòng cũ rồi INSERT lại toàn bộ danh sách mới, xem
 * ProductUpdateProcess) để không lặp SQL INSERT ở 2 nơi.
 */
final class ProductSupplierWriter {

	private ProductSupplierWriter() {
	}

	static void insertAll(DBAccessor dba, String productCode, List<ProductSupplierDto> suppliers, String userCode,
			String programCode) throws DBException {
		if (suppliers == null || suppliers.isEmpty()) {
			return;
		}

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO product_supplier "
					+ "(product_code, supplier_code, supplier_product_code, purchase_price, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

			ps = dba.prepareStatement(sql);
			for (ProductSupplierDto item : suppliers) {
				ps.setString(1, productCode);
				ps.setString(2, item.supplierCode);
				ps.setString(3, item.supplierProductCode);
				ps.setBigDecimal(4, item.purchasePrice);
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
