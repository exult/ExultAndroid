package com.exult.android;
import com.exult.android.shapeinf.*;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;

public class EggObject extends IregGameObject {
	protected byte type;		// One of the below types.
	protected byte probability;	// 1-100, chance of egg activating.
	protected byte criteria;	// How it's activated.  See below.
	protected byte distance;		// Distance for activation (0-31).
	protected byte flags;		// Formed from below flags.
	protected short data1, data2, data3;	// More data, dep. on type.
	protected Rectangle area;			// Active area.
	protected boolean solid_area;	// 1 if area is solid, 0 if outline.
	private static Rectangle world = new Rectangle(0, 0, 
			EConst.c_num_chunks*EConst.c_tiles_per_chunk,
			EConst.c_num_chunks*EConst.c_tiles_per_chunk);	
	private static final byte writeBuf[] = new byte[30];

	private static Rectangle inside = new Rectangle();	// A temp.
	Animator animator;		// Controls animation.
	protected void initField(byte ty) {
		type = ty;
		probability = 100;
		data1 = data2 = 0;
		area = new Rectangle(0, 0, 0, 0);
		criteria = party_footpad;
		distance = 0;
		solid_area = false;
		animator = null;
		flags = (1 << auto_reset);
	}
	public static final int // Types of eggs:
		monster = 1,
		jukebox = 2,
		soundsfx = 3,
		voice = 4,
		usecode = 5,
		missile = 6,
		teleport = 7,
		weather = 8,
		path = 9,
		button = 10,
		intermap = 11,
					// Our own:
		fire_field = 128,
		sleep_field = 129,
		poison_field = 130,
		caltrops_field = 131,
		mirror_object = 132
		;
	public static final int // enum Egg_flag_shifts {
		nocturnal = 0,
		once = 1,
		hatched = 2,
		auto_reset = 3
		;
	public static final int // enum Egg_criteria {
		cached_in = 0,		// Activated when chunk read in?
		party_near = 1,
		avatar_near = 2,	// Avatar steps into area.
		avatar_far = 3,		// Avatar steps outside area.
		avatar_footpad = 4,	// Avatar must step on it.
		party_footpad = 5,
		something_on = 6,	// Something placed on/near it.
		external_criteria = 7	// Appears on Isle of Avatar.  Guessing
					//   these set off all nearby.
		;
	public static EggObject createEgg(byte entry[], int entlen,
			boolean animated, int shnum, int frnum, int tx,
			int ty, int tz) {
		short type = (short)(entry[4] + 256*entry[5]);
		byte prob = entry[6];		// Probability (1-100).
		int data1 = ((int)entry[7]&0xff) + 256*((int)entry[8]&0xff);
		int data2 = ((int)entry[10]&0xff) + 256*((int)entry[11]&0xff);
		int data3 = entlen >= 14 ? (entry[12] + 256*entry[13]) : 0;
		return createEgg(animated, shnum, frnum, tx, ty, tz, type, prob, 
				(short)data1, (short)data2, (short)data3, null);
	}
	protected static EggObject createEgg
		(
		boolean animated,
		int shnum, int frnum,
		int tx, int ty, int tz,
		short itype,		// Type + flags, etc.
		byte prob, 
		short data1, short data2, short data3,
		String str1
		) {
		int type = itype&0xf;
							// Teleport destination?
		if (type == teleport && frnum == 6 && shnum == 275)
			type = path;		// (Mountains N. of Vesper).
		EggObject obj = null;
		switch (type) {		// The type:
		case monster:
			obj = new MonsterEgg(shnum, frnum, tx, ty, tz, itype, prob,
								data1, data2, data3);
			break;
		case jukebox:
			obj = new JukeboxEgg(shnum, frnum, tx, ty, tz, itype, prob,
								data1);
			break;
		case soundsfx:
			obj = new SoundsfxEgg(shnum, frnum, tx, ty, tz, itype, prob,
							data1);
			break;
		case voice:
			obj = new VoiceEgg(shnum, frnum, tx, ty, tz, itype, prob,
								data1);
			break;
		case usecode:
			obj = new UsecodeEgg(shnum, frnum, tx, ty, tz, itype, prob,
								data1, data2, str1);
			break;
		case missile:
			obj = new MissileEgg(shnum, frnum, tx, ty, tz, itype, prob,
								data1, data2);
			break;
		case teleport:
		case intermap:
			obj = new TeleportEgg(shnum, frnum, tx, ty, tz, itype, prob,
								data1, data2, data3);
			break;
		case weather:
			obj = new WeatherEgg(shnum, frnum, tx, ty, tz, itype, prob,
							data1, data2);
			break;
		case path:
			obj = new PathEgg(shnum, frnum, tx, ty, tz, itype, prob,
							data1, data2);
			break;
		case button:
			obj = new ButtonEgg(shnum, frnum, tx, ty, tz, itype, prob,
							data1, data2);
			break;
		default:
			System.out.println("Illegal egg itype:  " + type);
			obj = new EggObject(shnum, frnum, tx, ty, tz, itype, prob,
								data1, data2, data3);
		}
		if (animated)
			obj.setAnimator(new Animator.Frame(obj));
		return (obj);
	}

