package fafoshop.pos.auth.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.IdTokenUtility;
import fafoshop.common.utility.MessageUtility;
import fafoshop.common.utility.PasswordUtility;
import fafoshop.pos.auth.dto.AuthLoginRequest;
import fafoshop.pos.auth.dto.AuthLoginResponse;

/**
 * Đăng nhập — xác thực userCode/mật khẩu (PasswordUtility.verify(),
 * PBKDF2WithHmacSHA256 — không so sánh plaintext), phát hành token.
 *
 * Tên class này PHẢI khớp ConstantValue.LOGIN_PROCESS_ID để
 * AbstractProcess.checkAuth() bỏ qua kiểm tra quyền cho chính luồng đăng
 * nhập (lúc này user chưa có token/quyền gì để kiểm tra).
 */
public class AuthLoginProcess extends AbstractProcess {

	public AuthLoginProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new AuthLoginResponse();
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		AuthLoginRequest req = (AuthLoginRequest) request;
		AuthLoginResponse res = (AuthLoginResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;

		try {
			String sql = "SELECT user_code, name, password_hash, main_branch_code FROM app_user"
					+ " WHERE user_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, req.userCode);
			rs = ps.executeQuery();

			if (!rs.next() || !PasswordUtility.verify(req.password, rs.getString("password_hash"))) {
				List<ErrorDto> errors = new ArrayList<>();
				ErrorDto error = new ErrorDto();
				error.errId = "ME000037";
				error.errMsg = MessageUtility.getSystemErrMsg("ME000037");
				errors.add(error);
				throw new ProcessCheckErrorException(errors, ConstantValue.FATAL_ERROR);
			}

			String userName = rs.getString("name");
			String mainBranchCode = rs.getString("main_branch_code");
			ps.close();
			rs.close();

			req.accessInfo.userCode = req.userCode;
			req.accessInfo.userName = userName;
			req.accessInfo.branchCode = mainBranchCode;

			res.token = IdTokenUtility.generate(req.userCode);
			res.userName = userName;
			res.mainBranchCode = mainBranchCode;

			return res;

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
}
