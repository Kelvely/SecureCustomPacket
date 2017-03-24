package ink.aquar.scp.crypto;

import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;

/**
 * A wrapper to simplify the encryption and decryption process.<br>
 * Key is byte sequence in order to simplify the process creating the corresponding key<br>
 * <br>
 * <h1>P.S. You should use algorithms that are proved safe, do NOT create your own crypto 
 * algorithm unless you are a cryptography professor.</h1><br>
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface Crypto {
	
	/**
	 * Encrypt data by the key.<br>
	 * 
	 * @param data The data that you want to encrypt
	 * @param key The key
	 */
	public byte[] encrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException;
	
	/**
	 * Decrypt data by the key.<br>
	 * 
	 * @param data The encrypted data that you want to decrypt
	 * @param key The key
	 */
	public byte[] decrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException;

}