					// Create normal eggs.
	public EggObject(int shapenum, int framenum, int tilex,
			int tiley, int lft, short itype, byte prob, short d1, short d2, short d3) {
		super(shapenum, framenum, tilex, tiley, lft);
		area = new Rectangle();
		probability = prob;
		data1 = d1; data2 = d2; data3 = d3;
		type = (byte)(itype&0xf);
		// Teleport destination?
		if (type == teleport && framenum == 6 && shapenum == 275)
			type = path;		// (Mountains N. of Vesper).
		criteria = (byte)((itype & (7<<4)) >> 4);
		distance = (byte)((itype >> 10) & 0x1f);
		byte noct = (byte)((itype >> 7) & 1);
		byte do_once = (byte)((itype >> 8) & 1);
						// Missile eggs can be rehatched
		byte htch = (byte)((type == missile) ? 0 : ((itype >> 9) & 1));
		solid_area = ((criteria == something_on || criteria == cached_in ||
						// Teleports need solid area.
			type == teleport || type == intermap));
		byte ar = (byte)((itype >> 15) & 1);
		flags = (byte)((noct << nocturnal) + (do_once << once) +
						(htch << hatched) + (ar << auto_reset));
						// Party_near & auto_reset don't mix
						//   well.
		if (criteria == party_near && (flags&(1<<auto_reset)) != 0)
			criteria = avatar_near;
	}
					// Ctor. for fields:
	public EggObject(int shapenum, int framenum, int tilex, 
				int tiley, int lft, byte ty) {
		super(shapenum, framenum, tilex, tiley, lft);
		type = ty;
		probability = 100;
		data1 = data2 = 0;
		area = new Rectangle(0, 0, 0, 0);
		criteria = party_footpad;
		distance = 0;
		solid_area = false;
		animator = null;
		flags = (1 << auto_reset);
	}
	public void setArea() { // Set up active area.
		if (area == null)
			area = new Rectangle();	
		if (probability == 0 || type == path) { // No chance of normal activation?
			area.set(0, 0, 0, 0);
			return;
		}
		int tilex = getTileX(), tiley = getTileY();	// Get absolute tile coords.
		switch (criteria) {		// Set up active area.
		case cached_in:			// Make it really large.
			area.set(tilex - 32, tiley - 32, 64, 64);
			break;
		case avatar_footpad:
		case party_footpad:
			{
			ShapeInfo info = getInfo();
			int frame = getFrameNum();
			int xtiles = info.get3dXtiles(frame), 
			    ytiles = info.get3dYtiles(frame);
			area.set(tilex - xtiles + 1, tiley - ytiles + 1, xtiles, ytiles);
			break;
			}
		case avatar_far:		// Make it 1 tile bigger each dir.
			area.set(tilex - distance - 1, tiley - distance - 1, 
							2*distance + 3, 2*distance + 3);
			break;
		default:
			{
			int width = 2*distance;
			width++;		// Added 8/1/01.
			if (distance <= 1) {	// Small?
							// More guesswork:
				if (criteria == external_criteria)
					width += 2;
			}
			area.set(tilex - distance, tiley - distance, width, width);
			break;
			}
		}
		area.intersect(world);	// Don't go outside the world.
	}
	public final int getDistance()
		{ return distance; }
	public final int getCriteria()
		{ return criteria; }
	public final int getType()
		{ return type; }
	public String getStr1()
		{ return ""; }
	public void setStr1(String s)
		{  }
				// Can this be clicked on?
	public boolean isFindable() {
		if (animator != null)
			return super.isFindable();
		else 
			return gwin.paintEggs && super.isFindable();
	}
	public void set(int crit, int dist) {
		MapChunk echunk = getChunk();
		echunk.removeEgg(this);	// Got to add it back.
		criteria = (byte)crit;
		distance = (byte)dist; 
		echunk.addEgg(this);
	}
	public final boolean isDormant() {
		if ((flags & (1 << hatched)) != 0 &&
				(flags & (1 << auto_reset)) == 0)
			return true;
		else
			return false;
	}
				// Can it be activated?
	public boolean isActive(GameObject obj,
			int tx, int ty, int tz, int from_tx, int from_ty) {	
		if (isDormant())
			return false;		// For now... Already hatched.
		if ((flags & (1 << (int) nocturnal)) != 0) {	// Nocturnal.
				int hour = clock.getHour();
				if (!(hour >= 9 || hour <= 5))
					return false;	// It's not night.
		}
		int cri = getCriteria();
		int deltaz = tz - getLift();
		//System.out.println("Checking criteria " + cri + ", deltaz = " + deltaz);
		switch (cri) {
		case cached_in:			// Anywhere in square.
			// This seems to be true for SI in general. It has the side effect
			// of "fixing" Fawn Tower goblins.
			// It does NOT happen in BG, though.
			if (game.isSI() && deltaz/5 != 0 && type == monster) {
				// Mark hatched if not auto-reset.
				if ((flags & (1 << auto_reset)) == 0)
					flags |= (1 << hatched);
				return false;
			}
			if (obj != gwin.getMainActor() || !area.hasPoint(tx, ty))
				return false;	// Not in square.
			if ((flags & (1 << hatched)) == 0) {
				System.out.println("cached_in egg is active");
				return true;	// First time.
			}
							// Must have autoreset.
							// Just activate when reentering.
			return !area.hasPoint(from_tx, from_ty);
		case avatar_near:
			if (obj != gwin.getMainActor())
				return false;
			// fall through
		case party_near:		// Avatar or party member.
			if (!obj.getFlag(GameObject.in_party) && obj != gwin.getMainActor())
				return false;
			System.out.println("Party near: Area = " + area);
			if (type == teleport ||	// Teleports:  Any tile, exact lift.
				    type == intermap)
				return deltaz == 0 && area.hasPoint(tx, ty);
			else if (type == jukebox || type == soundsfx || type == voice)
				// Guessing. Fixes shrine of Spirituality and Sacrifice.
				return area.hasPoint(tx, ty);
			if (!((deltaz/2 == 0 || 
						// Using trial&error here:
				 (game.isSI() && type != missile) ||
					(type == missile && deltaz/5 == 0)) &&
						// New tile is in, old is out.
						area.hasPoint(tx, ty) &&
						!area.hasPoint(from_tx, from_ty)))
				return false;
			return true;
		case avatar_far:		// New tile is outside, old is inside.
			if (obj != gwin.getMainActor() || !area.hasPoint(tx, ty))
				return false;
			inside.set(area.x + 1, area.y + 1, 
								area.w - 2, area.h - 2);
			return inside.hasPoint(from_tx, from_ty) &&
					!inside.hasPoint(tx, ty);
		case avatar_footpad:
			return obj == gwin.getMainActor() && deltaz == 0 &&
								area.hasPoint(tx, ty);
		case party_footpad:
			return area.hasPoint(tx, ty) && deltaz == 0 &&
						obj.getFlag(GameObject.in_party);
		case something_on:
			return	 		// Guessing.  At SI end, deltaz == -1.
				deltaz/4 == 0 && area.hasPoint(tx, ty) && obj.asActor() == null;
		case external_criteria:
		default:
			return false;
		}
	}
	public final Rectangle getArea()	// Get active area.
		{ return area; }
	public final boolean isSolidArea()
		{ return solid_area; }
	void setAnimator(Animator a) {
		animator = a;
	}
	void stopAnimation() {
		if (animator != null)
			animator.stopAnimation();
	}
	public void paint() {
		if (animator != null) {
			animator.wantAnimation();	// Be sure animation is on.
			super.paint();	// Always paint these.
		} else
			if (gwin.paintEggs)
				super.paint();
	}
				// Run usecode function.
	public void activate(int event) {
		hatch(null, false);
		if (animator != null)
			flags &= ~(1 << (int) hatched);	// Moongate:  reset always.
	}
	public static void setWeather(int weather) {
		setWeather(weather, 15, null);
	}
	public static void setWeather(int weather, int len, GameObject egg) {
		if (len == 0)			// Means continuous.
			len = 6000;		// Confirmed from originals.
		int cur = eman.getWeather();
		// Experimenting.
		if (weather != 4 && (weather == 3 || cur != weather))
			eman.removeWeatherEffects(0);

		switch (weather)
			{
		case 0:		// Back to normal.
			eman.removeWeatherEffects(0);
			break;
		/* +++++++++FINISH
		case 1:		// Snow.
			eman.add_effect(new Snowstorm_effect(len, 0, egg));
			break;
		case 2:		// Storm.
			eman.add_effect(new Storm_effect(len, 0, egg));
			break;
		case 3:		// (On Ambrosia).
			eman.remove_weather_effects();
			eman.add_effect(new Sparkle_effect(len, 0, egg));
			break;
		case 4:		// Fog.
			// ++++ Disabling this.
			//eman.add_effect(new Fog_effect(len, 0, egg));
			break;
		*/
		case 5:		// Overcast.
		case 6:		// Clouds.
			new EffectsManager.CloudsEffect(len, 0, egg, weather);
			break;
		default:
			break;
			}
	}
		// Move to new abs. location.
	public void move(int newtx, int newty, int newlift, int newmap) {
		// Figure new chunk.
		int newcx = newtx/EConst.c_tiles_per_chunk, newcy = newty/EConst.c_tiles_per_chunk;
		GameMap eggmap = newmap >= 0 ? gwin.getMap(newmap) : getMap();
		if (eggmap == null) 
			eggmap = gmap;
		MapChunk newchunk = eggmap.getChunk(newcx, newcy);
		if (newchunk == null)
			return;			// Bad loc.
		removeThis();			// Remove from old.
		setLift(newlift);		// Set new values.
		setShapePos(newtx%EConst.c_tiles_per_chunk, newty%EConst.c_tiles_per_chunk);
		newchunk.addEgg(this);	// Updates cx, cy.
		gwin.addDirty(this);		// And repaint new area.
	}
		// Remove/delete this object.
	public void removeThis() {
		ContainerGameObject owner = getOwner();
		if (owner != null)		// Watch for this.
			owner.remove(this);
		else {
		 	if (chunk != null) {
				gwin.addDirty(this);	// (Make's .move() simpler.).
				chunk.removeEgg(this);
			}
		}
	}
	public boolean isEgg() { 
		return true; 
	}
	/*
	 *	Write out.
	 */

