package com.exult.android;

public class MapChunk {
	private GameMap map;				// Map we're a part of.
	private ChunkTerrain terrain;		// Flat landscape tiles.
	private byte cx, cy;
	
	public MapChunk(GameMap m, int chx, int chy) {
		map = m;
		cx = (byte)chx;
		cy = (byte)chy;
		terrain = null;
	}
	public void setTerrain(ChunkTerrain ter) {
		if (terrain != null) {
			terrain.removeClient();
			// ++++++++REMOVE OBJS.
		}
		terrain = ter;
		terrain.addClient();	
			// Get RLE objects in chunk.
			//++++++++FINISH
	}
	public ImageBuf getRenderedFlats() {
		return terrain != null ? terrain.getRenderedFlats() : null;
	}
}
