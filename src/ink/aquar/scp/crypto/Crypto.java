package ink.aquar.scp.crypto;

import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;

public interface Crypto {
	
	/**
	 * When it failed to encrypt, null is returned.
	 */
	public byte[] encrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException;
	
	/**
	 * When it failed to decrypt, null is returned.
	 * @param data
	 * @param key
	 * @return
	 */
	public byte[] decrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException;

}
