package ink.aquar.scp;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ink.aquar.scp.crypto.AESCrypto;
import ink.aquar.scp.crypto.Crypto;
import ink.aquar.scp.crypto.RSACrypto;
import ink.aquar.scp.util.TickingScheduler;

import java.util.Set;

/**
 * 
 * <h1>If you want to deny some requester's connection, please deny it on BasicReceptor implementation.<br>
 * <br>
 * This Delivery is one-to-one connection, thus server need multiple deliveries.<br>
 * 
 * @author Aquarink Studio
 *
 */
public class SecureDelivery {
	
	private final static byte[] EMPTY_BYTE_ARRAY = {};
	
	private final static byte[] DEFAULT_PUBLIC_KEY;
	private final static byte[] DEFAULT_PRIVATE_KEY;
	static {
		KeyPair keyPair = RSACrypto.genKeyPair();
		DEFAULT_PRIVATE_KEY = keyPair.getPrivate().getEncoded();
		DEFAULT_PUBLIC_KEY = keyPair.getPublic().getEncoded();
	}
	
	private final static Crypto DEFAULT_ASYM_CRYPTO = new RSACrypto();
	private final static Crypto DEFAULT_SYM_CRYPTO = new AESCrypto();
	
	private final static Random RANDOM = new Random();
	
	private final static TickingScheduler SCHEDULER = new TickingScheduler();
	static {
		SCHEDULER.start();
	}
	
	
	private final byte[] publicKey;
	private final byte[] privateKey;
	
	private byte[] sessionKey;
	private int sessionId;
	
	private final Crypto asymCrypto;
	private final Crypto symCrypto;
	
	private final BasicMessenger basicMessenger;
	
	public final String basicReceptorChannelName;
	
	private byte connectionStage;
	
	private int keepAliveInterval = 5000;
	private int deadTime = 20000;
	private int requestTimeout = 10000;
	private int requestReSends = 3;
	
	private final Map<String, SecureReceiver> receivers = new HashMap<>();
	private final ReadWriteLock receiversRWL = new ReentrantReadWriteLock();
	
	public SecureDelivery(String channelName, BasicMessenger basicMessenger) {
		this(channelName, basicMessenger, DEFAULT_PUBLIC_KEY, DEFAULT_PRIVATE_KEY, DEFAULT_ASYM_CRYPTO, DEFAULT_SYM_CRYPTO);
	}
	
