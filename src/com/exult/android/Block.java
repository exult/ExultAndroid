package com.exult.android;
/*
 *	A 3-dim. block.
 */
public final class Block {
	public int x, y, z;			// Position.
	public int w, d, h;			// Dimensions.
	public Block(int xin, int yin, int zin, int win, int din, int hin) {
		x = xin; y = yin; z = zin;
		w = win; d = din; h = hin;
	}
	public void set(int xin, int yin, int zin, int win, int din, int hin) {
		x = xin; y = yin; z = zin;
		w = win; d = din; h = hin;
	}
	public Block() { }			// An uninitialized one.
						// Is this point in it?
	public boolean hasPoint(int px, int py, int pz) {
		return (px >= x && px < x + w && py >= y && py < y + d &&
				  pz >= z && pz < z + h); 
	}
}
