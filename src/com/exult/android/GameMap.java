package com.exult.android;
import java.util.Vector;
import java.util.Arrays;
import java.io.RandomAccessFile;
import java.io.InputStream;
import java.io.IOException;

public class GameMap extends GameSingletons {
	private int num;			// Map #.  Index in gwin.maps.
	private static Vector<ChunkTerrain> chunkTerrains;
	private static RandomAccessFile chunks;	// "u7chunks" file.
	private static boolean v2Chunks;		// True if 3 bytes/entry.
	private static boolean readAllTerrain;	// Read them all.
	private short terrainMap[];				// ChunkTerrains index for each chunk.
	private MapChunk objects[];				// List of objects for each chunk.
	private boolean schunkRead[];			// 12x12, a flag for each superchunk.
	private static Rectangle worldRect = new Rectangle(0, 0, 
							EConst.c_num_chunks*EConst.c_tiles_per_chunk, 
							EConst.c_num_chunks*EConst.c_tiles_per_chunk);
	private static Rectangle nearbyRect = new Rectangle();
	private static final int V2_CHUNK_HDR_SIZE = 4+4+2;
	private static final byte v2hdr[] = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 
		'e', 'x', 'l', 't', 0, 0};
	/*
	 *	Constants for IREG files:
	 */
	public static int IREG_EXTENDED = 254;		// For shape #'s > 1023.
	public static int IREG_SPECIAL = 255;		// Precedes special entries.
	public static int IREG_UCSCRIPT	= 1;		// Saved Usecode_script for object.
	public static int IREG_ENDMARK = 2;		// Just an 'end' mark.
	public static int IREG_ATTS	= 3;		// Attribute/value pairs.
	public static int IREG_STRING = 4;		// A string; ie, function name.
	
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
	public final int getNum() {
		return num;
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
	static final String digits[] = {"0", "1", "2", "3", "4", "5", "6", "7", 
									"8", "9", "a", "b", "c", "d", "e", "f"};
	public String getSchunkFileName
		(
		String prefix,		// "ireg" or "ifix".
		int schunk			// Superchunk # (0-143).
		) {
		String fname = getMappedName(prefix);
		fname += digits[schunk/16];
		fname += digits[schunk%16];
		return (fname);
	}
	/*
	 *	Read in the objects for a superchunk from one of the "u7ifix" files.
	 */
	private void getIfixObjects
		(
		int schunk			// Superchunk # (0-143).
		)
		{
		
		String nm = getSchunkFileName(EFile.U7IFIX, schunk);
		String patchnm = getSchunkFileName(EFile.PATCH_U7IFIX, schunk);
		FlexFile ifix = (FlexFile) fman.getFileObject(nm, patchnm);
		int vers = ifix.getVers();
		int scy = 16*(schunk/12);	// Get abs. chunk coords.
		int scx = 16*(schunk%12);
						// Go through chunks.
		for (int cy = 0; cy < 16; cy++)
			for (int cx = 0; cx < 16; cx++) {
						// Get to index entry for chunk.
				int chunk_num = cy*16 + cx;
				byte data[] = ifix.retrieve(chunk_num);
				if (data != null && data.length > 0)
					getIfixChunkObjects(data, vers, scx + cx, scy + cy);
			}
		ifix.close();
	}
	/*
	 *	Get the objects from one ifix chunk entry.
	 */
	private void getIfixChunkObjects(byte data[], int vers, int cx, int cy) {
		IfixGameObject obj;
		int len = data.length, ent = 0;
						// Get object list for chunk.
		MapChunk olist = getChunk(cx, cy);
		if (vers == FlexFile.orig) {
			int cnt = len/4;
			for (int i = 0; i < cnt; i++, ent += 4) {
				int tx = (data[ent]>>4)&0xf, ty = data[ent]&0xf, 
					tz = data[ent + 1] & 0xf;
				int hi = data[ent + 3]&3;
				int shnum = (int)(data[ent + 2]&0xff)+256*hi, 
					frnum = (data[ent + 3]&0xff)>>2;
				ShapeInfo info = ShapeID.getInfo(shnum);
				obj = /*+++++ (info.isAnimated() || info.hasSfx()) ?
				    new Animated_ifix_object(shnum, frnum,tx, ty, tz)
				  : */  new IfixGameObject(shnum, frnum, tx, ty, tz);
				olist.add(obj);
				}
			}
		else if (vers == FlexFile.exultV2) {
			// b0 = tx,ty, b1 = lift, b2-3 = shnum, b4=frnum
			int cnt = len/5;
			for (int i = 0; i < cnt; i++, ent += 5)
				{
				int tx = (data[ent]>>4)&0xf, ty = data[ent]&0xf, 
					tz = data[ent + 1] & 0xf;
				int hi = data[ent + 3]&0xff;
				int shnum = (int)(data[ent + 2]&0xff)+256*hi, 
					frnum = data[ent + 4]&0xff;
				ShapeInfo info = ShapeID.getInfo(shnum);
				obj = /*++++++ (info.isAnimated() || info.hasSfx()) ?
				    new Animated_ifix_object(shnum, frnum,tx, ty, tz)
				  : */  new IfixGameObject(shnum, frnum, tx, ty, tz);
				olist.add(obj);
				}
			}
		/* ++++++FINISH
		olist.setup_dungeon_levels();	// Should have all dungeon pieces now.
		*/
		}
	/*
	 *	Read a list of ireg objects.  They are either placed in the desired
	 *	game chunk, or added to their container.
	 */
	private byte entbuf[] = new byte[20];	// For reading entries.
	public void readIregObjects
		(
		InputStream ireg,			// File to read from.
		int scx, int scy,			// Abs. chunk coords. of superchunk.
		GameObject container,		// Container, or null.
		long flags					// Usecode item flags.
		) throws IOException {
		int entlen;			// Gets entry length.
		int index_id = -1;
		GameObject last_obj = null;	// Last one read in this call.
						// Go through entries.
		while ((entlen = ireg.read()) >= 0) {
			boolean extended = false;	// 1 for 2-byte shape #'s.

			// Skip 0's & ends of containers.

			if (entlen == 0 || entlen == 1) {
				if (container != null)
					return;	// Skip 0's & ends of containers.
				else
					continue;
			} else if (entlen == 2) {	// Detect the 2 byte index id 
				index_id = EUtil.Read2(ireg);
				continue;
			/*
			} else if (entlen == IREG_SPECIAL)
				{
				Read_special_ireg(ireg, last_obj);
				continue;
				}
			*/
			} else if (entlen == IREG_EXTENDED) {
				extended = true;
				entlen = ireg.read();
			}
						// Get copy of flags.
			long oflags = flags & ~(1<<GameObject.is_temporary);
			int testlen = entlen - (extended?1:0);
			if (testlen != 6 && testlen != 10 && testlen != 12 && 
						testlen != 13 && testlen != 14 && testlen != 18) {
				System.out.println("Unknown entlen " + testlen + " reading ireg entry.");
				ireg.skip(entlen);
				continue;	// Only know these two types.
			}
			ireg.read(entbuf, 0, entlen);
			int cx = ((int)entbuf[0] >> 4)&0xf; // Get chunk indices within schunk.
			int cy = ((int)entbuf[1] >> 4)&0xf;
						// Get coord. #'s where shape goes.
			int tilex, tiley;
			if (container != null) {		// In container?  Get gump coords.
				tilex = (int)entbuf[0]&0xff;
				tiley = (int)entbuf[1]&0xff;
			} else {
			 	tilex = (int)entbuf[0] & 0xf;
				tiley = (int)entbuf[1] & 0xf;
			}
			int shnum, frnum;	// Get shape #, frame #.
			if (extended) {
				shnum = ((int)entbuf[2]&0xff) + 256*((int)entbuf[3]&0xff);
				frnum = (int)entbuf[4]&0xff;
				// So the rest is in the right place.
				System.arraycopy(entbuf, 1, entbuf, 0, entlen);
			} else {
				shnum = ((int)entbuf[2]&0xff) + 256*((int)entbuf[3]&3);
				frnum = ((int)entbuf[3]&0xff) >> 2;
			}
			if (shnum == 426 && frnum == 0 && container != null) {
				System.out.println("Found it: " + tilex + ", " + tiley + ", cx = " +
						cx + ", cy = " + cy + ", container is a " + container.getShapeNum());
			}
			ShapeInfo info = ShapeID.getInfo(shnum);
			int lift, quality = 0, type;
			IregGameObject obj = null;
			boolean is_egg = false;		// Fields are eggs.

			// Has flag byte(s)
			if (testlen == 10) {
				// Temporary
				if ((entbuf[6] & 1) != 0) 
					oflags |= 1<<GameObject.is_temporary;
			}	
						// An "egg"?
			if (info.getShapeClass() == ShapeInfo.hatchable) {
				boolean anim = info.isAnimated() || info.hasSfx();
				lift = ((int)entbuf[9]&0xff) >> 4;
				/*
				Egg_object *egg = Egg_object::create_egg(entry, entlen,
								anim, shnum, frnum, tilex, tiley, lift);
				getChunk(scx + cx, scy + cy).addEgg(egg);
				last_obj = egg;
				*/
				continue;
			} else if (testlen == 6 || testlen == 10) {	// Simple entry?
				type = 0;
				lift = ((int)entbuf[4] >> 4)&0xf;
				quality = (int)entbuf[5]&0xff;
				obj = IregGameObject.create(info, shnum, frnum,
								tilex, tiley, lift);
				is_egg = obj.isEgg();

						// Wierd use of flag:
				if (info.hasQuantity()) {
					if ((quality&0x80) == 0)
						oflags &= ~(1<<GameObject.okay_to_take);
					else
						quality &= 0x7f;
				} else if (info.hasQualityFlags()) {	// Use those flags instead of deflt.
					oflags = getQualityFlags((byte)quality);
					quality = 0;
				}
			}
			/*
			else if (info.is_body_shape())
				{	// NPC's body.
				int extbody = testlen == 13 ? 1 : 0;
				type = entry[4] + 256*entry[5];
				lift = entry[9 + extbody] >> 4;
				quality = entry[7];
				oflags =	// Override flags (I think).
					Get_quality_flags(entry[11 + extbody]);
				int npc_num;
				if (quality == 1 && (extbody || (entry[8] >= 0x80 || 	 
						Game::get_game_type() == SERPENT_ISLE)))
					npc_num = extbody ? (entry[8] + 256*entry[9]) :
							((entry[8] - 0x80) & 0xFF);
				else
					npc_num = -1;
				if (!npc_num)	// Avatar has no body.
					npc_num = -1;
				Dead_body *b = new Dead_body(shnum, frnum, 
						tilex, tiley, lift, npc_num);
				obj = b;
				if (npc_num > 0)
					gwin.set_body(npc_num, b);
				if (type)	// (0 if empty.)
					{	// Don't pass along invisibility!
					read_ireg_objects(ireg, scx, scy, obj, 
						oflags & ~(1<<Obj_flags::invisible));
					obj.elements_read();
					}
				}
			*/
			else if (testlen == 12) {	// Container?
				type = ((int)entbuf[4]&0xff) + 256*((int)entbuf[5]&0xff);
				lift = ((int)entbuf[9] >> 4)&0xf;
				quality = (int)entbuf[7]&0xff;
				oflags =	// Override flags (I think).
				 	getQualityFlags(entbuf[11]);
				/* +++++++++++++
				if (info.getShapeClass() == Shape_info::virtue_stone)
					{	// Virtue stone?
					Virtue_stone_object *v = 
					   new Virtue_stone_object(shnum, frnum, tilex,
							tiley, lift);
					v.set_target_pos(entry[4], entry[5], entry[6],
									entry[7]);
					v.set_target_map(entry[10]);
					obj = v;
					type = 0;
					}
				else if (info.get_shape_class() == Shape_info::barge)
					{
					Barge_object *b = new Barge_object(
					    shnum, frnum, tilex, tiley, lift,
						entry[4], entry[5],
						(quality>>1)&3);
					obj = b;
					if (!gwin.get_moving_barge() && 
								(quality&(1<<3)))
						gwin.set_moving_barge(b);
					}
				else if (info.is_jawbone()) // serpent jawbone
					{
					obj = new Jawbone_object(shnum, frnum,
						tilex, tiley, lift, entry[10]);
					}
				
				else */
					obj = new ContainerGameObject(
					    shnum, frnum, tilex, tiley, lift,
								(int)entbuf[10]&0xff);
						// Read container's objects.
				if (type != 0) {	// Don't pass along invisibility!
					readIregObjects(ireg, scx, scy, obj, 
						oflags & ~(1<<GameObject.invisible) );
					obj.elementsRead();
				}
			}
			/* ++++++++++++
			else if (info.get_shape_class() == Shape_info::spellbook)
				{		// Length 18 means it's a spellbook.
						// Get all 9 spell bytes.
				quality = 0;
				unsigned char circles[9];
				memcpy(&circles[0], &entry[4], 5);
				lift = entry[9] >> 4;
				memcpy(&circles[5], &entry[10], 4);
				uint8 *ptr = &entry[14];
						// 3 unknowns, then bookmark.
				unsigned char bmark = ptr[3];
				obj = new Spellbook_object(
					shnum, frnum, tilex, tiley, lift,
					&circles[0], bmark);
				}
			*/
			
			last_obj = obj;		// Save as last read.
			if (obj == null)
				continue;		// Can this happen?
			obj.setQuality(quality);
			obj.setFlags((int)oflags);
						// Add, but skip volume check.
			if (container != null) {
				if (index_id != -1 && 
				    container.addReadied(obj, index_id, true, true, false))
					continue;
				else if (container.add(obj, true, false, false))
					continue;
				else		// Fix tx, ty.
					obj.setShapePos(obj.getTx()&0xf,
							   obj.getTy()&0xf);
			}
			MapChunk chunk = getChunk(scx + cx, scy + cy);
			/*
			if (is_egg)
				chunk.addEgg((Egg_object *) obj);
			else */
				chunk.add(obj);
		}
	}
	private static final long getQualityFlags(
			byte qualbyte		// Quality byte containing flags.
		) {
		return 	((qualbyte&1) << GameObject.invisible) |
				(((qualbyte>>3)&1) << GameObject.okay_to_take);
	}
	/*
	 *	Read in the objects for a superchunk from one of the "u7ireg" files.
	 *	(These are the moveable objects.)
	 */
	private void getIregObjects(int schunk) { 	// Superchunk # (0-143).
		InputStream ireg;

		/*
		if (schunk_cache[schunk] && schunk_cache_sizes[schunk] >= 0) {
			// No items
			if (schunk_cache_sizes[schunk] == 0) return;
			ireg = new BufferDataSource (schunk_cache[schunk], schunk_cache_sizes[schunk]);
		}
		*/
		if (false)
			;
		else try {
			ireg = EUtil.U7openStream(getSchunkFileName(EFile.U7IREG, schunk));
			int scy = 16*(schunk/12);	// Get abs. chunk coords.
			int scx = 16*(schunk%12);
			readIregObjects(ireg, scx, scy, null, 0);
			ireg.close();
		} catch(IOException e) {
			return;			// Just don't show them.
		}
		
		/*	
						// A fixup:
		if (schunk == 10*12 + 11 && Game::get_game_type() == SERPENT_ISLE)
			{			// Lever in SilverSeed:
			Game_object_vector vec;
			if (Game_object::find_nearby(vec, Tile_coord(2936, 2726, 0),
						787, 0, 0, c_any_qual, 5))
				vec[0].move(2937, 2727, 2);
			}
		*/
		
		/*
		if (schunk_cache[schunk]) {
			delete [] schunk_cache[schunk];
			schunk_cache[schunk] = 0;
			schunk_cache_sizes[schunk] = -1;
		}
		*/
	}
	public void getSuperchunkObjects(int schunk) {
		getMapObjects(schunk);	// Get map objects/scenery.
		getIfixObjects(schunk);	// Get objects from ifix.
		getIregObjects(schunk);	// Get moveable objects.
		schunkRead[schunk] = true;	// Done this one now.
		// map_patches.apply(schunk);	// Move/delete objects.
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
		if (cx >= 0 && cx < EConst.c_num_chunks && 
			cy >= 0 && cy < EConst.c_num_chunks) {
			MapChunk list = objects[cy*EConst.c_num_chunks + cx];
			return list != null ? list : createChunk(cx, cy);
		} else
			return null;
	}
	public final int findNearby(Vector<GameObject> vec, Tile pos, int shapenum,
			int delta, int mask) {
		return findNearby(vec, pos, shapenum, delta, mask, EConst.c_any_qual,
														EConst.c_any_framenum);
	}
	public final int findNearby
		(
		Vector<GameObject> vec,	// Objects appended to this.
		Tile pos,				// Look near this point.
		int shapenum,			// Shape to look for.  
								//   -1=any (but always use mask?),
								//   c_any_shapenum=any.
		int delta,				// # tiles to look in each direction.
		int mask,				// See Check_mask() above.
		int qual,				// Quality, or c_any_qual for any.
		int framenum			// Frame #, or c_any_framenum for any.
		) {
		if (delta < 0)			// +++++Until we check all old callers.
			delta = 24;
		if (shapenum > 0 && mask == 4)	// Ignore mask=4 if shape given!
			mask = 0;
		int vecsize = vec.size();
		Rectangle tiles = nearbyRect;
		tiles.set(pos.tx - delta, pos.ty - delta, 1 + 2*delta, 1 + 2*delta);
					// Stay within world.
		tiles.intersect(worldRect);
					// Figure range of chunks.
		int start_cx = tiles.x/EConst.c_tiles_per_chunk,
	    	end_cx = (tiles.x + tiles.w - 1)/EConst.c_tiles_per_chunk;
		int start_cy = tiles.y/EConst.c_tiles_per_chunk,
	    	end_cy = (tiles.y + tiles.h - 1)/EConst.c_tiles_per_chunk;
					// Go through all covered chunks.
		for (int cy = start_cy; cy <= end_cy; cy++)
			for (int cx = start_cx; cx <= end_cx; cx++) { // Go through objects.
			MapChunk chunk = gmap.getChunk(cx, cy);
			ObjectList.ObjectIterator next = chunk.getObjects().getIterator();
			GameObject obj;
			while ((obj = next.next()) != null) {	// Check shape.
				if (shapenum >= 0) {
					if (obj.getShapeNum() != shapenum)
						continue;
				}
				if (qual != EConst.c_any_qual && obj.getQuality() != qual)
					continue;
				if (framenum !=  EConst.c_any_framenum &&
					obj.getFrameNum() != framenum)
					continue;
				if (!checkMask(obj, mask))
					continue;
				int tx = obj.getTileX(), ty = obj.getTileY();
				if (tiles.hasPoint(tx, ty)) {
					vec.addElement(obj);
				}
			}
		}
					// Return # added.
		return (vec.size() - vecsize);
	}
	/*
	 *	Check an object in find_nearby() against the mask.
	 *
	 *	Output:	1 if it passes.
	 */
	private static boolean checkMask(GameObject obj, int mask) {
		ShapeInfo info = obj.getInfo();
		if ((mask&(4|8)) != 0 &&		// Both seem to be all NPC's.
		    !info.isNpc())
			return false;
		int sclass = info.getShapeClass();
						// Egg/barge?
		if ((sclass == ShapeInfo.hatchable || sclass == ShapeInfo.barge) &&
		    (mask&0x10) == 0)		// Only accept if bit 16 set.
			return false;
		if (info.isTransparent() &&	// Transparent?
		    (mask&0x80) == 0)
			return false;
						// Invisible object?
		if (obj.getFlag(GameObject.invisible))
			if ((mask&0x20) == 0) {	// Guess:  0x20 == invisible.
				if ((mask&0x40) == 0)	// Guess:  Inv. party member.
					return false;
				if (!obj.getFlag(GameObject.in_party))
					return false;
				}
		return true;			// Passed all tests.
	}

}
