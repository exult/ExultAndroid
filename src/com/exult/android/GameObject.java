package com.exult.android;
import java.util.Set;
import android.graphics.Point;

public abstract class GameObject extends ShapeID {
	protected MapChunk chunk;	// Chunk we're in, or NULL.
	protected byte tx, ty;		// (X,Y) of shape within chunk, or if
								//   in a container, coords. within
								//   gump's rectangle.
	protected byte lift;		// Raise by 4* this number.
	protected short quality;	// Some sort of game attribute.
	public GameObject next, prev;	// ->next in chunk list or container.
	private Set<GameObject> dependencies;	// Objects which must be painted before
						//   this can be rendered.
	private Set<GameObject> dependors;	// Objects which must be painted after.
	private static byte rotate[] = new byte[8];	// For getting rotated frame #.
	protected Point paintLoc = new Point();	// Temp for getting coords.
	public int renderSeq;		// Render sequence #.
	
	public GameObject(int shapenum, int framenum, int tilex, 
			int tiley, int lft) {
		super(shapenum, framenum);
		tx = (byte)tilex; ty = (byte)tiley; lift = (byte)lft;
	}
	public final int getTx()		// Get tile (0-15) within chunk.
		{ return tx; }
	public final int getTy()
		{ return ty; }
	public final int getLift()
		{ return lift; }
	public final void setChunk(MapChunk c) {
		chunk = c;
	}
	public void paint() {
		int x, y;
		gwin.getShapeLocation(paintLoc, this);
		// paintShape(paintLoc.x, paintLoc.y);
	}
}

