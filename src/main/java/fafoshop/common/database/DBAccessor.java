package fafoshop.common.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import fafoshop.common.ILogSender;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.utility.AES128AndBase64;

/**
 * Kết nối DB — đọc db.properties, giải mã mật khẩu bằng AES128AndBase64,
 * JDBC thuần (không ORM), tự quản lý transaction (autoCommit=false,
 * commit()/rollback() gọi tay). Dùng driver class "com.mysql.cj.jdbc.Driver"
 * (driver mới, không deprecated).
 */
public final class DBAccessor {

	protected Connection conn;
	private String mySQLKey;

	public Connection getConnection() {
		return conn;
	}

	public void readOnly() throws DBException {
		try {
			conn.setReadOnly(true);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public DBAccessor() throws DBException, FatalException {
		conn = null;
		connect();
	}

	@Override
	protected void finalize() {
		try {
			disconnect();
		} catch (Exception e) {
		}
	}

	public void connect() throws DBException, FatalException {

		InputStream io = null;

		try {
			if (conn == null) {
				Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

				Properties properties = new Properties();
				io = getClass().getResourceAsStream("/db.properties");
				properties.load(io);

				String url = properties.getProperty("url");
				String user = properties.getProperty("user");
				String pass = properties.getProperty("pass");
				String key = properties.getProperty("key");

				pass = AES128AndBase64.decrypt(pass);
				mySQLKey = AES128AndBase64.decrypt(key);

				conn = DriverManager.getConnection(url, user, pass);
			}

			conn.setAutoCommit(false);

		} catch (SQLException e) {
			throw new DBException(e);
		} catch (Exception e) {
			throw new FatalException(e);
		} finally {
			try {
				if (io != null) io.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void disconnect() throws DBException {
		try {
			if (conn != null) {
				if (conn.isClosed()) {
					return;
				}
				conn.close();
				conn = null;
			}
		} catch (Exception e) {
			throw new DBException(e);
		}
	}

	public void commit() throws DBException {
		try {
			if (conn != null) {
				conn.commit();
			}
		} catch (Exception e) {
			throw new DBException(e);
		}
	}

	public void rollback() throws DBException {
		try {
			if (conn != null) {
				conn.rollback();
			}
		} catch (Exception e) {
			throw new DBException(e);
		}
	}

	public String getKey() {
		return mySQLKey;
	}

	public DBStatement prepareStatement(String sql) throws DBException {
		return new DBStatement(this, sql);
	}

	public DBStatement prepareStatement(StringBuilder sql) throws DBException {
		return new DBStatement(this, sql.toString());
	}

	public DBCallableStatement prepareCall(String sql) throws DBException {
		return new DBCallableStatement(this, sql);
	}

	public DBCallableStatement prepareCall(StringBuilder sql) throws DBException {
		return new DBCallableStatement(this, sql.toString());
	}

	protected ILogSender logSender = null;

	public ILogSender getLogSender() {
		return logSender;
	}

	public void setLogSender(ILogSender logSender) {
		this.logSender = logSender;
	}

	public void logSend(String level, Throwable e) {
		if (logSender != null) {
			logSender.logSend(level, e);
		}
	}

	public void logSend(String level, String message) {
		if (logSender != null) {
			logSender.logSend(level, message);
		}
	}
}
