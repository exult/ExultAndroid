package com.exult.android;

import java.util.Vector;

public abstract class UsecodeValue {
	private static UsecodeValue zval = new IntValue(0);
	private static UsecodeValue oneVal = new IntValue(1);
	private static UsecodeValue nullObj = new ObjectValue(null);
	public int getIntValue() {
		return 0;
	}
	public String getStringValue() {
		return null;
	}
	public int needIntValue() {
		return 0;
	}
	public GameObject getObjectValue() {
		return null;
	}
	public boolean isFalse() {
		return false;
	}
	public final boolean isTrue() {
		return !isFalse();
	}
	public boolean isArray() {
		return false;
	}
	public int getArraySize() {
		return 0;
	}
	public UsecodeValue getElem(int i) {
		return zval;
	}
	public UsecodeValue getElem0() {
		return this;
	}
	public int findElem(UsecodeValue val) {
		return -1;
	}
	public UsecodeValue putElem(int index, UsecodeValue val) {
		return this;
	}
	//	Append array or value to end.
	public UsecodeValue concat(UsecodeValue val) {
		if (val instanceof ArrayValue)
			return ((ArrayValue)val).insert(this);
		else
			return new ArrayValue(this, val);
	}
	public UsecodeValue plus(UsecodeValue v2) {
		return new IntValue(0);	// This is undefined for plus.
	}
	//	++++Not doing 'class_obj_type' yet.
	public int getClassVarCount() {
		return 0;
	}	
	public UsecodeValue nthClassVar(int n) {
		return zval;
	}
	public abstract boolean eq(UsecodeValue v2);
	public static final UsecodeValue getZero() {
		return zval;
	}
	public static final UsecodeValue getOne() {
		return oneVal;
	}
	public static final UsecodeValue getNullObj() {
		return nullObj;
	}
	
