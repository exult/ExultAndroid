package com.exult.android;
import android.graphics.Point;

public class EffectsManager extends GameSingletons {
	private SpecialEffect effects;	// Sprite effects, projectiles, etc.
	private TextEffect texts;		// Text snippets.
	
	/*
	 *  Add text over a given item.
	 */
	public void addText(String msg, GameObject item) {
		if (msg == null)
			return;
		// Don't duplicate for item.
		for (TextEffect each = texts; each != null; each = each.next)
			if (each.isText(item))
				return;		// Already have text on this.
		TextEffect txt = new TextEffect(msg, item);
		addTextEffect(txt);
	}
	/*
	 *	Add a text object at a given spot.
	 */
	public void addText(String msg, int x, int y) {
		TextEffect txt = new TextEffect(msg,
			gwin.getScrolltx() + x/EConst.c_tilesize, 
			gwin.getScrollty() + y/EConst.c_tilesize);
		addTextEffect(txt);
	}
	/*
	 *	Add a text effect at the start of the chain.
	 */
	public void addTextEffect(TextEffect effect) {
		effect.next = texts;		// Insert into chain.
		effect.prev = null;
		if (effect.next != null)
			effect.next.prev = effect;
		texts = effect;
		}
	/*
	 *	Remove text from the chain and delete it.
	 */
	public void removeTextEffect(TextEffect txt) {
		if (txt.inQueue())
			tqueue.remove(txt);
		if (txt.next != null)
			txt.next.prev = txt.prev;
		if (txt.prev != null)
			txt.prev.next = txt.next;
		else				// Head of chain.
			texts = txt.next;
	}	
	public void removeTextEffect(GameObject item) {
		for (TextEffect each = texts; each != null; each = each.next)
			if (each.isText(item)) {		// Found it.
				removeTextEffect(each);
				gwin.paint();
				return;
			}
	}
	public void removeTextEffects()	{
		while (texts != null)
			removeTextEffect(texts);
		gwin.setAllDirty();
	}

	public void paint() {
		for (SpecialEffect effect = effects; effect != null; effect = effect.next)
		effect.paint();
	}
	public void paintText() {
	for (TextEffect txt = texts; txt != null; txt = txt.next)
		txt.paint();
	}
	/*
	 *	Base class for special-effects:
	 */
	public static abstract class SpecialEffect extends GameSingletons implements TimeSensitive
		{
		private SpecialEffect next, prev;	// All of them are chained together.
						// Render.
		public abstract void paint();
		public boolean isWeather()	// Need to distinguish weather.
			{ return false; }
	}
	/*
	 *	A text object is a message that stays on the screen for just a couple
	 *	of seconds.  These are all kept in a single list, and managed by
	 *	Game_window.
	 */
	public static class TextEffect extends GameSingletons implements TimeSensitive {
		private TextEffect next, prev;	// All of them are chained together.
		private String msg;		// What to print.
		private GameObject item;	// Item text is on.  May be null.
		private Tile tpos;		// Position to display it at.
		private Rectangle pos;
		private int width, height;		// Dimensions of rectangle.
		private int numTicks;			// # ticks passed.
		private static Rectangle updRect = new Rectangle(), dirtyRect = new Rectangle();
		private static Point tempLoc = new Point();
		private int timeQueueCount;
		private void addDirty() {
			// Repaint slightly bigger rectangle.
			dirtyRect.set(pos.x - EConst.c_tilesize,
				       pos.y - EConst.c_tilesize,
					width + 2*EConst.c_tilesize, height + 2*EConst.c_tilesize);
			gwin.clipToWin(dirtyRect);
			gwin.addDirty(dirtyRect);
		}
		private void init() {
			width = 8 + fonts.getTextWidth(0, msg);
			height = 8 + fonts.getTextHeight(0);
			addDirty();			// Force first paint.
							// Start immediately.
			tqueue.add(tqueue.ticks, this, null);
			int from = 0, to = msg.length();
			if (msg.charAt(0) == '@')
				msg = '"' + msg.substring(1, to);;
			if (msg.charAt(to - 1) == '@')
				msg = msg.substring(0, msg.length() - 1) + '"';
		}
		private Rectangle figureTextPos(Rectangle r) {
			if (r == null)
				r = new Rectangle();
			if (item != null) {
							// See if it's in a gump.
				Gump gump = gumpman.findGump(item);
				if (gump != null) {
					gump.getShapeRect(r,item);
					System.out.println("Text posX = " + r.x);
				} else  {
					GameObject outer = item.getOutermost();
					if (outer.getChunk() == null) {
						r.set(0,0,1,1);	// Error?
					} else {
						gwin.getShapeRect(r, outer);
						return r;
					}
				}
			} else {	
				gwin.getShapeLocation(tempLoc, tpos.tx, tpos.ty, tpos.tz);
				r.set(tempLoc.x,tempLoc.y,EConst.c_tilesize, EConst.c_tilesize);
			}
			return r;
		}
		public TextEffect(String m, GameObject it) {
			msg = new String(m);
			item = it;
			pos = figureTextPos(null);
			init();
		}
		public TextEffect(String m, int t_x, int t_y) {
			msg = new String(m);
			tpos = new Tile(t_x, t_y, 0);
			pos = figureTextPos(null);
			init();
		}
							// At timeout, remove from screen.
		public void handleEvent(int ctime, Object udata) {
			if (++numTicks == 10) {		// About 1-2 seconds.
				// All done.
				addDirty();
				eman.removeTextEffect(this);
				return;
			}
						// Back into queue for 1 tick.
			tqueue.add(TimeQueue.ticks + 1, this, null);
			updateDirty();
		}
		public void paint() {
			fonts.paintText(0, msg, pos.x, pos.y);
		}
							// Check for matching item.
		public boolean isText(GameObject it)
			{ return it == item; }
		public void updateDirty() {
			// See if moved.
			Rectangle npos = figureTextPos(updRect);
			if (npos.equals(pos))		// No change?
				return;
			addDirty();			// Force repaint of old area.
			pos.set(npos);			// Then set to repaint new.
			addDirty();
		}
		/*
		 * For TimeSensitive
		 */
		public boolean alwaysHandle() {	
			return true;
		}
		public void addedToQueue() {
			++timeQueueCount;
		}
		public void removedFromQueue() {
			--timeQueueCount;
		}
		public final boolean inQueue() {
			return timeQueueCount > 0;
		}
	}

}	
