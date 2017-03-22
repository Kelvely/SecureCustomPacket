package ink.aquar.scp.util;

import java.nio.ByteBuffer;
import java.util.List;

public class ByteWrapper {
	
	public static byte[] toBytes(Object object) {
		if(object instanceof Boolean) {
			return new byte[] {
					(boolean) object ? (byte) 1 : (byte) 0
			};
		} else if (object instanceof Byte) {
			return new byte[] {
					(byte) object
			};
		} else if (object instanceof Short) {
			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.putShort((short) object);
			return buffer.array();
		} else if (object instanceof Integer) {
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.putInt((int) object);
			return buffer.array();
		} else if (object instanceof Long) {
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.putLong((long) object);
			return buffer.array();
		} else if (object instanceof Character) {
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.putInt((char) object);
			return buffer.array();
		} else if (object instanceof Float) {
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.putFloat((float) object);
			return buffer.array();
		} else if (object instanceof Double) {
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.putDouble((double) object);
			return buffer.array();
		} else if (object instanceof String) {
			byte[] charset = ((String) object).getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(4+charset.length);
			buffer.putInt(charset.length);
			buffer.put(charset);
			return buffer.array();
		} else {
			byte[] charset = object.toString().getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(4+charset.length);
			buffer.putInt(charset.length);
			buffer.put(charset);
			return buffer.array();
		}
	}
	
	public static <T> T fromBytes(byte[] bytes, int start, OutputType<T> outputType) {
		return outputType.fromBytes(bytes, start);
	}
	
	public static byte[] linkAll(List<byte[]> bytes) {
		int length = 0;
		for (byte[] section : bytes) {
			if(section != null) {
				length += section.length;
			}
		}
		byte[] unity = new byte[length];
		int pointer = 0;
		for (byte[] section : bytes) {
			if(section != null) {
				System.arraycopy(section, 0, unity, pointer, section.length);
				pointer += section.length;
			}
		}
		return unity;
	}
	
	public static interface OutputType<T> {
		
		public T fromBytes(byte[] bytes, int start);
		
		public final static OutputType<Boolean> BOOLEAN = new OutputType<Boolean>() {

			@Override
			public Boolean fromBytes(byte[] bytes, int start) {
				if (start < bytes.length && start >= 0) {
					return bytes[start] != 0 ? true : false;
				} else return false;
			}
			
		};
		
		public final static OutputType<Byte> BYTE = new OutputType<Byte>() {

			@Override
			public Byte fromBytes(byte[] bytes, int start) {
				if (start < bytes.length && start >= 0) {
					return bytes[start];
				} else return 0;
			}
			
		};
		
		public final static OutputType<Short> SHORT = new OutputType<Short>() {
			
			public final static int LENGTH = 2;

			@Override
			public Short fromBytes(byte[] bytes, int start) {
				ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + LENGTH - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				buffer.put(new byte[foreshift + lastshift]);
				for(int i=start+foreshift, d=start + LENGTH - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return buffer.getShort(0);
			}
			
		};
		
		public final static OutputType<Integer> INTEGER = new OutputType<Integer>() {
			
			public final static int LENGTH = 4;

			@Override
			public Integer fromBytes(byte[] bytes, int start) {
				ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + LENGTH - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				buffer.put(new byte[foreshift + lastshift]);
				for(int i=start+foreshift, d=start + LENGTH - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return buffer.getInt(0);
			}
			
		};
		
		public final static OutputType<Long> LONG = new OutputType<Long>() {
			
			public final static int LENGTH = 8;

			@Override
			public Long fromBytes(byte[] bytes, int start) {
				ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + LENGTH - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				buffer.put(new byte[foreshift + lastshift]);
				for(int i=start+foreshift, d=start + LENGTH - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return buffer.getLong(0);
			}
			
		};
		
		public final static OutputType<Character> CHARACTER = new OutputType<Character>() {
			
			public final static int LENGTH = 4;

			@Override
			public Character fromBytes(byte[] bytes, int start) {
				ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + LENGTH - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				buffer.put(new byte[foreshift + lastshift]);
				for(int i=start+foreshift, d=start + LENGTH - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return (char) buffer.getInt(0);
			}
			
		};
		
		public final static OutputType<Float> FLOAT = new OutputType<Float>() {
			
			public final static int LENGTH = 4;

			@Override
			public Float fromBytes(byte[] bytes, int start) {
				ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + LENGTH - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				buffer.put(new byte[foreshift + lastshift]);
				for(int i=start+foreshift, d=start + LENGTH - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return buffer.getFloat(0);
			}
			
		};
		
		public final static OutputType<Double> DOUBLE = new OutputType<Double>(){
			
			public final static int LENGTH = 8;

			@Override
			public Double fromBytes(byte[] bytes, int start) {
				ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + LENGTH - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				buffer.put(new byte[foreshift + lastshift]);
				for(int i=start+foreshift, d=start + LENGTH - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return buffer.getDouble(0);
			}
			
		};
		
		public final static OutputType<String> STRING = new OutputType<String>() {

			@Override
			public String fromBytes(byte[] bytes, int start) {
				int length = INTEGER.fromBytes(bytes, start);
				start += 4;
				
				int foreshift = (start >= 0 ? 0 : 0 - start);
				int lastshift = start + length - bytes.length;
				lastshift = lastshift > 0 ? lastshift : 0;
				
				ByteBuffer buffer = ByteBuffer.allocate(length - (foreshift + lastshift));
				for(int i=start+foreshift, d=start + length - lastshift;i<d;i++) {
					buffer.put(bytes[i]);
				}
				
				return new String(buffer.array());
			}
			
		};
		
	}
	
	private final static char[] HEX_ALPHABET = 
		{
				'0', '1', '2', '3', 
				'4', '5', '6', '7', 
				'8', '9', 'A', 'B', 
				'C', 'D', 'E', 'F'
				};
	
	public static String toHexString(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : bytes) {
			stringBuilder.append(HEX_ALPHABET[b>>4]);
			stringBuilder.append(HEX_ALPHABET[b]);
		}
		return stringBuilder.toString();
	}
	
	public static String toHexStringWithSpace(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : bytes) {
			stringBuilder.append(HEX_ALPHABET[0xF & (b>>4)]);
			stringBuilder.append(HEX_ALPHABET[0xF & b]);
			stringBuilder.append(' ');
		}
		return stringBuilder.toString();
	}

}