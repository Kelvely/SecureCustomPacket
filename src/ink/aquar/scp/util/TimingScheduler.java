package ink.aquar.scp.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of DelayableScheduler.<br>
 * <br>
 * An application of Java Timer.<br>
 * 
 * @see DelayableScheduler
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 */
public class TimingScheduler implements DelayableScheduler {
	
	private final Timer timer = new Timer();
	
	private WrapperedBoolean currentSignal = new WrapperedBoolean();
	private final Lock signalLock = new ReentrantLock();

	@Override
	public void schedule(Runnable task) {
		schedule(task, 0);
	}

	@Override
	public void schedule(Runnable task, long delayInMillisec) {
		signalLock.lock();
		
		timer.schedule(new SignedTimerTask(currentSignal) {
			
			@Override
			public void run() {
				if(isCancelled.get()) return;
				task.run();
			}
			
		}, delayInMillisec);
		
		signalLock.unlock();
	}
	
	@Override
	public void clear() {
		signalLock.lock();
		currentSignal.set(true);
		currentSignal = new WrapperedBoolean();
		signalLock.unlock();
	}
	
	private abstract static class SignedTimerTask extends TimerTask {
		
		final WrapperedBoolean isCancelled;
		public SignedTimerTask(WrapperedBoolean signal) {
			isCancelled = signal;
		}
		
	}
	
	private final static class WrapperedBoolean {
		
		private boolean value;
		
		public boolean get() {
			return value;
		}
		
		public void set(boolean value) {
			this.value = value;
		}
		
	}

}
