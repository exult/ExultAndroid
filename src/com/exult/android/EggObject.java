package com.exult.android;
import com.exult.android.shapeinf.*;
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
	private static Rectangle inside = new Rectangle();	// A temp.
	// +++++FINISH Animator *animator;		// Controls animation.
	protected void initField(byte ty) {
		//+++++++++++++
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
		/* ++++++FINISH
		if (animated)
			obj.setAnimator(new Frame_animator(obj));
		*/
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
		//++++++++FINISH animator = null;
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
		/* +++++++FINISH
		if (animator != null)
			return super.isFindable();
		else */
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
		/* +++++++++++FINISH
		if ((flags & (1 << (int) nocturnal)) != 0) {	// Nocturnal.
				int hour = gclock.getHour();
				if (!(hour >= 9 || hour <= 5))
					return false;	// It's not night.
		}
		*/
		int cri = getCriteria();
		int deltaz = tz - getLift();
		System.out.println("Checking criteria " + cri + ", deltaz = " + deltaz);
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
			System.out.println("Party_near");
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
	/* +++++++FINISH
	void set_animator(Animator *a);
	void stop_animation();
	*/
	public void paint() {
		/* +++++++++
		if (animator) {
			animator.want_animation();	// Be sure animation is on.
			Ireg_game_object::paint();	// Always paint these.
		} else */
			if (gwin.paintEggs)
				super.paint();
	}
				// Run usecode function.
	public void activate(int event) {
		hatch(null, false);
		/* +++++++++FINISH
		if (animator)
			flags &= ~(1 << (int) hatched);	// Moongate:  reset always.
		 */
	}
	public static void setWeather(int weather) {
		setWeather(weather, 15, null);
	}
	public static void setWeather(int weather, int len,
			GameObject egg) {
		//+++++++++FINISH
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
				gwin.addDirty(this);	// (Make's ::move() simpler.).
				chunk.removeEgg(this);
			}
		}
	}
	public boolean isEgg() { 
		return true; 
	}
	/* +++++++FINISH
		// Write out to IREG file.
	virtual void write_ireg(DataSource* out);
	// Get size of IREG. Returns -1 if can't write to buffer
	virtual int get_ireg_size();
	*/
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
			// ++++++++++FINISH Audio::get_ptr().start_music(score, continuous);
		}
	}
	public static class SoundsfxEgg extends JukeboxEgg {
		public SoundsfxEgg(int shnum, int frnum, int tx, int ty,
			int tz, short itype,
			byte prob, short d1) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1);
		}
		public void hatchNow(GameObject obj, boolean must) {
			int dir = 0;
			if (obj != null) {		// Get direction from obj. to egg.
				dir = EUtil.getDirection16(obj.getTileY() - getTileY(), 
							getTileX() - obj.getTileX());
				}
			/* ++++++FINISH
			Audio::get_ptr().play_sound_effect(score, this, AUDIO_MAX_VOLUME, 
								continuous);
			*/
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
			// +++++++FINISH  ucmachine.doSpeech(data1&0xff);
		}
	}
	public static class MonsterEgg extends EggObject {
		short mshape;		// For monster.
		byte mframe;
		byte sched, align, cnt;
		void createMonster(MonsterInfo inf) {
			/*++++++++
			Tile_coord dest = Map_chunk::find_spot(
					get_tile(), 5, mshape, 0, 1);
			*/
			Tile dest = new Tile(); getTile(dest);// +++++TESTING
			if (dest.tx != -1) {
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
		// ++++++++FINISH Missile_launcher *launcher;
		public MissileEgg(int shnum, int frnum, int tx, int ty,
				int tz, short itype, byte prob, short d1, short d2) {
			super(shnum, frnum, tx, ty, tz, itype, prob, d1, d2, (short)0);
			  weapon = d1; 
			  dir = (byte) (d2&0xff); 
			  delay = (byte)((d2>>8)&0xff);
		}
		public void removeThis() {
			/* ++++++++FINISH
			if (launcher) {		// Stop missiles.
				gwin.get_tqueue().remove(launcher);
				delete launcher;
				launcher = 0;
			}
			*/
			super.removeThis();
		}
		public void paint() {
			/* +++++++++++
						// Make sure launcher is active.
			if (launcher && !launcher.in_queue())
				gwin.get_tqueue().add(0L, launcher, 0);
			*/
			super.paint();
		}
		public void set(int crit, int dist) {
			/* +++++++++FINISH
			if (crit == external_criteria && launcher) {	// Cancel trap.
				tqueue.remove(launcher);
				launcher = 0;
			}
			super.set(crit, dist);
			*/
		}
		public void hatchNow(GameObject obj, boolean must) {
			ShapeInfo info = ShapeID.getInfo(weapon);
			/* +++++++++FINISH
			Weapon_info *winf = info.get_weapon_info();
			int proj;
			if (winf && winf.get_projectile())
				proj = winf.get_projectile();
			else
				proj = 856;	// Fireball.  Shouldn't get here.
			if (!launcher)
				launcher = new Missile_launcher(this, weapon,
				    proj, dir, gwin.get_std_delay()*delay);
			if (!launcher.in_queue())
				gwin.get_tqueue().add(0L, launcher, 0);
			*/
		}
	};


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
			// +++++++FINISH setWeather(weather, len, this);
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
}
