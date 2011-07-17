package com.exult.android;
import java.util.Vector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.IOException;
import android.graphics.Point;

import android.graphics.Canvas;
import java.util.Calendar;

import com.exult.android.NewFileGump.SaveGameParty;

public class GameWindow extends GameSingletons {
	private static GameWindow instance;
	private EffectsManager effects;	// Manages speciall effects.
	private Vector<GameMap> maps;	// Hold all terrain.
	private GameMap map;			// Current map.
	private GameRender render;
	private TimeQueue tqueue;
	private UsecodeMachine usecode;
	private Rectangle paintBox;		// Temp used for painting.
	private Rectangle clipBox;
	private Rectangle tempDirty;	// Temp for addDirty.
	private Rectangle tempFind;		// For findObject.
	private Point tempPoint = new Point();
	private Tile tempTile = new Tile(), tempTile2 = new Tile();
	private Tile SRTempTile = new Tile();	// For getShapeRect.
	private ImageBuf win;
	private Palette pal;
	// Gameplay objects.
	private MainActor mainActor;
	private Actor cameraActor;		// What to center view on.
	private Vector<Actor> npcs;
	private Vector<Actor.DeadBody> bodies;
	private int numNpcs1;			// Number of type1 NPC's.
	private BargeObject movingBarge;
	// Rendering
	private int scrolltx, scrollty;		// Top-left tile of screen.
	private Rectangle scrollBounds;	// Walking outside this scrolls.
	private boolean painted;			// We updated imagebuf.
	private Rectangle dirty;			// What to display.
	// Options:
	int stepTileDelta = 8;				// Multiplier for the delta in startActor.
	//	Game state values.
	private boolean combat;			// True if in combat.
	private int skipAboveActor;		// Level above actor to skip rendering.
	private int inDungeon;
	private boolean iceDungeon;
	private boolean ambientLight;	// Permanent version of special_light.
	private int specialLight;		// Game minute when light spell ends.
	private int timeStopped;		// For 'stop time' spell.
	int theftWarnings;		// # times warned in current chunk.
	short theftCx, theftCy;	// Chunk where warnings occurred.
	/*
	 *	Public flags and gameplay options:
	 */
	public int skipLift;	// Skip objects with lift >= this.  0
							//   means 'terrain-editing' mode.
	public static GameObject targetObj;	// Currently selected when targeting.
	public static GameObject onObj;		// Object that will receive dragged obj.
	public boolean paintEggs = true;//++++TRUE for testing.
	public int blits;		// For frame-counting.
	public boolean skipFirstScene;
	public boolean armageddon;
	public boolean wizardEye;		// When that spell is in effect.
	public String busyMessage;		// True when doing something we need to wait for.
	public void setBusyMessage(String s) {
		busyMessage = s;
		setAllDirty();
		if (s != null)
			tqueue.pause();
		else
			tqueue.resume();
	}
	static public GameWindow instanceOf() {
		return instance;
	}
	public GameWindow(int width, int height) {
		instance = this;
		maps = new Vector<GameMap>(1);
		map = new GameMap(0);
		render = new GameRender();
		tqueue = new TimeQueue();
		effects = new EffectsManager();
		usecode = new UsecodeMachine();
		maps.add(map);
		win = new ImageBuf(width, height);
		pal = new Palette(win);
		dirty = new Rectangle();
		scrollBounds = new Rectangle();
		paintBox = new Rectangle();
		clipBox = new Rectangle();
		tempDirty = new Rectangle();
		tempFind = new Rectangle();
		GameSingletons.init(this);
		skipLift = 16;
		skipAboveActor = 31;
		theftCx = theftCy = -1;
	}
	/*
	 *	Read any map.  (This is for "multimap" games, not U7.)
	 */
	public GameMap getMap(int num) {
		GameMap newMap;
		if (num >= maps.size())
			maps.setSize(num + 1);
		newMap = maps.elementAt(num);
		if (newMap == null) {
			newMap = new GameMap(num);
			maps.setElementAt(newMap, num);
			newMap.init();
			}
		return newMap;
	}
	public final GameMap getMap() {
		return map;
	}
	public void setMap(int num) {
		map = getMap(num);
		/*
		if (!map)
			abort("Map #d doesn't exist", num);
		*/
		GameSingletons.gmap = map;
	}
	public final Palette getPal() {
		return pal;
	}
	public final EffectsManager getEffects() {
		return effects;
	}
	public final TimeQueue getTqueue() {
		return tqueue;
	}
	public final UsecodeMachine getUsecode() {
		return usecode;
	}
	public final boolean isMoving() {
		return movingBarge != null ? movingBarge.isMoving()
			    : mainActor.isMoving();
	}
	public final boolean inCombat() {
		return combat;
	}
	//	Return party in alist, along with count.  Need room for 9.
	public int getParty(Actor a_list[], boolean avatar_too) {
		int n = 0;
		if (avatar_too && mainActor != null)
			a_list[n++] = mainActor;
		int cnt = partyman.getCount();
		for (int i = 0; i < cnt; i++) {
			int party_member = partyman.getMember(i);
			Actor person = getNpc(party_member);
			if (person != null)
				a_list[n++] = person;
		}
		return n;			// Return # actually stored.
	}
	/*
	 *	Return delay in ticks to expiration (or 1500 (30secs) if indefinite).
	 */
	public final int isTimeStopped() {
		if (timeStopped == 0)
			return 0;
		if (timeStopped == -1)	// Indefinite?
			return 1500;
		int delay = timeStopped - TimeQueue.ticks;
		if (delay > 0)
			return delay;
		timeStopped = 0;	// Done.
		return 0;
	}
	// Delay in ticks, -1 to stop indefinitely, or 0 to end.
	public final void setTimeStopped(int delay) {
		if (delay == -1)
			timeStopped = -1;
		else if (delay == 0)
			timeStopped = 0;
		else {
			int new_expire = TimeQueue.ticks + delay;
			if (new_expire > timeStopped)	// Set expiration time.
				timeStopped = new_expire;
		}
	}
	public void toggleCombat() {
		combat = !combat;
		ExultActivity.setInCombat();
		// Change party member's schedules.
		int newsched = combat ? Schedule.combat : Schedule.follow_avatar;
		int cnt = partyman.getCount();
		for (int i = 0; i < cnt; i++) {
			int party_member = partyman.getMember(i);
			Actor person = getNpc(party_member);
			if (person == null)
				continue;
			int sched = person.getScheduleType();
			if (sched != newsched && sched != Schedule.wait &&
			    sched != Schedule.loiter)
				person.setScheduleType(newsched);
			}
		if (mainActor.getScheduleType() != newsched)
			mainActor.setScheduleType(newsched);
		if (combat) {			// Get rid of flee modes.
			mainActor.readyBestWeapon();
			setMovingBarge(null);	// And get out of barge mode.
			Actor all[] = new Actor[9];
			cnt = getParty(all, true);
			for (int i = 0; i < cnt; i++) {
						// Did Usecode set to flee?
				Actor act = all[i];
				if (act.getAttackMode() == Actor.flee &&
				    !act.didUserSetAttack())
					act.setAttackMode(Actor.nearest, false);
						// And avoid attacking party members,
						//  in case of Usecode bug.
				GameObject targ = act.getTarget();
				if (targ != null && targ.getFlag(GameObject.in_party))
					act.setTarget(null);
				}
			}
		else				// Ending combat.
			CombatSchedule.resume();	// Make sure not still paused.
	}
	public final MainActor getMainActor() {
		return mainActor;
	}
	public final Actor getCameraActor() {
		return cameraActor;
	}
	public final boolean isMainActorInside()
		{ return skipAboveActor < 31 ; }
	public final int getSkipAboveActor() {
		return skipAboveActor;
	}
	// Returns if skip_above_actor changed!
	public final boolean setAboveMainActor(int lift) {
		if (skipAboveActor == lift) 
			return false;
		skipAboveActor = lift;
		return true;
	}
	public boolean setInDungeon(int lift) { 
		if (inDungeon == lift)
			return false;
		inDungeon = lift;
		return true;
	}
	public void setIceDungeon(boolean ice) { iceDungeon = ice; }
	public boolean isInIceDungeon() {
		return iceDungeon;
	}
	public int isInDungeon()
		{ return inDungeon; }
	public boolean isSpecialLight()	// Light spell in effect?
		{ return ambientLight || specialLight != 0; }
				// Light spell.	Light=500, GreatLight=5000.
	void addSpecialLight(int units) {
		if (specialLight == 0) {		// Nothing in effect now?
			specialLight = clock.getTotalMinutes();
			clock.setPalette();
		}
		specialLight += units/20;	// Figure ending time.
	}
	public void toggleAmbientLight(boolean state)
		{ ambientLight = state; }
	
