package com.exult.android;
import java.util.LinkedList;
import java.util.ListIterator;

public class TimeQueue {
	private LinkedList<QueueEntry> entries;
	private int pauseTime;		// Time when paused.
	private int paused;			// # calls to 'pause'.
	public static int ticks = 0;
	/*
	 *	Remove all entries.
	 */
	public void clear() {
		ListIterator<QueueEntry> it = entries.listIterator();
		while (it.hasNext()) {
			QueueEntry ent = it.next();
			TimeSensitive obj = ent.handler;
			it.remove();
			obj.removedFromQueue();
		}
	}
	/*
	 *	Add an entry to the queue.
	 */
	public void add
		(
		int t,		// When entry is to be activated.
		TimeSensitive obj,		// Object to be added.
		Object ud				// User data.
		) {
		obj.addedToQueue();		// It's going in, no matter what.
		QueueEntry newent = new QueueEntry();;
		if (paused > 0 && !obj.alwaysHandle())	// Paused?
						// Messy, but we need to fix time.
			t -= ticks - pauseTime;
		newent.set(t, obj, ud);
		if (entries.isEmpty()) {
			entries.addFirst(newent);
			return;
		}
		ListIterator<QueueEntry> it = entries.listIterator();
		while (it.hasNext()) {
			QueueEntry ent = it.next();
			if (newent.time < ent.time) {
				if (ent != it.previous()) {	// 
					System.out.println("TimeQueue.add:  ERROR INSERTING!!!");
				} else 
					it.add(newent);
				return;
				}
		}
		entries.addLast(newent);
	}
	/*
	 * Activate those entries marked 'always'.
	 */
	public final void activateAlways(int ctime) {
		if (entries.isEmpty())
			return;
		ListIterator<QueueEntry> it = entries.listIterator();
		while (it.hasNext()) {
			QueueEntry ent = it.next();
			if (ctime < ent.time)
				break;
			TimeSensitive obj = ent.handler;
			if (obj.alwaysHandle()) {
				obj.removedFromQueue();
				Object udata = ent.udata;
				entries.remove();
				obj.handleEvent(ctime, udata);
			}
		}
	}
	/*
	 * Remove and activate entries that are due.
	 */
	public final void activate(int ctime) {
		if (paused > 0) {
			activateAlways(ctime);
		} else {
			while (!entries.isEmpty() && ctime >= entries.getFirst().time) {
				QueueEntry ent = entries.removeFirst();
				TimeSensitive obj = ent.handler;
				Object udata = ent.udata;
				obj.removedFromQueue();
				obj.handleEvent(ctime, udata);
			}
		}
	}
	public void pause(int ctime) {	// Game paused.
	if (paused++ == 0)
		pauseTime = ctime;
	}
	public void resume(int ctime) {
		if (paused == 0 || --paused > 0)	// Only unpause when stack empty.
			return;			// Not paused.
		int diff = ctime - pauseTime;
		pauseTime = 0;
		if (diff < 0)			// Should not happen.
			return;
		ListIterator<QueueEntry> it = entries.listIterator();
		while (it.hasNext()) {
			QueueEntry ent = it.next();
			if (ent.handler.alwaysHandle())
				ent.time += diff;	// Push entries ahead.
		}
	}
	
	public class QueueEntry {
		TimeSensitive handler;
		Object udata;		// Data to pass to handler.
		int time;			// Time when due.
		void set(int t, TimeSensitive h, Object u) {
			time = t;
			handler = h;
			udata = u;
		}
	}
}
