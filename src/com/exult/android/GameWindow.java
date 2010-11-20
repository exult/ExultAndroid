package com.exult.android;
import java.util.Vector;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import android.graphics.Point;

import android.graphics.Canvas;

public class GameWindow {
	private static GameWindow instance;
	private Vector<GameMap> maps;	// Hold all terrain.
	private GameMap map;			// Current map.
	private GameRender render;
	private Rectangle paintBox;		// Temp used for painting.
	private Rectangle tempDirty;	// Temp for addDirty.
	private ImageBuf win;
	private Palette pal;
	// Gameplay objects.
	private Actor mainActor;
	private Actor cameraActor;		// What to center view on.
	Vector<Actor> npcs;
	// Rendering
	private int scrolltx, scrollty;		// Top-left tile of screen.
	private Rectangle scrollBounds;	// Walking outside this scrolls.
	private boolean painted;			// We updated imagebuf.
	private Rectangle dirty;			// What to display.
	//	Game state values.
	private int skipAboveActor;		// Level above actor to skip rendering.
	private int numNpcs1;			// Number of type1 NPC's.
	/*
	 *	Public flags and gameplay options:
	 */
	public int skipLift;	// Skip objects with lift >= this.  0
							//   means 'terrain-editing' mode.
	public int blits;		// For frame-counting.
	
	static public GameWindow instanceOf() {
		return instance;
	}
	public GameWindow(int width, int height) {
		instance = this;
		maps = new Vector<GameMap>(1);
		map = new GameMap(0);
		render = new GameRender();
		maps.add(map);
		win = new ImageBuf(width, height);
		pal = new Palette(win);
		dirty = new Rectangle();
		scrollBounds = new Rectangle();
		paintBox = new Rectangle();
		tempDirty = new Rectangle();
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
	public GameMap getMap() {
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
	public Palette getPal() {
		return pal;
	}
	public Actor getMainActor() {
		return mainActor;
	}
	int getRenderSkipLift()		// Skip rendering here.
	{ return skipAboveActor < skipLift ?
				skipAboveActor : skipLift; }
	// Get screen location for an object.
	public void getShapeLocation(Point loc, int tx, int ty, int tz) {
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
	public void getShapeLocation(Point loc, GameObject obj) {
		getShapeLocation(loc, obj.getTileX(), obj.getTileY(), obj.getLift());
	}
	/*
	 *	Get screen area used by object.
	 */
	Rectangle getShapeRect(Rectangle r, GameObject obj) {
		if (obj.getChunk() == null) {		// Not on map?
			/* +++++FINISH
			Gump *gump = gump_man->find_gump(obj);
			if (gump)
				return gump->get_shape_rect(obj);
			else
			*/
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
		int tx = obj.getTileX(), ty = obj.getTileY(), tz = obj.getLift();
		int lftpix = 4*tz;
		tx += 1 - getScrolltx();
		ty += 1 - getScrollty();
						// Watch for wrapping.
		if (tx < -EConst.c_num_tiles/2)
			tx += EConst.c_num_tiles;
		if (ty < -EConst.c_num_tiles/2)
			ty += EConst.c_num_tiles;
		return getShapeRect(r, s,
			tx*EConst.c_tilesize - 1 - lftpix,
			ty*EConst.c_tilesize - 1 - lftpix);
	}
	public Rectangle getShapeRect(Rectangle r, ShapeFrame s, int x, int y) {
		r.set(x - s.getXLeft(), y - s.getYAbove(),
			s.getWidth(), s.getHeight());
		return r;
	}
	public int getScrolltx() {
		return scrolltx;
	}
	public int getScrollty() {
		return scrollty;
	}
	public void setScrolls(int newscrolltx, int newscrollty) {
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
		set_above_main_actor(nlist.is_roof (tx, ty,
							camera_actor.get_lift()));
		set_in_dungeon(nlist.has_dungeon()?nlist.is_dungeon(tx, ty):0);
		set_ice_dungeon(nlist.is_ice_dungeon(tx, ty));
		*/
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
		/*
		if (gump_man.showing_gumps()) {		// Gump on screen?
			paint();
			return;
		}
		*/
		map.readMapData();		// Be sure objects are present.
		synchronized(win) {
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
		/*
		if (gump_man.showing_gumps())			// Gump on screen?
			{
			paint();
			return;
			}
		*/
		map.readMapData();		// Be sure objects are present.
		synchronized(win) {
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

			// if (main_actor) render.paint_map(gx, gy, gw, gh);
			// else 
			//	win.fill8(0);
			render.paintMap(gx, gy, gw, gh);
			/*
			effects.paint();		// Draw sprites.

			gump_man.paint(false);
			if (dragging) dragging.paint();	// Paint what user is dragging.
			effects.paint_text();
			gump_man.paint(true);

					// Complete repaint?
			if (!gx && !gy && gw == get_width() && gh == get_height() && main_actor)
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
	private void clipToWin(Rectangle r) {
		paintBox.set(0, 0, win.getWidth(), win.getHeight());
		r.intersect(paintBox);
	}
	public void paintDirty() {
		/*
		// Update the gumps before painting, unless in dont_move mode (may change dirty area)
	    if (!main_actor_dont_move())
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
		// if (main_actor != 0) map.read_map_data();		// Gather in all objs., etc.
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
		/* 
		Notebook_gump::initialize();		// Read in journal.
		usecode->read();		// Read the usecode flags
		CYCLE_RED_PLASMA();

		if (Game::get_game_type() == BLACK_GATE)
		{
			string yn;		// Override from config. file.
						// Skip intro. scene?
			config->value("config/gameplay/skip_intro", yn, "no");
			if (yn == "yes")
				usecode->set_global_flag(
					Usecode_machine::did_first_scene, 1);

						// Should Avatar be visible?
			if (usecode->get_global_flag(Usecode_machine::did_first_scene))
				main_actor->clear_flag(Obj_flags::bg_dont_render);
			else
				main_actor->set_flag(Obj_flags::bg_dont_render);
		}

		CYCLE_RED_PLASMA();

		// Fade out & clear screen before palette change
		pal->fade_out(c_fade_out_time);
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
	//+++++The below wasn't prev. done by ::read(), so maybe it should be
	//+++++controlled by a 'first-time' flag.
						// Want to activate first egg.
		MapChunk olist = mainActor.getChunk();
		olist.setupCache();

		int tx = mainActor.getTileX(), ty = mainActor.getTileY(), tz = mainActor.getLift();
		// Do them immediately.
		olist.activateEggs(main_actor, tx, ty, tz, -1,-1,true);
		// Force entire repaint.
		setAllDirty();
		painted = true;			// Main loop uses this.
		gump_man->close_all_gumps(true);		// Kill gumps.
		Face_stats::load_config(config);

		// Set palette for time-of-day.
		clock->reset();
		clock->set_palette();
		pal->fade(6, 1, -1);		// Fade back in.
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
				act->read(&nfile, -1, false, fix_unused);
				act->restore_schedule();
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
			try {
				if (EUtil.U7exists(EFile.PATCH_INITGAME) != null)
					restoreGamedat(EFile.PATCH_INITGAME);
				else {
						// Flag that we're reading U7 file.
					// Game::set_new_game();
					restoreGamedat(EFile.INITGAME);
				}
			} catch (IOException e) {
				//+++++++ABORT
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
				FileOutputStream out = EUtil.U7create(fname);
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
