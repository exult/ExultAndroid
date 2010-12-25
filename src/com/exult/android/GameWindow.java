package com.exult.android;
import java.util.Vector;
import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Point;

import android.graphics.Canvas;

public class GameWindow extends GameSingletons {
	private static GameWindow instance;
	private EffectsManager effects;	// Manages speciall effects.
	private Vector<GameMap> maps;	// Hold all terrain.
	private GameMap map;			// Current map.
	private GameRender render;
	private TimeQueue tqueue;
	private UsecodeMachine usecode;
	private Rectangle paintBox;		// Temp used for painting.
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
	private GameObject movingBarge;
	// Rendering
	private int scrolltx, scrollty;		// Top-left tile of screen.
	private Rectangle scrollBounds;	// Walking outside this scrolls.
	private boolean painted;			// We updated imagebuf.
	private Rectangle dirty;			// What to display.
	// Options:
	int stepTileDelta = 8;				// Multiplier for the delta in startActor.
	//	Game state values.
	private int skipAboveActor;		// Level above actor to skip rendering.
	private int numNpcs1;			// Number of type1 NPC's.
	/*
	 *	Public flags and gameplay options:
	 */
	public int skipLift;	// Skip objects with lift >= this.  0
							//   means 'terrain-editing' mode.
	public boolean paintEggs = true;//++++TRUE for testing.
	public int blits;		// For frame-counting.
	
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
		tempDirty = new Rectangle();
		tempFind = new Rectangle();
		GameSingletons.init(this);
		skipLift = 16;
		skipAboveActor = 31;
		
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
		return /* ++++++ moving_barge ? moving_barge.is_moving()
			    : */ mainActor.isMoving();
	}
	public final boolean inCombat() {
		return false;	//++++++FINISH
	}
	public final boolean isTimeStopped() {
		return false;	// +++++++FINISH
	}
	public final Actor getMainActor() {
		return mainActor;
	}
	public final Actor getCameraActor() {
		return cameraActor;
	}
	public final boolean isMainActorInside()
		{ return skipAboveActor < 31 ; }
	// Returns if skip_above_actor changed!
	public final boolean setAboveMainActor(int lift) {
		if (skipAboveActor == lift) 
			return false;
		skipAboveActor = lift;
		return true;
	}
	public final int getNumNpcs() {
		return npcs.size();
	}
	public final Actor getNpc(int n) {
		return n >= 0 && n < npcs.size() ? npcs.elementAt(n) : null;
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
		return mainActor.canAct();
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
		/*
		Barge_object *old_active_barge = moving_barge;
		*/
		map.readMapData();		// This pulls in objects.
		/*
						// Found active barge?
		if (!old_active_barge && moving_barge)
			{			// Do it right.
			Barge_object *b = moving_barge;
			moving_barge = 0;
			set_moving_barge(b);
			}
						// Set where to skip rendering.
		int cx = camera_actor.get_cx(), cy = camera_actor.get_cy();	
		Map_chunk *nlist = map.get_chunk(cx, cy);
		nlist.setup_cache();					 
		int tx = camera_actor.get_tx(), ty = camera_actor.get_ty();
		set_above_mainActor(nlist.is_roof (tx, ty,
							camera_actor.get_lift()));
		set_in_dungeon(nlist.has_dungeon()?nlist.is_dungeon(tx, ty):0);
		set_ice_dungeon(nlist.is_ice_dungeon(tx, ty));
		*/
	}
	public final void centerView(Tile t) {
		// OFFSET HERE
		int tw = getWidth()/EConst.c_tilesize, 
			th = (getHeight())/EConst.c_tilesize;
		setScrolls(EConst.DECR_TILE(t.tx, tw/2), EConst.DECR_TILE(t.ty, th/2));
		setAllDirty();
	}
	public boolean scrollIfNeeded(Actor a, Tile t) {
		if (a != cameraActor)
			return false;
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
	//	Center around given tile pos.
	public void centerView(int tx, int ty) {
		int tw = win.getWidth()/EConst.c_tilesize, th = (win.getHeight())/EConst.c_tilesize;
		setScrolls(EConst.DECR_TILE(tx, tw/2), EConst.DECR_TILE(ty, th/2));
		setAllDirty();
	}
	/*
	 *	Shift view by one tile.
	 */
	public void shiftViewHoriz(boolean toleft) {
		int w = getWidth(), h = getHeight();
		if (toleft) {
			scrolltx = EConst.DECR_TILE(scrolltx);
			scrollBounds.x = EConst.DECR_TILE(scrollBounds.x);
		} else {
						// Get current rightmost chunk.
			scrolltx = EConst.INCR_TILE(scrolltx);
			scrollBounds.x = EConst.INCR_TILE(scrollBounds.x);
		}
		if (gumpman.showingGumps()) {		// Gump on screen?
			setAllDirty();
			return;
		}
		map.readMapData();		// Be sure objects are present.
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
		int w = getWidth(), h = getHeight();
		if (up) {
			scrollty = EConst.DECR_TILE(scrollty);
			scrollBounds.y = EConst.DECR_TILE(scrollBounds.y);
		} else {
						// Get current bottomost chunk.
			scrollty = EConst.INCR_TILE(scrollty);
			scrollBounds.y = EConst.INCR_TILE(scrollBounds.y);
		}
		if (gumpman.showingGumps())			// Gump on screen?
			{
			setAllDirty();
			return;
			}
		map.readMapData();		// Be sure objects are present.
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
					if (!Map_chunk::is_blocked(start, 1, 
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
					// Wrap:Game_window::start_actor
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
		/*+++++++++++
		clock.reset();			// Reset and re-display palette.
		clock.set_palette();
		*/
		Tile t1 = new Tile();
		for (i = 0; i < cnt; i++) {
			int party_member=party_man.getMember(i);
			Actor person = getNpc(party_member);
			if (person != null && !person.isDead() /* +++++FINISH && 
			    person.getScheduleType() != Schedule::wait */) {
				person.setAction(null);
				/* +++++++++++++FINISH
				t1 = Map_chunk::find_spot(t, 8,
					person.get_shapenum(), person.get_framenum(),
										1);
				if (t1.tx != -1)
					person.move(t1, newMap);
				*/
			}
		}
		mainActor.getFollowers();
		/* +++++++++++
		if (!skip_eggs)			// Check all eggs around new spot.
			Map_chunk::try_all_eggs(main_actor, t.tx, t.ty, t.tz,
						oldpos.tx, oldpos.ty);
		*/
		/* +++++NEEDED? generate mousemotion event
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
				mainActor.getFlag(GameObject.paralyzed) /* ++++ ||
				mainActor.in_usecode_control() || 
				mainActor.get_schedule_type() == Schedule::sleep */)
			return;			// Zzzzz....
		
		if (gumpman.gumpMode())
			return;
		/*++++++++++
		if (moving_barge)
			{			// Want to move center there.
			int lift = mainActor.get_lift();
			int liftpixels = 4*lift;	// Figure abs. tile.
			int tx = get_scrolltx() + (winx + liftpixels)/c_tilesize,
		    ty = get_scrollty() + (winy + liftpixels)/c_tilesize;
					// Wrap:
			tx = (tx + c_num_tiles)%c_num_tiles;
			ty = (ty + c_num_tiles)%c_num_tiles;
			Tile_coord atile = moving_barge.get_center(),
			   btile = moving_barge.get_tile();
					// Go faster than walking.
			moving_barge.travel_to_tile(
				Tile_coord(tx + btile.tx - atile.tx, 
				   ty + btile.ty - atile.ty, btile.tz), 
					speed/2);
		} else
		*/
		{
			/* ++++++++++++
			// Set schedule.
			int sched = mainActor.get_schedule_type();
			if (sched != Schedule::follow_avatar &&
					sched != Schedule::combat &&
					!mainActor.get_flag(Obj_flags::asleep))
				mainActor.set_schedule_type(Schedule::follow_avatar);
			*/
			startActorSteps(fromx, fromy, tox, toy, speed);
		}
	}
	public final void stopActor() {
		/* +++++++++++++++
		if (moving_barge)
			moving_barge.stop();
		else
		*/
			{
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
		System.out.println("findObject at " + x + ", " + y + ", found ");
		if (best != null)
			System.out.println("Found " + best.getShapeNum());
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
			/* +++++++FINISH
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
		Actor npc = obj != null && obj instanceof Actor ? (Actor) obj : null;
		/* ++++CHEAT stuff went here. */
		if (obj != null) {			// Show name.
			System.out.println("Clicked on shape " + obj.getShapeNum() +
					":  " + obj.getName());
			// ++++ String namestr = Get_object_name(obj);
				// Combat and an NPC?
			/* ++++++++
			if (in_combat() && Combat::mode != Combat::original && npc)
				{
				char buf[128];
				sprintf(buf, "%s (%d)", objname, 
						npc.get_property(Actor::health));
				objname = &buf[0];
				}
			*/
			effects.addText(/* objname*/ obj.getName() , obj);
		}
		/*
		// If it's an actor and we want to grab the actor, grab it.
		if (npc != null && cheat.grabbing_actor() && 
		    (npc.get_npc_num() || npc==mainActor))
			cheat.set_grabbed_actor (npc);
		*/
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
		//++++++if (mainActorDontMove())
		//++++++	return;
		/*
						// Nothing going on?
		if (!Usecode_script::get_count())
			removed.flush();	// Flush removed objects.
		*/
						// Look for obj. in open gump.
		GameObject obj = null;
		Gump gump = gumpman.findGump(x, y);
		boolean avatar_can_act = mainActorCanAct();
		if (gump != null) {
			obj = gumpman.doubleClicked(gump, x, y);
		// If gump manager didn't handle it, we search the world for an object
		} else {
			obj = findObject(x, y);
			/* ++++++++++++++
			if (!avatarCanAct && obj && obj.as_actor()
			    	&& obj.as_actor() == mainActor.as_actor())
				{
				ActionFileGump(0);
				return;
				}
			
			// Check path, except if an NPC, sign, or if editing.
			if (obj && !obj.asActor() &&
				!cheat.in_hack_mover() &&
				//!Is_sign(obj.get_shapenum()) &&
				!Fast_pathfinder_client::is_grabable(mainActor, obj))
				{
				Mouse::mouse.flash_shape(Mouse::blocked);
				return;
				}
			*/
			}
		if (obj == null /*+++++TESTING || !avatar_can_act */)
			return;			// Nothing found or avatar disabled.
		System.out.println("Double-clicked on shape " + obj.getShapeNum() +
				":  " + obj.getName());
		/* +++++++++++
		if (combat && !gump &&		// In combat?
		    !Combat::is_paused() &&
		    (!gump_man.gump_mode() || gump_man.gumps_dont_pause_game()))
			{
			Actor *npc = obj.as_actor();
						// But don't attack party members.
			if ((!npc || !npc.is_in_party()) &&
						// Or bodies.
					!obj.get_info().is_body_shape())
				{		// In combat mode.
				// Want everyone to be in combat.
				combat = 0;
				mainActor.set_target(obj);
				toggle_combat();
				return;
				}
		}
		*/
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
	public void setPainted()
		{ painted = true; }
	public boolean wasPainted()
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
			gumpman.paint(false);
			if (drag != null) 
				drag.paint();	// Paint what user is dragging.
			effects.paintText();
			gumpman.paint(true);
			Conversation conv = GameSingletons.conv;
			if (conv != null)
				conv.paint();		// Conversation.
			
			/*
					// Complete repaint?
			if (!gx && !gy && gw == get_width() && gh == get_height() && mainActor)
			{			// Look for lights.
			Actor *party[9];	// Get party, including Avatar.
			int cnt = get_party(party, 1);
			int carried_light = 0;
			for (int i = 0; !carried_light && i < cnt; i++)
				carried_light = party[i].has_light_source();
					// Also check light spell.
			if (special_light && clock.get_total_minutes() >special_light)
				{		// Just expired.
				special_light = 0;
				clock.set_palette();
				}
					// Set palette for lights.
			clock.set_light_source(carried_light + (light_sources > 0),
								in_dungeon);
			}
		*/
		win.clearClip();
		} // End 'synchronized'.
	}	
	public void paint(Rectangle r)
		{ paint(r.x, r.y, r.w, r.h); }
	// Clip 'r' to window.
	public void clipToWin(Rectangle r) {
		paintBox.set(0, 0, win.getWidth(), win.getHeight());
		r.intersect(paintBox);
	}
	public void paintDirty() {
		/*
		// Update the gumps before painting, unless in dont_move mode (may change dirty area)
	    if (!mainActor_dont_move())
	        gump_man.update_gumps();

		effects.update_dirty_text();
		*/
		paintBox.set(dirty);
		clipToWin(paintBox);
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
			/* FINISH++++++++
			Game::clear_avname();
			Game::clear_avsex();
			Game::clear_avskin();
			*/
			return;
		}
		readNpcs();			// Read in all U7 NPC's.

		// Was a name, sex or skincolor set in Game
		// this bascially detects 
		boolean changed = false;

		/* ++++++++FINISH
		if (Game::get_avsex() == 0 || Game::get_avsex() == 1 || Game::get_avname()
				|| (Game::get_avskin() >= 0 && Game::get_avskin() <= 2))
			changed = true;

		Game::clear_avname();
		Game::clear_avsex();
		Game::clear_avskin();

		// Update gamedat if there was a change
		if (changed) {
			schedule_npcs(6,7,false);
			write_npcs();
		}
		*/
	}
	public void initFiles(boolean cycle) {
		ShapeID.loadStatic();
	}
	//	Prepare for game.
	public void setupGame() {
		// FOR NOW:  Unpack INITGAME if not already done.
		if (EUtil.U7exists(EFile.IDENTITY) == null)
			initGamedat(true);
		getMap(0).init();
		pal.set(Palette.PALETTE_DAY, -1, null);//+++++ALSO for testing.
		// Init. current 'tick'.
		// Game::set_ticks(SDL_GetTicks());
		try {
			initActors();		// Set up actors if not already done.
								// This also sets up initial 
								// schedules and positions.
		} catch (IOException e) {
			System.out.println("FAILED to read NPCs!");
		}
		// CYCLE_RED_PLASMA();
		/* ++++++++++FINISH 
		Notebook_gump::initialize();		// Read in journal.
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
					Usecode_machine::did_first_scene, 1);
			*/
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

		// note: we had to stop the plasma here already, because init_readied
		// and activate_eggs may update the screen through usecode functions
		// (Helm of Light, for example)
		Actor party[] = new Actor[9];
		int cnt = getParty(party, 1);	// Get entire party.
		for (int i = 0; i < cnt; i++) {	// Init. rings.
			party[i].initReadied();
		}
		time_stopped = 0;
		*/
	//+++++The below wasn't prev. done by ::read(), so maybe it should be
	//+++++controlled by a 'first-time' flag.
						// Want to activate first egg.
		MapChunk olist = mainActor.getChunk();
		int tx = mainActor.getTileX(), ty = mainActor.getTileY(), tz = mainActor.getLift();
		// Do them immediately.
		olist.activateEggs(mainActor, tx, ty, tz, -1,-1,true);
		// Force entire repaint.
		setAllDirty();
		painted = true;			// Main loop uses this.
		gumpman.closeAllGumps(true);		// Kill gumps.
		/*+++++++++FINISH
		Face_stats::load_config(config);

		// Set palette for time-of-day.
		clock.reset();
		clock.set_palette();
		pal.fade(6, 1, -1);		// Fade back in.
		*/
	}
	public void readNpcs() throws IOException {
		npcs = new Vector<Actor>(1);			// Create main actor.
		cameraActor = mainActor = new MainActor("", 0);
		npcs.add(mainActor);
		InputStream nfile = EUtil.U7openStream(EFile.NPC_DAT);
		int numNpcs;
		boolean fix_unused = false;	// Get set for old savegames.
		numNpcs1 = EUtil.Read2(nfile);	// Get counts.
		numNpcs = numNpcs1 + EUtil.Read2(nfile);
		mainActor.read(nfile, 0, false);
		npcs.setSize(numNpcs);
		// ++++++bodies.resize(num_npcs);
		int i;
		centerView(mainActor.getTileX(), mainActor.getTileY());
		for (i = 1; i < numNpcs; i++) {	// Create the rest.
			Actor actor = new NpcActor("", 0);
			npcs.set(i, actor);
			actor.read(nfile, i, i < numNpcs1);
			/* ++++++++FINISH
			if (actor.isUnused()) {		// Not part of the game.
				actor.removeThis(1);
				actor.set_schedule_type(Schedule::wait);
			} else
				actor.restore_schedule();
			CYCLE_RED_PLASMA();
			*/
		}
		nfile.close();
		// +++++ mainActor.setActorShape();
		/* ++++++++++FINISH
		try
		{
			U7open(nfile_stream, MONSNPCS);	// Monsters.
			// (Won't exist the first time; in this case U7open throws
			int cnt = nfile.read2();
			(void)nfile.read1();// Read 1 ahead to test.
			int okay = nfile_stream.good();
			nfile.skip(-1);
			while (okay && cnt--)
			{
						// Read ahead to get shape.
				nfile.skip(2);
				unsigned short shnum = nfile.read2()&0x3ff;
				okay = nfile_stream.good();
				nfile.skip(-4);
				ShapeID sid(shnum, 0);
				if (!okay || sid.get_num_frames() < 16)
					break;	// Watch for corrupted file.
				Monster_actor *act = Monster_actor::create(shnum);
				act.read(&nfile, -1, false, fix_unused);
				act.restore_schedule();
				CYCLE_RED_PLASMA();
			}
		}
		catch(exult_exception &) {
			Monster_actor::give_up();
		}
		if (moving_barge)		// Gather all NPC's on barge.
		{
			Barge_object *b = moving_barge;
			moving_barge = 0;
			set_moving_barge(b);
		}
		read_schedules();		// Now get their schedules.
		*/
		centerView(mainActor.getTileX(), mainActor.getTileY());
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
					// Game::set_new_game();
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
	 *	Write out the gamedat directory from a saved game.
	 *
	 *	Output: Aborts if error.
	 */
	void restoreGamedat(String fname) throws IOException {
		/*
						// Check IDENTITY.
		const char *id = get_game_identity(fname);
		const char *static_identity = get_game_identity(INITGAME);
						// Note: "*" means an old game.
		if(!id || (*id != '*' && strcmp(static_identity, id) != 0))
			{
			std::string msg("Wrong identity '");
			msg += id; msg += "'.  Open anyway?";
			int ok = Yesno_gump::ask(msg.c_str());
			if (!ok)
				return;
			}
		*/
		/*
		// Check for a ZIP file first
		if (restore_gamedat_zip(fname) != false)
			return;
		*/
	/*
	#ifdef RED_PLASMA
		// Display red plasma during load...
		setup_load_palette();
	#endif
	*/								
		EUtil.U7mkdir("<GAMEDAT>");		// Create dir. if not already there. Don't
										// use GAMEDAT define cause that's got a
										// trailing slash
		RandomAccessFile in = EUtil.U7open(fname, true);
		if (in == null)
			ExultActivity.fatal("Can't open file: " + EUtil.getSystemPath(fname));
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

		restoreFlexFiles(in, EFile.GAMEDAT);
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
		in.seek(0x80);			// Get to file info.
						// Read pos., length of each file.
		int finfo[] = new int[2*numfiles];
		int i;
		for (i = 0; i < numfiles; i++) {
			finfo[2*i] = EUtil.Read4(in);	// The position, then the length.
			finfo[2*i + 1] = EUtil.Read4(in);
		}
		int baselen = basepath.length();
		byte nm13[] = new byte[13];
		for (i = 0; i < numfiles; i++)	// Now read each file.
			{
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
			String fname = basepath + new String(nm13, 0, nlen);;
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
				// abort("Error writing '%s'.", fname);
				throw e;
			}
			
			// CYCLE_RED_PLASMA();
			}
		}
}