	public final void setMovingBarge(BargeObject b) {
		if (b != null && b != movingBarge) {
			b.gather();		// Will 'gather' on next move.
		if (!b.contains(mainActor))
			b.setToGather();
		} else if (b == null && movingBarge != null)
			movingBarge.done();	// No longer 'barging'.
		movingBarge = b;
	}
	public final BargeObject getMovingBarge() {
		return movingBarge;
	}
	public final int getNumNpcs() {
		return npcs.size();
	}
	public final Actor getNpc(int n) {
		return n >= 0 && n < npcs.size() ? npcs.elementAt(n) : null;
	}
	public void setBody(int npcNum, Actor.DeadBody body) {
		if (bodies == null)
			bodies = new Vector<Actor.DeadBody>(npcNum + 1);
		if (npcNum >= bodies.size())
			bodies.setSize(npcNum + 1);
		bodies.setElementAt(body, npcNum);
	}
	public Actor.DeadBody getBody(int npcNum) {
		return bodies != null && bodies.size() > npcNum ? bodies.elementAt(npcNum)
				: null;
	}
	public final int getRenderSkipLift()	{	// Skip rendering here.
		return skipAboveActor < skipLift ?
				skipAboveActor : skipLift; 
	}
	public final boolean mainActorDontMove() {
		return mainActor != null &&	// Not if map-editing.
		(mainActor.getFlag(GameObject.dont_move) ||
		 mainActor.getFlag(GameObject.dont_render));
	}
	public final boolean mainActorCanAct() {
		return !wizardEye && mainActor.canAct();
	}
	public final void scheduleNpcs(int hour, int backwards, boolean repaint) {
		// Go through npc's, skipping Avatar.
		Iterator<Actor> iter = npcs.iterator();
		iter.next();		// Skip Avatar.
		while (iter.hasNext()) {
			Actor npc = iter.next();
						// Don't want companions leaving.
			if (npc != null && npc.getScheduleType() != Schedule.wait &&
					(npc.getScheduleType() != Schedule.combat ||
						npc.getTarget() == null))
				npc.updateSchedule(hour/3, backwards, hour%3 == 0 ? -1 : 0);
			}

		if (repaint)
			paint();			// Repaint all.
	}
	public final void scheduleNpcs(int hour) {
		scheduleNpcs(hour, 7, true);
	}
	/*
	 *	Tell all npc's to restore some of their HP's and/or mana on the hour.
	 */
	public void mendNpcs() {
						// Go through npc's.
		for (Actor npc : npcs) {
			if (npc != null)
				npc.mendHourly();
		}
	}
	/*
	 *	Get guard shape.
	 */
	private int getGuardShape
		(
		int tx, int ty			// Position to use.
		) {
		if (!game.isSI())			// Default (BG).
			return (0x3b2);
						// Moonshade?
		if (tx >= 2054 && ty >= 1698 &&
		    tx < 2590 && ty < 2387)
			return 0x103;		// Ranger.
						// Fawn?
		if (tx >= 895 && ty >= 1604 &&
		    tx < 1173 && ty < 1960)
			return 0x17d;		// Fawn guard.
		if (tx >= 670 && ty >= 2430 &&
		    tx < 1135 && ty < 2800)
			return 0xe4;		// Pikeman.
		return -1;			// No local guard.
	}
	/*
	 *	Find a witness to the Avatar's thievery.
	 *
	 *	Output:	witness, or NULL.
	 *		closest_npc = closest one that's nearby.
	 */
	private Actor findWitness
		(
		Actor closest_npc[]		// Closest one returned.
		) {
		Vector<GameObject> npcs = new Vector<GameObject>();	// See if someone is nearby.
		mainActor.findNearbyActors(npcs, EConst.c_any_shapenum, 12, 0x28);
		closest_npc[0]= null;		// Look for closest NPC.
		int closest_dist = 5000;
		Actor witness = null;		// And closest facing us.
		int closest_witness_dist = 5000;
		for (GameObject each : npcs) {
			Actor npc = (Actor)each;
			if (npc instanceof MonsterActor || npc.getFlag(GameObject.in_party) ||
			    (npc.getFrameNum()&15) == Actor.sleep_frame ||
			    npc.getNpcNum() >= numNpcs1)
				continue;
			int dist = npc.distance(mainActor);
			if (dist >= closest_witness_dist ||
			    !PathFinder.FastClient.isGrabable(npc, mainActor))
				continue;
						// Looking toward Avatar?
			int dir = npc.getDirection(mainActor);
			int facing = npc.getDirFacing();
			int dirdiff = (dir - facing + 8)%8;
			if (dirdiff < 3 || dirdiff > 5) {		// Yes.
				witness = npc;
				closest_witness_dist = dist;
			}
			else if (dist < closest_dist) {
				closest_npc[0] = npc;
				closest_dist = dist;
			}
		}
		return witness;
	}
	/*
	 *	Handle theft.
	 */
	public void theft() {
						// See if in a new location.
		int cx = mainActor.getCx(), cy = mainActor.getCy();
		if (cx != theftCx || cy != theftCy) {
			theftCx = (short)cx;
			theftCy = (short)cy;
			theftWarnings = 0;
			}
		Actor closest_npc[] = new Actor[1];
		Actor witness = findWitness(closest_npc);
		if (witness == null) {
			if (closest_npc[0] != null && EUtil.rand()%2 != 0)
				closest_npc[0].say(ItemNames.heard_something);
			return;			// Didn't get caught.
			}
		int dir = witness.getDirection(mainActor);
						// Face avatar.
		witness.changeFrame(witness.getDirFramenum(dir, Actor.standing));
		theftWarnings++;
		if (theftWarnings < 2 + EUtil.rand()%3) {			// Just a warning this time.
			witness.say(ItemNames.first_theft, ItemNames.last_theft);
			return;
			}
		gumpman.closeAllGumps(false);	// Get gumps off screen.
		callGuards(witness);
	}
	/*
	 *	Create a guard to arrest the Avatar.
	 */
	public void callGuards
		(
		Actor witness			// .witness, or 0 to find one.
		) {
		
		if (armageddon || inDungeon > 0)
			return;
		if (witness != null || 
				(witness = findWitness(new Actor[1])) != null)
			witness.say(ItemNames.first_call_guards, ItemNames.last_call_guards);
		int gshape = getGuardShape(mainActor.getTileX(), mainActor.getTileY());
		if (gshape < 0) {	// No local guards; lets forward to attack_avatar.
			attackAvatar(0);
			return;
		}
		Tile actloc = new Tile(), dest = new Tile();
		mainActor.getTile(actloc);
		dest.set(actloc.tx + 128, actloc.ty + 128, actloc.tz);
						// Show guard running up.
						// Create it off-screen.
		MonsterActor guard = MonsterActor.create(gshape, dest);
		//++NEEDED add_nearby_npc(guard);
		if (!MapChunk.findSpot(actloc, 5, 
				guard.getShapeNum(), guard.getFrameNum(), 1)) {
			int dir = EUtil.getDirection(dest.ty - actloc.ty,
							actloc.tx - dest.tx);
						
			byte frames[] = new byte[2];	// Use frame for starting attack.
			frames[0] = (byte)guard.getDirFramenum(dir, Actor.standing);
			frames[1] = (byte)guard.getDirFramenum(dir, 3);
			ActorAction action = new ActorAction.Sequence(
					new ActorAction.Frames(frames, 2),
					new ActorAction.Usecode(0x625, guard,
						UsecodeMachine.double_click));
			Schedule.setActionSequence(guard, dest, action, true, 0);
		}
	}
	/*
	 *	Have nearby residents attack the Avatar.
	 */
	void attackAvatar
		(
		int create_guards		// # of extra guards to create.
		) {
		if (armageddon)
			return;
		
		int tx = mainActor.getTileX(), ty = mainActor.getTileY();
		Tile loc = new Tile(tx + 128, ty + 128, mainActor.getLift());
		int gshape = getGuardShape(tx, ty);
		if (gshape >= 0) {
			while (create_guards-- > 0) {
							// Create it off-screen.
				MonsterActor guard = MonsterActor.create(gshape, loc);
				//++NEEDED? add_nearby_npc(guard);
				guard.setTarget(mainActor, true);
				guard.approachAnother(mainActor, false);
			}
		}
		Vector<GameObject> npcs = new Vector<GameObject>();		// See if someone is nearby.
		mainActor.findNearbyActors(npcs, EConst.c_any_shapenum, 20, 0x28);
		for (GameObject each : npcs) {
			Actor npc = (Actor)each;
						// No monsters, except guards; unless no local guards.
			if ((gshape < 0 || npc.getShapeNum() == gshape || 
						!(npc instanceof MonsterActor))
					&& !npc.getFlag(GameObject.in_party))
				npc.setTarget(mainActor, true);
		}
	}
	// Get screen location for an object.
	public final void getShapeLocation(Point loc, int tx, int ty, int tz) {
		int lft = 4*tz;
		tx += 1 - scrolltx;
		ty += 1 - scrollty;
					// Watch for wrapping.
		if (tx < -EConst.c_num_tiles/2)
			tx += EConst.c_num_tiles;
		if (ty < -EConst.c_num_tiles/2)
			ty += EConst.c_num_tiles;
		loc.x = tx*EConst.c_tilesize - 1 - lft;
		loc.y = ty*EConst.c_tilesize - 1 - lft;
	}
	public final void getShapeLocation(Point loc, GameObject obj) {
		getShapeLocation(loc, obj.getTileX(), obj.getTileY(), obj.getLift());
	}
	/*
	 *	Get screen area used by object.
	 */
	public final Rectangle getShapeRect(Rectangle r, GameObject obj) {
		if (obj.getChunk() == null) {		// Not on map?
			Gump gump = gumpman.findGump(obj);
			if (gump != null)
				gump.getShapeRect(r, obj);
			else
				r.set(0, 0, 0, 0);
				return r;
			}
		ShapeFrame s = obj.getShape();
		if (s == null) {
			// This is probably fatal.
			r.set(0,0,0,0);
			return r;
		}
		// Get tile coords.
		obj.getTile(SRTempTile);
		int lftpix = 4*SRTempTile.tz;
		SRTempTile.tx += 1 - scrolltx;
		SRTempTile.ty += 1 - scrollty;
						// Watch for wrapping.
		if (SRTempTile.tx < -EConst.c_num_tiles/2)
			SRTempTile.tx += EConst.c_num_tiles;
		if (SRTempTile.ty < -EConst.c_num_tiles/2)
			SRTempTile.ty += EConst.c_num_tiles;
		return getShapeRect(r, s,
				SRTempTile.tx*EConst.c_tilesize - 1 - lftpix,
				SRTempTile.ty*EConst.c_tilesize - 1 - lftpix);
	}
	public final Rectangle getShapeRect(Rectangle r, ShapeFrame s, int x, int y) {
		r.set(x - s.getXLeft(), y - s.getYAbove(),
			s.getWidth(), s.getHeight());
		return r;
	}
	/*
	 *	Get the 'flat' that a screen point is in.
	 */
	public ShapeID getFlat
		(
		ShapeID id,				// What to set, or NULL.
		int x, int y			// Window point.
		) {
		int tx = (scrolltx + x/EConst.c_tilesize)%EConst.c_num_tiles;
		int ty = (scrollty + y/EConst.c_tilesize)%EConst.c_num_tiles;
		int cx = tx/EConst.c_tiles_per_chunk, cy = ty/EConst.c_tiles_per_chunk;
		tx = tx%EConst.c_tiles_per_chunk;
		ty = ty%EConst.c_tiles_per_chunk;
		MapChunk chunk = map.getChunk(cx, cy);
		if (id == null)
			id = new ShapeID();
		ChunkTerrain ter = chunk.getTerrain();
		if (ter != null)
			chunk.getTerrain().getFlat(id, tx, ty);
		else
			id.setShape(-1);
		return id;
		}
	public void getWinTileRect(Rectangle r) {
		r.set(getScrolltx(), getScrollty(),
				(win.getWidth() + EConst.c_tilesize - 1)/EConst.c_tilesize,
				(win.getHeight() + EConst.c_tilesize - 1)/EConst.c_tilesize);
	}
	public final int getScrolltx() {
		return scrolltx;
	}
	public final int getScrollty() {
		return scrollty;
	}
	public final void setScrolls(int newscrolltx, int newscrollty) {
		scrolltx = newscrolltx;
		scrollty = newscrollty;
						// Set scroll box.
						// Let's try 2x2 tiles.
		scrollBounds.w = scrollBounds.h = 2;
		scrollBounds.x = scrolltx + 
				(getWidth()/EConst.c_tilesize - scrollBounds.w)/2;
		// OFFSET HERE
		scrollBounds.y = scrollty + 
				((getHeight())/EConst.c_tilesize - scrollBounds.h)/2;
		BargeObject oldActiveBarge = movingBarge;
		map.readMapData();		// This pulls in objects.
						// Found active barge?
		if (oldActiveBarge == null && movingBarge != null) {	// Do it right.
			BargeObject b = movingBarge;
			movingBarge = null;
			setMovingBarge(b);
		}
						// Set where to skip rendering.
		int cx = cameraActor.getCx(), cy = cameraActor.getCy();	
		MapChunk nlist = map.getChunk(cx, cy);
		int tx = cameraActor.getTx(), ty = cameraActor.getTy();
		setAboveMainActor(nlist.isRoof (tx, ty,
							cameraActor.getLift()));
		setInDungeon(nlist.hasDungeon()?nlist.isDungeon(tx, ty):0);
		setIceDungeon(nlist.isIceDungeon(tx, ty));
	}
	public final void centerView(Tile t) {
		// OFFSET HERE
		int zoff = t.tz/2;
		int tw = getWidth()/EConst.c_tilesize, 
			th = (getHeight())/EConst.c_tilesize;
		setScrolls(EConst.DECR_TILE(t.tx, tw/2 + zoff), EConst.DECR_TILE(t.ty, th/2 + zoff));
		setAllDirty();
	}
	public boolean scrollIfNeeded(Tile t) {
		boolean scrolled = false;
		// 1 lift = 1/2 tile.
		int tx = t.tx - t.tz/2, ty = t.ty - t.tz/2;
		if (Tile.gte(EConst.DECR_TILE(scrollBounds.x), tx)) {
			shiftViewHoriz(true);
			scrolled = true;
		} else if (Tile.gte(tx, (scrollBounds.x + scrollBounds.w)%EConst.c_num_tiles)) {
			shiftViewHoriz(false);
			scrolled = true;
		}
		if (Tile.gte(EConst.DECR_TILE(scrollBounds.y), ty)) {
			shiftViewVertical(true);
			scrolled = true;
		} else if (Tile.gte(ty, (scrollBounds.y + scrollBounds.h)%EConst.c_num_tiles)) {
			shiftViewVertical(false);
			scrolled = true;
		}
		return (scrolled);	
	}
	public boolean scrollIfNeeded(Actor a, Tile t) {
		if (a != cameraActor)
			return false;
		else
			return scrollIfNeeded(t);
	}
	//	Center around given tile pos.
	public void centerView(int tx, int ty, int tz) {
		int tw = win.getWidth()/EConst.c_tilesize, th = (win.getHeight())/EConst.c_tilesize;
		setScrolls(EConst.DECR_TILE(tx, tw/2 + tz/2), EConst.DECR_TILE(ty, th/2 + tz/2));
		setAllDirty();
	}
	/*
	 *	Shift view by one tile.  Use 'nopaint = true' if there are thread issues.
	 */
	public void shiftViewHoriz(boolean toLeft) {
		shiftViewHoriz(toLeft, false);
	}
	public void shiftViewHoriz(boolean toleft, boolean nopaint) {
		int w = getWidth(), h = getHeight();
		if (toleft) {
			scrolltx = EConst.DECR_TILE(scrolltx);
			scrollBounds.x = EConst.DECR_TILE(scrollBounds.x);
		} else {
						// Get current rightmost chunk.
			scrolltx = EConst.INCR_TILE(scrolltx);
			scrollBounds.x = EConst.INCR_TILE(scrollBounds.x);
		}
		map.readMapData();		// Be sure objects are present.
		if (nopaint || gumpman.showingGumps()) {		// Gump on screen?
			setAllDirty();
			return;
		}
		
		synchronized(win) {
			mouse.hide();
			if (toleft) {			// Shift image to right.
				win.copy(0, 0, w - EConst.c_tilesize, h, EConst.c_tilesize, 0);
				paint(0, 0, EConst.c_tilesize, h);
				dirty.x += EConst.c_tilesize;
			} else { 				// Shift image to left.
				win.copy(EConst.c_tilesize, 0, w - EConst.c_tilesize, h, 0, 0);
						// Paint 1 column to right.
				paint(w - EConst.c_tilesize, 0, EConst.c_tilesize, h);
				dirty.x -= EConst.c_tilesize;	// Shift dirty rect.
			}
		}
		clipToWin(dirty);
	}
	public void shiftViewVertical(boolean up) {
		shiftViewVertical(up, false);
	}
	public void shiftViewVertical(boolean up, boolean nopaint) {
		int w = getWidth(), h = getHeight();
		if (up) {
			scrollty = EConst.DECR_TILE(scrollty);
			scrollBounds.y = EConst.DECR_TILE(scrollBounds.y);
		} else {
						// Get current bottomost chunk.
			scrollty = EConst.INCR_TILE(scrollty);
			scrollBounds.y = EConst.INCR_TILE(scrollBounds.y);
		}
		map.readMapData();		// Be sure objects are present.
		if (nopaint || gumpman.showingGumps()) {			// Gump on screen?
			setAllDirty();
			return;
		}
		synchronized(win) {
			mouse.hide();
			if (up) {
				win.copy(0, 0, w, h - EConst.c_tilesize, 0, EConst.c_tilesize);
				paint(0, 0, w, EConst.c_tilesize);
				dirty.y += EConst.c_tilesize;		// Shift dirty rect.
			} else {
				win.copy(0, EConst.c_tilesize, w, h - EConst.c_tilesize, 0, 0);
				paint(0, h - EConst.c_tilesize, w, EConst.c_tilesize);
				dirty.y -= EConst.c_tilesize;		// Shift dirty rect.
			}
		}
		clipToWin(dirty);
	}
	public void shiftWizardEye(int mx, int my) {
		// Figure dir. from center.
		int cx = getWidth()/2, cy = getHeight()/2;
		int dy = cy - my, dx = mx - cx;
		int dir = EUtil.getDirection(dy, dx);
		final int deltas[] = {0,-1, 1,-1, 1,0, 1,1, 0,1, 
							-1,1, -1,0, -1,-1};
		int dirx = deltas[2*dir], diry = deltas[2*dir + 1];
		for (int i = 0; i < 4; ++i) {
			if (dirx == 1)
				shiftViewHoriz(false, true);
			else if (dirx == -1)
				shiftViewHoriz(true, true);
			if (diry == 1)
				shiftViewVertical(false, true);
			else if (diry == -1)
				shiftViewVertical(true, true);
		}
	}
	/*
	 *	Set actor to center view around.
	 */
	public void setCameraActor(Actor a) {
		if (a == mainActor &&		// Setting back to main actor?
		    cameraActor != null &&		// Change in chunk?
		    (cameraActor.getCx() != mainActor.getCx() ||
		     cameraActor.getCy() != mainActor.getCy()))
		    				// Cache out temp. objects.
			//++++FINISH emulateCache(cameraActor.getChunk(), mainActor.getChunk());
		cameraActor = a;
		setCenter();
	}
	//	Re-center around camera actor.
	public void setCenter() {
		centerView(cameraActor.getTileX(), cameraActor.getTileY(), cameraActor.getLift());
		setAllDirty();
	}
	/*
	 * 	Start walking using pathfinder.
	 */
	public void startActorAlongPath(int winx, int winy, int speed) {
		if (mainActor.getFlag(GameObject.asleep) ||
		    mainActor.getFlag(GameObject.paralyzed) ||
		    mainActor.getScheduleType() == Schedule.sleep ||
		    movingBarge != null)		// For now, don't do barges.
			return;			// Zzzzz....
							// Animation in progress?
			int lift = mainActor.getLift();
			int liftpixels = 4*lift;	// Figure abs. tile.
			Tile dest = tempTile;
			dest.set(getScrolltx() + (winx + liftpixels)/EConst.c_tilesize,
					 getScrollty() + (winy + liftpixels)/EConst.c_tilesize, lift);
			if (!mainActor.walkPathToTile(dest, speed, 0, 0))
				System.out.println("Couldn't find path for Avatar.");
			else
				mainActor.getFollowers();
			}
	/*
	 * Start stepping towards given point.
	 */
	private void startActorSteps
		(
		int fromx, int fromy,// Mouse position to use as start.
		int winx, int winy,	// Mouse position to aim for.
		int speed			// Ticks between frames.
		) {
		mainActor.getTile(tempTile);
		Tile start = tempTile;
		int dir;
		Tile dest = tempTile2;
		dir = EUtil.getDirection (fromy - winy, winx - fromx);
		int tflags = mainActor.getTypeFlags();
		start.getNeighbor(dest, dir);
		if (!mainActor.areaAvailable(dest, start, tflags)) {
			start.getNeighbor(dest, (dir+1)%8);
			if (mainActor.areaAvailable(dest, start, tflags))
				dir = (dir+1)%8;
			else {
				start.getNeighbor(dest, (dir+7)%8);
				if (mainActor.areaAvailable(dest, start, tflags))
					dir = (dir+7)%8;
				else
					dir = -1;
			}
		}
		if (dir == -1) {
			stopActor();
			return;
		}
			
		/* ++++++++++++FINISH
		if (dir == -1)
		{
	   		Game_object *block = mainActor.is_moving() ? 0
			: mainActor.find_blocking(start.get_neighbor(dir), dir);
			// We already know the blocking object isn't the avatar, so don't
			// double check it here.
			if (!block || !block.move_aside(mainActor, dir))
				{
				stop_actor();
				if (mainActor.get_lift()%5)// Up on something?
				{	// See if we're stuck in the air.
					int savetz = start.tz;
					if (!Map_chunk.is_blocked(start, 1, 
						MOVE_WALK, 100) && 
						start.tz < savetz)
						mainActor.move(start.tx, start.ty, 
								start.tz);
				}
				return;
				}
			}
		 */
		int delta = stepTileDelta*EConst.c_tilesize;// Bigger # here avoids jerkiness,
												//   but causes probs. with followers.
		switch (dir) {
		case EConst.north:
			fromy -= delta;
			break;
		case EConst.northeast:
			fromy -= delta;
			fromx += delta;
			break;
		case EConst.east:
			fromx += delta;
			break;
		case EConst.southeast:
			fromy += delta;
			fromx += delta;
			break;
		case EConst.south:
			fromy += delta;
			break;
		case EConst.southwest:
			fromy += delta;
			fromx -= delta;
			break;
		case EConst.west:
			fromx -= delta;
			break;
		case EConst.northwest:
			fromy -= delta;
			fromx -= delta;
			break;
		}
		int lift = mainActor.getLift();
		int liftpixels = 4*lift;	// Figure abs. tile.
		int tx = scrolltx + (fromx + liftpixels)/EConst.c_tilesize,
	    	ty = scrollty + (fromy + liftpixels)/EConst.c_tilesize;
					// Wrap:Game_window.start_actor
		tx = (tx + EConst.c_num_tiles)%EConst.c_num_tiles;
		ty = (ty + EConst.c_num_tiles)%EConst.c_num_tiles;
		tempTile.set(tx, ty, lift);
		mainActor.walkToTile(tempTile, speed, 0);
		if (mainActor.getAction() != null)
			mainActor.getAction().setGetParty(true);
	}
	public void teleportParty(Tile t, boolean skipEggs, int newMap) {
		Tile oldpos = tempTile;
		mainActor.getTile(oldpos);
		mainActor.setAction(null);	// Definitely need this, or you may
						//   step back to where you came from.
		movingBarge = null;		// Calling 'done()' could be risky...
		PartyManager party_man = GameSingletons.partyman;
		int i, cnt = party_man.getCount();
		if (newMap != -1)
			setMap(newMap);
		mainActor.move(t.tx, t.ty, t.tz, newMap);	// Move Avatar.
		// Fixes a rare crash when moving between maps and teleporting:
		newMap = mainActor.getMapNum();
		centerView(t);			// Bring pos. into view, and insure all
		clock.reset();			// Reset and re-display palette.
		clock.setPalette();
		Tile t1 = new Tile();
		for (i = 0; i < cnt; i++) {
			int party_member=party_man.getMember(i);
			Actor person = getNpc(party_member);
			if (person != null && !person.isDead() && 
			    person.getScheduleType() != Schedule.wait ) {
				person.setAction(null);
				t1.set(t);
				if (MapChunk.findSpot(t1, 8,
					person.getShapeNum(), person.getFrameNum(), 1))
					person.move(t1.tx, t1.ty, t1.tz, newMap);
			}
		}
		mainActor.getFollowers();
		if (!skipEggs)			// Check all eggs around new spot.
			MapChunk.tryAllEggs(mainActor, t.tx, t.ty, t.tz,
						oldpos.tx, oldpos.ty);
		/* NEEDED? generate mousemotion event
		int x, y;
		SDL_GetMouseState(&x, &y);
		SDL_WarpMouse(x, y);
		*/
	}
	/*
	 *	Start the actor.
	 */
	public void startActor
		(
		int fromx, int fromy,	// Mouse position to use as start.
		int tox, int toy, // Mouse position to aim for.
		int speed			// Ticks between frames.
		) {
		if (mainActor.getFlag(GameObject.asleep) ||
				mainActor.getFlag(GameObject.paralyzed) ||
				mainActor.inUsecodeControl() || 
				mainActor.getScheduleType() == Schedule.sleep)
			return;			// Zzzzz....
		
		if (gumpman.gumpMode())
			return;
		if (movingBarge != null) {	// Want to move center there.
			int lift = mainActor.getLift();
			int liftpixels = 4*lift;	// Figure abs. tile.
			int tx = getScrolltx() + (tox + liftpixels)/EConst.c_tilesize,
		    ty = getScrollty() + (toy + liftpixels)/EConst.c_tilesize;
					// Wrap:
			tx = (tx + EConst.c_num_tiles)%EConst.c_num_tiles;
			ty = (ty + EConst.c_num_tiles)%EConst.c_num_tiles;
			Tile atile = movingBarge.getCenter();
			int bx = movingBarge.getTileX(), by = movingBarge.getTileY(),
				bz = movingBarge.getLift();
					// Go faster than walking.
			tempTile.set(tx + bx - atile.tx, 
				   ty + by - atile.ty, bz);
			movingBarge.travelToTile(tempTile, 1);
		} else {
			// Set schedule.
			int sched = mainActor.getScheduleType();
			if (sched != Schedule.follow_avatar &&
					sched != Schedule.combat &&
					!mainActor.getFlag(GameObject.asleep))
				mainActor.setScheduleType(Schedule.follow_avatar);
			startActorSteps(fromx, fromy, tox, toy, speed);
		}
	}
	public final void stopActor() {
		if (movingBarge != null)
			movingBarge.stop();
		else {
			mainActor.stop();	// Stop and set resting state.
			if (!gumpman.gumpMode())
					mainActor.getFollowers();
		}
	}
	/*
	 *	Find the top object that can be selected, dragged, or activated.
	 *	The one returned is the 'highest'.
	 */
	public GameObject findObject(int x, int y) {
		int not_above = getRenderSkipLift();
		// Figure chunk #'s.
		int start_cx = ((scrolltx + 
				x/EConst.c_tilesize)/EConst.c_tiles_per_chunk)%EConst.c_num_chunks;
		int start_cy = ((scrollty + 
				y/EConst.c_tilesize)/EConst.c_tiles_per_chunk)%EConst.c_num_chunks;
		// Check 1 chunk down & right too.
		int stop_cx = (2 + (scrolltx + 
				(x + 4*not_above)/EConst.c_tilesize)/EConst.c_tiles_per_chunk)%EConst.c_num_chunks;
		int stop_cy = (2 + (scrollty + 
				(y + 4*not_above)/EConst.c_tilesize)/EConst.c_tiles_per_chunk)%EConst.c_num_chunks;

		GameObject best = null;		// Find 'best' one.
		boolean trans = true;		// Try to avoid 'transparent' objs.
		// Go through them.
		for (int cy = start_cy; cy != stop_cy; cy = EConst.INCR_CHUNK(cy))
			for (int cx = start_cx; cx != stop_cx; cx = EConst.INCR_CHUNK(cx)) {
				MapChunk olist = map.getChunk(cx, cy);
				if (olist == null)
					continue;
				ObjectList.ObjectIterator iter = olist.getObjects().getIterator();
				GameObject obj;
				while ((obj = iter.next()) != null) {
					if (obj.getLift() >= not_above)
						continue;
					getShapeRect(tempFind, obj);
					if (!tempFind.hasPoint(x, y) ||
						!obj.isFindable())
						continue;
					// Check the shape itself.
					ShapeFrame s = obj.getShape();
					getShapeLocation(tempPoint, obj);
					if (!s.hasPoint(x - tempPoint.x, y - tempPoint.y))
						continue;
					// Fixes key under rock in BG at [915, 2434, 0]; need to
					// know if there are side effects.
					if (best == null || best.lt(obj) == 1 || trans) {
						boolean ftrans = obj.getInfo().isTransparent();
						if (!ftrans || trans) {
							best = obj;
							trans = ftrans;
						}
					}
				}
			}
		return (best);
	}
	/*
	 *	Show the name of the item the mouse is clicked on.
	 */
	public final void showItems
		(
		int x, int y			// Coords. in window.
		) {
		GameObject obj;
						// Look for obj. in open gump.
		Gump gump = gumpman.findGump(x, y);
		if (gump != null) {
			obj = gump.findObject(x, y);
			/* ++NEEDED?
			if (obj == null) 
				obj = gump.get_cont_or_actor(x, y);
			*/
		} else				// Search rest of world.
			obj = findObject(x, y);
		/*
						// All other cases:  unselect.
			cheat.clear_selected();	
		*/
			// Do we have an NPC?
		//UNUSED Actor npc = obj != null && obj instanceof Actor ? (Actor) obj : null;
		/* ++CHEAT stuff went here. */
		if (obj != null) {			// Show name.
			System.out.printf("Found '%1$s'(%2$d:%3$d) at (%4$h, %5$h, %6$h)\n",
					obj.getName(),
					obj.getShapeNum(), obj.getFrameNum(),
					obj.getTileX(), obj.getTileY(), obj.getLift());
			if (obj instanceof Actor)
				System.out.printf("Npc #%1$d, sched=%2$d\n",
					((Actor)obj).getNpcNum(), ((Actor)obj).getScheduleType());
			showObjName(obj);
		}
		/*
		// If it's an actor and we want to grab the actor, grab it.
		if (npc != null && cheat.grabbing_actor() && 
		    (npc.get_npc_num() || npc==mainActor))
			cheat.set_grabbed_actor (npc);
		*/
	}
	void showObjName(GameObject obj) {
		// ++++ String namestr = Get_object_name(obj);
		// Combat and an NPC?
		/* ++MAYBE LATER
		if (in_combat() && Combat.mode != Combat.original && npc)
		{
		char buf[128];
		sprintf(buf, "%s (%d)", objname, 
				npc.getProperty(Actor.health));
		objname = &buf[0];
		}
		 */
		effects.addText(/* objname*/ obj.getName() , obj);
	}
	/*
	 *	Handle a double-click.
	 */

