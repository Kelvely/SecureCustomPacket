package ink.aquar.scp.util;

public interface Scheduler {
	
	public void start();
	
	public void schedule(Runnable task);

	public boolean isRunning();
	
	public void stop();
	
}
