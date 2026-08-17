package fafoshop.common.database;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fafoshop.common.ConstantValue;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.LoopException;

/**
 * Wrapper mỏng quanh PreparedStatement. Khi gặp lỗi deadlock (mã 1213), ném
 * LoopException để AbstractProcess tự động retry.
 */
public class DBStatement {

	protected PreparedStatement ps;
	protected DBAccessor dba;
	protected String sql;
	protected StringBuilder params;

	protected DBStatement() {
	}

	public DBStatement(DBAccessor dba, String sql) throws DBException {
		try {
			this.dba = dba;
			this.sql = sql;
			this.ps = dba.getConnection().prepareStatement(sql);
			params = new StringBuilder().append(" param:");
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void close() throws DBException {
		try {
			ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public ResultSet executeQuery() throws DBException {
		try {
			dba.logSend("I", sql + "\n>" + params.toString());
			return ps.executeQuery();
		} catch (SQLException e) {
			if (e.getErrorCode() == ConstantValue.DEADLOCK_ERROR) {
				LoopException le = new LoopException();
				le.setSQLErrorCode(e.getErrorCode());
				throw le;
			}
			DBException dbException = new DBException(e);
			dbException.setSQLErrorCode(e.getErrorCode());
			throw dbException;
		}
	}

	public int executeUpdate() throws DBException {
		try {
			dba.logSend("I", sql + "\n>" + params.toString());
			return ps.executeUpdate();
		} catch (SQLException e) {
			if (e.getErrorCode() == ConstantValue.DEADLOCK_ERROR) {
				LoopException le = new LoopException();
				le.setSQLErrorCode(e.getErrorCode());
				throw le;
			}
			DBException de = new DBException();
			de.setSQLErrorCode(e.getErrorCode());
			throw de;
		}
	}

	public void setBinaryStream(int parameterIndex, InputStream x, Long binarySize) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			if (x == null) {
				ps.setBinaryStream(parameterIndex, null, 0);
			} else {
				ps.setBinaryStream(parameterIndex, x, binarySize);
			}
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void setString(int parameterIndex, String x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			if (x == null || x.isEmpty()) {
				ps.setString(parameterIndex, null);
			} else {
				ps.setString(parameterIndex, x);
			}
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void setLong(int parameterIndex, long x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			ps.setLong(parameterIndex, x);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void setInt(int parameterIndex, int x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			ps.setInt(parameterIndex, x);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	/**
	 * Set tham số kiểu số nguyên CÓ THỂ NULL thật (khác `setInt` chỉ nhận
	 * `int` nguyên thuỷ, không set được SQL NULL) — dùng cho cột nullable kiểu
	 * `Integer` (vd `sale_order_item.unit_qty`/`inbound_receipt_item.unit_qty`,
	 * xem docs/pos-da-don-vi-tinh.md).
	 */
	public void setNullableInt(int parameterIndex, Integer x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			if (x == null) {
				ps.setNull(parameterIndex, java.sql.Types.INTEGER);
			} else {
				ps.setInt(parameterIndex, x);
			}
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			ps.setBigDecimal(parameterIndex, x);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void setDate(int parameterIndex, java.sql.Date x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			ps.setDate(parameterIndex, x);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws DBException {
		try {
			params.append("[" + parameterIndex + "-" + x + "]");
			ps.setTimestamp(parameterIndex, x);
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