	public void doubleClicked
		(
		int x, int y			// Coords in window.
		)
		{
						// Animation in progress?
		if (mainActorDontMove())
			return;
						// Look for obj. in open gump.
		GameObject obj = null;
		Gump gump = gumpman.findGump(x, y);
		boolean avatar_can_act = mainActorCanAct();
		if (gump != null) {
			obj = gumpman.doubleClicked(gump, x, y);
		// If gump manager didn't handle it, we search the world for an object
		} else {
			obj = findObject(x, y);
			/* ++FINISH?
			if (!avatar_can_act && obj != null && obj.asActor()
			    	&& obj.asActor() == mainActor)
				{
				ActionFileGump(0);
				return;
				}
			*/
			// Check path, except if an NPC, sign, or if editing.
			if (obj != null && obj.asActor() == null &&
				!cheat.inHackMover() &&
				//!Is_sign(obj.get_shapenum()) &&
				!PathFinder.FastClient.isGrabable(mainActor, obj)) {
				mouse.flashShape(Mouse.blocked);
				return;
			}
		}
		if (obj == null || !avatar_can_act) {
			//startActorAlongPath(x, y, 1);	// Experiment.
			return;			// Nothing found or avatar disabled.
		}
		System.out.println("Double-clicked on shape " + obj.getShapeNum() +
				":  " + obj.getName());

		if (combat && gump != null &&		// In combat?
		    !CombatSchedule.isPaused() &&
		    (!gumpman.gumpMode())) {
			Actor npc = obj.asActor();
						// But don't attack party members.
			if ((npc == null || !npc.getFlag(GameObject.in_party)) &&
						// Or bodies.
					!obj.getInfo().isBodyShape()) {	// In combat mode.
				// Want everyone to be in combat.
				combat = false;
				mainActor.setTarget(obj);
				toggleCombat();
				return;
				}
		}
		effects.removeTextEffects();	// Remove text msgs. from screen.
		usecode.initConversation();
		obj.activate();
		//+++++++  npc_prox.wait(4);		// Delay "barking" for 4 secs.
	}

	
	/*
	 * 	Rendering:
	 */
	public ImageBuf getWin() {
		return win;
	}
	public final int getWidth() {
		return win.getWidth();
	}
	public final int getHeight() {
		return win.getHeight();
	}
	public final void setPainted()
		{ painted = true; }
	public final boolean wasPainted()
		{ return painted; }
	public boolean show(Canvas c, boolean force) {	// Returns true if blit occurred.
		if (painted || force) {
			win.show(c);
			++blits;
			painted = false;
			return true;
		}
		return false;
	}
	public void setAllDirty() {
		dirty.set(0, 0, win.getWidth(), win.getHeight());
	}
	public void clearDirty()		// Clear dirty rectangle.
		{ dirty.w = 0; }
	public boolean isDirty()
		{ return dirty.w > 0; }
	public void addDirty(Rectangle r) {	// Add rectangle to dirty area.
		if (dirty.w > 0)
			dirty.add(r);
		else
			dirty.set(r);
	}
				// Add dirty rect. for obj. Rets. false
				//   if not on screen.
	public boolean addDirty(GameObject obj) {
		getShapeRect(tempDirty, obj);
		tempDirty.enlarge(1+EConst.c_tilesize/2);
		clipToWin(tempDirty);
		if (tempDirty.w > 0 && tempDirty.h > 0) {
			addDirty(tempDirty);
			return true;
		} else
			return false;
	}
	public void paint(int x, int y, int w, int h) {
		// if (!win.ready()) return;
		//System.out.println("Paint: " + x + ", " + y + ", " + w + ", " + h);
		int gx = x, gy = y, gw = w, gh = h;
		if (gx < 0) { gw+=x; gx = 0; }
		if ((gx+gw) > win.getWidth()) gw = win.getWidth()-gx;
		if (gy < 0) { gh += gy; gy = 0; }
		if ((gy+gh) > win.getHeight()) gh = win.getHeight()-gy;
		synchronized(win) {
			win.setClip(gx, gy, gw, gh);	// Clip to this area.
	
			int light_sources = 0;

			// if (mainActor) render.paint_map(gx, gy, gw, gh);
			// else 
			//	win.fill8(0);
			if (mainActor != null)
				render.paintMap(gx, gy, gw, gh);
			else
				win.fill8((byte)0);
			effects.paint();		// Draw sprites.
			if (wizardEye)
				GameRender.paintWizardEye();
			gumpman.paint(false);
			if (drag != null) 
				drag.paint();	// Paint what user is dragging.
			effects.paintText();
			gumpman.paint(true);
			Conversation conv = GameSingletons.conv;
			if (conv != null)
				conv.paint();		// Conversation.
			paintBusy();			// 'busyMessage'.
					// Complete repaint?
			if (gx == 0 && gy == 0 && gw == getWidth() && gh == getHeight() && 
										mainActor != null) {	// Look for lights.
				int cnt = partyman.getCount();
				boolean carried_light = mainActor.hasLightSource();
				for (int i = 0; !carried_light && i < cnt; i++) {
					Actor npc = npcs.elementAt(partyman.getMember(i));
					if (npc != null)
						carried_light = npc.hasLightSource();
				}
					// Also check light spell.
				if (specialLight != 0 && clock.getTotalMinutes() > specialLight) {
						// Just expired.
					specialLight = 0;
					clock.setPalette();
				}
					// Set palette for lights.
			clock.setLightSource((carried_light?1:0) + ((light_sources > 0)?1:0),
								inDungeon);
			}
		win.clearClip();
		} // End 'synchronized'.
	}	
	public void paint(Rectangle r)
		{ paint(r.x, r.y, r.w, r.h); }
	// Paint just the map with given top-left-corner tile.
	public void paintMapAtTile(int x, int y, int w, int h, 
					int toptx, int topty, int skip_above) {
		synchronized (win) {
			int savescrolltx = scrolltx, savescrollty = scrollty;
			int saveskip = skipLift;
			scrolltx = toptx;
			scrollty = topty;
			skipLift = skip_above;
			map.readMapData();		// Gather in all objs., etc.
			win.setClip(x, y, w, h);
			render.paintMap(0, 0, getWidth(), getHeight());
			win.clearClip();
			scrolltx = savescrolltx;
			scrollty = savescrollty;
			skipLift = saveskip;
		}
	}
	public void paintBusy() {
		if (busyMessage != null) {
			int text_height = fonts.getTextHeight(0);
			int text_width = fonts.getTextWidth(0, busyMessage);
			
			fonts.paintText(0, busyMessage, getWidth()/2-text_width/2, 
									getHeight()/2-text_height);
		}
	}
	// Clip 'r' to window.
	public void clipToWin(Rectangle r) {
		clipBox.set(0, 0, win.getWidth(), win.getHeight());
		r.intersect(clipBox);
	}
	public void paintDirty() {
		// Update the gumps before painting, unless in dont_move mode (may change dirty area)
	    if (!mainActorDontMove())
	        gumpman.updateGumps();
		effects.updateDirtyText();
		paintBox.set(dirty);
		clipToWin(paintBox);
		//System.out.printf("paintDirty: x = %1$d, w = %2$d\n", dirty.x, dirty.w);
		if (paintBox.w > 0 && paintBox.h > 0)
			paint(paintBox);	// (Could create new dirty rects.)
		clearDirty();
	}
	//	Paint whole window.
	public void paint() {
		// if (mainActor != 0) map.read_map_data();		// Gather in all objs., etc.
		setAllDirty();
		paintDirty();
		}
	/*
	 * Save/restore/startup.
	 */
	public void initActors() throws IOException {
		if (mainActor != null) {		// Already done?
			game.clearAvName();
			game.clearAvSex();
			game.clearAvSkin();
			return;
		}
		readNpcs();			// Read in all U7 NPC's.

		// Was a name, sex or skincolor set in Game
		// this bascially detects 
		boolean changed = game.isNewGame();

		if (game.getAvSex() == 0 || game.getAvSex() == 1 || game.getAvName() != null
				|| (game.getAvSkin() >= 0 && game.getAvSkin() <= 2))
			changed = true;
		game.clearAvName();
		game.clearAvSex();
		game.clearAvSkin();
		// Update gamedat if there was a change
		if (changed) {
			scheduleNpcs(6,7,false);
			writeNpcs();
		}
	}
	public void initFiles(boolean cycle) {
		ShapeID.loadStatic();
		tqueue.add(TimeQueue.ticks, clock, this);	// Start clock.
	}
	//	Prepare for game.
	public void setupGame() {
		System.out.println("setupGame: at start");
		// FOR NOW:  Unpack INITGAME if not already done.
		if (EUtil.U7exists(EFile.IDENTITY) == null)
			initGamedat(true);
		getMap(0).init();
		pal.set(Palette.PALETTE_DAY, -1, null);//+++++ALSO for testing.
		try {
			initActors();		// Set up actors if not already done.
								// This also sets up initial 
								// schedules and positions.
		} catch (IOException e) {
			System.out.println("FAILED to read NPCs!");
		}
		System.out.println("setupGame: finished initActors");
		// CYCLE_RED_PLASMA();
		/* ++++++++++FINISH 
		Notebook_gump.initialize();		// Read in journal.
		*/
		usecode.read();		// Read the usecode flags
		/*
		CYCLE_RED_PLASMA();
		*/
		if (game.isBG()) {
			/* +++++++FINISH
			string yn;		// Override from config. file.
						// Skip intro. scene?
			config.value("config/gameplay/skip_intro", yn, "no");
			if (yn == "yes")
				usecode.set_global_flag(
					Usecode_machine.did_first_scene, 1);
			*/
			if (skipFirstScene)
				usecode.setGlobalFlag(UsecodeMachine.did_first_scene, 1);
						// Should Avatar be visible?
			if (usecode.getGlobalFlag(UsecodeMachine.did_first_scene))
				mainActor.clearFlag(GameObject.bg_dont_render);
			else
				mainActor.setFlag(GameObject.bg_dont_render);
		}
		/* +++++++++++FINISH
		CYCLE_RED_PLASMA();

		// Fade out & clear screen before palette change
		pal.fade_out(c_fade_out_time);
		clear_screen(true);
	#ifdef RED_PLASMA
		load_palette_timer = 0;
	#endif
		*/
		// note: we had to stop the plasma here already, because init_readied
		// and activate_eggs may update the screen through usecode functions
		// (Helm of Light, for example)
		mainActor.initReadied();
		int cnt = partyman.getCount();	// Get entire party.
		for (int i = 0; i < cnt; i++) {	// Init. rings.
			getNpc(partyman.getMember(i)).initReadied();
		}
		timeStopped = 0;
	//+++++The below wasn't prev. done by .read(), so maybe it should be
	//+++++controlled by a 'first-time' flag.
		System.out.println("setupGame: about to activate eggs");
						// Want to activate first egg.
		MapChunk olist = mainActor.getChunk();
		int tx = mainActor.getTileX(), ty = mainActor.getTileY(), tz = mainActor.getLift();
		System.out.printf("setupGame: Avatar is at %1$d, %2$d, %3$d\n", tx, ty, tz);
		// Do them immediately.
		olist.activateEggs(mainActor, tx, ty, tz, -1,-1,true);
		// Force entire repaint.
		setAllDirty();
		painted = true;			// Main loop uses this.
		gumpman.closeAllGumps(true);		// Kill gumps.
		/*+++++++++FINISH
		Face_stats.load_config(config);
		*/
		// Set palette for time-of-day.
		clock.reset();
		clock.setPalette();
		pal.fade(6, true, -1);		// Fade back in.
		System.out.println("setupGame: done");
	}
	public void readNpcs() throws IOException {
		npcs = new Vector<Actor>(1);			// Create main actor.
		cameraActor = mainActor = new MainActor("", 0);
		npcs.add(mainActor);
		InputStream nfile = EUtil.U7openStream(EFile.NPC_DAT);
		int numNpcs;
		//UNUSED boolean fix_unused = false;	// Get set for old savegames.
		numNpcs1 = EUtil.Read2(nfile);	// Get counts.
		numNpcs = numNpcs1 + EUtil.Read2(nfile);
		mainActor.read(nfile, 0, false);
		npcs.setSize(numNpcs);
		if (bodies != null)
			bodies.setSize(numNpcs);
		int i;
		centerView(mainActor.getTileX(), mainActor.getTileY(), mainActor.getLift());
		for (i = 1; i < numNpcs; i++) {	// Create the rest.
			Actor actor = new NpcActor("", 0);
			npcs.set(i, actor);
			actor.read(nfile, i, i < numNpcs1);
			if (actor.isUnused()) {		// Not part of the game.
				actor.removeThis();
				actor.setScheduleType(Schedule.wait);
			} else
				actor.restoreSchedule();
			//+++++++CYCLE_RED_PLASMA();
		}
		nfile.close();
		mainActor.setActorShape();
		nfile = EUtil.U7openStream(EFile.MONSNPCS);	// Monsters.
		if (nfile != null) try {
			PushbackInputStream nfile2 = new PushbackInputStream(nfile);
			
			// (Won't exist the first time; in this case U7open throws
			int cnt = EUtil.Read2(nfile2);
			while (cnt-- > 0) {
						// Read ahead to get shape.
				nfile2.skip(2);
				int shnum = (int)EUtil.Read2(nfile2)&0x3ff;
				nfile2.unread(4);
				if (ShapeFiles.SHAPES_VGA.getFile().getNumFrames(shnum) < 16)
					break;	// Watch for corrupted file.
				MonsterActor act = MonsterActor.create(shnum);
				act.read(nfile2, -1, false);
				act.restoreSchedule();
				// CYCLE_RED_PLASMA();
			}
			nfile2.close();
		} catch (IOException e) {
			//MonsterActor.giveUp();
		}
		if (movingBarge != null) {		// Gather all NPC's on barge.
			BargeObject b = movingBarge;
			movingBarge = null;
			setMovingBarge(b);
		}
		readSchedules();		// Now get their schedules.
		centerView(mainActor.getTileX(), mainActor.getTileY(), mainActor.getLift());
	}
	/*
	 *	Read in offsets.  When done, file is set to start of script names (if
	 *	there are any).
	 *	Returns: # of script names, or -1 if old-style (entsize==4) file.
	 */
	private static int setToReadSchedules
		(
		InputStream sfile,
		Vector<Integer> offsets		// List of offsets ret'd., preceded by num_npcs.
		){
		int num_script_names = -1;
		int num_npcs = EUtil.Read4(sfile);	// # of NPC's, not include Avatar.
		if (num_npcs == -1) {		// Exult format?
			num_npcs = EUtil.Read4(sfile);
			num_script_names = 0;
		} else if (num_npcs == -2) {
			num_npcs = EUtil.Read4(sfile);
			num_script_names = EUtil.Read2(sfile);
		}
		offsets.setSize(num_npcs + 1);
		offsets.setElementAt(num_npcs, 0);
		// Read offsets with list of scheds.
		for (int i = 0; i < num_npcs; i++)
			offsets.setElementAt(EUtil.Read2(sfile), i + 1);
		return num_script_names;
	}
	/*
	 *	Read one NPC's schedule.
	 */

