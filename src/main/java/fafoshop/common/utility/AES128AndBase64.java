package fafoshop.common.utility;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mã hoá/giải mã AES-128 + Base64 (AES/CBC/PKCS5Padding, Base64 URL-safe)
 * dùng khoá tĩnh riêng của dự án.
 */
public final class AES128AndBase64 {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String CIPHER_ALGORITHM = "AES";
	private static final String CIPHER_TRANSFORMATION = CIPHER_ALGORITHM + "/CBC/PKCS5Padding";

	// Khoá tĩnh riêng của dự án.
	private static final String STATIC_IV_STRING = "fIekrnFcv4nRQ2Qp";
	private static final String STATIC_KEY_STRING = "3cHfj1z0wKZCxvov";

	private static Cipher encryptor;
	private static Cipher decryptor;
	private static boolean initialized = false;

	private AES128AndBase64() {
	}

	synchronized public static String encrypt(String value) throws IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		if (!initialized) {
			init();
		}
		byte[] encrypted = encrypt(value.getBytes(UTF8));
		return Base64.getUrlEncoder().encodeToString(encrypted);
	}

	synchronized public static String decrypt(String value) throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		if (!initialized) {
			init();
		}
		return new String(decrypt(Base64.getUrlDecoder().decode(value)), UTF8);
	}

	synchronized private static void init() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		Key key = new SecretKeySpec(STATIC_KEY_STRING.getBytes(UTF8), CIPHER_ALGORITHM);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(STATIC_IV_STRING.getBytes(UTF8));
		encryptor = Cipher.getInstance(CIPHER_TRANSFORMATION);
		encryptor.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

		decryptor = Cipher.getInstance(CIPHER_TRANSFORMATION);
		decryptor.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

		initialized = true;
	}

	synchronized private static byte[] encrypt(byte[] src) throws IllegalBlockSizeException, BadPaddingException {
		return encryptor.doFinal(src);
	}

	synchronized private static byte[] decrypt(byte[] src) throws IllegalBlockSizeException, BadPaddingException {
		return decryptor.doFinal(src);
	}
}
