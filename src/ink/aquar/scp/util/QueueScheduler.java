package ink.aquar.scp.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QueueScheduler implements Scheduler {
	
	private final List<Runnable> schedule = new LinkedList<Runnable>();
	private final Lock scheduleLock = new ReentrantLock();
	
	private final Lock awaitLock = new ReentrantLock();
	private final Condition awaitCondition = awaitLock.newCondition();
	
	private boolean isRunning;
	private final Lock runningStatusLock = new ReentrantLock();
	
	private boolean isStopped;
	
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
		        	try{
		        		scheduleLock.lock();
		        		if(schedule.size() > 0) {
		        			Runnable task = schedule.remove(0);
		        			scheduleLock.unlock();
		        			try {
		        				task.run();
		        			} catch (RuntimeException ex) {
								System.err.println(
										"Unhandled exception occured on schedule process"
										);
								ex.printStackTrace();
							}
		        		} else {
		        			scheduleLock.unlock();
		        			awaitLock.lock();
		        			awaitCondition.await();
		        			awaitLock.unlock();
		        		}
		        	}catch(InterruptedException ex) {
		        		ex.printStackTrace();
		        	}
				}
				
				runningStatusLock.lock();
				isRunning = false;
				runningStatusLock.unlock();
			}
		}).start();
	}
	
	public void schedule(Runnable task) {
		scheduleLock.lock();
        schedule.add(task);
        awaitLock.lock();
        awaitCondition.signalAll();
        awaitLock.unlock();
        scheduleLock.unlock();
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

}
