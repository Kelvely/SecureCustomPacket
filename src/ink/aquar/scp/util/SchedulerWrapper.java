package ink.aquar.scp.util;

/**
 * A easy using wrapper for Scheduler to schedule tasks without start it.<br>
 * <br>
 * When you schedule a task, it will automatically start.<br>
 * 
 * @see Scheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public class SchedulerWrapper {
	
	private final Scheduler scheduler;
	
	/**
	 * Create a SchedulerWrapper with a Scheduler.<br>
	 * 
	 * @param scheduler The scheduler
	 */
	public SchedulerWrapper(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	/**
	 * Schedule a task.<br>
	 * 
	 * @param task The task
	 */
	public void schedule(Runnable task) {
		synchronized (scheduler) {
			if(!scheduler.isRunning()) {
				scheduler.start();
			}
		}
		scheduler.schedule(task);
	}
	
	/**
	 * Stop the scheduler.<br>
	 */
	public void stop(){
		scheduler.stop();
	}
	
	/**
	 * Check if the scheduler is running.<br>
	 * 
	 * @return If the scheduler is running
	 */
	public boolean isRunning(){
		return scheduler.isRunning();
	}

}
