package com.exult.android;
import java.util.Vector;
import java.util.Arrays;
import java.util.HashSet;

public final class MapChunk extends GameSingletons {
	private GameMap map;				// Map we're a part of.
	private ChunkTerrain terrain;		// Flat landscape tiles.
	private ObjectList objects;			// -'Flat'  obs. (lift=0,ht=0) stored 1st.
	private GameObject firstNonflat;			// .first nonflat in 'objects'.
	// Counts of overlapping objects from chunks below, to right.
	private short fromBelow, fromRight, fromBelowRight;
	private byte dungeonLevels[];	// A 'dungeon' level value for each tile (4 bit).
	private boolean roof;			// 1 if a roof present.
	// # light sources in chunk.
	private byte dungeonLights, nonDungeonLights;
	private short cx, cy;
	// Data that used to be in Chunk_cache:
	// Each member of 'blocked' is 16x16 tiles, with each short value using 2
	// bits for each bit level for #objs blocking there.
	private Vector<short[]> blocked;	// Each element represents the chunk for 8 lifts.
	private HashSet<GameObject> doors;
	private Vector<EggObject> eggObjects;
	private short eggs[];				// Eggs which influence this chunk.
	// Mask gives low bits (b0) for a given # of ztiles:
	private static final int tmasks[] = {
		0x0,
        0x1,
        0x5,
       0x15,
       0x55,
      0x155,
      0x555,
     0x1555};
	private static Rectangle footRect = new Rectangle(),	// Temp.
							 tilesRect = new Rectangle(),
							 eggRect = new Rectangle();
	private static Tile spotPos = new Tile();	// Temp.
	// Temp. storage for 'blocked' flags for a single tile.
	private static int tflags[] = new int[256/8];
	private static int tflagsMaxz;
	public MapChunk(GameMap m, int chx, int chy) {
		map = m;
		cx = (short)chx;
		cy = (short)chy;
		terrain = null;
		objects = new ObjectList();
	}
	public final int getCx() {
		return cx;
	}
	public final int getCy() {
		return cy;
	}
	public final GameMap getMap() {
		return map;
	}
	public final ObjectList getObjects() {
		return objects;
	}
	public final ObjectList.FlatObjectIterator getFlatObjectIterator() {
		return objects.getFlatIterator(firstNonflat);
	}
	public final ObjectList.NonflatObjectIterator getNonflatObjectIterator() {
		return objects.getNonflatIterator(firstNonflat);
	}
	public final void setTerrain(ChunkTerrain ter) {
		if (terrain != null) {
			terrain.removeClient();
			// ++++++++REMOVE OBJS.?
		}
		terrain = ter;
		terrain.addClient();	
		// Get RLE objects in chunk.
		ShapeID id = new ShapeID();
		for (int tiley = 0; tiley < EConst.c_tiles_per_chunk; tiley++)
			for (int tilex = 0; tilex < EConst.c_tiles_per_chunk; tilex++) {
				int shapenum = ter.getShapeNum(tilex, tiley);
				if (shapenum >= EConst.c_first_obj_shape) {
					ShapeInfo info = ShapesVgaFile.getInfo(shapenum);
					int framenum = ter.getFrameNum(tilex, tiley);
					GameObject obj = /* +++++++ info.is_animated() ?
						new Animated_object(shapenum,
						    	framenum, tilex, tiley)
						: */ new TerrainGameObject(shapenum,
						    	framenum, tilex, tiley, 0);
					add(obj);
				}
			}
	}
	/*
	 *	Add rendering dependencies for a new object.
	 */
	private void addDependencies(GameObject newobj, GameObject.OrderingInfo newinfo) {
		GameObject obj;		// Figure dependencies.
		ObjectList.NonflatObjectIterator next = objects.getNonflatIterator(firstNonflat);
		while ((obj = next.next()) != null) {
			/* Compare returns -1 if lt, 0 if dont_care, 1 if gt. */
			int newcmp = GameObject.compare(newinfo, obj);
			int cmp = newcmp == -1 ? 1 : newcmp == 1 ? 0 : -1;
			if (cmp == 0) {		// Bigger than this object?
				newobj.addDependency(obj);
				obj.addDependor(newobj);
			} else if (cmp == 1) {	// Smaller than?
				obj.addDependency(newobj);
				newobj.addDependor(obj);
			}
		}
	}
	/*
	 *	Add rendering dependencies for a new object to another chunk.
	 *	NOTE:  This is static.
	 *
	 *	Output:	.chunk that was checked.
	 */

	private MapChunk addOutsideDependencies
		(
		int cx, int cy,			// Chunk to check.
		GameObject newobj,		// Object to add.
		GameObject.OrderingInfo newinfo		// Info. for new object's ordering.
		)
		{
		MapChunk chunk = gwin.getMap().getChunk(cx, cy);
		chunk.addDependencies(newobj, newinfo);
		return chunk;
		}

