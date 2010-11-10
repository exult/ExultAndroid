package com.exult.android;

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

	public void paintChunkFlats
		(
		int cx, int cy,			// Chunk coords (0 - 12*16).
		int xoff, int yoff		// Pixel offset of top-of-screen.
		)
		{
		GameWindow gwin = GameWindow.instanceOf();
		MapChunk olist = gwin.getMap().getChunk(cx, cy);
						// Paint flat tiles.
		ImageBuf cflats = olist.getRenderedFlats();
		if (cflats != null)
			gwin.getWin().copy8(cflats.getPixels(), 0,
					EConst.c_chunksize, EConst.c_chunksize, xoff, yoff);
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
		// Shape_manager *sman = gwin->shape_man;
		renderSeq++;			// Increment sequence #.
		gwin.setPainted();

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
				/* 
				if (cheat.in_map_editor())
					Paint_chunk_outline(gwin, 
					    sman->get_special_pixel(HIT_PIXEL), cx, cy,
					    map->get_terrain_num(cx, cy), xoff, yoff);
				 */
				}
			}
		/* +++++++++++++FINISH
		// Now the flat RLE terrain.
		for (cy = start_chunky; cy != stop_chunky; cy = INCR_CHUNK(cy))
			{
			int yoff = figureScreenOffset(cy, scrollty) -  - gwin->get_scrollty_lo();
			for (cx = start_chunkx; cx != stop_chunkx; cx = INCR_CHUNK(cx))
				{
				int xoff = figureScreenOffset(cx, scrolltx) -  - gwin->get_scrolltx_lo();
				paint_chunk_flat_rles(cx, cy, xoff, yoff);

				if (cheat.in_map_editor())
					Paint_chunk_outline(gwin, 
					    sman->get_special_pixel(HIT_PIXEL), cx, cy,
					    map->get_terrain_num(cx, cy), xoff, yoff);
				}
			}
						// Draw the chunks' objects
						//   diagonally NE.
		int tmp_stopy = DECR_CHUNK(start_chunky);
		for (cy = start_chunky; cy != stop_chunky; cy = INCR_CHUNK(cy))
			{
			for (int dx = start_chunkx, dy = cy;
				dx != stop_chunkx && dy != tmp_stopy; 
					dx = INCR_CHUNK(dx), dy = DECR_CHUNK(dy))
				light_sources += paint_chunk_objects(dx, dy);
			}
		for (cx = (start_chunkx + 1)%EConst.c_num_chunks; cx != stop_chunkx; 
								cx = INCR_CHUNK(cx))
			{
			for (int dx = cx, 
				dy = (stop_chunky - 1 + EConst.c_num_chunks)%EConst.c_num_chunks; 
				dx != stop_chunkx && dy != tmp_stopy; 
					dx = INCR_CHUNK(dx), dy = DECR_CHUNK(dy))
				light_sources += paint_chunk_objects(dx, dy);
			}
		/// Dungeon Blackness (but disable in map editor mode)
		if ((int)gwin->in_dungeon >= gwin->skip_above_actor && 
								!cheat.in_map_editor())
			paint_blackness (start_chunkx, start_chunky, stop_chunkx, 
						stop_chunky, gwin->ice_dungeon?73:0);

						// Outline selected objects.
		const Game_object_vector& sel = cheat.get_selected();
		int render_skip = gwin->get_render_skip_lift();
		for (Game_object_vector::const_iterator it = sel.begin();
							it != sel.end(); ++it)
			{
			Game_object *obj = *it;
			if (!obj->get_owner() && obj->get_lift() < render_skip)
				obj->paint_outline(HIT_PIXEL);
			}

		// Paint tile grid if desired.
		if (cheat.in_map_editor())
			{
			if (cheat.show_tile_grid())
				Paint_grid(gwin, sman->get_xform(16));
			if (cheat.get_edit_mode() == Cheat::select_chunks)
				Paint_selected_chunks(gwin, sman->get_xform(13),
					start_chunkx, start_chunky, stop_chunkx,
								stop_chunky);
			}
		*/
		return light_sources;
		}
}