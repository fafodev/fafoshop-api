package fafoshop.common.exception;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.dto.ErrorDto;

/**
 * Lỗi nghiệp vụ (validate, thiếu quyền...) — mang theo danh sách lỗi thường
 * (lstNormalError) hoặc lỗi nghiêm trọng cần rollback (lstFatalError).
 */
public class ProcessCheckErrorException extends Exception {

	protected static final long serialVersionUID = 8027046016920567409L;

	private List<ErrorDto> lstNormalError = new ArrayList<>();
	private List<ErrorDto> lstFatalError = new ArrayList<>();

	public ProcessCheckErrorException() {
		super();
	}

	public ProcessCheckErrorException(Exception e) {
		super(e);
	}

	public ProcessCheckErrorException(List<ErrorDto> msg, Integer errorType) {
		super();
		if (ConstantValue.FATAL_ERROR.equals(errorType)) {
			setFatalError(msg);
		} else {
			setNormalError(msg);
		}
	}

	public void setFatalError(List<ErrorDto> msg) {
		lstFatalError = msg;
	}

	public List<ErrorDto> getFatalError() {
		return lstFatalError;
	}

	public void setNormalError(List<ErrorDto> msg) {
		lstNormalError = msg;
	}

	public List<ErrorDto> getNormalError() {
		return lstNormalError;
	}
}