	public void add(GameObject newobj) {
		newobj.setChunk(this);
		GameObject.OrderingInfo ord = newobj.getOrderingInfo1();
		if (firstNonflat != null)
			objects.insertBefore(newobj, firstNonflat);
		else
			objects.append(newobj);
		//+++++++FINISH - Lots of code involving sorting objects, etc.
		if (newobj.getLift() > 0 || ord.info.get3dHeight() > 0) {
					// Deal with dependencies.
					// First this chunk.
			addDependencies(newobj, ord);
			if (fromBelow > 0)		// Overlaps from below?
				addOutsideDependencies(cx, EConst.INCR_CHUNK(cy), newobj, ord);
			if (fromRight > 0)		// Overlaps from right?
				addOutsideDependencies(EConst.INCR_CHUNK(cx), cy, newobj, ord);
			if (fromBelowRight > 0)
				addOutsideDependencies(EConst.INCR_CHUNK(cx), 
					EConst.INCR_CHUNK(cy), newobj, ord);
					// See if newobj extends outside.
			/* Let's try boundary. YES.  This helps with statues through roofs!*/
			boolean ext_left = (newobj.getTx() - ord.xs) < 0 && cx > 0;
			boolean ext_above = (newobj.getTy() - ord.ys) < 0 && cy > 0;
			if (ext_left) {
				addOutsideDependencies(EConst.DECR_CHUNK(cx), cy, 
						newobj, ord).fromRight++;
				if (ext_above)
					addOutsideDependencies(EConst.DECR_CHUNK(cx), 
							 EConst.DECR_CHUNK(cy), newobj, ord).fromBelowRight++;
			}
			if (ext_above)
				addOutsideDependencies(cx, EConst.DECR_CHUNK(cy),
					newobj, ord).fromBelow++;
			firstNonflat = newobj;	// Inserted before old first_nonflat.
		}
		addObjectBlocking(newobj);
		/* +++++++++FINISH
		if (ord.info.is_light_source())	// Count light sources.
			{
			if (dungeon_levels && is_dungeon(newobj.get_tx(),
							newobj.get_ty()))
				dungeon_lights++;
			else
				non_dungeon_lights++;
			}
		 */
		if (newobj.getLift() >= 5) {	// Looks like a roof?
			if (ord.info.getShapeClass() == ShapeInfo.building)
				roof = true;
		}
	}
	public void remove(GameObject remove) {
		removeObjectBlocking(remove);
		remove.clearDependencies();	// Remove all dependencies.
		ShapeInfo info = remove.getInfo();
						// See if it extends outside.
		int frame = remove.getFrameNum(), tx = remove.getTx(),
						ty = remove.getTy();
		/* Let's try boundary. YES.  Helps with statues through roofs. */
		boolean ext_left = (tx - info.get3dXtiles(frame)) < 0 && cx > 0;
		boolean ext_above = (ty - info.get3dYtiles(frame)) < 0 && cy > 0;
		if (ext_left) {
			gmap.getChunk(cx - 1, cy).fromBelowRight--;
			if (ext_above)
				gmap.getChunk(cx - 1, cy - 1).fromBelowRight--;
		}
		if (ext_above)
			gmap.getChunk(cx, cy - 1).fromBelow--;
		if (info.isLightSource()) {	// Count light sources.
			if (dungeonLevels != null /* && +++++ is_dungeon(tx, ty) */)
				dungeonLights--;
			else
				nonDungeonLights--;
		}
		if (remove == firstNonflat)	{ // First nonflat?
			firstNonflat = remove.getNext();
			if (firstNonflat == objects.getFirst())
				firstNonflat = null;
			}
		objects.remove(remove);		// Remove from list.
		remove.setInvalid();		// No longer part of world.
	}
	
