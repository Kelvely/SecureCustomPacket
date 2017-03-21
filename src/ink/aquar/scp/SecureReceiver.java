package ink.aquar.scp;

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
	 * By default, it returns true to accept the connection.<br>
	 * <br>
	 * In addition, if false is returned, SecureDelivery will do nothing,
	 * if true is returned, public key would be responded.<br>
	 * <br>
	 * @param datagram The extra data for connection
	 * @return If you want to accept the connection.
	 */
	public default boolean onConnect(byte[] datagram) {
		return true;
	}
	
	/**
	 * When another SecureDelivery offers a public key.<br>
	 * By default, it returns true to establish the connection.<br>
	 * <br>
	 * @param publicKey The public key
	 * @return if the connection should be established
	 */
	public default boolean onPublicKeyRespond(byte[] publicKey) {
		return true;
	}
	
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
	 * @param datagram The extra data for disconnection
	 */
	public void onDisconnect(byte[] datagram);

}
