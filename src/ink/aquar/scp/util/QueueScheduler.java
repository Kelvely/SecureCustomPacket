package ink.aquar.scp.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of Scheduler.<br>
 * <br>
 * This is a synchronized scheduler.<br>
 * 
 * @see Scheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public class QueueScheduler implements Scheduler {
	
	private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	
	private final long submitTimeout;
	private final TimeUnit submitTimeUnit;
	private final static long DEFAULT_TIMEOUT = 1000;
	private final static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
	
	private final Lock runningLock = new ReentrantLock();
	private boolean isRunning;
	
	public QueueScheduler(long submitTimeout, TimeUnit submitTimeUnit) {
		this.submitTimeout = submitTimeout;
		this.submitTimeUnit = submitTimeUnit;
	}
	
	public QueueScheduler() {
		this(DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
	}

	@Override
	public void schedule(Runnable task) {
		queue.offer(task);
		start();
	}

	@Override
	public void clear() {
		queue.clear();
	}
	
	private void start() {
		runningLock.lock();
		if(isRunning) {
			runningLock.unlock();
			return;
		} else {
			isRunning = true;
			runningLock.unlock();
		}
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				for(;;) {
					try {
						
						Runnable task = queue.poll(submitTimeout, submitTimeUnit);
						
						runningLock.lock();
						if(task == null) {
							isRunning = false;
							runningLock.unlock();
							return;
						} else {
							runningLock.unlock();
						}
						
						try {
	        				task.run();
	        			} catch (RuntimeException ex) {
							System.err.println(
									"Unhandled exception occured on schedule process"
									);
							ex.printStackTrace();
						}
						
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
		}).start();
	}

}