	public void activateEggs(GameObject obj, int tx, int ty, int tz, 
					int from_tx, int from_ty, boolean now) {
		if (eggs == null)
			return;
		int eggbits = ((int)eggs[
			(ty%EConst.c_tiles_per_chunk)*EConst.c_tiles_per_chunk + 
						(tx%EConst.c_tiles_per_chunk)]&0xffff);
		if (eggbits == 0)
			return;
		int i;				// Go through eggs.
		for (i = 0; i < 15 && eggbits != 0; i++, eggbits = (eggbits >> 1)) {
			EggObject egg;
			if ((eggbits&1) != 0 && i < eggObjects.size() &&
			    (egg = eggObjects.elementAt(i)) != null &&
			    !egg.isDormant() &&
			    egg.isActive(obj, tx, ty, tz, from_tx, from_ty)) {
				egg.hatch(obj, now);
				/*++++++STILL NEEDED?
				if (chunk.get_cache() != this)
					return;	// A teleport could have deleted us!
				*/
			}
		}
		if (eggbits != 0) {			// Check 15th bit.
						// DON'T use an iterator here, since
						//   the list can change as eggs are
						//   activated, causing a CRASH!
			int sz = eggObjects.size();
			for (  ; i < sz; i++) {
				EggObject egg = eggObjects.elementAt(i);
				if (egg != null && egg.isActive(obj, tx, ty, tz, from_tx, from_ty)) {
					egg.hatch(obj, now);
					/* ++++++NEEDED?
					if (chunk.get_cache() != this)
						return;	// A teleport could have deleted us!	
					*/		
				}
			}
		}
	}
	private void setEgged(EggObject egg, Rectangle tiles, boolean add) {
		// Egg already there?
		int eggnum = -1, spot = -1;
		if (eggObjects == null) {
			if (!add)
				return;
			eggObjects = new Vector<EggObject>(4);
		}
		int cnt = eggObjects.size();
		for (int i = 0; i < cnt; ++i) {
			EggObject e = eggObjects.elementAt(i);
			if (e == egg) {
				eggnum = i;;
				break;
			} else if (e == null && spot == -1)
				spot = i;
		}
		if (add) {
			if (eggs == null)
				eggs = new short[256];
			if (eggnum < 0) {		// No, so add it.
				eggnum = spot >= 0 ? spot : eggObjects.size();
				if (spot >= 0)
					eggObjects.setElementAt(egg, spot);
				else
					eggObjects.add(egg);
			}
			if (eggnum > 15)	// We only have 16 bits.
				eggnum = 15;
			short mask = (short)(1<<eggnum);
			int stopx = tiles.x + tiles.w, stopy = tiles.y + tiles.h;
			for (int ty = tiles.y; ty < stopy; ++ty)
				for (int tx = tiles.x; tx < stopx; ++tx)
					eggs[ty*EConst.c_tiles_per_chunk + tx] |= mask;
		} else {			// Remove.
			if (eggnum < 0)
				return;		// Not there.
			eggObjects.setElementAt(null, eggnum);
			if (eggnum >= 15) {	// We only have 16 bits.
								// Last one at 15 or above?
				for (int i = 15; i < cnt; ++i) {
					EggObject e = eggObjects.elementAt(i);
					if (e != null)
						// No, so leave bits alone.
						return;
				}
				eggnum = 15;
			}
			short mask = (short)(~(1<<eggnum));
			int stopx = tiles.x + tiles.w, stopy = tiles.y + tiles.h;
			for (int ty = tiles.y; ty < stopy; ty++)
				for (int tx = tiles.x; tx < stopx; tx++)
					eggs[ty*EConst.c_tiles_per_chunk + tx] &= mask;
		}
	}
	private void updateEgg(EggObject egg, boolean add) {
		// Get footprint with abs. tiles.
		Rectangle foot = egg.getArea();
		if (foot.w == 0)
			return;			// Empty (probability = 0).
		MapChunk chunk;
		ChunkIntersectIterator iter = new ChunkIntersectIterator();
		if (egg.isSolidArea()) {
						// Do solid rectangle.
			iter.set(foot);
			while ((chunk = iter.getNext(eggRect)) != null)
				chunk.setEgged(egg, eggRect, add);
			return;
			}
						// Just do the perimeter.
						// Go through intersected chunks.
		eggRect.set(foot.x, foot.y, foot.w, 1);		// Top.
		iter.set(eggRect);
		while ((chunk = iter.getNext(eggRect)) != null)
			chunk.setEgged(egg, eggRect, add);
		eggRect.set(foot.x, foot.y + foot.h - 1, foot.w, 1);		// Bottom.
		iter.set(eggRect);
		while ((chunk = iter.getNext(eggRect)) != null)
			chunk.setEgged(egg, eggRect, add);
		eggRect.set(foot.x, foot.y + 1, 1, foot.h - 2);		// Left
		iter.set(eggRect);
		while ((chunk = iter.getNext(eggRect)) != null)
			chunk.setEgged(egg, eggRect, add);
		eggRect.set(foot.x + foot.w - 1, foot.y + 1, 1, foot.h - 2);		// Right.
		iter.set(eggRect);
		while ((chunk = iter.getNext(eggRect)) != null)
			chunk.setEgged(egg, eggRect, add);
	}
	public void addEgg(EggObject egg) {
		add(egg);
		egg.setArea();
		updateEgg(egg, true);
	}
	public void removeEgg(EggObject egg) {
		remove(egg);			// Remove it normally.
		updateEgg(egg, false);
	}
	public ImageBuf getRenderedFlats() {
		return terrain != null ? terrain.getRenderedFlats() : null;
	}
	/*
	 *	Set (actually, increment count) for a given tile.
	 *	Want:	00 => 01,	01 => 10,
	 *		10 => 11,	11 => 11.
	 *	So:	newb0 = !b0 OR b1,
	 *		newb1 =  b1 OR b0
	 */
	private void setBlockedTile
		(
		short blocked[],		// 16x16 flags,
		int tx, int ty,			// Tile #'s (0-15).
		int lift,			// Starting lift to set.
		int ztiles			// # tiles along z-axis.
		) {
		int ind = ty*EConst.c_tiles_per_chunk + tx;
		int val = blocked[ind]&0xffff;
							// Get mask for the bit0's:
		int mask0 = (tmasks[ztiles]<<(2*lift))&0xffff;
		int mask1 = mask0<<1;	// Mask for the bit1's.
		int val0s = val&mask0;
		int Nval0s = (~val)&mask0;
		int val1s = val&mask1;
		int newval = val1s | (val0s<<1) | Nval0s | (val1s>>1);
							// Replace old values with new.
		blocked[ind] = (short)((val&~(mask0|mask1)) | newval);
	}
	/*
	 *	Clear (actually, decrement count) for a given tile.
	 *	Want:	00 => 00,	01 => 00,
	 *		10 => 01,	11 => 10.
	 *	So:	newb0 =  b1 AND !b0
	 *		newb1 =  b1 AND  b0
	 */
	private void clearBlockedTile
		(
		short blocked[],		// 16x16 flags,
		int tx, int ty,			// Tile #'s (0-15).
		int lift,			// Starting lift to set.
		int ztiles			// # tiles along z-axis.
		)
		{
		int ind = ty*EConst.c_tiles_per_chunk + tx;
		int val = blocked[ind]&0xffff;
						// Get mask for the bit0's:
		int mask0 = (tmasks[ztiles]<<(2*lift))&0xffff;
		int mask1 = mask0<<1;	// Mask for the bit1's.
		int val0s = val&mask0;
		int Nval0s = (~val)&mask0;
		int val1s = val&mask1;
		int newval = (val1s & (val0s<<1)) | ((val1s>>1) & Nval0s);
						// Replace old values with new.
		blocked[ind] = (short)((val&~(mask0|mask1)) | newval);
	}
	/*
	 *	Create new blocked flags for a given z-level, where each level
	 *	covers 8 lifts.
	 */
	private short[] newBlockedLevel
		(
		int zlevel
		) {
		if (zlevel >= blocked.size())
			blocked.setSize(zlevel + 1);
		short[] block = new short[256];
		blocked.setElementAt(block, zlevel);
//		std::cout << "***Creating block for level " << zlevel << ", cache = " 
//			<< (void*)this << std::endl;
		return block;
	}
	private short[] needBlockedLevel(int zlevel) {
		if (blocked == null)
			blocked = new Vector<short[]>();
		if (zlevel < blocked.size()) {
			short[] block = blocked.elementAt(zlevel);
			if (block != null)
				return block;
		}
		return newBlockedLevel(zlevel);
	}
	/*
	 *	Set/unset the blocked flags in a region.
	 */
	private void setBlocked
		(
		int startx, int starty,		// Starting tile #'s.
		int endx, int endy,		// Ending tile #'s.
		int lift, int ztiles		// Lift, height info.
		) {
		int z = lift;
		while (ztiles > 0) {
			int zlevel = z/8, thisz = z%8, zcnt = 8 - thisz;
			if (ztiles < zcnt)
				zcnt = ztiles;
			short block[] = needBlockedLevel(zlevel);
			for (int y = starty; y <= endy; y++)
				for (int x = startx; x <= endx; x++)
					setBlockedTile(block, x, y, thisz, zcnt);
			z += zcnt;
			ztiles -= zcnt;
		}
	}
	private void clearBlocked
		(
		int startx, int starty,		// Starting tile #'s.
		int endx, int endy,		// Ending tile #'s.
		int lift, int ztiles		// Lift, height info.
		) {
		int z = lift;
		while (ztiles > 0) {
			int zlevel = z/8;
			int thisz = z%8, zcnt = 8 - thisz;
			if (zlevel >= blocked.size())
				break;		// All done.
			if (ztiles < zcnt)
				zcnt = ztiles;
			short block[] = blocked.elementAt(zlevel);
			if (block != null) {
				for (int y = starty; y <= endy; y++)
					for (int x = startx; x <= endx; x++)
						clearBlockedTile(block,x, y, thisz, zcnt);
			}
			z += zcnt;
			ztiles -= zcnt;
		}
	}
	// Process 'blocked' for an object that's added.
	private void addObjectBlocking(GameObject obj) {
		ShapeInfo info = obj.getInfo();
		if (info.isDoor()) {		// Special door list.
			if (doors == null)
				doors = new HashSet<GameObject>();
			doors.add(obj);
		}
		int ztiles = info.get3dHeight(); 
		if (ztiles == 0 || !info.isSolid())
			return;			// Skip if not an obstacle.
		obj.getFootprint(footRect);
		int endx = obj.getTx();	// Lower-right corner of obj.
		int endy = obj.getTy();
		int lift = obj.getLift();
		// Simple case.
		if (footRect.w == 1 && footRect.h == 1 && ztiles <= 8-lift%8) {
			setBlockedTile(needBlockedLevel(lift/8), 
					endx, endy, lift%8, ztiles);
			return;
		}				// Go through intersected chunks.
		ChunkIntersectIterator next_chunk = 
							new ChunkIntersectIterator(footRect);
		MapChunk chunk;
		while ((chunk = next_chunk.getNext(tilesRect)) != null)
			chunk.setBlocked(tilesRect.x, tilesRect.y, 
				tilesRect.x + tilesRect.w - 1, tilesRect.y + tilesRect.h - 1, 
				lift, ztiles);
	}
	// Process 'blocked' for an object that's removed.
	private void removeObjectBlocking(GameObject obj) {
		ShapeInfo info = obj.getInfo();
		if (info.isDoor())		// Special door list.
			doors.remove(obj);
		int ztiles = info.get3dHeight(); 
		if (ztiles == 0 || !info.isSolid())
			return;			// Skip if not an obstacle.
		obj.getFootprint(footRect);
		int endx = obj.getTx();	// Lower-right corner of obj.
		int endy = obj.getTy();
		int lift = obj.getLift();
		// Simple case.
		if (footRect.w == 1 && footRect.h == 1 && ztiles <= 8-lift%8) {
			short[] block = blocked.elementAt(lift/8);
			if (block != null)
				clearBlockedTile(block, endx, endy, lift%8, ztiles);
			return;
		}				// Go through intersected chunks.
		ChunkIntersectIterator next_chunk = 
							new ChunkIntersectIterator(footRect);
		MapChunk chunk;
		while ((chunk = next_chunk.getNext(tilesRect)) != null)
			chunk.clearBlocked(tilesRect.x, tilesRect.y, 
				tilesRect.x + tilesRect.w - 1, tilesRect.y + tilesRect.h - 1, 
				lift, ztiles);
	}
	private void setTflags(int tx, int ty, int maxz) {
		int zlevel = maxz/8, bsize = blocked == null ? 0 : blocked.size();
		if (zlevel >= bsize) {
			Arrays.fill(tflags, bsize, zlevel + 1, 0);
			zlevel = bsize - 1;
			}
		while (zlevel >= 0) {
			short block[] = blocked.elementAt(zlevel);
			tflags[zlevel--] = block != null 
						? block[ty*EConst.c_tiles_per_chunk + tx] : 0;
		}
		tflagsMaxz = maxz;
	}
	//	Test for given z-coord. (lift)
	private static boolean testTflags(int i) {
		return (tflags[(i)/8] & (3<<(2*((i)%8)))) != 0;
	}
	/*
	 *	Get highest blocked lift below a given level for a given tile.
	 *
	 *	Output:	Highest lift that's blocked by an object, or -1 if none.
	 */
	private int getHighestBlocked
		(
		int lift			// Look below this lift.
		) {
		int i;				// Look downwards.
		for (i = lift - 1; i >= 0 && !testTflags(i); i--)
			;
		return i;
	}
	/*
	 *	Get lowest blocked lift above a given level for a given tile.
	 *
	 *	Output:	Lowest lift that's blocked by an object, or -1 if none.
	 */
	private static int getLowestBlocked(int lift) {
		int i;				// Look upward.
		for (i = lift; i <= tflagsMaxz && !testTflags(i); i++)
			;
		if (i > tflagsMaxz) return -1;
		return i;
	}
	private int getLowestBlocked
		(
		int lift,			// Look above this lift.
		int tx, int ty			// Square to test.
		) {
		setTflags(tx, ty, 255);	// FOR NOW, look up to max.
		return getLowestBlocked(lift);
		}
	/*
	 *	See if a tile is water or land.
	 *	Returns bit0=1 if land, bit1=1 if water, bit2=1 if solid.
	 */
	int checkTerrain
		(
		int tx, int ty			// Tile within chunk.
		)
		{
		int shnum = terrain.getShapeNum(tx, ty);
		int terrain = 0;
		if (shnum >= 0) {
			ShapeInfo info = ShapeID.getInfo(shnum);
			if (info.isWater())
				terrain |= 2;
			else if (info.isSolid())
				terrain |= 4;
			else
				terrain |= 1;
		}
		return terrain;
	}
	/*
	 *	Is a given square available at a given lift?
	 *
	 *	Output: lift that's free if so, else -1.
	 *		If >=0 (tile is free), returns the new height that
	 *		   an actor will be at if he walks onto the tile.
	 */
	public int spotAvailable
		(
		int height,			// Height (in tiles) of obj. being
							//   tested.
		int tx, int ty, int lift,			// Square to test.
		int move_flags,
		int max_drop,			// Max. drop/rise allowed.
		int max_rise			// Max. rise, or -1 to use old beha-
								//   viour (max_drop if FLY, else 1).
		) {
		int new_lift;
		// Ethereal beings always return not blocked
		// and can only move horizontally
		if ((move_flags & EConst.MOVE_ETHEREAL) != 0)
			return lift;
						// Figure max lift allowed.
		if (max_rise == -1)
			max_rise = (move_flags & EConst.MOVE_FLY) != 0 ? max_drop : 1;
		int max_lift = lift + max_rise;
		if (max_lift > 255)
			max_lift = 255;		// As high as we can go.
		setTflags(tx, ty, max_lift + height);
		for (new_lift = lift; new_lift <= max_lift; new_lift++) {
			if (!testTflags(new_lift)) {
						// Not blocked?
				int new_high = getLowestBlocked(new_lift);
						// Not blocked above?
				if (new_high == -1 || new_high >= (new_lift + height))
					break;	// Okay.
				}
			}
		if (new_lift > max_lift) {	// Spot not found at lift or higher?
						// Look downwards.
			new_lift = getHighestBlocked(lift) + 1;
			if (new_lift >= lift)	// Couldn't drop?
				return -1;
			int new_high = getLowestBlocked(new_lift);
			if (new_high != -1 && new_high < new_lift + height)
				return -1;	// Still blocked above.
		}
		if (new_lift <= lift) {		// Not going up?  See if falling.
			new_lift =  (move_flags & EConst.MOVE_LEVITATE) != 0 ? lift :
					getHighestBlocked(lift) + 1;
						// Don't allow fall of > max_drop.
			if (lift - new_lift > max_drop)
				return -1;
			int new_high = getLowestBlocked(new_lift);
		
			// Make sure that where we want to go is tall enough for us
			if (new_high != -1 && new_high < (new_lift + height)) 
				return -1;
		}
		// Found a new place to go, lets test if we can actually move there
		// Lift 0 tests
		if (new_lift == 0) {
			int ter = 0;
			ter = checkTerrain(tx, ty);
			if ((ter & 2) != 0)	{	// Water
				if ((move_flags & (EConst.MOVE_FLY|EConst.MOVE_SWIM)) != 0)
					return new_lift;
				else
					return -1;
			} else if ((ter & 1) != 0) {	// Land
				if ((move_flags & (EConst.MOVE_FLY|EConst.MOVE_WALK)) != 0)
					return new_lift;
				else
					return -1;
			} else if ((ter & 4) != 0) {	// Blocked
				if ((move_flags & EConst.MOVE_FLY) != 0)
					return new_lift;
				else
					return -1;
			} else	// Other
				return new_lift;
		} else if ((move_flags & (EConst.MOVE_FLY|EConst.MOVE_WALK)) != 0)
			return new_lift;
		return -1;
	}
	/*
	 *	This one is used to see if an object of dims. possibly > 1X1 can
	 *	step onto an adjacent square.
	 */
	public static boolean areaAvailable
		(
						// Object dims:
		int xtiles, int ytiles, int ztiles,
		Tile from,		// Stepping from here.
		Tile to,		// Stepping to here.  Tz updated.
		int move_flags,
		int max_drop,			// Max drop/rise allowed.
		int max_rise			// Max. rise, or -1 to use old beha-
								//   viour (max_drop if FLY, else 1).
		) {
		int vertx0, vertx1;		// Get x-coords. of vert. block
						//   to right/left.
		int horizx0, horizx1;		// Get x-coords of horiz. block
						//   above/below.
		int verty0, verty1;		// Get y-coords of horiz. block
						//   above/below.
		int horizy0, horizy1;		// Get y-coords of vert. block
						//   to right/left.
						// !Watch for wrapping.
		horizx0 = (to.tx + 1 - xtiles + EConst.c_num_tiles)%EConst.c_num_tiles;
		horizx1 = EConst.INCR_TILE(to.tx);
		if (Tile.gte(to.tx, from.tx)) {		// Moving right?
			// Start to right of hot spot.
			vertx0 = EConst.INCR_TILE(from.tx);
			vertx1 = EConst.INCR_TILE(to.tx);	// Stop past dest.
		} else {				// Moving left?
			vertx0 = (to.tx + 1 - xtiles + EConst.c_num_tiles)%EConst.c_num_tiles;
			vertx1 = (from.tx + 1 - xtiles + EConst.c_num_tiles)%EConst.c_num_tiles;
		}
		verty0 = (to.ty + 1 - ytiles + EConst.c_num_tiles)%EConst.c_num_tiles;
		verty1 = EConst.INCR_TILE(to.ty);
		if (Tile.gte(to.ty, from.ty)) {		// Moving down?
						// Start below hot spot.
			horizy0 = EConst.INCR_TILE(from.ty);	
			horizy1 = EConst.INCR_TILE(to.ty);	// End past dest.
			if (to.ty != from.ty)	// Includes bottom of vert. area.
				verty1 = EConst.DECR_TILE(verty1);
		} else {				// Moving up?
			horizy0 = (to.ty + 1 - ytiles + EConst.c_num_tiles)%EConst.c_num_tiles;
			horizy1 = (from.ty + 1 - ytiles + EConst.c_num_tiles)%EConst.c_num_tiles;
						// Includes top of vert. area.
			verty0 = EConst.INCR_TILE(verty0);
		}
		int x, y;			// Go through horiz. part.
		int new_lift = from.tz;
		int new_lift0 = -1;		// All lift changes must be same.
		for (y = horizy0; y != horizy1; y = EConst.INCR_TILE(y)) {
						// Get y chunk, tile-in-chunk.
			int cy = y/EConst.c_tiles_per_chunk, rty = y%EConst.c_tiles_per_chunk;
			for (x = horizx0; x != horizx1; x = EConst.INCR_TILE(x))
				{
				MapChunk olist = gmap.getChunk(
						x/EConst.c_tiles_per_chunk, cy);
				int rtx = x%EConst.c_tiles_per_chunk;
				new_lift = olist.spotAvailable(ztiles, rtx, rty, from.tz, 
							move_flags, max_drop, max_rise);
				if (new_lift == -1)
					return false;
				if (new_lift != from.tz) {
					if (new_lift0 == -1)
						new_lift0 = new_lift;
					else if (new_lift != new_lift0)
						return false;
				}
			}
		}
						// Do vert. block.
		for (x = vertx0; x != vertx1; x = EConst.INCR_TILE(x)) {
						// Get x chunk, tile-in-chunk.
			int cx = x/EConst.c_tiles_per_chunk, rtx = x%EConst.c_tiles_per_chunk;
			for (y = verty0; y != verty1; y = EConst.INCR_TILE(y))
				{
				MapChunk olist = gmap.getChunk(cx, y/EConst.c_tiles_per_chunk);
				int rty = y%EConst.c_tiles_per_chunk;
				new_lift = olist.spotAvailable(ztiles, rtx, rty, from.tz,
						move_flags, max_drop, max_rise);
				if (new_lift == -1)
					return false;
				if (new_lift != from.tz) {
					if (new_lift0 == -1)
						new_lift0 = new_lift;
					else if (new_lift != new_lift0)
						return false;
				}
			}
		}
		to.tz = (short) new_lift;
		return true;			// All clear.
	}
	/*
	 *	Is a given rectangle of tiles available at a given lift?
	 *
	 *	Output: 1 if so, else 0.
	 *		If 1 (tile is free), loc.tz contains the new height where the
	 *			space is available.
	 */
	public static boolean areaAvailable(				
		int xtiles, int ytiles, int ztiles,	// Object dims:
		Tile loc,		// Location we want.  Tz is updated.
		int move_flags,
		int max_drop,			// Max drop/rise allowed.
		int max_rise			// Max. rise, or -1 to use old beha-
								//   viour (max_drop if FLY, else 1).
		) {
		int tx, ty;
		int new_lift = 0;
		int startx = loc.tx%EConst.c_num_tiles;		// Watch for wrapping.
		int starty = loc.ty%EConst.c_num_tiles;
		int stopy = (starty + ytiles)%EConst.c_num_tiles, 
		    stopx = (startx + xtiles)%EConst.c_num_tiles;
		for (ty = starty; ty != stopy; ty = EConst.INCR_TILE(ty)) {
						// Get y chunk, tile-in-chunk.
			int cy = ty/EConst.c_tiles_per_chunk, rty = ty%EConst.c_tiles_per_chunk;
			for (tx = startx; tx != stopx; tx = EConst.INCR_TILE(tx)) {
				int this_lift;
				MapChunk olist = gmap.getChunk(
						tx/EConst.c_tiles_per_chunk, cy);
				if ((this_lift = olist.spotAvailable(ztiles, 
						tx%EConst.c_tiles_per_chunk, rty, loc.tz,
						move_flags, max_drop,max_rise)) < 0)
					return false;
						// Take highest one.
				new_lift = this_lift > new_lift ? this_lift : new_lift;
			}
		}
		loc.tz = (short)new_lift;
		return true;
	}
	public static boolean areaAvailable(				
			int xtiles, int ytiles, int ztiles,	// Object dims:
			Tile loc,		// Location we want.  Tz is updated.
			int move_flags, int max_drop) {
		return areaAvailable(xtiles, ytiles, ztiles, loc, move_flags, max_drop, -1);
	}
	/*
	 *	Find a free area for an object of a given shape, looking outwards.
	 *
	 *	Output:	Pos is set to tile if successful.
	 */
	public static final int 	// For findSpot:
		anywhere = 0,
		inside = 1,
		outside = 2;
	public static boolean findSpot(Tile pos,  int dist,
			int shapenum, int framenum, int max_drop,int dir,
			int where) {
		ShapeInfo info = ShapeID.getInfo(shapenum);
		int xs = info.get3dXtiles(framenum);
		int ys = info.get3dYtiles(framenum);
		int zs = info.get3dHeight();
						// The 'MOVE_FLY' flag really means
						//   we can look upwards by max_drop.
		final int mflags = EConst.MOVE_WALK|EConst.MOVE_FLY;
						// Start with original position.
		spotPos.set(pos.tx - xs + 1, pos.ty - ys + 1, pos.tz);
		if (MapChunk.areaAvailable(xs, ys, zs, spotPos, mflags, max_drop)) {
			pos.tz = spotPos.tz;
			return true;
		}
		if (dir < 0)
			dir = EUtil.rand()%8;		// Choose dir. randomly.
		dir = (dir + 1)%8;		// Make NW the 0 point.
		for (int d = 1; d <= dist; d++)	// Look outwards.
			{
			int square_cnt = 8*d	;// # tiles in square's perim.
						// Get square (starting in NW).
			Tile square[] = getSquare(pos, d);
			int index = dir*d;	// Get index of preferred spot.
						// Get start of preferred range.
			index = (index - d/2 + square_cnt)%square_cnt;
			for (int cnt = square_cnt; cnt > 0; cnt--, index++) {
				Tile p = square[index%square_cnt];
				spotPos.set(p.tx - xs + 1, p.ty - ys + 1, p.tz);
				if (MapChunk.areaAvailable(xs, ys, zs, spotPos, mflags, max_drop) &&
				    (where == anywhere || 
					  checkSpot(where, p.tx, p.ty, spotPos.tz))) {
						// Use tile before deleting.
					pos.set(p.tx, p.ty, spotPos.tz);
					return true;
				}
			}
		}
		return false;
	}
	public static boolean findSpot(Tile pos,  int dist,
			int shapenum, int framenum, int max_drop) {
		return findSpot(pos, dist, shapenum, framenum, 0, -1, anywhere);
	}
	/*
	 *	Find a free area for an object (usually an NPC) that we want to
	 *	approach a given position.
	 *
	 *	Output:	Pos is set to tile if successful.
	 */
	public static boolean findSpot
		(
		Tile pos,			// Starting point.
		int dist,			// Distance to look outwards.  (0 means
							//   only check 'pos'.
		GameObject obj,		// Object that we want to move.
		int max_drop,		// Allow to drop by this much.
		int where			// Inside/outside.
		) {
		int t2x = obj.getTileX(), t2y = obj.getTileY();
						// Get direction from pos. to object.
		int dir = (int) EUtil.getDirection(pos.ty - t2y, t2x - pos.tx);
		return findSpot(pos, dist, obj.getShapeNum(), obj.getFrameNum(),
				max_drop, dir, where);
	}
	/*
	 *	Get the list of tiles in a square perimeter around a given tile.
	 *
	 *	Output:	List (8*dist) of tiles, starting in Northwest corner and going
	 *		   clockwise.  List is on heap.
	 */
	private static Tile[] getSquare
		(
		Tile pos,			// Center of square.
		int dist			// Distance to perimeter (>0)
		) {
		Tile square[] = new Tile[8*dist];
						// Upper left corner:
		square[0] = new Tile(EConst.DECR_TILE(pos.tx, dist), 
							 EConst.DECR_TILE(pos.ty, dist), pos.tz);
		int i;				// Start with top row.
		int len = 2*dist + 1;
		int out = 1;
		for (i = 1; i < len; i++, out++)
			square[out] = new Tile(EConst.INCR_TILE(square[out - 1].tx),
				square[out - 1].ty, pos.tz);
						// Down right side.
		for (i = 1; i < len; i++, out++)
			square[out] = new Tile(square[out - 1].tx,
					EConst.INCR_TILE(square[out - 1].ty), pos.tz);
						// Bottom, going back to left.
		for (i = 1; i < len; i++, out++)
			square[out] = new Tile(EConst.DECR_TILE(square[out - 1].tx),
				square[out - 1].ty, pos.tz);
						// Left side, going up.
		for (i = 1; i < len - 1; i++, out++)
			square[out] = new Tile(square[out - 1].tx,
					EConst.DECR_TILE(square[out - 1].ty), pos.tz);
		return square;
	}
	/*
	 *	Check a spot against the 'where' paramater to findSpot.
	 *	Output:	true if it passes.
	 */
	private static boolean checkSpot(int where, int tx, int ty, int tz) {
		int cx = tx/EConst.c_tiles_per_chunk, cy = ty/EConst.c_tiles_per_chunk;
		MapChunk chunk = gmap.getChunk(cx, cy);
		return (where == inside) == 
					(chunk.isRoof(tx % EConst.c_tiles_per_chunk, 
						ty % EConst.c_tiles_per_chunk, tz) < 31);
	}

