package fafoshop.common;

/**
 * Hằng số dùng chung cho AbstractProcess/CheckAuthProcess/DBStatement.
 */
public class ConstantValue {

	/** Mức log mặc định (F=Fatal, E=Error, W=Warning, I=Information, T=Test, D=Debug) */
	public static String LOG_OUTPUT_LEVEL = "FEWITD";

	/** Mã lỗi MySQL khi deadlock — dùng để quyết định có retry hay không */
	public static Integer DEADLOCK_ERROR = 1213;

	/** Loại lỗi trong ProcessCheckErrorException */
	public static Integer NORMAL_ERROR = 0;
	public static Integer FATAL_ERROR = 1;

	/**
	 * PRCSID (tên class Process) của luồng đăng nhập — được checkAuth() bỏ qua
	 * vì lúc đó user chưa có quyền gì để kiểm tra.
	 */
	public static String LOGIN_PROCESS_ID = "AuthLoginProcess";
}
