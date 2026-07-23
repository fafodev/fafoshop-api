package fafoshop.common.utility;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Băm/kiểm tra mật khẩu người dùng bằng PBKDF2WithHmacSHA256 (có sẵn trong
 * JDK, không cần thêm thư viện) — không bao giờ lưu/so sánh mật khẩu dạng
 * plaintext.
 *
 * Định dạng lưu trong cột password_hash (varchar(255)):
 *   "{số vòng lặp}:{salt base64}:{hash base64}"
 */
public final class PasswordUtility {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 120_000;
	private static final int KEY_LENGTH_BITS = 256;
	private static final int SALT_LENGTH_BYTES = 16;

	private PasswordUtility() {
	}

	public static String hash(String rawPassword) {
		byte[] salt = new byte[SALT_LENGTH_BYTES];
		new SecureRandom().nextBytes(salt);
		byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
		return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
				+ Base64.getEncoder().encodeToString(hash);
	}

	public static boolean verify(String rawPassword, String storedHash) {
		if (storedHash == null || storedHash.isEmpty()) {
			return false;
		}
		String[] parts = storedHash.split(":");
		if (parts.length != 3) {
			return false;
		}
		int iterations = Integer.parseInt(parts[0]);
		byte[] salt = Base64.getDecoder().decode(parts[1]);
		byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
		byte[] actualHash = pbkdf2(rawPassword, salt, iterations);
		return constantTimeEquals(expectedHash, actualHash);
	}

	private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < a.length; i++) {
			diff |= a[i] ^ b[i];
		}
		return diff == 0;
	}
}
