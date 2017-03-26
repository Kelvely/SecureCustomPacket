package ink.aquar.scp;

/**
 * A communicator for library user to implement, you can connect it to Socket, Bukkit PluginMessage, etc.<br>
 * You also need to implement BasicReceptor to receive messages.<br>
 * 
 * You can try some erasure codings to avoid data damage!<br>
 * <h1>Reed Solomon Erasure Coding is recommended: </h1><link>https://github.com/Backblaze/JavaReedSolomon</link><br>
 * <br>
 * 
 * @see BasicReceptor
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface BasicMessenger {
	
	/**
	 * Send a message.<br>
	 * 
	 * @param data The content of the message
	 */
	public void send(byte[] data);
	
	/**
	 * To register a receptor with a unique channel name.<br>
	 * 
	 * @param name The unique channel name that you can unregister the receptor later
	 * @param receptor The receptor that receives the message
	 */
	public void registerReceptor(String name, BasicReceptor receptor);
	
	/**
	 * To unregister a receptor by its unique channel name.<br>
	 * 
	 * @param name The unique channel name set when register the receptor
	 */
	public void unregisterReceptor(String name);

}
