package fafoshop.common.utility;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;

/**
 * Phát hành/kiểm tra token đăng nhập, lưu vào bảng session_token. Token chỉ
 * có 2 trạng thái: hợp lệ (còn hạn, đúng user) hoặc không hợp lệ — không cần
 * cơ chế chống double-submit phức tạp hơn cho quy mô 1 cửa hàng.
 */
public final class IdTokenUtility {

	private IdTokenUtility() {
	}

	/** Số phút hết hạn phiên (đọc từ session.properties) — dùng để tính cả
	 * expire_datetime lưu DB lẫn Max-Age của cookie phiên (AuthWebService). */
	public static int getSessionMinutes() throws IOException {
		try (InputStream io = IdTokenUtility.class.getResourceAsStream("/session.properties")) {
			Properties properties = new Properties();
			properties.load(io);
			return Integer.parseInt(properties.getProperty("time"));
		}
	}

	/**
	 * Phát hành token mới cho user đã đăng nhập thành công, lưu vào
	 * session_token, trả về token đã mã hoá AES cho client.
	 */
	synchronized public static String generate(String usrCd) throws FatalException {

		DBAccessor dba = null;
		DBStatement ps = null;

		try {
			int sessionMinutes = getSessionMinutes();
			Timestamp expireDatetime = new Timestamp(System.currentTimeMillis() + 60_000L * sessionMinutes);

			String rawToken = usrCd + ":" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ":"
					+ UUID.randomUUID();

			dba = new DBAccessor();
			ps = dba.prepareStatement(
					"INSERT INTO session_token (token, user_code, expire_datetime) VALUES (?, ?, ?)");
			ps.setString(1, rawToken);
			ps.setString(2, usrCd);
			ps.setTimestamp(3, expireDatetime);
			ps.executeUpdate();
			ps.close();
			dba.commit();

			return AES128AndBase64.encrypt(rawToken);

		} catch (Exception e) {
			try {
				if (dba != null) dba.rollback();
			} catch (DBException ignore) {
			}
			throw new FatalException(e);
		} finally {
			try {
				if (dba != null) dba.disconnect();
			} catch (DBException ignore) {
			}
		}
	}

	/**
	 * Kiểm tra token, trả về mã người dùng nếu hợp lệ, null nếu không.
	 */
	synchronized public static String verify(String encryptedToken) throws FatalException, DBException {

		if (encryptedToken == null || encryptedToken.isEmpty()) {
			return null;
		}

		DBAccessor dba = null;
		DBStatement ps = null;
		ResultSet rs = null;

		try {
			String rawToken;
			try {
				rawToken = AES128AndBase64.decrypt(encryptedToken);
			} catch (Exception e) {
				return null;
			}

			dba = new DBAccessor();
			ps = dba.prepareStatement(
					"SELECT user_code FROM session_token WHERE token = ? AND expire_datetime > CURRENT_TIMESTAMP");
			ps.setString(1, rawToken);
			rs = ps.executeQuery();

			String usrCd = null;
			if (rs.next()) {
				usrCd = rs.getString("user_code");
			}
			dba.commit();
			return usrCd;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (dba != null) dba.disconnect();
			} catch (Exception ignore) {
			}
		}
	}

	/**
	 * Huỷ token (đăng xuất) — xoá khỏi session_token, dùng cho luồng logout để
	 * cookie/token cũ không còn dùng lại được nữa dù chưa hết hạn tự nhiên.
	 * Token không hợp lệ/không giải mã được thì coi như đã đăng xuất, không
	 * báo lỗi (logout phải luôn thành công theo góc nhìn client).
	 */
	synchronized public static void revoke(String encryptedToken) throws FatalException, DBException {

		if (encryptedToken == null || encryptedToken.isEmpty()) {
			return;
		}

		DBAccessor dba = null;
		DBStatement ps = null;

		try {
			String rawToken;
			try {
				rawToken = AES128AndBase64.decrypt(encryptedToken);
			} catch (Exception e) {
				return;
			}

			dba = new DBAccessor();
			ps = dba.prepareStatement("DELETE FROM session_token WHERE token = ?");
			ps.setString(1, rawToken);
			ps.executeUpdate();
			ps.close();
			dba.commit();

		} catch (DBException e) {
			try {
				if (dba != null) dba.rollback();
			} catch (DBException ignore) {
			}
			throw e;
		} finally {
			try {
				if (ps != null) ps.close();
				if (dba != null) dba.disconnect();
			} catch (Exception ignore) {
			}
		}
	}
}
