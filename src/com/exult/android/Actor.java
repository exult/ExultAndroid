package com.exult.android;
import com.exult.android.shapeinf.*;
import java.io.InputStream;
import java.io.IOException;

public abstract class Actor extends ContainerGameObject implements TimeSensitive {
	protected String name;			// Its name.
	protected int usecode;			// # of usecode function.
	protected boolean usecodeAssigned;		// Usecode # explicitly assigned.
	protected String usecodeName;		// Name of usecode fun explicitly assigned.
	protected boolean unused;			// If npc_num > 0, this NPC is unused in the game.
	protected short npcNum;			// # in Game_window::npcs list, or -1.
	protected short faceNum;			// Which shape for conversations.
	protected short partyId;			// Index in party, or -1.
	protected int properties[] = new int[12];		// Properties set/used in 'usecode'.
	protected byte temperature;	// Measure of coldness (0-63).
	protected short shapeSave;		// Our old shape, or -1.
	protected short oppressor;		// NPC ID (>= 0) of oppressor, or -1.
	protected GameObject target;		// Who/what we're attacking.
	protected short castingMode;		//For displaying casting frames.
	protected int castingShape;	//Shape of casting frames.
	// Walking:
	private static Tile walkSrc = new Tile(), swapTile1 = new Tile(), 
						swapTile2 = new Tile();
	private ZombiePathFinder zombiePath;
	// These 2 are set by the Usecode function 'set_to_attack':
	protected GameObject targetObject;
	protected int target_tile_tx, target_tile_ty, target_tile_tz;
	protected int attackWeapon;
	public static final int		// Attack mode setting from gump.
		nearest = 0,
		weakest = 1,		// Attack weakest.
		strongest = 2,
		berserk = 3,		// Always attack, never retreat.
		protect = 4,		// Protect NPC with halo.
		defend = 5,
		flank = 6,		// Attempt to attack from side.
		flee = 7,
		random = 8,		// Choose target at random.
		manual = 9
		;
	public static final int // Casting mode.
		not_casting = 0,		// The NPC is not casting.
		init_casting = 1,		// When set, the next usecode script will
						// display casting frames (shape 859).
		show_casting_frames = 2	// Used for displaying the casting frames.
						// Also flags that the when the script finishes, the
						// casting frames should be disabled.
		;
	public static final int // type_flags 
		tf_fly = 4,
		tf_walk = 5,
		tf_swim = 6,
		tf_ethereal = 7,
		tf_want_primary = 8,
		tf_sex = 9,
		tf_bleeding = 10,
		tf_in_party = 12,
		tf_in_action = 13,
		tf_conjured = 14,
		tf_summonned = 15
		;
	public static final int // Item_properties
		strength = 0,		// This is also max. health.
		dexterity = 1,
		intelligence = 2,
		health = 3,
		combat = 4,
		mana = 5,
		magic = 6,		// Max. mana.
		training = 7,		// Training points.
		exp = 8,		// Experience.
		food_level = 9,
		sex_flag = 10,	// Seems to be get/set sex type flag in SI.
		missile_weapon = 11	// Pretty sure it returns 1 if wearing
					// weapon with uses >= 2, 0 otherwise.
		;
	public static final int // Frames 0-15.  16-31 are the same,
							//   only S instead of N.
		standing = 0,
		step_right_frame = 1,
		step_left_frame = 2,
		ready_frame = 3,	// Ready to fight?
		raise1_frame = 4,	// 1-handed strikes.
		reach1_frame = 5,
		strike1_frame = 6,
		raise2_frame = 7,	// 2-handed strikes.
		reach2_frame = 8,
		strike2_frame = 9,
		sit_frame = 10,
		bow_frame = 11,
		kneel_frame = 12,
		sleep_frame = 13,
		up_frame = 14,		// Both hands reach up.
		out_frame = 15		// Both hands reach out.
		;	
	public static final int // Describes alignment field.
			neutral = 0,
			friendly = 1,
			hostile = 2,
			unknown_align = 3;	// Bees have this, & don't attack until
						// Party positions
	// protected static short party_pos[4][10][2];

	protected int attackMode;
					// A frame sequence for each dir.:
	protected static FramesSequence avatarFrames[] = new FramesSequence[4];
	protected static FramesSequence npcFrames[] = new FramesSequence[4];
	protected FramesSequence frames[];
	protected byte scheduleType;	// Schedule type (scheduleType).	
	// Location (x,y) of Shedule
	protected int schedule_loc_tx, schedule_loc_ty, schedule_loc_tz;
	protected byte next_schedule;	// Used so correct schedule type 
									//   will be saved
	// protected Schedule *schedule;		// Current schedule.
	protected boolean dormant;			// I.e., off-screen.
	protected boolean hit;			// Just hit in combat.
	protected boolean combatProtected;		// 'Halo' on paperdoll screen.
	protected boolean userSetAttack;		// True if player set attack_mode.
	protected short alignment;		// 'Feelings' towards Ava. See below.
	// Where things can go.  See 'Spots' below for description.
	protected GameObject spots[] = new GameObject[18];	
	protected boolean twoHanded;		// Carrying a two-handed item.
	protected boolean twoFingered;		// Carrying gauntlets (both fingers)
	protected boolean useScabbard;		// Carrying an item in scabbard (belt, back 2h, shield).
	protected boolean useNeck;			// Carrying cloak (amulet, cloak)
	protected byte lightSources;	// # of light sources readied.
	protected byte usecodeDir;	// Direction (0-7) for usecode anim.
	protected int typeFlags;	// 32 flags used in movement among other things
	protected byte gearImmunities;	// Damage immunities granted by gear.
	protected byte gearPowers;		// Other powers granted by gear.

	protected byte ident;
	protected int	skinColor;
	protected ActorAction action;		// Controls current animation.
	protected int frameTime;			// Time between frames in ticks.  0 if
										//   actor not moving.
	protected int stepIndex;			// Index into walking frames, 1 1st.
	protected int qsteps;				// # steps since last quake.

	// Npc_timer_list *timers;		// Timers for poison, hunger, etc.
	protected Rectangle weaponRect;		// Screen area weapon was drawn in.
	protected long restTime;			// # ticks of not doing anything.
	protected int timeQueueCount;		// # times in timeQueue.
	public Actor(String nm, int shapenum, int num, int uc) {
		super(shapenum, 0, 0, 0, 0, 0);
		init();
		frames = npcFrames;
		name = nm;
		usecode = uc; 
		npcNum = (short)num;
		partyId = -1;
		shapeSave = -1;
		oppressor = -1;
		castingShape = -1;
		target_tile_tx = target_tile_ty = -1;
		attackWeapon = -1;
		attackMode = nearest;
		//+++++scheduleType = loiter;
		dormant = true;
		skinColor = -1;
		weaponRect = new Rectangle(0,0,0,0);
	}
	/*
	 *	Initialize frames, properties and spots.
	 */
	private void initDefaultFrames() {
						// Set up actor's frame lists.
						// Most NPC's walk with a 'stand'
						//   frame between steps.
		final int FRAME_NUM = 5;
		final byte	npc_north_frames[] = { 0,  1,  0,  2,  0},
					npc_south_frames[] = {16, 17, 16, 18, 16},
					npc_east_frames[] = {48, 49, 48, 50, 48},
					npc_west_frames[] = {32, 33, 32, 34, 32};
		npcFrames[EConst.north/2] = new FramesSequence(npc_north_frames);
		npcFrames[EConst.south/2] = new FramesSequence(npc_south_frames);
		npcFrames[EConst.east/2] = new FramesSequence(npc_east_frames);
		npcFrames[EConst.west/2] = new FramesSequence(npc_west_frames);
						// Avatar just walks left, right.
		final byte	avatar_north_frames[] = {0, 1, 2},
					avatar_south_frames[] = {16, 17, 18},
					avatar_east_frames[] = {48, 49, 50},
					avatar_west_frames[] = {32, 33, 34};
		avatarFrames[EConst.north/2] = new FramesSequence(avatar_north_frames);
		avatarFrames[EConst.south/2] = new FramesSequence(avatar_south_frames);
		avatarFrames[EConst.east/2] = new FramesSequence(avatar_east_frames);
		avatarFrames[EConst.west/2] = new FramesSequence(avatar_west_frames);
		}

