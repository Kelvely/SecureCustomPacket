package ink.aquar.scp.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedList;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSACrypto implements AsymmetricCrypto {
	
	private final static String KEY_ALGORITHM = "RSA";
	private final static String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
	
	private final static byte[][] EMPTY_2D_BYTE_ARRAY = new byte[0][];
	
	private final static int PUBLIC_KEY_LIMIT_DASH = 11;
	
	private final static KeyPairGenerator KEY_PAIR_GENERATOR;
	static {
		KeyPairGenerator keyPairGenerator = null;
		try {
			keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		keyPairGenerator.initialize(2048);
		KEY_PAIR_GENERATOR = keyPairGenerator;
	}

	@Override
	public byte[] encrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException { //Public key
		RSAPublicKey publicKey;
		try {
			publicKey = (RSAPublicKey) restorePublicKey(key);
		} catch (InvalidKeySpecException ex) {
			throw new InvalidKeyException(ex.getMessage());
		}
		int keyLen = publicKey.getModulus().bitLength();
		byte[][] marshalling = split(data, keyLen / 8 - PUBLIC_KEY_LIMIT_DASH);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			for(int i=0;i<marshalling.length;i++){
				marshalling[i] = cipher.doFinal(marshalling[i]);
			}
			return organize(marshalling);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] decrypt(byte[] data, byte[] key) throws InvalidKeyException, BadPaddingException {
		RSAPrivateKey privateKey;
		try {
			privateKey = (RSAPrivateKey) restorePrivateKey(key);
		} catch (InvalidKeySpecException ex) {
			throw new InvalidKeyException(ex.getMessage());
		}
		int keyLen = privateKey.getModulus().bitLength();
		byte[][] marshalling = split(data, keyLen / 8);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			for(int i=0;i<marshalling.length;i++){
				marshalling[i] = cipher.doFinal(marshalling[i]);
			}
			return organize(marshalling);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private static PublicKey restorePublicKey(byte[] keyBytes) throws InvalidKeySpecException {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
		
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
			PublicKey publicKey = factory.generatePublic(x509EncodedKeySpec);
			return publicKey;
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private static PrivateKey restorePrivateKey(byte[] keyBytes) throws InvalidKeySpecException {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
			PrivateKey privateKey = factory.generatePrivate(pkcs8EncodedKeySpec);
			return privateKey;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static byte[] organize(byte[][] list) {
		int totalLen = 0;
		for (byte[] bytes : list) {
			totalLen += bytes.length;
		}
		byte[] result = new byte[totalLen];
		int pointer = 0;
		for(int i=0;i<list.length;i++) {
			byte[] bytes = list[i];
			System.arraycopy(bytes, 0, result, pointer, bytes.length);
			pointer += bytes.length;
		}
		return result;
	}
	
	private static byte[][] split(byte[] data, int len) {
		List<byte[]> marshalling = new LinkedList<>();
		int pointer = 0;
		while(true) {
			int dest = pointer + len;
			byte[] bytes;
			if(dest < data.length) {
				bytes = new byte[dest - pointer];
				System.arraycopy(data, pointer, bytes, 0, bytes.length);
				marshalling.add(bytes);
				pointer = dest;
			} else {
				dest = data.length;
				bytes = new byte[dest - pointer];
				System.arraycopy(data, pointer, bytes, 0, bytes.length);
				marshalling.add(bytes);
				break;
			}
		}
		return marshalling.toArray(EMPTY_2D_BYTE_ARRAY);
	}

	@Override
	public ByteKeyPair generateKeyPair() {
		KeyPair keyPair = KEY_PAIR_GENERATOR.generateKeyPair();
		return new ByteKeyPair(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
	}

}
