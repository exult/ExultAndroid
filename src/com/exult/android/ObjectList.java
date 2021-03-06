package com.exult.android;

/*
 * A list of objects chained together using the 'next' and 'prev' fields in GameObject.
 */
public class ObjectList {
	private GameObject first;		// .first in (circular) chain.
	private short iterCount;	// # of iterators.
	public ObjectList(GameObject f) {
		first = f;
	}
	public ObjectList() { }
	public void reportProblem() {
		//++FINISH?
	}
	public boolean isEmpty()
		{ return first == null; }
	public void addIterator()
		{ iterCount++; }
	public void removeIterator()
		{ iterCount--; }
	public GameObject getFirst()
		{ return first; }
				// Insert at head of chain.
	public void insert(GameObject nobj) {
		if (iterCount != 0)
			reportProblem();
		if (first == null)		// First one.
			nobj.next = nobj.prev = nobj;
		else {
			nobj.next = first;
			nobj.prev = first.prev;
			first.prev.next = nobj;
			first.prev = nobj;
		}
		first = nobj;
	}
				// Insert before given obj.
	public void insertBefore(GameObject nobj, GameObject before) {
		if (iterCount != 0)
			reportProblem();
		nobj.next = before;
		nobj.prev = before.prev;
		before.prev.next = nobj;
		before.prev = nobj;
		first = before == first ? nobj : first;
	}
				// Append.
	public void append(GameObject nobj)
		{ insert(nobj); first = nobj.next; }
	public void remove(GameObject dobj) {
	if (iterCount != 0)
		reportProblem();
		if (dobj == first)
			first = dobj.next != first ? dobj.next : null;
		dobj.next.prev = dobj.prev;
		dobj.prev.next = dobj.next;
	}
	/*
	 * Iterators
	 */
	public static abstract class ObjectIteratorBase {
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
	public static class ObjectIterator extends ObjectIteratorBase {
		public ObjectIterator(ObjectList l) {
			super(l);
			cur = first = l.first;
			stop = null;
		}
		public void reset() {
			cur = first; stop = null;
		}
		public GameObject next() {
			if (cur == stop)
				return null;
			GameObject ret = cur;
			cur = cur.next;
			stop = first;
			return ret;
		}
	}
	public static class ObjectIteratorBackwards extends ObjectIteratorBase {
		public void reset() {
			cur = first; stop = null; }
		public ObjectIteratorBackwards(ObjectList l) {
			super(l);
			cur = first = l.getFirst();
			stop = null;
		}
		public GameObject next() {
			if (cur == stop)
				return null;
			cur = cur.prev;
			stop = first;
			return cur;
		}
	}
	public static class NonflatObjectIterator extends ObjectIterator {
		private GameObject nonflats;
		public void reset()
			{ this.cur = nonflats; this.stop = null; }
		public NonflatObjectIterator(ObjectList l, GameObject firstNonflat) { 
		super(l);
		nonflats = firstNonflat;
		reset(); 
		}
	};
	public static class FlatObjectIterator extends ObjectIteratorBase {
		private GameObject stopAt;
		public FlatObjectIterator(ObjectList l, GameObject firstNonflat) {
			super(l);
			first = l.first == firstNonflat ? null : l.first;
			stopAt = firstNonflat != null ? firstNonflat : l.first;
			cur = first; stop = null;
		}
		public GameObject next() {
			if (cur == stop)
				return null;
			GameObject ret = cur;
			cur = cur.next;
			stop = stopAt;
			return ret;
		}
	}
	ObjectIterator getIterator() {
		return new ObjectIterator(this);
	}
	FlatObjectIterator getFlatIterator(GameObject firstNonflat) {
		return new FlatObjectIterator(this, firstNonflat);
	}
	NonflatObjectIterator getNonflatIterator(GameObject firstNonflat) {
		return new NonflatObjectIterator(this, firstNonflat);
	}
}