	public SecureDelivery(
			String channelName, BasicMessenger basicMessenger, 
			byte[] publicKey, byte[] privateKey, 
			Crypto asymCrypto, Crypto symCrypto) {
		basicReceptorChannelName = channelName;
		this.basicMessenger = basicMessenger;
		basicMessenger.registerReceptor(channelName, new LowLevelReceptor());
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.asymCrypto = asymCrypto;
		this.symCrypto = symCrypto;
	}
	
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}
	
	public void setKeepAliveInterval(int keepAliveInterval) {
		if(keepAliveInterval < 1000) keepAliveInterval = 1000;
		this.keepAliveInterval = keepAliveInterval;
	}
	
	public int getDeadTime() {
		return deadTime;
	}
	
	public void setDeadTime(int deadTime) {
		if(deadTime < 1000) deadTime = 1000;
		this.deadTime = deadTime;
	}
	
	public int getRequestTimeout() {
		return requestTimeout;
	}
	
	public void setRequestTimeout(int requestTimeout) {
		if(requestTimeout < 1000) requestTimeout = 1000;
		this.requestTimeout = requestTimeout;
	}
	
	public int getRequestReSends() {
		return requestReSends;
	}
	
	public void setRequestReSends(int requestReSends) {
		if(requestReSends < 0) requestReSends = 0;
		this.requestReSends = requestReSends;
	}
	
	public void send(int tag, byte[] data) {
		if(connectionStage < 4) throw new NoConnectionException();
		// TODO
	}
	
	public void connect() {
		connect(EMPTY_BYTE_ARRAY);
	}
	
	public void connect(byte[] datagram) {
		// TODO
	}
	
	public void connect(String message) {
		connect(message.getBytes());
	}
	
	public void disconnect() {
		disconnect(EMPTY_BYTE_ARRAY);
	}
	
	public void disconnect(byte[] datagram) {
		if(connectionStage < 4) throw new NoConnectionException();
		// TODO
	}
	
	public void disconnect(String message) {
		disconnect(message.getBytes());
	}
	
	/**
	 * Manually send keep alive packet.
	 */
	public void keepAlive() {
		if(connectionStage < 4) throw new NoConnectionException();
		// TODO
	}
	
	public void registerReceiver(String channelName, SecureReceiver receiver) {
		receiversRWL.writeLock().lock();
		receivers.put(channelName, receiver);
		receiversRWL.writeLock().unlock();
	}
	
	public void unregisterReceiver(String channelName) {
		receiversRWL.writeLock().lock();
		receivers.remove(channelName);
		receiversRWL.writeLock().unlock();
	}
	
	public int getConnectionStage(){
		return connectionStage;
	}
	
	private final class LowLevelReceptor implements BasicReceptor {

		@Override
		public void receive(byte[] data) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/** 
	 * 	Requester							Acceptor
	 * 		|									|
	 * 		|		   connect request			|
	 * 		|---------------------------------->| Stage 1
	 * 		|									|
	 * 		|		 public key offering		|
	 * 		|<----------------------------------| Stage 2
	 * 		|									|
	 * 		|		generate session key		|
	 * 		|	  and encrypt by public key		|
	 * 		|---------------------------------->| Stage 3
	 * 		|									|
	 * 		|		connection established		|
	 * 		|<----------------------------------| Stage 4
	 * 		|									|
	 */
	
	/*
	 * Form
	 * | HEAD CRC | SESSION ID | OPERATION | DATA CRC | DATAGRAM |
	 *      8B          4B          1B          8B     length-21B
	 */
	@SuppressWarnings("unused")
	private final static class ExplicitOperations {
		// TODO You should use these operations
		public final static byte DISCONNECT = 0; //Disconnection can be both implicit and explicit.
		public final static byte CONNECT = 1;
		public final static byte PUBLIC_KEY_OFFER = 2;
		public final static byte START_SESSION = 3;
	}
	
	@SuppressWarnings("unused")
	private final static class ImplicitOperations {
		// TODO And also here
		public final static byte DISCONNECT = 0; //However, disconnect(byte[])'s extra data is encrypted.
		public final static byte CONFIRM_SESSION = 4;
		public final static byte CONNECTION_ESTABLISH = 5;
		public final static byte SEND_DATA = 6;
		public final static byte CONFIRM_DATA = 7;
		public final static byte BROKEN_DATA = 8;
	}
	
	/**
	 * You should notice that no one said the alive-keeper works on time :P
	 * Tiny delay is here. 
	 * 
	 * @author Aquarink
	 *
	 */
	private final static class KeepAliveScheduler implements Runnable {
		
		private final static KeepAliveScheduler KEEP_ALIVE_SCHEDULER = new KeepAliveScheduler();
		private static boolean started;
		
		private int time;
		
		private final Set<KeepAliveWrapper> agenda = new HashSet<>();
		
		private static void startIfNotStarted() {
			if(!started) {
				new Thread(KEEP_ALIVE_SCHEDULER).start();
			}
		}

		@Override
		public void run() {
			for(;;) {
				plus1s();
				
				synchronized (agenda) {
					Iterator<KeepAliveWrapper> iterator = agenda.iterator();
					while(iterator.hasNext()) {
						KeepAliveWrapper wrapper = iterator.next();
						if(wrapper.next == time) {
							try {
								wrapper.delivery.keepAlive();
								wrapper.next += wrapper.delay;
							} catch (NoConnectionException ex) {
								agenda.remove(wrapper);
							}
						} else if (wrapper.next < time) {
							agenda.remove(wrapper);
						}
					}
				}
				
				time++;
			}
		}
		
		/**
		 * Big news θ..θ!
		 */
		private static void plus1s(){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public void plan(SecureDelivery delivery, int delay) {
			synchronized (agenda) {
				agenda.add(new KeepAliveWrapper(delivery, delay, time));
			}
		}
		
		/**
		 * @param delivery
		 * @param delay By second
		 */
		public static void schedule(SecureDelivery delivery, int delay) {
			startIfNotStarted();
			KEEP_ALIVE_SCHEDULER.plan(delivery, delay);
		}
		
		private final static class KeepAliveWrapper {
			
			public final SecureDelivery delivery;
			
			public final int delay;
			
			public int next;
			
			public KeepAliveWrapper(SecureDelivery delivery, int delay, int time) {
				this.delivery = delivery;
				this.delay = delay;
				next = delay + time;
			}
			
			@Override
			public int hashCode() {
				return delivery.hashCode();
			}
			
			@Override
			public boolean equals(Object obj) {
				return delivery.equals(obj);
			}
			
		}
	
	}
	
	private final static class ConnectionReaper implements Runnable {
		
		private final static ConnectionReaper CONNECTION_REAPER = new ConnectionReaper();
		private static boolean started;
		
		private int time;
		
		private final Map<SecureDelivery, Integer> agenda = new HashMap<>();
		
		private static void startIfNotStarted() {
			if(!started) {
				new Thread(CONNECTION_REAPER).start();
			}
		}

		@Override
		public void run() {
			for(;;) {
				plus1s();
				
				synchronized (agenda) {
					List<SecureDelivery> deliveriesDied = new LinkedList<>();
					
					for(Entry<SecureDelivery, Integer> entry : agenda.entrySet()) {
						int timeToDie = entry.getValue();
						if(timeToDie <= time) {
							entry.getKey().disconnect(); // TODO Not really like this :P
							deliveriesDied.add(entry.getKey());
						}
					}
					
					Iterator<SecureDelivery> iterator = deliveriesDied.iterator();
					while (iterator.hasNext()) {
						agenda.remove(iterator.next());
					}
				}
				
				time++;
			}
		}
		
		private static void plus1s(){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public void elongate(SecureDelivery delivery, int delay) {
			synchronized (agenda) {
				agenda.put(delivery, time + delay);
			}
		}
		
		/**
		 * @param delivery
		 * @param delay By second
		 */
		public static void keep(SecureDelivery delivery, int delay) {
			startIfNotStarted();
			CONNECTION_REAPER.elongate(delivery, delay);
		}
		
	}

}
