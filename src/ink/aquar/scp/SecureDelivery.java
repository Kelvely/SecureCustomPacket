package ink.aquar.scp;

import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.crypto.BadPaddingException;

import org.jacoco.core.internal.data.CRC64;

import ink.aquar.scp.crypto.AESCrypto;
import ink.aquar.scp.crypto.AsymmetricCrypto;
import ink.aquar.scp.crypto.Crypto;
import ink.aquar.scp.crypto.RSACrypto;
import ink.aquar.scp.crypto.SymmetricCrypto;
import ink.aquar.scp.crypto.AsymmetricCrypto.ByteKeyPair;
import ink.aquar.scp.util.ByteWrapper;
import ink.aquar.scp.util.ByteWrapper.OutputType;
import ink.aquar.scp.util.QueueScheduler;
import ink.aquar.scp.util.Scheduler;
import ink.aquar.scp.util.SchedulerWrapper;
import ink.aquar.scp.util.TickingScheduler;

/**
 * SecureDeliver provides secure connection between two terminals. It provides data validity verification, 
 * applicable for most of the stream communicator, which need to be connected in a BasicMessenger. It will 
 * work pretty well on UDP communications(P.S. If you can use TCP, then you should use SSH instead of this), 
 * and firstly, this class is put in use in secure communication of Minecraft CustomPacket.<br>
 * <br>
 * Packet timeout or damage handle policies should be considered by library user when implementing 
 * SecureReceiver, because SecureDelivery provides only data verification, but not damaged data handle.<br>
 * <br>
 * <h1>If you want to deny some requester's connection, please deny it on BasicReceptor implementation.</h1><br>
 * <br>
 * This Delivery is one-to-one connection, thus server need multiple deliveries.<br>
 * <br>
 * <h1>You have to implement SecureReceiver to receive packets.</h1><br>
 * <br>
 * Since scheduler are involved, you can choose whether use a customized scheduler or use a default 
 * scheduler to sync packets. Feel free on async packets submitted to SecureDelivery!<br>
 * In addition, receivers receive from scheduler's thread.<br>
 * <br>
 * 
 * @see SecureReceiver
 * @see BasicMessenger
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 */
public class SecureDelivery {
	// Fields that labeled 'Complete' means here should have no more development related to this field.
	
	private final static byte[] EMPTY_BYTE_ARRAY = {};
	
	private final static AsymmetricCrypto DEFAULT_ASYM_CRYPTO = new RSACrypto(); // Complete.
	private final static SymmetricCrypto DEFAULT_SYM_CRYPTO = new AESCrypto(); // Complete.
	
	private final static byte[] DEFAULT_PUBLIC_KEY; // Complete.
	private final static byte[] DEFAULT_PRIVATE_KEY; // Complete.
	static {
		ByteKeyPair keyPair = new RSACrypto().generateKeyPair();
		DEFAULT_PRIVATE_KEY = keyPair.privateKey;
		DEFAULT_PUBLIC_KEY = keyPair.publicKey;
	}
	
	private final static Random RANDOM = new Random();
	
	private final static TickingScheduler.Wrapper TICK_SCHEDULER = new TickingScheduler.Wrapper(new TickingScheduler());
	
	private final static byte[] BAD_PACKET = "BAD_PACKET".getBytes();
	private final static byte[] BAD_PUBLIC_KEY = "BAD_PUBLIC_KEY".getBytes();
	private final static byte[] BAD_SESSION_KEY = "BAD_SESSION_KEY".getBytes();
	private final static byte[] TIMEOUT = "TIMEOUT".getBytes();
	private final static byte[] INVALID_SESSION_KEY = "INVALID_SESSION_KEY".getBytes();
	private final static byte[] CONNECT_REJECT = "CONNECT_REJECT".getBytes();
	
	private final static Scheduler DEFAULT_SCHEDULER = new QueueScheduler(); // Complete.
	
	// For asymCrypto
	private final byte[] publicKey;
	private final byte[] privateKey;
	
	private byte[] sessionKey; // For symCrypto.
	private long sessionId;
	
	private final AsymmetricCrypto asymCrypto;
	private final SymmetricCrypto symCrypto;
	
	private final BasicMessenger basicMessenger; // Complete.
	
	public final String basicReceptorChannelName; // Complete.
	
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
	 * 
	 * TODO
	 */
	private int connectionStage;
	
	private TimeoutTask nextTimeout;
	private TimeoutTask standByTimeout;
	
	public final TimeoutProfile timeoutProfile = new TimeoutProfile(); // Complete.
	
