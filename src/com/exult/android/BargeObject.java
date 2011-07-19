package com.exult.android;
import java.util.Vector;
import android.graphics.Point;
import java.io.OutputStream;
import java.io.IOException;

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
	//UNUSED private boolean complete;			// Flag:  all members have been read.
	private boolean gathered;			// Items on barge have been gathered.
	private boolean iceRaft;			// For Serpent Isle.
	private boolean firstStep;		// So first motion can just be 1 tile.
	private boolean taking2ndStep;		// Skip animation on 2nd step.
	private int boat;				// 1 if a boat, 0 if not; -1=untested.
	private int frameTime;			// Time between frames in msecs.  0 if
	//   not moving.
	private PathFinder path;		// For traveling.
	private Tile center;		// Center of barge.
	private Tile pos, eventPos;			// Temp.
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
	private Tile Rotate90r
		(
		Tile result,
		int xtiles, int ytiles,		// Object dimensions.
		Tile c			// Rotate around this.
		) {
		getTile(result);
						// Rotate hot spot.
		Rotate90r(result, result, c);
						// New hotspot is what used to be the
						//   upper-right corner.
		result.tx = (short)((result.tx + ytiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		result.ty = (short)((result.ty + EConst.c_num_tiles)%EConst.c_num_tiles);
		return result;
	}

	/*
	 *	Figure tile where an object will be if it's rotated 90 degrees around
	 *	a point, assuming its 'hot spot' is at its lower-right corner.
	 */

	private Tile Rotate90l
		(
		Tile result,
		int xtiles, int ytiles,		// Object dimensions.
		Tile c			// Rotate around this.
		) {
		getTile(result);
						// Rotate hot spot.
		Rotate90l(result, result, c);
						// New hot-spot is old lower-left.
		result.ty = (short)((result.ty + xtiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		result.tx = (short)((result.tx + EConst.c_num_tiles)%EConst.c_num_tiles);
		return result;
	}
	/*
	 *	Figure tile where an object will be if it's rotated 180 degrees around
	 *	a point, assuming its 'hot spot' is at its lower-right corner.
	 */
	private Tile Rotate180
		(
		Tile result,
		int xtiles, int ytiles,		// Object dimensions.
		Tile c			// Rotate around this.
		) {
		getTile(result);
						// Rotate hot spot.
		Rotate180(result, result, c);
						// New hotspot is what used to be the
						//   upper-left corner.
		result.tx = (short)((result.tx + xtiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		result.ty = (short)((result.ty + ytiles + EConst.c_num_tiles)%EConst.c_num_tiles);
		return result;
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
		center = new Tile(); pos = new Tile(); eventPos = new Tile();
		footprint = new Rectangle(); newfoot = new Rectangle();
		dirty = new Rectangle();
		loc = new Point();
		objects = new Vector<GameObject>();
	}
	public Rectangle getTileFootprint() {
		int tx = getTileX(), ty = getTileY();
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
	public Tile getCenter() {
		return center;		// DON'T MODIFY this!!
	}
	public void setToGather()		// Require 'gather' on next move.
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
				boolean isbarge = info.isBargePart();
				if (t.tz + info.get3dHeight() > lift && 
				    ((isbarge && t.tz >= lift - 1) ||
					(t.tz < lift + 5 && t.tz >= lift))) {
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
			ShapeInfo info = ShapeID.getInfo(flatShape);
			boat = info.isWater() ? 1 : 0;
		}
		gathered = true;
	}
	public void faceDirection(int ndir) {	// Face dir. (0-7).
		ndir /= 2;			// Convert to 0-3.
		switch ((4 + ndir - dir)%4) {
		case 1:				// Rotate 90 degrees right.
			turnRight();
			break;
		case 2:
			turnAround();		// 180 degrees.
			break;
		case 3:
			turnLeft();
			break;
		default:
			break;
		}
	}
					// Start rolling/sailing.
	public void travelToTile(Tile dest, int speed) {
		if (path == null)
			path = new ZombiePathFinder();
		Tile t = new Tile();
		getTile(t);
						// Set up new path.
		if (path.NewPath(t, dest)) {
			frameTime = speed;
						// Figure new direction.
			int curtx = getTileX(), curty = getTileY();
			int dy = Tile.delta(curty, dest.ty),
			    dx = Tile.delta(curtx, dest.tx);
			int ndir = EUtil.getDirection4(-dy, dx);
			if (!iceRaft)		// Ice-raft doesn't rotate.
				faceDirection(ndir);
			if (!inQueue())	// Not already in queue?
				tqueue.add(TimeQueue.ticks, this, null);
		} else
			frameTime = 0;		// Not moving.
	}
	public void turnRight() {		// Turn 90 degrees right.
		addDirty();			// Want to repaint old position.
		// Move the barge itself.
		Rotate90r(pos, xtiles, ytiles, center);
		if (!okayToRotate(pos))	// Check for blockage.
			return;
		super.move(pos.tx, pos.ty, pos.tz);
		swapDims();			// Exchange xtiles, ytiles.
		dir = (dir + 1)%4;		// Increment direction.
		int cnt = objects.size();	// We'll move each object.
						// But 1st, remove & save new pos.
		Tile positions[] = new Tile[cnt];
		for (int i = 0; i < cnt; i++) {
			GameObject obj = getObject(i);
			int frame = obj.getFrameNum();
			ShapeInfo info = obj.getInfo();
			
			positions[i] = Rotate90r(new Tile(), info.get3dXtiles(frame),
						info.get3dYtiles(frame), center);
			obj.removeThis();	// Remove object from world.
						// Set to rotated frame.
			obj.setFrame(obj.getRotatedFrame(1));
			obj.setInvalid();	// So it gets added back right.
		}
		finishMove(positions, -1);		// Add back & del. positions.	
	}
	public void turnLeft() {
		addDirty();			// Want to repaint old position.
		// Move the barge itself.
		Rotate90l(pos, xtiles, ytiles, center);
		if (!okayToRotate(pos))	// Check for blockage.
			return;
		super.move(pos.tx, pos.ty, pos.tz);
		swapDims();			// Exchange xtiles, ytiles.
		dir = (dir + 3)%4;		// Increment direction.
		int cnt = objects.size();	// We'll move each object.
						// But 1st, remove & save new pos.
		Tile positions[] = new Tile[cnt];
		for (int i = 0; i < cnt; i++) {
			GameObject obj = getObject(i);
			int frame = obj.getFrameNum();
			ShapeInfo info = obj.getInfo();
			positions[i] = Rotate90l(new Tile(), info.get3dXtiles(frame),
						info.get3dYtiles(frame), center);
			obj.removeThis();	// Remove object from world.
						// Set to rotated frame.
			obj.setFrame(obj.getRotatedFrame(3));
			obj.setInvalid();	// So it gets added back right.
		}
		finishMove(positions, -1);		// Add back & del. positions.
	}
	public void turnAround() {
		addDirty();			// Want to repaint old position.
		// Move the barge itself.
		Rotate180(pos, xtiles, ytiles, center);
		super.move(pos.tx, pos.ty, pos.tz);
		dir = (dir + 2)%4;		// Increment direction.
		int cnt = objects.size();	// We'll move each object.
						// But 1st, remove & save new pos.
		Tile positions[] = new Tile[cnt];
		for (int i = 0; i < cnt; i++) {
			GameObject obj = getObject(i);
			int frame = obj.getFrameNum();
			ShapeInfo info = obj.getInfo();
			positions[i] = Rotate180(new Tile(), info.get3dXtiles(frame),
						info.get3dYtiles(frame), center);
			obj.removeThis();	// Remove object from world.
						// Set to rotated frame.
			obj.setFrame(obj.getRotatedFrame(2));
			obj.setInvalid();	// So it gets added back right.
		}
		finishMove(positions, -1);		// Add back & del. positions.
	}
	public void stop()			// Stop moving.
		{ frameTime = 0; firstStep = true; }
	/*
	 *	Ending 'barge mode'.
	 */
	private static int norecurse = 0;// Don't recurse on the code below.
	public void done() {			// No longer being operated.
		gathered = false;		// Clear for next time. (needed for SI turtle)
		if (norecurse > 0)
			return;
		norecurse++;
		if (boat == 1) {			// Look for sails on boat.
						// Pretend they were clicked on.
			int cnt = objects.size();	// Look for open sail.
			for (int i = 0; i < cnt; i++) {
				GameObject obj = objects.elementAt(i);
				if (obj.getInfo().getBargeType() == ShapeInfo.barge_sails &&
					    (obj.getFrameNum()&7) < 4) {
					obj.activate();
					break;
				}
		}			}
		norecurse--;
	}
	public boolean okayToLand() {		// See if clear to land.
		Rectangle foot = getTileFootprint();
		int lift = getLift();		// How high we are.
						// Go through intersected chunks.
		MapChunk.ChunkIntersectIterator iter = 
						new MapChunk.ChunkIntersectIterator(foot);
		Rectangle tiles = newfoot;
		MapChunk chunk;
		while ((chunk = iter.getNext(tiles)) != null) {		// Check each tile.
			for (int ty = tiles.y; ty < tiles.y + tiles.h; ty++)
				for (int tx = tiles.x; tx < tiles.x + tiles.w; tx++)
					if (chunk.getHighestBlocked(lift, tx, ty)
									!= -1)
						return false;
		}
		return true;
	}
	@Override
	public BargeObject asBarge() { return this; }
	@Override				// Move to new abs. location.
	public void move(int newtx, int newty, int newlift, int newmap) {
		if (chunk == null) {			// Not currently on map?
			// UNTIL drag-n-drop does the gather properly.
			super.move(newtx, newty, newlift, newmap);
			return;
		}
		if (!gathered)			// Happens in SI with turtle.
			gather();
						// Want to repaint old position.
		addDirty();
						// Get current location.
		Tile old = pos;
		if (newmap == -1) newmap = getMapNum();
						// Move the barge itself.
		super.move(newtx, newty, newlift, newmap);
						// Get deltas.
		int dx = newtx - old.tx, dy = newty - old.ty, dz = newlift - old.tz;
		int cnt = objects.size();	// We'll move each object.
						// But 1st, remove & save new pos.
		Tile positions[] = new Tile[cnt];
		int i;
		for (i = 0; i < cnt; i++) {
			GameObject obj = getObject(i);
			Tile ot = new Tile();
			obj.getTile(ot);
						// Watch for world-wrapping.
			positions[i] = ot;
			ot.set((ot.tx + dx + EConst.c_num_tiles)%EConst.c_num_tiles,
					(ot.ty + dy + EConst.c_num_tiles)%EConst.c_num_tiles, 
					ot.tz + dz);
			obj.removeThis();	// Remove object from world.
			obj.setInvalid();	// So it gets added back right.
			if (!taking2ndStep)
				{		// Animate a few shapes.
				int frame = obj.getFrameNum();
				switch (obj.getInfo().getBargeType()) {
				case ShapeInfo.barge_wheel:		// Cart wheel.
					obj.setFrame(((frame + 1)&3)|(frame&32));
					break;
				case ShapeInfo.barge_draftanimal:		// Draft horse.
					obj.setFrame(((frame + 4)&15)|(frame&32));
					break;
				}
			}
		}
		finishMove(positions, newmap);	// Add back & del. positions.
	}
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
	@Override			// Drop another onto this.
	public boolean drop(GameObject obj) {
		return false;
	}
	@Override				// Render.
	public void paint() {
		// DON'T paint barge shape itself.
		// The objects are in the chunk too.
		if(gwin.paintEggs) {
			super.paint();
			byte pix = ShapeID.getSpecialPixel(ShapeID.CURSED_PIXEL);
			int lx, ty;	// Left, top.
			gwin.getShapeLocation(loc, this);
			lx = loc.x - xtiles*EConst.c_tilesize + 1;
			ty = loc.y - ytiles*EConst.c_tilesize + 1;
						// Little square at lower-right.
			gwin.getWin().fill8(pix, 4, 4, loc.x-2, loc.y-2);
						// Little square at top.
			gwin.getWin().fill8(pix, 4, 4, lx-1, ty-1);
						// Horiz. line along top, bottom.
			gwin.getWin().fill8(pix, xtiles*EConst.c_tilesize, 1, lx, ty);
			gwin.getWin().fill8(pix, xtiles*EConst.c_tilesize, 1, lx, loc.y);
						// Vert. line to left, right.
			gwin.getWin().fill8(pix, 1, ytiles*EConst.c_tilesize, lx, ty);
			gwin.getWin().fill8(pix, 1, ytiles*EConst.c_tilesize, loc.x, ty);
			}
	}
	@Override
	public void activate(int event) {
	}
	/*
	 *	Step onto an adjacent tile.
	 *
	 *	Output:	0 if blocked.
	 *		Dormant is set if off screen.
	 */
	@Override
	public boolean step(Tile t, int frame, boolean force) {
		if (!gathered)			// Happens in SI with turtle.
			gather();
		getTile(pos);
						// Blocked? (Assume ht.=4, for now.)
		int move_type;
		if (pos.tz > 0)
			move_type = EConst.MOVE_LEVITATE;
		else if (force)
			move_type = EConst.MOVE_ALL;
		else if (boat == 1) 
			move_type = EConst.MOVE_SWIM;
		else
			move_type = EConst.MOVE_WALK;
						// No rising/dropping.
		if (!MapChunk.areaAvailable(getXtiles(), getYtiles(), 
							4, pos, t, move_type, 0, 0))
			return false;		// Done.
		move(t.tx, t.ty, t.tz);		// Move it & its objects.
						// Near an egg?
		MapChunk nlist = gmap.getChunk(getCx(), getCy());
		nlist.activateEggs(gwin.getMainActor(), t.tx, t.ty, t.tz, 
							pos.tx, pos.ty, false);
		return true;			// Add back to queue for next time.
	}
	@Override				// Write out to IREG file.
	public void writeIreg(OutputStream out) throws IOException {
		byte buf[] = new byte[20];		// 13-byte entry + length-byte.
		int ind = writeCommonIreg(12, buf);
						// Write size.
		buf[ind++] = (byte)xtiles;
		buf[ind++] = (byte)ytiles;
		buf[ind++] = 0;			// Unknown.
						// Flags (quality).  Taking B3 to in-
						//   dicate barge mode.
		buf[ind++] = (byte)((dir<<1) | (((gwin.getMovingBarge() == this)?1:0)<<3));
		buf[ind++] = 0;			// (Quantity).
		buf[ind++] = (byte)(((int)getLift()&15)<<4);
		buf[ind++] = 0;			// Data2.
		buf[ind++] = 0;			// 
		out.write(buf, 0, ind);
						// Write permanent objects.
		for (int i = 0; i < permCount; i++) {
			GameObject obj = getObject(i);
			obj.writeIreg(out);
		}
		out.write(0x01);			// A 01 terminates the list.
						// Write scheduled usecode.
		GameMap.writeScheduled(out, this, false);	
	}
	@Override			// Get size of IREG. Returns -1 if can't write to buffer
	public int getIregSize() {
		// These shouldn't ever happen, but you never know
		if (gwin.getMovingBarge() == this || UsecodeScript.find(this) != null)
			return -1;
		int total_size = 8 + getCommonIregSize();

		for (int i = 0; i < permCount; i++) {
			GameObject obj = getObject(i);
			int size = obj.getIregSize();
			if (size < 0) return -1;
			total_size += size;
		}
		total_size += 1;
		return total_size;
	}
	@Override
	public void elementsRead() {	// Called when all member items read.	
		permCount = 0;			// So we don't get haystack!
		//UNUSED complete = true;
	}
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
		if (path == null || frameTime == 0 || gwin.getMovingBarge() != this)
			return;			// We shouldn't be doing anything.
						// Get spot & walk there.	
						// Take two steps for speed.
		if (!path.getNextStep(eventPos) || !step(eventPos, -1, false))
			frameTime = 0;
		else if (!firstStep) {		// But not when just starting.
			taking2ndStep = true;
			if (!path.getNextStep(eventPos) || !step(eventPos, -1, false))
				frameTime = 0;
			taking2ndStep = false;
		}
		if (frameTime > 0)			// Still good?
			tqueue.add(ctime + frameTime, this, udata);
		firstStep = false;		// After 1st, move 2 at a time.
	}
	@Override
	public void removedFromQueue() {
		--timeQueueCount;
	}
	public boolean inQueue() {
		return timeQueueCount > 0;
	}
}
