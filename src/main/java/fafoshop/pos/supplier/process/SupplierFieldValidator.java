package fafoshop.pos.supplier.process;

import fafoshop.common.ConstantValue;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Validate name/shortName trước khi INSERT/UPDATE — dùng chung cho
 * SupplierCreateProcess/SupplierUpdateProcess để trả lỗi nghiệp vụ tiếng
 * Việt rõ ràng, thay vì để ràng buộc NOT NULL/độ dài cột của MySQL tự bắn
 * SQLException khó hiểu ra client (theo đúng cách ProductFieldValidator đã
 * làm cho Product Master).
 */
final class SupplierFieldValidator {

	private static final int NAME_MAX_LENGTH = 80;
	private static final int SHORT_NAME_MAX_LENGTH = 40;

	private SupplierFieldValidator() {
	}

	static void validate(String name, String shortName) throws ProcessCheckErrorException {
		validateName(name);
		validateShortName(shortName);
	}

	private static void validateName(String name) throws ProcessCheckErrorException {
		if (name == null || name.trim().isEmpty()) {
			throwError("ME000069");
		}
		if (name.length() > NAME_MAX_LENGTH) {
			throwError("ME000070");
		}
	}

	private static void validateShortName(String shortName) throws ProcessCheckErrorException {
		if (shortName == null || shortName.trim().isEmpty()) {
			throwError("ME000071");
		}
		if (shortName.length() > SHORT_NAME_MAX_LENGTH) {
			throwError("ME000072");
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
