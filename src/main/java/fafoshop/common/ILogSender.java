package fafoshop.common;

/**
 * Giao diện gửi log.
 */
public interface ILogSender {

	void logSend(String level, Throwable e);

	void logSend(String level, String message);

	String getOutputLevel();

	void setOutputLevel(String outputLevel);
}
