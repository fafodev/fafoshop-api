package fafoshop.common.exception;

/**
 * Lỗi truy cập DB.
 */
public class DBException extends Exception {

	protected static final long serialVersionUID = 6152506782115905018L;

	protected int sqlErrorCode = -1;

	public DBException() {
		super();
	}

	public DBException(Exception e) {
		super(e);
	}

	public DBException(String message) {
		super(message);
	}

	public int getSQLErrorCode() {
		return sqlErrorCode;
	}

	public void setSQLErrorCode(int value) {
		sqlErrorCode = value;
	}
}
