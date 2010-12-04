package com.exult.android;
import java.util.HashSet;
import java.util.Iterator;
import android.graphics.Point;
import java.lang.ref.WeakReference;

public abstract class GameObject extends ShapeID {
	//	Flags
	public static final int
	invisible = 0,
	asleep = 1,
	charmed = 2,
	cursed = 3,
	dead = 4,
	in_party = 6,		// Guess, appears to be correct
	paralyzed = 7,
	poisoned = 8,
	protection = 9,
	on_moving_barge = 10,	// ??Guessing.
	okay_to_take = 11,	// Okay to take??
	might = 12,		// Double strength, dext, intel.
	immunities = 13,		// Test flag in Monster_info.
	cant_die = 14,		// Test flag in Monster_info.
	dancing = 15,		// ??Not sure.
	dont_move = 16,			// User can't move.
	bg_dont_render = 16,	// In BG: also completely invisible.
	si_on_moving_barge = 17,// SI's version of 10?
	is_temporary = 18,	// Is temporary
	okay_to_land = 21,	// Used for flying-carpet.
	bg_dont_move = 22,		// Exult-only: BG version of dont_move flag
	dont_render = 22,	// Exult-only: Non-BG version of bg_dont_render flag
	in_dungeon = 23,	// Pretty sure.  If set, you won't
				//   be accused of stealing food.
	confused = 25,		// ??Guessing.
	in_motion = 26,		// ??Guessing (cart, boat)??
	met = 28,			// Has the npc been met
	tournament = 29,	// Call usecode (eventid=7)
				// Originally SI-only, but allowed for BG in Exult
	si_zombie = 30,		// Used for sick Neyobi.
	no_spell_casting = 31,	// Known (cheat screen).
	// Flags > 31
	polymorph = 32,		// SI.  Pretty sure about this.
	tattooed = 33,			// Guess (SI).
	read = 34,			// Guess (SI).
	petra = 35,			// Guess
	fly = 36,			// Known (cheat screen). Get/set/clear fly type flag.
	freeze = 37,		// SI.  Pretty sure.
	naked = 38;			// Exult. Makes the avatar naked given its skin.
	
