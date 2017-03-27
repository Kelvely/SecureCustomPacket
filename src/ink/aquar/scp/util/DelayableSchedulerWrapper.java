package ink.aquar.scp.util;

/**
 * A SchedulerWrapper that is prepared for DelayableScheduler, allowing you to 
 * schedule delayed task with the wrapper.<br>
 * 
 * @see SchedulerWrapper
 * @see DelayableScheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public class DelayableSchedulerWrapper extends SchedulerWrapper {

	private final DelayableScheduler scheduler;
	
	/**
	 * Create a TickingScheduler.Wrapper with a TickingScheduler.<br>
	 * 
	 * @param scheduler The ticking scheduler
	 */
	public DelayableSchedulerWrapper(DelayableScheduler scheduler) {
		super(scheduler);
		this.scheduler = scheduler;
	}
	
	
	@Override
	public void schedule(Runnable task) {
		schedule(task, 0);
	}
	
	/**
	 * Schedule a delayed task.<br>
	 * 
	 * @param task The task
	 * @param delay Delay in millisecond
	 */
	public void schedule(Runnable task, long delay) {
		synchronized (scheduler) {
			if(!scheduler.isRunning()) {
				scheduler.start();
			}
		}
		scheduler.schedule(task, delay);
	}

}
