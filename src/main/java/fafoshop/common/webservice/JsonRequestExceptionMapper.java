package fafoshop.common.webservice;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;

import fafoshop.common.dto.ErrorDto;
import fafoshop.common.utility.MessageUtility;

/**
 * Bắt lỗi Jackson tự ném lúc deserialize request JSON (field sai kiểu, số
 * vượt phạm vi int, JSON hỏng cú pháp...) - XẢY RA TRƯỚC khi request kịp
 * chạm AbstractProcess nên SystemExceptionMapper (ExceptionMapper&lt;Throwable&gt;)
 * KHÔNG can thiệp được: thư viện jersey-media-json-jackson tự đăng ký sẵn
 * JsonParseExceptionMapper/JsonMappingExceptionMapper của Jackson (khớp kiểu
 * cụ thể hơn Throwable nên luôn được JAX-RS chọn trước), mặc định trả
 * message exception thô kèm tên package/class Java nội bộ thẳng ra client -
 * vi phạm luật "không lộ thông tin nội bộ trong lỗi trả về client" (xem
 * CLAUDE.md). Đăng ký riêng mapper này (khớp CHÍNH XÁC
 * com.fasterxml.jackson.core.JsonProcessingException - lớp cha chung của cả
 * JsonParseException lẫn JsonMappingException) để JAX-RS ưu tiên nó thay vì
 * mapper mặc định của Jackson, trả cùng khuôn lỗi MC000001 như
 * SystemExceptionMapper - đúng tinh thần comment "phòng hờ lỗi ngoài luồng
 * đó - ví dụ lỗi parse JSON request" đã ghi sẵn ở đó.
 */
@Provider
public class JsonRequestExceptionMapper implements ExceptionMapper<JsonProcessingException> {

	@Override
	public Response toResponse(JsonProcessingException exception) {
		ErrorDto error = new ErrorDto();
		error.errId = "MC000001";
		error.errMsg = MessageUtility.getSystemErrMsg(error.errId);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_JSON)
				.entity(error)
				.build();
	}
}
