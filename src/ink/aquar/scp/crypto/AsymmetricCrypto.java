package ink.aquar.scp.crypto;

/**
 * Asymmetric crypto, which is using different key to encrypt and decrypt, generates 
 * a pair of asymmetric key for corresponding crypto.<br>
 * <br>
 * For most of the time, asymmetric crypto are used to make a public region communication 
 * safe. A pair of asymmetric key contains encrypt key, which called <h1>Public Key</h1>, 
 * and a decrypt key, which called <h1>Private Key</h1>. A process of asymmetric encryption 
 * requires public key to encrypt the data, and private key to decrypt the encrypted data.<br>
 * Suppose we have 2 computers on the Internet. A wants to share a message to B in private. 
 * So B offers A its public key, which everyone can know, and A encrypt the message by the 
 * public key. Then A send the encrypted message to B by Internet. Because others doesn't have 
 * B's private key, they can't decrypt what A sent to B on Internet, but B has its private 
 * key, so it can decrypt the message that A sent to it. That's how asymmetric crypto works 
 * on real situations.<br>
 * 
 * @see Crypto
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface AsymmetricCrypto extends Crypto {
	
	public ByteKeyPair generateKeyPair();
	
	public final static class ByteKeyPair {
		public final byte[] publicKey;
		public final byte[] privateKey;
		
		public ByteKeyPair(byte[] publicKey, byte[] privateKey) {
			this.publicKey = publicKey;
			this.privateKey = privateKey;
		}
	}
	
}
