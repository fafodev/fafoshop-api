package fafoshop.common.database;

import java.sql.CallableStatement;
import java.sql.SQLException;

import fafoshop.common.exception.DBException;

/**
 * Wrapper cho CallableStatement (gọi stored procedure).
 */
public class DBCallableStatement extends DBStatement {

	protected CallableStatement cs;

	public DBCallableStatement(DBAccessor dba, String sql) throws DBException {
		try {
			this.dba = dba;
			this.sql = sql;
			this.cs = dba.getConnection().prepareCall(sql);
			this.ps = cs;
			params = new StringBuilder().append(" param:");
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void registerOutParameter(int parameterIndex, int sqlType) throws DBException {
		try {
			params.append("[" + parameterIndex + "-OUT]");
			cs.registerOutParameter(parameterIndex, sqlType);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public String getString(int parameterIndex) throws DBException {
		try {
			return cs.getString(parameterIndex);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
