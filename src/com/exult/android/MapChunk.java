package com.exult.android;

public class MapChunk {
	private GameMap map;				// Map we're a part of.
	private ChunkTerrain terrain;		// Flat landscape tiles.
	private ObjectList objects;			// -'Flat'  obs. (lift=0,ht=0) stored 1st.
	private GameObject firstNonflat;			// ->first nonflat in 'objects'.
	private short cx, cy;
	
	public MapChunk(GameMap m, int chx, int chy) {
		map = m;
		cx = (short)chx;
		cy = (short)chy;
		terrain = null;
		objects = new ObjectList();
	}
	public int getCx() {
		return cx;
	}
	public int getCy() {
		return cy;
	}
	public ObjectList getObjects() {
		return objects;
	}
	public ObjectList.FlatObjectIterator getFlatObjectIterator() {
		return objects.getFlatIterator(firstNonflat);
	}
	public void setTerrain(ChunkTerrain ter) {
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
	public void add(GameObject newobj) {
		newobj.setChunk(this);
		if (firstNonflat != null)
			objects.insertBefore(newobj, firstNonflat);
		else
			objects.append(newobj);
		//+++++++FINISH - Lots of code involving sorting objects, etc.
	}
	public ImageBuf getRenderedFlats() {
		return terrain != null ? terrain.getRenderedFlats() : null;
	}
}
