package fafoshop.common.health.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import fafoshop.common.ILogSender;
import fafoshop.common.LogLevel;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.health.dto.HealthResponse;
import fafoshop.common.process.AbstractProcess;

/**
 * Ping DB bằng "SELECT 1" — KHÔNG khai getFuncId() (mặc định trả null ở
 * AbstractProcess) nên checkAuth() bỏ qua kiểm tra quyền, đúng ý đồ: script
 * vận hành gọi endpoint này TRƯỚC KHI có ai đăng nhập được.
 *
 * Lỗi DB (mất kết nối, chưa lên...) là tình huống BÌNH THƯỜNG cần health
 * check phát hiện — tự bắt DBException tại đây, KHÔNG throw ra ngoài, để
 * response vẫn trả về HTTP 200 kèm "dbOk":false thay vì rơi vào cơ chế
 * lstFatalError chung (dành cho lỗi hệ thống bất ngờ, xem AbstractResponse).
 */
public class HealthProcess extends AbstractProcess {

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public HealthProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new HealthResponse();
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) {

		HealthResponse res = (HealthResponse) response;
		res.serverTime = LocalDateTime.now().format(DATETIME_FMT);

		DBStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dba.prepareStatement("SELECT 1");
			rs = ps.executeQuery();
			res.dbOk = rs.next();
		} catch (DBException | SQLException e) {
			logSend(LogLevel.WARNING, "Health:DbPingFailed " + e.getMessage());
			res.dbOk = false;
		} finally {
			// rs.close() (java.sql.ResultSet thuan) throws SQLException, con
			// ps.close() (DBStatement wrapper) throws DBException - 2 loai
			// exception khac nhau, bat rieng tung cai de khong bo sot dong con
			// lai neu 1 trong 2 loi.
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				logSend(LogLevel.WARNING, "Health:CloseResultSetFailed " + e.getMessage());
			}
			try {
				if (ps != null) ps.close();
			} catch (DBException e) {
				logSend(LogLevel.WARNING, "Health:CloseStatementFailed " + e.getMessage());
			}
		}

		res.ok = res.dbOk;
		return res;
	}
}