	protected final int MAX_QUANTITY = 100;
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
	public final void setLift(int l) {
		lift = (byte)l;
	}
	public final void getTile(Tile t) {
		if (chunk == null)
			t.set(255*EConst.c_tiles_per_chunk, 255*EConst.c_tiles_per_chunk, 0);
		else 
			t.set(chunk.getCx()*EConst.c_tiles_per_chunk + tx,
				  chunk.getCy()*EConst.c_tiles_per_chunk + ty, lift);
	}
	public void getOriginalTileCoord(Tile t) {	// Animated obs. will override.
		getTile(t);
	}
	public final GameObject getNext() {
		return next;
	}
	public final GameObject getPrev() {
		return prev;
	}
	//	Get absolute tile coords.
	public final int getTileX() {
		return chunk != null ? chunk.getCx()*EConst.c_tiles_per_chunk + tx : 255*EConst.c_tiles_per_chunk;
	}
	public final int getTileY() {
		return chunk != null ? chunk.getCy()*EConst.c_tiles_per_chunk + ty : 255*EConst.c_tiles_per_chunk;
	}
	public final int getVolume() {
		return getInfo().getVolume();
	}
	public final int getQuality() {
		return quality;
	}
	public final void setQuality(int q) {
		quality = (short) q;
	}
	public final int getQuantity() {
		int shnum = getShapeNum();
		if (ShapeID.getInfo(shnum).hasQuantity()) {
			int qual = quality & 0x7f;
			return qual != 0 ? qual : 1;
		} else
			return 1;
	}
	/*
	 *	Add or remove from object's 'quantity', and delete if it goes to 0.
	 *	Also, this sets the correct frame, even if delta == 0.
	 *
	 *	Output:	Delta decremented/incremented by # added/removed.
	 *		Container's volume_used field is updated.
	 */
	public final int modifyQuantity(int delta) {
		ShapeInfo info = getInfo();
		if (!info.hasQuantity()) {
						// Can't do quantity here.
			if (delta > 0)
				return (delta);
			removeThis();		// Remove from container (or world).
			return (delta + 1);
		}
		int quant = quality&0x7f;	// Get current quantity.
		if (quant == 0)
			quant = 1;		// Might not be set.
		int newquant = quant + delta;
		if (delta >= 0)			// Adding?
			{			// Too much?
			if (newquant > MAX_QUANTITY)
				newquant = MAX_QUANTITY;
			}
		else if (newquant <= 0) {		// Subtracting.
			removeThis();		// We're done for.
			return (newquant);
			}
		int oldvol = getVolume();	// Get old volume used.
		quality = (short) newquant;	// Store new value.
		
						// Set appropriate frame.
		/* ++++++++++FINISH
		if (info.hasWeaponInfo())	// Starbursts, serpent(ine) daggers, knives.
			setFrame(0);		// (Fixes messed-up games.)
		else if (info.hasQuantityFrames())
			{
				// This is actually hard-coded in the originals, but doing
				// it this way is consistent with musket ammo.
			int base = info.hasAmmoInfo() ? 24 : 0;
				// Verified.
			int new_frame = newquant > 12 ? 7 : (newquant > 6 ? 6 : newquant - 1);
			setFrame(base + new_frame);
			}
		ContainerGameObject owner = getOwner();
		if (owner != null)			// Update owner's volume.
			owner.modifyVolumeUsed(getVolume() - oldvol);
		*/
		return (delta - (newquant - quant));
	}
	public String getName() {
		//+++++FOR NOW:
		int shnum = getShapeNum();
		return shnum >= 0 && shnum < ItemNames.names.length ? ItemNames.names[shnum]
		         : new String("Unknown");
	}
	public void removeThis() {
		if (chunk != null)
			chunk.remove(this);
	}
	public int getShapeReal() {		// Actor class overrides this.
		return getShapeNum();
	}
	public ContainerGameObject getOwner() {
		return null;
	}
	public void setOwner(ContainerGameObject o)
		{  }
	public final GameObject getOutermost() {
		GameObject top = this;
		GameObject above;
		while ((above = top.getOwner()) != null)
			top = above;
		return top;
	}
	public int getUsecode() {
		ShapeInfo inf = getInfo();
		/* +++++++++FINISH
		Frame_usecode_info *useinf = inf.get_frame_usecode(
				get_framenum(), inf.has_quality() ? get_quality() : -1);
		if (useinf)
			{
			// Shape has frame- or quality-dependent usecode.
			std::string ucname = useinf->get_usecode_name();
			int ucid = -1;
			if (ucname.length())	// Try by name first.
				ucid = ucmachine->find_function(ucname.c_str(), true);
			if (ucid == -1)			// Now try usecode number.
				ucid = useinf->get_usecode();
			if (ucid >= 0)			// Have frame usecode.
				return ucid;
			}
		*/
		return ucmachine.getShapeFun(getShapeNum());
	}
	public void activate(int event) {
		/* ++++++++++
		int gump = getInfo().getGumpShape();
		// Serpent Isle spell scrolls:
		if (gump == 65 && Game::get_game_type() == SERPENT_ISLE) {
			gumpman.add_gump(this, gump);
			return;
		}
		*/
		ucmachine.callUsecode(getUsecode(), this, event);
	}
	public final void activate() {
		activate(UsecodeMachine.double_click);
	}
	// Set shape coord. in chunk/gump.
	public final void setShapePos(int shapex, int shapey)
		{ tx = (byte)shapex; ty = (byte)shapey; }
	public final void setInvalid() {
		chunk = null;
	}
	public final MapChunk getChunk() {
		return chunk;
	}
	public final void setChunk(MapChunk c) {
		chunk = c;
	}
	public final GameMap getMap() {
		return chunk != null ? chunk.getMap() : null;
	}
	public void move(int newtx, int newty, int newlift, int newmap) {
					// Figure new chunk.
		int newcx = newtx/EConst.c_tiles_per_chunk, newcy = newty/EConst.c_tiles_per_chunk;
		GameMap objmap = newmap >= 0 ? gwin.getMap(newmap) : getMap();
		if (objmap == null) objmap = gmap;
		MapChunk newchunk = objmap.getChunk(newcx, newcy);
		if (newchunk == null)
			return;			// Bad loc.
		MapChunk oldchunk = chunk;	// Remove from old.
		if (oldchunk != null) {
			gwin.addDirty(this);	// Want to repaint old area.
			oldchunk.remove(this);
		}
		setLift(newlift);		// Set new values.
		tx = (byte)(newtx%EConst.c_tiles_per_chunk);
		ty = (byte)(newty%EConst.c_tiles_per_chunk);
		newchunk.add(this);		// Updates 'chunk'.
		gwin.addDirty(this);		// And repaint new area.
	}
	public void changeFrame(int frnum) {
		gwin.addDirty(this);		// Set to repaint old area.
		setFrame(frnum);
		gwin.addDirty(this);		// Set to repaint new.
	}
	//	Step:  Overridden by Actors.
	public boolean step(Tile t, int frame, boolean force) {
		return false;
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
	public void clearDependencies() {
		Iterator X;
		GameObject obj;
		
		if (dependencies != null) {
			// First do those we depend on.
			for (X = dependencies.iterator(); X.hasNext(); ) {
				obj = (GameObject)X.next();
				if (obj.dependors != null)
					obj.dependors.remove(this);
			}
			dependencies.clear();
		}
		if (dependors != null) {
			// Now those who depend on us.
			for (X = dependors.iterator(); X.hasNext();  ) {
				obj = (GameObject)X.next();
				if (obj.dependencies != null)
					obj.dependencies.remove(this);
			}
			dependors.clear();
		}
	}
	public void paint() {
		int x, y;
		gwin.getShapeLocation(paintLoc, this);
		paintShape(paintLoc.x, paintLoc.y);
	}
	public boolean isFindable() { 
		return true; 
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
	public boolean getFlag(int flag) {
		return false;	// Only Ireg objects have flags.
	}
	public void setFlag(int flag) {
	}
	public void clearFlag(int flag) {
	}
	public Actor asActor() {
		return null;
	}
	//	Containers should override this.
	public int countObjects(int shapenum, int qual, int framenum) {
		return 0;
	}
	public void elementsRead() {
	}
	/*
	 * Compare objects for rendering.
	 */
	public static class OrderingInfo {
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
		public OrderingInfo() {
		}
	}
	private static OrderingInfo ordInfo1, ordInfo2;
	private static Rectangle ordArea1 = new Rectangle(), ordArea2 = new Rectangle();
	// Use this for object #1.
	public OrderingInfo getOrderingInfo1() {
		if (ordInfo1 == null)
			ordInfo1 = new OrderingInfo();
		ordInfo1.init(this, gwin.getShapeRect(ordArea1, this));
		return ordInfo1;
	}
	public OrderingInfo getOrderingInfo2(Rectangle a) {
		if (ordInfo2 == null)
			ordInfo2 = new OrderingInfo();
		ordInfo2.init(this, a);
		return ordInfo2;
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
		GameObject.OrderingInfo inf2 = obj2.getOrderingInfo2(r2);
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
	/*
	 *	Should this object be rendered before obj2?
	 *	NOTE:  This older interface isn't as efficient.
	 *
	 *	Output:	1 if so, 0 if not, -1 if cannot compare.
	 */
	public final int lt(GameObject obj2) {
		OrderingInfo ord = getOrderingInfo1();
		int cmp = compare(ord, obj2);
		return cmp == -1 ? 1 : cmp == 1 ? 0 : -1;
	}
}

