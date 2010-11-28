package com.exult.android;

public abstract class UsecodeValue {

	public static class IntUsecodeValue {
		private int intval;
		public IntUsecodeValue(int i) {
			intval = i;
		}
	}
	public static class StringUsecodeValue {
		private String str;
		public StringUsecodeValue(String s) {
			str = s;
		}
	}
	public static class ArrayUsecodeValue {
		private UsecodeValue elems[];
		public ArrayUsecodeValue(int sz) {
			elems = new UsecodeValue[sz];
		}
	}
	public static class ObjectUsecodeValue {
		private GameObject obj;
		public ObjectUsecodeValue(GameObject o) {
			obj = o;
		}
	}
}
