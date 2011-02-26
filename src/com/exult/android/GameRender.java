package com.exult.android;
import java.util.Set;
import java.util.Iterator;

public class GameRender {
	private long renderSeq;	// For marking rendered objects.
	private int skip;			// Set for each render.  We skip
								//   painting at or above this.
	public GameRender() {
		renderSeq = 0;
		skip = 31;
	}
	private static int figureScreenOffset
		(
		int ch,				// Chunk #
		int scroll			// Top/left tile of screen.
		) {
						// Watch for wrapping.
		int t = ch*EConst.c_tiles_per_chunk - scroll;
		if (t < -EConst.c_num_tiles/2)
			t += EConst.c_num_tiles;
		t %= EConst.c_num_tiles;
		return t*EConst.c_tilesize;
	}
	/*
	 *	Paint the flat (non-rle) shapes in a chunk.
	 */

	private void paintChunkFlats
		(
		int cx, int cy,			// Chunk coords (0 - 12*16).
		int xoff, int yoff		// Pixel offset of top-of-screen.
		)
		{
		GameWindow gwin = GameWindow.instanceOf();
		MapChunk olist = gwin.getMap().getChunk(cx, cy);
						// Paint flat tiles.
		byte cflats[] = olist.getRenderedFlats();
		if (cflats != null)
			gwin.getWin().copy8(cflats, 0,
					EConst.c_chunksize, EConst.c_chunksize, xoff, yoff);
		}
	/*
	 *	Paint the flat RLE (terrain) shapes in a chunk.
	 */
	private void paintChunkFlatRles
		(
		int cx, int cy,			// Chunk coords (0 - 12*16).
		int xoff, int yoff		// Pixel offset of top-of-screen.
		) {
		GameWindow gwin = GameWindow.instanceOf();
		MapChunk olist = gwin.getMap().getChunk(cx, cy);
		ObjectList.FlatObjectIterator iter = olist.getFlatObjectIterator();
		GameObject obj;
		while ((obj = iter.next()) != null)
			obj.paint();
		iter.done();
		}
	/*
	 *	Paint a chunk's objects, left-to-right, top-to-bottom.
	 *
	 *	Output:	# light sources found.
	 */
	private int paintChunkObjects
		(
		int cx, int cy			// Chunk coords (0 - 12*16).
		)
		{
		GameObject obj;
		GameWindow gwin = GameWindow.instanceOf();
		MapChunk olist = gwin.getMap().getChunk(cx, cy);
		int light_sources =		// Also check for light sources.
				gwin.isInDungeon() > 0 ? olist.getDungeonLights()
						: olist.getNonDungeonLights();
		
		skip = gwin.getRenderSkipLift(); 
		ObjectList.NonflatObjectIterator iter = olist.getNonflatObjectIterator();

		while ((obj = iter.next()) != null) {
			if (obj.renderSeq != renderSeq)
				paintObject(obj);
		}
		skip = 255;			// Back to a safe #.
		return light_sources;
		}
	/*
	 *	Render an object after first rendering any that it depends on.
	 */
	private void paintObject(GameObject obj) {
		int lift = obj.getLift();
		if (lift >= skip)
			return;
		obj.renderSeq = renderSeq;
		Set<GameObject> deps = obj.getDependencies();
		if (deps != null) {
			Iterator iter = deps.iterator();
			while (iter.hasNext()) {
				GameObject dep = (GameObject) iter.next();
				if (dep.renderSeq != renderSeq)
					paintObject(dep);
			}
		}
		obj.paint();			// Finally, paint this one.
	}

