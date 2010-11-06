package com.exult.android;

public class EConst {
	public static final int c_basetilesize = 8;		// A tile (shape) is 8x8 pixels.
	public static final int c_tilesize = 8;	// A tile (shape) is 8x8 pixels.
	public static final int c_num_tile_bytes = c_tilesize * c_tilesize;	// Total pixels per tile.
	public static final int c_screen_tile_size = 320/c_basetilesize;	// Number of tiles in a 'screen'.
	public static final int c_tiles_per_chunk = 16;	// A chunk is 16x16 tiles.
	public static final int c_chunksize = 16 * 8;		// A chunk has 16 8x8 shapes.
	public static final int c_num_schunks = 12;
	public static final int c_num_chunks = 12*16;		// Total # of chunks in each dir.
	public static final int c_chunks_per_schunk = 16;	// # chunks in each superchunk.
	public static final int c_tiles_per_schunk = 16*16;	// # tiles in each superchunk.
						// Total # tiles in each dir.:
	public static final int c_num_tiles = c_tiles_per_chunk*c_num_chunks;

	public static final int c_fade_in_time = 30;	// Time for fade in
	public static final int c_fade_out_time = 30;	// Time for fade out
	public static final int c_std_delay = 200;	// Standard animation delay.  May want to
					//   make this settable!

	public static final int c_any_shapenum = -359;
	public static final int c_any_qual = -359;
	public static final int c_any_framenum = -359;
	public static final int c_any_quantity = -359;

	// Maximum number of shapes:
	public static final int c_max_shapes = 2048;
	public static final int c_occsize = c_max_shapes/8 + (((c_max_shapes%8) !=0) ? 1 : 0);

	// Maximum number of global flags:
	public static final int c_last_gflag = 2047;
	
	public static final int  INCR_CHUNK(int x) {
		return ((x) + 1)%c_num_chunks;
	}
}
