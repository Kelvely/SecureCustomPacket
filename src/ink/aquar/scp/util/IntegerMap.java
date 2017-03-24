package ink.aquar.scp.util;

/**
 * A map that uses integer as key.
 * 
 * @author Aquarink Studio
 * @author Kevin Iry
 *
 * @param <T> The type of object stored in the map.
 */
public class IntegerMap<T> {
	
	private final static int DEFAULT_TOP_DEPTH = 7;
	
	private final int topDepth; // min for 0, byte for 1, short for 3, int for 7, max for 7.
	
	private int quantity;
	
	private final Object[] box;
	
	/**
	 * Create an IntegerMap with default topDepth 7.<br>
	 */
	public IntegerMap(){
		this(new Object[16], DEFAULT_TOP_DEPTH);
	}
	
	/**
	 * Create an IntegerMap with a customized topDepth.<br>
	 * <br>
	 * Capability = 16<sup>topDepth+1</sup><br>
	 * 
	 * @param topDepth The top depth decides the maximum capability of the map
	 */
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
	
	/**
	 * Get the corresponding object by the index.<br>
	 * 
	 * @param index The index of object
	 * @return The object
	 */
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
	
	/**
	 * Set the object to corresponding index.<br>
	 * 
	 * @param index The index of object
	 * @param obj The object
	 */
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
	
	/**
	 * Remove the corresponding object of the index.<br>
	 * 
	 * @param index The index
	 */
	public void remove(int index){
		remove(index, topDepth);
	}
	
	/**
	 * Don't be misled! The return is if the sub-box is empty!<br>
	 * 
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
	
	/**
	 * Find the object's first index in the map.<br>
	 * 
	 * @param obj The object
	 * @return The index of the object, if doesn't exist, -1 is returned
	 */
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
	
	/**
	 * Get number of elements in the map.<br>
	 * 
	 * @return The number of elements in the map
	 */
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
	
	/**
	 * Check if the map is empty.<br>
	 * 
	 * @return If the map is empty
	 */
	public boolean isEmpty(){
		return !(quantity > 0);
	}
	
	/**
	 * Check if an object is in the map.<br>
	 * 
	 * @param obj The object
	 * @return If the object is in the map
	 */
	public boolean contains(Object obj){
		return indexOf(obj, topDepth) >= 0;
	}
	
	/**
	 * Remove all objects in the map.
	 */
	public void clear(){
		for(int i=0;i<16;i++) box[i] = null;
		quantity = 0;
	}
	
	@Override
	/**
	 * 
	 */
	public IntegerMap<T> clone(){
		return clone(topDepth);
	}
	
	@SuppressWarnings("unchecked")
	private IntegerMap<T> clone(int depth){
		if(depth>0){
			IntegerMap<T> nIntMap = new IntegerMap<T>();
			for(int i=0;i<16;i++) if(box[i] != null) nIntMap.box[i] = ((IntegerMap<T>) box[i]).clone(depth-1);
			nIntMap.quantity = quantity;
			return nIntMap;
		}else{
			Object[] nbox = box.clone();
			IntegerMap<T> nIntMap = new IntegerMap<T>(nbox, topDepth);
			nIntMap.quantity = quantity;
			//nIntMap.box = nbox;
			return nIntMap;
		}
	}
	
	@Override
	public boolean equals(Object object){
		if(!(object instanceof IntegerMap)) {
			return false;
		}
		return equals((IntegerMap<?>) object, topDepth);
	}
	
	private boolean equals(IntegerMap<?> map, int depth){
		if(map == null) return false;
		
		if(topDepth != map.topDepth) return false;
		
		if(map.quantity != quantity) return false;
		
		if(depth>0){
			for(int i=0;i<16;i++){
				if(map.box[i] != null && box[i] != null){
					if(!((IntegerMap<?>) box[i]).equals((IntegerMap<?>) map.box[i], depth-1)) return false;
				}else if(map.box[i] != box[i]) return false;
			}
			return true;
		}else{
			for(int i=0;i<16;i++){
				if(map.box[i] != null && box[i] != null){
					if(!(box[i].equals(map.box[i]))) return false;
				}else if(map.box[i] != box[i]) return false;
			}
			return true;
		}
	}

}
