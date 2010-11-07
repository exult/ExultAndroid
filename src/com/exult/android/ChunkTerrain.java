package com.exult.android;
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
	private ImageBuf renderedFlats;	// Flats rendered for entire chunk.
	// Most-recently used circular queue
	//   for rendered_flats:
	private static ChunkTerrain renderQueue = null;
	private static int queueSize;
	private ChunkTerrain renderQueueNext, renderQueuePrev;
	//   Kept only for nearby chunks.
	private void insertInQueue() {		// Queue methods.
		if (renderQueueNext != null) {		// In queue already?
										// !!Assuming it's not at head!!
			renderQueueNext.renderQueuePrev = renderQueuePrev;
		renderQueuePrev.renderQueueNext = renderQueueNext;
		} else
			queueSize++;		// Adding, so increment count.
		if (renderQueue == null)		// First?
			renderQueueNext = renderQueuePrev = this;
		else {
			renderQueueNext = renderQueue;
			renderQueuePrev = renderQueue.renderQueuePrev;
			renderQueuePrev.renderQueueNext = this;
			renderQueue.renderQueuePrev = this;
		}
		renderQueue = this;
	}
	private void removeFromQueue() {
		if (renderQueueNext == null)
			return;			// Not in queue.
		queueSize--;
		if (renderQueueNext == this)	// Only element?
			renderQueue = null;
		else {
			if (renderQueue == this)
				renderQueue = renderQueueNext;
			renderQueueNext.renderQueuePrev = renderQueuePrev;
			renderQueuePrev.renderQueueNext = renderQueueNext;
			}
		renderQueueNext = renderQueuePrev = null;
	}
	// Create rendered_flats.
	private final void paintTile(int tilex, int tiley) {
		ShapeFrame shape = getShape(tilex, tiley);
		if (shape != null && !shape.isRle())		// Only do flat tiles.
			renderedFlats.copy8(shape.getData(), 0,
				EConst.c_tilesize, EConst.c_tilesize, tilex*EConst.c_tilesize,
							tiley*EConst.c_tilesize);
	}
	private void freeRenderedFlats() {
		renderedFlats = null;
	}
	private ImageBuf renderFlats() {
		if (renderedFlats != null) {
		if (queueSize > 100) {	// FOR NOW.
								// Grown too big.  Remove last.
			ChunkTerrain last = renderQueue.renderQueuePrev;
			last.freeRenderedFlats();
			renderQueue.renderQueuePrev = last.renderQueuePrev;
			last.renderQueuePrev.renderQueueNext = renderQueue;
			last.renderQueueNext = last.renderQueuePrev = null;
			queueSize--;
		}
		renderedFlats = new ImageBuf(EConst.c_chunksize, EConst.c_chunksize);
		}
					// Go through array of tiles.
		for (int tiley = 0; tiley < EConst.c_tiles_per_chunk; tiley++)
			for (int tilex = 0; tilex < EConst.c_tiles_per_chunk; tilex++)
				paintTile(tilex, tiley);
		return renderedFlats;
	}
	// Create from 16x16x2 data:
	public ChunkTerrain(byte []data, boolean v2_chunks) {
		numClients = 0;
		renderedFlats = null;
		renderQueueNext = renderQueuePrev = null;
		shapes = new int[16*16];
		int ind = 0;
		for (int tiley = 0; tiley < EConst.c_tiles_per_chunk; tiley++)
			for (int tilex = 0; tilex < EConst.c_tiles_per_chunk; tilex++) {
				int shnum, frnum;
				if (v2_chunks) { 
					shnum = data[ind + 0] + 256*data[ind + 1];
					frnum = data[ind + 2];
					ind += 3;
				} else {
					shnum = data[ind + 0]+256*(data[ind + 1]&3);
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
	public final ShapeFrame getShape(int tilex, int tiley) {
		int n = shapes[16*tiley + tilex];
		return ShapeFiles.SHAPES_VGA.getFile().getShape((n>>8)&0xffff, n&0xff);
	}
	public ImageBuf getRenderedFlats() {
		if (renderQueue != this)// Not already first in queue?
			// Move to front of queue.
			insertInQueue();
		return renderedFlats != null ? renderedFlats : renderFlats();
	}
}