	/*
	 * All these subclasses are intended to be immutable.
	 */
	public final static class IntValue extends UsecodeValue {
		private int intval;
		public IntValue(int i) {
			intval = i;
		}
		public int getIntValue() {
			return intval;
		}
		public int needIntValue() {
			return intval;
		}
		public boolean isFalse() {
			return intval == 0;
		}
		public String toString() {
			return Integer.toHexString(intval);
		}
		public UsecodeValue plus(UsecodeValue v2) {
			if (v2 instanceof StringValue) {
				String s = intval + v2.getStringValue();
				return new StringValue(s);
			} else if (v2 instanceof IntValue) {
				return new IntValue(intval + v2.getIntValue());
			} else
				return this;
		}
		public boolean eq(UsecodeValue v2) {
			if (v2 instanceof IntValue) {
				return intval == v2.getIntValue();
			} else if (v2 instanceof ArrayValue) {
				ArrayValue arr2 = (ArrayValue) v2;
				if (arr2.elems.length == 0)
					return intval == 0;
				else
					return eq(arr2.elems[0]);
			} else if (intval == 0 && v2 instanceof ObjectValue) {
				return ((ObjectValue)v2).obj == null;
			} else
				return false;
		}
	}
	public final static class StringValue extends UsecodeValue {
		private String str;
		public StringValue(String s) {
			str = s;
		}
		public String getStringValue() {
			return str;
		}
		public int needIntValue() {
			try {
				return Integer.parseInt(str);
			} catch (NumberFormatException n) {
				return 0;
			}
		}
		public String toString() {
			return '"' + str + '"';
		}
		public final UsecodeValue plus(UsecodeValue v2) {
			if (v2 instanceof IntValue) {
				String s = str + v2.getIntValue();
				return new StringValue(s);
			} else if (v2 instanceof StringValue) {
				String s = str + v2.getStringValue();
				return new StringValue(s);
			} else
				return this;
		}
		public boolean eq(UsecodeValue v2) {
			String s2 = v2.getStringValue();
			return s2 != null && str.equals(s2);
		}
	}
	public final static class ArrayValue extends UsecodeValue {
		private UsecodeValue elems[];
		private ArrayValue(UsecodeValue e[]) {
			elems = e;
		}
		public ArrayValue(Vector<UsecodeValue> vals) {
			int cnt = vals.size();
			elems = new UsecodeValue[cnt];
			for (int i = 0; i < cnt; ++i)
				elems[i] = vals.elementAt(i);
		}
		public ArrayValue(UsecodeValue v0, UsecodeValue v1) {
			elems = new UsecodeValue[2];
			elems[0] = v0; elems[1] = v1;
		}
		public ArrayValue(UsecodeValue v0, UsecodeValue v1, UsecodeValue v2) {
			elems = new UsecodeValue[3];
			elems[0] = v0; elems[1] = v1; elems[2] = v2;
		}
		public ArrayValue(UsecodeValue v0, UsecodeValue v1, UsecodeValue v2,
									UsecodeValue v3) {
			elems = new UsecodeValue[4];
			elems[0] = v0; elems[1] = v1; elems[2] = v2; elems[3] = v3;
		}
		public static final ArrayValue createObjectsList(Vector<GameObject> objs) {
			int cnt = objs.size();
			UsecodeValue elems[] = new UsecodeValue[cnt];
			for (int i = 0; i < cnt; ++i)
				elems[i] = new ObjectValue(objs.elementAt(i));
			return new ArrayValue(elems);
		}
		public int needIntValue() {
			return elems.length > 0 ? elems[0].needIntValue() : 0;
		}
		public String toString() {
			StringBuffer s = new StringBuffer(50);
			s.append('[');
			for (int i = 0; i < elems.length; ++i) {
				if (i > 0)
					s.append(", ");
				s.append(elems[i].toString());
			}
			s.append(']');
			return s.toString();
		}
		public boolean isFalse() {
			return elems.length == 0;
		}
		public boolean isArray() {
			return true;
		}
		public int getArraySize() {
			return elems.length;
		}
		public UsecodeValue getElem(int i) {
			return elems[i];
		}
		public UsecodeValue getElem0() {
			return elems[0];
		}
		public int findElem(UsecodeValue val) {
			int cnt = elems.length;
			for (int i = 0; i < cnt; i++)
				if (elems[i].eq(val))
					return (i);
			return (-1);
		}
		public UsecodeValue putElem(int ind, UsecodeValue val) {
			if (ind >= 0) {
				int newcnt = ind >= elems.length ? ind + 1 : elems.length;
				UsecodeValue newelems[] = new UsecodeValue[newcnt];
				System.arraycopy(elems, 0, newelems, 0, elems.length);
				newelems[ind] = val;
				return new ArrayValue(newelems);
			} else 
				return this;
		}
		//	Set element, and create array if needed.
		public static UsecodeValue forceElem(UsecodeValue arr,
								int ind, UsecodeValue val) {
			if (arr == null) {
				UsecodeValue newElems[] = new UsecodeValue[ind + 1];
				newElems[ind] = val;
				return new ArrayValue(newElems);
			} else if (arr instanceof ArrayValue) {
				return arr.putElem(ind, val);
			} else {
				UsecodeValue newElems[] = new UsecodeValue[ind + 1];
				newElems[0] = arr;		// Original becomes value 0.
				newElems[ind] = val;
				return new ArrayValue(newElems);
			}
		}
		//	Append array or value to end.
		public UsecodeValue concat(UsecodeValue val) {
			if (val instanceof ArrayValue) {
				ArrayValue arr2 = (ArrayValue) val;
				UsecodeValue newelems[] = 
					new UsecodeValue[elems.length + arr2.elems.length];
				System.arraycopy(elems, 0, newelems, 0, elems.length);
				System.arraycopy(arr2.elems, 0, newelems, elems.length, 
						arr2.elems.length);
				return new ArrayValue(newelems);
			} else 
				return putElem(elems.length, val);
		}
		//	Insert into front.
		public final UsecodeValue insert(UsecodeValue val) {
			UsecodeValue newelems[] = new UsecodeValue[elems.length + 1];
			newelems[0] = val;
			System.arraycopy(elems, 0, newelems, 1, elems.length);
			return new ArrayValue(newelems);
		}
		public boolean eq(UsecodeValue v2) {
			if (v2 instanceof ArrayValue) {
				ArrayValue arr2 = (ArrayValue) v2;
				int cnt = elems.length;
				if (cnt != arr2.elems.length)
					return false;
				for (int i = 0; i < cnt; i++) {
					UsecodeValue e1 = elems[i], e2 = arr2.elems[i];
					if (!e1.eq(e2))
						return false;
				}
				return true;		// Arrays matched.
			} else
				return v2.eq(this);
		}
		// Add values to end of a vector.
		public final static void addValues(Vector<UsecodeValue>vals, UsecodeValue val2) {
			if (val2 instanceof ArrayValue) {
				ArrayValue arr2 = (ArrayValue) val2;
				int size2 = arr2.elems.length;
				vals.ensureCapacity(size2);
				for (int i = 0; i < size2; i++) {
					vals.addElement(arr2.elems[i]);
				}
			} else {
				vals.addElement(val2);
			}
		}
	}
	public final static class ObjectValue extends UsecodeValue {
		private GameObject obj;
		public ObjectValue(GameObject o) {
			obj = o;
		}
		public int needIntValue() {
			//++++++++???
			return 0;
		}
		public String toString() {
			return obj != null ? obj.toString() : "null";
		}
		public GameObject getObjectValue() {
			return obj;
		}
		public boolean isFalse() {
			return obj == null;
		}
		public boolean eq(UsecodeValue v2) {
			if (v2 instanceof ObjectValue)
				return ((ObjectValue)v2).obj == obj;
			else if (obj == null && v2 instanceof IntValue)
				return ((IntValue)v2).intval == 0;
			else if (v2 instanceof ArrayValue) {
				ArrayValue arr2 = (ArrayValue) v2;
				return arr2.elems.length > 0 && eq(arr2.elems[0]);
			} else
				return false;
		}
	}
}
