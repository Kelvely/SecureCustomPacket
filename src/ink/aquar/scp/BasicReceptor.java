package ink.aquar.scp;

/**
 * Basic and simple receptor interface that library user should implement 
 * in order to receive packets from BasicMessenger.<br>
 * 
 * @see BasicMessenger
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface BasicReceptor {
	
	/**
	 * When you got a message to receive.<br>
	 * 
	 * @param data Content of message
	 */
	public void receive(byte[] data);

}
