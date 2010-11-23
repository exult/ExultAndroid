package com.exult.android;

public abstract class PathFinder {
	/* +++++MAYBE not needed.
	protected Tile src, dest;
	
	public final void getSrc(Tile s) {
		return s.set(src);
	}
	public final void getDest(Tile d) {
		return d.set(dest);
	}
	*/
	abstract public boolean NewPath(Tile s, Tile d);
	abstract public boolean getNextStep(Tile n);
	abstract public int getNumSteps();
	abstract public boolean isDone();
}
