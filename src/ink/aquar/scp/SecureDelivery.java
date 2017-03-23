package ink.aquar.scp;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.crypto.BadPaddingException;

import org.jacoco.core.internal.data.CRC64;

import ink.aquar.scp.crypto.AESCrypto;
import ink.aquar.scp.crypto.Crypto;
import ink.aquar.scp.crypto.RSACrypto;
import ink.aquar.scp.util.ByteWrapper;
import ink.aquar.scp.util.ByteWrapper.OutputType;
import ink.aquar.scp.util.QueueScheduler;
import ink.aquar.scp.util.Scheduler;
import ink.aquar.scp.util.SchedulerWrapper;
import ink.aquar.scp.util.TickingScheduler;

/**
 * 
 * <h1>If you want to deny some requester's connection, please deny it on BasicReceptor implementation.<br>
 * <br>
 * This Delivery is one-to-one connection, thus server need multiple deliveries.<br>
 * <br>
 * THIS IS NOT THREAD SAFE UNLESS YOU PUT SYNC SCHEDULER FOR DELIVERY!<br>
 * QueueScheduler, which is default, is recommended for scheduler delivery.
 * <br>
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
	
	private final static TickingScheduler.Wrapper TICK_SCHEDULER = new TickingScheduler.Wrapper(new TickingScheduler());
	
	private final static byte[] BAD_PACKET = "BAD_PACKET".getBytes();
	private final static byte[] TIMEOUT = "TIMEOUT".getBytes();
	
	private final static Scheduler DEFAULT_SCHEDULER = new QueueScheduler();
	
	// For asymCrypto
	private final byte[] publicKey;
	private final byte[] privateKey;
	
	private byte[] sessionKey; // For symCrypto.
	private long sessionId;
	
	private final Crypto asymCrypto;
	private final Crypto symCrypto;
	
	private final BasicMessenger basicMessenger;
	
	public final String basicReceptorChannelName;
	
	/*
	 * 	Requester							Acceptor
	 * 		|									|
	 * 		|			non-connected			| Stage 0
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
	 * 		|		  verify session key		|
	 * PCON	|<----------------------------------| Stage 4 PCON
	 * 		|									|
	 * 		|		connection established		|
	 * 		|---------------------------------->| Stage 5,6 RCON
	 * 		|									|
	 * 		|  connection confirm / send data	|
	 * RCON	|<----------------------------------| Stage 6
	 * 		|									|
	 */
	private int connectionStage;
	
	public final TimeoutProfile timeoutProfile = new TimeoutProfile();
	
	private final Map<String, SecureReceiver> receivers = new HashMap<>();
	
	private int preRequestReSends;
	
	/*
	 * You can implement a BukkitManagedScheduler :P while BukkitScheduler is already used.
	 */
	private final SchedulerWrapper scheduler; 
	
	public SecureDelivery(String channelName, BasicMessenger basicMessenger) {
		this(
				channelName, basicMessenger, 
				DEFAULT_PUBLIC_KEY, DEFAULT_PRIVATE_KEY, 
				DEFAULT_ASYM_CRYPTO, DEFAULT_SYM_CRYPTO
				);
	}
	
	public SecureDelivery(String channelName, BasicMessenger basicMessenger, Scheduler scheduler) {
		this(
				channelName, basicMessenger, 
				DEFAULT_PUBLIC_KEY, DEFAULT_PRIVATE_KEY, 
				DEFAULT_ASYM_CRYPTO, DEFAULT_SYM_CRYPTO, 
				scheduler);
	}
	
	public SecureDelivery(
			String channelName, BasicMessenger basicMessenger, 
			byte[] publicKey, byte[] privateKey, 
			Crypto asymCrypto, Crypto symCrypto) {
		this(
				channelName, basicMessenger, 
				publicKey, privateKey, asymCrypto, 
				symCrypto, DEFAULT_SCHEDULER
				);
	}
	
	public SecureDelivery(
			String channelName, BasicMessenger basicMessenger, 
			byte[] publicKey, byte[] privateKey, 
			Crypto asymCrypto, Crypto symCrypto, Scheduler scheduler) {
		basicReceptorChannelName = channelName;
		this.basicMessenger = basicMessenger;
		basicMessenger.registerReceptor(channelName, new LowLevelReceptor());
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.asymCrypto = asymCrypto;
		this.symCrypto = symCrypto;
		this.scheduler = new SchedulerWrapper(scheduler);
	}
	
	
	
	
	public void send(long tag, byte[] data) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkConnection();
				// TODO
			}
			
		});
	}
	
	
	//Simplification
	public void connect() {
		connect(EMPTY_BYTE_ARRAY);
	}
	
	public void connect(byte[] datagram) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkStage(Stages.NOT_CONNECTED);
				// TODO
			}
			
		});
	}
	
	//Simplification
	public void connect(String message) {
		connect(message.getBytes());
	}
	
	
	public void respondConnect(boolean confirmation) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkStage(Stages.CONNECT_REQUEST_SENT);
				// TODO
			}
			
		});
	}
	
	
	public void connectStandBy(){
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkStage(Stages.CONNECT_REQUEST_SENT);
				// TODO
			}
			
		});
	}
	
	
	public void respondPublicKey(boolean confirmation) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkStage(Stages.PUBLIC_KEY_OFFERED);
				// TODO
			}
			
		});
	}
	
	
	public void publicKeyStandBy(){
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkStage(Stages.PUBLIC_KEY_OFFERED);
				// TODO
			}
			
		});
	}
	
	
	//Simplification
	public void disconnect() {
		disconnect(EMPTY_BYTE_ARRAY);
	}
	
	public void disconnect(byte[] datagram) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkConnection();
				// TODO
			}
			
		});
	}
	
	//Simplification
	public void disconnect(String message) {
		disconnect(message.getBytes());
	}
	
	
	/**
	 * Manually send keep alive packet.
	 */
	public void keepAlive() {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				checkConnection();
				// TODO
			}
			
		});
	}
	
	
	private void checkStage(int stage) {
		if(connectionStage != stage) throw new InconsistentStageException();
	}
	
	private void checkConnection() {
		if(connectionStage < Stages.CONNECTED) throw new NoConnectionException();
	}
	
	
	public void registerReceiver(String channelName, SecureReceiver receiver) {
		synchronized (receivers) {
			receivers.put(channelName, receiver);
		}
	}
	
	public void unregisterReceiver(String channelName) {
		synchronized (receivers) {
			receivers.remove(channelName);
		}
	}
	
	
	private void broadcastReceive(int tag, byte[] data) {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				byte[] clonedData = cloneBytes(data);
				try {
					entry.getValue().receive(tag, clonedData);
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"receive(int, byte[])\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void broadcastPostConfirm(int tag) {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				try {
					entry.getValue().postConfirm(tag);
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"postConfirm(int)\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void broadcastPostBroken(int tag) {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				try {
					entry.getValue().postBroken(tag);
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"postBroken(int)\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void broadcastOnConnect(byte[] datagram) {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				byte[] clonedDatagram = cloneBytes(datagram);
				try {
					entry.getValue().onConnect(clonedDatagram);
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"onConnect(byte[])\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void broadcastOnPublicKeyRespond(byte[] publicKey) {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				byte[] clonedPublicKey = cloneBytes(publicKey);
				try {
					entry.getValue().onPublicKeyRespond(clonedPublicKey);
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"onPublicKeyRespond(byte[])\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void broadcastOnConnectionEstablish() {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				try {
					entry.getValue().onConnectionEstablish();
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"onConnectionEstablish()\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void broadcastOnDisconnect(byte[] datagram) {
		synchronized (receivers) {
			for(Entry<String, SecureReceiver> entry : receivers.entrySet()) {
				byte[] clonedDatagram = cloneBytes(datagram);
				try {
					entry.getValue().onDisconnect(clonedDatagram);
				} catch (RuntimeException ex) {
					System.err.println(
							"Unhandled exception occured on receiving method \"onDisconnect(byte[])\" of receiver \""+ 
									entry.getKey() +"\"");
					ex.printStackTrace();
				}
			}
		}
	}
	
	private static byte[] cloneBytes(byte[] data) {
		if(data == null) return null;
		byte[] bytes = new byte[data.length];
		System.arraycopy(data, 0, bytes, 0, data.length);
		return bytes;
	}
	
	private void resolve(Packet packet) {
		// TODO
		// TODO
		// TODO!!!!
	}
	
	private void sendDisconnect(byte[] datagram) {
		byte[] letter = LetterWrapper.wrap(datagram);
		Packet packet = new Packet(sessionId, Operations.DISCONNECT, 0, letter);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendConnect(byte[] datagram) {
		byte[] letter = LetterWrapper.wrap(datagram);
		Packet packet = new Packet(sessionId, Operations.CONNECT, 0, letter);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendConnectStandBy() {
		Packet packet = new Packet(sessionId, Operations.CONNECT_STANDBY, 0, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendPublicKeyOffer(byte[] publicKey) {
		byte[] letter = LetterWrapper.wrap(publicKey);
		Packet packet = new Packet(sessionId, Operations.PUBLIC_KEY_OFFER, 0, letter);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendPublicKeyStandBy() {
		Packet packet = new Packet(sessionId, Operations.PUBLIC_KEY_STANDBY, 0, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendStartSession(byte[] encryptedSessionKey) {
		byte[] letter = LetterWrapper.wrap(encryptedSessionKey);
		Packet packet = new Packet(sessionId, Operations.START_SESSION, 0, letter);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendBrokenPreRequest(int stage) {
		Packet packet = new Packet(sessionId, Operations.BROKEN_PRE_REQUEST, stage, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendConfirmSession(byte[] sessionKey) throws InvalidKeyException, BadPaddingException {
		byte[] letter = LetterWrapper.wrapAndEncrypt(sessionKey, symCrypto, sessionKey);
		Packet packet = new Packet(sessionId, Operations.CONFIRM_SESSION, 0, letter);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendConnectionEstablish() {
		Packet packet = new Packet(sessionId, Operations.CONNECTION_ESTABLISH, 0, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendConectionConfirm() {
		Packet packet = new Packet(sessionId, Operations.CONNECTION_CONFIRM, 0, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendData(long tag, byte[] data) throws InvalidKeyException, BadPaddingException {
		byte[] letter = LetterWrapper.wrapAndEncrypt(data, symCrypto, sessionKey);
		Packet packet = new Packet(sessionId, Operations.SEND_DATA, tag, letter);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendDataConfirm(long tag) {
		Packet packet = new Packet(sessionId, Operations.CONFIRM_DATA, tag, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void sendDataBroken(long tag) {
		Packet packet = new Packet(sessionId, Operations.BROKEN_DATA, tag, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void windUp(byte[] datagram) {
		sendDisconnect(datagram);
		connectionStage = Stages.NOT_CONNECTED;
		broadcastOnDisconnect(datagram);
	}
	
	private final class LowLevelReceptor implements BasicReceptor {

		@Override
		public void receive(byte[] data) {
			scheduler.schedule(new Runnable() {
				
				@Override
				public void run() {
					try {
						resolve(Packet.resolve(data));
					} catch (DataBrokenException ex) {
						if(connectionStage < Stages.CONNECTED) {
							if(connectionStage == Stages.NOT_CONNECTED) {
								sendBrokenPreRequest(connectionStage);
							} else if (preRequestReSends++ < timeoutProfile.preRequestReSends.get()) {
								sendBrokenPreRequest(connectionStage);
							} else {
								windUp(BAD_PACKET);
							}
						}
					}
				}
				
			});
		}
		
	}
	
	
	/*
	 * Form
	 * | HEAD CRC | SESSION ID | OPERATION | TAG | LETTER |
	 *      8B          8B          1B        8B  length-25B
	 */
	private final static class Operations {
		public final static byte DISCONNECT = 0;
		public final static byte CONNECT = 1;
		public final static byte CONNECT_STANDBY = 2;
		public final static byte PUBLIC_KEY_OFFER = 3;
		public final static byte PUBLIC_KEY_STANDBY = 4;
		public final static byte START_SESSION = 5;
		public final static byte BROKEN_PRE_REQUEST = 6;
		public final static byte CONFIRM_SESSION = 7; //ENCRYPTED
		public final static byte CONNECTION_ESTABLISH = 8;
		public final static byte CONNECTION_CONFIRM = 9;
		public final static byte SEND_DATA = 10; // ENCRYPTED
		public final static byte CONFIRM_DATA = 11;
		public final static byte BROKEN_DATA = 12;
	}
	
	public final static class Stages {
		public final static int NOT_CONNECTED = 0;
		public final static int CONNECT_REQUEST_SENT = 1;
		public final static int PUBLIC_KEY_OFFERED = 2;
		public final static int SESSION_KEY_SENT = 3;
		public final static int SESSION_VERIFICATION_SENT = 4;
		public final static int CONNECTION_ESTABLISHING = 5;
		public final static int CONNECTED = 6;
	}
	
	
	public final static class LetterWrapper {
		
		private final static int CRC_START = 0;
		public final static int DATA_START = 8;
		
		public static byte[] wrap(byte[] data) {
			byte[] bytes = new byte[data.length + DATA_START];
			
			long dataSum = CRC64.checksum(data);
			ByteWrapper.toBytes(dataSum, bytes, CRC_START);
			System.arraycopy(data, 0, bytes, DATA_START, data.length);
			
			return bytes;
		}
		
		public static byte[] wrapAndEncrypt(byte[] data, Crypto crypto, byte[] key) 
				throws InvalidKeyException, BadPaddingException {
			return crypto.encrypt(wrap(data), key);
		}
		
		public static byte[] resolve(byte[] letter) throws DataBrokenException {
			if(letter.length < DATA_START) {
				throw new DataBrokenException();
			}
			
			long headSum = ByteWrapper.fromBytes(letter, CRC_START, OutputType.LONG);
			if(!CRC64.isDataComplete(headSum, letter, DATA_START, letter.length)) {
				throw new DataBrokenException();
			}
			
			byte[] bytes = new byte[letter.length - DATA_START];
			System.arraycopy(letter, DATA_START, bytes, 0, bytes.length);
			
			return bytes;
		}
		
		public static byte[] decryptAndResolve(byte[] letter, Crypto crypto, byte[] key) 
				throws InvalidKeyException, BadPaddingException, DataBrokenException {
			return resolve(crypto.decrypt(letter, key));
		}
		
		private LetterWrapper() {}
	}
	
	
	public final static class Packet {
		
		public final static int HEAD_CRC_START = 0;
		public final static int SESSION_ID_START = 8;
		public final static int OPERATION_START = 16;
		public final static int TAG_START = 17;
		public final static int LETTER_START = 25;
		
		public final static int HEAD_START = 8;
		public final static int HEAD_LENGTH = 17;
		
		public final Head head;
		public final byte[] letter;
		
		public Packet(Head head, byte[] letter) {
			this.head = head;
			this.letter = letter;
		}
		
		public Packet(long sessionId, byte operation, long tag, byte[] letter) {
			this(new Head(sessionId, operation, tag), letter);
		}
		
		public byte[] wrap() {
			byte[] bytes = new byte[letter.length + LETTER_START];
			
			ByteWrapper.toBytes(head.sessionId, bytes, SESSION_ID_START);
			ByteWrapper.toBytes(head.operation, bytes, OPERATION_START);
			ByteWrapper.toBytes(head.tag, bytes, TAG_START);
			
			long headSum = CRC64.checksum(bytes, HEAD_START, LETTER_START);
			ByteWrapper.toBytes(headSum, bytes, HEAD_CRC_START);
			
			System.arraycopy(letter, 0, bytes, LETTER_START, letter.length);
			return bytes;
		}
		
		public static Packet resolve(byte[] bytes) throws DataBrokenException {
			if(bytes.length < LETTER_START) {
				throw new DataBrokenException();
			}
			
			long headSum = ByteWrapper.fromBytes(bytes, HEAD_CRC_START, OutputType.LONG);
			if(!CRC64.isDataComplete(headSum, bytes, HEAD_START, LETTER_START)) {
				throw new DataBrokenException();
			}
			
			Head head = new Head(
					ByteWrapper.fromBytes(bytes, SESSION_ID_START, OutputType.LONG), 
					ByteWrapper.fromBytes(bytes, OPERATION_START, OutputType.BYTE),
					ByteWrapper.fromBytes(bytes, TAG_START, OutputType.LONG)
					);
			
			byte[] letter = new byte[bytes.length - LETTER_START];
			System.arraycopy(bytes, LETTER_START, letter, 0, letter.length);
			
			return new Packet(head, letter);
		}
		
		public final static class Head {
			public final long sessionId;
			public final byte operation;
			public final long tag;
			
			public Head(long sessionId, byte operation, long tag) {
				this.sessionId = sessionId;
				this.operation = operation;
				this.tag = tag;
			}
		}
		
	}
	
	
	public final static class DataBrokenException extends Exception {
		
		private static final long serialVersionUID = 5943658912911088754L;
		
	}
	
	
	public final static class TimeoutProfile {
		
		public final SingleProfile<Integer> connectRequestTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> connectRequestReSends = 
				new SingleProfile<Integer>(new ReSendsConstrain(3), 3);
		
		public final SingleProfile<Integer> publicKeyOfferWaitTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(600000), 600000);
		
		public final SingleProfile<Integer> publicKeyOfferTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> publicKeyOfferReSends = 
				new SingleProfile<Integer>(new ReSendsConstrain(10), 10);
		
		public final SingleProfile<Integer> startSessionWaitTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(600000), 600000);
		
		public final SingleProfile<Integer> startSessionTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> startSessionReSends = 
				new SingleProfile<Integer>(new ReSendsConstrain(15), 15);
		
		public final SingleProfile<Integer> connectionEstablishTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> connectionEstablishReSends = 
				new SingleProfile<Integer>(new ReSendsConstrain(5), 5);
		
		public final SingleProfile<Integer> connectionTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(20000), 20000);
		public final SingleProfile<Integer> keepAliveDelay = 
				new SingleProfile<Integer>(new TimeoutConstrain(5000), 5000);
		
		public final SingleProfile<Integer> preRequestReSends = 
				new SingleProfile<Integer>(new ReSendsConstrain(20), 20);
		
		private final static class TimeoutConstrain implements Constrain<Integer> {
			
			public final int defaultTimeout;
			public final static int MIN_TIMEOUT = 1000;
			
			public TimeoutConstrain(int defaultTimeout) {
				this.defaultTimeout = defaultTimeout;
			}

			@Override
			public Integer apply(Integer value) {
				if(value == null) value = defaultTimeout;
				if(value < MIN_TIMEOUT) value = MIN_TIMEOUT;
				return value;
			}
			
		}
		
		private final static class ReSendsConstrain implements Constrain<Integer> {
			
			public final int defaultReSends;
			
			public ReSendsConstrain(int defaultReSends) {
				this.defaultReSends = defaultReSends;
			}

			@Override
			public Integer apply(Integer value) {
				if(value == null) value = defaultReSends;
				if(value < 0) value = -1; // NOT RECOMMENDED: RESEND FOREVER 
				return value;
			}
			
		}
		
		public final static class SingleProfile<T> {
			private T value;
			private final Constrain<T> constrain;
			
			public SingleProfile(Constrain<T> constrain, T value) {
				this.constrain = constrain;
				set(value);
			}
			
			public void set(T value) {
				this.value = constrain.apply(value);
			}
			
			public T get() {
				return value;
			}
		}
		
		private static interface Constrain<T> {
			public T apply(T value);
		}
		
	}
	
	public final static class NoConnectionException extends RuntimeException {
		
		private static final long serialVersionUID = 1499031180680426223L;

	}
	
	public final static class InconsistentStageException extends RuntimeException {

		private static final long serialVersionUID = 896642529227957971L;
		
	}
	
	
	/*public static void main(String[] args) throws InvalidKeyException, BadPaddingException {
		
		byte[] key = AESCrypto.genKeyPair().getEncoded();
		
		Packet packet = new Packet(
				1486712, (byte) 2, 87497233, 
				LetterWrapper.wrapAndEncrypt(
						"We have implicit trust in him.".getBytes(), 
						DEFAULT_SYM_CRYPTO, key)
				);
		
		byte[] bytes = packet.wrap();
		
		//bytes[10] = 22;
		
		try {
			Packet received = Packet.resolve(bytes);
			System.out.println(
					new String(
							LetterWrapper.decryptAndResolve(received.letter, DEFAULT_SYM_CRYPTO, key)
							)
					);
			System.out.println("Session ID: " + received.head.sessionId);
			System.out.println("Operation: " + received.head.operation);
			System.out.println("Tag: " + received.head.tag);
		} catch (DataBrokenException e) {
			System.out.println("Head broken!");
		}
		
	}*/ // Test Code for Packet

}
