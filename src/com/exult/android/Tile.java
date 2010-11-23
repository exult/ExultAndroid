package com.exult.android;

/*
 * 3D tile coordinate.
 */
public class Tile {
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
}
