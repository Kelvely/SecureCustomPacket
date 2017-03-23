package ink.aquar.scp.crypto;

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
