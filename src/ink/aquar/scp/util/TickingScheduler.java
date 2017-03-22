package ink.aquar.scp.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TickingScheduler {
	
	private final static int DEFAULT_TICK_INTERVAL = 50;
	
	private final int tickInterval;
	
	private int time;
	
	private final IntegerMap<List<Runnable>> agenda = new IntegerMap<>();
	
	private boolean isRunning;
	private final Lock runningStatusLock = new ReentrantLock();
	
	private boolean isStopped;
	
	public TickingScheduler() {
		this(DEFAULT_TICK_INTERVAL);
	}
	
	public TickingScheduler(int tickInterval) {
		if(tickInterval < 1) tickInterval = 1;
		this.tickInterval = tickInterval;
	}
	
	public void start() {
		runningStatusLock.lock();
		if(isRunning) {
			runningStatusLock.unlock();
			return;
		}
		isRunning = true;
		runningStatusLock.unlock();
		
		new Thread(new Runnable() {
			public void run() {
				for(;;) {
					if(isStopped) break;
					synchronized (agenda) {
						List<Runnable> list = agenda.get(time);
						if(list != null) {
							for(int i=0;i<list.size();i++) {
								list.get(i).run();
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
	
	public boolean isRunning() {
		runningStatusLock.lock();
		boolean isRunning = this.isRunning;
		runningStatusLock.unlock();
		return isRunning;
	}
	
	
	
	public void stop() {
		isStopped = true;
	}
	
	public void schedule(Runnable task) {
		schedule(task, 0);
	}
	
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
	
	public final static class Wrapper {
		
		private final TickingScheduler scheduler;
		
		public Wrapper() {
			scheduler = new TickingScheduler();
		}
		
		public Wrapper(int tickInterval) {
			scheduler = new TickingScheduler(tickInterval);
		}
		
		
		public void schedule(Runnable task) {
			schedule(task, 0);
		}
		
		public void schedule(Runnable task, int delay) {
			synchronized (scheduler) {
				if(!scheduler.isRunning()) {
					scheduler.start();
				}
			}
			scheduler.schedule(task, delay);
		}
		
		public void stop(){
			scheduler.stop();
		}
		
		public boolean isRunning(){
			return scheduler.isRunning();
		}
		
	}

}
