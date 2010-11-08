package com.exult.android;
import java.util.Vector;
import java.util.Arrays;
import java.io.RandomAccessFile;
import java.io.IOException;

public class GameMap {
	private int num;			// Map #.  Index in gwin.maps.
	private static Vector<ChunkTerrain> chunkTerrains;
	private static RandomAccessFile chunks;	// "u7chunks" file.
	private static boolean v2Chunks;		// True if 3 bytes/entry.
	private static boolean readAllTerrain;	// Read them all.
	private short terrainMap[];				// ChunkTerrains index for each chunk.
	private MapChunk objects[];				// List of objects for each chunk.
	private boolean schunkRead[];			// 12x12, a flag for each superchunk.
	private static final int V2_CHUNK_HDR_SIZE = 4+4+2;
	private static final byte v2hdr[] = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 
		'e', 'x', 'l', 't', 0, 0};
	private static ChunkTerrain readTerrain(int chunkNum) {
		int ntiles = EConst.c_tiles_per_chunk*EConst.c_tiles_per_chunk;
		byte buf[] = new byte[ntiles*3];
		ChunkTerrain ter = null;
		try {
			if (v2Chunks) {
				chunks.seek(V2_CHUNK_HDR_SIZE + chunkNum*ntiles*3);
				chunks.read(buf);
			} else {
				chunks.seek(chunkNum*ntiles*2);
				chunks.read(buf);
			}
			ter = new ChunkTerrain(buf, v2Chunks);
			if (chunkNum >= chunkTerrains.size())
				chunkTerrains.setSize(chunkNum + 1);
		} catch (IOException e) {
		}
		chunkTerrains.setElementAt(ter, chunkNum);
		return ter;
	}
	
	public GameMap(int n) {
		num = n;
		terrainMap = new short[EConst.c_num_chunks*EConst.c_num_chunks];
		objects = new MapChunk[EConst.c_num_chunks * EConst.c_num_chunks];
		schunkRead = new boolean[12*12];
	}
	// Init. the static data.
	public static void initChunks() {
		int numChunkTerrains;
		int hdrsize = 0, chunksz = EConst.c_tiles_per_chunk*EConst.c_tiles_per_chunk*2;
		try {
			if (chunks != null)
			chunks.close();
			String nm = EUtil.U7exists(EFile.PATCH_U7CHUNKS);
			if (nm != null)
				chunks = new RandomAccessFile(EFile.PATCH_U7CHUNKS, "r");
			else {
				nm = EUtil.U7exists(EFile.U7CHUNKS);
				chunks = new RandomAccessFile(nm != null ? nm : EFile.U7CHUNKS, "r");
			}
			byte v2buf[] = new byte[V2_CHUNK_HDR_SIZE];	// Check for V2.
			chunks.read(v2buf);
			if (Arrays.equals(v2hdr, v2buf)) {
				v2Chunks = true;
				hdrsize = V2_CHUNK_HDR_SIZE;
				chunksz = EConst.c_tiles_per_chunk*EConst.c_tiles_per_chunk*3;
			}
									// 2 bytes/tile.
			numChunkTerrains = ((int)chunks.length() - hdrsize)/chunksz;
		} catch (IOException e) {
			numChunkTerrains = 0;
			//CRASH+++++
		}

		if (chunkTerrains == null)
			chunkTerrains = new Vector<ChunkTerrain>();
						// Resize list to hold all.
		chunkTerrains.setSize(numChunkTerrains);
		readAllTerrain = false;
	}
	/*
	 *	Build a file name with the map directory before it; ie,
	 *		get_mapped_name("<GAMEDAT>/ireg, 3, to) will store
	 *			"<GAMEDAT>/map03/ireg".
	 */
	public String getMappedName(String from) {
		//++++FINISH, using 'num'
		return from;
	}
	// Init. for new/restored game.
	public void init() {
		String nm;
		RandomAccessFile u7map = null;
		Boolean nomap = false;
		if (num == 0)
			initChunks();
		try {
			if (EUtil.isSystemPathDefined("<PATCH>") && 
					(nm = EUtil.U7exists(getMappedName(EFile.PATCH_U7MAP))) != null) {
				u7map = new RandomAccessFile(nm, "r");
			} else  {
				String mname = getMappedName(EFile.U7MAP);
				nm = EUtil.U7exists(mname);
				u7map = new RandomAccessFile(nm != null ? nm : mname, "r");
			}
			byte buf[] = new byte[16*16*2];
			for (int schunk = 0; schunk < EConst.c_num_schunks*EConst.c_num_schunks; schunk++) {
						// Read in the chunk #'s.
				
				u7map.read(buf);
				int scy = 16*(schunk/12);// Get abs. chunk coords.
				int scx = 16*(schunk%12);
				int ind = 0;
							// Go through chunks.
				for (int cy = 0; cy < 16; cy++)
					for (int cx = 0; cx < 16; cx++) {
						short n = (short) EUtil.Read2(buf, ind);
						ind += 2;
						int i = (scy+cy)*EConst.c_num_chunks + scx + cx;
						terrainMap[i] = n;
					}
			}
			u7map.close();
		} catch (IOException e) {
			Arrays.fill(terrainMap, (short)0);
		}
	// Would fill various buffers here++++++++++++
	}
	/*
	 * Read in superchunk data to cover the screen.
	 */
	public void readMapData() {
		GameWindow gwin = GameWindow.instanceOf();
		int scrolltx = gwin.getScrolltx(), scrollty = gwin.getScrollty();
		int w = gwin.getWidth(), h = gwin.getHeight();
						// Start one tile to left.
		int firstsx = (scrolltx - 1)/EConst.c_tiles_per_schunk, 
		    firstsy = (scrollty - 1)/EConst.c_tiles_per_schunk;
						// End 8 tiles to right.
		int lastsx = (scrolltx + (w + EConst.c_tilesize - 2)/EConst.c_tilesize + 
					EConst.c_tiles_per_chunk/2)/EConst.c_tiles_per_schunk;
		int lastsy = (scrollty + (h + EConst.c_tilesize - 2)/EConst.c_tilesize + 
					EConst.c_tiles_per_chunk/2)/EConst.c_tiles_per_schunk;
						// Watch for wrapping.
		int stopsx = (lastsx + 1)%EConst.c_num_schunks,
		    stopsy = (lastsy + 1)%EConst.c_num_schunks;
						// Read in "map", "ifix" objects for
						//  all visible superchunks.
		for (int sy = firstsy; sy != stopsy; sy = (sy + 1)%EConst.c_num_schunks)
			for (int sx = firstsx; sx != stopsx; 
							sx = (sx + 1)%EConst.c_num_schunks) {
						// Figure superchunk #.
				int schunk = 12*sy + sx;
						// Read it if necessary.
				if (!schunkRead[schunk])
					getSuperchunkObjects(schunk);
			}
	}
	/*
	 *	Read in terrain graphics data into window's image.  (May also be
	 *	called during map-editing if the chunknum changes.)
	 */
	public void getChunkObjects(int cx, int cy) {
						// Get list we'll store into.
		MapChunk chunk = getChunk(cx, cy);
		int chunkNum = terrainMap[cy*EConst.c_num_chunks + cx];
		ChunkTerrain ter = getTerrain(chunkNum);
		chunk.setTerrain(ter);
		}
	public void getMapObjects(int schunk) {
		int scy = 16*(schunk/12);	// Get abs. chunk coords.
		int scx = 16*(schunk%12);
						// Go through chunks.
		for (int cy = 0; cy < 16; cy++)
			for (int cx = 0; cx < 16; cx++)
				getChunkObjects(scx + cx, scy + cy);
	}
	public void getSuperchunkObjects(int schunk) {
		getMapObjects(schunk);	// Get map objects/scenery.
		/*
		get_ifix_objects(schunk);	// Get objects from ifix.
		get_ireg_objects(schunk);	// Get moveable objects.
		*/
		schunkRead[schunk] = true;	// Done this one now.
		// map_patches->apply(schunk);	// Move/delete objects.
	}
	public static ChunkTerrain getTerrain(int tnum) {
		ChunkTerrain ter = (ChunkTerrain) chunkTerrains.elementAt(tnum);
		return ter != null ? ter : readTerrain(tnum);
	}
	public int getNumChunkTerrains()
		{ return chunkTerrains.size(); }
	private MapChunk createChunk(int cx, int cy) {
		MapChunk c = new MapChunk(this, cx, cy);
		objects[cy*EConst.c_num_chunks + cx] = c;
		return c;
	}
	public MapChunk getChunk(int cx, int cy) {
		MapChunk list = objects[cy*EConst.c_num_chunks + cx];
	return list != null ? list : createChunk(cx, cy);
	}
	
}
