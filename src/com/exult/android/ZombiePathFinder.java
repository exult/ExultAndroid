package com.exult.android;

public class ZombiePathFinder extends PathFinder {
	private int major_distance;		// Distance in tiles to go.
	private int major_frame_incr;	// # steps to take in faster dir.
	private int cur[] = new int[3];	// Current pos. within world (x, y, z).
	private static final int X = 0, Y = 1, Z = 2;
									// Indices to cur.tx, cur.ty.
	int major_coord, minor_coord1, minor_coord2;
					// 1 or -1 for dir. along each axis.
	private int major_dir, minor_dir1, minor_dir2;
	private int major_delta, minor_delta1, minor_delta2;	
					// For each tile we move along major
					//   axis, we add 'minor_delta'.  When
					//   the sum >= 'major_delta', we move
					//   1 tile along minor axis, and
					//   subtract 'major_delta' from sum.
	private int sum1, sum2;			// Sum of 'minor_delta''s.
	
	@Override
	public boolean NewPath(Tile s, Tile d) {
		cur[X] = s.tx; cur[Y] = s.ty; cur[Z] = s.tz;
		sum1 = sum2 = 0;		// Clear accumulators.
		int deltax = Tile.delta(s.tx, d.tx);
		int deltay = Tile.delta(s.ty, d.ty);
		int deltaz = Tile.delta(s.tz, d.tz);
		if (deltax == 0 && deltay == 0 && deltaz == 0) {	// Going nowhere?
			major_distance = 0;
			return false;
		}		
		int abs_deltax, abs_deltay, abs_deltaz;
		int x_dir, y_dir, z_dir;	// Figure directions.
		if (deltax >= 0) {
			abs_deltax = deltax; x_dir = 1;
		} else {
			abs_deltax = -deltax; x_dir = -1;
		}
		if (deltay >= 0) {
			abs_deltay = deltay; y_dir = 1;
		} else {
			abs_deltay = -deltay; y_dir = -1;
		}
		if (deltaz >= 0) {
			abs_deltaz = deltaz; z_dir = 1;
		} else {
			abs_deltaz = -deltaz; z_dir = -1;
		}
		if (abs_deltaz >= abs_deltax &&	// Moving fastest along z?
		    abs_deltaz >= abs_deltay)
			{
			major_coord = Z;
			minor_coord1 = X;
			minor_coord2 = Y;
			major_dir = z_dir;
			minor_dir1 = x_dir;
			minor_dir2 = y_dir;
			major_delta = abs_deltaz;
			minor_delta1 = abs_deltax;
			minor_delta2 = abs_deltay;
			}
		else if (abs_deltay >= abs_deltax &&	// Moving fastest along y?
		    	 abs_deltay >= abs_deltaz)
			{
			major_coord = Y;
			minor_coord1 = X;
			minor_coord2 = Z;
			major_dir = y_dir;
			minor_dir1 = x_dir;
			minor_dir2 = z_dir;
			major_delta = abs_deltay;
			minor_delta1 = abs_deltax;
			minor_delta2 = abs_deltaz;
			}
		else				// Moving fastest along x?
			{
			major_coord = X;
			minor_coord1 = Y;
			minor_coord2 = Z;
			major_dir = x_dir;
			minor_dir1 = y_dir;
			minor_dir2 = z_dir;
			major_delta = abs_deltax;
			minor_delta1 = abs_deltay;
			minor_delta2 = abs_deltaz;
			}
		major_distance = major_delta;	// How far to go.
		return true;
	}
	@Override
	public boolean getNextStep(Tile n) {
		if (major_distance <= 0) {
			return false;
		}
					// Subtract from distance to go.
		major_distance -= major_frame_incr;
					// Accumulate change.
		sum1 += major_frame_incr * minor_delta1;
		sum2 += major_frame_incr * minor_delta2;
					// Figure change in slower axes.
		int minor_frame_incr1 = sum1/major_delta;
		int minor_frame_incr2 = sum2/major_delta;
		sum1 = sum1 % major_delta;	// Remove what we used.
		sum2 = sum2 % major_delta;	// Remove what we used.
					// Update coords. within world.
		cur[major_coord] += major_dir*major_frame_incr;
		cur[minor_coord1] += minor_dir1*minor_frame_incr1;
		cur[minor_coord2] += minor_dir2*minor_frame_incr2;
					// Watch for wrapping.
		cur[major_coord] = (cur[major_coord] + EConst.c_num_tiles)%EConst.c_num_tiles;
		cur[minor_coord1] = (cur[minor_coord1] + EConst.c_num_tiles)%EConst.c_num_tiles;
		cur[minor_coord2] = (cur[minor_coord2] + EConst.c_num_tiles)%EConst.c_num_tiles;
		if (cur[Z] < 0) {		// We are below ground level.
			cur[Z] = 0;
			major_distance = 0;
			return false;
		}
		n.set(cur[X], cur[Y], cur[Z]);
		return true;
	}
	@Override
	public int getNumSteps() {
		 return major_distance/major_frame_incr;
	}
}
