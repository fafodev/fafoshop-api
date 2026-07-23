package fafoshop.common.exception;

/**
 * Lỗi hệ thống nghiêm trọng, không lường trước.
 */
public class FatalException extends Exception {

	protected static final long serialVersionUID = 8027046016920567409L;

	public FatalException() {
		super();
	}

	public FatalException(Exception e) {
		super(e);
	}

	public FatalException(String message) {
		super(message);
	}
}
