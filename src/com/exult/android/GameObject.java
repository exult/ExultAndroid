package com.exult.android;
import java.util.HashSet;
import android.graphics.Point;
import java.lang.ref.WeakReference;

public abstract class GameObject extends ShapeID {
	protected MapChunk chunk;	// Chunk we're in, or NULL.
	protected byte tx, ty;		// (X,Y) of shape within chunk, or if
								//   in a container, coords. within
								//   gump's rectangle.
	protected byte lift;		// Raise by 4* this number.
	protected short quality;	// Some sort of game attribute.
	public GameObject next, prev;	// ->next in chunk list or container.
	private HashSet<GameObject> dependencies;	// Objects which must be painted before
						//   this can be rendered.
	private HashSet<GameObject> dependors;	// Objects which must be painted after.
	private static byte rotate[] = new byte[8];	// For getting rotated frame #.
	protected Point paintLoc = new Point();	// Temp for getting coords.
	public long renderSeq;		// Render sequence #.
	private WeakReference ordInfo;
	
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
	// Set shape coord. in chunk/gump.
	public final void setShapePos(int shapex, int shapey)
		{ tx = (byte)shapex; ty = (byte)shapey; }
	public final MapChunk getChunk() {
		return chunk;
	}
	public final void setChunk(MapChunk c) {
		chunk = c;
	}
	public final HashSet<GameObject> getDependencies() {
		return dependencies;
	}
	public final HashSet<GameObject> getDependors() {
		return dependors;
	}
	public final void addDependency(GameObject obj) {
		if (dependencies == null)
			dependencies = new HashSet<GameObject>();
		dependencies.add(obj);
	}
	public final void addDependor(GameObject obj) {
		if (dependors == null)
			dependors = new HashSet<GameObject>();
		dependors.add(obj);
	}
	public void paint() {
		int x, y;
		gwin.getShapeLocation(paintLoc, this);
		paintShape(paintLoc.x, paintLoc.y);
	}
	public int getIregSize() {
		return 0;
	}
	public boolean add(GameObject obj, boolean dont_check,
			boolean combine, boolean noset) {
		// ++++ return combine ? drop(obj)!=0 : false;
		return false;
	}
	// Add to NPC 'ready' spot.
	public boolean addReadied(GameObject obj, int index,
				boolean dont_check, boolean force_pos, boolean noset)
		{ return add(obj, dont_check, false, noset); }
	public boolean isEgg() {
		return false;
	}
	public void elementsRead() {
	}
	/*
	 * Compare objects for rendering.
	 */
	public class OrderingInfo {
		// Temps for comparing two objects.
		
		public Rectangle area;			// Area (pixels) rel. to screen.
		public ShapeInfo info;		// Info. about shape.
		public int tx, ty, tz;			// Absolute tile coords.
		public int xs, ys, zs;			// Tile dimensions.
		public int xleft, xright, ynear, yfar, zbot, ztop;
		public void init(GameObject obj, Rectangle a) {
			area = a;				// +++++IS this safe?
			info = obj.getInfo();
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
		public OrderingInfo(GameObject obj, Rectangle a) {
			init(obj, a); 
		}
	}
	private static Rectangle ordArea1 = new Rectangle(), ordArea2 = new Rectangle();
	// Use this for object #1.
	public OrderingInfo getOrderingInfo() {
		return getOrderingInfo(gwin.getShapeRect(ordArea1, this));
	}
	public OrderingInfo getOrderingInfo(Rectangle a) {
		OrderingInfo inf;
		if (ordInfo == null || (inf = (OrderingInfo) ordInfo.get()) == null) {
			ordInfo = new WeakReference(inf = new OrderingInfo(this, a));
		} else {
			inf.init(this, a);
		}
		return inf;
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
		Rectangle r2 = gwin.getShapeRect(ordArea2, obj2);
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

