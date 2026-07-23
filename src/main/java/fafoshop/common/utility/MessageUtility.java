package fafoshop.common.utility;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Tra thông báo lỗi hệ thống — chỉ 1 file systemerror.properties dùng chung
 * (fafoshop chỉ có 1 cửa hàng, 1 ngôn ngữ tiếng Việt, không cần tra theo
 * nhiều khách hàng/nhiều ngôn ngữ).
 */
public class MessageUtility {

	private static final String SYSTEM_ERR_FILE = "systemerror";

	private MessageUtility() {
	}

	/**
	 * ResourceBundle.getBundle() mặc định đọc file .properties theo
	 * ISO-8859-1 (hành vi chuẩn của Java, kể cả Java 8/11) — trong khi
	 * systemerror.properties lưu UTF-8 thật (tiếng Việt có dấu) để dễ đọc/sửa
	 * trực tiếp, không encode kiểu native2ascii. Không ép UTF-8 ở đây sẽ làm
	 * mọi thông báo lỗi trả về client bị mojibake (đã phát hiện qua response
	 * thật khi test API: hiển thị sai ký tự tiếng Việt có dấu).
	 */
	private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
		@Override
		public ResourceBundle newBundle(String baseName, java.util.Locale locale, String format,
				ClassLoader loader, boolean reload) throws java.io.IOException {
			String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
			try (Reader reader = new InputStreamReader(loader.getResourceAsStream(resourceName),
					StandardCharsets.UTF_8)) {
				return new PropertyResourceBundle(reader);
			}
		}
	};

	synchronized private static ResourceBundle getBundle() {
		try {
			return ResourceBundle.getBundle(SYSTEM_ERR_FILE, java.util.Locale.getDefault(), UTF8_CONTROL);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Lấy nội dung lỗi hệ thống (tiếng Việt) theo mã lỗi.
	 * @param key mã lỗi, ví dụ "MC000001"
	 * @return nội dung lỗi, hoặc chuỗi rỗng nếu không tìm thấy
	 */
	synchronized public static String getSystemErrMsg(String key) {
		ResourceBundle bundle = getBundle();
		try {
			if (bundle != null) {
				return bundle.getString(key);
			}
		} catch (Exception e) {
			// không tìm thấy key — trả rỗng, không throw để không chặn luồng lỗi khác
		}
		return "";
	}
}
