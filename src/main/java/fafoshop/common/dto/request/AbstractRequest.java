package fafoshop.common.dto.request;

import fafoshop.common.dto.AccessInfoDto;

/**
 * Request gốc cho mọi Process — fafoshop-api là API JSON thuần, không có
 * màn hình server-side nên không cần các field liên quan tới form UI.
 */
public class AbstractRequest {

	/** Thông tin truy cập (được AuthTokenFilter điền userCode sau khi xác thực) */
	public AccessInfoDto accessInfo = new AccessInfoDto();

	/** Có phải lần gọi đầu tiên của process hay không */
	public boolean isFirstCall = false;
}
