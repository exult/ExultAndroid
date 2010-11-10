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
		//++++++FINISH
	}
	public boolean isEmpty()
		{ return first == null; }
	public void addIterator()
		{ iterCount++; }
	public void remove_iterator()
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
}
