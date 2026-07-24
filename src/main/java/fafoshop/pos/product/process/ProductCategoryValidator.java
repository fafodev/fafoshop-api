package fafoshop.pos.product.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;

/**
 * Validate categoryCode tồn tại trong bảng category (category_type='PRODUCT',
 * del_flg='0') — dùng chung cho ProductCreateProcess/ProductUpdateProcess để
 * trả lỗi nghiệp vụ tiếng Việt rõ ràng, thay vì để ràng buộc khoá ngoại
 * fk_product_category tự bắn SQLException khó hiểu ra client.
 */
final class ProductCategoryValidator {

	private ProductCategoryValidator() {
	}

	static void validate(DBAccessor dba, String categoryCode) throws DBException, ProcessCheckErrorException {
		if (categoryCode == null || categoryCode.trim().isEmpty()) {
			return;
		}

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT category_code FROM category WHERE category_code = ? "
					+ "AND category_type = 'PRODUCT' AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, categoryCode.trim());
			rs = ps.executeQuery();

			if (!rs.next()) {
				List<ErrorDto> errors = new ArrayList<>();
				ErrorDto error = new ErrorDto();
				error.errId = "ME000060";
				error.errMsg = MessageUtility.getSystemErrMsg("ME000060");
				errors.add(error);
				throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
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
}
