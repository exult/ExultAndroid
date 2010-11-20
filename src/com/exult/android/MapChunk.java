package com.exult.android;

public class MapChunk extends GameSingletons {
	private GameMap map;				// Map we're a part of.
	private ChunkTerrain terrain;		// Flat landscape tiles.
	private ObjectList objects;			// -'Flat'  obs. (lift=0,ht=0) stored 1st.
	private GameObject firstNonflat;			// ->first nonflat in 'objects'.
	// Counts of overlapping objects from chunks below, to right.
	private short fromBelow, fromRight, fromBelowRight;
	private byte dungeonLevels[];	// A 'dungeon' level value for each tile (4 bit).
	private byte roof;		// 1 if a roof present.
	// # light sources in chunk.
	private byte dungeonLights, nonDungeonLights;
	private short cx, cy;
	
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
				ter.getFlat(id, tilex, tiley);
				ShapeFrame shape = id.getShape();
				if (shape != null && shape.isRle()) {
					int shapenum = id.getShapeNum(),
					    framenum = id.getFrameNum();
					ShapeInfo info = id.getInfo();
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
	 *	Output:	->chunk that was checked.
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
	/* +++++++++FINISH
	if (cache)			// Add to cache.
		cache.update_object(this, newobj, 1);
	if (ord.info.is_light_source())	// Count light sources.
		{
		if (dungeon_levels && is_dungeon(newobj.get_tx(),
							newobj.get_ty()))
			dungeon_lights++;
		else
			non_dungeon_lights++;
		}
	if (newobj.get_lift() >= 5)	// Looks like a roof?
		{
		if (ord.info.get_shape_class() == Shape_info::building)
			roof = 1;
		}
	*/
	}
	public void remove(GameObject remove) {
		/* ++++++
		if (cache)			// Remove from cache.
			cache->update_object(this, remove, 0);
		 */
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
	public ImageBuf getRenderedFlats() {
		return terrain != null ? terrain.getRenderedFlats() : null;
	}
}