	/*
	 *	Paint just the map and its objects (no gumps, effects).
	 *	(The caller should set/clear clip area.)
	 *
	 *	Output:	# light-sources found.
	 */
	public int paintMap(int x, int y, int w, int h) {
		GameWindow gwin = GameWindow.instanceOf();
		GameMap map = gwin.getMap();
		// Shape_manager *sman = gwin.shape_man;
		renderSeq++;			// Increment sequence #.
		gwin.setPainted();

		//System.out.printf("paint x = %1$d, y = %2$d, w = %3$d, h = %4$d\n", 
		//		x, y, w, h);
		int scrolltx = gwin.getScrolltx(), scrollty = gwin.getScrollty();
		int light_sources = 0;		// Count light sources found.
						// Get chunks to start with, starting
						//   1 tile left/above.
		int start_chunkx = (scrolltx + x/EConst.c_tilesize - 1)/EConst.c_tiles_per_chunk;
						// Wrap around.
		start_chunkx = (start_chunkx + EConst.c_num_chunks)%EConst.c_num_chunks;
		int start_chunky = (scrollty + y/EConst.c_tilesize - 1)/EConst.c_tiles_per_chunk;
		start_chunky = (start_chunky + EConst.c_num_chunks)%EConst.c_num_chunks;
						// End 8 tiles to right.
		int stop_chunkx = 2 + (scrolltx + (x + w + EConst.c_tilesize - 2)/EConst.c_tilesize + 
						EConst.c_tiles_per_chunk/2)/EConst.c_tiles_per_chunk;
		int stop_chunky = 2 + (scrollty + (y + h + EConst.c_tilesize - 2)/EConst.c_tilesize + 
						EConst.c_tiles_per_chunk/2)/EConst.c_tiles_per_chunk;
						// Wrap around the world:
		stop_chunkx = (stop_chunkx + EConst.c_num_chunks)%EConst.c_num_chunks;
		stop_chunky = (stop_chunky + EConst.c_num_chunks)%EConst.c_num_chunks;
		int cx, cy;			// Chunk #'s.
							// Paint all the flat scenery.
		for (cy = start_chunky; cy != stop_chunky; cy = EConst.INCR_CHUNK(cy))
			{
			int yoff = figureScreenOffset(cy, scrollty);
			for (cx = start_chunkx; cx != stop_chunkx; cx = EConst.INCR_CHUNK(cx)) {
				int xoff = figureScreenOffset(cx, scrolltx);
				paintChunkFlats(cx, cy, xoff, yoff);
				}
			}
		// Now the flat RLE terrain.
		for (cy = start_chunky; cy != stop_chunky; cy = EConst.INCR_CHUNK(cy)) {
			int yoff = figureScreenOffset(cy, scrollty);
			for (cx = start_chunkx; cx != stop_chunkx; cx = EConst.INCR_CHUNK(cx)) {
				int xoff = figureScreenOffset(cx, scrolltx);
				paintChunkFlatRles(cx, cy, xoff, yoff);
			}
		}
		
						// Draw the chunks' objects
						//   diagonally NE.
		int tmp_stopy = EConst.DECR_CHUNK(start_chunky);
		for (cy = start_chunky; cy != stop_chunky; cy = EConst.INCR_CHUNK(cy))
			{
			for (int dx = start_chunkx, dy = cy;
				dx != stop_chunkx && dy != tmp_stopy; 
					dx = EConst.INCR_CHUNK(dx), dy = EConst.DECR_CHUNK(dy))
				light_sources += paintChunkObjects(dx, dy);
			}
		for (cx = (start_chunkx + 1)%EConst.c_num_chunks; cx != stop_chunkx; 
								cx = EConst.INCR_CHUNK(cx))
			{
			for (int dx = cx, 
				dy = (stop_chunky - 1 + EConst.c_num_chunks)%EConst.c_num_chunks; 
				dx != stop_chunkx && dy != tmp_stopy; 
					dx = EConst.INCR_CHUNK(dx), dy = EConst.DECR_CHUNK(dy))
				light_sources += paintChunkObjects(dx, dy);
			}
		// Dungeon Blackness (but disable in map editor mode)
		if (gwin.isInDungeon() >= gwin.getSkipAboveActor())
			paintBlackness (start_chunkx, start_chunky, stop_chunkx, 
						stop_chunky, gwin.isInIceDungeon() ? 73 : 0);
		/*++LATER maybe
						// Outline selected objects.
		GameObject_vector& sel = cheat.get_selected();
		int render_skip = gwin.get_render_skip_lift();
		for (Game_object_vector::const_iterator it = sel.begin();
							it != sel.end(); ++it)
			{
			Game_object *obj = *it;
			if (!obj.get_owner() && obj.get_lift() < render_skip)
				obj.paint_outline(HIT_PIXEL);
			}
		*/
		if (GameWindow.targetObj != null)
			GameWindow.targetObj.paintOutline(ShapeID.HIT_PIXEL);
		return light_sources;
	}
	/*
	 *	Dungeon Blacking
	 *
	 *	This is really simple. If there is a dungeon roof over our head	we
	 *	black out every tile on screen that doens't have a roof at the height
	 *	of the roof that is directly over our head. The tiles are blacked out
	 *	at the height of the the roof. 
	 *
	 *	I've done some simple optimizations. Generally all the blackness will
	 *	cover entire chunks. So, instead of drawing each tile individually, I
	 *	work out home many tiles in a row that need to be blacked out, and then
	 *	black them all out at the same time.
	 */

