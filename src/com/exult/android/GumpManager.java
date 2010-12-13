package com.exult.android;
import java.util.LinkedList;
import java.util.ListIterator;

public final class GumpManager extends GameSingletons {	
	private static int gumpCount = 0;			// For staggering them.
	private LinkedList<Gump> openGumps;
	private Gump kbdFocus = null;
	private int nonPersistentCount;
	private boolean dontPauseGame;	// NEVER SET THIS MANUALLY! YOU MUST 
										// CALL set_gumps_dontPauseGame.
	public GumpManager() {
		openGumps = new LinkedList<Gump>();
	}
	/*
	 *	Showing gumps.
	 */
	public boolean showingGumps(boolean no_pers)  {
		// If no gumps, or we do want to check for persistent, just check to see if any exist
		if (!no_pers || openGumps == null) 
			return openGumps != null;

		// If we don't want to check for persistent
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {
			Gump g = iter.next();
			if (!g.isPersistent())
				return true;
		}
		return false;
	}
	public boolean showingGumps() {
		return showingGumps(false);
	}
	public boolean gumpMode() {
		return nonPersistentCount > 0;
	}
	/*
	 *	Find the highest gump that the mouse cursor is on.
	 *
	 *	Output:	.gump, or null if none.
	 */
	public Gump findGump(int x, int y) {
		return findGump(x, y, false);
	}
	public Gump findGump
		(
		int x, int y,			// Pos. on screen.
		boolean pers				// Persistent?
		) {
		Gump found = null;		// We want last found in chain.
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {
			Gump g = iter.next();
			if (g.hasPoint(x,y) && (pers || !g.isPersistent()))
				found = g;
		}
		return (found);
	}
	/*
	 *	Find gump containing a given object.
	 */
	public Gump findGump(GameObject obj) {
						// Get container object is in.
		GameObject owner = obj.getOwner();
		if (owner == null)
			return null;
						// Look for container's gump.
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {
			Gump g = iter.next();
			if (g.getContainer() == owner)
				return (g);
		}
		/* ++++++++FINISH
		Gump *dragged = gwin.get_dragging_gump();
		if (dragged && dragged.get_container() == owner)
			return dragged;
		*/
		return null;
	}
	/*
	 *	Find gump with a given owner & shapenum.
	 */
	public Gump findGump
		(
		GameObject owner,
		int shapenum			// May be c_any_shapenum
		) {
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {	// See if already open.
			Gump g = iter.next();
			if (g.getContainer() == owner &&
			    (shapenum == EConst.c_any_shapenum ||
			     g.getShapeNum() == shapenum))
				return g;
		}
		/* ++++++++++FINISH
		Gump *dragged = gwin.get_dragging_gump();
		if (dragged && dragged.getOwner() == owner &&
			    (shapenum == EConst.c_any_shapenum || 
				 dragged.get_shapenum() == shapenum))
			return dragged;
		*/
		return null;
	}
	// Add to end of list.
	public void addGump(Gump g) {
		setKbdFocus(g);
		openGumps.addLast(g);	
		if (!g.isPersistent()) {	// Count 'gump mode' gumps.
			// And pause the game, if we want it
			nonPersistentCount++;
			if (!dontPauseGame) 
				tqueue.pause(TimeQueue.ticks);
		}
	}
	public void addGump(ContainerGameObject obj, int shapenum, 
								boolean actorgump) {
		boolean paperdoll = false;
		// overide for paperdolls
		/* ++++++++FINISH
		if (actorgump && (sman.can_use_paperdolls() && sman.are_paperdolls_enabled()))
			paperdoll = true;
		Gump *dragged = gwin.get_dragging_gump();
		// If we are dragging the same, just return
		if (dragged && dragged.getOwner() == obj && 
			dragged.get_shapenum() == shapenum)
			return;
		*/
		ListIterator<Gump> iter = openGumps.listIterator();
		Gump gump = null;
		while (iter.hasNext()) {	// See if already open.
			gump = iter.next();
			if (gump.getContainer() == obj &&
				    gump.getShapeNum() == shapenum)
				break;
		}
		if (gump != null) {	// If found, move to end.
			if (iter.hasNext()) {
				iter.remove();
				addGump(gump);
			} else
				setKbdFocus(gump);
			gwin.setAllDirty();
			return;
		}
		int x = (1 + gumpCount)*gwin.getWidth()/10, 
	    	y = (1 + gumpCount)*gwin.getHeight()/10;
		ShapeFiles sfile = paperdoll ? ShapeFiles.PAPERDOL : ShapeFiles.GUMPS_VGA;
    	ShapeFrame shape = sfile.getShape(shapenum, 0);
		if (x + shape.getXRight() > gwin.getWidth() ||
			y + shape.getYBelow() > gwin.getHeight()) {
			gumpCount = 0;
			x = gwin.getWidth()/10;
			y = gwin.getWidth()/10;
		}
		Gump new_gump = null;
		/* ++++++++++FINISH
		Actor npc = null;
		if (obj != null)
			npc = obj.asActor();
		if (npc && paperdoll)
			new_gump = new Paperdoll_gump(npc, x, y, npc.get_npc_num());
		else if (npc && actorgump) 
			new_gump = new Actor_gump(npc, x, y, shapenum);
		else if (shapenum == game.get_shape("gumps/statsdisplay"))
			new_gump = Stats_gump.create(obj, x, y);
		else if (shapenum == game.get_shape("gumps/spellbook"))
			new_gump = new Spellbook_gump((Spellbook_object *) obj);
		else if (Game.get_game_type() == SERPENT_ISLE) {
			if (shapenum == game.get_shape("gumps/spell_scroll"))
				new_gump = new Spellscroll_gump(obj);
			else if (shapenum >= game.get_shape("gumps/cstats/1")&&
					 shapenum <= game.get_shape("gumps/cstats/6"))
				new_gump = new CombatStats_gump(x, y);
			else if (shapenum == game.get_shape("gumps/jawbone"))
				new_gump = new Jawbone_gump((Jawbone_object*) obj, x, y);
		}
		*/
		if (new_gump == null)
			new_gump = new Gump.Container(obj, x, y, shapenum);
		if (++gumpCount == 8)
			gumpCount = 0;
		/* ++++++++++FINISH
		int sfx = Audio.game_sfx(14);
		Audio.get_ptr().play_sound_effect(sfx);	// The weird noise.
		*/
		gwin.setAllDirty();			// Show everything.
	}
	public void closeGump(Gump g) {
		removeGump(g);
	}
	public void removeGump(Gump g) {
		if (g == null)
			return;
		if (g == kbdFocus)
			kbdFocus = null;
		openGumps.remove(g);	
		if (!g.isPersistent()) {	// Count 'gump mode' gumps.
				// And resume queue if last.
				// Gets messed up upon 'load'.
			if (nonPersistentCount > 0)
				nonPersistentCount--;
			if (!dontPauseGame) 
				tqueue.resume(TimeQueue.ticks);
		}
	}
	/*
	 *	End gump mode.
	 */
	public void closeAllGumps(boolean pers) {
		boolean removed = false;
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {		// Remove all gumps.
			Gump gump = iter.next();
			// Don't delete if persistant or modal.
			if ((!gump.isPersistent() || pers) && !gump.isModal()) {
				if (!gump.isPersistent())
					tqueue.resume(TimeQueue.ticks);
				iter.remove();
				removed = true;
			}
		}
		nonPersistentCount = 0;
		setKbdFocus(null);
		/* +++++FINISH
		gwin->get_npc_prox()->wait(4);		// Delay "barking" for 4 secs.
		*/
		if (removed) 
			gwin.setAllDirty();
	}
	public void setKbdFocus(Gump gump) {
		if (gump != null && gump.canHandleKbd()) {
			kbdFocus = gump;
		} else {
			kbdFocus = null;
		}
	}
	public void paint() {
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {
			Gump g = iter.next();
			g.paint();
		}
	}
}
