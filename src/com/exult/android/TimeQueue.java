package com.exult.android;
import java.util.LinkedList;
import java.util.ListIterator;

public class TimeQueue {
	private LinkedList<QueueEntry> entries = new LinkedList<QueueEntry>();
	private int pauseTime;		// Time when paused.
	private int paused;			// # calls to 'pause'.
	public static int ticks = 0;
	public static int tickMsecs = 10;
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
		synchronized (entries) {
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
		}
		entries.addLast(newent);
	}
	/*
	 * Activate those entries marked 'always'.
	 */
	public final void activateAlways(int ctime) {
		if (entries.isEmpty())
			return;
		
		boolean tryAgain = true;
		while (tryAgain) synchronized (entries) {
			ListIterator<QueueEntry> it = entries.listIterator();
			tryAgain = false;
			while (it.hasNext()) {
				QueueEntry ent = it.next();
				if (ctime < ent.time)
					return;
				TimeSensitive obj = ent.handler;
				if (obj.alwaysHandle()) {
					obj.removedFromQueue();
					Object udata = ent.udata;
					entries.remove();
					obj.handleEvent(ctime, udata);
					tryAgain = true;
					break;	// So we don't crash on the iterator.
				}
			}
		}
	}
	/*
	 * Remove and activate entries that are due.
	 */
	public final void activate(int ctime) {
		if (paused > 0) {
			activateAlways(ctime);
		} else synchronized(entries) {
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
	public void pause() {
		pause(ticks);
	}
	public void resume(int ctime) {
		if (paused == 0 || --paused > 0)	// Only unpause when stack empty.
			return;			// Not paused.
		int diff = ctime - pauseTime;
		pauseTime = 0;
		if (diff < 0)			// Should not happen.
			return;
		synchronized(entries) {
			ListIterator<QueueEntry> it = entries.listIterator();
			while (it.hasNext()) {
				QueueEntry ent = it.next();
				if (!ent.handler.alwaysHandle())
					ent.time += diff;	// Push entries ahead.
			}
		}
	}
	public void resume() {
		resume(ticks);
	}
	/*
	 *	Remove first entry containing a given object.
	 */
	public boolean remove(TimeSensitive obj) {
		if (entries.isEmpty())
			return false;
		synchronized(entries) {
			ListIterator<QueueEntry> it = entries.listIterator();
			while (it.hasNext()) {
				QueueEntry ent = it.next();
				if (ent.handler == obj) {
					obj.removedFromQueue();
					it.remove();
					return true;
				}
			}
		}
		return false;			// Not found.
	}
	/*
	 *	Find when an entry is due.
	 *
	 *	Output:	delay in msecs. when due, or -1 if not in queue.
	 */
	public int findDelay(TimeSensitive obj, int ctime) {
		if (entries.isEmpty())
			return -1;
		ListIterator<QueueEntry> it = entries.listIterator();
		while (it.hasNext()) {
			QueueEntry ent = it.next();
			if (ent.handler==obj) {
				if (pauseTime != 0)	// Watch for case when paused.
					ctime = pauseTime;
				int delay = ent.time - ctime;
				return delay >= 0 ? delay : 0;
			}
		}
		return -1;
	}
	public static class QueueEntry {
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
