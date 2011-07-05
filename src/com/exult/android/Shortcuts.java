package com.exult.android;
import android.app.Activity;
import android.graphics.Point;

public final class Shortcuts extends GameSingletons {
	// Get party member, with 0 = Avatar.
	private static Actor getPartyMember(int num) {
		int npc_num = 0;	 	// Default to Avatar
		if (num > 0)
			npc_num = partyman.getMember(num - 1);
		return gwin.getNpc(npc_num);
	}
	public static void target() {
    	Thread t = new Thread() {
    		public void run() {
    			GameObject t = ExultActivity.getTarget(new Point());
    			if (t != null)
    				t.activate();
    		}
    	};
    	t.start();
    }
    public static void combat() {
    	gwin.toggleCombat();
    }
    /*
     * Show inventory page.
     */
    private static int inventoryPage = -1;
    public static void inv() {
    	int party_count = partyman.getCount();
		int shapenum = game.getShape("gumps/statsdisplay");
		Actor actor;
		for(int i=0;i<=party_count;++i) {
			actor = getPartyMember(i);
			if (actor == null)
				continue;

			shapenum = actor.inventoryShapenum();
			// Check if this actor's inventory page is open or not
			if (gumpman.findGump(actor, shapenum) == null) {
				gumpman.addGump(actor, shapenum, true); //force showing inv.
				inventoryPage = i;
				return;
			}
		}
		inventoryPage = (inventoryPage+1)%(party_count+1);
		actor = getPartyMember(inventoryPage);
		if (actor != null) {
			actor.showInventory(); //force showing inv.
		}
    }
    /*
     * Show stats page.
     */
    private static int statsPage = -1;
    public static void stats() {
    	int party_count = partyman.getCount();
    	int shapenum = game.getShape("gumps/statsdisplay");	
    	Actor actor;
	
    	for (int i=0;i<=party_count;++i) {
    		actor = getPartyMember(i);
    		if (actor == null)
    			continue;

    		// Check if this actor's stats page is open or not
    		if (gumpman.findGump(actor, shapenum) == null) {
    			gumpman.addGump(actor, shapenum, false); //force showing stats.
    			statsPage = i;
    			return;
    		}
    	}
    	statsPage = (statsPage+1)%(party_count+1);	
    	actor = getPartyMember(statsPage);
    	if (actor != null)
    		gumpman.addGump(actor, game.getShape("gumps/statsdisplay"), false);
    }
    public static void feed() {
    	
    }
    private static int zoomCount = 0;
    private static float zoomFactor = 1.0f;
    //	Cycle through zooms.
    public static void zoom() {
    	if (zoomFactor > 1.0f) {
    		clearZoom();
    		return;
    	}
    	synchronized(win) {
    		zoomCount = (zoomCount + 1)%4;
    		int w = EConst.c_game_w/(zoomCount + 1), h = EConst.c_game_h/(zoomCount + 1);
    		int x = (EConst.c_game_w - w)/2, y = (EConst.c_game_h - h)/2;
    		System.out.printf("Zoom: %1$d,%2$d,%3$d,%4$d\n", x, y, w, h);
    		win.setZoom(x, y, w, h);
    	}
    	gwin.setPainted();
    }
    public static float getZoomFactor() {
    	return zoomFactor;
    }
    //	Zoom to given factor.  Returns true if size changed.
    public static boolean zoom(float f) {
    	
    	if (f <= 1) {
    		clearZoom();
    		return true;
    	} else {
    		int oldw = win.getZoomWidth(), oldh = win.getZoomHeight();
    		int w = (int) (EConst.c_game_w/f), h = (int) (EConst.c_game_h/f);
    		if (Math.abs(w - oldw) > 10 || Math.abs(h - oldh) > 10) {
    			synchronized(win) {
    				int x = win.getZoomX() + (oldw - w)/2, y = win.getZoomY() + (oldh - h)/2;
    				zoomCount = 0;
    				if (win.setZoom(x, y, w, h))
    					zoomFactor = f;
    				gwin.setPainted();
    				return true;
    			}
    		} else
    			return false;
    	}
    }
    public static void pan(float dx, float dy) {
    	int deltax = (int)(dx*win.getZoomWidth()/win.getWidth());
    	int deltay = (int)(dy*win.getZoomHeight()/win.getHeight());
    	win.pan(deltax, deltay);
    	gwin.setPainted();
    }
    private static void clearZoom() {
    		synchronized(win) {
    		//System.out.println("clearZoom");
    		zoomCount = 0;
    		zoomFactor = 1.0f;
    		win.setZoom(0, 0, EConst.c_game_w, EConst.c_game_h);
    		gwin.setPainted();
    	}
    }
    public static void save(Activity exult) {
    	new NewFileGump();
    	//+++++++new AndroidSave(exult);
		gwin.setAllDirty();
    }
    public static void quit() {
    	ExultActivity.quit();
    }
}