	private final Map<String, SecureReceiver> receivers = new HashMap<>(); // Complete.
	
	private int preRequestResends;
	
	private byte[] encryptedSessionKey;
	
	private boolean isAcceptor; // Which means is bottom lol :D
	// TODO On sending, and accepting. // Sending is done :)
	
	/*
	 * You can implement a BukkitManagedScheduler :P while BukkitScheduler is already exists.
	 */
	private final SchedulerWrapper scheduler; 
	
	/**
	 * Construct a SecureDelivery by default public key and private key, which are auto generated, 
	 * RSA as asymmetric crypto, and AES 128 as symmetric crypto, 
	 * and SecureDelivery public schedule bus as scheduler to sync packets.<br>
	 * @param channelName The channel name of the SecureDelivery's registry on basicMessenger.
	 * @param basicMessenger The messenger that provide simple IO for delivery.
	 */
	public SecureDelivery(String channelName, BasicMessenger basicMessenger) {
		this(
				channelName, basicMessenger, 
				DEFAULT_PUBLIC_KEY, DEFAULT_PRIVATE_KEY, 
				DEFAULT_ASYM_CRYPTO, DEFAULT_SYM_CRYPTO
				);
	}
	
	/**
	 * 
	 * @param channelName The channel name of the SecureDelivery's registry on basicMessenger.
	 * @param basicMessenger The messenger that provide simple IO for delivery.
	 * @param scheduler The scheduler to sync packets.
	 */
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
			AsymmetricCrypto asymCrypto, SymmetricCrypto symCrypto) {
		this(
				channelName, basicMessenger, 
				publicKey, privateKey, asymCrypto, 
				symCrypto, DEFAULT_SCHEDULER
				);
	}
	
	public SecureDelivery(
			String channelName, BasicMessenger basicMessenger, 
			byte[] publicKey, byte[] privateKey, 
			AsymmetricCrypto asymCrypto, SymmetricCrypto symCrypto, Scheduler scheduler) {
		basicReceptorChannelName = channelName;
		this.basicMessenger = basicMessenger;
		basicMessenger.registerReceptor(channelName, new LowLevelReceptor());
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.asymCrypto = asymCrypto;
		this.symCrypto = symCrypto;
		this.scheduler = new SchedulerWrapper(scheduler);
	}
	
	//////////////////////////////////////////////// Any side
	
	public void send(long tag, byte[] data) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(!isStageConsistent(Stages.CONNECTED)) return;
				try {
					sendData(tag, data);
				} catch (InvalidKeyException ex) {
					windUp(INVALID_SESSION_KEY);
				} catch (BadPaddingException ex) {
					ex.printStackTrace();
				}
			}
			
		});
	}
	
	//////////////////////////////////////////////// Requester side
	
	//Simplification
	public void connect() {
		connect(EMPTY_BYTE_ARRAY);
	}
	
	public void connect(byte[] datagram) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(!isStageConsistent(Stages.NOT_CONNECTED)) return;
				
				setStage(Stages.CONNECT_REQUEST_SENT);
				isAcceptor = false;
				changeSessionId();
				
				sendConnect(datagram);
				
				TimeoutTask standByTask = new WindUpTask();
				TICK_SCHEDULER.schedule(standByTask, timeoutProfile.publicKeyOfferWaitTimeout.get());
				standByTimeout = standByTask;
				
				TimeoutTask timeoutTask = new ConnectTimeoutTask(0, datagram);
				TICK_SCHEDULER.schedule(timeoutTask, timeoutProfile.connectRequestTimeout.get());
				nextTimeout = timeoutTask;
			}
			
		});
	}
	
	private final class ConnectTimeoutTask extends TimeoutTask {
		
		private final byte[] datagram;
		
		public ConnectTimeoutTask(int timeoutLeft, byte[] datagram) {
			super(timeoutLeft);
			this.datagram = datagram;
		}

		@Override
		public void run() {
			scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					if(isCancelled) return;
					if(timeoutLeft < timeoutProfile.connectRequestResends.get()) {
						sendConnect(datagram);
						TimeoutTask timeoutTask = new ConnectTimeoutTask(timeoutLeft + 1, datagram);
						TICK_SCHEDULER.schedule(timeoutTask, timeoutProfile.connectRequestTimeout.get());
						nextTimeout = timeoutTask;
					} else {
						windUp(TIMEOUT);
					}
				}
			});
		}
		
	}
	
	//Simplification
	public void connect(String message) {
		connect(message.getBytes());
	}
	
	//////////////////////////////////////////////// Acceptor side
	
	public void respondConnect(boolean confirmation) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(!isAcceptor) return;
				if(!isStageConsistent(Stages.CONNECT_REQUEST_SENT)) return;
				if(confirmation) {
					setStage(Stages.PUBLIC_KEY_OFFERED);
					
					sendPublicKeyOffer(publicKey);
					
					TimeoutTask standByTask = new WindUpTask();
					TICK_SCHEDULER.schedule(standByTask, timeoutProfile.startSessionWaitTimeout.get());
					standByTimeout = standByTask;
					
					TimeoutTask timeoutTask = new PublicKeyTimeoutTask(0);
					TICK_SCHEDULER.schedule(timeoutTask, timeoutProfile.publicKeyOfferTimeout.get());
					nextTimeout = timeoutTask;
				} else {
					windUp(CONNECT_REJECT);
				}
			}
			
		});
	}
	
	private final class PublicKeyTimeoutTask extends TimeoutTask {

		public PublicKeyTimeoutTask(int timeoutLeft) {
			super(timeoutLeft);
		}

		@Override
		public void run() {
			scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					if(isCancelled) return;
					if(timeoutLeft < timeoutProfile.publicKeyOfferResends.get()) {
						sendPublicKeyOffer(publicKey);
						TimeoutTask timeoutTask = new PublicKeyTimeoutTask(timeoutLeft + 1);
						TICK_SCHEDULER.schedule(timeoutTask, timeoutProfile.publicKeyOfferTimeout.get());
						nextTimeout = timeoutTask;
					} else {
						windUp(TIMEOUT);
					}
				}
			});
		}
		
	}
	
	//////////////////////////////////////////////// Acceptor side
	
	public void connectStandBy(){
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(!isAcceptor) return;
				if(!isStageConsistent(Stages.CONNECT_REQUEST_SENT)) return;
				sendConnectStandBy();
			}
			
		});
	}
	
	//////////////////////////////////////////////// Requester side
	//
	public void respondPublicKey(boolean confirmation) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(isAcceptor) return;
				if(!isStageConsistent(Stages.PUBLIC_KEY_OFFERED)) return;
				if(confirmation) {
					setStage(Stages.SESSION_KEY_SENT);
					
					sendStartSession(encryptedSessionKey);
					
					TimeoutTask timeoutTask = new SessionStartTimeoutTask(0);
					TICK_SCHEDULER.schedule(timeoutTask, timeoutProfile.startSessionTimeout.get());
					nextTimeout = timeoutTask;
				} else {
					windUp(CONNECT_REJECT);
				}
			}
			
		});
	}
	
	private final class SessionStartTimeoutTask extends TimeoutTask {

		public SessionStartTimeoutTask(int timeoutLeft) {
			super(timeoutLeft);
		}

		@Override
		public void run() {
			scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					if(isCancelled) return;
					if(timeoutLeft < timeoutProfile.startSessionResends.get()) {
						sendStartSession(encryptedSessionKey);
						
						TimeoutTask timeoutTask = new SessionStartTimeoutTask(timeoutLeft + 1);
						TICK_SCHEDULER.schedule(timeoutTask, timeoutProfile.startSessionTimeout.get());
						nextTimeout = timeoutTask;
					} else {
						windUp(TIMEOUT);
					}
				}
			});
		}
		
	}
	
	//////////////////////////////////////////////// Requester side
	
	public void publicKeyStandBy() {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(isAcceptor) return;
				if(!isStageConsistent(Stages.PUBLIC_KEY_OFFERED)) return;
				sendPublicKeyStandBy();
			}
			
		});
	}
	
	//////////////////////////////////////////////// Any side
	
	//Simplification
	public void disconnect() {
		disconnect(EMPTY_BYTE_ARRAY);
	}
	
	public void disconnect(byte[] datagram) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(!isStageConsistent(Stages.CONNECTED)) return;
				windUp(datagram);
			}
			
		});
	}
	
	//Simplification
	public void disconnect(String message) {
		disconnect(message.getBytes());
	}
	
	//////////////////////////////////////////////// Any side
	
	/**
	 * Manually send keep alive packet.
	 */
	public void keepAlive() {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(!isStageConsistent(Stages.CONNECTED)) return;
				sendKeepAlive();
			}
			
		});
	}
	
	//////////////////////////////////////////////// <<<<<<<<<
	
	private final class WindUpTask extends TimeoutTask {

		public WindUpTask() {
			super(0);
		}
		
		@Override
		public void run() {
			scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					if(isCancelled) return;
					windUp(TIMEOUT);
				}
			});
		}
		
	}
	
	private boolean isStageConsistent(int stage) {
		return connectionStage == stage;
	}
	
	private void setStage(int stage) {
		connectionStage = stage;
	}
	
	private void changeSessionId() {
		for(;;) {
			long randomLong = RANDOM.nextLong();
			if(randomLong != sessionId) {
				sessionId = randomLong;
				break;
			}
		}
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
	
	private void handle(Packet packet) {
		
		if(!(packet.head.sessionId == sessionId || isStageConsistent(Stages.NOT_CONNECTED))) return;
		
		switch (packet.head.operation) {
		
		case Operations.SEND_DATA:
			handleSendData(packet);
			break;
			
		case Operations.CONFIRM_DATA:
			handleConfirmData(packet);
			break;
			
		case Operations.BROKEN_DATA:
			handleBrokenData(packet);
			break;
			
		case Operations.KEEP_ALIVE:
			handleKeepAlive(packet);
			break;
		
		//These 4 packets are frequently sent, put them here as optimization :P
			
		case Operations.DISCONNECT:
			handleDisconnect(packet);
			break;

		case Operations.CONNECT:
			handleConnect(packet);
			break;
			
		case Operations.CONNECT_STANDBY:
			handleConnectStandBy(packet);
			break;
			
		case Operations.PUBLIC_KEY_OFFER:
			handlePublicKeyOffer(packet);
			break;
			
		case Operations.PUBLIC_KEY_STANDBY:
			handlePublicKeyStandBy(packet);
			break;
			
		case Operations.START_SESSION:
			handleStartSession(packet);
			break;
			
		case Operations.BROKEN_PRE_REQUEST:
			handleBrokenPreRequest(packet);
			break;
			
		case Operations.CONFIRM_SESSION:
			handleConfirmSession(packet);
			break;
			
		case Operations.CONNECTION_ESTABLISH:
			handleConnectionEstablish(packet);
			break;
			
		case Operations.CONNECTION_CONFIRM:
			handleConnectionConfirm(packet);
			break;
			
		}
		
	}
	
	// Any side
	private void handleDisconnect(Packet packet) {
		if(isStageConsistent(Stages.NOT_CONNECTED)) return;
		byte[] datagram;
		try {
			datagram = LetterWrapper.resolve(packet.letter);
		} catch (DataBrokenException ex) {
			datagram = null;
		}
		windUpLocal(datagram);
	}
	
	// Acceptor side
	private void handleConnect(Packet packet) {
		if((!isStageConsistent(Stages.NOT_CONNECTED))) return;
		
		byte[] datagram;
		try {
			datagram = LetterWrapper.resolve(packet.letter);
		} catch (DataBrokenException ex) {
			sendBrokenPreRequest(connectionStage);
			return;
		}
		
		isAcceptor = true;
		sessionId = packet.head.sessionId;
		setStage(Stages.CONNECT_REQUEST_SENT);
		
		broadcastOnConnect(datagram);
	}
	
	// Requester side
	private void handleConnectStandBy(Packet packet) {
		if((!isStageConsistent(Stages.CONNECT_REQUEST_SENT)) || (packet.head.sessionId != sessionId)) return;
		// TODO <<<<<<<<<<<<<<<<<<<<<<< HERE :D!
	}
	
	// Requester side
	private void handlePublicKeyOffer(Packet packet) {
		if((!isStageConsistent(Stages.CONNECT_REQUEST_SENT)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Acceptor side
	private void handlePublicKeyStandBy(Packet packet) {
		if((!isStageConsistent(Stages.PUBLIC_KEY_OFFERED)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Acceptor side
	private void handleStartSession(Packet packet) {
		if((!isStageConsistent(Stages.PUBLIC_KEY_OFFERED)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Any side
	private void handleBrokenPreRequest(Packet packet) {
		if(packet.head.tag == Stages.NOT_CONNECTED && isStageConsistent(Stages.CONNECT_REQUEST_SENT)) {
			// TODO Re-send the connect request.
		} else if(packet.head.sessionId != sessionId) {
			return;
		} else if(isStageConsistent((int) packet.head.tag + 1)){
			// TODO Re-send the request.
		}
	}
	
	// Requester side
	private void handleConfirmSession(Packet packet) {
		if((!isStageConsistent(Stages.SESSION_KEY_SENT)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Acceptor side
	private void handleConnectionEstablish(Packet packet) {
		if((!isStageConsistent(Stages.SESSION_VERIFICATION_SENT)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Requester side
	private void handleConnectionConfirm(Packet packet) {
		if((!isStageConsistent(Stages.CONNECTION_ESTABLISHING)) || (packet.head.sessionId != sessionId)) return;
		// TODO Establish the connection
	}
	
	// Any side, special for requester side as connection confirm
	private void handleSendData(Packet packet) {
		if(packet.head.sessionId != sessionId) return;
		if(isStageConsistent(Stages.CONNECTION_ESTABLISHING)) {
			// TODO Establish the connection
		} else if (!isStageConsistent(Stages.CONNECTED)) return;
		// TODO
	}
	
	// Any side
	private void handleConfirmData(Packet packet) {
		if((!isStageConsistent(Stages.CONNECTED)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Any side
	private void handleBrokenData(Packet packet) {
		if((!isStageConsistent(Stages.CONNECTED)) || (packet.head.sessionId != sessionId)) return;
		// TODO
	}
	
	// Any side
	private void handleKeepAlive(Packet packet) {
		if((!isStageConsistent(Stages.CONNECTED)) || (packet.head.sessionId != sessionId)) return;
		// TODO
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
	
	private void sendKeepAlive() {
		Packet packet = new Packet(sessionId, Operations.KEEP_ALIVE, 0, EMPTY_BYTE_ARRAY);
		basicMessenger.send(packet.wrap());
	}
	
	private void windUp(byte[] datagram) {
		sendDisconnect(datagram);
		windUpLocal(datagram);
	}
	
	private void windUpLocal(byte[] datagram) {
		setStage(Stages.NOT_CONNECTED);
		changeSessionId();
		if(standByTimeout != null) {
			standByTimeout.cancel();
			standByTimeout = null;
		}
		if(nextTimeout != null) {
			standByTimeout.cancel();
			standByTimeout = null;
		}
		broadcastOnDisconnect(datagram);
	}
	
	private final class LowLevelReceptor implements BasicReceptor {

		@Override
		public void receive(byte[] data) {
			scheduler.schedule(new Runnable() {
				
				@Override
				public void run() {
					
					try {
						Packet packet = Packet.resolve(data);
						preRequestResends = 0;
						handle(packet);
					} catch (DataBrokenException ex) {
						if(!isStageConsistent(Stages.CONNECTED)) {
							if(connectionStage == Stages.NOT_CONNECTED) {
								sendBrokenPreRequest(connectionStage);
							} else if (preRequestResends++ < timeoutProfile.preRequestResends.get()) {
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
		public final static byte KEEP_ALIVE = 13;
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
		public final SingleProfile<Integer> connectRequestResends = 
				new SingleProfile<Integer>(new ResendsConstrain(3), 3);
		
		public final SingleProfile<Integer> publicKeyOfferWaitTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(600000), 600000);
		
		public final SingleProfile<Integer> publicKeyOfferTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> publicKeyOfferResends = 
				new SingleProfile<Integer>(new ResendsConstrain(10), 10);
		
		public final SingleProfile<Integer> startSessionWaitTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(600000), 600000);
		
		public final SingleProfile<Integer> startSessionTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> startSessionResends = 
				new SingleProfile<Integer>(new ResendsConstrain(15), 15);
		
		public final SingleProfile<Integer> connectionEstablishTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(10000), 10000);
		public final SingleProfile<Integer> connectionEstablishResends = 
				new SingleProfile<Integer>(new ResendsConstrain(5), 5);
		
		public final SingleProfile<Integer> connectionTimeout = 
				new SingleProfile<Integer>(new TimeoutConstrain(20000), 20000);
		public final SingleProfile<Integer> keepAliveDelay = 
				new SingleProfile<Integer>(new TimeoutConstrain(5000), 5000);
		
		public final SingleProfile<Integer> preRequestResends = 
				new SingleProfile<Integer>(new ResendsConstrain(20), 20);
		
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
		
		private final static class ResendsConstrain implements Constrain<Integer> {
			
			public final int defaultResends;
			
			public ResendsConstrain(int defaultResends) {
				this.defaultResends = defaultResends;
			}

			@Override
			public Integer apply(Integer value) {
				if(value == null) value = defaultResends;
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
	
	private static abstract class TimeoutTask implements Runnable {
		
		boolean isCancelled;
		
		public final int timeoutLeft;
		
		public TimeoutTask(int timeoutLeft) {
			this.timeoutLeft = timeoutLeft;
		}
		
		public void cancel() {
			isCancelled = true;
		}
		
	}

}
