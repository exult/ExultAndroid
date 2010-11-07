package com.exult.android;
import java.util.Vector;

import android.graphics.Canvas;

public class GameWindow {
	private static GameWindow instance;
	private Vector<GameMap> maps;	// Hold all terrain.
	private GameMap map;			// Current map.
	private GameRender render;
	private Rectangle paintBox;		// Temp used for painting.
	private ImageBuf win;
	private Palette pal;
	private int scrolltx, scrollty;		// Top-left tile of screen.
	private boolean painted;			// We updated imagebuf.
	private Rectangle dirty;			// What to display.
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
		paintBox = new Rectangle();
		//GameSingletons.init(this);
		
	}
	//	Prepare for game.
	public void setupGame() {
		getMap(0).init();
		centerView(6*16*16, 6*16*16);//+++++FOR NOW testing.
		//+++++Find other maps here.
		//+++++LOTS MORE to do.
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
		/*
		scroll_bounds.w = scroll_bounds.h = 2;
		scroll_bounds.x = scrolltx + 
				(get_width()/c_tilesize - scroll_bounds.w)/2;
		// OFFSET HERE
		scroll_bounds.y = scrollty + 
				((get_height())/c_tilesize - scroll_bounds.h)/2;

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
		int cx = camera_actor->get_cx(), cy = camera_actor->get_cy();	
		Map_chunk *nlist = map->get_chunk(cx, cy);
		nlist->setup_cache();					 
		int tx = camera_actor->get_tx(), ty = camera_actor->get_ty();
		set_above_main_actor(nlist->is_roof (tx, ty,
							camera_actor->get_lift()));
		set_in_dungeon(nlist->has_dungeon()?nlist->is_dungeon(tx, ty):0);
		set_ice_dungeon(nlist->is_ice_dungeon(tx, ty));
		*/
	}
	//	Center around given tile pos.
	public void centerView(int tx, int ty) {
		int tw = win.getWidth()/EConst.c_tilesize, th = (win.getHeight())/EConst.c_tilesize;
		setScrolls(EConst.DECR_TILE(tx, tw/2), EConst.DECR_TILE(ty, th/2));
		setAllDirty();
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
	public void paint(int x, int y, int w, int h) {
		// if (!win->ready()) return;
		int gx = x, gy = y, gw = w, gh = h;
		if (gx < 0) { gw+=x; gx = 0; }
		if ((gx+gw) > win.getWidth()) gw = win.getWidth()-gx;
		if (gy < 0) { gh += gy; gy = 0; }
		if ((gy+gh) > win.getHeight()) gh = win.getHeight()-gy;
		win.setClip(gx, gy, gw, gh);	// Clip to this area.
	
		int light_sources = 0;

		// if (main_actor) render->paint_map(gx, gy, gw, gh);
		// else 
		//	win->fill8(0);
		render.paintMap(gx, gy, gw, gh);
		/*
		effects->paint();		// Draw sprites.

		gump_man->paint(false);
		if (dragging) dragging->paint();	// Paint what user is dragging.
		effects->paint_text();
		gump_man->paint(true);

					// Complete repaint?
		if (!gx && !gy && gw == get_width() && gh == get_height() && main_actor)
		{			// Look for lights.
		Actor *party[9];	// Get party, including Avatar.
		int cnt = get_party(party, 1);
		int carried_light = 0;
		for (int i = 0; !carried_light && i < cnt; i++)
			carried_light = party[i]->has_light_source();
					// Also check light spell.
		if (special_light && clock->get_total_minutes() >special_light)
			{		// Just expired.
			special_light = 0;
			clock->set_palette();
			}
					// Set palette for lights.
		clock->set_light_source(carried_light + (light_sources > 0),
								in_dungeon);
		}
		*/
	win.clearClip();
	}	
	public void paint(Rectangle r)
		{ paint(r.x, r.y, r.w, r.h); }
	private Rectangle clipToWin(Rectangle r) {
		paintBox.set(0, 0, win.getWidth(), win.getHeight());
		paintBox.intersect(r);
		return paintBox;
	}
	public void paintDirty() {
		/*
		// Update the gumps before painting, unless in dont_move mode (may change dirty area)
	    if (!main_actor_dont_move())
	        gump_man->update_gumps();

		effects->update_dirty_text();
		*/
		Rectangle box = clipToWin(dirty);
		if (box.w > 0 && box.h > 0)
			paint(box);	// (Could create new dirty rects.)
		clearDirty();
	}
	//	Paint whole window.
	public void paint() {
		// if (main_actor != 0) map->read_map_data();		// Gather in all objs., etc.
		setAllDirty();
		paintDirty();
		}
	
}
