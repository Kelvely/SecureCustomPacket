package ink.aquar.scp.util;

/**
 * Scheduler that can schedule delayed task.
 * 
 * @see Scheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public interface DelayableScheduler extends Scheduler {
	
	/**
	 * To schedule a task with a delay.<br>
	 * 
	 * @param task The task
	 * @param delay Delay in millisecond
	 */
	public void schedule(Runnable task, long delayInMillisec);

}
