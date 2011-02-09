package com.exult.android;
import java.util.Vector;
import android.graphics.Point;
/*
 *	A 'barge', such as a ship or horse-and-cart.  The elements of a barge
 *	are stored in the outside world, so rendering and obstacle detection
 *	don't have to be reimplemented.
 */
public class BargeObject extends ContainerGameObject implements TimeSensitive {
	private int timeQueueCount;	
	private Vector<GameObject> objects;	// All objects in/on barge.
	private int permCount;			// Counts permanent parts of barge,
	//   which proceed those placed on it.
	private int xtiles, ytiles;	// Tiles covered (when vertical).
	private int dir;		// Direction: 0=N, 1=E, 2=S, 3=W.
	private boolean complete;			// Flag:  all members have been read.
	private boolean gathered;			// Items on barge have been gathered.
	private boolean iceRaft;			// For Serpent Isle.
	private boolean firstStep;		// So first motion can just be 1 tile.
	private boolean taking2ndStep;		// Skip animation on 2nd step.
	private int boat;				// 1 if a boat, 0 if not; -1=untested.
	private int frameTime;			// Time between frames in msecs.  0 if
	//   not moving.
	private PathFinder path;		// For traveling.
	private Tile center;		// Center of barge.
	private Tile pos;			// Temp.
	private Point loc;
	private Rectangle footprint, newfoot, dirty;
	GameObject getObject(int i)
		{ return objects.elementAt(i); }
	/*
	 *	Rotate a point 90 degrees to the right around a point.
	 *
	 *	In cartesian coords with 'c' as center, the rule is:
	 *		(newx, newy) = (oldy, -oldx)
	 */
	private static void Rotate90r
		(
		Tile result,
		Tile t,			// Tile to move.
		Tile c			// Center to rotate around.
		)
		{
						// Get cart. coords. rel. to center.
		int rx = t.tx - c.tx, ry = c.ty - t.ty;
		result.set(c.tx + ry, c.ty + rx, t.tz);
		}

	/*
	 *	Rotate a point 90 degrees to the left around a point.
	 *
	 *	In cartesian coords with 'c' as center, the rule is:
	 *		(newx, newy) = (-oldy, oldx)
	 */

	private static void Rotate90l
		(
		Tile result,
		Tile t,			// Tile to move.
		Tile c			// Center to rotate around.
		)
		{
						// Get cart. coords. rel. to center.
		int rx = t.tx - c.tx, ry = c.ty - t.ty;
		result.set(c.tx - ry, c.ty - rx, t.tz);
		}

	/*
	 *	Rotate a point 180 degrees around a point.
	 *
	 *	In cartesian coords with 'c' as center, the rule is:
	 *		(newx, newy) = (-oldx, -oldy)
	 */

