package com.exult.android;

import com.exult.android.Gump.Container;

public class ActorGump extends Container {
	public static final int TWO_HANDED_BROWN_SHAPE = 48;
	public static final int TWO_HANDED_BROWN_FRAME = 0;
	public static final int TWO_FINGER_BROWN_SHAPE = 48;
	public static final int TWO_FINGER_BROWN_FRAME = 1;
	protected static short coords[] = {	// Coords. of where to draw things,
										//   indexed by spot # (0-11).
		114, 10,	/* head */	115, 24,	/* back */
		115, 37,	/* belt */	115, 55,	/* lhand */
		115, 71,	/* lfinger */	114, 85,	/* legs */
		76, 98,		/* feet */	35, 70,		/* rfinger */
		37, 56,		/* rhand */	37, 37,		/* torso */
		37, 24,		/* neck */	37, 11};	/* ammo */
	protected static int spotx(int i) { return coords[2*i]; }
	protected static int spoty(int i) { return coords[2*i + 1]; }
	
	// Locations:
	protected static short diskx = 124, disky = 115;// 'diskette' button.
	protected static short heartx = 124, hearty = 132;	// 'stats' button.
	protected static short combatx = 52, combaty = 100;	// Combat button.
	protected static short halox = 47, haloy = 110;	// "Protected" halo.
	protected static short cmodex = 48, cmodey = 132;	// Combat mode.
	// Find index of closest spot, or -1 if unsuccessful.
	protected int findClosest(int mx, int my, boolean only_empty) {
		mx -= x; my -= y;		// Get point rel. to us.
		long closest_squared = 1000000;	// Best distance squared.
		int closest = -1;		// Best index.
		for (int i = 0; i < coords.length/2; i++) {
			int dx = mx - spotx(i), dy = my - spoty(i);
			long dsquared = dx*dx + dy*dy;
						// Better than prev.?
			if (dsquared < closest_squared && (!only_empty ||
							container.getReadied(i) == null)) {
				closest_squared = dsquared;
				closest = i;
			}
		}
		return closest;
	}
	protected void setToSpot(GameObject obj, int index) {	
		ShapeFrame shape = obj.getShape();
		if (shape == null)
			return;			// Not much we can do.
		int w = shape.getWidth(), h = shape.getHeight();
					// Set object's position.
		obj.setShapePos(
				spotx(index) + shape.getXLeft() - w/2 - objectArea.x,
				spoty(index) + shape.getYAbove() - h/2 - objectArea.y);
					// Shift if necessary.
		int x0 = obj.getTx() - shape.getXLeft(), 
	    	y0 = obj.getTy() - shape.getYAbove();
	    	int newcx = obj.getTx(), newcy = obj.getTy();
		if (x0 < 0)
			newcx -= x0;
		if (y0 < 0)
			newcy -= y0;
		int x1 = x0 + w, y1 = y0 + h;
		if (x1 > objectArea.w)
			newcx -= x1 - objectArea.w;
		if (y1 > objectArea.h)
			newcy -= y1 - objectArea.h;
		obj.setShapePos(newcx, newcy);
	}
	ActorGump(Actor npc, int initx, int inity, int shnum) {
		super(npc, initx, inity, shnum);	
		setObjectArea(26, 0, 104, 132, 6, 136);
		addElem(new GumpWidget.HeartButton(this, heartx, hearty));
		if (npc.getNpcNum() == 0) {
			addElem(new GumpWidget.DiskButton(this, diskx, disky));
			addElem(new GumpWidget.CombatButton(this, combatx, combaty));
			}
		addElem(new GumpWidget.HaloButton(this, halox, haloy, npc));
		addElem(new GumpWidget.CombatModeButton(this, cmodex, cmodey, npc));
		for (int i = 0; i < coords.length/2; i++) {
					// Set object coords.
			GameObject obj = container.getReadied(i);
			if (obj != null)
				setToSpot(obj, i);
		}
	}
	/*
	 *	Add an object.
	 *
	 *	Output:	0 if cannot add it.
	 */
	public boolean add
		(
		GameObject obj,
		int mx, int my,			// Screen location of mouse.
		int sx, int sy,			// Screen location of obj's hotspot.
		boolean dont_check,		// Skip volume check.
		boolean combine			// True to try to combine obj.  MAY
								//   cause obj to be deleted.
		) {
		GameObject cont = findObject(mx, my);
		
		if (cont != null && cont.add(obj, false, combine, false))
			return true;
		int index = findClosest(mx, my, true);
		if (index != -1 && container.addReadied(obj, index, false, false, false))
			return true;

		if (container.add(obj, dont_check, combine, false))
			return true;

		return false;
	}
	public void paint() {
					// Watch for any newly added objs.
		for (int i = 0; i < coords.length/2; i++) {
				// Set object coords.
		GameObject obj = container.getReadied(i);
		if (obj != null)
			setToSpot(obj, i);
		}
		super.paint();			// Paint gump & objects.

		// Paint over blue lines for 2 handed
		Actor actor = container.asActor();
		if (actor != null) {
			if (actor.isTwoFingered()) {
				int sx = x + 36,	// Note this is the right finger slot shifted slightly
					sy = y + 70;
				ShapeFrame s = ShapeFiles.GUMPS_VGA.getShape(TWO_FINGER_BROWN_SHAPE, TWO_FINGER_BROWN_FRAME);
				s.paint(win, sx, sy);
			}
			if (actor.isTwoHanded()) {
				int sx = x + 36,	// Note this is the right hand slot shifted slightly
					sy = y + 55;
				ShapeFrame s = ShapeFiles.GUMPS_VGA.getShape(TWO_HANDED_BROWN_SHAPE, TWO_HANDED_BROWN_FRAME);
				s.paint(win, sx, sy);
			}
		}
					// Show weight.
		int max_weight = container.getMaxWeight();
		int weight = container.getWeight()/10;
		String text = weight + "/" + max_weight;
		int twidth = fonts.getTextWidth(2, text);
		int boxw = 102;
		fonts.paintText(2, text, x + 28 + (boxw - twidth)/2, y + 120);
	}
	public ContainerGameObject findActor(int mx, int my) {
		return container;
	}
}
