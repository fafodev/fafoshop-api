package fafoshop.pos.category.process;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;

/**
 * Validate name trước khi INSERT/UPDATE — dùng chung cho
 * CategoryCreateProcess/CategoryUpdateProcess. category_code KHÔNG còn cần
 * validate ở đây vì đã chuyển sang sinh tự động qua SeqNoUtility (prefix
 * "DM", xem .claude/seqno-convention.md) — không còn nhận từ client nên
 * không có input người dùng nào cần kiểm tra định dạng/trùng lặp nữa.
 */
final class CategoryFieldValidator {

	private static final int NAME_MAX_LENGTH = 100;

	private CategoryFieldValidator() {
	}

	static void validateName(String name) throws ProcessCheckErrorException {
		if (name == null || name.trim().isEmpty()) {
			throwError("ME000075");
		}
		if (name.length() > NAME_MAX_LENGTH) {
			throwError("ME000076");
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