	private void readASchedule(InputStream sfile, int index, Actor npc,
					int entsize, Vector<Integer> offsets, byte ent[])
					throws IOException {
		int cnt = offsets.elementAt(index) - offsets.elementAt(index - 1);
					// Read schedules into this array.
		Schedule.ScheduleChange schedules[] = cnt > 0 
					? new Schedule.ScheduleChange[cnt] : null;
		if (entsize == 4) {	// U7 format?
			for (int j = 0; j < cnt; j++) {
				sfile.read(ent, 0, 4);
				schedules[j] = new Schedule.ScheduleChange();
				schedules[j].set4(ent);
			}
		} else {		// Exult formats.
			for (int j = 0; j < cnt; j++) {
				sfile.read(ent, 0, 8);
				schedules[j] = new Schedule.ScheduleChange();
				schedules[j].set8(ent);
			}
		}
		//System.out.printf("Read %1$d schedules for NPC # %2$d\n", cnt, npc.getNpcNum());
		if (npc != null)			// Store in NPC.
			npc.setSchedules(schedules);
		}

	private void readSchedules() throws IOException {
		InputStream sfile = EUtil.U7openStream2(EFile.GSCHEDULE, EFile.SCHEDULE_DAT);
		if (sfile == null) {
			ExultActivity.fileFatal(EFile.SCHEDULE_DAT);
			return;
		}
		int i, num_npcs, entsize;
		Vector<Integer> offsets = new Vector<Integer>();
		int num_script_names = setToReadSchedules(sfile, offsets);
		num_npcs = offsets.remove(0);
		entsize = num_script_names >= 0 ? 8 : 4;
		//+++++++FINISH Schedule_change.clear();
		//++++++vector<String>& script_names = Schedule_change.get_script_names();
		if (num_script_names > 0) {
			EUtil.Read2(sfile);	// Skip past total size.
			//++++++++++script_names.reserve(num_script_names);
			for (i = 0; i < num_script_names; ++i) {
				int sz = EUtil.Read2(sfile);
				byte nm[] = new byte[sz];
				sfile.read(nm);
				//+++++++++ script_names.push_back(nm);
			}
		}
		byte ent[] = new byte[10];
		for (i = 0; i < num_npcs - 1; i++) {	// Do each NPC, except Avatar.
						// Avatar isn't included here.
			Actor npc = npcs.elementAt(i + 1);
			readASchedule(sfile, i + 1, npc, entsize, offsets, ent);
			//+++++++ CYCLE_RED_PLASMA();
		}
		sfile.close();
	}
	/*
	 *	Create initial 'gamedat' directory if needed
	 *
	 */
	boolean initGamedat(boolean create) {
						// Create gamedat files 1st time.
		if (create) {
			System.out.println("Creating 'gamedat' files.");
			String fname = EFile.PATCH_INITGAME;
			try {
				if (EUtil.U7exists(fname) != null)
					restoreGamedat(fname);
				else {
						// Flag that we're reading U7 file.
					game.setNewGame();
					restoreGamedat(fname = EFile.INITGAME);
				}
			} catch (IOException e) {
				ExultActivity.fileFatal(fname);
			}
			/*
			// log version of exult that was used to start this game
			U7open(out, GNEWGAMEVER);
			getVersionInfo(out);
			out.close();
			*/
		} else if (EUtil.U7exists(EFile.IDENTITY) == null) {
			return false;
		} else {
				byte id[] = new byte[256];
				try {
					RandomAccessFile identity_file = EUtil.U7open(EFile.IDENTITY, false);				
					int i, cnt = identity_file.read(id);
					identity_file.close();
					for(i = 0; i < cnt && id[i] != 0x1a && id[i] != 0x0d && id[i] != 0x0a; i++)
						;
					System.out.println("Gamedat identity " + new String(id, 0, i));
				} catch (IOException e) { }
				/* ++++++FINISH
				const char *static_identity = get_game_identity(INITGAME);
				if(strcmp(static_identity, gamedat_identity))
					{
						delete [] static_identity;
						return false;
					}
				delete [] static_identity;
				*/
			}
		// ++++++ read_save_names();		// Read in saved-game names.	
		return true;
	}
	/*
	 *	Clear out world's contents.  Should be used during a 'restore'.
	 */
	private void clearWorld() {
		CombatSchedule.resume();
		tqueue.clear();		// Remove all entries.
		clearDirty();
		UsecodeScript.clear();	// Clear out all scheduled usecode.
		int cnt = maps.size();
		for (int i = 0; i < cnt; ++i)
			maps.elementAt(i).clear();
		setMap(0);			// Back to main map.
		MonsterActor.deleteAll();	// To be safe, del. any still around.
		//+++++++++ Notebook_gump.clear();
		mainActor = null;
		cameraActor = null;
		numNpcs1 = 0;
		theftCx = theftCy = -1;
		combat = false;
		npcs.setSize(0);			// NPC's already deleted above.
		if (bodies != null)
			bodies.setSize(0);
		movingBarge = null;		// Get out of barge mode.
		specialLight = 0;		// Clear out light spells.
		ambientLight = false;	// And ambient lighting.
		effects.removeAllEffects();
		/* ++++++FINISH
		Schedule_change.clear();
		*/
	}
	/*
	 * The whole 'restore'.
	 */
	static class RestoreThread extends Thread {
		private int num;
		public RestoreThread(int n) {
			num = n;
		}
		public void run() {
			try {
				gwin.restoreGamedat(num);
				gwin.read();
			} catch (IOException e) {
				ExultActivity.fatal(String.format("Failed restoring: %1$s", e.getMessage()));
			}
			gwin.setBusyMessage(null);
		}
	}	
	public void read(int num) {
		setBusyMessage("Restoring Game");
		Thread t = new RestoreThread(num);
		t.start();
	}
	/*
	 *	Restore game by reading in 'gamedat'.
	 */
	public void read() {
		
		audio.cancelStreams();
		/* +++++++++FINISH
	#ifdef RED_PLASMA
		// Display red plasma during load...
		setup_load_palette();
	#endif
		*/
		clearWorld();			// Wipe clean.
		readGwin();			// Read our data.
						// DON'T do anything that might paint()
						//   before calling read_npcs!!
		setupGame();	// Read NPC's, usecode.
	}
	/*
	 *	Read data for the game.
	 *	 */
	public void readGwin() {
		if (!clock.inQueue())		// Be sure clock is running.
			tqueue.add(TimeQueue.ticks, clock, this);
		InputStream gin = null;
		try {
			gin = EUtil.U7openStream(EFile.GWINDAT);	// Gamewin.dat.
		} catch (IOException e) {
			return;
		}
						// Start with scroll coords (in tiles).
		scrolltx = EUtil.Read2(gin);
		scrollty = EUtil.Read2(gin);
						// Read clock.
		clock.reset();
		clock.setDay(EUtil.Read2(gin));
		clock.setHour(EUtil.Read2(gin));
		clock.setMinute(EUtil.Read2(gin));
		specialLight = EUtil.Read4(gin);
		armageddon = false;		// Old saves may not have this yet.
		/* ++++++++++FINISH
		if (gin.available() == 0) {
			specialLight = 0;
			return;//++++++CLOSE
		}
		int track_num = EUtil.Read4(gin);
		int repeat = EUtil.Read4(gin);
		
		if (!gin_stream.good())
		{
			audio.stop();
			return;//++++++++++CLOSE
		}

		audio.startMusic(track_num, repeat != false);
		armageddon = gin.read1() == 1 ? true : false;
		if (!gin_stream.good())
			armageddon = false;

		ambient_light = gin.read1() == 1 ? true : false;
		if (!gin_stream.good())
			ambient_light = false;
		*/
		try {
			gin.close();
		} catch (IOException e) { }
	}
	/*
	 *	Save game by writing out to the 'gamedat' directory.  Call before saveGamedat().
	 */
	public void write() throws IOException {
		int mapcnt = maps.size();
		try {
			for (int i = 0; i < mapcnt; ++i)
				maps.elementAt(i).writeIreg();	// Write ireg files.
			writeNpcs();			// Write out npc.dat.
			usecode.write();		// Usecode.dat (party, global flags).
			//+++++ Notebook_gump.write();		// Write out journal.
			writeGwin();			// Write our data.
			writeSaveInfo();
		} catch (IOException e) {
			ExultActivity.fatal("Error saving: " + e.getMessage());
		}
	}
	/*
	 * The whole 'save'.
	 */
	static class SaveThread extends Observable implements Runnable {
		private int num;
		private String savename;
		public SaveThread(int n, String s, Observer client) {
			num = n; savename = s;
			if (client != null)
				addObserver(client);
		}
		public void start() {
			Thread t = new Thread(this);
			t.start();
		}
		public void run() {
			try {
				gwin.write();
				gwin.saveGamedat(num, savename);
				setChanged();
				notifyObservers();
			} catch (IOException e) {
				ExultActivity.fatal(String.format("Failed saving: %1$s", e.getMessage()));
			}
			System.out.println("Finished save");
			gwin.setBusyMessage(null);
		}
	}	
	public void write(int num, String savename, Observer client) {
		setBusyMessage("Saving Game");
		SaveThread t = new SaveThread(num, savename, client);
		t.start();
	}
	public void write(int num, String savename) {
		write(num, savename, null);
	}
	private void writeGwin() throws IOException {
		OutputStream gout = EUtil.U7create(EFile.GWINDAT);
					// Start with scroll coords (in tiles).
		EUtil.Write2(gout, getScrolltx());
		EUtil.Write2(gout, getScrollty());
					// Write clock.
		EUtil.Write2(gout, clock.getDay());
		EUtil.Write2(gout, clock.getHour());
		EUtil.Write2(gout, clock.getMinute());
		EUtil.Write4(gout, specialLight);	// Write spell expiration minute.
		/*++++++++++FINISH
		MyMidiPlayer *player = Audio.get_ptr().get_midi();
		if (player) {
			EUtil.Write4(gout, static_cast<uint32>(player.get_current_track()));
			EUtil.Write4(gout, static_cast<uint32>(player.is_repeating()));
		} else */ {
			EUtil.Write4(gout, -1);
			EUtil.Write4(gout, 0);
		}
		gout.write(armageddon ? 1 : 0);
		gout.write(ambientLight ? 1 : 0);
		gout.flush();
	}