	public void init() {
		if (avatarFrames[0] == null)
			initDefaultFrames();
		int i, len;
		for (i = 0, len = properties.length; i < len; i++)
			properties[i] = 0;
		for (i = 0, len = spots.length; i < len; i++)
			spots[i] = null;
	}
	public Actor asActor() {
		return this;
	}
	public final int getProperty(int prop) {
		/* ++++++FINISH
		if (prop == Actor.sex_flag)
			// Correct in BG and SI, but the flag is never normally set
			// for anyone but avatar in BG.
			return get_type_flag(Actor.tf_sex);
		else if (prop == Actor.missile_weapon)
			{
			// Seems to give the same results as in the originals.
			Game_object *weapon = get_readied(lhand);
			Weapon_info *winf = weapon ? weapon.get_info().get_weapon_info() : 0;
			if (!winf)
				return 0;
			return (winf.get_uses() >= 2);
			}
		*/
		return (prop >= 0 && prop < Actor.sex_flag) ? properties[prop] : 0;
	}
	public final void setProperty(int prop, int val) {
		/* ++++++++++
		if (prop == health && ((partyId != -1) || (npcNum == 0)) && 
				cheat.in_god_mode() && val < properties[prop])
			return;
		*/
		switch (prop) {
		case exp:
			{			// Experience?  Check for new level.
			int old_level = getLevel();
			properties[exp] = val;
			int delta = getLevel() - old_level;
			if (delta > 0)
				properties[training] += 3*delta;
			break;
			}
		case food_level:
			if (val > 31)		// Never seems to get above this.
				val = 31;
			else if (val < 0)
				val = 0;
			properties[prop] = val;
			break;
		case combat:			// These two are limited to 30.
		case magic:
			properties[prop] = val > 30 ? 30 : val;
			break;
		case training:			// Don't let this go negative.
			properties[prop] = val < 0 ? 0 : val;
			break;
		case sex_flag:
			// Doesn't seem to be settable in original BG except by hex-editing
			// the save game, but there is no problem in allowing it in Exult.
			/*++++++FINISH
			if (val != 0)
				setTypeFlag(tf_sex);
			else
				clearTypeFlag(tf_sex);
			*/
			break;
		default:
			if (prop >= 0 && prop < missile_weapon)
				properties[prop] = val;
			break;
		}
		if (gumpman.showingGumps())
			gwin.setAllDirty();
	} 
	public final int getEffectiveProp(int prop) {
		int val = getProperty(prop);
		switch (prop)
			{
		case Actor.dexterity:
		case Actor.combat:
			if (getFlag(GameObject.paralyzed) || getFlag(GameObject.asleep) ||
				getProperty(Actor.health) < 0)
				return prop == Actor.dexterity ? 0 : 1;
		case Actor.intelligence:
			if (isDead())
				return prop == Actor.dexterity ? 0 : 1;
			else if (getFlag(GameObject.charmed) || getFlag(GameObject.asleep))
				val--;
		case Actor.strength:
			if (getFlag(GameObject.might))
				val += (val < 15 ? val : 15);	// Add up to 15.
			if (getFlag(GameObject.cursed))
				val -= 3;	// Matches originals.
			break;
			}
		return val;
	}
	public void setFlag(int flag) {	
		MonsterInfo minf = getInfo().getMonsterInfo();
		if (minf == null)
			minf = MonsterInfo.getDefault();
		switch (flag) {
		/* ++++++++FINISH
		case GameObject.asleep:
			if (minf.sleepSafe() || minf.powerSafe() ||
					(gear_powers&(Frame_flags::power_safe|Frame_flags::sleep_safe)))
				return;		// Don't do anything.
						// Avoid waking Penumbra.
			if (scheduleType == Schedule.sleep && Bg_dont_wake(gwin, this))
				break;
				// Set timer to wake in a few secs.
			need_timers().start_sleep();
			set_action(0);		// Stop what you're doing.
			lay_down(false);	// Lie down.
			break;
		case GameObject.poisoned:
			if (minf.poison_safe() || (gear_powers&Frame_flags::poison_safe))
					return;		// Don't do anything.
			need_timers().start_poison();
			break;
		case GameObject.protection:
			need_timers().start_protection();
			break;
		case GameObject.might:
			need_timers().start_might();
			break;
		case GameObject.cursed:
			if (minf.curse_safe() || minf.power_safe() ||
				(gear_powers&(Frame_flags::power_safe|Frame_flags::curse_safe)))
				return;		// Don't do anything.
			need_timers().start_curse();
			break;
		case GameObject.charmed:
			if (minf.charm_safe() || minf.power_safe() ||
					(gear_powers&(Frame_flags::power_safe|Frame_flags::charm_safe)))
				return;		// Don't do anything.
			need_timers().start_charm();
			set_target(0);		// Need new opponent if in combat.
			break;
		case GameObject.paralyzed:
			if (minf.paralysis_safe() || minf.power_safe() ||
					(gear_powers&(Frame_flags::power_safe|Frame_flags::paralysis_safe)))
				return;		// Don't do anything.
			fall_down();
			need_timers().start_paralyze();
			break;
		case GameObject.invisible:
			flags |= ((uint32) 1 << flag);
			need_timers().start_invisibility();
			Combat_Schedule.stop_attacking_invisible(this);
			gclock.set_palette();
			break;
		*/
		case GameObject.dont_move:
		case GameObject.bg_dont_move:
			stop();			// Added 7/6/03.
			setAction(null);	// Force actor to stop current action.
			break;
		/* +++++++++++++
		case GameObject.naked:
			{
			// set_polymorph needs this, and there are no problems
			// in setting this twice.
			flags2 |= ((uint32) 1 << (flag-32));
			if (get_npc_num() != 0)	// Ignore for all but avatar.
				break;
			int sn;
			int female = getTypeFlag(tf_sex)?1:0;
			Skin_data *skin = Shapeinfo_lookup::GetSkinInfoSafe(this);
	
			if (!skin ||	// Should never happen, but hey...
				(!sman.have_si_shapes() &&
					Shapeinfo_lookup::IsSkinImported(skin.naked_shape)))
				sn = Shapeinfo_lookup::GetBaseAvInfo(female != 0).shape_num;
			else
				sn = skin.naked_shape;
			set_polymorph(sn);
			break;
			}
			*/
		}
		// Doing it here to prevent problems with immunities.
		if (flag >= 0 && flag < 32)
			flags |= (1 << flag);
		else if (flag >= 32 && flag < 64)
			flags2 |= (1 << (flag-32));
				// Update stats if open.
		if (gumpman.showingGumps())
			gwin.setAllDirty();
		setActorShape();
	}
	public final void set_type_flag(int flag) {
		//+++++++++FINISH
	}
	public void clearFlag(int flag) {
		if (flag >= 0 && flag < 32)
			flags &= ~(1 << flag);
		else if (flag >= 32 && flag < 64)
			flags2 &= ~(1 << (flag-32));
		if (flag == GameObject.invisible)	// Restore normal palette.
			;// ++++FINISH gclock.set_palette();
		else if (flag == GameObject.asleep) {
			if (scheduleType == Schedule.sleep)
				setScheduleType(Schedule.stand);
			else if ((getFrameNum()&0xf) == Actor.sleep_frame) {
				/* +++++++++FINISH
						// Find spot to stand.
				Tile pos = get_tile();
				pos.tz -= pos.tz%5;	// Want floor level.
				pos = Map_chunk::find_spot(pos, 6, get_shapenum(),
					Actor.standing, 0);
				if (pos.tx >= 0)
					move(pos);
				changeFrame(Actor.standing);
				*/
			}
			UsecodeScript.terminate(this);
		}
		/*++++++++++++
		else if (flag == GameObject.charmed)
			set_target(0);			// Need new opponent.
		*/
		else if (flag == GameObject.bg_dont_move || flag == GameObject.dont_move)
			// Start again after a little while
			start(1, 1);
		else if (flag == GameObject.polymorph && getFlag(GameObject.naked))
			clearFlag(GameObject.naked);
		else if (flag == GameObject.naked && getFlag(GameObject.polymorph))
			clearFlag(GameObject.polymorph);
		
		setActorShape();
	}
	public void clear_type_flag(int flag) {
		//++++++++FINISH
	}
	public int getTypeFlag(int flag) {
		//++++++++++++FINISH
		return 0;
	}
	public void setTypeFlags(int tflags) {
		typeFlags = tflags;
		/* +++++FINISH set_actor_shape(); */
	}
	public int getTypeFlags() {
		return typeFlags;
	}
	public void setSkinColor (int color) { 
		skinColor = color; 
		/* +++++ set_actor_shape(); */
	}
	public final int getFaceShapeNum() {
		return faceNum;
	}
	public int getUsecode() {
		return usecode == -1 ? super.getUsecode() : usecode;
	}
	public final int getFrameTime() {
		return frameTime;
	}
	public final void setFrameTime(int f) {
		frameTime = f;
	}
	public final boolean isMoving() {
		return frameTime > 0;
	}
	public void clearRestTime() {
		restTime = 0;
	}
	public boolean isDying() {		// Dead when health below -1/3 str.
		return properties[health] < 
				-(properties[strength]/3); 
	}
	public final boolean isDead() {
		return (flags&(1<<GameObject.dead)) != 0; 
	}
	public final int getLevel() {
		return 1 + EUtil.log2(getProperty(exp)/50);
	}
	public final boolean isDormant() {
		return dormant;
	}
	public void setAttribute(String nm, int val) {
		//++++++++++LATER
	}
	public int getAttribute(String nm) {
		return 0;	// +++++++LATER
	}
	public final int getScheduleType() {
		return scheduleType;
	}
	public final void setScheduleType(int s) {
		//+++++++++++FINISH
		scheduleType = (byte)s;
	}
	public final ActorAction getAction() {
		return action;
	}
	public final void setAction(ActorAction newact) {
		action = newact;
		if (action == null)			// No action?  We're stopped.
			frameTime = 0;
	}
	public int get_ident() { return ident; }
	public void set_ident(int id) { ident = (byte)id; }
	public final int getNpcNum() {
		return npcNum;
	}
	public final int getPartyId() {
		return partyId;
	}
	public final void setPartyId(int i) {
		partyId = (short)i;
	}
	public final void setUsecodeDir(int d) {
		usecodeDir = (byte)(d&7);
	}
	public final int getAlignment() {
		return alignment;
	}
	public final void setAlignment(int a) {
		alignment = (short)a;
	}
	public String getName() {
		return !getFlag(GameObject.met)?super.getName():getNpcName();
	}
	public final String getNpcName() {
		return (name == null || name == "") ? super.getName() : name;	
	}
	int getTemperature()	// Get/set measure of coldness.
		{ return temperature; }
	void setTemperature(int v) {	
		if (v < 0)
			v = 0;
		else if (v > 63)
			v = 63;
		temperature = (byte)v;
	}/*
	 *	Figure warmth based on what's worn.  In trying to mimic the original
	 *	SI, the base value is -75.
	 */
	static final int warmthLocs[] = {Ready.head, Ready.cloak, Ready.feet, 
				Ready.torso, Ready.gloves, Ready.legs};
	public final int figureWarmth() {
		int warmth = -75;		// Base value.
		for (int i = 0; i < warmthLocs.length; i++) {
			GameObject worn = spots[warmthLocs[i]];
			/* +++++++FINISH
			if (worn != null)
				warmth += worn.getInfo().getObjectWarmth(
											worn.getFrameNum());
			*/
			}
		return warmth;
		}
	/*
	 *	Get maximum weight in stones that can be held.
	 *
	 *	Output:	Max. allowed, or 0 if no limit (i.e., not carried by an NPC).
	 */
	public int getMaxWeight() {
		return 2*getEffectiveProp(Actor.strength);
	}	
	// Step aside to a free tile, or try to swap places
	protected boolean moveAside(Actor forActor, int dir) {	
		Tile cur = swapTile1; getTile(cur);
		Tile to = swapTile2;
		int i;
		int d = 8;
		// Try orthogonal directions first.
		cur.getNeighbor(to, (dir + 2)%8);
		if (areaAvailable(to, null, getTypeFlags()))
			d = (dir + 2)%8;
		else {
			cur.getNeighbor(to, (dir + 6)%8);
			if (areaAvailable(to, null, getTypeFlags()))
				d = (dir + 6)%8;
			else {
				for (i = 0; i < 4; i++) {		// Try diagonals now.
					cur.getNeighbor(to, 2*i+1);
					if (areaAvailable(to, null, getTypeFlags())) {
						d = 2*i+1;
						break;
					}
				}
			}
		}
		int stepdir = d;		// This is the direction.
		if (d == 8 || to.tx < 0)	// Failed?  Try to swap places.
			return swapPositions(forActor);
					// Step, and face direction.
		step(to, getDirFramenum(stepdir, Actor.standing), false);
		return (getTileX() == to.tx && getTileY() == to.ty);
	}
	protected final GameObject findBlocking(Tile tile, int dir) {
		Rectangle footprint = new Rectangle();
		getFootprint(footprint);
		Rectangle base = new Rectangle(footprint.x, footprint.y, footprint.w,
														footprint.h);
		switch (dir) {
			case EConst.north:
				footprint.shift(0, -1); break;
			case EConst.northeast:
				footprint.shift(1, -1); break;
			case EConst.east:
				footprint.shift(1, 0); break;
			case EConst.southeast:
				footprint.shift(1, 1); break;
			case EConst.south:
				footprint.shift(0, 1); break;
			case EConst.southwest:
				footprint.shift(-1, 1); break;
			case EConst.west:
				footprint.shift(-1, 0); break;
			case EConst.northwest:
				footprint.shift(-1, -1); break;
		}
		GameObject block;
		Tile pos = new Tile();
		for (int i = footprint.x; i < footprint.x+footprint.w; i++)
			for (int j = footprint.y; j < footprint.y+footprint.h; j++)
				if (base.hasPoint(i, j))
					continue;
				else  {
					pos.set(i, j, getLift());
					if ((block = GameObject.findBlocking(pos)) != null)
						return block;
				}
		return null;
	}
	protected final boolean isReallyBlocked(Tile t, boolean force) {
		if (Math.abs(t.tz - getLift()) > 1)
			return true;
		GameObject block = findBlocking(t, getDirection(t));
		if (block == null)
			return true;		// IE, water.
		if (block == this)
			return false;
		Actor a = block.asActor();
						// Try to get blocker to move aside.
		if (a != null && a.moveAside(this, getDirection(block)))
			return false;
		// (May have swapped places.)  If okay, try one last time.
		return ((t.tx != getTileX() || t.ty != getTileY() || t.tz != getLift()) && 
				!areaAvailable(t, null, force ? EConst.MOVE_ALL : 0));
	}
	/*
	 *	Call usecode function for an object that's readied/unreadied.
	 */

