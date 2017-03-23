package ink.aquar.scp.util;

public class SchedulerWrapper {
	
	private final Scheduler scheduler;
	
	public SchedulerWrapper(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	
	public void schedule(Runnable task) {
		synchronized (scheduler) {
			if(!scheduler.isRunning()) {
				scheduler.start();
			}
		}
		scheduler.schedule(task);
	}
	
	public void stop(){
		scheduler.stop();
	}
	
	public boolean isRunning(){
		return scheduler.isRunning();
	}

}
