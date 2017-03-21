package ink.aquar.scp;

public interface BasicMessenger {
	
	public void send(byte[] data);
	
	public void registerReceptor(String name, BasicReceptor receptor);
	
	public void unregisterReceptor(String name);

}