	public void writeIreg(OutputStream out) throws IOException {
		int sz = data3 > 0 ? 14 : 12;
		int ind = writeCommonIreg(sz, writeBuf);
		int tword = type&0xf;// Set up 'type' word.
		tword |= ((criteria&7)<<4);
		tword |= (((flags>>nocturnal)&1)<<7);
		tword |= (((flags>>once)&1)<<8);
		tword |= (((flags>>hatched)&1)<<9);
		tword |= ((distance&0x1f)<<10);
		tword |= (((flags>>auto_reset)&1)<<15);
		EUtil.Write2(writeBuf, ind, tword);
		ind += 2;
		writeBuf[ind++] = probability;
		EUtil.Write2(writeBuf, ind, data1);
		ind += 2;
		writeBuf[ind++] = (byte)((getLift()&15)<<4);
		EUtil.Write2(writeBuf, ind, data2);
		ind += 2;
		if (data3 > 0) {
			EUtil.Write2(writeBuf, ind, data3);
			ind += 2;
		}
		out.write(writeBuf, 0, ind);
		String str1 = getStr1();
		if (str1 != null && str1.length() != 0)	// This will be usecode fun. name.
			GameMap.writeString(out, str1);
						// Write scheduled usecode.
		GameMap.writeScheduled(out, this, false);	
	}
	// Get size of IREG. Returns -1 if can't write to buffer
	public int getIregSize() {
		// These shouldn't ever happen, but you never know
		if (gumpman.findGump(this) != null || UsecodeScript.find(this) != null)
			return -1;
		String str1 = getStr1();
		boolean hasStr = str1 != null && str1.length() != 0;
		return 8 + getCommonIregSize() + ((data3 > 0) ? 2 : 0)
			+ (hasStr ? GameMap.getIregStringLength(str1) : 0);
	}
	public void reset() { 
		flags &= ~(1 << hatched); 
	}
	public void hatchNow(GameObject obj, boolean must)
		{  }
	public void hatch(GameObject obj, boolean must) {
		/*
		  MAJOR HACK!
		  This is an attempt at a work-around of a potential bug in the original
		  Serpent Isle. See SourceForge bug #879253

		  Prevent the Serpent Staff egg from hatching only once
		*/
		if (game.isSI() && getTileX() == 1287 && getTileY() == 2568 && getLift() == 0) {
			flags &= ~(1 << (int) hatched);
		}
		/* end hack */
		int roll = must ? 0 : 1 + EUtil.rand()%100;
		System.out.println("Hatch: roll = " + roll + "prob = " + probability +
				", tx = " + getTileX() + ", ty = " + getTileY());
		if (roll <= probability) {
			// Time to hatch the egg.
			hatchNow(obj, must);
			if ((flags & (1 << (int) once)) != 0) {
				removeThis();
				return;
			}
		}
			// Flag it as done, whether or not it has been hatched.
		flags |= (1 << (int) hatched);
	}
	
