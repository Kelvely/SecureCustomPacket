package ink.aquar.scp.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESCrypto implements Crypto {
	
	private final static String KEY_ALGORITHM = "AES";
	private final static String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
	
	private final static KeyGenerator KEY_GENERATOR;
	static {
		KeyGenerator keyGenerator = null;
		try {
			keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		keyGenerator.init(128);
		KEY_GENERATOR = keyGenerator;
	}

	@Override
	public byte[] encrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException {
		SecretKey secretKey = toKey(key);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return cipher.doFinal(data);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] decrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException {
		SecretKey secretKey = toKey(key);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return cipher.doFinal(data);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static SecretKey toKey(byte[] key) {
		return new SecretKeySpec(key, KEY_ALGORITHM);
	}
	
	public static SecretKey genKeyPair(){
		return KEY_GENERATOR.generateKey();
	}

}
