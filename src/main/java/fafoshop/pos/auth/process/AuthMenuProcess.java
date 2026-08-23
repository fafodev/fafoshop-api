package fafoshop.pos.auth.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.pos.auth.dto.AuthMenuResponse;

/**
 * Trả mã chức năng được phép hiện trên sidebar. Sidebar Angular đang hardcode
 * — phải lọc theo {@code app_function.menu_show_flg} vì cột này không được
 * CheckAuthProcess đọc (CheckAuthProcess chỉ xem function_permission).
 */
public class AuthMenuProcess extends AbstractProcess {

	public AuthMenuProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new AuthMenuResponse();
	}

	@Override
	protected String getFuncId() {
		return null;
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		AuthMenuResponse res = (AuthMenuResponse) response;
		res.functionCodes = queryVisibleFunctionCodes(dba);
		return res;
	}

	private List<String> queryVisibleFunctionCodes(DBAccessor dba) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT function_code FROM app_function "
					+ "WHERE menu_show_flg = '1' AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			rs = ps.executeQuery();
			List<String> codes = new ArrayList<>();
			while (rs.next()) {
				codes.add(rs.getString("function_code"));
			}
			return codes;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
			} catch (SQLException e) {
				throw new DBException(e);
			}
		}
	}
}
