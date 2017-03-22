package ink.aquar.scp.crypto;

import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;

public class AESCrypto implements Crypto {

	@Override
	public byte[] encrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] decrypt(byte[] data, byte[] key) throws BadPaddingException, InvalidKeyException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static SecretKey restore(byte[] key) throws InvalidKeySpecException {
		return null;
	}

}
