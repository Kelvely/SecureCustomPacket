package ink.aquar.scp;

/**
 * The interface that library user should implement in order to receive packets from SecureDelivery.<br>
 * 
 * @see SecureDelivery
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface SecureReceiver {
	
	/**
	 * When another SecureDelivery try to send a data to you.<br>
	 * <br>
	 * @param tag The tag of data
	 * @param data The content of the data.
	 */
	public void receive(int tag, byte[] data);
	
	/**
	 * When a data is confirmed by target SecureDelivery.<br>
	 * <br>
	 * @param tag The tag of data
	 */
	public void postConfirm(int tag);
	
	/**
	 * When a data is broken on target SecureDelivery.<br>
	 * Most of the time, you can re-send the data.<br>
	 * <br>
	 * @param tag The tag of data
	 */
	public void postBroken(int tag);
	
	/**
	 * When another SecureDelivery try to connect to you.<br>
	 * <br>
	 * You HAVE TO respond MANUALLY by SecureDelivery.respondConnect(boolean)!<br>
	 * <br>
	 * @param datagram The extra data for connection
	 */
	public void onConnect(byte[] datagram);
	
	/**
	 * When another SecureDelivery offers a public key.<br>
	 * <br>
	 * You HAVE TO respond MANUALLY by SecureDelivery.respondPublicKey(boolean)!<br>
	 * <br>
	 * If you want to delay the confirmation, you have to keep up the wait status MANUALLY 
	 * by SecureDelivery.connectStandBy().<br>
	 * <br>
	 * @param publicKey The public key
	 */
	public boolean onPublicKeyRespond(byte[] publicKey);
	
	/**
	 * When the connection is established.
	 */
	public void onConnectionEstablish();
	
	/**
	 * When disconnect by another SecureDelivery disconnected or timeout.<br>
	 * When that SecureDelivery denied your public key, it also send a disconnect message.<br>
	 * <br>
	 * Extra data will be null as disconnection by timeout.<br>
	 * <br>
	 * If you want to delay the confirmation, you have to keep up the wait status MANUALLY 
	 * by SecureDelivery.publicKeyStandBy().<br>
	 * <br>
	 * @param datagram The extra data for disconnection
	 */
	public void onDisconnect(byte[] datagram);

}
