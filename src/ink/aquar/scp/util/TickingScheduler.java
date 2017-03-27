package ink.aquar.scp.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of Scheduler, creating a new Thread to sync the tasks.<br>
 * You can add delayed tasks into TickingScheduler.<br>
 * 
 * @see Scheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 */
public class TickingScheduler implements DelayableScheduler {
	
	private final static int DEFAULT_TICK_INTERVAL = 50;
	
	private final int tickInterval;
	
	private int time;
	
	private final IntegerMap<List<Runnable>> agenda = new IntegerMap<>();
	
	private boolean isRunning;
	private final Lock runningStatusLock = new ReentrantLock();
	
	private boolean isStopped;
	
	/**
	 * Create a TickingScheduler with default ticking interval 50ms.<br>
	 */
	public TickingScheduler() {
		this(DEFAULT_TICK_INTERVAL);
	}
	
	/**
	 * Create a TickingScheduler with a ticking interval.<br>
	 * <br>
	 * 
	 * @param tickInterval Interval between task flush, in millisecond
	 */
	public TickingScheduler(int tickInterval) {
		if(tickInterval < 1) tickInterval = 1;
		this.tickInterval = tickInterval;
	}
	
	@Override
	public void start() {
		runningStatusLock.lock();
		if(isRunning) {
			runningStatusLock.unlock();
			return;
		}
		isRunning = true;
		runningStatusLock.unlock();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				for(;;) {
					if(isStopped) break;
					synchronized (agenda) {
						List<Runnable> list = agenda.get(time);
						if(list != null) {
							for(int i=0;i<list.size();i++) {
								try{
									list.get(i).run();
								} catch (RuntimeException ex) {
									System.err.println(
											"Unhandled exception occured on schedule process"
											);
									ex.printStackTrace();
								}
							}
						}
						agenda.remove(time);
						sleep(tickInterval);
						time++;
					}
				}
				runningStatusLock.lock();
				isRunning = false;
				runningStatusLock.unlock();
			}
		}).start();
		
	}
	
	@Override
	public boolean isRunning() {
		runningStatusLock.lock();
		boolean isRunning = this.isRunning;
		runningStatusLock.unlock();
		return isRunning;
	}
	
	
	
	@Override
	public void stop() {
		isStopped = true;
	}
	
	@Override
	public void schedule(Runnable task) {
		schedule(task, 0);
	}
	
	@Override
	public void schedule(Runnable task, int delay) {
		if(delay<0) delay = 0;
		synchronized (agenda) {
			int time = this.time + delay / tickInterval;
			List<Runnable> list = agenda.get(time);
			if(list == null) {
				list = new LinkedList<>();
				agenda.set(time, list);
			}
			list.add(task);
		}
	}
	
	private static void sleep(int d){
		try {
			Thread.sleep(d);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
