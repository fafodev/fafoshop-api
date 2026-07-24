package fafoshop.pos.product.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fafoshop.common.ConstantValue;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;
import fafoshop.pos.product.dto.ProductSupplierDto;

/**
 * Validate danh sách nhà cung cấp gắn với sản phẩm (ProductCreateProcess/
 * ProductUpdateProcess) — mỗi supplierCode phải tồn tại (del_flg='0'),
 * không trùng lặp trong cùng danh sách gửi lên (khoá chính product_supplier
 * là (product_code, supplier_code) — trùng sẽ vỡ khoá chính nếu không chặn
 * sớm ở đây), purchasePrice không được âm nếu có nhập.
 */
final class ProductSupplierValidator {

	private ProductSupplierValidator() {
	}

	static void validate(DBAccessor dba, List<ProductSupplierDto> suppliers)
			throws DBException, ProcessCheckErrorException {
		if (suppliers == null || suppliers.isEmpty()) {
			return;
		}

		Set<String> seen = new HashSet<>();
		for (ProductSupplierDto item : suppliers) {
			if (item.supplierCode == null || item.supplierCode.trim().isEmpty()) {
				throwError("ME000082");
			}
			if (!seen.add(item.supplierCode.trim())) {
				throwError("ME000083");
			}
			if (item.purchasePrice != null && item.purchasePrice.signum() < 0) {
				throwError("ME000084");
			}
			validateSupplierExists(dba, item.supplierCode.trim());
		}
	}

	private static void validateSupplierExists(DBAccessor dba, String supplierCode)
			throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT supplier_code FROM supplier WHERE supplier_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, supplierCode);
			rs = ps.executeQuery();

			if (!rs.next()) {
				throwError("ME000082");
			}

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

	private static void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}
}
