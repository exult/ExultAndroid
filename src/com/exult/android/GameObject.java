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
	//	Get absolute tile coords.
	public final int getTileX() {
		return chunk != null ? chunk.getCx()*EConst.c_tiles_per_chunk + tx : 255*EConst.c_tiles_per_chunk;
	}
	public final int getTileY() {
		return chunk != null ? chunk.getCy()*EConst.c_tiles_per_chunk + ty : 255*EConst.c_tiles_per_chunk;
	}
	public final MapChunk getChunk() {
		return chunk;
	}
	public final void setChunk(MapChunk c) {
		chunk = c;
	}
	public final Set<GameObject> getDependencies() {
		return dependencies;
	}
	public final Set<GameObject> getDependors() {
		return dependors;
	}
	public void paint() {
		int x, y;
		gwin.getShapeLocation(paintLoc, this);
		paintShape(paintLoc.x, paintLoc.y);
	}
	/*
	 * Compare objects for rendering.
	 */
	public class OrderingInfo {
		public Rectangle area;			// Area (pixels) rel. to screen.
		public ShapeInfo info;		// Info. about shape.
		public int tx, ty, tz;			// Absolute tile coords.
		public int xs, ys, zs;			// Tile dimensions.
		public int xleft, xright, ynear, yfar, zbot, ztop;
		private void init(GameObject obj) {
			tx = obj.getTileX(); ty = obj.getTileY(); tz = obj.getLift();
			int frnum = obj.getFrameNum();
			xs = info.get3dXtiles(frnum);
			ys = info.get3dYtiles(frnum);
			zs = info.get3dHeight();
			xleft = tx - xs + 1;
			xright = tx;
			yfar = ty - ys + 1;
			ynear = ty;
			ztop = tz + zs - 1;
			zbot = tz;
			if (zs == 0)		// Flat?
				zbot--;
		}
		public OrderingInfo(GameWindow gwin, GameObject obj) {
			area = gwin.getShapeRect(obj);
			info = obj.getInfo();
			init(obj); 
		}
		public OrderingInfo(GameWindow gwin, GameObject obj, Rectangle a) {
			area = a;				// +++++IS this safe?
			info = obj.getInfo();
			init(obj); 
		}
	}
	//	+++++Maybe we should cache these?
	public OrderingInfo getOrderingInfo() {
		return new OrderingInfo(gwin, this);
	}
	public OrderingInfo getOrderingInfo(Rectangle a) {
		return new OrderingInfo(gwin, this, a);
	}
	/*
	 *	Compare ranges along a given dimension.
	 *	Returns: byte 0: 0 if 1st < 2nd, 1 if equal, 2 if 1st > 2nd,
	 *			 byte 1: 1 if they overlap 
	 */
	private static int compareRanges (
		int from1, int to1,		// First object's range.
		int from2, int to2) {		// Second object's range.
		byte cmp, overlap;
		if (to1 < from2) {
			overlap = 0;
			cmp = 0;
		} else if (to2 < from1) {
			overlap = 0;
			cmp = 2;
		} else {				// X's overlap.
			overlap = 1;
			if (from1 < from2)
				cmp = 0;
			else if (from1 > from2)
				cmp = 2;
			else if (to1 - from1 < to2 - from2)
				cmp = 2;
			else if (to1 - from1 > to2 - from2)
				cmp = 0;
			else
				cmp = 1;
		}
		return (overlap<<8)|cmp;
	}
	/*
	 *	Compare two objects.
	 *
	 *	Output:	-1 if 1st < 2nd, 0 if dont_care, 1 if 1st > 2nd.
	 */
	public static int compare
		(
		OrderingInfo inf1,		// Info. for object 1.
		GameObject obj2
		)
		{
		GameWindow gwin = obj2.gwin;
						// See if there's no overlap.
		Rectangle r2 = gwin.getShapeRect(obj2);
		if (!inf1.area.intersects(r2))
			return (0);		// No overlap on screen.
		GameObject.OrderingInfo inf2 = obj2.getOrderingInfo(r2);
		int xcmp, ycmp, zcmp;		// Comparisons for a given dimension:
						//   -1 if o1<o2, 0 if o1==o2,
						//    1 if o1>o2.
		boolean xover, yover, zover;	// True if dim's overlap.
		xcmp = compareRanges(inf1.xleft, inf1.xright, inf2.xleft, inf2.xright);
		xover = (xcmp&0x100) != 0; xcmp = (xcmp&0xff) - 1;
		ycmp = compareRanges(inf1.yfar, inf1.ynear, inf2.yfar, inf2.ynear);
		yover = (ycmp&0x100) != 0; ycmp = (ycmp&0xff) - 1;
		zcmp = compareRanges(inf1.zbot, inf1.ztop, inf2.zbot, inf2.ztop);
		zover = (zcmp&0x100) != 0; zcmp = (zcmp&0xff) - 1;
		if (xcmp == 0 && ycmp == 0 && zcmp == 0)
						// Same space?
						// Paint biggest area sec. (Fixes 
						//   plaque at Penumbra's.)
			return (inf1.area.w < inf2.area.w  && 
				inf1.area.h < inf2.area.h) ? -1 : 
				(inf1.area.w > inf2.area.w &&
				inf1.area.h > inf2.area.h) ? 1 : 0;
//			return 0;		// Equal.
		if (xover & yover & zover) {	// Complete overlap?
			if (inf1.zs == 0)		// Flat one is always drawn first.
				return inf2.zs == 0 ? 0 : -1;
			else if (inf2.zs == 0)
				return 1;
		}
		if (xcmp >= 0 && ycmp >= 0 && zcmp >= 0)
			return 1;		// GTE in all dimensions.
		if (xcmp <= 0 && ycmp <= 0 && zcmp <= 0)
			return -1;		// LTE in all dimensions.
		if (yover) {		// Y's overlap.
			if (xover)		// X's too?
				return zcmp;
			else if (zover)		// Y's and Z's?
				return xcmp;
						// Just Y's overlap.
			else if (zcmp == 0)		// Z's equal?
				return xcmp;
			else			// See if X and Z dirs. agree.
				if (xcmp == zcmp)
					return xcmp;
						// Experiment:  Fixes Trinsic mayor
						//   statue-through-roof.
			else if (inf1.ztop/5 < inf2.zbot/5 && inf2.info.occludes())
				return -1;	// A floor above/below.
			else if (inf2.ztop/5 < inf1.zbot/5 && inf1.info.occludes())
				return 1;
			else
				return 0;
		} else if (xover) {		// X's overlap.
			if (zover)		// X's and Z's?
				return ycmp;
			else if (zcmp == 0)		// Z's equal?
				return ycmp;
			else
				return ycmp == zcmp ? ycmp : 0;
		}
						// Neither X nor Y overlap.
		else if (xcmp == -1) {		// o1 X before o2 X?
			if (ycmp == -1)		// o1 Y before o2 Y?
						// If Z agrees or overlaps, it's LT.
				return (zover || zcmp <= 0) ? -1 : 0;
		} else if (ycmp == 1) {		// o1 Y after o2 Y?
			if (zover || zcmp >= 0)
				return 1;
						// Experiment:  Fixes Brit. museum
						//   statue-through-roof.
			else if (inf1.ztop/5 < inf2.zbot/5)
				return -1;	// A floor above.
			else
				return 0;
		}
		return 0;
	}
}

