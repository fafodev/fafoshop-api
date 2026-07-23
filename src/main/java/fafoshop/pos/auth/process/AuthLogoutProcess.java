package fafoshop.pos.auth.process;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.IdTokenUtility;
import fafoshop.pos.auth.dto.AuthLogoutRequest;
import fafoshop.pos.auth.dto.AuthLogoutResponse;

/**
 * Đăng xuất — xoá session_token tương ứng token đang dùng (nếu còn) để
 * cookie cũ không dùng lại được nữa dù chưa hết hạn tự nhiên. Không có
 * getFuncId() riêng (mặc định null) — không cần kiểm tra quyền, vì
 * AuthLogoutWebService là @NoAuth (phải cho phép gọi kể cả khi cookie đã
 * hết hạn/không hợp lệ, miễn là vẫn xoá sạch cookie phía trình duyệt).
 */
public class AuthLogoutProcess extends AbstractProcess {

	public AuthLogoutProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new AuthLogoutResponse();
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		AuthLogoutRequest req = (AuthLogoutRequest) request;

		if (req.token != null && !req.token.isEmpty()) {
			IdTokenUtility.revoke(req.token);
		}

		return response;
	}
}
