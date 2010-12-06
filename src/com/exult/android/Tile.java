package com.exult.android;

/*
 * 3D tile coordinate.
 */
public class Tile {
	private final int neighbors[] = 
		{0,-1, 1,-1, 1,0, 1,1, 0,1, -1,1, -1,0, -1,-1 };
	public short tx, ty, tz;
	public void set(int x, int y, int z) {
		tx = (short)x; ty = (short)y; tz = (short)z;
	}
	public Tile(int x, int y, int z) {
		tx = (short)tx; ty = (short)y; tz = (short)z;
	}
	public Tile() {
	}
	public Tile(Tile s) {
		tx = s.tx; ty = s.ty; tz = s.tz;
	}
	public static boolean gte(int t1, int t2) {	// Ret t1 >= t2 with wrapping.
		int diff = t1 - t2;
		return diff >= 0 ? (diff < EConst.c_num_tiles/2) :
						diff < -EConst.c_num_tiles/2;
	}
	public static int fix(int x) {
		 return (x+EConst.c_num_tiles)%EConst.c_num_tiles;
	}
	public final void fixme() {
		tx = (short)fix(tx); ty = (short)fix(ty);
	}
	public static int delta(int from, int to) {
		int diff = to - from;
		return diff >= EConst.c_num_tiles/2 ? (diff - EConst.c_num_tiles) :
			(diff <= -EConst.c_num_tiles/2 ? (diff + EConst.c_num_tiles) :
							diff);
	}	
	public int distance(Tile t2) {	// Distance to another tile?
		int delta = distance2d(t2);
		int dz = t2.tz - tz;
		if (dz < 0)
			dz = -dz;
					// Take larger abs. value.
		return (delta > dz ? delta : dz);
	}
	public int distance2d(Tile t2)	// For pathfinder.
		{			// Handle wrapping round the world.
		int dy = (t2.ty - ty + EConst.c_num_tiles)%EConst.c_num_tiles;
		int dx = (t2.tx - tx + EConst.c_num_tiles)%EConst.c_num_tiles;
		if (dy >= EConst.c_num_tiles/2)// World-wrapping.
			dy = EConst.c_num_tiles - dy;
		if (dx >= EConst.c_num_tiles/2)
			dx = EConst.c_num_tiles - dx;
					// Take larger abs. value.
		return (dy > dx ? dy : dx);
	}				// Get neighbor in given dir (0-7) & set 't' to it.
	public void getNeighbor(Tile t, int dir) {
		t.set(
		(tx + neighbors[2*dir] + EConst.c_num_tiles)%EConst.c_num_tiles,
		(ty + neighbors[2*dir + 1] + EConst.c_num_tiles)%EConst.c_num_tiles,
							 tz); 
	}
}
