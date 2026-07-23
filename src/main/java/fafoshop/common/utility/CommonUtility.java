package fafoshop.common.utility;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tiện ích dùng chung cho AbstractProcess/AbstractWebService (ghi log stack
 * trace).
 */
public class CommonUtility {

	private CommonUtility() {
	}

	public static String getStackTraceString(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	/** Dùng cho log runtime — hiện "---" thay vì "null"/chuỗi rỗng khi chưa có userCode (trước khi đăng nhập). */
	public static String logUserCode(String userCode) {
		return (userCode == null || userCode.isEmpty()) ? "---" : userCode;
	}
}
