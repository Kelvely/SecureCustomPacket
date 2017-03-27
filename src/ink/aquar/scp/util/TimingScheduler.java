package ink.aquar.scp.util;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of DelayableScheduler.<br>
 * <br>
 * You can schedule delayed task to scheduler.<br>
 * <br>
 * However, the time is not very accurate, but enough for network.<br>
 * 
 * @see DelayableScheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public class TimingScheduler implements DelayableScheduler {
	
	private final long submitTimeout;
	private final TimeUnit submitTimeUnit;
	private final static long DEFAULT_TIMEOUT = 1000;
	private final static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
	
	private final LinkedList<DelayedTask> queue = new LinkedList<>();
	private final Lock queueLock = new ReentrantLock();
	private final Condition queueCondition = queueLock.newCondition();
	
	private final Lock runningLock = new ReentrantLock();
	private boolean isRunning;
	
	/**
	 * Create a TimingScheduler with a task submission timeout.<br>
	 *
	 * @param submitTimeout Task submission timeout
	 * @param submitTimeUnit Time unit of task submission timeout
	 */
	public TimingScheduler(long submitTimeout, TimeUnit submitTimeUnit) {
		this.submitTimeout = submitTimeout;
		this.submitTimeUnit = submitTimeUnit;
	}
	
	/**
	 * Create a TimingScheduler with default task submission timeout 1 second.<br>
	 */
	public TimingScheduler() {
		this(DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
	}

	@Override
	public void schedule(Runnable task) {
		schedule(task, 0);
	}

	@Override
	public void clear() {
		queueLock.lock();
		queue.clear();
		queueCondition.signalAll();
		queueLock.unlock();
	}

	@Override
	public void schedule(Runnable task, long delayInMillisec) {
		insertIntoQueue(task, delayInMillisec + System.currentTimeMillis());
		start();
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
						
						queueLock.lock();
						if(queue.isEmpty()) {
							queueCondition.await(submitTimeout, submitTimeUnit);
							boolean isStillEmpty = queue.isEmpty();
							runningLock.lock();
							if(isStillEmpty) {
								queueLock.unlock();
								isRunning = false;
								runningLock.unlock();
								return;
							} else {
								runningLock.unlock();
							}
						}
						DelayedTask delayedTask = queue.removeFirst();
						queueLock.unlock();
						
						
						long time = System.currentTimeMillis();
						
						if(delayedTask.time > time) {
							queueLock.lock();
							queue.addFirst(delayedTask);
							queueCondition.await(delayedTask.time - time, TimeUnit.MILLISECONDS);
							queueLock.unlock();
						} else {
							try {
								delayedTask.task.run();
							} catch (RuntimeException ex) {
								System.err.println(
										"Unhandled exception occured on schedule process"
										);
								ex.printStackTrace();
							}
						}
						
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
			
		}).start();
	}
	
	private void insertIntoQueue(Runnable task, long time) {
		queueLock.lock();
		
		if(queue.isEmpty()) {
			queue.add(new DelayedTask(task, time));
		} else if(queue.getFirst().time >= time) {
			queue.addFirst(new DelayedTask(task, time));
		} else if (queue.getLast().time <= time) {
			queue.addLast(new DelayedTask(task, time));
		} else {
			ListIterator<DelayedTask> iterator = queue.listIterator(1);
			while(iterator.hasNext()) {
				DelayedTask delayedTask = iterator.next();
				if(delayedTask.time <= time) {
					iterator.previous();
					iterator.add(new DelayedTask(task, time));
					break;
				}
			}
		}
		
		queueCondition.signalAll();
		
		queueLock.unlock();
	}
	
	private final static class DelayedTask {
		public final Runnable task;
		public final long time;
		
		public DelayedTask(Runnable task, long time) {
			this.task = task;
			this.time = time;
		}
	}

}
