package fafoshop.common.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;

/**
 * Kiểm tra quyền theo chức năng — tra trực tiếp bảng function_permission
 * (user_code, function_code, auth_type='1'), không đa tenant, không qua
 * bảng trung gian nào.
 */
public class CheckAuthProcess extends AbstractProcess {

	public CheckAuthProcess(ILogSender logSender) {
		super(logSender);
	}

	public void checkAuth(DBAccessor dba, String userCode, String functionCode)
			throws DBException, ProcessCheckErrorException {

		ResultSet rs = null;
		DBStatement ps = null;

		try {
			String sql = "SELECT COUNT(*) AS CNT FROM function_permission"
					+ " WHERE user_code = ? AND function_code = ? AND auth_type = '1'";

			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			ps.setString(2, functionCode);
			rs = ps.executeQuery();

			if (rs.next() && rs.getInt("CNT") == 0) {
				List<ErrorDto> lstErrorDto = new ArrayList<>();
				ErrorDto msg = new ErrorDto();
				msg.errId = "MC000003";
				msg.errMsg = MessageUtility.getSystemErrMsg("MC000003");
				lstErrorDto.add(msg);
				throw new ProcessCheckErrorException(lstErrorDto, ConstantValue.FATAL_ERROR);
			}

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
