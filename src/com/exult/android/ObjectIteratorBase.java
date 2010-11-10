package com.exult.android;

public abstract class ObjectIteratorBase {
	protected ObjectList list;
	protected GameObject first, stop, cur;
	public ObjectIteratorBase(ObjectList l) {
		list = l;
		list.addIterator();
	}
	public void done() {
		list.removeIterator();
	}
	public abstract GameObject next();
}
