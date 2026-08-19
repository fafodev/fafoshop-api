package fafoshop.pos.product.process;

import java.math.BigDecimal;
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
 * Validate name/price/barcode trước khi INSERT/UPDATE — dùng chung cho
 * ProductCreateProcess/ProductUpdateProcess để trả lỗi nghiệp vụ tiếng Việt
 * rõ ràng, thay vì để ràng buộc NOT NULL/độ dài cột của MySQL tự bắn
 * SQLException khó hiểu ra client (rơi vào nhánh lỗi hệ thống chung
 * MC000001 — phát hiện qua test thủ công, xem báo cáo kiểm thử Product
 * Master).
 */
final class ProductFieldValidator {

	private static final int NAME_MAX_LENGTH = 100;

	private ProductFieldValidator() {
	}

	static void validate(DBAccessor dba, String name, BigDecimal price, String barcode, String excludeProductCode)
			throws DBException, ProcessCheckErrorException {
		validateName(name);
		validatePrice(price);
		validateBarcodeUnique(dba, barcode, excludeProductCode);
	}

	/**
	 * Giá vốn KHÔNG bắt buộc (null hợp lệ = chưa cấu hình/chưa nhập hàng lần
	 * nào, xem docs/pos-dong-bo-gia.md) — chỉ chặn giá trị âm.
	 */
	static void validateCost(BigDecimal cost) throws ProcessCheckErrorException {
		if (cost != null && cost.signum() < 0) {
			throwError("ME000129");
		}
	}

	/** null hợp lệ (Process tự áp mặc định 90 ngày) — chỉ chặn giá trị âm. */
	static void validateExpiryWarningDays(Integer expiryWarningDays) throws ProcessCheckErrorException {
		if (expiryWarningDays != null && expiryWarningDays < 0) {
			throwError("ME000089");
		}
	}

	private static void validateName(String name) throws ProcessCheckErrorException {
		if (name == null || name.trim().isEmpty()) {
			throwError("ME000064");
		}
		if (name.length() > NAME_MAX_LENGTH) {
			throwError("ME000065");
		}
	}

	private static void validatePrice(BigDecimal price) throws ProcessCheckErrorException {
		if (price == null) {
			throwError("ME000066");
		}
		if (price.signum() < 0) {
			throwError("ME000067");
		}
	}

	private static void validateBarcodeUnique(DBAccessor dba, String barcode, String excludeProductCode)
			throws DBException, ProcessCheckErrorException {
		if (barcode == null || barcode.trim().isEmpty()) {
			return;
		}

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT product_code FROM product WHERE barcode = ? AND del_flg = '0' ");
			if (excludeProductCode != null) {
				sql.append("AND product_code <> ? ");
			}

			ps = dba.prepareStatement(sql.toString());
			ps.setString(1, barcode.trim());
			if (excludeProductCode != null) {
				ps.setString(2, excludeProductCode);
			}
			rs = ps.executeQuery();

			if (rs.next()) {
				throwError("ME000068");
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
