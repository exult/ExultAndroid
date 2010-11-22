package com.exult.android;

public abstract class PathFinder {
	/* +++++MAYBE not needed.
	protected Tile src, dest;
	
	public final Tile getSrc() {
		return src;
	}
	public final Tile getDest() {
		return dest;
	}
	*/
	abstract public boolean NewPath(Tile s, Tile d);
	abstract public boolean getNextStep(Tile n);
	abstract public int getNumSteps();
}
