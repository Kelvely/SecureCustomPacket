package ink.aquar.scp;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.BadPaddingException;

import org.jacoco.core.internal.data.CRC64;

import ink.aquar.scp.crypto.AESCrypto;
import ink.aquar.scp.crypto.Crypto;
import ink.aquar.scp.crypto.RSACrypto;
import ink.aquar.scp.util.ByteWrapper;
import ink.aquar.scp.util.ByteWrapper.OutputType;
import ink.aquar.scp.util.TickingScheduler;

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
	
	private final static TickingScheduler.Wrapper SCHEDULER = new TickingScheduler.Wrapper();
	
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
	
	public void send(long tag, byte[] data) {
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
	
	public void respondConnect(boolean confirmation) {
		// TODO
	}
	
	public void connectStandBy(){
		// TODO
	}
	
	public void respondPublicKey(boolean confirmation) {
		// TODO
	}
	
	public void publicKeyStandBy(){
		// TODO
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
	
	/*
	 * Form
	 * | HEAD CRC | SESSION ID | OPERATION | TAG | LETTER |
	 *      8B          8B          1B        8B  length-25B
	 */
	@SuppressWarnings("unused")
	private final static class Operations {
		public final static byte DISCONNECT = 0;
		public final static byte CONNECT = 1;
		public final static byte CONNECT_STANDBY = 2;
		public final static byte PUBLIC_KEY_OFFER = 3;
		public final static byte PUBLIC_KEY_STANDBY = 4;
		public final static byte START_SESSION = 5;
		
		public final static byte CONFIRM_SESSION = 6;
		public final static byte CONNECTION_ESTABLISH = 7;
		public final static byte SEND_DATA = 8;
		public final static byte CONFIRM_DATA = 9;
		public final static byte BROKEN_DATA = 10;
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
				new SingleProfile<Integer>(new ReSendsConstrain(10), 10);
		
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
