package ink.aquar.scp;

public interface Crypto {
	
	public byte[] encrypt(byte[] data, byte[] key);
	
	public byte[] decrypt(byte[] data, byte[] key);

}
