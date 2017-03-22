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
	
	/*
	 * Form
	 * | HEAD CRC | SESSION ID | OPERATION | DATA CRC | DATAGRAM |
	 *      8B          8B          1B          8B     length-25B
	 */
	@SuppressWarnings("unused")
	private final static class ExplicitOperations {
		public final static byte DISCONNECT = 0; //Disconnection can be both implicit and explicit.
		public final static byte CONNECT = 1;
		public final static byte PUBLIC_KEY_OFFER = 2;
		public final static byte START_SESSION = 3;
	}
	
	@SuppressWarnings("unused")
	private final static class ImplicitOperations {
		public final static byte DISCONNECT = 0; //However, disconnect(byte[])'s extra data is encrypted.
		public final static byte CONFIRM_SESSION = 4;
		public final static byte CONNECTION_ESTABLISH = 5;
		public final static byte SEND_DATA = 6;
		public final static byte CONFIRM_DATA = 7;
		public final static byte BROKEN_DATA = 8;
	}
	
	public final static class Packet {
		public final static int HEAD_WITH_CRC_LENGTH = 25; //bytes
		public final static int HEAD_LENGTH = 9; //Session ID and Operation
		public final static int HEADCRC_START = 0;
		public final static int SESSIONID_START = 8;
		public final static int OPERATION_START = 16;
		public final static int DATA_CRC_START = 17;
		
		public final Head head;
		public final byte[] datagram;
		
		public Packet(long sessionId, byte operation, byte[] datagram) {
			this(new Head(sessionId, operation), datagram);
		}
		
		public Packet(Head head, byte[] datagram) {
			this.head = head;
			this.datagram = datagram;
		}
		
		public byte[] wrapInExplicit(){
			byte[] bytes = new byte[HEAD_WITH_CRC_LENGTH + datagram.length];
			ByteWrapper.toBytes(head.sessionId, bytes, SESSIONID_START);
			ByteWrapper.toBytes(head.operation, bytes, OPERATION_START);
			long headCRC = CRC64.checksum(bytes, SESSIONID_START, SESSIONID_START + HEAD_LENGTH);
			ByteWrapper.toBytes(headCRC, bytes, HEADCRC_START);
			
			long dataCRC = CRC64.checksum(datagram);
			System.arraycopy(datagram, 0, bytes, HEAD_WITH_CRC_LENGTH, datagram.length);
			ByteWrapper.toBytes(dataCRC, bytes, DATA_CRC_START);
			return bytes;
		}
		
		public byte[] wrapInImplicit(Crypto crypto, byte[] key) throws InvalidKeyException, BadPaddingException {
			return crypto.encrypt(wrapInExplicit(), key);
		}
		
		public static Packet resolveByImplicit(byte[] data, Crypto crypto, byte[] key) 
				throws DataBrokenException, InvalidKeyException, BadPaddingException {
			return resolveByExplicit(crypto.decrypt(data, key));
		}
		
		public static Packet resolveByExplicit(byte[] data) throws DataBrokenException {
			if(data.length < HEAD_WITH_CRC_LENGTH) throw new DataBrokenException();
			long headCRC = ByteWrapper.fromBytes(data, HEADCRC_START, OutputType.LONG);
			if(!isDataComplete(headCRC, data, SESSIONID_START, HEAD_LENGTH)) {
				throw new DataBrokenException();
			}
			
			Head head = new Head(
					ByteWrapper.fromBytes(data, SESSIONID_START, OutputType.LONG), 
					ByteWrapper.fromBytes(data, OPERATION_START, OutputType.BYTE)
					);
			
			long dataCRC = ByteWrapper.fromBytes(data, DATA_CRC_START, OutputType.LONG);
			if(!isDataComplete(dataCRC, data, HEAD_WITH_CRC_LENGTH, data.length - HEAD_WITH_CRC_LENGTH)) {
				throw new DataBrokenException(head);
			}
			
			byte[] datagram = new byte[data.length - HEAD_WITH_CRC_LENGTH];
			System.arraycopy(data, HEAD_WITH_CRC_LENGTH, datagram, 0, datagram.length);
			
			return new Packet(head, datagram);
		}
		
		private static boolean isDataComplete(long crc, byte[] data, int start, int length) {
			long realCRC = CRC64.checksum(data, start, start + length);
			return crc == realCRC;
		}
		
		public final static class DataBrokenException extends Exception {
			
			private static final long serialVersionUID = 1186487923670618064L;
			
			public final Head head;
			
			public DataBrokenException() {
				this(null);
			}
			
			public DataBrokenException(Head head) {
				this.head = head;
			}
			
		}
		
		public final static class Head {
			public final long sessionId;
			public final byte operation;
			
			public Head(long sessionId, byte operation) {
				this.sessionId = sessionId;
				this.operation = operation;
			}
		}
		
	}
	
	/*public static void main(String[] args) {
		
		Packet packet = new Packet(
				1486712, (byte) 2, 
				"We have implicit trust in him.".getBytes()
				);
		
		byte[] bytes;
		try {
			bytes = packet.wrapInImplicit(DEFAULT_ASYM_CRYPTO, DEFAULT_PUBLIC_KEY);
		} catch (InvalidKeyException e1) {
			System.out.println("Invalid Public Key!");
			return;
		} catch (BadPaddingException e1) {
			System.out.println("Invalid Padding on Encryption!");
			return;
		}
		
		try {
			Packet received = Packet.resolveByImplicit(bytes, DEFAULT_ASYM_CRYPTO, DEFAULT_PRIVATE_KEY);
			System.out.println(new String(received.datagram));
		} catch (DataBrokenException e) {
			if(e.head != null) {
				System.out.println("Data broken!");
				System.out.println("Session ID: " + e.head.sessionId);
				System.out.println("Operation: " + e.head.operation);
			} else {
				System.out.println("Head broken!");
			}
		} catch (InvalidKeyException e) {
			System.out.println("Invalid Private Key!");
		} catch (BadPaddingException e) {
			System.out.println("Invalid Padding on Decryption!");
		}
		
	}*/ // Test Code for Packet

}
