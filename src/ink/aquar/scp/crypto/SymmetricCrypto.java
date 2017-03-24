package ink.aquar.scp.crypto;

/**
 * Symmetric crypto, which is using the same key to encrypt and decrypt, generates 
 * symmetric key for corresponding crypto.<br>
 * 
 * @see Crypto
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface SymmetricCrypto extends Crypto {
	
	/**
	 * To generate a symmetric key for corresponding crypto.<br>
	 * 
	 * @return The key
	 */
	public byte[] generateKey();

}