	private void writeNpcs() throws IOException {	
		int num_npcs = npcs.size();
		OutputStream out = EUtil.U7create(EFile.NPC_DAT);

		EUtil.Write2(out, numNpcs1);	// Start with counts.
		EUtil.Write2(out, npcs.size() - numNpcs1);
		for (int i = 0; i < num_npcs; i++)
			npcs.elementAt(i).write(out);
			
		out.close();
		writeSchedules();		// Write schedules
					// Now write out monsters in world.
		out = EUtil.U7create(EFile.MONSNPCS);
		int cnt = 0;
		HashSet<MonsterActor> monsters = MonsterActor.getAll();
		for (MonsterActor mact : monsters) {		// Count them.
			if (!mact.isDead())		// Alive?
				cnt++;
		}
		EUtil.Write2(out, cnt);
		for (MonsterActor mact : monsters) {
			if (!mact.isDead())		// Alive?
				mact.write(out);
		}
		out.close();
	}
	/*
	 *	Write NPC schedules.
	 */
	private void writeSchedules () throws IOException {
		Schedule.ScheduleChange schedules[];
		int cnt;
		short offset = 0;
		int i;
		int num;

		// So do I allow for all NPCs (type1 and type2) - Yes i will
		num = npcs.size();
		OutputStream sfile = EUtil.U7create(EFile.GSCHEDULE);
		//+++++FINISH vector<char *>& script_names = Schedule_change.get_script_names();

		EUtil.Write4(sfile, -2);		// Exult version #.
		EUtil.Write4(sfile, num);		// # of NPC's, not include Avatar.
		EUtil.Write2(sfile, 0 /* +++++FINISH script_names.size() */);
		EUtil.Write2(sfile, 0);		// First offset

		for (i = 1; i < num; i++) {	// write offsets with list of scheds.
			schedules = npcs.elementAt(i).getSchedules();
			cnt = schedules == null ? 0 : schedules.length;
			offset += cnt;
			EUtil.Write2(sfile, offset);
		}
		/* ++++++++++FINISH
		if (script_names.size()) {
			int total = 0;		// Figure total size.
			vector<char *>.iterator it;
			for (it = script_names.begin(); it != script_names.end(); ++it)
				total += 2 + strlen(*it);
			EUtil.Write2(sfile, total);
			for (it = script_names.begin(); 
						it != script_names.end(); ++it) {
				int len = strlen(*it);
				EUtil.Write2(sfile, len);
				sfile.write(*it, len);
			}
		}
		*/
		byte ent[] = new byte[20];
		for (i = 1; i < num; i++) {	// Do each NPC, except Avatar.
			schedules = npcs.elementAt(i).getSchedules();
			cnt = schedules == null ? 0 : schedules.length;
			for (int j = 0; j < cnt; j++) {
				schedules[j].write8(ent);
				sfile.write(ent, 0, 8);
			}
		}
		sfile.close();
	}
	/*
	 *	Write out the gamedat directory from a saved game.
	 *
	 *	Output: Aborts if error.
	 */
	void restoreGamedat(int num) throws IOException {
		String nm = String.format(EFile.SAVENAME, num, game.isBG() ? "bg" : "si");
		restoreGamedat(nm);
	}
	void restoreGamedat(String fname) throws IOException {
		System.out.println("restoreGamedat:  " + fname);
		/*
						// Check IDENTITY.
		const char *id = get_game_identity(fname);
		const char *static_identity = get_game_identity(INITGAME);
						// Note: "*" means an old game.
		if(!id || (*id != '*' && strcmp(static_identity, id) != 0))
			{
			std.string msg("Wrong identity '");
			msg += id; msg += "'.  Open anyway?";
			int ok = Yesno_gump.ask(msg.c_str());
			if (!ok)
				return;
			}
		#ifdef RED_PLASMA
		// Display red plasma during load...
		setup_load_palette();
		#endif
		 */								
		EUtil.U7mkdir("<GAMEDAT>");		// Create dir. if not already there. Don't
										// use GAMEDAT define cause that's got a
										// trailing slash
		if (!EUtil.isFlex(fname)) {
			restoreGamedatZip(fname);
			return;
		}
		RandomAccessFile in = EUtil.U7open(fname, true);
		if (in == null)
			ExultActivity.fatal("Can't open file: " + EUtil.getSystemPath(fname));
		removeBeforeRestore();
		try {
			restoreFlexFiles(in, EFile.GAMEDAT);
		} catch (IOException e) {
			ExultActivity.fatal("Error restoring from: " + EUtil.getSystemPath(fname));
			return;
		}
		in.close();
	/* #ifdef RED_PLASMA
		load_palette_timer = 0;
	#endif
	 */
	}
	/*
	 *	Write files from flex assuming first 13 characters of
	 *	each flex object are an 8.3 filename.
	 */
	void restoreFlexFiles(RandomAccessFile in, String basepath) throws IOException {
		in.seek(0x54);			// Get to where file count sits.
		int numfiles = EUtil.Read4(in);
		System.out.println("RestoreFlexFiles: cnt = " + numfiles);
		in.seek(0x80);			// Get to file info.
						// Read pos., length of each file.
		int finfo[] = new int[2*numfiles];
		int i;
		for (i = 0; i < numfiles; i++) {
			finfo[2*i] = EUtil.Read4(in);	// The position, then the length.
			finfo[2*i + 1] = EUtil.Read4(in);
		}
		byte nm13[] = new byte[13];
		for (i = 0; i < numfiles; i++) {	// Now read each file.
						// Get file length.
			int len = finfo[2*i + 1] - 13, pos = finfo[2*i];
			if (len <= 0)
				continue;
			in.seek(pos);	// Get to it.
			in.read(nm13);
			int nlen;
			for (nlen = 0; nlen < nm13.length && nm13[nlen] != 0; ++nlen)
				;
			if (nm13[nlen] == '.')	// Watch for names ending in '.'.
				nlen--;
			String fname = basepath + new String(nm13, 0, nlen);
						// Now read the file.
			byte buf[] = new byte[len];
			in.read(buf);
			//+++++FINISH: multimap stuff here.
			//+++++++++++++
			try {
				OutputStream out = EUtil.U7create(fname);
				out.write(buf);	// Then write it out.
				out.close();
			} catch (IOException e) {
				ExultActivity.fatal(String.format("Error writing '%1$s'.", 
														EUtil.getSystemPath(fname)));
				return;
			}
			
			// CYCLE_RED_PLASMA();
		}
	}
	private boolean restoreGamedatZip(String fname) {
		System.out.println("restoreGamedatZip: " + fname);
		InputStream in;
		ZipInputStream zin;
		try {
			in = EUtil.U7openStream(fname);
			in.skip(saveNameSize);
			zin = new ZipInputStream(in);
			String nm = EUtil.getSystemPath(fname);
			System.out.println("restoreGamedatZip: opening " + nm);
		} catch (IOException e) {
			System.out.println("Zip exception: " + e.getMessage());
			ExultActivity.fileFatal(fname);
			return false;
		}
		removeBeforeRestore();
		ZipEntry ze = null;
		byte buf[] = null;
		System.out.println("About to read zip entries");
		try {
			while ((ze = zin.getNextEntry()) != null) {
				String fnm = EFile.GAMEDAT + ze.getName();
				
				//+++++FINISH: multimap stuff here.
				//++++++++++++
				int len = (int)ze.getSize();
				// System.out.println("Unzipping " + fnm + " of length " + len);
				if (len == -1)			// Means 'unknown'.
					len = 0x1000;
				if (buf == null || buf.length < len)
					buf = new byte[len];
				OutputStream out = EUtil.U7create(fnm);
				int rcnt;
				while ((rcnt = zin.read(buf, 0, len)) > 0) {
					// System.out.println("Read " + rcnt + " bytes for " + fnm);
					out.write(buf, 0, rcnt);	// Then write it out.
				}
				//System.out.println("Entry " + fnm + ", done");
				out.close();
				zin.closeEntry();
			} 
			// CYCLE_RED_PLASMA();
	        
	        zin.close();
	        System.out.println("restoreGamedatZip completed");
	    } catch (IOException e) {
	    	String err = String.format("Error restoring '%1$s': %2$s", 
					EUtil.getSystemPath(fname), e.getMessage());
	    	System.out.println(err);
	    	ExultActivity.fatal(err);
			return false;
		}
	    return true;
	}
	private void removeBeforeRestore() {
		EUtil.U7remove (EFile.USEDAT);
		EUtil.U7remove (EFile.USEVARS);
		EUtil.U7remove (EFile.U7NBUF_DAT);
		EUtil.U7remove (EFile.NPC_DAT);
		EUtil.U7remove (EFile.MONSNPCS);
		EUtil.U7remove (EFile.FLAGINIT);
		EUtil.U7remove (EFile.GWINDAT);
		EUtil.U7remove (EFile.IDENTITY);
		EUtil.U7remove (EFile.GSCHEDULE);
		EUtil.U7remove ("<STATIC>/flags.flg");
		EUtil.U7remove (EFile.GSCRNSHOT);
		EUtil.U7remove (EFile.GSAVEINFO);
		EUtil.U7remove (EFile.GNEWGAMEVER);
		EUtil.U7remove (EFile.GEXULTVER);
		EUtil.U7remove (EFile.KEYRINGDAT);
		EUtil.U7remove (EFile.NOTEBOOKXML);
	}
	/*
	 *	List of 'gamedat' files to save (in addition to 'iregxx'):
	 */
	private static final String bgsavefiles[] = {
		EFile.GSCRNSHOT,	EFile.GSAVEINFO,	// MUST BE FIRST!!
		EFile.IDENTITY,			// MUST BE #2
		EFile.GEXULTVER, 	EFile.GNEWGAMEVER,
		EFile.NPC_DAT,	EFile.MONSNPCS,
		EFile.USEVARS,	EFile.USEDAT,
		EFile.FLAGINIT,	EFile.GWINDAT,
		EFile.GSCHEDULE /* ,	EFile.NOTEBOOKXML */
		};
	private static final int bgnumsavefiles = bgsavefiles.length;