	/*
	 *  Finds if there is a 'roof' above lift in tile (tx, ty)
	 *  of the chunk. Point is taken 4 above lift
	 *
	 *  Roof can be any object, not just a literal roof
	 *
	 *  Output: height of the roof.
	 *  A return of 31 means no roof
	 *
	 */
	public int isRoof(int tx, int ty, int lift) {
		/* Might be lying on bed at lift==2. */
		int height = getLowestBlocked (lift+4, tx, ty);
		if (height == -1) return 255;
		return height;
	}
	/*
	 *	Here's an iterator that takes a rectangle of tiles, and sequentially
	 *	returns the interesection of that rectangle with each chunk that it
	 *	touches.
	 */
	public static class ChunkIntersectIterator {
		private Rectangle tiles;		// Original rect, shifted -cx, -cy.
		private int start_tx;			// Saves start of tx in tiles.
						// Chunk #'s covered:
		private int startcx, stopcx, stopcy;
		private int curcx, curcy;		// Next chunk to return.
		void set(Rectangle t) {
			tiles.set(t);
			startcx = t.x/EConst.c_tiles_per_chunk;
			stopcx = EConst.INCR_CHUNK((t.x + t.w - 1)/EConst.c_tiles_per_chunk);
			stopcy = EConst.INCR_CHUNK((t.y + t.h - 1)/EConst.c_tiles_per_chunk);
			curcy = t.y/EConst.c_tiles_per_chunk;
			curcx = startcx;
			tiles.shift(-curcx*EConst.c_tiles_per_chunk,
						-curcy*EConst.c_tiles_per_chunk);
			start_tx = tiles.x;
			if (t.x < 0 || t.y < 0) {		// Empty to begin with.
				curcx = stopcx;
				curcy = stopcy;
			}
		}
		ChunkIntersectIterator(Rectangle t) {
			tiles = new Rectangle(); 
			set(t);
		}
		ChunkIntersectIterator() {
			tiles = new Rectangle();
		}
		// Intersect is ranged within chunk.
		MapChunk getNext(Rectangle intersect) {
			if (curcx == stopcx) {	// End of row?
				if (curcy == stopcy)
					return null;
				else {
					tiles.y -= EConst.c_tiles_per_chunk;
					tiles.x = start_tx;
					curcy = EConst.INCR_CHUNK(curcy);
					if (curcy == stopcy)
						return null;
					curcx = startcx;
				}
			}
			intersect.set(0, 0, EConst.c_tiles_per_chunk, EConst.c_tiles_per_chunk);
						// Intersect given rect. with chunk.
			intersect.intersect(tiles);
			MapChunk chunk = gmap.getChunk(curcx, curcy);
			curcx = EConst.INCR_CHUNK(curcx);
			tiles.x -= EConst.c_tiles_per_chunk;
			return chunk;
		}
	}

}
