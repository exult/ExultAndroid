package com.exult.android;

import java.util.Vector;

public abstract class UsecodeValue {
	private static UsecodeValue zval = new IntValue(0);
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
		public ArrayValue(Vector<UsecodeValue> vals) {
			elems = vals.toArray(elems);
		}
		public int needIntValue() {
			return elems.length > 0 ? elems[0].needIntValue() : 0;
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
		// Add values to end, and return # added.
		public final static void addValues(Vector<UsecodeValue>vals, UsecodeValue val2) {
			if (val2 instanceof ArrayValue) {
				ArrayValue arr2 = (ArrayValue) val2;
				int size2 = arr2.elems.length;
				vals.ensureCapacity(size2);
				for (int i = 0; i < size2; i++)
					vals.addElement(arr2.elems[i]);
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