	private static final String sisavefiles[] = {
		EFile.GSCRNSHOT,	EFile.GSAVEINFO,	// MUST BE FIRST!!
		EFile.IDENTITY,			// MUST BE #2
		EFile.GEXULTVER,	EFile.GNEWGAMEVER,
		EFile.NPC_DAT,	EFile.MONSNPCS,
		EFile.USEVARS,	EFile.USEDAT,
		EFile.FLAGINIT,	EFile.GWINDAT,
		EFile.GSCHEDULE,	EFile.KEYRINGDAT,
		EFile.NOTEBOOKXML
		};
	private static final int sinumsavefiles = sisavefiles.length;
	private static final int saveNameSize = 0x50;
	//	Save, using user's 'savename'.
	public void saveGamedat(int num, String savename) throws IOException {
		String nm = String.format(EFile.SAVENAME, num, game.isBG() ? "bg" : "si");
		saveGamedat(nm, savename);
		/* ++++++OLD savegames.
		if (num >=0 && num < 10) {
			saveNames[num] = savename;
		}
		*/
	}
	private boolean saveOneZip(ZipOutputStream zout, String fname, byte buf[]) 
											throws IOException {
		int sz;
		InputStream in;
		
		try {
			in = EUtil.U7openStream(fname);
			sz = in.available();
			//System.out.println("Saving to zip: " + fname + ", sz = " + sz);
			if (buf == null || buf.length < sz)
				buf = new byte[sz];
			in.read(buf, 0, sz);
		} catch (IOException e) {
			//++++FOR NOW, ignore   ExultActivity.fileFatal(fname);
			// ++++++++return false;
			return true;
		}
		
		ZipEntry entry = new ZipEntry(EUtil.baseName(fname));
		zout.putNextEntry(entry); // Store entry
		zout.write(buf, 0, sz);
		in.close();
		return true;
	}
	private void saveGamedat(String fname, String savename) throws IOException {
		// setup correct file list 
		int numsavefiles = game.isBG() ? bgnumsavefiles : sinumsavefiles;
		String savefiles[] = game.isBG() ? bgsavefiles : sisavefiles;	
		OutputStream out = EUtil.U7create(fname);
		byte namebytes[] = savename.getBytes();
		int namelen = Math.min(namebytes.length, saveNameSize);
		byte namebuf[] = new byte[saveNameSize];
		System.arraycopy(namebytes, 0, namebuf, 0, namelen);
		out.write(namebuf);
		ZipOutputStream zout = new ZipOutputStream(out);
		System.out.println("Saving to " + fname);
		byte buf[] = null;
		for (int i = 0; i < numsavefiles; ++i) {
			if (!saveOneZip(zout, savefiles[i], buf))
				return;
		}
		// Now the IREG files.
		int mapcnt =  maps.size();
		for (int j = 0; j < mapcnt; ++j) {
			GameMap map = maps.elementAt(j);
			if (map != null) {
				for (int schunk = 0; schunk < 12*12; schunk++) {
					//Check to see if the ireg exists before trying to
					//save it; prevents crash when creating new maps
					//for existing games
					String iname = map.getSchunkFileName(EFile.U7IREG, schunk);
					if (EUtil.U7exists(iname) != null) {
						if (!saveOneZip(zout, iname, buf))
							return;
					}
				}
			}
		}
		zout.close();
	}
	public boolean getSaveInfo(int num, NewFileGump.SaveInfo info) {
		String fname = String.format(EFile.SAVENAME, num, game.isBG() ? "bg" : "si");
		if (!EUtil.isFlex(fname)) {
			return getSaveInfoZip(fname, info);
		}
		//++++++++FOR NOW, we don't support non-zipped.
		return false;
	}
	public boolean getSaveInfoZip(String fname, NewFileGump.SaveInfo info) {
		//++++++++
		InputStream in;
		ZipInputStream zin;
		ZipEntry ze = null;
		try {
			in = EUtil.U7openStream(fname);
			byte namebuf[] = new byte[saveNameSize];
			in.read(namebuf);
			int i;
			for (i = 0; i < saveNameSize; ++i)
				if (namebuf[i] == 0)
					break;
			info.savename = new String(namebuf, 0, i);
			zin = new ZipInputStream(in);
			System.out.println("getSaveInfoZip: name is " + info.savename);
			String screenshotName = EUtil.baseName(EFile.GSCRNSHOT);
			String saveinfoName = EUtil.baseName(EFile.GSAVEINFO);
			int found = 0;
			while (found < 2 && (ze = zin.getNextEntry()) != null) {
				String fnm = ze.getName();
				if (fnm.equals(screenshotName)) {
					++found;
					int ind = 0, rcnt, sz = EUtil.Read4(zin);
					byte buf[] = new byte[sz];
					EUtil.Write4(buf, 0, sz);
					ind += 4;
					while (ind < sz && (rcnt = zin.read(buf, ind, sz - ind)) > 0)
						ind += rcnt;
					info.screenshot = new VgaFile.ShapeFile(buf);
				} else if (fnm.equals(saveinfoName)) {
					++found;
					info.readSaveInfo(zin);
				}
				zin.closeEntry();
			}
			zin.close();
		} catch (IOException e) {
			System.out.println("Zip exception: " + e.getMessage());
			ExultActivity.fileFatal(fname);
			return false;
		}
		return true;
	}
	private void writeSaveInfo() throws IOException {
		int save_count = 1;

		try {
			InputStream in = EUtil.U7openStream(EFile.GSAVEINFO); 
			in.skip(10);	// Skip 10 bytes.
			save_count += EUtil.Read2(in);
			in.close();
		} catch (IOException e) { }
		int party_size = partyman.getCount()+1;

		Calendar timeinfo = Calendar.getInstance();
		OutputStream out = EUtil.U7create(EFile.GSAVEINFO);

		// This order must match struct SaveGame_Details

		// Time that the game was saved
		out.write(timeinfo.get(Calendar.MINUTE));
		out.write(timeinfo.get(Calendar.HOUR));
		out.write(timeinfo.get(Calendar.DAY_OF_MONTH));
		out.write(timeinfo.get(Calendar.MONTH) + 1);
		EUtil.Write2(out, timeinfo.get(Calendar.YEAR));

		// The Game Time that the save was done at
		out.write(clock.getMinute());
		out.write(clock.getHour());
		EUtil.Write2(out, clock.getDay());		
		EUtil.Write2(out, save_count);
		out.write(party_size);

		out.write(0);			// Unused

		out.write(timeinfo.get(Calendar.SECOND));	// 15

		// Packing for the rest of the structure
		for (int j = NewFileGump.SaveGameDetails.skip; j > 0; --j)
			out.write(0);

		for (int i = 0; i<party_size ; i++) {
			Actor npc;
			if (i == 0)
				npc = mainActor;
			else
				npc = getNpc(partyman.getMember(i-1));

			// Write out 18 bytes of name.
			byte namestr[] = npc.getNpcName().getBytes();
			int namelen = Math.min(namestr.length, 18);
			out.write(namestr, 0, namelen);
			for ( ; namelen < 18; ++namelen)
				out.write(0);
			EUtil.Write2(out, npc.getShapeNum());

			EUtil.Write4(out, npc.getProperty(Actor.exp));
			EUtil.Write4(out, npc.getFlags());
			EUtil.Write4(out, npc.getFlags2());

			out.write(npc.getProperty(Actor.food_level));
			out.write(npc.getProperty(Actor.strength));
			out.write(npc.getProperty(Actor.combat));
			out.write(npc.getProperty(Actor.dexterity));
			out.write(npc.getProperty(Actor.intelligence));
			out.write(npc.getProperty(Actor.magic));
			out.write(npc.getProperty(Actor.mana));
			out.write(npc.getProperty(Actor.training));

			EUtil.Write2(out, npc.getProperty(Actor.health));
			EUtil.Write2(out, /* +++++npc.get_shapefile()*/  0);

			// Packing for the rest of the structure
			for (int j = SaveGameParty.skip; j > 0; --j)
				out.write(0);
		}

		out.close();
		// Save Shape
		VgaFile.ShapeFile map = createMiniScreenshot(true);	// High quality but slow.
		out = EUtil.U7create(EFile.GSCRNSHOT);
		map.save(out);
		out.close();

		// Current Exult version
		/* ++++++++++++FINISH
		out = EUtil.U7create(EFile.GEXULTVER);
		getVersionInfo(out);
		out.close();
		*/
		// Exult version that started this game
		if (EUtil.U7exists(EFile.GNEWGAMEVER) == null) {
			out = EUtil.U7create(EFile.GNEWGAMEVER);
			String unk = new String("Unknown\n");
			out.write(unk.getBytes());
			out.close();
		}
	}
	/*
	 * Create mini-screenshot for savegames.
	 */
	public VgaFile.ShapeFile createMiniScreenshot(boolean fast) {
		VgaFile.ShapeFile sh = null;
		ShapeFrame fr = null;
		byte img[] = null;
		
		synchronized(win) {
			setAllDirty();
			render.paintMap(0, 0, getWidth(), getHeight());
			img = win.miniScreenshot(fast);
			if (img != null) {
				fr = new ShapeFrame();
				fr.createRle(img, 0, 0, 96, 60);
				sh = new VgaFile.ShapeFile(fr);
			}
			setAllDirty();
			paint();
		}
		return sh;
	}
}
