package fafoshop.common.exception;

/**
 * Lỗi deadlock — AbstractProcess bắt exception này để tự động retry.
 */
public class LoopException extends DBException {

	private static final long serialVersionUID = -5345572391091687630L;

	public LoopException() {
		super();
	}

	public LoopException(Exception e) {
		super(e);
	}

	public LoopException(String message) {
		super(message);
	}
}
