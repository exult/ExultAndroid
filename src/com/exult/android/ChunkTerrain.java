package com.exult.android;

import java.lang.ref.SoftReference;

/*
 *	The flat landscape, 16x16 tiles:
 */
public class ChunkTerrain {
	private int shapes[];	//   The flat (non-RLE's) are
							//   rendered here, the others are
							//   turned into Game_objects in the
							//   chunks that point to us. Each entry
							//   is of form 0x00ssssff, s=shape, f=frame.
	private int numClients;		// # of Chunk's that point to us.
	private SoftReference<byte[]> renderedFlats;	// Flats rendered for entire chunk.
	//   Kept only for nearby chunks.
	
	// Create rendered_flats.
	private final void paintTile(int tilex, int tiley) {
		ShapeFrame shape = getShape(tilex, tiley);
		if (shape != null && !shape.isRle()) {		// Only do flat tiles.
			byte src[] = shape.getData();
			byte flats[] = renderedFlats.get();
			int from = 0, to = tilex*EConst.c_tilesize + 
							   tiley*EConst.c_tilesize*EConst.c_chunksize;
			for (int y = 0; y < EConst.c_tilesize; ++y) {
				System.arraycopy(src, from, flats, to, EConst.c_tilesize);
				from += EConst.c_tilesize;
				to += EConst.c_chunksize;
			}
		}
	}
	private byte[] renderFlats() {
		byte flats[] = new byte[EConst.c_chunksize*EConst.c_chunksize];
		assert(renderedFlats == null || renderedFlats.get() == null);
		renderedFlats = new SoftReference<byte[]>(flats);
					// Go through array of tiles.
		for (int tiley = 0; tiley < EConst.c_tiles_per_chunk; tiley++)
			for (int tilex = 0; tilex < EConst.c_tiles_per_chunk; tilex++)
				paintTile(tilex, tiley);
		return flats;
	}
	// Create from 16x16x2 data:
	public ChunkTerrain(byte []data, boolean v2_chunks) {
		numClients = 0;
		renderedFlats = null;
		shapes = new int[16*16];
		int ind = 0;
		for (int tiley = 0; tiley < EConst.c_tiles_per_chunk; tiley++)
			for (int tilex = 0; tilex < EConst.c_tiles_per_chunk; tilex++) {
				int shnum, frnum;
				if (v2_chunks) { 
					shnum = data[ind + 0]&0xff + 256*(data[ind + 1]&0xff);
					frnum = data[ind + 2]&0xff;
					ind += 3;
				} else {
					int hi = data[ind + 1]&3;
					shnum = (int)(data[ind + 0]&0xff) + 256*hi;
				    frnum = (data[ind + 1]>>2)&0x1f;
					ind += 2;
				}
				shapes[16*tiley + tilex] = ((shnum<<8)&0xffff00) | (frnum&0xff);
			}
	}
	public final void addClient()
		{ numClients++; }
	public final void removeClient()
		{ numClients--; }
	// Get tile's shape ID.
	public final void getFlat(ShapeID retId, int tilex, int tiley) {
		int n = shapes[16*tiley + tilex];
		retId.set((n>>8)&0xffff, n&0xff, ShapeFiles.SHAPES_VGA);
	}
	public final int getShapeNum(int tilex, int tiley) {
		int n = shapes[16*tiley + tilex];
		return (n>>8)&0xffff;
	}
	public final int getFrameNum(int tilex, int tiley) {
		int n = shapes[16*tiley + tilex];
		return n&0xff;
	}
	public final ShapeFrame getShape(int tilex, int tiley) {
		int n = shapes[16*tiley + tilex];
		return ShapeFiles.SHAPES_VGA.getFile().getShape((n>>8)&0xffff, n&0xff);
	}
	public final byte[] getRenderedFlats() {
		if (renderedFlats == null)
			return renderFlats();
		else {
			byte flats[] = renderedFlats.get();
			return flats != null ? flats : renderFlats();
		}
	}
}