	public void callReadiedUsecode
		(
		int index,
		GameObject obj,
		int eventid
		) {
		ShapeInfo info = obj.getInfo();
						// Limit to certain types.
		if (!info.hasUsecodeEvents())
			return;
		if (info.getShapeClass() != ShapeInfo.container)
			ucmachine.callUsecode(obj.getUsecode(), obj, eventid);
	}
	/*
	 *	Returns true if NPC is in a non-no_halt usecode script or if
	 *	dont_move flag is set.
	 */
	public final boolean inUsecodeControl() {
		if (getFlag(GameObject.dont_render) || 
							getFlag(GameObject.dont_move))
			return true;
		/* +++++++++FINISH
		UsecodeScript scr = null;
		while ((scr = UsecodeScript.findActive(this, scr)) != null)
			// no_halt scripts seem not to prevent movement.
			if (!scr.isNoHalt())
				return true;
		*/
		return false;
	}

	public void setActorShape() { 	// Set shape based on sex, skin color
		//+++++FINISH
	}
	public final boolean addDirty(boolean figureWeapon) {
		if (!gwin.addDirty(this))
			return false;
		//+++++++FINISH: Figure casting/weapon rectangle.
		return true;
	}
	public final void changeFrame(int frnum) {
		addDirty(false);			// Set to repaint old area.
		/* FINISH+++++++++++
		ShapeID id(get_shapenum(), frnum, get_shapefile());
		Shape_frame *shape = id.get_shape();
		if (!shape || shape.is_empty())
			{		// Swap 1hand <=> 2hand frames.
			frnum = (frnum&48)|visible_frames[frnum&15];
			id.set_frame(frnum);
			if (!(shape = id.get_shape()) || shape.is_empty())
				frnum = (frnum&48)|Actor.standing;
			}
		*/
		restTime = 0;
		setFrame(frnum);
		addDirty(true);			// Set to repaint new.
	}
	// Get frame seq. for given dir.
	public FramesSequence getFrames(int dir)
		{ return frames[dir/2]; }
	public final int getStepIndex() {
		return stepIndex;
	}
	public final void setStepIndex(int i) {
		stepIndex = i;
	}
	public void paint() {
		int flag = game.isBG() ? GameObject.bg_dont_render : GameObject.dont_render;
		if ((flags & (1L << flag)) == 0) {
			int xoff, yoff;
			gwin.getShapeLocation(paintLoc, this);
			boolean invis = (flags & (1L << GameObject.invisible)) != 0;
			if (invis && partyId < 0 && this != gwin.getMainActor())
				return;	// Don't render invisible NPCs not in party.
			/* ++++++++++FINISH else if (invis)
				paint_invisible(xoff, yoff);
			*/ else
				paintShape(paintLoc.x, paintLoc.y);
			/* +++++++++FINISH
			paint_weapon();
			if (hit)		// Want a momentary red outline.
				ShapeID::paint_outline(xoff, yoff, HIT_PIXEL);
			else if (flags & ((1L<<GameObject.protection) | 
			    (1L << GameObject.poisoned) | (1 << GameObject.cursed) |
			    	(1 << GameObject.charmed) | (1 << GameObject.paralyzed)))
				{
				if (flags & (1L << GameObject.poisoned))
					ShapeID::paint_outline(xoff,yoff,POISON_PIXEL);
				else if (flags & (1L << GameObject.cursed))
					ShapeID::paint_outline(xoff,yoff,CURSED_PIXEL);
				else if (flags & (1L << GameObject.charmed))
					ShapeID::paint_outline(xoff, yoff,
									CHARMED_PIXEL);
				else if (flags & (1L << GameObject.paralyzed))
					ShapeID::paint_outline(xoff, yoff,
									PARALYZE_PIXEL);
				else
					ShapeID::paint_outline(xoff, yoff,
									PROTECT_PIXEL);
				}
			*/
		}
	}
	/*
	 *	Run usecode when double-clicked.
	 */
	public void activate(int event) {
		boolean show_party_inv = gumpman.showingGumps(true) || 
								gwin.inCombat();
		int sched = getScheduleType();
		if (npcNum == 0 ||		// Avatar
			(show_party_inv && partyId >= 0) /* +++++ || // Party
			// Pickpocket cheat && double click
			(cheat.in_pickpocket() && event == 1) */)
			showInventory();
		// Asleep (but not awakened)?
		else if ((sched == Schedule.sleep &&
				(getFrameNum()&0xf) == Actor.sleep_frame) ||
					getFlag(GameObject.asleep))
			return;
		else if (sched == Schedule.combat && partyId < 0)
			return;			// Too busy fighting.
		// Usecode
		// Failed copy-protection?
		else if (game.isSI() &&
				gwin.getMainActor().getFlag(GameObject.confused))
			ucmachine.callUsecode(0x63d, this, event);	
		else if (usecode == -1)
			ucmachine.callUsecode(getUsecode(), this, event);
		else if (partyId >= 0 || !gwin.isTimeStopped())
			ucmachine.callUsecode(getUsecode(), this, event);
	}
	public boolean fitsInSpot(GameObject obj, int spot) {
		ShapeInfo inf = obj.getInfo();
		int rtype = inf.getReadyType(),
			alt1 = inf.getAltReady1(),
			alt2 = inf.getAltReady2();
		boolean can_scabbard = (alt1 == Ready.scabbard || 
					alt2 == Ready.scabbard);
		boolean can_neck = (rtype == Ready.neck || 
					alt1 == Ready.neck || alt2 == Ready.neck);
		if (spot == Ready.both_hands)
			spot = Ready.lhand;
		else if (spot == Ready.lrgloves)
			spot = Ready.lfinger;
		else if (spot == Ready.neck)
			spot = Ready.amulet;
		else if (spot == Ready.scabbard)
			spot = Ready.belt;
		// If occupied, can't place
		if (spots[spot] != null)
			return false;
		// If want to use 2h or a 2h is already equiped, can't go in right
		else if ((rtype == Ready.both_hands || twoHanded) && spot == Ready.rhand)
			return false;
		// If want to use 2f or a 2f is already equiped, can't go in right or gloves
		else if ((rtype == Ready.lrgloves || twoFingered) &&
				(spot == Ready.rfinger || spot == Ready.gloves))
			return false;
		// If want to use scabbard or a scabbard is already equiped, can't go in others
		else if ((can_scabbard || useScabbard) &&
				(spot == Ready.back_2h || spot == Ready.back_shield))
			return false;
		// If want to use neck or neck is already filled, can't go in cloak
		else if ((can_neck || useNeck) && spot == Ready.cloak)
			return false;
		// Can't use 2h in left if right occupied
		else if (rtype == Ready.both_hands && spot == Ready.lhand && 
					spots[Ready.rhand] != null)
			return false;
		// Can't use 2f in left if right occupied
		else if (rtype == Ready.lrgloves && spot == Ready.lfinger && 
					spots[Ready.rfinger] != null)
			return false;
		// Can't use scabbard in belt if back 2h or back shield occupied
		else if (can_scabbard && spot == Ready.belt &&
				(spots[Ready.back_2h] != null || spots[Ready.back_shield] != null))
			return false;
		// Can't use neck in amulet if cloak occupied
		else if (can_neck && spot == Ready.amulet && spots[Ready.cloak] != null)
			return false;
		// If in left or right hand allow it
		else if (spot == Ready.lhand || spot == Ready.rhand)
			return true;
		// Special Checks for Belt
		else if (spot == Ready.belt)
		{
			if (inf.isSpell() || can_scabbard)
				return true;
		}
		// Special Checks for back 2h and back shield
		else if (spot == Ready.back_2h || spot == Ready.back_shield)
		{
			if (can_scabbard)
				return true;
		}
		// Special Checks for amulet and cloak
		else if (spot == Ready.amulet || spot == Ready.cloak)
		{
			if (can_neck)
				return true;
		}

		// Lastly if we have gotten here, check the paperdoll table 
		return inf.isObjectAllowed(obj.getFrameNum(), spot);
	}
	// Return the preferred, alt1, and alt2 slots where an item would prefer
	//  to be readied.
	public void getPreferedSlots(GameObject obj, int pref[]) {
		ShapeInfo info = obj.getInfo();
		pref[0] = info.getReadyType();
		pref[1] = info.getAltReady1();
		pref[2] = info.getAltReady2();
		if (pref[1] < 0)
			pref[1] = Ready.lhand;
		if (pref[0] == Ready.lhand)
			{
			if (info.isObjectAllowed(obj.getFrameNum(), Ready.rhand))
				if (!info.isObjectAllowed(obj.getFrameNum(), Ready.lhand))
					pref[0] = Ready.rhand;
				else
					pref[1] = Ready.rhand;
			else
				pref[1] = Ready.rhand;
			}
	}
	/*
	 *	Find the best spot where an item may be readied.
	 *	Output:	Index, or -1 if none found.
	 */
	public int findBestSpot(GameObject obj) {
		int pref[] = new int[3];

		// Get the preferences
		getPreferedSlots (obj, pref);
		// Check Prefered
		if (fitsInSpot(obj, pref[0])) return pref[0];
		// Alternate
		else if (pref[1] >= 0 && fitsInSpot (obj, pref[1])) return pref[1];
		// Second alternate
		else if (pref[2] >= 0 && fitsInSpot (obj, pref[2])) return pref[2];
		// Belt
		else if (fitsInSpot (obj, Ready.belt)) return Ready.belt;
		// Back - required???
		else if (fitsInSpot (obj, Ready.backpack)) return Ready.backpack;
		// Back2h
		else if (fitsInSpot (obj, Ready.back_2h)) return Ready.back_2h;
		// Sheild Spot
		else if (fitsInSpot (obj, Ready.back_shield)) return Ready.back_shield;
		// Left Hand
		else if (fitsInSpot (obj, Ready.lhand)) return Ready.lhand;
		// Right Hand
		else if (fitsInSpot (obj, Ready.rhand)) return Ready.rhand;

		return -1;
	}
	/*
	 *	Add an object.
	 *
	 *	Output:	1, meaning object is completely contained in this.  Obj may
	 *			be deleted in this case if combine==true.
	 *		0 if not enough space, although obj's quantity may be
	 *			reduced if combine==true.
	 */
	public boolean add(GameObject obj, boolean dont_check,
			boolean combine, boolean noset) {
		int index = findBestSpot(obj);// Where should it go?
		if (npcNum == 0)
		System.out.println("Adding shape " + obj.getShapeNum() +
				", spot = " + index);
		if (index < 0) {		// No free spot?  Look for a bag.
			if (spots[Ready.backpack] != null && 
					spots[Ready.backpack].add(obj, false, combine, false))
				return true;
			if (spots[Ready.belt] != null && 
					spots[Ready.belt].add(obj, false, combine, false))
				return true;
			if (spots[Ready.lhand] != null && 
					spots[Ready.lhand].add(obj, false, combine, false))
				return true;
			if (spots[Ready.rhand] != null && 
					spots[Ready.rhand].add(obj, false, combine, false))
				return true;
			if (!dont_check)
				return false;

			// try again without checking volume/weight
			if (spots[Ready.backpack] != null && 
						spots[Ready.backpack].add(obj, true, combine, false))
				return true;
			if (spots[Ready.belt] != null && 
						spots[Ready.belt].add(obj, true, combine, false))
				return true;
			if (spots[Ready.lhand] != null && 
						spots[Ready.lhand].add(obj, true, combine, false))
				return true;
			if (spots[Ready.rhand] != null && 
						spots[Ready.rhand].add(obj, true, combine, false))
				return true;

			if (partyId != -1 || npcNum==0) {
				// WARNING.
			}
			return super.add(obj, dont_check, combine, false);
		}
						// Add to ourself (DON'T combine).
		if (!super.add(obj, true, false, false))
			return false;

		if (index == Ready.both_hands) {	// Two-handed?
			twoHanded = true;
			index = Ready.lhand;
		} else if (index == Ready.lrgloves) {	// BG Gloves?
			twoFingered = true;
			index = Ready.lfinger;
		} else if (index == Ready.scabbard) {		// Use scabbard?
			useScabbard = true;
			index = Ready.belt;
		} else if (index == Ready.neck) {		// Use neck?
			useNeck = true;
			index = Ready.amulet;
		}

		spots[index] = obj;		// Store in correct spot.
		/* ++++++++++FINISH
		if (index == Ready.lhand && schedule != null && !noset)
			schedule.setWeapon();	// Tell combat-schedule about it.
		obj.setShapePos(0, 0);	// Clear coords. (set by gump).
		if (!dont_check)
			callReadiedUsecode(index, obj, UsecodeMachine.readied);
						// (Readied usecode now in drop().)
		*/
		ShapeInfo info = obj.getInfo();

		if (info.isLightSource() &&
				(index == Ready.lhand || index == Ready.rhand))
			lightSources++;

		// Refigure granted immunities.
		/* ++++++FINISH
		gear_immunities |= info.getArmorImmunity();
		gear_powers |= info.getObjectFlags(obj.getFrameNum(),
					info.hasQuality() ? obj.getQuality() : -1);
		*/
		return true;
	}
	public void remove(GameObject obj) {
		int index = findReadied(obj);	// Remove from spot.
		// Note:  gwin.drop() also does this,
		//   but it needs to be done before
		//   removal too.
		// Definitely DO NOT call if dead!
		/* +++++++++FINISH
		if (!isDead() && !ucmachine.inUsecodeFor(
									obj, UsecodeMachine.unreadied))
			callReadiedUsecode(index, obj, UsecodeMachine.unreadied);
		*/
		super.remove(obj);
		if (index >= 0) {
			spots[index] = null;
			if (index == Ready.rhand || index == Ready.lhand)
				twoHanded = false;
			if (index == Ready.rfinger || index == Ready.lfinger)
				twoFingered = false;
			if (index == Ready.belt || 
					index == Ready.back_2h || index == Ready.back_shield)
				useScabbard = false;
			if (index == Ready.amulet || index == Ready.cloak)
				useNeck = false;
			/* ++++++++++FINISH
			if (index == lhand && schedule != null)
				schedule.setWeapon(true);
			// Recheck armor immunities and frame powers.
			refigure_gear();
			*/
		}
	}
	/*
	 *	Find index of spot where an object is readied.
	 *
	 *	Output:	Index, or -1 if not found.
	 */
	public int findReadied(GameObject obj) {
		for (int i = 0; i < spots.length; i++)
			if (spots[i] == obj)
				return (i);
		return (-1);
	}
	public void showInventory() {
		int shapenum = inventoryShapenum();
		if (shapenum >= 0)
			gumpman.addGump(this, shapenum, true);
	}
	public int inventoryShapenum() {
		// We are serpent if we can use serpent isle paperdolls
		boolean serpent = false; // +++++FINISH(sman.can_use_paperdolls() && sman.are_paperdolls_enabled());
		
		if (!serpent) {
				// Can't display paperdolls (or they are disabled)
				// Use BG gumps
			int gump = getInfo().getGumpShape();
			/* +++++++FINISH
			if (gump < 0)
				gump = ShapeID::get_info(get_sexed_coloured_shape()).get_gump_shape();
			if (gump < 0)
				gump = ShapeID::get_info(get_shape_real()).get_gump_shape();
			
			if (gump < 0) {
				int shape = getTypeFlag(Actor.tf_sex) ?
					Shapeinfo_lookup::GetFemaleAvShape() :
					Shapeinfo_lookup::GetMaleAvShape();
				gump = ShapeID::get_info(shape).get_gump_shape();
			}
			*/
			if (gump < 0)
				// No gump at ALL; should never happen...
				return (65);	// Default to male (Pickpocket Cheat)
			return gump;
		} else /* if (serpent) */
			return (123);		// Show paperdolls
	}
	GameObject getReadied(int index) {
		return index >= 0 && index < spots.length ? spots[index] : null;
	}
	/*
	 *	Drop another onto this.
	 *
	 *	Output:	0 to reject, 1 to accept.
	 */
	// Drop another onto this one.
	public boolean drop(GameObject obj) {
		if (getFlag(GameObject.in_party)) {	// In party?
			boolean res = add(obj, false, true, false);// We'll take it, and combine.
			/* +++++++++FINISH
			int ind = findReadied(obj);
			if (ind >= 0)
				callReadiedUsecode(ind,obj,Usecode_machine::readied);
			*/
			return res;
		} else
			return false;
	}
	/*
	 *	Begin animation.
	 */
	public void start
		(
		int speed,			// Time between frames (ticks).
		int delay			// Delay before starting (ticks) (only
							//   if not already moving).
		) {
		dormant = false;		// 14-jan-2001 - JSF.
		frameTime = speed;
		if (!inQueue() || delay > 0) {	// Not already in queue?
			if (delay > 0)
				gwin.getTqueue().remove(this);
			int curtime = TimeQueue.ticks;
			gwin.getTqueue().add(curtime + delay, this, gwin);
			}
	}
	public void stop() {
		if (action != null) {
			action.stop(this);
			addDirty(false);
		}
		frameTime = 0;
	}
	public final boolean canAct() {
		return !(getFlag(GameObject.paralyzed) || getFlag(GameObject.asleep)
				|| isDead() || getProperty(health) <= 0);
	}
	/*
	 *	Walk towards a given tile.
	 */
	public void walkToTile
		(
		Tile dest,			// Destination.
		int speed,			// Time between frames (ticks).
		int delay,			// Delay before starting (ticks) (only
							//   if not already moving).
		int maxblk			// Max. # retries if blocked.
		) {
		if (zombiePath == null)
			zombiePath = new ZombiePathFinder();
		if (action == null)
			action = new ActorAction.PathWalkingActorAction(zombiePath, maxblk);
		getTile(walkSrc);
		setAction(action.walkToTile(this, walkSrc, dest, 0));
		if (action != null)			// Successful at setting path?
			start(speed, delay);
		else
			frameTime = 0;		// Not moving.
		}
	public void walkToTile(Tile dest, int speed, int delay) {
		walkToTile(dest, speed, delay, 3);
	}
	public void switchedChunks(MapChunk oldchunk, MapChunk newchunk) {
	}
	protected void movef(MapChunk oldChunk, MapChunk newChunk, int newSx, int newSy,
					int newFrame, int newLift) {
		if (oldChunk != null)
			oldChunk.remove(this);
		setShapePos(newSx, newSy);
		if (newFrame >= 0)
			setFrame(newFrame);
		if (newLift >= 0)
			setLift(newLift);
		newChunk.add(this);
	}
	/**
	 *	See if it's blocked when trying to move to a new tile.
	 *	@param t Tile to step to. Tz is possibly updated by this function.
	 *	@param f Pointer to tile we are stepping from, or null for current tile.
	 *	@param move_flags Additional movement flags to consider for step.
	 *	@return Returns 1 if so, else 0.
	 */
	public boolean areaAvailable
		(
		Tile t,			// Tz possibly updated.
		Tile f,			// Step from here, or curpos if null.
		int move_flags
		) {
		ShapeInfo info = getInfo();
						// Get dim. in tiles.
		int frame = getFrameNum();
		int xtiles = info.get3dXtiles(frame), ytiles = info.get3dYtiles(frame);
		int ztiles = info.get3dHeight();
		t.fixme();
		if (xtiles == 1 && ytiles == 1) {	// Simple case?
			MapChunk nlist = gmap.getChunk(
				t.tx/EConst.c_tiles_per_chunk, t.ty/EConst.c_tiles_per_chunk);
			int new_lift = nlist.spotAvailable(ztiles, 
					t.tx%EConst.c_tiles_per_chunk, t.ty%EConst.c_tiles_per_chunk, t.tz,
					move_flags | getTypeFlags(), 1, -1);
			t.tz = (short)new_lift;
			return new_lift >= 0;
			}
		if (f == null) {
			f = new Tile();
			getTile(f);
		}
		return MapChunk.areaAvailable(xtiles, ytiles, ztiles,
				f, t, move_flags | getTypeFlags(), 1, -1);
	}
	/*
	 *	Read in actor from a given file.
	 */
	public void read
		(
		InputStream nfile,		// 'npc.dat', generally.
		int num,			// NPC #, or -1.
		boolean has_usecode		// 1 if a 'type1' NPC.
		) throws IOException {
		npcNum = (short) num;

		// This is used to get around parts of the files that we don't know
		// what the uses are. 'fix_first' is used fix issues in the originals
		// files that cause problems for the extra info we save.
		boolean fix_first = true; // +++++++ Game::is_new_game();
							
		init();				// Clear rest of stuff.
		int locx = nfile.read()&0xff;	// Get chunk/tile coords.
		int locy = nfile.read()&0xff;
							// Read & set shape #, frame #.
		int shnum = EUtil.Read2(nfile)&0xffff;
		/* +++++
		if (num == 0 && Game::get_game_type() != BLACK_GATE && 
								(shnum & 0x3ff) < 12)
			setShape((shnum & 0x3ff) | 0x400);
		else */
			setShape(shnum & 0x3ff);

		setFrame(shnum >> 10);
			
		int iflag1 = EUtil.Read2(nfile);	// Inventory flag.
							// We're going to use these bits.
							// iflag1:0 == has_contents.
							// iflag1:1 == sched. usecode follows,
							//   possibly empty.
							// iflag1:2 == usecode # assigned by
							//   ES, so always use it.
							// iflag1:3 == usecode fun name assigned
							//   by ES, so use it instead.
							// iflag1:4 == Extended skin number
		boolean read_sched_usecode = !fix_first && (iflag1&2) != 0;
		boolean usecode_name_used = !fix_first && (iflag1&8) != 0;
		if (usecode_name_used || (!fix_first && (iflag1&4) != 0))
			usecodeAssigned = true;
		boolean extended_skin = !fix_first && (iflag1&16) != 0;

		int schunk = nfile.read();	// Superchunk #.
							// For multi-map:
		int map_num = nfile.read();
		if (fix_first)
			map_num = 0;
		GameMap npcmap = gwin.getMap(map_num);
		int usefun = EUtil.Read2(nfile);	// Get usecode function #.
		setLift(usefun >> 12);		// Lift is high 4 bits.
		usecode = usefun & 0xfff;
							// Need this for BG. (Not sure if SI.)
		if (npcNum >= 0 && npcNum < 256)
			usecode = 0x400 + npcNum;
							// Watch for new NPC's added.
		else if (usecode_name_used || (!has_usecode && !usecodeAssigned &&
			          usecode != 0x400 + npcNum) || usecode == 0xfff)
			usecode = -1;		// Let's try this.
							// Guessing:  !!  (Want to get signed.)
		int health_val = nfile.read();
		setProperty(Actor.health, health_val);
		nfile.skip(3);	// Skip 3 bytes.
		int iflag2 = EUtil.Read2(nfile);	// The 'used-in-game' flag.
		if (iflag2 == 0 && num >= 0 /* ++++ && !fix_unused */) {
			if (num == 0)		// Old (bad) savegame?
				/* +++++ fix_unused = true; */;
			else
				unused = true;
		}
		boolean has_contents = fix_first ? (iflag1 != 0 && !unused) : (iflag1&1) != 0;
		// Read first set of flags
		int rflags = EUtil.Read2(nfile);
			
		if (((rflags >> 0x7) & 1) != 0) setFlag (GameObject.asleep);
		if (((rflags >> 0x8) & 1) != 0) setFlag (GameObject.charmed);
		if (((rflags >> 0x9) & 1) != 0) setFlag (GameObject.cursed);
		if (((rflags >> 0xB) & 1) != 0) setFlag (GameObject.in_party);
		if (((rflags >> 0xC) & 1) != 0) setFlag (GameObject.paralyzed);
		if (((rflags >> 0xD) & 1) != 0) setFlag (GameObject.poisoned);
		if (((rflags >> 0xE) & 1) != 0) setFlag (GameObject.protection);
		if (!fix_first && ((rflags >> 0xF) & 1) != 0) 
			setFlag (GameObject.dead);

		// Guess
		if (((rflags >> 0xA) & 1) != 0)
			setFlag (GameObject.on_moving_barge);
		alignment = (short)((rflags >> 3)&3);

		// Unknown, using for is_temporary (only when not fix_first)
		if (((rflags >> 0x6) & 1) != 0 && !fix_first) setFlag (GameObject.is_temporary);

		/*	Not used by exult

		Unknown in U7tech
		if (((rflags >> 0x5) & 1) setFlag (GameObject.unknown);
		*/
			
						// Get char. atts.

		// In BG - Strength (0-5), skin colour(6-7)
		// In SI - Strength (0-4), skin colour(5-6), freeze (7)
		int strength_val = nfile.read();

		if (true /* +++++ Game::get_game_type() == BLACK_GATE */) {
			setProperty(Actor.strength, strength_val & 0x3F);

			if (num == 0) {
				if (!extended_skin) {	// We will do it later for extended skins.
					/*+++++++++++
					if (Game::get_avskin() >= 0)
						setSkinColor (Game::get_avskin());
					else */
						setSkinColor (((strength_val >> 6)-1) & 0x3);
				}
			} else 
					setSkinColor (-1);
		} else {
			setProperty(Actor.strength, strength_val & 0x1F);	
			if (num == 0) {
				if (!extended_skin) {	// We will do it later for extended skins.
				/* ++++++FINISH
					if (Game::get_avskin() >= 0 && Game::get_avskin() <= 2)
						set_skin_color (Game::get_avskin());
					else
						set_skin_color ((strength_val >> 5) & 0x3);
				*/
				}
			} else 
				setSkinColor (-1);
			if (((strength_val << 7) & 1) != 0) 
				setFlag (GameObject.freeze);
		}
		if (isDying() &&		// Now we know health, strength.
		    npcNum > 0)		// DON'T do this for Avatar!
			setFlag(GameObject.dead);	// Fixes older savegames.
		// Dexterity
		setProperty(Actor.dexterity, nfile.read());
		// Intelligence (0-4), read(5), Tournament (6), polymorph (7)
		int intel_val = nfile.read();

		setProperty(Actor.intelligence, intel_val & 0x1F);
		if (((intel_val >> 5) & 1) != 0) 
			setFlag (GameObject.read);
							// Tournament.
		if (((intel_val >> 6) & 1) != 0)
			setFlag (GameObject.tournament);
		if (((intel_val >> 7) & 1) != 0) 
			setFlag (GameObject.polymorph);

			// Combat skill (0-6), Petra (7)
		int combat_val = nfile.read();

		setProperty(Actor.combat, combat_val & 0x7F);
		if (((combat_val << 7) & 1) != 0) 
			setFlag (GameObject.petra);
		scheduleType = (byte) nfile.read();
		int amode = nfile.read();	// Default attack mode
							// Just stealing 2 spare bits:
		combatProtected = (amode&(1<<4)) != 0;
		userSetAttack = (amode&(1<<5)) != 0;
		attackMode = (amode&0xf);

		nfile.skip(1); 		// Unknown
		int unk0 = nfile.read();	// We set high bit of this value.
		int unk1 = nfile.read();
		int magic = 0, mana= 0, temp, flags3, ident = 0;
		if (fix_first || unk0 == 0) {	// How U7 stored things.
			// If NPC 0: MaxMagic (0-4), TempHigh (5-7) and Mana(0-4), 
			//						TempLow (5-7)
			// Else: ID# (0-4), TempHigh (5-7) and Met (0), 
			//	No Spell Casting (1), Zombie (2), TempLow (5-7)
			int magic_val = nfile.read();
			int mana_val = nfile.read();
			temp = ((magic_val >> 2) & 0x38) + ((mana_val >> 5) & 7);
			if (num == 0) {
				magic = magic_val&0x1f;
				mana = mana_val&0x1f;
				flags3 = 1;		// Met.
			} else {
				ident = magic_val&0x1f;
				flags3 = mana_val;
			}
		} else {			// Exult stores magic for all NPC's.
			magic = unk0 & 0x7f;
			mana = unk1;
			temp = nfile.read();
			flags3 = nfile.read();
			ident = flags3 >> 3;
			flags3 &= 0x7;
		}
		setProperty(Actor.magic, magic);
		// Need to make sure that mana is less than magic
		setProperty(Actor.mana, mana<magic ? mana : magic);
		setTemperature (temp);
		set_ident(ident);
		if (((flags3 >> 0) & 1) != 0) 
			setFlag (GameObject.met);
		if (((flags3 >> 1) & 1) != 0) 
			setFlag(GameObject.no_spell_casting);
		if (((flags3 >> 2) & 1) != 0)
			setFlag (GameObject.si_zombie);

		faceNum = (short)EUtil.Read2(nfile);	// NOTE:  Exult's using 2 bytes,
		if (fix_first)	// Not used in the original.
			//faceNum &= 0xff;	// Just 1 byte in orig.
			faceNum = npcNum;
		else if (faceNum == 0 && npcNum > 0)	// Fix older savegames.
			faceNum = npcNum;
		nfile.skip(1);	// Unknown

		setProperty(Actor.exp, EUtil.Read4(nfile));
		setProperty(Actor.training, nfile.read());

		nfile.skip (2);	// Primary Attacker
		nfile.skip (2);	// Secondary Attacker
		oppressor = (short)EUtil.Read2(nfile);	// Oppressor NPC id.
		nfile.skip (4);	//I-Vr ??? (refer to U7tech.txt)
		schedule_loc_tx = EUtil.Read2(nfile);	//S-Vr Where npc is supposed to 
		schedule_loc_ty = EUtil.Read2(nfile);	//be for schedule)
		// Type flags 2
		int tflags = EUtil.Read2(nfile);
		// First time round, all the flags are garbage
		if (fix_first) {
			setTypeFlags (1 << tf_walk);
				// Correct for SI, no problems for BG:
			if ((tflags & (1 << tf_sex)) != 0)
				clear_type_flag (tf_sex);
			else
				set_type_flag (tf_sex);
		} else
			setTypeFlags (tflags);
		/* ++++++++FINISH
		if (num == 0 && Game::get_avsex() == 0) {
			clear_type_flag (Actor.tf_sex);
		} else if (num == 0 && Game::get_avsex() == 1) {
			set_type_flag (Actor.tf_sex);
		}
		*/
		nfile.skip (5);	// Unknown
		next_schedule = (byte)nfile.read();	// Acty ????? what is this??
		nfile.skip (1);	// SN ????? (refer to U7tech.txt)
		nfile.skip (2);	// V1 ????? (refer to U7tech.txt)
		nfile.skip (2);	// V2 ????? (refer to U7tech.txt)

		// 16 Bit Shape Numbers, allows for shapes > 1023
		shnum = EUtil.Read2(nfile);
		if (!fix_first && shnum != 0) {
				// ++++ Testing
			if (npcNum == 0)
				setActorShape();
			else
				setShape(shnum);		// 16 Bit Shape Number

			shnum = EUtil.Read2(nfile);	// 16 Bit Polymorph Shape Number
			if (getFlag (GameObject.polymorph)) {
						// Try to fix messed-up flag.
				if (shnum != getShapeNum())
					; /* +++++ finish set_polymorph(shnum); */
				else
					clearFlag(GameObject.polymorph);
			}
		} else {
			nfile.skip (2);
			// +++++++++ set_polymorph_default();
		}

		// More Exult stuff
		if (!fix_first) {
			int	f;
			// Flags
			f = EUtil.Read4(nfile);
			flags |= f;
			/* ++++++++
			if (getFlag(GameObject.invisible))	// Force timer.	
				need_timers().start_invisibility();
			*/
			// SIFlags -- no longer used.
			nfile.skip (2);
			// Flags2	But don't set polymorph.
			boolean polym = getFlag(GameObject.polymorph)!= false;
			f = EUtil.Read4(nfile);
			flags2 |= f;
			if (!polym && getFlag(GameObject.polymorph))
				clearFlag(GameObject.polymorph);
			/*
			if (usecode_name_used) {	// Support for named functions.
				int funsize = nfile.read();
				char *nm = new char[funsize+1];
				nfile.read(nm, funsize);
				nm[funsize] = 0;
				usecode_name = nm;
				usecode = ucmachine.find_function(nm);
				delete [] nm;
			}
			*/
			int skin = nfile.read();
			/*++++++++++++++++
			if (extended_skin) {
				if (Game::get_avskin() >= 0)
					set_skin_color (Game::get_avskin());
				else
					set_skin_color(skin);
			}
			*/
		} else {
			// Flags
			nfile.skip (4);
			// SIFlags
			nfile.skip (2);
			// Flags2 
			nfile.skip (4);
			// Extended skins
			nfile.skip (1);
		}
		// Skip 14
		nfile.skip (14);
						// Get (signed) food level.
		int food_read = nfile.read();
		if (fix_first) food_read = 18;
		setProperty(Actor.food_level, food_read);
		// Skip 7
		nfile.skip(7);
		byte namebuf[] = new byte[16];
		nfile.read(namebuf);
		//+++++++++++++++
		int len;
		for (len = 0; len < 16; len++)
			if (namebuf[len] == 0) 
				break;
			/*	Used to also require namebuf[i] < 127 (jsf).	*/
			else if (namebuf[len] < ' ') {
				break;
			}
//			cout << "Actor " << namebuf << " has alignment " << alignment << endl;
		/*+++++++++FINISH
		if (num == 0 && Game::get_avname()) {
			name = Game::get_avname();
		} else
		*/
			name = new String(namebuf, 0, len);		// Store copy of it.

							// Get abs. chunk. coords. of schunk.
		int scy = 16*(schunk/12);
		int scx = 16*(schunk%12);
		if (has_contents)		// Inventory?  Read.
			npcmap.readIregObjects(nfile, scx, scy, this, 0);
		/* +++++++FINISH
		if (read_sched_usecode)		// Read in scheduled usecode.
			npcmap.read_special_ireg(nfile, this);
		*/
		int cx = locx >> 4;		// Get chunk indices within schunk.
		int cy = locy >> 4;
							// Get tile #'s.
		int tilex = locx & 0xf;
		int tiley = locy & 0xf;
		setShapePos(tilex, tiley);
		MapChunk olist = npcmap.getChunk(scx + cx, scy + cy);
		setInvalid();			// Not in world yet.
		if (olist != null && !isDead() &&	// Valid & alive?  Put into chunk list.
		    !unused) {
			move((scx + cx)*EConst.c_tiles_per_chunk + tilex,
			     (scy + cy)*EConst.c_tiles_per_chunk + tiley, 
					getLift(), map_num);
			if (this == gwin.getMainActor())
				gwin.setMap(map_num);
		}
		/* +++++++++++FINISH
		// We do this here because we need the NPC's final shape.
		if (health_val <= 0 && !unused) {
			// If a monster can't die, force it to have at least 1 hit point,
			// but only if the monster is used.
			// Maybe we should restore it to full health?
			Monster_info *minf = get_info().get_monster_info();
			if (minf && minf.cant_die())
				setProperty(Actor.static_cast<int>(Actor.health),
					get_property(static_cast<int>(Actor.strength)));
		}

		// Only do ready best weapon if we are in BG, this is the first time
		// and we are the Avatar or Iolo
		if (Game::get_game_type() == BLACK_GATE && Game::get_avname() && (num == 0 || num == 1))
			ready_best_weapon();
		*/				
	}
	/*
	 * For TimeSensitive
	 */
	public boolean alwaysHandle() {	
		return false;
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
	/*
	 * Sequence of frames, with 0 being the resting frame.
	 */
	public static class FramesSequence {
		private byte frames[];
		public FramesSequence(byte f[]) {
			frames = f;
		}
		public final byte getResting() {
			return frames[0];
		}
		public int nextIndex(int index) {
			if (++index >= frames.length)
				index = 1;
			return index;
		}
		public int prevIndex(int index) {
			if (--index <= 0)
				index = frames.length - 1;
			return index;
		}
		public final int get(int i) {
			return frames[i];
		}
		// Find frame, masking off rotation, or 0 if not found.
		public int findUnrotated(int frame) {
			for (int i = frames.length - 1; i > 0; i--)
				if (((frame ^ frames[i])&0xf) == 0)
					return i;
			return 0;
		}
	}
}

