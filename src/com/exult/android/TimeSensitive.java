package com.exult.android;

public interface TimeSensitive {
	public void handleEvent(int ctime, Object udata);
	public boolean alwaysHandle();	// Activate even if time is paused.
	public void addedToQueue();
	public void removedFromQueue();
}