	/*
	 *	Each egg type:
	 */
	public static class JukeboxEgg extends EggObject {
		protected byte score;
		protected boolean continuous;
		public JukeboxEgg(int shnum, int frnum, int tx, int ty,
						int tz, short itype, byte prob, short d1) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, (short)0, (short)0);
			score = (byte)(d1&0xff); 
			continuous = (((d1>>8)&1) != 0);
		}
		public void hatchNow(GameObject obj, boolean must) {
			audio.startMusic(score, continuous);
		}
	}
	public static class SoundsfxEgg extends JukeboxEgg {
		public SoundsfxEgg(int shnum, int frnum, int tx, int ty,
			int tz, short itype,
			byte prob, short d1) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1);
		}
		public void hatchNow(GameObject obj, boolean must) {
			/* UNUSED
			int dir = 0;
			if (obj != null) {		// Get direction from obj. to egg.
				dir = EUtil.getDirection16(obj.getTileY() - getTileY(), 
							getTileX() - obj.getTileX());
			}
			*/
			audio.playSfx((int)score, this, Audio.MAX_VOLUME, 
							continuous ? -1 : 0);
		}
	};
	public static class VoiceEgg extends EggObject {
		short speechnum;
		public VoiceEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, (short)0, (short)0);
			speechnum = (d1);
		}
		public void hatchNow(GameObject obj, boolean must) {
			ucmachine.doSpeech(data1&0xff);
		}
	}
	public static class MonsterEgg extends EggObject {
		short mshape;		// For monster.
		byte mframe;
		byte sched, align, cnt;
		void createMonster(MonsterInfo inf) {
			Tile dest = new Tile();
			getTile(dest);
			if (MapChunk.findSpot(dest,5, mshape, 0, 1)) {
				MonsterActor monster = 
				    MonsterActor.create((int)mshape, dest, sched, align);
				monster.changeFrame(mframe);
				gwin.addDirty(monster);
				// +++++FINISH gwin.add_nearby_npc(monster);
			}
		}
		public MonsterEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2, short d3) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1,d2,d3);
			sched = (byte)((d1>>8)&0xff); 
			align = (byte)(d1&3); 
			cnt = (byte)((d1&0xff)>>2);
			if (d3 > 0) {		// Exult extension.
				mshape = d3;
				mframe = (byte)(d2&0xff);
			} else {
				mshape = (short)(d2&1023);
				mframe = (byte)((d2>>10)&0xff);
			}
		}
		public void hatchNow(GameObject obj, boolean must) {
			MonsterInfo inf = 
					ShapeID.getInfo(mshape).getMonsterInfo();
			if (inf != null) {
				/* +++++++FINISH
				if (gwin.armageddon)
					return;
				*/
				int num = cnt;
				if (num > 1)	// Randomize.
					num = 1 + (EUtil.rand()%num);
				while (num-- > 0)
					createMonster(inf);
			} else {		// Create item.
				ShapeInfo info = ShapeID.getInfo(mshape);
				GameObject nobj = IregGameObject.create(info,
				    mshape, mframe, getTx(), getTy(), getLift());
				if (nobj.isEgg())
					chunk.addEgg((EggObject) nobj);
				else
					chunk.add(nobj);
				gwin.addDirty(nobj);
				nobj.setFlag(GameObject.okay_to_take);
						// Objects are created temporary
				nobj.setFlag(GameObject.is_temporary);
			}
		}
	};

	public static class UsecodeEgg extends EggObject {
		short fun;
		String fun_name;		// Actual name in usecode source.
		public UsecodeEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2, String fnm) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2, (short)0);
			
			fun = (d2);  
			setQuality(d1&0xff);
			super.setStr1(fnm);
		}
		public void setStr1(String s) {
			fun_name = s;
			if (s != null && s != "")
				fun = 0;	// Want to look this up.
		}
		public String getStr1()
			{ return fun_name; }
		public void hatchNow(GameObject obj, boolean must) {
			/* ++++++++FINISH
			if (fun == 0 && fun_name != null && fun_name != "")
				fun = ucmachine.findFunction(fun_name);
			*/
			System.out.println("Hatched UsecodeEgg: fun = " + fun);
			if (must)		// From script?  Do immediately.
				ucmachine.callUsecode(fun, this,
						UsecodeMachine.egg_proximity);
			else {			// Do on next animation frame.
				UsecodeValue opv = new UsecodeValue.IntValue(UsecodeScript.usecode);
				UsecodeValue funv = new UsecodeValue.IntValue(fun);
				UsecodeValue code;
				if ((flags & (1<<(int)once)) != 0) {
					code = new UsecodeValue.ArrayValue(opv, funv,
							new UsecodeValue.IntValue(UsecodeScript.remove));
					flags &= ~(1<<(int)once);
				} else
					code = new UsecodeValue.ArrayValue(
						new UsecodeValue.IntValue(UsecodeScript.usecode),
						new UsecodeValue.IntValue(fun));
				UsecodeScript scr = new UsecodeScript(this, code);
				scr.start(1);
			}
		}
	}
	public static class MissileEgg extends EggObject {
		short weapon;
		byte dir, delay;
		MissileLauncher launcher;
		public MissileEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2, (short)0);
			  weapon = d1; 
			  dir = (byte) (d2&0xff); 
			  delay = (byte)((d2>>8)&0xff);
		}
		public void removeThis() {
			if (launcher != null) {		// Stop missiles.
				tqueue.remove(launcher);
				launcher = null;
			}
			super.removeThis();
		}
		public void paint() {
						// Make sure launcher is active.
			if (launcher != null && !launcher.inQueue())
				tqueue.add(0, launcher, null);
			super.paint();
		}
		public void set(int crit, int dist) {
			if (crit == external_criteria && launcher != null) {	// Cancel trap.
				tqueue.remove(launcher);
				launcher = null;
			}
			super.set(crit, dist);
		}
		public void hatchNow(GameObject obj, boolean must) {
			ShapeInfo info = ShapeID.getInfo(weapon);
			WeaponInfo winf = info.getWeaponInfo();
			int proj;
			if (winf != null && winf.getProjectile() != 0)
				proj = winf.getProjectile();
			else
				proj = 856;	// Fireball.  Shouldn't get here.
			if (launcher == null)
				launcher = new MissileLauncher(this, weapon,
				    proj, dir, delay);
			if (!launcher.inQueue())
				tqueue.add(0, launcher, null);
		}
	}
	
	public static class TeleportEgg extends EggObject {
		short mapnum;			// If not -1.
		int destx, desty, destz;
		public TeleportEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2, short d3) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2,d3);
			 mapnum = (short)(-1);
			if (type == intermap)
				mapnum = (short)(d1&0xff);
			else
				setQuality(d1&0xff);	// Teleport egg.
			int schunk = d1 >> 8;
			destx = (schunk%12)*EConst.c_tiles_per_schunk + (d2&0xff);
			desty = (schunk/12)*EConst.c_tiles_per_schunk + (d2>>8);
			destz = d3&0xff;
		}
		public void hatchNow(GameObject obj, boolean must) {
			Tile pos = new Tile(-1, -1, -1);	// Get position to jump to.
			int eggnum = 255;
			if (mapnum == -1)
	 			eggnum = getQuality();
			if (eggnum == 255) {		// Jump to coords.
				pos.set(destx, desty, destz);
			} else {
				getTile(pos);
				Vector<GameObject> vec =	// Look for dest. egg (frame == 6).
					new Vector<GameObject>();
				if (gmap.findNearbyEggs(vec, pos, 275, 256, eggnum, 6) != 0) {
					EggObject path = (EggObject) vec.elementAt(0);
					path.getTile(pos);
				}
			}
			System.out.println("Should teleport to map " + mapnum + 
					", (" + pos.tx + ", " +
						pos.ty + ')');
			if (pos.tx != -1 && obj != null && obj.getFlag(GameObject.in_party))
						// Teleport everyone!!!
				gwin.teleportParty(pos, false, mapnum);
		}
	};

	public static class WeatherEgg extends EggObject {
		byte weather;		// 0-6
		byte len;		// In game minutes.
		public WeatherEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2, (short)0);
			 weather = (byte)(d1&0xff);
			 len = (byte)((d1>>8)&0xff);
			if (len == 0)		// Means continuous.
				len = 120;	// How about a couple game hours?
		}
		public void hatchNow(GameObject obj, boolean must) {
			setWeather(weather, len, this);
		}
	};

	public static class ButtonEgg extends EggObject {
		byte dist;
		public ButtonEgg(int shnum, int frnum, int tx, int ty,
			int tz, short itype, byte prob, short d1, short d2) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2, (short)0);
			 dist = (byte)(d1&0xff);
		}
		public void hatchNow(GameObject obj, boolean must) {
			Vector<GameObject> eggs = new Vector<GameObject>();
			Tile pos = new Tile();
			getTile(pos);
			gmap.findNearbyEggs(eggs, pos, 275, dist, EConst.c_any_qual, 
																EConst.c_any_framenum);
			int cnt = eggs.size();
			for (int i = 0; i < cnt; ++i) {
				EggObject egg = (EggObject)eggs.elementAt(i);
				if (egg != this &&
				    egg.criteria == external_criteria && 
					// Attempting to fix problem in Silver Seed
				    	(egg.flags & (1 << (int) hatched)) == 0) 
					egg.hatch(obj, false);
			}
		}
	}
	public static class PathEgg extends EggObject {
		public PathEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2, (short)0);
			setQuality(d1&0xff);
		}
	}
	/*
	 * Path markers behave a little like eggs.
	 */
	public static final class PathMarker extends IregGameObject {
		public PathMarker(int shapenum, int framenum,
				int tilex, int tiley, int lft) {
			super(shapenum, framenum, tilex, tiley, lft);
		}
					// Render.
		public void paint() {
			if (gwin.paintEggs)
				super.paint();
		}
					// Can this be clicked on?
		public boolean isFindable() {
			return gwin.paintEggs && super.isFindable();
		}
	}
	/*
	 *	Fields are activated like eggs.
	 */
	public static class Field extends EggObject {
		/*
		 *	Apply field.
		 *
		 *	Output:	True to delete field.
		 */
		private boolean fieldEffect(Actor actor) {// Apply field.
			boolean del = false;		// Only delete poison, sleep fields.
			switch ((int)type) {
			case poison_field:
				if (EUtil.rand()%2 != 0 && !actor.getFlag(GameObject.poisoned)) {
					actor.setFlag(GameObject.poisoned);
					del = true;
				}
				break;
			case sleep_field:
				if (EUtil.rand()%2 != 0 && !actor.getFlag(GameObject.asleep)) {
					actor.setFlag(GameObject.asleep);
					del = true;
				}
				break;
			case fire_field:
				actor.reduceHealth(2 + EUtil.rand()%3, WeaponInfo.fire_damage,
											null, null);
							// But no sleeping here.
				actor.clearFlag(GameObject.asleep);
				break;
			case caltrops_field:
				if (actor.getEffectiveProp(Actor.intelligence) < EUtil.rand()%40)
					//actor.reduceHealth(2 + EUtil.rand()%3, Weapon_info.normal_damage);
					// Caltrops don't seem to cause much damage.
					actor.reduceHealth(1 + EUtil.rand()%1, 
										WeaponInfo.normal_damage, null, null);
				return false;
				}
			if (!del)			// Tell animator to keep checking.
				((Animator.FieldFrame) animator).activated = true;
			return del;
		}
		public Field(int shapenum, int framenum, int tilex, 
						int tiley, int lft, byte typ) {
			super(shapenum, framenum, tilex, tiley, lft, typ);
			ShapeInfo info = getInfo();
			if (info.isAnimated())
				setAnimator(new Animator.FieldFrame(this));
		}
		@Override
		public void paint() {
			if (animator != null)
				animator.wantAnimation();	// Be sure animation is on.
			paintObj();	// Always paint these.
		}
		/*
		 *	Run usecode when double-clicked or when activated by proximity.
		 *	(Generally, nothing will happen.)
		 */
		@Override		
		public void activate(int event) {
							// Field_frame_animator calls us with
							//   event==0 to check for damage.
			if (event != UsecodeMachine.npc_proximity) {
				super.activate(event);
				return;
			}
			Vector<GameObject> npcs = new Vector<GameObject>();// Find all nearby NPC's.
			findNearbyActors(npcs, EConst.c_any_shapenum, 
					2*EConst.c_tiles_per_chunk);
			Rectangle eggfoot = new Rectangle(), actfoot = new Rectangle();
			getFootprint(eggfoot);
							// Clear flag to check.
			((Animator.FieldFrame) animator).activated = false;
			for (GameObject obj : npcs) {
				Actor actor = (Actor)obj;
				if (actor.isDead() || distance(actor) > 4)
					continue;
				actor.getFootprint(actfoot);
				if (actfoot.intersects(eggfoot))
					hatch(actor, false);
			}
		}
		@Override
		public void hatch(GameObject obj, boolean must) {
			if (fieldEffect((Actor) obj))// Apply field.
				removeThis();		// Delete sleep/poison if applied.
		}
		@Override		// Write out to IREG file.
		public void writeIreg(OutputStream out) throws IOException {
			iregWriteIreg(out);	// Write as normal ireg.
		}
		@Override		// Get size of IREG. Returns -1 if can't write to buffer
		public int getIregSize() {
			return iregGetIregSize();
		}
		@Override
		public boolean isFindable()
			{ return true; }
	}/*
	 *	Mirrors are handled like eggs.
	 */

	public static class Mirror extends EggObject {
		Tile t = new Tile();
		public Mirror(int shapenum, int framenum,  int tilex, 
			 int tiley,  int lft) {
			super(shapenum, framenum, tilex, tiley, lft, (byte)mirror_object);
				solid_area = true;
		}
		@Override				// Run usecode function.
		public void activate(int event) {
			ucmachine.callUsecode(getUsecode(), this, event);
		}
		@Override
		public void hatch(GameObject obj, boolean must) {
			// These are broken, so dont touch
			if ((getFrameNum()%3) == 2)  
				return;
			int wanted_frame = getFrameNum()/3;
			wanted_frame *= 3;
			// Find upperleft or our area
			getTile(t);
			// To left or above?
			if (getShapeNum()==268) {	// Left
				t.tx++;
				t.ty--;
			}
			else {				// Above
				t.tx--;
				t.ty++;
			}
			// We just want to know if the area is blocked
			if (!MapChunk.areaAvailable(2, 2, 1, t, EConst.MOVE_WALK, 0, 0)) {
				wanted_frame++;
			}
			// Only if it changed update the shape
			if (getFrameNum() != wanted_frame)
				changeFrame(wanted_frame);
		}
		@Override				// Can it be activated?
		public boolean isActive(GameObject obj,
				int tx, int ty, int tz, int from_tx, int from_ty) {
			// These are broken, so dont touch
			int frnum = getFrameNum();
			if (frnum%3 == 2)
				return false;
			if (frnum >= 3 && game.isBG())	// Demon mirror in FOV.
				return false;
			return true;
		}
		@Override
		public void setArea() {		// Set up active area.
			// These are broken, so dont touch
			if ((getFrameNum()%3) == 2) 
				area.set(0, 0, 0, 0);
			
			// Get absolute tile coords.
			int tx = getTileX(), ty = getTileY();
			// To left or above?
			if (getShapeNum()==268) 
				area.set(tx-1, ty-3, 6, 6);
			else  
				area.set(tx-3 , ty-1, 6, 6);
		}

		@Override				// Render.
		public void paint() {
			paintObj();		// Always paint these.
		}
		@Override		// Can this be clicked on?
		public boolean isFindable()
			{ return true; }
		@Override
		public void writeIreg(OutputStream out) throws IOException {
			iregWriteIreg(out);
		}
		@Override		// Get size of IREG. Returns -1 if can't write to buffer
		public int getIregSize() {
			return iregGetIregSize();
		}
	}
	/*
	 *	Timer for a missile egg (type-6 egg).
	 */
	static class MissileLauncher extends TimeSensitive.Timer {
		EggObject egg;		// Egg this came from.
		Tile src = new Tile(), adj = new Tile();
		static Rectangle winRect = new Rectangle();
		int weapon;			// Shape for weapon.
		int shapenum;			// Shape for missile.
		int dir;			// Direction (0-7).  (8==??).
		int delay;			// Delay (msecs) between launches.
		public MissileLauncher(EggObject e, int weap, int shnum, int di, int del) {
			egg = e; weapon = weap; shapenum = shnum; dir = di; delay = del;
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			egg.getTile(src);
			// Is egg off the screen?
			gwin.getWinTileRect(winRect);
			if (!winRect.hasPoint(src.tx, src.ty))
				return;			// Return w'out adding back to queue.
			EffectsManager.Projectile proj = null;
			if (dir < 8) {			// Direction given?
								// Get adjacent tile in direction.
				src.getNeighbor(adj, dir%8);
							// Make it go 20 tiles.
				int dx = adj.tx - src.tx, dy = adj.ty - src.ty;
				src.tx += 20*dx;
				src.ty += 20*dy;
				proj = new EffectsManager.Projectile(egg, src, weapon, 
											shapenum, shapenum, 60, -1, false);
			} else {				// Target a party member.
				int psize = partyman.getCount() + 1;	// Include Avatar.
				int cnt = psize;
				int n = EUtil.rand()%psize;	// Pick one at random.
							// Find one we can hit.
				for (int i = n; proj == null && cnt > 0; 
											cnt--, i = (i + 1)%psize) {
					Actor act = i == 0 ? gwin.getMainActor() : gwin.getNpc(i+1);
					act.getTile(adj);
					if (PathFinder.FastClient.isStraightPath(src, adj))
						proj = new EffectsManager.Projectile(
							src, act, weapon, shapenum, shapenum, 60, -1, false);
				}
			}
			if (proj != null)
				eman.addEffect(proj);
							// Add back to queue for next time.
			tqueue.add(curtime + (delay > 0 ? delay : 1), this, udata);
		}
	}
}
