package ink.aquar.scp.util;

/**
 * Scheduler to schedule tasks.<br>
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 */
public interface Scheduler {
	
	/**
	 * To start the scheduler, which start to run scheduled tasks and wait for new tasks.<br>
	 */
	public void start();
	
	/**
	 * To schedule a task to the scheduler, it would run a little bit later.<br>
	 * 
	 * @param task The task
	 */
	public void schedule(Runnable task);

	/**
	 * Check if the scheduler is running.<br>
	 * 
	 * @return If the scheduler is running
	 */
	public boolean isRunning();
	
	/**
	 * Stop the scheduler.<br>
	 */
	public void stop();
	
}
