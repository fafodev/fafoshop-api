package fafoshop.common.utility;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích dùng chung cho AbstractProcess/AbstractWebService (ghi log stack
 * trace).
 */
public class CommonUtility {

	private CommonUtility() {
	}

	private static final DateTimeFormatter COMPACT_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	public static String getStackTraceString(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	/** Dùng cho log runtime — hiện "---" thay vì "null"/chuỗi rỗng khi chưa có userCode (trước khi đăng nhập). */
	public static String logUserCode(String userCode) {
		return (userCode == null || userCode.isEmpty()) ? "---" : userCode;
	}

	/**
	 * Timestamp dạng chuỗi "yyyyMMddHHmmssSSS" (17 ký tự, có mili giây) —
	 * dùng khi cần sinh mã/tên file có timestamp DỄ ĐỌC bằng mắt thường (ví
	 * dụ product_code, tên file export), thay vì số epoch millis khó đọc
	 * (vd "1784857386951"). Độ chính xác mili giây giữ nguyên như
	 * System.currentTimeMillis() nên không đổi rủi ro trùng lặp.
	 */
	public static String compactTimestamp() {
		return LocalDateTime.now().format(COMPACT_TIMESTAMP_FMT);
	}
}
