package fafoshop.common.dto.response;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.ErrorDto;

/**
 * Response gốc cho mọi Process. Lỗi NGHIỆP VỤ (validate input, thiếu
 * quyền...) dùng lstNormalError/lstFatalError, vẫn trả HTTP 200 kèm danh
 * sách lỗi trong JSON body — vì client (Angular POS) cần hiển thị thông báo
 * cụ thể cho người dùng, không chỉ 1 mã lỗi HTTP chung.
 *
 * Lỗi HỆ THỐNG (exception không lường trước) KHÔNG dùng cơ chế này — xem
 * SystemExceptionMapper, trả đúng HTTP 500.
 */
public class AbstractResponse {

	protected List<ErrorDto> lstNormalError = new ArrayList<>();
	protected List<ErrorDto> lstFatalError = new ArrayList<>();

	public List<ErrorDto> getNormalError() {
		return lstNormalError;
	}

	public void setNormalError(List<ErrorDto> lstNormalError) {
		this.lstNormalError = lstNormalError;
	}

	public void addNormalError(ErrorDto error) {
		this.lstNormalError.add(error);
	}

	public void addNormalErrorList(List<ErrorDto> lstNormalError) {
		this.lstNormalError.addAll(lstNormalError);
	}

	public List<ErrorDto> getFatalError() {
		return lstFatalError;
	}

	public void setFatalError(List<ErrorDto> lstFatalError) {
		this.lstFatalError = lstFatalError;
	}

	public void addFatalError(ErrorDto error) {
		this.lstFatalError.add(error);
	}

	public void addFatalErrorList(List<ErrorDto> lstFatalError) {
		this.lstFatalError.addAll(lstFatalError);
	}

	@Override
	public String toString() {
		return ">" + this.getClass().getSimpleName();
	}
}
