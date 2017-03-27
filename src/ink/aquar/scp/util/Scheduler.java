package ink.aquar.scp.util;

/**
 * Scheduler to schedule tasks.<br>
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 */
public interface Scheduler {
	
	/**
	 * To schedule a task to the scheduler, it would run a little bit later.<br>
	 * 
	 * @param task The task
	 */
	public void schedule(Runnable task);
	
	/**
	 * To stop all tasks that is already scheduled.<br>
	 */
	public void clear();
	
}