	private void paintBlackness(int start_chunkx, int start_chunky, 
			int stop_chunkx, int stop_chunky, int index) {
		GameWindow gwin = GameWindow.instanceOf();
		ImageBuf win = gwin.getWin();
		GameMap map = gwin.getMap();
		// Calculate the offset due to the lift (4x the lift).
		int off = gwin.isInDungeon() << 2;

		// For each chunk that might be renderable
		for (int cy = start_chunky; cy != stop_chunky; cy = EConst.INCR_CHUNK(cy)) {
			for (int cx = start_chunkx; cx != stop_chunkx; cx = EConst.INCR_CHUNK(cx)) {
				// Coord of the left edge
				int xoff = figureScreenOffset(cx, gwin.getScrolltx()) - off;
				// Coord of the top edge 
				int y = figureScreenOffset(cy, gwin.getScrollty()) - off;

				// Need the chunk cache (needs to be setup!)
				MapChunk mc = map.getChunk(cx, cy);
				if (!mc.hasDungeon()) {
					win.fill8((byte)index, 
							EConst.c_tilesize*EConst.c_tiles_per_chunk,
							EConst.c_tilesize*EConst.c_tiles_per_chunk, xoff, y);
					continue;
				}
				// For each line in the chunk
				for (int tiley = 0; tiley < EConst.c_tiles_per_chunk; tiley++) {
					// Start and width of the area to black out
					int x = xoff;
					int w = 0;
					// For each tile in the line
					for (int tilex = 0; tilex < EConst.c_tiles_per_chunk; tilex++) {
						// If the tile is blocked by 'roof'
						if (mc.isDungeon(tilex, tiley) == 0) {
							// Add to the width of the area
							w += EConst.c_tilesize;
						} else if (w > 0) {	// If not blocked and have area,
							// Draw blackness
							win.fill8((byte)index, w, EConst.c_tilesize, x, y);	

							// Set the start of the area to the next tile
							x += w + EConst.c_tilesize;

							// Clear the width
							w = 0;	
						} else {	// Not blocked, and no area
							// Increment the start of the area to the next tile
							x += EConst.c_tilesize;
						}
					}
					// If we have an area, paint it.
					if (w > 0) 
						win.fill8((byte)index, w, EConst.c_tilesize, x, y);
					// Increment the y coord for the next line
					y += EConst.c_tilesize;
				}
			}
		}
	}
	//	Paint the overlay and blackness around it.
	public void paintWizardEye() {
		// Paint sprite over view.
		GameWindow gwin = GameWindow.instanceOf();
		ImageBuf win = gwin.getWin();
		ShapeFrame spr = ShapeFiles.SPRITES_VGA.getShape(10, 0);
				// Center it.
		int w = gwin.getWidth(), h = gwin.getHeight();
		int sw = spr.getWidth(), sh = spr.getHeight();
		int topx = (w - sw)/2,
		    topy = (h - sh)/2;
		ShapeID.paintShapeTranslucent(spr, topx + spr.getXLeft(),
				topy + spr.getYAbove());
		int sizex = (w - 320)/2, sizey = (h - 200)/2;
		if (sizey > 0) {	// Black-fill area outside original resolution.
			win.fill8((byte)0, w, sizey, 0, 0);
			win.fill8((byte)0, w, sizey, 0, h - sizey);
		}
		if (sizex > 0){
			win.fill8((byte)0, sizex, 200, 0, sizey);
			win.fill8((byte)0, sizex, 200, w - sizex, sizey);
		}
	}
}
