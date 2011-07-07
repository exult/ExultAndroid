package com.exult.android;

public interface TimeSensitive {
	public void handleEvent(int ctime, Object udata);
	public boolean alwaysHandle();	// Activate even if time is paused.
	public void addedToQueue();
	public void removedFromQueue();
	
	/*	A simple timer that just needs handleEvent overridden. */
	public static class Timer extends GameSingletons implements TimeSensitive {
		protected int timeQueueCount;
		public boolean alwaysHandle() {	// Activate even if time is paused.
			return false;
		}
		public void addedToQueue() {
			++timeQueueCount;
		}
		public void removedFromQueue() {
			--timeQueueCount;
		}
		public boolean inQueue() {
			return timeQueueCount > 0;
		}
		public void handleEvent(int ctime, Object udata) {
			
		}
	}
}
