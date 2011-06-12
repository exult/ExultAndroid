package com.exult.android;
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
    public static void zoom() {
    	ImageBuf win = gwin.getWin();
    	if (zoomCount == 2) {	// Cycle back to full size.
    		zoomCount = 0;
    		win.setSize(EConst.c_game_w, EConst.c_game_h);
    	} else {
    		++zoomCount;
    		win.setSize(win.getWidth()/2, win.getHeight()/2);
    	}
    	gwin.setCenter();
    }
    public static void save() {
    	new NewFileGump();
		gwin.setAllDirty();
    }
    public static void quit() {
    	ExultActivity.quit();
    }
}
