package ink.aquar.scp.util;

public class IntegerMap<T> {
	
private final static int DEFAULT_TOP_DEPTH = 7;
	
	private final int topDepth; // min for 0, byte for 1, short for 3, int for 7, long for 15, max for 15.
	
	private int quantity;
	
	private final Object[] box;
	
	public IntegerMap(){
		this(new Object[16], DEFAULT_TOP_DEPTH);
	}
	
	public IntegerMap(int topDepth){
		this(new Object[16], topDepth);
	}
	
	/*IntegerMap(Object[] box){
		this(box, DEFAULT_TOP_DEPTH);
	}*/
	
	private IntegerMap(Object[] box, int topDepth){
		this.box = box;
		this.topDepth = topDepth;
	}
	
	@SuppressWarnings("unchecked")
	public T get(int index){
		return (T) get(index, topDepth);
	}
	
	private Object get(int index, int depth){
		if (depth>0) {
			int i = (int) ((index>>((depth)<<2)) & 0xF);
			if(box[i] == null) return null;
			else return ((IntegerMap<?>) box[i]).get(index, depth-1);
		} else {
			return box[(int)(index & 0xF)];
		}
	}
	
	public void set(int index, T obj){
		if(obj == null) {
			remove(index, topDepth);
		} else {
			set(index, obj, topDepth);
		}
	}
	
	private void set(int index, Object obj, int depth){
		if (depth>0) {
			int i = (int) ((index>>((depth)<<2)) & 0xF);
			if(box[i] == null) {
				box[i] = new IntegerMap<Object>();
				quantity++;
			}
			((IntegerMap<?>) box[i]).set(index, obj, depth-1);
			//checkFullAsMap();
		} else {
			int i = (int)(index & 0xF);
			// add
			if(box[i] == null){
				box[i] = obj;
				quantity++;
			// edit
			}else{
				box[i] = obj;
			}
			//box[(int)(index & 0xF)] = obj;
			//checkFullAsBase();
		}
	}
	
	public void remove(int index){
		remove(index, topDepth);
	}
	
	/**
	 * Don't be misled! The return is if the sub-box is empty!
	 * @param index Initial index
	 * @param depth Magic Value
	 * @return if the sub-box is empty
	 */
	private boolean remove(int index, int depth){
		if(depth>0){
			int i = (int) ((index>>((depth)<<2)) & 0xF);
			if(box[i] == null) return false; // Do nothing, just cancel.
			// Tip: return false also can cancel the nest checking.
			//full = false;
			if(((IntegerMap<?>) box[i]).remove(index, depth-1)){
				box[i] = null;
				quantity--;
				return !(quantity > 0);
				//return checkEmpty();
			} else {
				return false;
			}
		} else {
			int i = (int)(index & 0xF);
			if(box[i] == null) return false;
			else{
				box[i] = null;
				quantity--;
				return !(quantity > 0);
			}
			//full = false;
			//box[i] = null;
			//return checkEmpty();
		}
	}
	
	/*private void checkFullAsMap(){
		for(Object subbox:box) if(subbox == null || !((IntegerMap) subbox).full) {
			full = false;
			return;
		}
		full = true;
	}*/
	
	/*private void checkFullAsBase(){
		for(Object obj:box) if(obj == null) {
			full = false;
			return;
		}
		full = true;
	}*/
	
	/*private boolean checkEmpty(){
		for(Object obj:box) if(obj != null) return false;
		return true;
	}*/
	
	public int indexOf(Object obj){
		return indexOf(obj, topDepth);
	}
	
	private int indexOf(Object obj, int depth){
		if(depth>0){
			for(int i=0;i<16;i++) if(box[i] != null) {
				int index = ((IntegerMap<?>) box[i]).indexOf(obj, depth-1);
				if(index >= 0) return index + (((int)(i))<<(depth<<2));
			}
			return -1;
		} else {
			if(obj == null){
				for(int i=0;i<16;i++) if(null == box[i]) return i;
				return -1;
			} else {
				for(int i=0;i<16;i++) if(obj.equals(box[i])) return i;
				return -1;
			}
			
		}
	}
	
	public int size(){
		return size(topDepth);
	}
	
	private int size(int depth){
		if(depth>0){
			int size = 0;
			for(Object subbox:box) if(subbox != null) size += ((IntegerMap<?>) subbox).size(depth-1);
			return size;
		} else {
			return quantity;
		}
	}
	
	public boolean isEmpty(){
		return !(quantity > 0);
	}
	
	public boolean contains(Object obj){
		return indexOf(obj, topDepth) >= 0;
	}
	
	public void clear(){
		for(int i=0;i<16;i++) box[i] = null;
		quantity = 0;
	}

}
