package com.exult.android;
import android.graphics.Point;

public final class EffectsManager extends GameSingletons {
	private SpecialEffect effects;	// Sprite effects, projectiles, etc.
	private TextEffect texts;		// Text snippets.
	
	/* 
	 * Add an effect at the start of the chain.
	 */
	public void addEffect(SpecialEffect effect) {
		effect.next = effects;		// Insert into chain.
		effect.prev = null;
		if (effect.next != null)
			effect.next.prev = effect;
			effects = effect;
	}
	/*
	 *	Remove a sprite from the chain and delete it.
	 */
	public void removeEffect(SpecialEffect effect) {
		if (effect.inQueue())
			tqueue.remove(effect);
		if (effect.next != null)
			effect.next.prev = effect.prev;
		if (effect.prev != null)
			effect.prev.next = effect.next;
		else				// Head of chain.
			effects = effect.next;
	}
	public void removeAllEffects() {
		while (effects != null)
			removeEffect(effects);
		while (texts != null)
			removeTextEffect(texts);
	}
	//	Get # of last weather added.
	public int getWeather() {
		for (SpecialEffect effect = effects; effect != null; effect = effect.next)
			if (effect.isWeather()) {
				WeatherEffect weather = (WeatherEffect) effect;
				if (weather.getNum() >= 0)
					return weather.getNum();
			}
		return 0;
	}
	/*
	 *	Remove weather effects.
	 *	@param	dist	Only remove those from eggs at least this far away.
	 */
	void removeWeatherEffects(int dist) {
		Actor mainActor = gwin.getMainActor();
		Tile apos = new Tile();
		if (mainActor != null)
			mainActor.getTile(apos);
		else
			apos.set(-1, -1, -1);
		SpecialEffect each = effects;
		while (each != null) {
			SpecialEffect next = each.next;
						// See if we're far enough away.
			if (each.isWeather() && (dist == 0 ||
			    ((WeatherEffect) each).outOfRange(apos, dist)))
				removeEffect(each);
			each = next;
		}
		gwin.setAllDirty();
	}
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
		private int timeQueueCount;
		protected boolean always;			// For TimeQueue.
		private SpecialEffect next, prev;	// All of them are chained together.
						// Render.
		public void paint() {
		}
		public boolean isWeather()	// Need to distinguish weather.
			{ return false; }
		/*
		 * For TimeSensitive
		 */
		public void addedToQueue() {
				++timeQueueCount;
		}
		public void removedFromQueue() {
			--timeQueueCount;
		}
		public final boolean inQueue() {
			return timeQueueCount > 0;
		}
		public boolean alwaysHandle() {
			return always;
		}
	}
	/*
	 * A mouse flash for about 1/2 second.
	 */
	public static class MouseFlash extends SpecialEffect {
		private int x, y;
		private ShapeFrame shape;
		public MouseFlash(ShapeFrame s, int mx, int my) {
			shape = s;
			x = mx;
			y = my;
			always = true;
			gwin.setAllDirty();
			eman.addEffect(this);
			tqueue.add(tqueue.ticks + (1000/tqueue.tickMsecs)/2, this, null);
		}
		public void paint() {
			shape.paintRle(win, x, y);
		}					// At timeout, remove from screen.
		public void handleEvent(int ctime, Object udata) {
			gwin.setAllDirty();
			eman.removeEffect(this);
		}
	}
	/*
	 * Earthquakes.
	 */
	public static class Earthquake extends SpecialEffect {
		private static boolean soundOnce;
		private int len;			// From Usecode intrinsic.
		private int i;				// Current index.
		private int dx, dy;
		public Earthquake(int l) {
			len = l;
			eman.addEffect(this);
		}
						// Execute when due.
		public void handleEvent(int ctime, Object udata) {
			if (!soundOnce) {
				soundOnce = true;;
				// Play earthquake SFX once
		  		audio.playSfx(Audio.gameSfx(60));
			}
			ImageBuf win = gwin.getWin();
			if (dx != 0) {
				gwin.shiftViewHoriz(dx < 0);
				dx = 0;
			} else {
				dx = EUtil.rand()%9 - 4;
				if (dx != 0)
					gwin.shiftViewHoriz(dx > 0);
			}
			if (dy != 0) {
				gwin.shiftViewVertical(dy < 0);
				dy = 0;
			} else {
				dy = EUtil.rand()%9 - 4;
				if (dy != 0)
					gwin.shiftViewVertical(dy > 0);
			}
			if (++i < len)			// More to do?  Put back in queue.
				tqueue.add(ctime + 1, this, udata);
			else {
				soundOnce = false;	
				eman.removeEffect(this);
			}
		}
	}
	/*
	 *	Weather.
	 */
	public static abstract class WeatherEffect extends SpecialEffect {
		protected int stopTime;		// Time in ticks to stop.
		int num;			// Weather ID (0-6), or -1.
		Tile eggloc;		// Location of egg that started this.
		public WeatherEffect(int duration, int delay, GameObject egg, int n) {
			num = n;
			if (egg != null)
				egg.getTile(eggloc = new Tile());
			stopTime = TimeQueue.ticks + delay + duration*GameClock.ticksPerMinute;
			eman.addEffect(this);
			// Start immediately.
			tqueue.add(TimeQueue.ticks + delay, this, null);
		}
						// Avatar out of range?
		boolean outOfRange(Tile avpos, int dist) {
			if (eggloc == null)
				return false;
			else
				return eggloc.distance(avpos) >= dist;
		}
		public boolean isWeather()
			{ return true; }
		int getNum() 
			{ return num; }
	}
	//	A single cloud.
	private static class Cloud extends GameSingletons {
		ShapeID cloud;
		Rectangle area = new Rectangle();
		int wx, wy;			// Position within world.
		int deltax, deltay;		// How to move.
		int count;			// Counts down to 0.
		int maxCount;
		int startTime;	// When to start.
		static int randcnt;		// For generating random times.
		static final int CLOUD = 2;	// Shape #.
		// Return y<<16 + x.
		int setStartPos(ShapeFrame shape, int w, int h) {
			int x, y;
			if (deltax == 0) {			// Special cases first.
				x = EUtil.rand()%w;
				y = deltay > 0 ? -shape.getYBelow() : 
							h + shape.getYAbove();
			} else if (deltay == 0) {
				y = EUtil.rand()%h;
				x = deltax > 0 ? -shape.getXRight() : w + shape.getXLeft();
			} else {
				int halfp = w + h;		// 1/2 perimeter.
				int r = EUtil.rand()%halfp;		// Start on one of two sides.
				if (r > h) {			// Start on top/bottom.
					x = r - h;
					y = deltay > 0 ? -shape.getYBelow() : 
							h + shape.getYAbove();
				} else {
					y = r;				// On left or right side.
					if (deltax > 0)			// Going right?
						x = -shape.getXRight();
					else				// Going left?
						x = w + shape.getXLeft();
				}
			}
			return (y<<16)|x;
		}
		Cloud(int dx, int dy) {
			cloud = new ShapeID(CLOUD, 0, ShapeFiles.SPRITES_VGA);
			deltax = dx; deltay = dy;
			count = -1;
			int adx = deltax > 0 ? deltax : -deltax;
			int ady = deltay > 0 ? deltay : -deltay;
			if (adx < ady)
				maxCount = 2*gwin.getHeight()/ady;
			else
				maxCount = 2*gwin.getWidth()/adx;
			startTime = 0;
		}
					// Move to next position & paint.
		void next(int curtime, int w, int h) {
			if (curtime < startTime)
				return;			// Not yet.
							// Get top-left world pos.
			int scrollx = gwin.getScrolltx()*EConst.c_tilesize;
			int scrolly = gwin.getScrollty()*EConst.c_tilesize;
			ShapeFrame shape = cloud.getShape();
			gwin.clipToWin(gwin.getShapeRect(area,
					shape, wx - scrollx, wy - scrolly));
			area.enlarge(EConst.c_tilesize/2);
			gwin.addDirty(area);
			if (count <= 0) {			// Time to restart?
							// Set start time randomly.
				randcnt = (randcnt + 1)%4;
				startTime = TimeQueue.ticks + 2*randcnt + EUtil.rand()%3;
				count = maxCount;
				cloud.setFrame(EUtil.rand()%cloud.getNumFrames());
				int pos = setStartPos(shape, w, h);	// Get screen pos.
				int x = pos & 0xffff, y = (pos>>16) & 0xffff; 
				wx = x + scrollx;
				wy = y + scrolly;
			} else {
				wx += deltax;
				wy += deltay;
				count--;
			}
			gwin.clipToWin(gwin.getShapeRect(area,
					shape, wx - scrollx, wy - scrolly));
			area.enlarge(EConst.c_tilesize/2);
			gwin.addDirty(area);
		}
		void paint() {
			if (count > 0)			// Might not have been started.
				cloud.paintShape(
					(int)(wx - gwin.getScrolltx()*EConst.c_tilesize), 
					(int)(wy - gwin.getScrollty()*EConst.c_tilesize));
		}
	}
	//  Clouds floating by.
	public static class CloudsEffect extends WeatherEffect {	
		Cloud clouds[];	
		boolean overcast;
		public CloudsEffect(int duration, int delay, GameObject egg, int n) {
			super(duration, delay, egg, n);
			overcast = (n != 6);
			/* ++++++++FINISH
			if (overcast)
				gclock.set_overcast(true);
			else
				gclock.set_overcast(false);
			*/
			int num_clouds = 2 + EUtil.rand()%5;	// Pick #.
			if (overcast)
				num_clouds += EUtil.rand()%2;
			clouds = new Cloud[num_clouds];
							// Figure wind direction.
			int dx = EUtil.rand()%5 - 2;
			int dy = EUtil.rand()%5 - 2;
			if (dx == 0 && dy == 0) {
				dx = 1 + EUtil.rand()%2;
				dy = 1 - EUtil.rand()%3;
			}
			for (int i = 0; i < num_clouds; i++) {		// Modify speed of some.
				int deltax = dx, deltay = dy;
				if (EUtil.rand()%2 == 0) {
					deltax += deltax/2;
					deltay += deltay/2;
				}
				clouds[i] = new Cloud(deltax, deltay);
			}
			System.out.println("Clouds: num = " + num_clouds + ", len (mins.) = " + duration);
		}
		// Execute when due.
		public void handleEvent(int ctime, Object udata) {
			int delay = 1;
			if (ctime >= stopTime) {			// Time to stop.
				eman.removeEffect(this);
				gwin.setAllDirty();
				return;
			}
			int w = gwin.getWidth(), h = gwin.getHeight();
			for (int i = 0; i < clouds.length; i++)
				clouds[i].next(ctime, w, h);
			tqueue.add(ctime + delay, this, udata);
		}
		// Render.
		public void paint() {
			if (!gwin.isMainActorInside())
				for (int i = 0; i < clouds.length; i++)
					clouds[i].paint();
		}
	}
	
	/*
	 *	A text object is a message that stays on the screen for just a couple
	 *	of seconds.  These are all kept in a single list, and managed by
	 *	Game_window.
	 */
	public static class TextEffect extends SpecialEffect {
		private TextEffect next, prev;	// All of them are chained together.
		private String msg;		// What to print.
		private GameObject item;	// Item text is on.  May be null.
		private Tile tpos;		// Position to display it at.
		private Rectangle pos;
		private int width, height;		// Dimensions of rectangle.
		private int numTicks;			// # ticks passed.
		private static Rectangle updRect = new Rectangle(), dirtyRect = new Rectangle();
		private static Point tempLoc = new Point();
		
		private void addDirty() {
			// Repaint slightly bigger rectangle.
			dirtyRect.set(pos.x - EConst.c_tilesize,
				       pos.y - EConst.c_tilesize,
					width + 2*EConst.c_tilesize, height + 2*EConst.c_tilesize);
			gwin.clipToWin(dirtyRect);
			gwin.addDirty(dirtyRect);
		}
		private void init() {
			always = true;
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
					//System.out.println("Text posX = " + r.x);
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
	}

}	
