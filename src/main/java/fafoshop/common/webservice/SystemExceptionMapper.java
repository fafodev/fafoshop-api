package fafoshop.common.webservice;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fafoshop.common.dto.ErrorDto;
import fafoshop.common.utility.MessageUtility;

/**
 * Nếu 1 exception thoát khỏi AbstractProcess.execute() (không nên xảy ra vì
 * execute() đã bắt hết, nhưng phòng hờ lỗi ngoài luồng đó — ví dụ lỗi parse
 * JSON request), trả đúng HTTP 500 thay vì lẫn vào lstFatalError của JSON
 * body như lỗi nghiệp vụ thông thường.
 */
@Provider
public class SystemExceptionMapper implements ExceptionMapper<Throwable> {

	@Override
	public Response toResponse(Throwable exception) {
		ErrorDto error = new ErrorDto();
		error.errId = "MC000001";
		error.errMsg = MessageUtility.getSystemErrMsg(error.errId);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON)
				.entity(error)
				.build();
	}
}
