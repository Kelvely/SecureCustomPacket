package ink.aquar.scp;

public interface SecureReceiver {
	
	/**
	 * When another SecureDelivery try to send a data to you.
	 * 
	 * @param data The content of the data.
	 */
	public void receive(byte[] data);
	
	/**
	 * When another SecureDelivery try to connect to you.
	 * 
	 * On default, it returns true to accept the connection.
	 * 
	 * In addition, if false is returned, SecureDelivery will do nothing,
	 * if true is returned, public key would be responded.
	 * 
	 * @param datagram The extra data for connection
	 * @return If you want to accept the connection.
	 */
	public default boolean onConnect(byte[] datagram) {
		return true;
	}
	
	/**
	 * When another SecureDelivery offers a public key.
	 * On default, it returns true to establish the connection.
	 * 
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
	 * When disconnect by another SecureDelivery disconnected or timeout.
	 * 
	 * Extra data will be null as disconnection by timeout.
	 * 
	 * @param datagram The extra data for disconnection
	 */
	public void onDisconnect(byte[] datagram);

}
