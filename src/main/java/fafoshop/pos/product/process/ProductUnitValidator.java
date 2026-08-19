package fafoshop.pos.product.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fafoshop.common.ConstantValue;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;
import fafoshop.pos.product.dto.ProductUnitDto;

/**
 * Validate danh sách đơn vị đóng gói gắn với sản phẩm (ProductCreateProcess/
 * ProductUpdateProcess) — mirror ProductSupplierValidator: unitName không
 * trống, không trùng lặp trong cùng danh sách gửi lên (khoá chính
 * product_unit là (product_code, unit_name) — trùng sẽ vỡ khoá chính nếu
 * không chặn sớm ở đây), conversionQty phải > 0, unitPrice không được âm.
 * KHÔNG kiểm tra unitName trùng product.unit_name (đơn vị lẻ) — sản phẩm có
 * quyền đặt tên đơn vị đóng gói trùng tên đơn vị lẻ nếu muốn, không có lý do
 * nghiệp vụ nào bắt buộc phải khác.
 */
final class ProductUnitValidator {

	private ProductUnitValidator() {
	}

	static void validate(List<ProductUnitDto> units) throws ProcessCheckErrorException {
		if (units == null || units.isEmpty()) {
			return;
		}

		Set<String> seen = new HashSet<>();
		for (ProductUnitDto item : units) {
			if (item.unitName == null || item.unitName.trim().isEmpty()) {
				throwError("ME000121");
			}
			if (!seen.add(item.unitName.trim())) {
				throwError("ME000122");
			}
			if (item.conversionQty == null || item.conversionQty <= 0) {
				throwError("ME000123");
			}
			if (item.unitPrice == null || item.unitPrice.signum() < 0) {
				throwError("ME000124");
			}
			// unitCost KHÔNG bắt buộc (null = chưa cấu hình giá vốn cho đơn vị
			// này) — chỉ chặn giá trị âm, xem docs/pos-dong-bo-gia.md.
			if (item.unitCost != null && item.unitCost.signum() < 0) {
				throwError("ME000130");
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
