package com.exult.android;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.Comparator;
import java.util.Collections;
import android.graphics.Point;
import java.io.OutputStream;
import java.io.IOException;
import com.exult.android.shapeinf.*;

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
	public GameObject next, prev;	// .next in chunk list or container.
	private HashSet<GameObject> dependencies;	// Objects which must be painted before
						//   this can be rendered.
	private HashSet<GameObject> dependors;	// Objects which must be painted after.
	private static final byte rotate[] = { 0, 0, 48, 48, 16, 16, 32, 32};	// For getting rotated frame #.
	protected Point paintLoc = new Point();	// Temp for getting coords.
	private static Tile nearbyLoc = new Tile();	// Temp.
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
	private static short deltaTemp[] = new short[2];
	private static void deltaCheck
		(
		int delta1,
		int size1,
		int size2,
		short coord[]) {
		if (delta1 < 0) {
			if (coord[0] + size1 > coord[1])
				coord[0] = coord[1];
			else
				coord[0] += size1;
		} else if (delta1 > 0) {
			if (coord[1] + size2 > coord[0])
				coord[1] = coord[0];
			else
				coord[1] += size2;
		}
	}
	private static void deltaWrapCheck(
		int dir,			// Neg. if coord[1] < coord[0].
		int size1,
		int size2,
		short coord[]			// Coords.  Updated.
		)
		{
		// NOTE: An obj's tile is it's lower-right corner.
		if (dir > 0)			// coord[1] > coord[0].
			coord[1] = (short)((coord[1] - size2 + EConst.c_num_tiles)%EConst.c_num_tiles);
		else if (dir < 0)
			coord[0] = (short)((coord[0] - size1 + EConst.c_num_tiles)%EConst.c_num_tiles);
	}
	private static Tile distTile1 = new Tile(), distTile2 = new Tile();
	public final int distance(GameObject o2) {
		Tile t1 = distTile1, t2 = distTile2;
		getTile(t1);
		o2.getTile(t2);

		ShapeInfo info1 = getInfo(), info2 = o2.getInfo();
		int f1 = getFrameNum(), f2 = o2.getFrameNum();
		int dx = Tile.delta(t1.tx, t2.tx),
			dy = Tile.delta(t1.ty, t2.ty),
			dz = t1.tz - t2.tz;
		deltaTemp[0] =  t1.tx; deltaTemp[1] = t2.tx;
		deltaWrapCheck(dx, info1.get3dXtiles(f1)-1,
				info2.get3dXtiles(f2)-1, deltaTemp);
		t1.tx = deltaTemp[0]; t2.tx = deltaTemp[1];
		deltaTemp[0] =  t1.ty; deltaTemp[1] = t2.ty;
		deltaWrapCheck(dy, info1.get3dYtiles(f1)-1,
				info2.get3dYtiles(f2)-1, deltaTemp);
		t1.ty = deltaTemp[0]; t2.ty = deltaTemp[1];
		deltaTemp[0] =  t1.tz; deltaTemp[1] = t2.tz;
		deltaCheck(dz, info1.get3dHeight(),
				info2.get3dHeight(), deltaTemp);
		t1.tz = deltaTemp[0]; t2.tz = deltaTemp[1];
		return t1.distance(t2);
	}
	public final int distance(Tile t2) {
		Tile t1 = distTile1;
		getTile(t1);
		ShapeInfo info1 = getInfo();
		int f1 = getFrameNum();
		int dx = Tile.delta(t1.tx, t2.tx),
			dy = Tile.delta(t1.ty, t2.ty),
			dz = t1.tz - t2.tz;
		deltaTemp[0] =  t1.tx; deltaTemp[1] = t2.tx;
		deltaWrapCheck(dx, info1.get3dXtiles(f1)-1, 0, deltaTemp);
		t1.tx = deltaTemp[0]; t2.tx = deltaTemp[1];
		deltaTemp[0] =  t1.ty; deltaTemp[1] = t2.ty;
		deltaWrapCheck(dy, info1.get3dYtiles(f1)-1, 0, deltaTemp);
		t1.ty = deltaTemp[0]; t2.ty = deltaTemp[1];
		deltaTemp[0] =  t1.tz; deltaTemp[1] = t2.tz;
		deltaCheck(dz, info1.get3dHeight(), 0, deltaTemp);
		t1.tz = deltaTemp[0]; t2.tz = deltaTemp[1];
		return t1.distance(t2);
	}
	public final void getCenterTile(Tile t) {
		if (chunk == null) {
			t.set(255*EConst.c_tiles_per_chunk, 255*EConst.c_tiles_per_chunk, 0);
			return;
		}
		int frame = getFrameNum();
		ShapeInfo info = getInfo();
		int dx = (info.get3dXtiles(frame)-1) >> 1,
	    	dy = (info.get3dYtiles(frame)-1) >> 1,
	    	dz = (info.get3dHeight()*3)/4;
	    int x = chunk.getCx()*EConst.c_tiles_per_chunk + tx - dx,
	    	y = chunk.getCy()*EConst.c_tiles_per_chunk + ty - dy;
	    t.set(x, y, lift + dz);
	}
	public final int getDirection(GameObject o2) {
		Tile t = new Tile();
		o2.getCenterTile(t);
		int x2 = t.tx, y2 = t.ty;
		getCenterTile(t);
		return EUtil.getDirection(t.ty - y2, x2 - t.tx);
	}
	public final int getDirection(Tile t2) {
		Tile t1 = new Tile();
		getCenterTile(t1);
		return EUtil.getDirection(t1.ty - t2.ty, t2.tx - t1.tx);
	}
	/*
	 *	Get direction to best face an object.
	 */

	public final int getFacingDirection(GameObject o2) {
		Tile t1 = new Tile();
		getTile(t1);
		Rectangle torect = new Rectangle();
		o2.getFootprint(torect);
		if (torect.x + torect.w <= t1.tx && 
		    t1.ty >= torect.y && t1.ty < torect.y + torect.h)
			return EConst.west;
		else if (t1.tx < torect.x &&
		    t1.ty >= torect.y && t1.ty < torect.y + torect.h)
			return EConst.east;
		else if (torect.y + torect.h <= t1.ty &&
		    t1.tx >= torect.x && t1.tx < torect.w + torect.h)
			return EConst.south;
		else if (t1.ty < torect.y &&
		    t1.tx >= torect.x && t1.tx < torect.w + torect.h)
			return EConst.north;
		else
			return getDirection(o2);
	}
	// Find object blocking a given tile.
	public static GameObject findBlocking(Tile tile) {
		tile.fixme();
		MapChunk chunk = gmap.getChunk(tile.tx/EConst.c_tiles_per_chunk,
			    tile.ty/EConst.c_tiles_per_chunk);
		GameObject obj;
		ObjectList.ObjectIterator iter = new ObjectList.ObjectIterator(
													chunk.getObjects());
		while ((obj = iter.next()) != null) {
			if (obj.blocks(tile))
				return obj;
		}
		return null;
	}
	/*
	 * For sorting closest to a given spot.
	 */
	public static class ClosestSorter implements Comparator<GameObject> {
		Tile pos;		// Pos to get closest to.
		public ClosestSorter(Tile p) {
			pos = p;
		}
		public int compare(GameObject o1, GameObject o2) {
			return o1.distance(pos) - o2.distance(pos);
		}
	}
	public GameObject findClosest(Vector<GameObject> vec, int shapenums[], int dist) {
		int i, cnt = shapenums.length;
		Tile pos = nearbyLoc;;
		getTile(pos);
		for (i = 0; i < cnt; i++)
						// 0xb0 mask finds anything.
			gmap.findNearby(vec, pos, shapenums[i], dist, 0xb0);
		cnt = vec.size();
		if (cnt == 0)
			return null;
		if (cnt > 1) {
			Collections.sort(vec, new ClosestSorter(pos));
		}
		return vec.elementAt(0);
	}
	public GameObject findClosest(Vector<GameObject> vec, int shapenums[]) {
		return findClosest(vec, shapenums, 24);
	}
	public static GameObject findClosest(Tile pos, int shapenum, int dist, 
			int mask, int qual, int framenum) {
		Vector<GameObject> vec = new Vector<GameObject>();
		if (gmap.findNearby(vec, pos, shapenum, dist, mask,
													qual, framenum) == 0)
			return null;
		GameObject closest = null;
		int bestDist = 10000;	// Tiles.
		for (GameObject obj:vec) {
			int d = obj.distance(pos);
			if (d < bestDist) {
				bestDist = d;
				closest = obj;
			}
		}
		return closest;
	}
	public static GameObject findClosest(Tile pos, int shapenum, int dist) {
		return findClosest(pos, shapenum, dist, 0xb0, EConst.c_any_qual,
				EConst.c_any_framenum);
	}
	public GameObject findClosest(int shapenum, int dist, 
								int mask, int qual, int framenum) {
		getTile(nearbyLoc);
		return findClosest(nearbyLoc, shapenum, dist, mask, qual, framenum);
	}
	public GameObject findClosest(int shapenum, int dist) {
		return findClosest(shapenum, dist, 0xb0, EConst.c_any_qual,
				EConst.c_any_framenum);
	}
	public GameObject findClosest(int shapenum) {
		return findClosest(shapenum, 24);
	}
	public int findNearby(Vector<GameObject> vec, int shapenum, int delta, 
			int mask, int qual, int framenum) {
		getTile(nearbyLoc);
		return gmap.findNearby(vec, nearbyLoc, shapenum, delta, mask, qual,
														framenum);
	}
	public int findNearby(Vector<GameObject> vec, int shapenum, int delta, 
			int mask) {
		return findNearby(vec, shapenum, delta, mask, EConst.c_any_qual,
													EConst.c_any_framenum);
	}
	public int findNearbyActors(Vector<GameObject> vec, int shapenum, int delta) {
		return findNearby(vec, shapenum, delta, 8);
	}
	public boolean isClosedDoor() {
		ShapeInfo info = getInfo();
		if (!info.isDoor())
			return false;
					// Get door's footprint.
		int frame = getFrameNum();
		int xtiles = info.get3dXtiles(frame), ytiles = info.get3dYtiles(frame);
					// Get its location.
		int doorx = getTileX(), doory = getTileY();
		int beforex, beforey, afterx, aftery;	// Want tiles to both sides.
		if (xtiles > ytiles) {		// Horizontal footprint?
			beforex = doorx - xtiles; beforey = doory;
			afterx = doorx + 1; aftery = doory;
		} else {				// Vertical footprint.
			beforex = doorx; beforey = doory - ytiles;
			afterx = doorx; aftery = doory + 1;
		}
					// Should be blocked before/after.
		return (gmap.isTileOccupied(beforex, beforey, getLift()) &&
				gmap.isTileOccupied(afterx, aftery, getLift()));
	}
	public static GameObject findDoor(Tile tile) {
		tile.fixme();
		MapChunk chunk = gmap.getChunk(tile.tx/EConst.c_tiles_per_chunk,
							    tile.ty/EConst.c_tiles_per_chunk);
		return chunk.findDoor(tile);
	}
	// Does this object block a given tile?
	public final boolean blocks(Tile tile) {
		int tx = getTileX(), ty = getTileY(), tz = getLift();
		if (tx < tile.tx || ty < tile.ty || tz > tile.tz)
			return false;		// Out of range.
		ShapeInfo info = getInfo();
		int ztiles = info.get3dHeight(); 
		if (ztiles == 0 || !info.isSolid())
			return false;		// Skip if not an obstacle.
					// Occupies desired tile?
		int frame = getFrameNum();
		if (tile.tx > tx - info.get3dXtiles(frame) &&
			tile.ty > ty - info.get3dYtiles(frame) &&
			tile.tz < tz + ztiles)
		return true;
	return false;
	}

	public final GameObject getNext() {
		return next;
	}
	public final GameObject getPrev() {
		return prev;
	}
	public final int getCx() {
		return chunk != null ? chunk.getCx() : 255;
	}
	public final int getCy() {
		return chunk != null ? chunk.getCy() : 255;
	}
	//	Get absolute tile coords.
	public final int getTileX() {
		return getCx()*EConst.c_tiles_per_chunk + tx;
	}
	public final int getTileY() {
		return getCy()*EConst.c_tiles_per_chunk + ty;
	}
	//	Get footprint in absolute tiles.
	public final void getFootprint(Rectangle r) {
		ShapeInfo info = getInfo();
		// Get footprint.
		int frame = getFrameNum();
		int xtiles = info.get3dXtiles(frame);
		int ytiles = info.get3dYtiles(frame);
		int tx = getTileX(), ty = getTileY();
		r.set((tx - xtiles + 1 + EConst.c_num_tiles)%EConst.c_num_tiles, 
			  (ty - ytiles + 1 + EConst.c_num_tiles)%EConst.c_num_tiles, 
				xtiles, ytiles);
	}
	/*
	 *	Based on frame #, get direction (N, S, E, W, 0-7), this (generally an
	 *	NPC) is facing.
	 */
	public int getDirFacing() {
		int reflect = getFrameNum()&(16 | 32);
		switch (reflect)
			{
		case 0:
			return EConst.north;
		case 48:
			return EConst.east;
		case 16:
			return EConst.south;
		case 32:
		default:
			return EConst.west;
		}
	}
	public final boolean insideLocked() {
		GameObject top = this;
		GameObject above;
		while ((above = top.getOwner()) != null) {
			if (above.getInfo().isContainerLocked())
				return true;
			top = above;
		}
		return false;
	}
	public final int getVolume() {
		return getInfo().getVolume();
	}
	//	Get weight in 1/10 stones.
	public int getWeight() {
		return getWeight(getShapeNum(), getQuantity());
	}
	public static int getWeight(int shnum, int quant) {
		ShapeInfo info = ShapeID.getInfo(shnum);
		int wt = quant * info.getWeight();
		if (info.isLightweight()) {	// Special case:  reagents, coins.
			wt /= 10;
			if (wt <= 0) wt = 1;
		}
		if (info.hasQuantity())
			if (wt <= 0) wt = 1;
		return wt;
	}
	public int getMaxWeight() {
		// Looking outwards for NPC.
		ContainerGameObject own = getOwner();
		return own != null ? own.getMaxWeight() : 0;
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
		//System.out.println("modifyQuantity: old is " + quant + ", new is " + newquant);
		if (delta >= 0)			// Adding?
			{			// Too much?
			if (newquant > MAX_QUANTITY)
				newquant = MAX_QUANTITY;
			}
		else if (newquant <= 0) {		// Subtracting.
			quality = (short)0;	// So caller knows.
			removeThis();		// We're done for.
			return (newquant);
		}
		int oldvol = getVolume();	// Get old volume used.
		quality = (short) newquant;	// Store new value.
		
						// Set appropriate frame.
		if (info.hasWeaponInfo())	// Starbursts, serpent(ine) daggers, knives.
			setFrame(0);		// (Fixes messed-up games.)
		else if (info.hasQuantityFrames()) {
				// This is actually hard-coded in the originals, but doing
				// it this way is consistent with musket ammo.
			int base = info.hasAmmoInfo() ? 24 : 0;
				// Verified.
			int new_frame = newquant > 12 ? 7 : (newquant > 6 ? 6 : newquant - 1);
			setFrame(base + new_frame);
		}
		/* ++++++++++FINISH
		ContainerGameObject owner = getOwner();
		if (owner != null)			// Update owner's volume.
			owner.modifyVolumeUsed(getVolume() - oldvol);
		*/
		return (delta - (newquant - quant));
	}
	/*
	 *	Get effective maximum range for weapon.
	 */
	public int getEffectiveRange(WeaponInfo winf, int reach) {
		if (reach < 0) {
			if (winf == null)
				return 3;
			reach = winf.getRange();
		}
		int uses = winf != null ? winf.getUses() : WeaponInfo.melee;
		if (uses == 0 || uses == WeaponInfo.ranged)
			return reach;
		else
			return 31;
	}
	static boolean hasHitpoints(int shnum) {
		ShapeInfo info = ShapeID.getInfo(shnum);
		return ((info.getShapeClass() == ShapeInfo.has_hp) ||
				(info.getShapeClass() == ShapeInfo.container));
		// containers have hitpoints too ('resistance')
	}
	public int getObjHp() {	// hitpoints for non-NPCs
		if (hasHitpoints(getShapeNum()))
			return quality;
		else
			return 0;
	}
	public void setObjHp(int hp) {
		int shnum = getShapeNum();
		if (hasHitpoints(shnum))
			setQuality(hp);
	}
	public int getEffectiveObjHp(int weapon_shape) {
	 	int hps = getObjHp();
		if (hps == 0) {
			ShapeInfo inf = getInfo();
			int qual = inf.hasQuality() ? getQuality() : -1;
			hps = inf.getEffectiveHps(getFrameNum(), qual);
		}
		return hps;
	}
	/*
	 *	For objects that can have a quantity, the name is in the format:
	 *		%1/%2/%3/%4
	 *	Where
	 *		%1 : singular prefix (e.g. "a")
	 *		%2 : main part of name
	 *		%3 : singular suffix
	 *		%4 : plural suffix (e.g. "s")
	 */
	/*
	 *	Extracts the first, second and third parts of the name string
	 */
	private static String getSingularName(String name) {
		StringBuffer output_name = new StringBuffer(name.length() + 10);
		int cnt;
		int ind = 0;
		if (name.charAt(0) != '/') {		// Output the first part
			cnt = name.indexOf('/');
			if (cnt < 0)
				cnt = name.length();
			output_name.append(name.substring(0, cnt));
			ind = cnt;
			// If there is a first part it is followed by a space
			output_name.append(' ');
		}
		ind++;
						// Output the second part
		cnt = name.indexOf('/', ind);
		if (cnt < 0)
			cnt = name.length();
		output_name.append(name.substring(ind, cnt));
		ind = cnt + 1;
						// Output the third part
		cnt = name.indexOf('/', ind);
		if (cnt < 0)
			cnt = name.length();
		output_name.append(name.substring(ind, cnt));
		return new String(output_name);
	}
	/*
	 *	Extracts the second and fourth parts of the name string
	 */
	private static String getPluralName(String name, int quantity) {
		StringBuffer output_name = new StringBuffer(name.length() + 20);
		//System.out.println("getPluralName from '" + name + "'");
		output_name.append(String.format("%1$d ", quantity));
		int namelen = name.length();
						// Skip the first part
		int ind = name.indexOf('/');
		if (ind < 0)		// should not happen
			return "?";
		ind++;
						// Output the second part
		int cnt = name.indexOf('/', ind);
		if (cnt < 0)
			cnt = namelen;
		output_name.append(name.substring(ind, cnt));
		ind = cnt + 1;
		// Skip the third part
		cnt = name.indexOf('/', ind);
		if (cnt < 0)
			cnt = namelen;
		ind = cnt + 1;
		if (ind < namelen)
			output_name.append(name.substring(ind, namelen));	// Output the last part
		return new String(output_name);
	}
	public String getName() {
		ShapeInfo info = getInfo();
		int qual = info.hasQuality() && !info.isNpc() ? getQuality() : -1;
		FrameNameInfo nminf = info.getFrameName(getFrameNum(), qual);
		if (nminf != null)
			System.out.println("nminf.frame = " + nminf.getFrame() + 
					", quality = " + nminf.getQuality());
		int shnum = getShapeNum();
		String name;
		String shpname = shnum >= 0 && shnum < ItemNames.names.length 
					? ItemNames.names[shnum] : null;
		int type = nminf != null ? nminf.getType() : -255;
		int msgid = -1;
		if (type == -255  || (msgid = nminf.getMsgid()) >= ItemNames.misc.length)
			name = shpname;
		else if (type < 0)
			return "";	// None.
		else if (type == 0)
			name = ItemNames.misc[msgid];
		else if (!info.hasQuality() && !info.isBodyShape())
			name = shpname;		// Use default name for these.
		else {
			int othermsg = nminf.getOthermsg();
			System.out.println("othermsg = " + othermsg);
			boolean defname = false;
			String msg;
			String other;
			/* +++++++++FINISH
			if (type >= 3) {	// Special names (in SI, corpse, urn).
				int npcnum = -1;
				if (!info.isBodyShape())
					npcnum = getQuality();
				else if (qual == 1)
					npcnum = getLiveNpcNum();
				Actor npc = gwin.getNpc(npcnum);
				if (npc && !npc->is_unused() &&
						(!info.is_body_shape() || npc->get_flag(Obj_flags::met)))
					{
					other = npc->get_npc_name_string();
					if (other.empty())	// No name.
						defname = true;
					else
						msg = misc_names[msgid];
					}
				else	// Default name.
					defname = true;
			}
			else */ {
				msg = ItemNames.misc[msgid];
				other = (othermsg >= 0 && othermsg < ItemNames.misc.length) ?
							ItemNames.misc[othermsg] : shpname;
			}
			if (defname) {
				if (othermsg >= 0 && othermsg < ItemNames.misc.length)
					name = ItemNames.misc[othermsg];
				else if (othermsg < 0 && othermsg != -255)	// None.
					return "";
				else	// Use shape's.
					name = shpname;
			} else if ((type & 1) != 0)
				return other + msg;
			else
				return msg + other;
		}
		int quantity;
		String display_name;
		if (name == null)
			return "";
	    if (ShapeID.getInfo(shnum).hasQuantity())
			quantity = quality & 0x7f;
		else
			quantity = 1;

		// If there are no slashes then it is simpler
		if (name.indexOf('/') == -1) {
			if(quantity <= 1)
				display_name = name;
			else {
				display_name = String.format("%1$d %2$s", quantity, name);
			}
		} else if(quantity <= 1)		// quantity might be zero?
			display_name = getSingularName(name);
		else
			display_name = getPluralName(name, quantity);
		return display_name;
	}
	public String toString() {
		return "obj:" + getName();
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
	public boolean isDragable() {
		return false;
	}
	// Drop another onto this one.
	public boolean drop(GameObject obj) {
		ShapeInfo inf = getInfo();
		int shapenum = getShapeNum();	// It's possible if shapes match.
		if (obj.getShapeNum() != shapenum || !inf.hasQuantity() ||
		    (!inf.hasQuantityFrames() && getFrameNum() != obj.getFrameNum()))
			return false;
		int objq = obj.getQuantity();
		int total_quant = getQuantity() + objq;
		if (total_quant > MAX_QUANTITY)	// Too much?
			return false;
		modifyQuantity(objq);		// Add to our quantity.
		obj.removeThis();		// It's been used up.
		return true;
	}
	public final GameObject getOutermost() {
		GameObject top = this;
		GameObject above;
		while ((above = top.getOwner()) != null)
			top = above;
		return top;
	}
	// Show text by the object.
	public void say(String text) {
		eman.addText(text, this);
	}
	// Show random msg. from 'text.flx'.
	public void say(int from, int to) {
		if (from > to) 
			return;
		int msgnum = from + EUtil.rand()%(to - from + 1);
		if (msgnum < ItemNames.msgs.length)
			say(ItemNames.msgs[msgnum]);
	}
	public int getUsecode() {
		ShapeInfo inf = getInfo();
		FrameUsecodeInfo useinf = inf.getFrameUsecode(
				getFrameNum(), inf.hasQuality() ? getQuality() : -1);
		if (useinf != null) {
			// Shape has frame- or quality-dependent usecode.
			//++++ String ucname = useinf.getUsecodeName();
			int ucid = -1;
			/* +++++++FINISH
			if (ucname.length() > 0)	// Try by name first.
				ucid = ucmachine.findFunction(ucname, true);
			*/
			if (ucid == -1)			// Now try usecode number.
				ucid = useinf.getUsecode();
			if (ucid >= 0)			// Have frame usecode.
				return ucid;
		}
		return ucmachine.getShapeFun(getShapeNum());
	}
	public void activate(int event) {
		/* +++++FINISH
		int gump = getInfo().getGumpShape();
		// Serpent Isle spell scrolls:
		if (gump == 65 && game.isSI()) {
			gumpman.addGump(this, gump, false);
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
	public final boolean isPosInvalid() {
		return chunk == null;
	}
	public final MapChunk getChunk() {
		return chunk;
	}
	public final void setChunk(MapChunk c) {
		chunk = c;
	}					
	// Get frame for desired direction.
	public final int getDirFramenum(int dir, int frnum) {
		return (frnum&0xf) + rotate[dir]; 
	}
	// Get it using current dir.
	public final int getDirFramenum(int frnum) {
		return (frnum&0xf) + (getFrameNum()&(16 | 32));
	}
	public final GameMap getMap() {
		return chunk != null ? chunk.getMap() : null;
	}
	public final int getMapNum() {
		return chunk != null ? chunk.getMap().getNum() : -1;
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
	public final void move(int newtx, int newty, int newlift) {
		move(newtx, newty, newlift, -1);
	}
	public final void move(Tile t) {
		move(t.tx, t.ty, t.tz, -1);
	}
	public void changeFrame(int frnum) {
		gwin.addDirty(this);		// Set to repaint old area.
		setFrame(frnum);
		gwin.addDirty(this);		// Set to repaint new.
	}
	public final boolean swapPositions(GameObject obj2) {
		ShapeInfo inf1 = getInfo();
		ShapeInfo inf2 = obj2.getInfo();
		int frame1 = getFrameNum();
		int frame2 = obj2.getFrameNum();
		if (inf1.get3dXtiles(frame1) != inf2.get3dXtiles(frame2) ||
		    inf1.get3dYtiles(frame1) != inf2.get3dYtiles(frame2))
			return false;		// Not the same size.
		int x1 = getTileX(), y1 = getTileY(), z1 = getLift();
		int x2 = obj2.getTileX(), y2 = obj2.getTileY(), z2 = obj2.getLift();
		removeThis();			// Remove (but don't delete) each.
		setInvalid();
		obj2.removeThis();
		obj2.setInvalid();
		move(x2, y2, z2);	// Move to new locations.
		obj2.move(x1, y1, z1);
		return true;
	}
	//	Step:  Overridden by Actors.
	public boolean step(Tile t, int frame, boolean force) {
		return false;
	} 
	public GameObject findWeaponAmmo(int weapon, int needed, boolean recursive)
			{ return null; }
	/*
	 *	Checks to see if the object has ammo for a weapon.
	 *	Output is ammount of ammo needed and ptr. to ammo
	 *	object, if the argument is not null.
	 */
	public final int getWeaponAmmo(int weapon, int family, int proj, boolean ranged,
				GameObject ammo[], boolean recursive)
		{
		if (ammo != null)
			ammo[0] = null;
		if (weapon < 0)
			return 0;	// Bare hands.
		// See if we need ammo.
		WeaponInfo winf = ShapeID.getInfo(weapon).getWeaponInfo();
		if (winf == null)
			// No ammo needed.
			return 0;
		int uses = winf.getUses();
		int need_ammo = 0;
			// This seems to match perfectly the originals.
		if (family == -1 || !ranged)
			need_ammo = (uses == 0 && winf.usesCharges()) ? 1 : 0;
		else
			need_ammo = 1;
		if (need_ammo != 0 && family >= 0 && proj >= 0) {
			// BG triple crossbows uses 3x ammo.
			ShapeInfo info = ShapeID.getInfo(winf.getProjectile());
			if (info.getReadyType() == Ready.triple_bolts)
				need_ammo = 3;
			}

		if (ammo != null)
			ammo[0] = findWeaponAmmo(weapon, need_ammo, recursive);
		return need_ammo;
	}
	/*
	 *	Being attacked.
	 *
	 *	Output:	Hits taken or < 0 for explosion.
	 */
	public int figureHitPoints
		(
		GameObject attacker,
		int weapon_shape,		// < 0 for readied weapon.
		int ammo_shape,			// < 0 for no ammo shape.
		boolean explosion			// If this is an explosion attacking.
		) {
		return 0; /*++++++++++++FINISH
		WeaponInfo winf;
		AmmoAnfo ainf;

		int wpoints = 0;
		if (weapon_shape >= 0)
			winf = ShapeID::get_info(weapon_shape).get_weapon_info();
		else
			winf = 0;
		if (ammo_shape >= 0)
			ainf = ShapeID::get_info(ammo_shape).get_ammo_info();
		else
			ainf = 0;
		if (!winf && weapon_shape < 0)
			{
			Actor *npc = attacker ? attacker->as_actor() : 0;
			winf = npc ? npc->get_weapon(wpoints) : 0;
			}

		int usefun = -1;
		int type = Weapon_data::normal_damage;
		bool explodes = false;

		if (winf)
			{
			wpoints = winf->get_damage();
			usefun = winf->get_usecode();
			type = winf->get_damage_type();
			explodes = winf->explodes();
			}
		else
			wpoints = 1;	// Give at least one, but only if there's no weapon
		if (ainf)
			{
			wpoints += ainf->get_damage();
				// Replace damage type.
			if (ainf->get_damage_type() != Weapon_data::normal_damage)
				type = ainf->get_damage_type();
			explodes = explodes || ainf->explodes();
			}

		if (explodes && !explosion)	// Explosions shouldn't explode again.
			{	// Time to explode.
			Tile_coord offset(0, 0, get_info().get_3d_height()/2);
			eman->add_effect(new Explosion_effect(get_tile() + offset,
					0, 0, weapon_shape, ammo_shape, attacker));
			return -1;
			}

		int delta = 0;
		int effstr = attacker && attacker->as_actor()
			? attacker->as_actor()->get_effective_prop(Actor::strength) : 0;
		if (winf && (winf->get_powers() & Weapon_data::no_damage) == 0)
			delta = apply_damage(attacker, effstr, wpoints, type);
			
			// Objects are not affected by weapon powers.

			// Object may be in the remove list by this point.
		if (usefun >= 0)
			ucmachine->call_usecode(usefun, this,
						Usecode_machine::weapon);
		return delta;
		*/
	}
	/*
	 *	Being attacked.
	 *
	 *	Output:	0 if destroyed, else object itself.
	 */
	public GameObject attacked(GameObject attacker, int weapon_shape,
			int ammo_shape, boolean explosion) {
		int shnum = getShapeNum();

		if (shnum == 735 && ammo_shape == 722) {
				// Arrows hitting archery practice target.
			int frnum = getFrameNum();
			int newframe = frnum == 0 ? (3*(EUtil.rand()%8) + 1)
					: ((frnum%3) != 0 ? frnum + 1 : frnum);
			changeFrame(newframe);
		}
		int oldhp = getEffectiveObjHp(0);
		int delta = figureHitPoints(attacker, weapon_shape, ammo_shape, explosion);
		int newhp = getEffectiveObjHp(0);
		/*+++++++FINISH
		if (CombatSchedule.combatTrace) {
			String name = "<trap>";
			if (attacker != null)
				name = attacker.getName();
			cout << name << " attacks " << get_name();
			if (oldhp < delta)
				{
				cout << ", destroying it." << endl;
				return 0;
				}
			else if (!delta || oldhp == newhp)
				{	// undamaged/indestructible
				cout << " to no effect." << endl;
				return this;
				}
			else if (delta < 0)
				cout << " causing an explosion." << endl;
			else
				cout << " for " << delta << " hit points, leaving "
					<< newhp << " remaining." << endl;
		}
		*/
		return this;
	}
	/*
	 *	Get frame if rotated 1, 2, or 3 quadrants clockwise.  This is to
	 *	support barges (ship, cart, flying carpet).
	 */
	public int getRotatedFrame
		(
		int quads			// 1=90, 2=180, 3=270.
		) {
		int curframe = getFrameNum();
		return getInfo().getRotatedFrame(curframe, quads);
	}
	public int reduceHealth(int delta, int damage_type, GameObject attacker,
			int exp[]) {
		//+++++++++++FINISH
		return delta;
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
		gwin.getShapeLocation(paintLoc, this);
		paintShape(paintLoc.x, paintLoc.y);
	}
	public void paintOutline(int pix) {
		gwin.getShapeLocation(paintLoc, this);
		paintOutline(paintLoc.x, paintLoc.y, pix);
	}
	public boolean isFindable() { 
		return true; 
	}
	public void writeIreg(OutputStream out) throws IOException {
	}
	public int getIregSize() {
		return 0;
	}
	// Add to NPC 'ready' spot.
	public boolean addReadied(GameObject obj, int index,
				boolean dont_check, boolean force_pos, boolean noset)
		{ return add(obj, dont_check, false, noset); }
	public boolean addReadied(GameObject obj, int index) {
		return addReadied(obj, index, false, false, false);
	}
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
	public void setFlagRecursively(int flag) {
	}
	public Actor asActor() {
		return null;
	}
	public BargeObject asBarge() {
		return null;
	}
	//	Containers should override these.
	public int countObjects(int shapenum, int qual, int framenum) {
		return 0;
	}					// Get contained objs.
	public int getObjects(Vector<GameObject> vec, int shapenum, int qual,
								int framenum) {
		return 0; 
	}
	public void elementsRead() {
	}
	public boolean add(GameObject obj, boolean dont_check,
			boolean combine, boolean noset) {
		return combine ? drop(obj) : false;
	}
	public final boolean add(GameObject obj, boolean dont_check) {
		return add(obj, dont_check, false, false);
	}
	public int addQuantity(int delta, int shapenum, int qual,
								int framenum, boolean dontcreate) {
		return delta; 
	}
	public int createQuantity(int delta, int shapenum, int qual,
						int framenum, boolean temporary) {
		return delta; 
	}
	public int removeQuantity(int delta, int shapenum, int qual, int framenum) {
		return delta;
	} 
	public GameObject findItem(int shapenum, int qual, int framenum) {
		return null; 
	}
	public int getLiveNpcNum() {
		return -1;
	}
	public void deleteContents() {
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

