package com.exult.android;
import com.exult.android.shapeinf.*;
import android.graphics.Point;
import java.util.Vector;

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
	public static abstract class SpecialEffect extends TimeSensitive.Timer
		{
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
		@Override
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
	 *	A fire field that dies out after a few seconds.
	 */
	public static class FireField extends SpecialEffect {
		private GameObject field;		// What we create.
		public FireField(Tile t) {
			field = IregGameObject.create(895, 0);
			field.setFlag(GameObject.is_temporary);
			field.move(t.tx, t.ty, t.tz);
			tqueue.add(TimeQueue.ticks + 
					(3000 + EUtil.rand()%2000)/TimeQueue.tickMsecs, this, null);
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			int frnum = field.getFrameNum();
			if (frnum == 0) {			// All done?
				field.removeThis();
				eman.removeEffect(this);
			} else {
				if (frnum > 3) {		// Starting to wind down?
					((EggObject) field).stopAnimation();
					frnum = 3;
				} else
					frnum--;
				gwin.addDirty(field);
				field.setFrame(frnum);
				tqueue.add(curtime + 1, this, udata);
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
			clock.setOvercast(overcast);
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
	 *	An animation from 'sprites.vga':
	 */
	public static class SpritesEffect extends SpecialEffect {
		protected ShapeID sprite;
		protected Rectangle area = new Rectangle();
		protected int frames;			// # frames.
		protected GameObject item;		// Follows this around if not null.
		protected Tile pos;			// Position within world.
		protected int xoff, yoff;			// Offset from position in pixels.
		protected int deltax, deltay;		// Add to xoff, yoff on each frame.
		protected int reps;			// Repetitions, or -1.
		protected void addDirty(int frnum) {
			if (pos.tx == -1 || frnum == -1)
				return;			// Already at destination.
			ShapeFrame shape = sprite.getShape();
			int lp = pos.tz/2;
			gwin.getShapeRect(area, shape,
					xoff + (pos.tx - lp - gwin.getScrolltx())*EConst.c_tilesize,
					yoff + (pos.ty - lp - gwin.getScrollty())*EConst.c_tilesize);
			area.enlarge((3*EConst.c_tilesize)/2);
			gwin.clipToWin(area);
			gwin.addDirty(area);
		}
		private void init(int num, int frm, int dx, int dy, int rps) {
			//System.out.printf("SpritesEffect.init: num = %1$d, dx = %2$d, dy = %3$d, rps = %4$d\n",
			//		num, dx, dy, rps);
			//System.out.printf("pos = %1$s\n", pos);
			deltax = dx; deltay = dy;
			reps = rps;
			sprite = new ShapeID(num, frm, ShapeFiles.SPRITES_VGA);
			frames = sprite.getNumFrames();
			tqueue.add(TimeQueue.ticks, this, null);
		}
		public SpritesEffect(int num, Tile p, int dx, int dy, 
				int delay, int frm, int rps) {
			pos = new Tile(p);
			init(num, frm, dx, dy, rps);
		}
		public SpritesEffect(int num, GameObject it, 
				int xf, int yf, int dx, int dy, int frm, int rps) {
			it.getTile(pos = new Tile());
			xoff = xf; yoff = yf;
			item = it;
			init(num, frm, dx, dy, rps);
		}
		@Override		// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			int frame_num = sprite.getFrameNum();
							;// Delay between frames.  Needs to
							//   match usecode animations.
			if (reps == 0 || (reps < 0 && frame_num == frames)) {	// At end?
							// Remove & delete this.
				eman.removeEffect(this);
				gwin.setAllDirty();
				return;
			}
			addDirty(frame_num);		// Clear out old.
			gwin.setPainted();
			if (item != null)			// Following actor?
				item.getTile(pos=new Tile());
			xoff += deltax;			// Add deltas.
			yoff += deltay;
			frame_num++;			// Next frame.
			if (reps > 0) {			// Given a count?
				--reps;
				frame_num %= frames;
			}
			addDirty(frame_num);		// Want to paint new frame.
			sprite.setFrame(frame_num);
							// Add back to queue for next time.
			tqueue.add(time + 1, this, udata);
		}
		@Override		// Render.
		public void paint() {
			
			if (sprite.getFrameNum() >= frames)
				return;
			int lp = pos.tz/2;		// Account for lift.
			int x = xoff + (pos.tx - lp - gwin.getScrolltx())*EConst.c_tilesize;
			int y = yoff + (pos.ty - lp - gwin.getScrollty())*EConst.c_tilesize;
			//System.out.printf("SpritesEffect.paint: frnum = %1$d, frames = %2$d, x = %3$d, y = %4$d\n",
			//		sprite.getFrameNum(), frames, x, y);
			sprite.paintShape(x, y);
		}
	}
	/*
	 *	An explosion.
	 */
	public static class ExplosionEffect extends SpritesEffect	{
		private GameObject explode;		// What's exploding, or 0.
		private int weapon;			// Weapon to use for attack values.
		private int projectile;		// The projectile, for e.g., burst arrows
		private int expSfx;		// Explosion SFX.
		private GameObject attacker;	//Who is responsible for the explosion;
								//otherwise, explosion and delayed blast spells
								//would not trigger a response from target
		private static int getExplosionShape(int weap, int proj) {
			int shp = proj >= 0 ? proj : (weap >= 0 ? weap : 704);
			return ShapeID.getInfo(shp).getExplosionSprite();
		}
		private static int getExplosionSfx(int weap, int proj) {
			int shp = proj >= 0 ? proj : (weap >= 0 ? weap : 704);
			return ShapeID.getInfo(shp).getExplosionSfx();
		}
		public ExplosionEffect(Tile p, GameObject exp, int delay, int weap,
				int proj, GameObject att) {
			super(getExplosionShape(weap, proj), p, 0, 0, delay, 0, -1);
			explode = exp;
			weapon = weap >= 0 ? weap : (proj >= 0 ? proj : 704);
			projectile = proj;
			expSfx = getExplosionSfx(weap, proj);
			attacker = att;
			if (exp != null && exp.getInfo().isExplosive())  // powderkeg
				exp.setQuality(1); // mark as detonating

			if (attacker == null || attacker.asActor() == null)
					// Blame avatar: if we have no living attacker.
				attacker = gwin.getMainActor();
		}
		@Override		// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			int frnum = sprite.getFrameNum();
			if (frnum == 0) {			// Max. volume, with stereo position.
				audio.playSfx(expSfx, pos, Audio.MAX_VOLUME, 0);
			}
			if (frnum == frames/4) {
				// this was in ~Explosion_effect before
				if (explode != null && !explode.isPosInvalid()) {
					gwin.addDirty(explode);
					explode.removeThis();
					explode = null;
				}
				ShapeFrame shape = sprite.getShape();
				int width = shape.getWidth();		//Get the sprite's width
				Vector<GameObject> vec = new Vector<GameObject>();	// Find objects near explosion.
				gmap.findNearby(vec, pos, EConst.c_any_shapenum,
						width/(2*EConst.c_tilesize), 0);
				for (GameObject obj : vec) {
					obj.attacked(attacker, weapon, projectile, true);
				}
			}
			super.handleEvent(time, udata);
		}
	}
	/*
	 *	A moving animation, followed by an 'attack' at the end, to
	 *	implement Usecode intrinsic 0x41:
	 */
	public static class Projectile extends SpecialEffect {
		private Tile tempSrc = new Tile(), tempDest = new Tile();
		private Rectangle dirtyRect = new Rectangle();
		private GameObject attacker;		// Source of attack/spell.
		private GameObject target;		// Target of path.
		private int weapon;			// Shape # of firing weapon.
		private int projectileShape;		// Shape # of projectile/spell.
		private ShapeID sprite;			// Sprite shape to display.
		private int frames;			// # frames.
		private PathFinder path;		// Determines path.
		private Tile pos = new Tile();		// Current position.
		private boolean returnPath;		// Returning a boomerang.
		private boolean noBlocking;		// Don't get blocked by things.
		private boolean skipRender;	// For delayed blast.
						// Add dirty rectangle.
		private int speed;			// Missile speed.
		private int attval;			// Attack value of projectile.
		private boolean autohit;
		private static final int getDir16(Tile t1, Tile t2) {
			return EUtil.getDirection16(t1.ty - t2.ty, t2.tx - t1.tx);
		}
		//	Return target hit, or null.
		private GameObject findTarget(Tile pos) {
			if (pos.tz%5 == 0)		// On floor?
				pos.tz++;		// Look up 1 tile.
			int newz;
			if ((newz = gmap.spotAvailable(1, pos.tx, pos.ty, pos.tz, 
										EConst.MOVE_FLY, 0, -1)) >= 0 &&
											newz == pos.tz)
				return null;
			return GameObject.findBlocking(pos);
		}
		private void addDirty() {
			if (skipRender)
				return;
			ShapeFrame shape = sprite.getShape();
							// Force repaint of prev. position.
			int liftpix = pos.tz*EConst.c_tilesize/2;
			gwin.getShapeRect(dirtyRect, shape, 
					(pos.tx - gwin.getScrolltx())*EConst.c_tilesize - liftpix,
					(pos.ty - gwin.getScrollty())*EConst.c_tilesize - liftpix
				);
			dirtyRect.enlarge(EConst.c_tilesize/2);
			gwin.addDirty(dirtyRect);
		}
		private void init(Tile s, Tile d) {
			noBlocking = false;		// We'll check the ammo & weapon.
			WeaponInfo winfo = ShapeID.getInfo(weapon).getWeaponInfo();
			if (winfo != null) {
				noBlocking = noBlocking || winfo.noBlocking();
				if (speed < 0)
					speed = winfo.getMissileSpeed();
				autohit = winfo.autohits();
				}
			if (speed < 0)
				speed = 4;
			AmmoInfo ainfo = ShapeID.getInfo(projectileShape).getAmmoInfo();
			if (ainfo != null) {
				noBlocking = noBlocking || ainfo.no_blocking();
				autohit = autohit || ainfo.autohits();
			}
			if (attacker != null) {			// Try to set start better.
				int dir = target != null ?
						attacker.getDirection(target) :
						attacker.getDirection(d);
				attacker.getMissileTile(pos, dir);
			} else
				pos.set(s);			// Get starting position.
			tempDest.set(d);
			d = tempDest;
			if (target != null)			// Try to set end better.
				target.getCenterTile(d);
			else
				d.tz = pos.tz;
			path = new ZombiePathFinder();		// Create simple pathfinder.
							// Find path.  Should never fail.
			boolean explodes = (winfo != null && winfo.explodes()) || 
							   (ainfo != null && ainfo.explodes());
			if (explodes && ainfo != null && ainfo.isHoming())
				path.NewPath(pos, pos, null);	//A bit of a hack, I know...
			else
				path.NewPath(pos, d, null);
			int sprite_shape = sprite.getShapeNum();
			setSpriteShape(sprite_shape);
							// Start after a slight delay.
			tqueue.add(TimeQueue.ticks, this, 1);
		}
		/*
		 *	Create a projectile animation.
		 */
		public Projectile
			(
			GameObject att,		// Source of spell/attack.
			GameObject to,		// End here, 'attack' it with shape.
			int weap,			// Weapon (bow, gun, etc.) shape.
			int proj,			// Projectile shape # in 'shapes.vga'.
			int spr,			// Shape to render on-screen or -1 for none.
			int attpts,			// Attack points of projectile.
			int spd				// Projectile speed, or -1 to use default.
			) {
			attacker = att; target = to; weapon = weap; projectileShape = proj;
			sprite = new ShapeID(spr, 0);
			skipRender = spr < 0; speed = spd;
			attval = attpts;
			att.getTile(tempSrc);
			to.getTile(tempDest);
			init(tempSrc, tempDest);
		}
		/*
		 *	Constructor used by missile eggs & fire_projectile intrinsic.
		 */
		public Projectile
			(
			GameObject att,		// Source of spell/attack.
			Tile d,			// End here.
			int weap,			// Weapon (bow, gun, etc.) shape.
			int proj,			// Projectile shape # in 'shapes.vga'.
			int spr,			// Shape to render on-screen or -1 for none.
			int attpts,			// Attack points of projectile.
			int spd,			// Projectile speed, or -1 to use default.
			boolean retpath		// Return of a boomerang.
			) {
			attacker = att; weapon = weap; projectileShape = proj;
			sprite = new ShapeID(spr, 0);
			returnPath = retpath; skipRender = spr < 0; speed = spd;
			attval = attpts;
			att.getTile(tempSrc);
			init(tempSrc, d);
		}
		/* 
		 *	Used by missile eggs and for 'boomerangs'.
		 */
		public Projectile
			(
			Tile s,				// Start here.
			GameObject to,		// End here, 'attack' it with shape.
			int weap,			// Weapon (bow, gun, etc.) shape.
			int proj,			// Projectile shape # in 'shapes.vga'.
			int spr,			// Shape to render on-screen or -1 for none.
			int attpts,			// Attack points of projectile.
			int spd,			// Projectile speed, or -1 to use default.
			boolean retpath			// Return of a boomerang.
			) {
			target = to; weapon = weap; projectileShape = proj;
			sprite = new ShapeID(spr, 0);
			returnPath = retpath; skipRender = spr < 0; speed = spd;
			attval = attpts;
			to.getTile(tempDest);
			init(s, tempDest);
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			int delay = 1;
			addDirty();			// Force repaint of old pos.
			tempSrc.set(pos);		// Save pos.
			Tile epos = tempSrc;
			WeaponInfo winf = ShapeID.getInfo(weapon).getWeaponInfo();
			if (winf != null && winf.getRotationSpeed() != 0) {
					// The missile rotates (such as axes/boomerangs)
				int new_frame = sprite.getFrameNum() + winf.getRotationSpeed();
				sprite.setFrame(new_frame > 23 ? ((new_frame - 8)%16) + 8 
												: new_frame);
			}
			boolean path_finished = false;
			for (int i = 0; i < speed; i++) {
					// This speeds up the missile.
				path_finished = (!path.getNextStep(pos)) ||	// Get next spot.
						// If missile egg, detect target.
					(target == null && !noBlocking && 
								(target = findTarget(pos)) != null);
				if (path_finished)
					break;
				}
			AmmoInfo ainf = ShapeID.getInfo(projectileShape).getAmmoInfo();
			if (path_finished) {			// Done?
				boolean explodes = (winf != null && winf.explodes()) || 
								   (ainf != null && ainf.explodes());
				if (returnPath) {	// Returned a boomerang?
					IregGameObject obj = IregGameObject.create(
												sprite.getShapeNum(), 0);
					if (target == null || !target.add(obj, false)) {
						obj.setFlag(GameObject.okay_to_take);
						obj.setFlag(GameObject.is_temporary);
						obj.move(epos.tx, epos.ty, epos.tz, -1);
					}
				} else if (explodes) {	// Do this here (don't want to explode
										// returning weapon).
					int offz = 0;
					if (target != null)
						offz = target.getInfo().get3dHeight()/2;
					tempDest.set(pos.tx, pos.ty, pos.tz + offz);
					/* +++++++++FINISH
					if (ainf != null && ainf.isHoming())
						;
						eman.addEffect(new HomingProjectile(weapon,
								attacker, target, pos, tempDest));
					else */
						eman.addEffect(new ExplosionEffect(tempDest,
								null, 0, weapon, projectileShape, attacker));
					target = null;	// Takes care of attack.
				} else {		// Not teleported away ?
					boolean returns = (winf != null && winf.returns()) || 
									  (ainf != null && ainf.returns());
					boolean hit = false;
					if (target != null && attacker != target && 
											target.distance(epos) < 3) {
						hit = autohit || target.tryToHit(attacker, attval);
						if (hit) {
							target.playHitSfx(weapon, true);
							target.attacked(attacker, weapon, projectileShape, false);
						}
					} else {
						// Hack warning: this exists solely to make Mind Blast (SI)
						// work as it does in the original when you target the
						// avatar with the spell.
						if (winf != null && winf.getUsecode() > 0)
							ucmachine.callUsecode(winf.getUsecode(), null,
										UsecodeMachine.weapon);
					}
					if (returns && attacker != null &&	// boomerangs
							attacker.distance(epos) < 50) {
						 					// not teleported away
						Projectile proj = new Projectile(
									pos, attacker, weapon, projectileShape,
									sprite.getShapeNum(), attval, speed, true);
						proj.speed = speed;
						proj.setSpriteShape(sprite.getShapeNum());
						eman.addEffect(proj);
					} else {	// See if we should drop projectile.
						boolean drop = false;
							// Seems to match originals quite well.
						if (winf == null)
							drop = true;
						else if (ainf != null) {
							int ammo = winf.getAmmoConsumed(),
								type = ainf.getDropType();
							drop = (ammo >= 0 || ammo == -3) &&
								(type == AmmoInfo.always_drop ||
								(!hit && type != AmmoInfo.never_drop));
						}
						if (drop) {
							if (MapChunk.findSpot(epos, 3,
										sprite.getShapeNum(), 0, 1)) {
								GameObject aobj = IregGameObject.create(
											sprite.getShapeNum(), 0);
								if (attacker == null || attacker.getFlag(GameObject.is_temporary))
									aobj.setFlag(GameObject.is_temporary);
								aobj.setFlag(GameObject.okay_to_take);
								aobj.move(pos);
							}
						}
					}
				}
				addDirty();
				skipRender = true;
				eman.removeEffect(this);
				return;
			}
			addDirty();			// Paint new spot/frame.
							// Add back to queue for next time.
			tqueue.add(curtime + delay, this, udata);
		}
		@Override
		public void paint() {
			if (skipRender)
				return;
			int liftpix = pos.tz*EConst.c_tilesize/2;
			sprite.paintShape(
				(pos.tx - gwin.getScrolltx())*EConst.c_tilesize - liftpix,
				(pos.ty - gwin.getScrollty())*EConst.c_tilesize - liftpix);
		}
		public void setSpriteShape(int s) {
			if (s < 0) {
			skipRender = true;
			sprite.setShape(s);
			sprite.setFrame(0);
			return;
			}
			sprite.setShape(s);
			frames = sprite.getNumFrames();
			if (frames >= 24) {		// Use frames 8-23, for direction
						//   going clockwise from North.
				path.getSrc(tempSrc); 
				path.getDest(tempDest);
				int dir = getDir16(tempSrc, tempDest);
				sprite.setFrame(8 + dir);
			} else if (frames == 1 && sprite.getInfo().isExplosive())
				sprite.setFrame(0);	// (Don't show powder keg!)
			else
				skipRender = true;		// We just won't show it.
			addDirty();			// Paint immediately.
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
