package com.exult.android;

public abstract class UsecodeValue {
	public int getIntValue() {
		return 0;
	}
	public String getStringValue() {
		return null;
	}
	public int needIntValue() {
		return 0;
	}
	public boolean isFalse() {
		return false;
	}
	public final boolean isTrue() {
		return !isFalse();
	}
	public UsecodeValue plus(UsecodeValue v2) {
		return new IntUsecodeValue(0);	// This is undefined for plus.
	}
	
	public static class IntUsecodeValue extends UsecodeValue {
		private int intval;
		public IntUsecodeValue(int i) {
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
			if (v2 instanceof StringUsecodeValue) {
				String s = intval + v2.getStringValue();
				return new StringUsecodeValue(s);
			} else if (v2 instanceof IntUsecodeValue) {
				return new IntUsecodeValue(intval + v2.getIntValue());
			} else
				return this;
		}
	}
	public static class StringUsecodeValue extends UsecodeValue {
		private String str;
		public StringUsecodeValue(String s) {
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
		public UsecodeValue plus(UsecodeValue v2) {
			if (v2 instanceof IntUsecodeValue) {
				String s = str + v2.getIntValue();
				return new StringUsecodeValue(s);
			} else if (v2 instanceof StringUsecodeValue) {
				String s = str + v2.getStringValue();
				return new StringUsecodeValue(s);
			} else
				return this;
		}
	}
	public static class ArrayUsecodeValue extends UsecodeValue {
		private UsecodeValue elems[];
		public ArrayUsecodeValue(int sz) {
			elems = new UsecodeValue[sz];
		}
		public int needIntValue() {
			return elems.length > 0 ? elems[0].needIntValue() : 0;
		}
		public boolean isFalse() {
			return elems.length == 0;
		}
	}
	public static class ObjectUsecodeValue extends UsecodeValue {
		private GameObject obj;
		public ObjectUsecodeValue(GameObject o) {
			obj = o;
		}
		public int needIntValue() {
			//++++++++???
			return 0;
		}
		public boolean isFalse() {
			return obj == null;
		}
	}
}