	private static void Rotate180
		(
		Tile result,
		Tile t,			// Tile to move.
		Tile c			// Center to rotate around.
		) {
						// Get cart. coords. rel. to center.
		int rx = t.tx - c.tx, ry = c.ty - t.ty;
		result.set(c.tx - rx, c.ty + ry, t.tz);
	}
	/*
	 *	Figure tile where an object will be if it's rotated 90 degrees around
	 *	a point counterclockwise, assuming its 'hot spot' 
	 *	is at its lower-right corner.
	 */
	private static void Rotate90r
		(
		Tile result,
		GameObject obj,
		int xtiles, int ytiles,		// Object dimensions.
		Tile c			// Rotate around this.
		) {
		obj.getTile(result);
						// Rotate hot spot.
		Rotate90r(result, result, c);
						// New hotspot is what used to be the
						//   upper-right corner.
		result.tx = (short)((result.tx + ytiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		result.ty = (short)((result.ty + EConst.c_num_tiles)%EConst.c_num_tiles);
	}

	/*
	 *	Figure tile where an object will be if it's rotated 90 degrees around
	 *	a point, assuming its 'hot spot' is at its lower-right corner.
	 */

	private static void Rotate90l
		(
		Tile result,
		GameObject obj,
		int xtiles, int ytiles,		// Object dimensions.
		Tile c			// Rotate around this.
		) {
		obj.getTile(result);
						// Rotate hot spot.
		Rotate90l(result, result, c);
						// New hot-spot is old lower-left.
		result.ty = (short)((result.ty + xtiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		result.tx = (short)((result.tx + EConst.c_num_tiles)%EConst.c_num_tiles);
	}
	/*
	 *	Figure tile where an object will be if it's rotated 180 degrees around
	 *	a point, assuming its 'hot spot' is at its lower-right corner.
	 */
	private static void Rotate180
		(
		Tile result,
		GameObject obj,
		int xtiles, int ytiles,		// Object dimensions.
		Tile c			// Rotate around this.
		) {
		obj.getTile(result);
						// Rotate hot spot.
		Rotate180(result, result, c);
						// New hotspot is what used to be the
						//   upper-left corner.
		result.tx = (short)((result.tx + xtiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		result.ty = (short)((result.ty + ytiles + EConst.c_num_tiles)%EConst.c_num_tiles);
	}
	private void swapDims() {
		int tmp = xtiles;		// Swap dims.
		xtiles = ytiles;
		ytiles = tmp;
	}
	private void setCenter() {
		getTile(center);
		center.tx = (short)((center.tx - xtiles/2 + EConst.c_num_tiles)%EConst.c_num_tiles);
		center.ty = (short)((center.ty - ytiles/2 + EConst.c_num_tiles)%EConst.c_num_tiles);
	}
	private boolean okayToRotate(Tile pos) {
		int lift = getLift();
		// Special case for carpet.
		int move_type = (lift > 0) ? (EConst.MOVE_LEVITATE) : EConst.MOVE_ALL_TERRAIN;
						// Get footprint in tiles.
		Rectangle foot = getTileFootprint();
		int xts = xtiles, yts = ytiles;
						// Get where new footprint will be.
		newfoot.set(pos.tx - yts + 1, pos.ty - xts + 1, yts, xts);
		if (newfoot.y < foot.y) {		// Got a piece above the old one?
						// Check area.  (No dropping allowed.)
			pos.set(newfoot.x, newfoot.y, lift);
			if (!MapChunk.areaAvailable(newfoot.w, foot.y - newfoot.y, 4, pos,
					move_type, 0) || pos.tz != lift)
				return false;
		}
		if (foot.y + foot.h < newfoot.y + newfoot.h) {
						// A piece below old one.
			pos.set(newfoot.x, foot.y + foot.h, lift);
			if (!MapChunk.areaAvailable(newfoot.w, 
				newfoot.y + newfoot.h - (foot.y + foot.h), 4, pos,
					move_type, 0) || pos.tz != lift)
				return false;
		}
		if (newfoot.x < foot.x)	{	// Piece to the left?
			pos.set(newfoot.x, newfoot.y, lift);
			if (!MapChunk.areaAvailable(foot.x - newfoot.x, newfoot.h, 4, pos,
					move_type, 0) || pos.tz != lift)
				return false;
		}
		if (foot.x + foot.w < newfoot.x + newfoot.w)
						// Piece to the right.
			pos.set(foot.x + foot.w, newfoot.y, lift);
			if (!MapChunk.areaAvailable(
				newfoot.x + newfoot.w - (foot.x + foot.w), newfoot.h, 4, pos,
					move_type, 0) || pos.tz != lift)
				return false;
		return true;		
	}
	private void addDirty() {	
					// Get lower-right corner.
		gwin.getShapeLocation(loc, this);
		int w = xtiles*EConst.c_tilesize, h = ytiles*EConst.c_tilesize;
		dirty.set(loc.x - w, loc.y - h, w, h);
		final int barge_enlarge = (EConst.c_tilesize+EConst.c_tilesize/4);
		final int barge_stretch = (4*EConst.c_tilesize+EConst.c_tilesize/2);
		dirty.enlarge(barge_enlarge);		// Make it a bit bigger.
		if (dir%2 != 0) {			// Horizontal?  Stretch.
			dirty.x -= barge_enlarge/2; dirty.w += barge_stretch; 
		} else { 
			dirty.y -= barge_enlarge/2; dirty.h += barge_stretch; 
		}
		gwin.clipToWin(dirty);	// Intersect with screen.
		gwin.addDirty(dirty);
	}
				// Finish up move/rotate operation.
	private void finishMove(Tile positions[], int newmap) {
		setCenter();			// Update center.
		int cnt = objects.size();	// We'll move each object.
		for (int i = 0; i < cnt; i++) {	// Now add them back in new location.
			GameObject obj = getObject(i);
			if (i < permCount)	// Restore us as owner.
				obj.setOwner(this);
			obj.move(positions[i].tx, positions[i].ty, positions[i].tz, newmap);
			}
		// Check for scrolling.
		gwin.scrollIfNeeded(center);
	}
	
	public BargeObject(int shapenum, int framenum,
			int shapex, int shapey, int lft,
				int xt, int yt, int d) {
		super(shapenum, framenum, shapex, shapey, lft, 0);
		xtiles = xt; ytiles = yt; dir = d;
		firstStep = true;
		boat = -1;
		center = new Tile(); pos = new Tile();
		footprint = new Rectangle(); newfoot = new Rectangle();
		dirty = new Rectangle();
		loc = new Point();
		objects = new Vector<GameObject>();
	}
	public Rectangle getTileFootprint() {
		int tx = getTileX(), ty = getTileY(), tz = getLift();
		int xts = xtiles, yts = ytiles;
		footprint.set((tx - xts + 1 + EConst.c_num_tiles)%EConst.c_num_tiles, 
				(ty - yts + 1 + EConst.c_num_tiles)%EConst.c_num_tiles, xts, yts);
		return footprint;
	}
	public boolean isMoving() {
		return frameTime > 0;
	}
	public int getXtiles()		// Dims. in tiles.
		{ return xtiles; }
	public int getYtiles()
		{ return ytiles; }
	public void setToFather()		// Require 'gather' on next move.
		{ gathered = false; }
	public void gather() {			// Gather up objects on barge.
		if (gmap.getChunk(getCx(), getCy()) == null)
			return;			// Not set in world yet.
		iceRaft = false;		// We'll just detect it each time.
		objects.setSize(permCount);	// Start fresh.
						// Get footprint in tiles.
		Rectangle foot = getTileFootprint();
		int lift = getLift();		// How high we are.
						// Go through intersected chunks.
		MapChunk.ChunkIntersectIterator iter = 
						new MapChunk.ChunkIntersectIterator(foot);
		Rectangle tiles = new Rectangle();
		int cx, cy;
		MapChunk chunk;
		while ((chunk = iter.getNext(tiles)) != null) {
			tiles.x += chunk.getCx()*EConst.c_tiles_per_chunk;
			tiles.y += chunk.getCy()*EConst.c_tiles_per_chunk;
			GameObject obj;
			ObjectList.ObjectIterator next = new ObjectList.ObjectIterator(
														chunk.getObjects());
			while ((obj = next.next()) != null) {	// Look at each object.
				if (obj == this)
					continue;
				if (obj.isEgg()) // don't pick up eggs
					continue;
				Tile t = pos;
				obj.getTile(t);
				if (!tiles.hasPoint(t.tx, t.ty) ||
					obj.getOwner() == this)
					continue;
				ShapeInfo info = obj.getInfo();
						// Above barge, within 5-tiles up?
				boolean isbarge = info.isBargePart() /*+++ || !info.get_weight() */;
				if (t.tz + info.get3dHeight() > lift && 
				    ((isbarge && t.tz >= lift - 1) ||
					(t.tz < lift + 5 && t.tz >= lift /*+++ + 1 */))) {
					objects.add(obj);
					int btype = obj.getInfo().getBargeType();
					if (btype == ShapeInfo.barge_raft)
						iceRaft = true;
					else if (btype == ShapeInfo.barge_turtle)
						xtiles = 20;
				}
			}
		}
		setCenter();
						// Test for boat.
		chunk = gmap.getChunk(
			center.tx/EConst.c_tiles_per_chunk, center.ty/EConst.c_tiles_per_chunk);
		if (boat == -1 && chunk != null) {
			ChunkTerrain ter = chunk.getTerrain();
			int flatShape = ter.getShapeNum(center.tx%EConst.c_tiles_per_chunk,
					center.ty%EConst.c_tiles_per_chunk);
			
			/* ++++++ NEEDED? if (flat.is_invalid())
				boat = 0;
			else */ {
				ShapeInfo info = ShapeID.getInfo(flatShape);
				boat = info.isWater() ? 1 : 0;
			}
		}
		gathered = true;
	}
	/* ++++++FINISH
	public void faceDirection(int ndir);	// Face dir. (0-7).
					// Start rolling/sailing.
	public void travel_to_tile(Tile_coord dest, int speed);
	public void turn_right();		// Turn 90 degrees right.
	public void turn_left();
	public void turn_around();
	public void stop()			// Stop moving.
		{ frame_time = 0; first_step = true; }
	public void done();			// No longer being operated.
	public int okay_to_land();		// See if clear to land.
	public Barge_object *as_barge() { return this; }
	@Override				// Move to new abs. location.
	public void move(int newtx, int newty, int newlift, int newmap = -1);
	*/
					// Remove an object.
	@Override
	public void remove(GameObject obj) {
		obj.setOwner(null);
		obj.removeThis();		// Now remove from outside world.
	}
	@Override		// Add an object.
	public boolean add(GameObject obj, boolean dont_check,
							boolean combine, boolean noset) {
		objects.add(obj);		// Add to list.
		return (false);			// We want it added to the chunk.
	}
	public final boolean contains(GameObject obj) {
		return objects.contains(obj);
	}
	/*++++++++
	@Override			// Drop another onto this.
	public int drop(Game_object *obj);
	@Override				// Render.
	public void paint();
	@Override
	public void activate(int event = 1);
					// Step onto an (adjacent) tile.
	@Override
	public int step(Tile_coord t, int frame = -1, bool force = false);
	@Override				// Write out to IREG file.
	public void write_ireg(DataSource* out);
	@Override			// Get size of IREG. Returns -1 if can't write to buffer
	public int get_ireg_size();
	@Override
	public void elements_read();	// Called when all member items read.	
	*/
	@Override
	public void addedToQueue() {
		++timeQueueCount;
	}

	@Override
	public boolean alwaysHandle() {
		return false;
	}

	@Override
	public void handleEvent(int ctime, Object udata) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removedFromQueue() {
		--timeQueueCount;
	}

}
