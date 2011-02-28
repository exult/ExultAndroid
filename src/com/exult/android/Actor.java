package com.exult.android;
import com.exult.android.shapeinf.*;
import java.util.Vector;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.graphics.Point;

public abstract class Actor extends ContainerGameObject implements TimeSensitive {
	protected String name;			// Its name.
	protected int usecode;			// # of usecode function.
	protected boolean usecodeAssigned;		// Usecode # explicitly assigned.
	protected String usecodeName;
	protected boolean unused;			// If npcNum > 0, this NPC is unused in the game.
	protected short npcNum;			// # in Game_window.npcs list, or -1.
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
	protected Tile targetTile;
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
		;	//	Actor frame to substitute when a frame is empty (as some are):
	public static final int visibleFrames[] = {
			Actor.standing,		// Standing.
			Actor.standing,		// Steps.
			Actor.standing,
			Actor.standing,		// Ready.
			Actor.raise2_frame,		// 1-handed strikes => 2-handed.
			Actor.reach2_frame,
			Actor.strike2_frame,
			Actor.raise1_frame,		// 2-handed => 1-handed.
			Actor.reach1_frame,
			Actor.strike1_frame,
			Actor.standing,		// When you can't sit...
			Actor.kneel_frame,		// When you can't bow.
			Actor.bow_frame,		// When you can't kneel.
			Actor.standing,		// Can't lie.
			Actor.strike2_frame,		// Can't raise hands.
			Actor.ready_frame };	// Can't strech arms outward.

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
	private static final byte sea_serpent_attack_frames[] = 
			{13, 12, 11, 0, 1, 2, 3, 11, 12, 13, 14};
	private static final byte reach_attack_frames1[] = {3, 6};
	private static final byte raise_attack_frames1[] = {3, 4, 6};
	private static final byte fast_swing_attack_frames1[] = {3, 5, 6};
	private static final byte slow_swing_attack_frames1[] = {3, 4, 5, 6};
	private static final byte reach_attack_frames2[] = {3, 9};
	private static final byte raise_attack_frames2[] = {3, 7, 9};
	private static final byte fast_swing_attack_frames2[] = {3, 8, 9};
	private static final byte slow_swing_attack_frames2[] = {3, 7, 8, 9};
	protected FramesSequence frames[];
	protected byte scheduleType;	// Schedule type (scheduleType).	
	// Location (x,y) of Shedule
	protected Tile scheduleLoc;
	protected byte nextSchedule;	// Used so correct schedule type 
									//   will be saved
	protected Schedule schedule;		// Current schedule.
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
	protected int lightSources;	// # of light sources readied.
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

	NpcTimers timers;					// Timers for poison, hunger, etc.
	protected Rectangle weaponRect;		// Screen area weapon was drawn in.
	protected static Rectangle tempRect = new Rectangle();
	protected Point weaponPoint;
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
		targetTile = null;
		attackWeapon = -1;
		attackMode = nearest;
		scheduleType = Schedule.loiter;
		dormant = true;
		skinColor = -1;
		weaponRect = new Rectangle(0,0,0,0);
		weaponPoint = new Point();
	}
	/*
	 *	Initialize frames, properties and spots.
	 */
	private void initDefaultFrames() {
						// Set up actor's frame lists.
						// Most NPC's walk with a 'stand'
						//   frame between steps.
		//UNUSED final int FRAME_NUM = 5;
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
	/**
	 *	Goes through the actor's readied gear and caches powers
	 *	and immunities.
	 */
	public void refigureGear() {
		final int locs[] = {Ready.head, Ready.belt, Ready.lhand,
				Ready.lfinger, Ready.legs,
				Ready.feet, Ready.rfinger, Ready.rhand, Ready.torso, 
				Ready.amulet, Ready.earrings, Ready.cloak, Ready.gloves
			};
		int powers = 0, immune = 0;
		lightSources = 0;
		for (int i = 0; i < locs.length; i++) {
			GameObject worn = spots[locs[i]];
			if (worn != null) {
				ShapeInfo info = worn.getInfo();
				int rdy = info.getReadyType();
				if (info.isLightSource() && (locs[i] != Ready.belt ||
						(rdy != Ready.lhand && rdy != Ready.rhand && rdy != Ready.both_hands)))
					lightSources++;
				powers |= info.getObjectFlags(worn.getFrameNum(),
					info.hasQuality() ? worn.getQuality() : -1);
				immune |= info.getArmorImmunity();
			}
		}
		gearImmunities = (byte)immune;
		gearPowers = (byte)powers;
	}
	/*
	 *	Decrement food level and print complaints if it gets too low.
	 *	NOTE:  Should be called every hour.
	 */
	void useFood() {
		if (getInfo().doesNotEat() || (gearPowers&FrameFlagsInfo.doesnt_eat) != 0)
			return;
		int food = getProperty(food_level);
		food -= (EUtil.rand()%4);		// Average 1.5 level/hour.
		setProperty(food_level, food);
		if (food <= 0) {		// Really low?
			if (EUtil.rand()%4 != 0)
				say(ItemNames.first_starving, ItemNames.first_starving + 2);
						// Set timer for damage.
			needTimers().startHunger();
		} else if (food <= 4) {
			if (EUtil.rand()%3 != 0)
				say(ItemNames.first_needfood, ItemNames.first_needfood + 2);
		} else if (food <= 8) {
			if (EUtil.rand()%2 != 0)
				say(ItemNames.first_hunger, ItemNames.first_hunger + 2);
		}
	}
	//	Don't wake Skara Brae ghost or Penumbra in Black Gate
	public boolean bgDontWake() {
		int num;
		return (game.isBG() &&
				(getInfo().hasTranslucency() ||
							// Horace or Penumbra?
				 (num = getNpcNum()) == 141 || num == 150));
	}
	public final int getProperty(int prop) {
		
		if (prop == Actor.sex_flag)
			// Correct in BG and SI, but the flag is never normally set
			// for anyone but avatar in BG.
			return getTypeFlag(Actor.tf_sex) ? 1 : 0;
		else if (prop == Actor.missile_weapon) {
			// Seems to give the same results as in the originals.
			GameObject weapon = getReadied(Ready.lhand);
			WeaponInfo winf = weapon != null ? weapon.getInfo().getWeaponInfo() : null;
			if (winf == null)
				return 0;
			return (winf.getUses() >= 2 ? 1 : 0);
		}
		return (prop >= 0 && prop < Actor.sex_flag) ? properties[prop] : 0;
	}
	public final void setProperty(int prop, int val) {
		if (prop == health && ((partyId != -1) || (npcNum == 0)) && 
				cheat.inGodMode() && val < properties[prop])
			return;
		//System.out.println("Npc# " + npcNum + ", set prop " + prop + " to " + val);
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
			if (val != 0)
				setTypeFlag(tf_sex);
			else
				clearTypeFlag(tf_sex);
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
	public void forceSleep() {
		flags |= (1 << GameObject.asleep);
		needTimers().startSleep();
		setAction(null);		// Stop what you're doing.
		layDown(false);	// Lie down.
	}
	public final NpcTimers needTimers() {
		if (timers == null)
			timers = new NpcTimers(this);
		return timers;
	}
	//	Need to clear flag without going recursive.
	public void clearSleep() {
		 flags &= ~(1 << GameObject.asleep);
	}
	public void setFlag(int flag) {	
		MonsterInfo minf = getInfo().getMonsterInfo();
		if (minf == null)
			minf = MonsterInfo.getDefault();
		switch (flag) {
		case GameObject.asleep:
			if (minf.sleepSafe() || minf.powerSafe() ||
					(gearPowers&
					 (FrameFlagsInfo.power_safe|FrameFlagsInfo.sleep_safe)) != 0)
				return;		// Don't do anything.
						// Avoid waking Penumbra.
			if (scheduleType == Schedule.sleep && bgDontWake())
				break;
				// Set timer to wake in a few secs.
			needTimers().startSleep();
			setAction(null);		// Stop what you're doing.
			layDown(false);	// Lie down.
			break;
		case GameObject.poisoned:
			if (minf.poisonSafe() || (gearPowers&FrameFlagsInfo.poison_safe) != 0)
					return;		// Don't do anything.
			needTimers().startPoison();
			break;
		case GameObject.protection:
			needTimers().startProtection();
			break;
		case GameObject.might:
			needTimers().startMight();
			break;
		case GameObject.cursed:
			if (minf.curseSafe() || minf.powerSafe() ||
				(gearPowers&
					(FrameFlagsInfo.power_safe|FrameFlagsInfo.curse_safe)) != 0)
				return;		// Don't do anything.
			needTimers().startCurse();
			break;
		case GameObject.charmed:
			if (minf.charmSafe() || minf.powerSafe() ||
					(gearPowers&
					(FrameFlagsInfo.power_safe|FrameFlagsInfo.charm_safe)) != 0)
				return;		// Don't do anything.
			needTimers().startCharm();
			setTarget(null);		// Need new opponent if in combat.
			break;
		case GameObject.paralyzed:
			if (minf.paralysisSafe() || minf.powerSafe() ||
					(gearPowers&
					(FrameFlagsInfo.power_safe|FrameFlagsInfo.paralysis_safe)) != 0)
				return;		// Don't do anything.
			fallDown();
			needTimers().startParalyze();
			break;
		case GameObject.invisible:
			flags |= (1 << flag);
			needTimers().startInvisibility();
			CombatSchedule.stopAttackingInvisible(this);
			clock.setPalette();
			break;
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
			if (get_npcNum() != 0)	// Ignore for all but avatar.
				break;
			int sn;
			int female = getTypeFlag(tf_sex)?1:0;
			Skin_data *skin = Shapeinfo_lookup.GetSkinInfoSafe(this);
	
			if (!skin ||	// Should never happen, but hey...
				(!sman.have_si_shapes() &&
					Shapeinfo_lookup.IsSkinImported(skin.naked_shape)))
				sn = Shapeinfo_lookup.GetBaseAvInfo(female != 0).shape_num;
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
	public void clearFlag(int flag) {
		if (flag >= 0 && flag < 32)
			flags &= ~(1 << flag);
		else if (flag >= 32 && flag < 64)
			flags2 &= ~(1 << (flag-32));
		if (flag == GameObject.invisible)	// Restore normal palette.
			clock.setPalette();
		else if (flag == GameObject.asleep) {
			if (scheduleType == Schedule.sleep)
				setScheduleType(Schedule.stand);
			else if ((getFrameNum()&0xf) == Actor.sleep_frame) {
				Tile pos;		// Find spot to stand.
				getTile(pos = new Tile());
				pos.tz -= pos.tz%5;	// Want floor level.
				if (MapChunk.findSpot(pos, 6, getShapeNum(), Actor.standing, 0))
					move(pos);
				changeFrame(Actor.standing);
			}
			UsecodeScript.terminate(this);
		} else if (flag == GameObject.charmed)
			setTarget(null); 			// Need new opponent.
		else if (flag == GameObject.bg_dont_move || flag == GameObject.dont_move)
			// Start again after a little while
			start(1, 1);
		else if (flag == GameObject.polymorph && getFlag(GameObject.naked))
			clearFlag(GameObject.naked);
		else if (flag == GameObject.naked && getFlag(GameObject.polymorph))
			clearFlag(GameObject.polymorph);
		
		setActorShape();
	}
	public final void setTypeFlag(int flag) {
		if (flag >= 0 && flag < 16)
			typeFlags |= (1 << flag);
		setActorShape();
	}
	public void clearTypeFlag(int flag) {
		if (flag >= 0 && flag < 16)
			typeFlags &= ~(1 << flag);
		setActorShape();
	}
	public boolean getTypeFlag(int flag) {
		return (flag >= 0 && flag < 16) ? (typeFlags & (1 << flag)) != 0 : false;
	}
	public void setTypeFlags(int tflags) {
		typeFlags = tflags;
		setActorShape();
	}
	public int getTypeFlags() {
		return typeFlags;
	}	
	/*
	 *	A class whose whole purpose is to stop casting mode.
	 */
	private static class ClearCasting extends TimeSensitive.Timer {
		public void handleEvent(int curtime, Object udata) {
			Actor a = (Actor)(udata);
			a.hideCastingFrames();
			a.addDirty();
		}
	}
	/*
	 *	A class whose whole purpose is to clear the 'hit' flag.
	 */
	private static class ClearHit extends TimeSensitive.Timer {
		public void handleEvent(int curtime, Object udata) {
			Actor a = (Actor)(udata);
			a.hit = false;
			a.addDirty();
		}
	}
	public final void hideCastingFrames() {
		castingMode = not_casting;
	}
	public final short getCastingMode ()  { return castingMode; }
	public final void endCastingMode (int delay) {
		ClearCasting c = new ClearCasting();
		tqueue.add(TimeQueue.ticks + 2, c, this);
	}
	public final int getCasting_shape () { return castingShape; }
	public final void beginCasting (int s)
		{ castingMode = init_casting; castingShape = s; }
	public final void displayCastingFrames () 
		{ castingMode = show_casting_frames; }
	public final void hideCasting_frames () 
		{ castingMode = not_casting; }
	public void setSkinColor (int color) { 
		skinColor = color; 
		setActorShape();
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
	public final void setDormant() {
		dormant = true;
	}
	public void setAttribute(String nm, int val) {
		//++++++++++LATER
	}
	public int getAttribute(String nm) {
		return 0;	// +++++++LATER
	}
	public void setSchedules(Schedule.ScheduleChange scheds[]) {
		// Overloaded.
	}
	public Schedule.ScheduleChange[] getSchedules() {
		return null;
	} 
	public void updateSchedule(int hour3, int backwards, int delay) {
	}
	//	Set schedule after reading in.
	public final void restoreSchedule() {
						// Activate schedule if not in party.
		if (!isPosInvalid() && partyId < 0) {
			if (nextSchedule != 255 && 
					scheduleType == Schedule.walk_to_schedule)
				setScheduleAndLoc(nextSchedule, scheduleLoc, -1);
			else
				setScheduleType(scheduleType);
			}
	}
	public final Schedule getSchedule() {
		return schedule;
	}
	public final int getScheduleType() {
		return scheduleType;
	}
	public final void setScheduleType(int newScheduleType, Schedule newSched) {
		stop();
		if (schedule != null)
			schedule.ending(newScheduleType);
		setAction(null);
		int oldSchedule = scheduleType;
		schedule = newSched;
		if (schedule == null) {
			switch (newScheduleType) {
			case Schedule.combat:
				schedule = new CombatSchedule(this, oldSchedule);
				break;
			case Schedule.horiz_pace:
				readyBestWeapon();
				schedule = Schedule.Pace.createHoriz(this);
				break;
			case Schedule.vert_pace:
				readyBestWeapon();
				schedule = Schedule.Pace.createVert(this);
				break;
			case Schedule.talk:
				schedule = new Schedule.Talk(this);
				break;
			/*++++++
			case Schedule.dance:
				unready_weapon();
				schedule = new Dance_schedule(this);
				break;
			case Schedule.farm:	// Use a scythe.
				schedule = new Tool_schedule(this, 618);
				break;
			case Schedule.tend_shop:// For now.
				unready_weapon();
				schedule = new Loiter_schedule(this, 3);
				break;
			case Schedule.miner:	// Use a pick.
				schedule = new Miner_schedule(this);
				break;
			case Schedule.hound:
				readyBestWeapon();
				schedule = new Hound_schedule(this);
				break;
			*/
			case Schedule.loiter:
			case Schedule.graze:	// For now.
				schedule = new Schedule.Loiter(this);
				break;
			case Schedule.wander:
				schedule = new Schedule.Wander(this);
				break;
			/*++++++++++++++
			case Schedule.blacksmith:
				schedule = new Forge_schedule(this);
				break;
			*/
			case Schedule.sleep:
				unreadyWeapon();
				schedule = new Schedule.Sleep(this);
				break;
			case Schedule.wait:
				schedule = new Schedule.Wait(this);
				break;
			case Schedule.eat:		// For now.
			case Schedule.sit:
				//++++FINISH unready_weapon();
				schedule = new Schedule.Sit(this, null);
				break;	
			/*++++++++++++++++
			case Schedule.bake:
				schedule = new Bake_schedule(this);
				break;
			case Schedule.sew:
				schedule = new Sew_schedule(this);
				break;
			
			case Schedule.shy:
				//+++++++FINISH unready_weapon();
				schedule = new Shy_schedule(this);
				break;
			*/
			/*++++++++++++++++
			case Schedule.lab:
				schedule = new Lab_schedule(this);
				break;
			case Schedule.thief:
				unready_weapon();
				schedule = new Thief_schedule(this);
				break;
			*/
			case Schedule.waiter:
				unreadyWeapon();
				schedule = new Schedule.Waiter(this);
				break;
			/*+++++++++++
			case Schedule.kid_games:
				unready_weapon();
				schedule = new Kid_games_schedule(this);
				break;
			*/
			case Schedule.eat_at_inn:
				unreadyWeapon();
				schedule = new Schedule.EatAtInn(this);
				break;
			case Schedule.duel:
				schedule = new CombatSchedule.Duel(this);
				break;
			case Schedule.preach:
				readyBestWeapon();	// Fellowship staff.
				schedule = new Schedule.Preach(this);
				break;
			case Schedule.patrol:
				readyBestWeapon();
				schedule = new Schedule.Patrol(this);
				break;
			/*
			case Schedule.desk_work:
				unreadyWeapon();
				schedule = new Desk_schedule(this);
				break;
			*/
			case Schedule.follow_avatar:
				schedule = new Schedule.FollowAvatar(this);
				break;
			case Schedule.walk_to_schedule:
				System.out.println(
					"Attempted to set a \"walk to schedule\" activity for NPC " +
								getNpcNum());
				break;
			default:
				/*+++++++++FINISH
				if (newScheduleType >= 
						Schedule.first_scripted_schedule)
					schedule = new Scripted_schedule(this,
								newScheduleType);
				*/
				schedule = new Schedule.Loiter(this);	//+++++++FOR NOW.
				break;
			}
		}
		scheduleType = (byte)newScheduleType;
		// Reset next schedule.
		if (scheduleLoc != null)
			scheduleLoc.set(0, 0, 0);
		nextSchedule = (byte)255;
		int cx = getCx(), cy = getCy();
		if (!gmap.isChunkRead(cx, cy)) {
			
			dormant = true;		// Chunk hasn't been read in yet.
		} else if (schedule != null) {		// Try to start it.
			dormant = false;
			System.out.printf("setScheduleType: Npc #%1$d is active\n", npcNum);
			schedule.nowWhat();
		}
	}
	public final void setScheduleType(int newScheduleType) {
		setScheduleType(newScheduleType, null);
	}
	/*
	 *	Set new schedule by type AND location.
	 */
	protected void setScheduleAndLoc (int newScheduleType, Tile dest,
					int delay) {	// -1 for random delay.
		stop();				// Stop moving.
		if (schedule != null)			// End prev.
			schedule.ending(newScheduleType);
		int cx = getCx(), cy = getCy();
		//System.out.printf("Setting NPC #%1$d schedule to %2$d at %3$s\n", 
		//		npcNum, newScheduleType, dest);
		int mapnum = getMapNum();
		if (mapnum < 0) mapnum = gmap.getNum();
		if ((mapnum != gmap.getNum()) ||
		    (!gmap.isChunkRead(cx, cy) &&
		     !gmap.isChunkRead(dest.tx/EConst.c_tiles_per_chunk,
								dest.ty/EConst.c_tiles_per_chunk))) {	
						// Not on current map, or
						//   src, dest. are off the screen.
			move(dest.tx, dest.ty, dest.tz, mapnum);
			setScheduleType(newScheduleType);
			return;
		}
						// Going to walk there.
		if (scheduleLoc != null)
			scheduleLoc = new Tile();
		scheduleLoc.set(dest);
		nextSchedule = (byte)newScheduleType;
		scheduleType = Schedule.walk_to_schedule;
		schedule = new Schedule.WalkToSchedule(this, dest, nextSchedule, delay);
		dormant = false;
		schedule.nowWhat();
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
	public final boolean isUnused() {
		return unused;
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
	public final int getEffectiveAlignment() {
		if ((flags&(1<<GameObject.charmed)) == 0)
			return alignment;
		else switch(alignment) {
		case neutral:
			return unknown_align;
		case friendly:
			return hostile;
		case hostile:
			return friendly;
		case unknown_align:
			return neutral;
		}
		return neutral;
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
			if (worn != null)
				warmth += worn.getInfo().getObjectWarmth(
											worn.getFrameNum());
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
		if (this == gwin.getMainActor() && forActor.partyId < 0)
			return false;
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
		UsecodeScript scr = null;
		while ((scr = UsecodeScript.findActive(this, scr)) != null)
			// no_halt scripts seem not to prevent movement.
			if (!scr.isNoHalt())
				return true;
		return false;
	}
	/*
	 *	Attack preset target/tile using preset weapon shape.
	 *
	 *	Output: True if attack was realized and hit target (or is
	 *	a missile flying towards target), false otherwise
	 */
	boolean usecodeAttack() {
		return CombatSchedule.attackTarget(
				this, targetObject, targetTile, attackWeapon, false, null);
	}
	/*
	 *	Should be called after actors and usecode are initialized.
	 */
	void initReadied () {
		for (int i = 0; i < spots.length; i++)
			if (spots[i] != null)
				callReadiedUsecode(i, spots[i],
								UsecodeMachine.readied);
	}
	public void setActorShape() { 	// Set shape based on sex, skin color
		//+++++FINISH
	}
	/*
	 *	Get effective maximum range for weapon taking in consideration
	 *	the actor's strength and combat.
	 *
	 *	@return Weapon's effective range.
	 */
	@Override
	public int getEffectiveRange(WeaponInfo winf, int reach) {
		//System.out.println("getEffectiveRange for " + getName() +
		//		", reach = " + reach);
		if (reach < 0) {
			if (winf == null) {
				MonsterInfo minf = getInfo().getMonsterInfo();
				return minf != null ? minf.getReach()
						: MonsterInfo.getDefault().getReach();
			}
			reach = winf.getRange();
		}
		int uses = winf != null ? winf.getUses() : WeaponInfo.melee;
		if (uses == 0 || uses == WeaponInfo.ranged)
			return reach;
		else {
			int eff_range;
			int str = getEffectiveProp(Actor.strength);
			int combat = getEffectiveProp(Actor.combat);
			//System.out.println("getEffectiveRange: str = " + str +
			//		", combat = " + combat);
			if (str < combat)
				eff_range = str;
			else
				eff_range = combat;
			if (uses == WeaponInfo.good_thrown)
				eff_range *= 2;
			if (eff_range < reach)
				eff_range = reach;
			if (eff_range > 31)
				eff_range = 31;
			return eff_range;
		}
	}
	//	Find best ammo of given type.
	public GameObject findBestAmmo(int family, int needed) {
		GameObject best = null;
		int best_strength = -20;
		Vector<GameObject> vec = new Vector<GameObject>(50);		// Get list of all possessions.
		getObjects(vec, EConst.c_any_shapenum, EConst.c_any_qual, EConst.c_any_framenum);
		for (GameObject obj : vec) {
			if (obj.insideLocked() || !inAmmoFamily(obj.getShapeNum(), family))
				continue;
			AmmoInfo ainf = obj.getInfo().getAmmoInfo();
			if (ainf == null)	// E.g., musket ammunition doesn't have it.
				continue;
				// Can't use it.
			if (obj.getQuantity() < needed)
				continue;
				// Calc ammo strength.
			int strength = ainf.getBaseStrength();
				// Favor those with more shots remaining.
			if (obj.getQuantity() < 5*needed)
				strength /= 3;
			else if (obj.getQuantity() < 10*needed)
				strength /= 2;
			if (strength > best_strength) {
				best = obj;
				best_strength = strength;
				}
			}
		return best;
	}
	/*
	 *	Is a given ammo shape in a given family. Was in combat.cc in Exult.
	 */
	public static boolean inAmmoFamily(int shnum, int family) {
		if (shnum == family)
			return true;
		AmmoInfo ainf = ShapeID.getInfo(shnum).getAmmoInfo();
		return (ainf != null && ainf.getFamilyShape() == family);
	}
	@Override
	public GameObject findWeaponAmmo(int weapon, int needed, boolean recursive) {
		if (weapon < 0)
			return null;
		WeaponInfo winf = ShapeID.getInfo(weapon).getWeaponInfo();
		if (winf == null)
			return null;
		int family = winf.getAmmoConsumed();
		if (family >= 0) {
			GameObject aobj = getReadied(Ready.quiver);
			if (aobj != null && inAmmoFamily(aobj.getShapeNum(), family) &&
										aobj.getQuantity() >= needed)
				return aobj;		// Already readied.
			else if (recursive)
				return findBestAmmo(family, needed);
			return null;
		}
		// Search readied weapons first.
		final int wspots[] = {Ready.lhand, Ready.rhand, Ready.back_2h, Ready.belt};
		final int num_weapon_spots = wspots.length;
		for (int i = 0; i < num_weapon_spots; i++) {
			GameObject obj = spots[wspots[i]];
			if (obj == null || obj.getShapeNum() != weapon)
				continue;
			ShapeInfo inf = obj.getInfo();
			if (family == -2) {
				if (!inf.hasQuality() || obj.getQuality() >= needed)
					return obj;
			}
				// Family -1 and family -3.
			else if (obj.getQuantity() >= needed)
				return obj;
			}

		// Now recursively search all contents.
		return recursive ? super.findWeaponAmmo(weapon, 1, false) : null;
	}
	/*
	 *	Swap new ammo with old.
	 */
	public void swapAmmo(GameObject newammo) {
		GameObject aobj = getReadied(Ready.quiver);
		if (aobj == newammo)
			return;			// Already what we need.
		if (aobj != null)			// Something there already?
			aobj.removeThis();	// Remove it.
		newammo.removeThis();
		add(newammo, true);		// Should go to the right place.
		if (aobj != null)			// Put back old ammo.
			add(aobj, true);
	}
	/*
	 *	Recursively searches for ammunition for a given weapon, if needed.
	 *	@return true if the weapon can be used, ammo is pointer to best ammunition.
	 */
	private boolean isWeaponUsable(GameObject bobj, GameObject ammo[], 
					boolean recursive) {
		ammo[0] = null;
		WeaponInfo winf = bobj.getInfo().getWeaponInfo();
		if (winf == null)
			return false;		// Not a weapon.
		// Check ranged first.
		int need_ammo = getWeaponAmmo(bobj.getShapeNum(),
				winf.getAmmoConsumed(), winf.getProjectile(),
				true, ammo, recursive);
		if (need_ammo == 0)
			return true;
			// Try melee if the weapon is not ranged.
		else if (ammo[0] == null && winf.getUses() != WeaponInfo.ranged)
			need_ammo = getWeaponAmmo(bobj.getShapeNum(),
					winf.getAmmoConsumed(), winf.getProjectile(),
					false, ammo, recursive);
		if (need_ammo != 0 && ammo[0] == null)
			return false;
		return true;
	}
	/*
	 *	Ready ammo for weapon being carried.
	 */
	boolean readyAmmo() {
		GameObject weapon = spots[Ready.lhand];
		System.out.println("readyAmmo for npc #" + npcNum +
				", weapon = " + weapon.getShapeNum());
		if (weapon == null)
			return false;
		ShapeInfo info = weapon.getInfo();
		WeaponInfo winf = info.getWeaponInfo();
		if (winf == null)
			return false;
		System.out.println("readyAmmo: ammoConsumed = " + winf.getAmmoConsumed());
		if ((winf.getAmmoConsumed()) < 0) {	// Ammo not needed.
			if (winf.usesCharges() && info.hasQuality() &&
						weapon.getQuality() <= 0)
				return false;	// Uses charges, but none left.
			else
				return true;
		}
		GameObject found[] = new GameObject[1];
			// Try non-recursive search for ammo first.
		boolean usable = isWeaponUsable(weapon, found, false);
		if (usable)	// Ammo is available and ready.
			return true;
		else if (winf.getAmmoConsumed() < 0)
			return false;	// Weapon can't be used.
			// Try recursive search now.
		found[0] = findBestAmmo(winf.getAmmoConsumed(), 1);
		if (found[0] == null)
			return false;
		swapAmmo(found[0]);
		return true;
	}
	/*
	 *	If no shield readied, look through all possessions for the best one.
	 *	@return Returns true if successful.
	 */
	boolean readyBestShield()	{
		if (spots[Ready.rhand] != null) {
			ShapeInfo inf = spots[Ready.rhand].getInfo();
			return inf.getArmor() != 0 || inf.getArmorImmunity() != 0;
		}
		Vector<GameObject> vec = new Vector<GameObject>(50);		// Get list of all possessions.
		getObjects(vec, EConst.c_any_shapenum, EConst.c_any_qual, EConst.c_any_framenum);
		GameObject best = null;
		int best_strength = -20;
		for (GameObject obj : vec) {
			if (obj.insideLocked())
				continue;
			ShapeInfo info = obj.getInfo();
				// Only want those that can be readied in hand.
			int ready = info.getReadyType();
			if (ready != Ready.lhand && ready != Ready.backpack)
				continue;
			ArmorInfo arinf = info.getArmorInfo();
			if (arinf == null)
				continue;	// Not a shield.
			int strength = arinf.getBaseStrength();
			if (strength > best_strength)
				{
				best = obj;
				best_strength = strength;
				}
			}
		if (best == null)
			return false;
		// Spot is free already.
		best.removeThis();
		add(best, true);			// Should go to the right place.
		return true;
	}
	public final boolean checkGearPowers(int f) {
		return (gearPowers&f) != 0;
	}
	/*
	 *	Get weapon value.
	 */
	public GameObject getWeapon() {
		GameObject weapon = spots[Ready.lhand];
		if (weapon != null && weapon.getInfo().getWeaponInfo() != null) {
			return weapon;
		} else
			return null;
	}
	/*
	 *	If no weapon readied, look through all possessions for the best one.
	 */
	public boolean readyBestWeapon() {
		//System.out.println("readyBestWeapon for NPC " + npcNum +
		//		":" + getName());
		if (getWeapon() != null && readyAmmo())
			return true;		// Already have one.
		// Check for spellbook.
		GameObject robj = getReadied(Ready.lhand);
		if (robj != null && robj.getInfo().getShapeClass() == ShapeInfo.spellbook) {
			if (((SpellbookObject)robj).canDoSpell(this))
				return true;
		}
		Vector<GameObject> vec = new Vector<GameObject>(50);		// Get list of all possessions.
		getObjects(vec, EConst.c_any_shapenum, EConst.c_any_qual, EConst.c_any_framenum);
		GameObject best = null, best_ammo = null;
		GameObject ammo_obj[] = new GameObject[1];
		int best_strength = -20;
		int wtype = Ready.backpack;
		for (GameObject obj : vec) {
			if (obj.insideLocked())
				continue;
			ShapeInfo info = obj.getInfo();
			int ready = info.getReadyType();
			//System.out.println("Checking " + obj.getShapeNum() + ", " + 
			//		obj.getName() + ", ready = " + ready);
			// backpack and rhand added for dragon breath and some spells
			if (ready != Ready.lhand && ready != Ready.both_hands &&
					 ready != Ready.rhand && ready != Ready.backpack)
				continue;
			WeaponInfo winf = info.getWeaponInfo();
			//System.out.println("winf = " + (winf == null?"null":"nonnull"));
			if (winf == null)
				continue;	// Not a weapon.
			if (!isWeaponUsable(obj, ammo_obj, true))
				continue;
			
			int strength = winf.getBaseStrength();
			System.out.println("Strength = " + strength);
			strength += getEffectiveRange(winf, -1);
			if (strength > best_strength) {
				wtype = ready;
				best = obj;
				best_ammo = ammo_obj[0] != obj ? ammo_obj[0] : null;
				best_strength = strength;
			}
		}
		if (best == null)
			return false;
			// If nothing is in left hand, nothing will happen.
		GameObject remove1 = spots[Ready.lhand], remove2 = null;
		if (wtype == Ready.both_hands)
			remove2 = spots[Ready.rhand];
			// Prevent double removal and double add (can corrupt objects list).
			// No need for similar check for remove1 as we wouldn't be here
			// if remove1 were a weapon we could use.
		if (remove2 == best)
			remove2 = null;
						// Free the spot(s).
		if (remove1 != null)
			remove1.removeThis();
		if (remove2 != null)
			remove2.removeThis();
		best.removeThis();
		if (wtype == Ready.rhand) // tell it the correct ready spot
			addReadied(best, Ready.lhand);
		else
			add(best, true);			// Should go to the right place.
		if (wtype == Ready.lhand)
			readyBestShield();	// Also add a shield for 1-handed weapons.
		if (remove1 != null)			// Put back other things.
			add(remove1, true);
		if (remove2 != null)
			add(remove2, true);
		if (best_ammo != null)
			swapAmmo(best_ammo);
		return true;
	}
	/*
	 *	Try to store the readied weapon.
	 */
	public void unreadyWeapon() {
		GameObject obj = spots[Ready.lhand];
		if (obj == null)
			return;
		ShapeInfo info = obj.getInfo();
		if (info.getWeaponInfo() == null)	// A weapon?
			return;
		gwin.addDirty(this);
		if (spots[Ready.belt] == null) {	// Belt free?
			obj.removeThis();
			addReadied(obj, Ready.belt);
		}
	}
	public final int getAttackMode() {
		return attackMode;
	}
	public final void setAttackMode(int amode, boolean byUser) {
		attackMode = amode;
		userSetAttack = byUser;
	}
	public final boolean didUserSetAttack() {
		return userSetAttack;
	}
	public final boolean isCombatProtected() {
		return combatProtected;
	}
	public final void setCombatProtected(boolean tf) {
		combatProtected = tf;
	}
	public static boolean rollToWin(int attacker, int defender) {
		final int sides = 30;
		int roll = EUtil.rand()%sides;
		if (roll == 0)			// Always lose.
			return false;
		else if (roll == sides - 1)	// High?  Always win.
			return true;
		else
			return roll + attacker - defender >= sides/2 - 1;
	}
	/*
	 *	Get effective property, or default value.
	 */
	static final int getEffectiveProp
		(
		Actor npc,			// ...or NULL.
		int prop,	// Property #.
		int defval			// Default val if npc==0.
		) {
		return npc != null ? npc.getEffectiveProp(prop) : defval;
	}
	/*
	 *	Figure hit points lost from an attack, and subtract from total.
	 *
	 *	Output:	Hits taken or < 0 for explosion.
	 */
	public int figureHitPoints
		(
		GameObject attacker,
		int weapon_shape,		// < 0 for readied weapon.
		int ammo_shape,			// < 0 for no ammo shape.
		boolean explosion			// If this is an explosion attacking.
		) {
		boolean were_party = partyId != -1 || npcNum == 0;
		// godmode effects:
		if (were_party && cheat.inGodMode())
			return 0;
		Actor npc = attacker != null ? attacker.asActor() : null;
		boolean theyre_party = npc != null &&
				(npc.partyId != -1 || npc.npcNum == 0);
		boolean instant_death = (cheat.inGodMode() && theyre_party);
						// Modify using combat difficulty.
		int bias = were_party ? CombatSchedule.difficulty :
				(theyre_party ? -CombatSchedule.difficulty : 0);
		WeaponInfo winf;
		AmmoInfo ainf;

		int wpoints = 0;
		if (weapon_shape >= 0)
			winf = ShapeID.getInfo(weapon_shape).getWeaponInfo();
		else
			winf = null;
		if (ammo_shape >= 0)
			ainf = ShapeID.getInfo(ammo_shape).getAmmoInfo();
		else
			ainf = null;
		if (winf == null && weapon_shape < 0 && npc != null) {
			GameObject weapon = npc.getWeapon();
			if (weapon != null) {
				winf = weapon.getInfo().getWeaponInfo();
				if (winf != null)
					wpoints = winf.getDamage();
			} else
				wpoints = 1;	// Bare hands.
		}
		int usefun = -1, powers = 0;
		int type = WeaponInfo.normal_damage;
		boolean explodes = false;
		if (winf != null) {
			wpoints = winf.getDamage();
			usefun = winf.getUsecode();
			type = winf.getDamageType();
			powers = winf.getPowers();
			explodes = winf.explodes();
		} else
			wpoints = 1;	// Give at least one, but only if there's no weapon
		if (ainf != null) {
			wpoints += ainf.getDamage();
				// Replace damage type.
			if (ainf.getDamageType() != WeaponInfo.normal_damage)
				type = ainf.getDamageType();
			powers |= ainf.getPowers();
			explodes = explodes || ainf.explodes();
			}

		if (explodes && !explosion) {	// Explosions shouldn't explode again.
				// Time to explode.
			Tile t = new Tile();
			getTile(t);
			t.tz += getInfo().get3dHeight()/2;
			eman.addEffect(new EffectsManager.ExplosionEffect(t,
					null, 0, weapon_shape, ammo_shape, attacker));
				// The explosion will handle the damage.
			return -1;
		}
		int expval = 0, hits = 0;
		boolean nodamage = (powers & (WeaponInfo.no_damage)) != 0;
		if (wpoints != 0 && instant_death)
			wpoints = 127;
		if (wpoints != 0 && !nodamage) {
			// This may kill the NPC; this comes before powers because no
			// damage means no powers -- except for the no_damage flag.
			int tmp[] = new int[1];
			hits = applyDamage(attacker, 
					getEffectiveProp(npc, Actor.strength, 0),
					wpoints, type, bias, tmp);
			expval = tmp[0];
		}
			// Apply weapon powers if needed.
			// wpoints == 0 ==> some spells that don't hurt (but need to apply powers).
		if (powers != 0 && (hits != 0 || wpoints == 0 || nodamage)) {
				// Protection prevents powers.
			if (!getFlag(GameObject.protection))
				{
				int attint = getEffectiveProp(npc, Actor.intelligence, 16),
					defstr = getEffectiveProp(this, Actor.strength, 0),
					defint = getEffectiveProp(this, Actor.intelligence, 0);

					// These rolls are bourne from statistical analisys and are,
					// as far as I can tell, how the game works.
				if ((powers & WeaponInfo.poison) != 0 && rollToWin(attint, defstr))
					setFlag(GameObject.poisoned);
				if ((powers & WeaponInfo.curse) != 0 && rollToWin(attint, defint))
					setFlag(GameObject.cursed);
				if ((powers & WeaponInfo.charm) != 0 && rollToWin(attint, defint))
					setFlag(GameObject.charmed);
				if ((powers & WeaponInfo.sleep) != 0 && rollToWin(attint, defint))
					setFlag(GameObject.asleep);
				if ((powers & WeaponInfo.paralyze) != 0 && rollToWin(attint, defstr))
					setFlag(GameObject.paralyzed);
				if ((powers & WeaponInfo.magebane) != 0) {
						// Magebane weapons (magebane sword, death scythe).
						// Take away all mana.
					setProperty(Actor.mana, 0);
					int num_spells = 0;
					Vector<GameObject> vec = new Vector<GameObject>(50);
					getObjects(vec, EConst.c_any_shapenum, EConst.c_any_qual, 
														EConst.c_any_framenum);
						// Gather all spells... and take them away.
					for (GameObject obj : vec) {
						if (obj.getInfo().isSpell()) {	// Seems to be right.
							++num_spells;
							obj.removeThis();
						}
					}
					vec.clear();
					if (num_spells != 0) {
							// Display magebane struck string and set
							// no_spell_casting flag. This is only done
							// to prevent monsters from teleporting or
							// doing other such things.
						setFlag(GameObject.no_spell_casting);
						if (game.isSI()) {
							eman.removeTextEffect(this);
							say(ItemNames.first_magebane_struck, 
								ItemNames.last_magebane_struck);
						}
							// Tell schedule we need a new weapon.
						if (schedule != null && spots[Ready.lhand] == null)
							schedule.setWeapon(false);
					}
				}
			}
			if (nodamage && ammo_shape == 568) {
					// This is *only* done for SI sleep arrows, and all other
					// powers have had their effect by now (as can be verified
					// by using the called usecode function).
				if (npc != null)	// Just to be sure.
					setOppressor(npc.getNpcNum());
				// Allowing for BG too, as it doesn't have a function 0x7e1.
				ucmachine.callUsecode(0x7e1, this,UsecodeMachine.weapon);
			}
		}
		if (expval > 0 && npc != null)	// Give experience.
			npc.setProperty(exp, npc.getProperty(exp) + expval);

			// Weapon usecode comes last of all.
		if (usefun > 0) {
			if (npc != null)	// Just to be sure.
				setOppressor(npc.getNpcNum());
			ucmachine.callUsecode(usefun, this,
						UsecodeMachine.weapon);
		}
		return hits;
	}
	/*
	 *	Trying to hit NPC with an attack.
	 *
	 *	Output:	true if attack hit, false otherwise.
	 */
	@Override
	public boolean tryToHit
		(
		GameObject attacker,
		int attval
		)
		{
		int defval = getEffectiveProp(combat) +
				(getFlag(GameObject.protection) ? 3 : 0);
		if (CombatSchedule.combatTrace) {
			String name = "<trap>";
			if (attacker != null)
				name = attacker.getName();
			int prob = 30 - (15 + defval - attval) + 1;
			if (prob >= 30)
				prob = 29;	// 1 always misses.
			else if (prob <= 1)
				prob = 1;	// 30 always hits.
			prob *= 100;
			System.out.println(name + " is attacking " + getName()
				+ " with hit probability " + (float)prob/30 + "%");
			}

		return Actor.rollToWin(attval, defval);
		}

	/*
	 *	Being attacked.
	 *
	 *	Output:	0 if defeated, else object itself.
	 */
	public GameObject attacked
		(
		GameObject attacker,
		int weapon_shape,		// < 0 for readied weapon.
		int ammo_shape,			// < 0 for no ammo shape.
		boolean explosion			// If this is an explosion attacking.
		) {
		if (isDead() ||		// Already dead?
						// Or party member of dead Avatar?
		    (partyId >= 0 && gwin.getMainActor().isDead()))
			return null;
		Actor npc = attacker != null ? attacker.asActor() : null;
		if (npc != null)
			setOppressor(npc.getNpcNum());
		if (npc != null && npc.getScheduleType() == Schedule.duel)
			return this;	// Just play-fighting.

		int oldhp = properties[health];
		int delta = figureHitPoints(attacker, weapon_shape, ammo_shape, explosion);

		if (CombatSchedule.showHits && !isDead() && delta >= 0) {
			eman.removeTextEffect(this);
			String hpmsg = String.format("-%1$d(%2$d)", delta, oldhp - delta);
			eman.addText(hpmsg, this);
		}
		if (CombatSchedule.combatTrace) {
			String name = "<trap>";
			if (attacker != null)
				name = attacker.getName();
			System.out.print(name + " hits " + getName());
			if (delta > 0)
				{
				System.out.print(" for " + delta + " hit points; ");
				if (oldhp > 0 && oldhp < delta)
					System.out.println(getName() + " is defeated.");
				else
					System.out.println(oldhp - delta + " hit points are left.");
				}
			else if (delta == 0)
				System.out.println(" to no damage.");
			else
				System.out.println(" causing an explosion.");
			}

		if (attacker != null && (isDead() || properties[health] < 0))
			return null;
		return this;
	}
	/*
	 *	There's probably a smarter way to do this, but this routine checks
	 *	for the dragon Draco.
	 */

	static boolean isDraco(Actor dragon){
		Vector<GameObject> vec = new Vector<GameObject>();		// Gets list.
							// Should have a special scroll.
		int cnt = dragon.getObjects(vec, 797, 241, 4);
		return cnt > 0;
	}
	/*
	 *	We're dead.  We're removed from the world, but not deleted.
	 */

	public void die(GameObject attacker) {
		// If the actor is already dead, we shouldn't do anything
		//(fixes a resurrection bug).
		if (isDead())
			return;
		setAction(null);
		schedule = null;
		tqueue.remove(this);// Remove from time queue.
		setFlag(GameObject.dead);// IMPORTANT:  Set this before moving
						//   objs. so Usecode(eventid=6) isn't called.
		int shnum = getShapeNum();
						// Special case:  Hook, Dracothraxus.
		if (((shnum == 0x1fa || (shnum == 0x1f8 && isDraco(this))) && 
		    game.isBG())) {
						// Exec. usecode before dying.
			ucmachine.callUsecode(shnum, this, 
						UsecodeMachine.internal_exec);
			if (isPosInvalid())	// Invalid now?
				return;
		}
						// Get location.
		Tile pos = new Tile();
		getTile(pos);
		//properties[static_cast<int>(health)] = -50;
		ShapeInfo info = getInfo();
		MonsterInfo minfo = info.getMonsterInfo();
		boolean frost_serp = game.isSI() && getShapeNum() == 832;
		if ((frost_serp && (getFrameNum() & 0xf) == Actor.sit_frame)
			|| (getFrameNum() & 0xf) == Actor.sleep_frame) {
			UsecodeScript scr = new UsecodeScript(this);
			scr.add(UsecodeScript.delay_ticks, 4, UsecodeScript.remove);
			scr.start();
		} else	// Laying down to die.
			layDown(true);

		DeadBody body;		// See if we need a body.
		if (minfo == null || !minfo.hasNoBody()) {
					// Get body shape/frame.
			shnum = info.getBodyShape();		// Default 400.
			int frnum = info.getBodyFrame();	// Default 3.
						// Reflect if NPC reflected.
			frnum |= (getFrameNum()&32);
			body = new DeadBody(shnum, 0, 0, 0, 0, 
						npcNum > 0 ? npcNum : -1);
			UsecodeScript scr = new UsecodeScript(body);
			scr.add(UsecodeScript.delay_ticks, 4, UsecodeScript.frame, frnum);
			scr.start();
			if (npcNum > 0) {
					// Originals would use body.set_quality(2) instead
					// for bodies of dead monsters. What we must do for
					// backwards compatibility...
				body.setQuality(1);	// Flag for dead body of NPC.
				gwin.setBody(npcNum, body);
			}
						// Tmp. monster => tmp. body.
			if (getFlag(GameObject.is_temporary))
				body.setFlag(GameObject.is_temporary);
						// Okay to take its contents.
			body.setFlagRecursively(GameObject.okay_to_take);
						// Find a spot within 1 tile.
			if (!MapChunk.findSpot(pos, 1, shnum, frnum, 2))
				getTile(pos);	// Default to NPC pos, even if blocked.
			body.move(pos);
		} else
			body = null;
		GameObject item;		// Move/remove all the items.
		// Some shouldn't be moved.
		Vector<GameObject> tooheavy = new Vector<GameObject>();	
		Tile pos2 = new Tile();
		while ((item = objects.getFirst()) != null) {
			remove(item);
			item.setInvalid();
			// Guessing it is spells that get deleted.
			if (item.getInfo().isSpell()) {
				tooheavy.add(item);
				continue;
			}
			if (body != null)
				body.add(item, true);// Always succeed at adding.
			else {			// No body?  Drop on ground.
				item.setFlagRecursively(GameObject.okay_to_take);
				pos2.set(pos);
				if (MapChunk.findSpot(pos2, 5, item.getShapeNum(), 
															item.getFrameNum(), 1))
					item.move(pos2);
				else		// No room anywhere.
					tooheavy.add(item);
			}
		}
						// Put the heavy ones back.
		for (GameObject obj : tooheavy) {
			add(obj, true);
		}
		if (body != null)
			gwin.addDirty(body);
		addDirty();			// Want to repaint area.
		deleteContents();		// remove what's left of inventory
		Actor npc = attacker != null ? attacker.asActor() : null;
		if (npc != null) {
				// Set oppressor and cause nearby NPCs to attack attacker.
			fightBack(attacker);
			setTarget(null, false);
			setScheduleType(Schedule.wander);
					// Is this a bad guy?
					// Party defeated an evil monster?
			if (npc.getFlag(GameObject.in_party) && !getFlag(GameObject.in_party) && 
					alignment != neutral && alignment != friendly)
				CombatSchedule.monsterDied();
		}
			// Move party member to 'dead' list.
		partyman.updatePartyStatus(this);
	}
	private static int lastcall = 0;	// Last time yelled.
	public void fightBack (GameObject attacker) {
		// ++ TODO: I think that nearby NPCs will help NPCs (or party members,
		// as the case may be) of the same alignment when attacked by other NPCs,
		// not just when the avatar & party attack. Although this is tricky to
		// test (except, maybe, by exploiting the agressive U7 & SI duel schedule.
		Actor npc = attacker != null ? attacker.asActor() : null;
		if (npc == null)
			return;
		if (target == null && !getFlag(GameObject.in_party))
			setTarget(npc, npc.getScheduleType() != Schedule.duel);
		// Being a bully?
		if (npc.getFlag(GameObject.in_party) &&
				npcNum > 0 &&
				(alignment == Actor.friendly || alignment == Actor.neutral) &&
				(flags & (1<<GameObject.charmed)) == 0 && 
				!getFlag(GameObject.in_party) &&
				getInfo().getShapeClass() == ShapeInfo.human) {
			
			int curtime = TimeQueue.ticks;
			int delta = curtime - lastcall;
			if (delta > 10000) {	// Call if 10 secs. has passed.
				eman.removeTextEffect(this);
				say(ItemNames.first_call_police, ItemNames.last_call_police);
				lastcall = curtime;
				gwin.attackAvatar(1 + EUtil.rand()%2);
			} else if (EUtil.rand()%20 == 0) {
				gwin.attackAvatar(1 + EUtil.rand()%2);
				// To reduce the guard pile-up.
				lastcall = curtime;
			}
		}
	}
	public void setAttackTarget(GameObject t, int w) {
		targetTile = null;
		targetObject = t;
		attackWeapon = w;
	}
	public void setAttackTarget(Tile t, int w) {
		targetObject = null;
		targetTile = t;
		t.fixme();
		attackWeapon = w;
	}
	/*
	 *	This method should be called to cause damage from traps, attacks.
	 *
	 *	Output:	Hits taken. If exp is nonzero, experience value if defeated.
	 */
	@Override
	public int applyDamage
		(
		GameObject attacker,	// Attacker, or null.
		int str,		// Attack strength.
		int wpoints,	// Weapon bonus.
		int type,		// Damage type.
		int bias,		// Different combat difficulty.
		int exp[]
		)
		{
		if (exp != null)
			exp[0] = 0;
		int damage = bias;
		str /= 3;

			// In the original, wpoints == 127 does fixed 127 damage.
			// Allowing for >= 127 in Exult, as the original seems to
			// use only a byte for damage/health.
		if (wpoints >= 127)
			damage = 127;
		else {
				// Lightning damage ignores strength.
			if (type != WeaponInfo.lightning_damage && str > 0)
				damage += (1 + EUtil.rand()%str);
			if (wpoints > 0)
				damage += (1 + EUtil.rand()%wpoints);
		}
		int armor = -bias;
		MonsterInfo minf = getInfo().getMonsterInfo();
			// Monster armor protects only in UI_apply_damage.
		if (minf != null)
			armor += minf.getArmor();

			// Armor defense and immunities only affect UI_apply_damage.
		final int num_spots = spots.length;
		for (int i = 0; i < num_spots; i++) {
			GameObject obj = spots[i];
			if (obj != null) {
				ShapeInfo info = obj.getInfo();
				armor += info.getArmor();
				if ((info.getArmorImmunity() & (1 << type)) != 0) {
						// Armor gives immunity.
						// Metal clang.
					int sfx = Audio.gameSfx(5);
					new Animator.ObjectSfx(this, sfx, 0);
						// Attack back anyway.
					fightBack(attacker);
					return 0;	// No damage == no powers.
					}
				}
			}

			// Some attacks ignore armor (unless the armor gives immunity).
		if (wpoints == 127 || type == WeaponInfo.lightning_damage ||
				type == WeaponInfo.ethereal_damage ||
				type == WeaponInfo.sonic_damage ||
				armor < 0)	// Armor should never help the attacker.
			armor = 0;

		if (armor != 0)
			damage -= (1 + EUtil.rand()%armor);

			// Paralyzed/defenseless foes may take damage even if
			// the armor protects them. This code is guesswork,
			// but it matches statistical tests.
		if (damage <= 0 && !canAct()) {
			if (str > 0)
				damage = 1 + EUtil.rand()%str;
			else
				damage = 0;
		}
		if (damage <= 0) {	// No damage caused.
			int sfx = Audio.gameSfx(5);
			new Animator.ObjectSfx(this, sfx, 0);

				// Flash red outline.
			hit = true;
			addDirty();
			ClearHit c = new ClearHit();
			tqueue.add(TimeQueue.ticks + 1, c, this);
				// Attack back.
			fightBack(attacker);
			return 0;	// No damage == no powers (usually).
		}

		return reduceHealth(damage, type, attacker, exp);
	}
	/*
	 *	This method should be called to decrement health directly.
	 *
	 *	Output:	Hits taken. If exp is nonzero, experience value if defeated.
	 */
	@Override
	public int reduceHealth
		(
		int delta,			// # points to lose.
		int type,		// Type of damage
		GameObject attacker,		// Attacker, or null.
		int exp[]
		)
		{
		if (exp != null)
			exp[0] = 0;
			// Cheater, cheater.
		if (isDead() || (cheat.inGodMode() && ((partyId != -1) || (npcNum == 0))))
			return 0;

		MonsterInfo minf = getInfo().getMonsterInfoSafe();
		Actor npc = attacker != null ? attacker.asActor() : null;

			// Monster immunities DO affect UI_reduce_health, unlike
			// armor immunities.
		if (isDead() || minf.cantDie() ||
			(minf.getImmune() & (1 << type)) != 0) {
				// Monster data gives immunity to damage.
				// Attack back.
			fightBack(attacker);
			return 0;
		}
			// Monsters vulnerable to a damage type take 2x the damage.
			// The originals seem to limit damage to 127 points; we
			// set a lower bound on final health below.
		if ((minf.getVulnerable() & (1 << type)) != 0)
			delta *= 2;

		int oldhp = properties[health];
		int maxhp = properties[strength];
		int val = oldhp - delta;
		if (val < -50) {	// Limit how low it goes for safety.
			val = -50;
			delta = oldhp + 50;
		}
			// Don't set health yet!! (see tournament below for why)
			// The following thresholds are exact.
		if (this == gwin.getMainActor() &&
						// Flash red if Avatar badly hurt.
				(delta >= maxhp/3 || oldhp < maxhp/4 ||
						// Or if lightning damage.
				type == WeaponInfo.lightning_damage))
			; //+++++++++FINISH gwin.getPal().flashRed();
		else {
			hit = true;		// Flash red outline.
			addDirty();
			ClearHit c = new ClearHit();
			tqueue.add(TimeQueue.ticks + 1, c, this);
		}
		if (oldhp >= maxhp/2 && val < maxhp/2 && EUtil.rand()%2 != 0) {
						// A little oomph.
						// Goblin?
			if (game.isSI() &&
				 (getShapeNum() == 0x1de ||
				  getShapeNum() == 0x2b3 ||
				  getShapeNum() == 0x2d5 ||
				  getShapeNum() == 0x2e8))
				say(0x4d2, 0x4da);	// ++++ TODO: Not sure they use all these.
			else if (minf == null || !minf.cantYell())
				say(ItemNames.first_ouch, ItemNames.last_ouch);
		}
		// Create blood.
		int blood = 912;		// ++++TAG for future de-hard-coding.
				// Bleed only for normal damage.
		if (type == WeaponInfo.normal_damage && !minf.cantBleed()
			// Trying something new. Seems to match originals better, but
			// it is hard to judge accurately (although 10 or more hits
			// *always* cause bleeding).
			&& EUtil.rand()%10 < delta
			&& findNearby(new Vector<GameObject>(), blood, 1, 0) < 2) {
						// Create blood where actor stands.
			GameObject bobj = IregGameObject.create(blood, 0);
			bobj.setFlag(GameObject.is_temporary);
			bobj.move(getTileX(), getTileY(), getLift());
		}
		if (val <= 0 && oldhp > 0 && getFlag(GameObject.tournament)) {
				// HPs are never reduced before tournament usecode
				// (this can be checked on weapon usecode, tournament
				// usecode or even UI_reduce_health directly followed
				// by UI_get_npc_prop).
				// THIS is why we haven't reduced health yet.
				// This makes foes with tournament flag EXTREMELLY
				// tough, particularly if they have high hit points
				// to begin with!
				// No more pushover banes!
			if (npc != null)	// Just to be sure.
				setOppressor(npc.getNpcNum());
			ucmachine.callUsecode(getUsecode(), this, UsecodeMachine.died);
			return 0;	// If needed, NPC usecode does the killing (possibly
						// by calling this function again).
		}

			// We do slimes here; they DO split through reduce_health intrinsic.
			// They also do *not* split if hit by damage they are vulnerable to.
		if (minf.splits() && val > 0 && 
			(minf.getVulnerable() & (1 << type)) == 0 && EUtil.rand()%2 == 0)
			clone();
		
			// Doing this here simplifies the tournament code, above.
		properties[health] = val;
		boolean defeated = isDying() || (val <= 0 && oldhp > 0);
		
		if (defeated && exp != null) {
				// Verified: No experience for killing sleeping people.
			if (!getFlag(GameObject.asleep)) {
				int expval = 0;
					// Except for 2 details mentioned below, this formula
					// is an exact match to what the originals give.
					// We also have to do this *here*, before we kill the
					// NPC, because the equipment is deleted (spells) or
					// transferred to the dead body it leaves.
				int combval = properties[combat];
				expval = properties[strength] + combval + 
						(properties[dexterity]+1)/3 + 
						properties[intelligence]/5;
				minf = getInfo().getMonsterInfo();
				int immune = minf != null ? minf.getImmune() : 0;
				int vuln = minf != null ? minf.getVulnerable() : 0;
				if (minf != null)
					expval += minf.getBaseXpValue();

				if (!objects.isEmpty()) {
							// Get list of all possessions.
					Vector<GameObject> vec = new Vector<GameObject>(50);
					getObjects(vec, EConst.c_any_shapenum, EConst.c_any_qual, 
							EConst.c_any_framenum);
					for (GameObject obj : vec) {
							// This matches the original, but maybe
							// we should iterate through all items.
							// After all, a death bolt in the backpack
							// can still be dangerous...
						if (obj.getOwner() != this)
							continue;
						ShapeInfo inf = obj.getInfo();
						expval += inf.getArmor();
							// Strictly speaking, the original does not give
							// XP for armor immunities; but I guess this is
							// mostly because very few armors give immunities
							// (ethereal ring, cadellite helm) and no monsters
							// use them anyway.
							// I decided to have them give a (non-cumulative)
							// increase in experience.
						immune |= inf.getArmorImmunity();
						WeaponInfo winf = inf.getWeaponInfo();
						if (winf == null)
							continue;
						expval += winf.getBaseXpValue();
							// This was tough to figure out, but figured out it was;
							// it is a perfect match to what the original does.
						switch (winf.getUses()) {
						case WeaponInfo.melee:
							{
							int range = winf.getRange();
							expval += range > 5 ? 2 : (range > 3 ? 1 : 0);
							break;
							}
						case WeaponInfo.poor_thrown:
							expval += combval/5; break;
						case WeaponInfo.good_thrown:
							expval += combval/3; break;
						case WeaponInfo.ranged:
							expval += winf.getRange()/2; break;
						}
					}
					}
					// Originals don't do this, but hey... they *should*.
					// Also, being vulnerable to something you are immune
					// should not matter because immunities are checked first;
					// the originals don't do this check, but neither do they
					// have a monster vulnerable and immune to the same thing.
				vuln &= ~immune;
				expval += EUtil.bitcount((byte)immune);
				expval -= EUtil.bitcount((byte)vuln);
					// And the final touch (verified).
				expval /= 2;
				exp[0] = expval;
				}
			}

		if (isDying())
			die(attacker);
		else if (val <= 0 && !getFlag(GameObject.asleep)) {
			CombatSchedule.stopAttackingNpc(this);
			setFlag(GameObject.asleep);
		} else if (npc != null && target == null  && !getFlag(GameObject.in_party)) {
			setTarget(npc, npc.getScheduleType() != Schedule.duel);
			setOppressor(npc.getNpcNum());
		}
		fightBack(attacker);
		return delta;
	}	
	/*
	 *	Causes the actor to fall to the ground and take damage.
	 */
	public void fallDown() {
		if (!getTypeFlag(tf_fly))
			return;
		int tx = getTileX(), ty = getTileY(), tz = getLift();
		int newz;
		if ((newz = gmap.spotAvailable(1, tx, ty, tz, 
								EConst.MOVE_WALK, 100, -1)) >= 0 && 
				newz < tz) {
			move(tx, ty, newz);
			reduceHealth(1 + EUtil.rand()%5, WeaponInfo.normal_damage, null, null);
		}
	}
	/*
	 *	Causes the actor to lie down to sleep (or die).
	 */
	public void layDown(boolean die) {
		if (!die && getFlag(GameObject.asleep))
			return;
			// Fall to the ground.
		fallDown();
		boolean frost_serp = game.isSI() && getShapeNum() == 832;
			// Don't do it if already in sleeping frame.
		if (frost_serp && (getFrameNum() & 0xf) == Actor.sit_frame)
			return;		// SI Frost serpents are weird.
		else if ((getFrameNum() & 0xf) == Actor.sleep_frame)
			return;

		setAction(null);
		UsecodeScript scr = new UsecodeScript(this);
		scr.add(UsecodeScript.finish, (UsecodeScript.npc_frame + Actor.standing));
		if (game.isSI() && getShapeNum() == 832) {	// SI Frost serpent
					// Frames are in reversed order. This results in a
					// strangeanimation in the original, which we fix here.
			scr.add((UsecodeScript.npc_frame + Actor.sleep_frame),
					(UsecodeScript.npc_frame + Actor.kneel_frame),
					(UsecodeScript.npc_frame + Actor.bow_frame),
					(UsecodeScript.npc_frame + Actor.sit_frame));
		} else if (game.isBG() && getShapeNum() == 525) {	// BG Sea serpent
			scr.add((UsecodeScript.npc_frame + Actor.bow_frame),
					(UsecodeScript.npc_frame + Actor.kneel_frame),
					(UsecodeScript.npc_frame + Actor.sleep_frame));
		} else {	// Slimes are done elsewhere.
			scr.add((UsecodeScript.npc_frame + Actor.kneel_frame),
					UsecodeScript.sfx, Audio.gameSfx(86));
			if (!die)
				scr.add(UsecodeScript.npc_frame + Actor.sleep_frame);
		}
		if (die)	// If dying, remove.
			scr.add(UsecodeScript.remove);
		scr.start();
	}
	/*
	 *	Create another monster of the same type as this, and adjacent.
	 *
	 *	Output:	monster, or 0 if failed.
	 */
	public MonsterActor clone() {
		ShapeInfo info = getInfo();
						// Base distance on greater dim.
		int frame = getFrameNum();
		int xs = info.get3dXtiles(frame), ys = info.get3dYtiles(frame);
		Tile pos = new Tile();				// Find spot.
		getTile(pos);
		if (!MapChunk.findSpot(pos, xs > ys ? xs : ys, getShapeNum(), 0, 1))
			return null;		// Failed.
						// Create, temporary & with equip.
		MonsterActor monst = MonsterActor.create(
				getShapeNum(), pos, getScheduleType(),
				getAlignment(), true, true);
		return monst;
	}
	/*
	 *	Restore HP's on the hour.
	 */
	public void mendHourly() {
		if (isDead())
			return;
		int maxhp = properties[strength];
		int hp = properties[health];
		if (maxhp > 0 && hp < maxhp) {
			if (maxhp >= 3)  
				hp += 1 + EUtil.rand()%(maxhp/3);
			else
				hp += 1;
			if (hp > maxhp)
				hp = maxhp;
			properties[health] = hp;
						// ??If asleep & hps now >= 0, should
						//   we awaken?
		}
						// Restore some mana also.
		int maxmana = properties[magic];
		int curmana = properties[mana];
		clearFlag(GameObject.no_spell_casting);
		if (maxmana > 0 && curmana < maxmana) {
			if (maxmana >= 3)	
				curmana += 1 + EUtil.rand()%(maxmana/3);
			else
				curmana += 1;
			properties[mana] = curmana <= maxmana ? curmana : maxmana;
		}
	}
	/*
	 *	Restore from body.  It must not be owned by anyone.
	 *
	 *	Output:	actor if successful, else 0.
	 */
	public Actor resurrect
		(
		DeadBody body			// Must be this actor's body.
		)
		{
		Tile pos = new Tile();
		if (body != null) {
			if (body.getOwner() != null ||	// Must be on ground.
				npcNum <= 0 || gwin.getBody(npcNum) != body)
				return null;
			gwin.setBody(npcNum, null);	// Clear from gwin's list.
			GameObject item;		// Get back all the items.
			ObjectList.ObjectIterator iter = body.getIterator();
			while ((item = iter.next()) != null) {
				body.remove(item);
				add(item, true);		// Always succeed at adding.
			}
			gwin.addDirty(body);		// Need to repaint here.
			body.getTile(pos);
			body.removeThis();		// Remove and delete body.
		} else
			pos.set(-1, -1, 0);
		move(pos);			// Move back to life.
						// Restore health to max.
		properties[health] = properties[strength];
		clearFlag(GameObject.dead);
		clearFlag(GameObject.poisoned);
		clearFlag(GameObject.paralyzed);
		clearFlag(GameObject.asleep);
		clearFlag(GameObject.protection);
		clearFlag(GameObject.cursed);
		clearFlag(GameObject.charmed);
						// Restore to party if possible.
		partyman.updatePartyStatus(this);
						// Give a reasonable schedule.
		setScheduleType(getFlag(Actor.in_party) ? Schedule.follow_avatar
						: Schedule.loiter);
						// Stand up.
		if (body == null)
			return (this);
		UsecodeScript scr = new UsecodeScript(this);
		scr.add((UsecodeScript.npc_frame + Actor.sleep_frame),
				(UsecodeScript.npc_frame + Actor.kneel_frame),
				(UsecodeScript.npc_frame + Actor.standing));
		scr.start(1);
		return (this);
	}
	public final boolean addDirty(boolean figureWeapon) {
		if (!gwin.addDirty(this))
			return false;
		if (figureWeapon || getCastingMode() == Actor.show_casting_frames) {
			if (figureWeaponPos(weaponRect)) {
				int weapon_x = weaponRect.x, weapon_y = weaponRect.y, 
					weapon_frame = weaponRect.w;
				int shnum = getEffectiveWeaponShape();
			
				ShapeFrame wshape = 
					ShapeFiles.SHAPES_VGA.getShape(shnum, weapon_frame);
				if (wshape != null)	// Set dirty area rel. to NPC.
					gwin.getShapeRect(weaponRect, wshape, weapon_x, weapon_y);
				else
					weaponRect.w = 0;
			} else
			weaponRect.w = 0;
		}
		if (weaponRect.w > 0) {		// Repaint weapon area too.
			Rectangle r = tempRect;
			r.set(weaponRect);
			gwin.getShapeLocation(weaponPoint, this);
			r.shift(weaponPoint.x, weaponPoint.y);
			r.enlarge(EConst.c_tilesize/2);
			gwin.clipToWin(r);
			gwin.addDirty(r);
		}
		return true;
	}
	public final boolean addDirty() {
		return addDirty(false);
	}
	public final void changeFrame(int frnum) {
		addDirty(false);			// Set to repaint old area.
		int shnum = getShapeNum();
		ShapeFrame shape = getShapeFile().getFile().getShape(shnum, frnum);
		if (shape == null || shape.isEmpty()) {
					// Swap 1hand <=> 2hand frames.
			frnum = (frnum&48)|visibleFrames[frnum&15];
			shape = getShapeFile().getFile().getShape(shnum, frnum);
			if (shape == null || shape.isEmpty())
				frnum = (frnum&48)|Actor.standing;
		}
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
	// Get attack frames.  'frames' should have room for 12.
	public int getAttackFrames(int weapon, boolean projectile,
				int dir, byte frames[]) {
		final byte baseframes[] = {0, 0, 0, 0};
		byte which[] = baseframes;
		int cnt = 4;
		switch (getShapeNum()) {		// Special cases.
		case 525:			// Sea serpent.
			which = sea_serpent_attack_frames;
			cnt = sea_serpent_attack_frames.length;
			break;
		case 529:			// Slimes.
			return 0;		// None, I believe.
		default:
			byte[] reach_attack_frames;
			byte[] raise_attack_frames;
			byte[] fast_swing_attack_frames;
			byte[] slow_swing_attack_frames;
			if (twoHanded) {
				reach_attack_frames = reach_attack_frames2;
				raise_attack_frames = raise_attack_frames2;
				fast_swing_attack_frames = fast_swing_attack_frames2;
				slow_swing_attack_frames = slow_swing_attack_frames2;
			} else {
				reach_attack_frames = reach_attack_frames1;
				raise_attack_frames = raise_attack_frames1;
				fast_swing_attack_frames = fast_swing_attack_frames1;
				slow_swing_attack_frames = slow_swing_attack_frames1;
			}
			int frame_flags;	// Get Actor_frame flags.
			WeaponInfo winfo;
			if (weapon >= 0 && 
			    (winfo = ShapeID.getInfo(weapon).getWeaponInfo()) != null)
				frame_flags = winfo.getActorFrames(projectile);
			else				// Default to normal swing.
				frame_flags = projectile ? WeaponInfo.reach : WeaponInfo.fast_swing;
			switch (frame_flags) {
				case WeaponInfo.reach:
					which = reach_attack_frames;
					cnt = reach_attack_frames1.length;
					break;
				case WeaponInfo.raise:
					which = raise_attack_frames;
					cnt = raise_attack_frames1.length;
					break;
				case WeaponInfo.fast_swing:
					which = fast_swing_attack_frames;
					cnt = fast_swing_attack_frames1.length;
					break;
				case WeaponInfo.slow_swing:
					which = slow_swing_attack_frames;
					cnt = slow_swing_attack_frames1.length;
					break;
			}
			break;
		}
		for (int i = 0; i < cnt; i++) {	// Copy frames with correct dir.
			int frame = getDirFramenum(dir, which[i]);
						// Check for empty shape.
			int shnum = getShapeNum();
			ShapeFrame shape = getShapeFile().getShape(shnum, frame);
			if (shape == null || shape.isEmpty()) {
						// Swap 1hand <=> 2hand frames.
				frame = getDirFramenum(dir,visibleFrames[frame&15]);
				shape = getShapeFile().getShape(shnum, frame);
				if (shape == null || shape.isEmpty())
					frame = getDirFramenum(dir, Actor.standing);
			}
			frames[i] = (byte)frame;
		}
		return (cnt);
	}
	public final boolean hasLightSource() 	// Carrying a torch?
		{ return lightSources > 0; }
	public final void addLightSource()	// Add a torch
		{ lightSources++; }
	public final void removeLightSource() {	// Remove a torch
		if (lightSources > 0)
			lightSources--;
	}
	/*
	 *	Get effective weapon shape, taking casting frames in consideration.
	 */
	private int getEffectiveWeaponShape() {
		if (getCastingMode() == Actor.show_casting_frames)
			// Casting frames
			return castingShape;
		else {
			GameObject weapon = spots[Ready.lhand];
			return weapon.getShapeNum();
		}
	}
	protected void paintWeapon() {
		
		if (figureWeaponPos(weaponRect)) {
			int weapon_x = weaponRect.x, weapon_y = weaponRect.y, 
				weapon_frame = weaponRect.w;
			int shnum = getEffectiveWeaponShape();
			ShapeFrame wshape = ShapeFiles.SHAPES_VGA.getShape(shnum, weapon_frame);
			if (wshape == null) {
				weaponRect.w = 0;
				return;
			}
						// Set dirty area rel. to NPC.
			gwin.getShapeRect(weaponRect, wshape, weapon_x, weapon_y);
			// Paint the weapon shape using the actor's coordinates
			gwin.getShapeLocation(weaponPoint, this);
			int xoff = weaponPoint.x + weapon_x, yoff = weaponPoint.y + weapon_y;
			/* +++++++FINISH if ((flags & (1L<<GameObject.invisible)) != 0)
				wshape.paintInvisible(xoff, yoff);
			else */
				wshape.paint(gwin.getWin(), xoff, yoff);
		} else
			weaponRect.w = 0;
	}
	/*
	 *	Figure weapon drawing info.  We need this in advance to set the dirty
	 *	rectangle.
	 *
	 *	Output:	false if don't need to paint weapon.
	 *  Weapon frames:
		0 - normal item
		1 - in hand, actor facing north/south
		2 - attacking (pointing north)
		3 - attacking (pointing east)
		4 - attacking (pointing south)
	*/
	private boolean figureWeaponPos(Rectangle ret) {	// Gets x, y, weapon_frame
		int weapon_frame;
		if((spots[Ready.lhand] == null) && (castingMode != Actor.show_casting_frames))
			return false;
		// Get offsets for actor shape
		int myframe = getFrameNum();
		int off = getInfo().getWeaponOffset(myframe & 0x1f);
		int actor_x = (off>>8)&0xff, actor_y = off&0xff;
		// Get weapon frames for actor frame:
		switch (myframe & 0x1f) {
			case 4:
			case 7:
			case 22:
			case 25:
				weapon_frame = 4;
				break;
			case 5:
			case 8:
			case 21:
			case 24:
				weapon_frame = 3;
				break;
			case 6:
			case 9:
			case 20:
			case 23:
				weapon_frame = 2;
				break;
			//The next cases (before the default) are here to make use of all
			//the frames of the "casting frames" shape (shape 859):
			case 14:
			case 30:
				weapon_frame = 5;
				break;
			case 15:
				weapon_frame = 6;
				break;
			case 31:
				weapon_frame = 7;
				break;
			
			default:
				weapon_frame = 1;
		}
		weapon_frame |= (myframe & 32);

		// Get offsets for weapon shape
		int shnum = getEffectiveWeaponShape();
		ShapeInfo info = ShapeID.getInfo(shnum);
		off = info.getWeaponOffset(weapon_frame&0xf);
		int wx = (off>>8)&0xff, wy = off&0xff;
		// actor_x will be 255 if (for example) the actor is lying down
		// wx will be 255 if the actor is not holding a proper weapon
		if(actor_x != 255 && wx != 255) {
						// Store offsets rel. to NPC.
			ret.x = wx - actor_x;
			ret.y = wy - actor_y;
			ret.w = weapon_frame;
			// Need to swap offsets if actor's shape is reflected
			if((myframe & 32) != 0) {
				ret.x = wy - actor_y;
				ret.y = wx - actor_x;
			}			
			return true;// Combat frames are already done.
		} else
			return false;
	}
	public void paint() {
		int flag = game.isBG() ? GameObject.bg_dont_render : GameObject.dont_render;
		if ((flags & (1L << flag)) == 0) {
			gwin.getShapeLocation(paintLoc, this);
			int xoff = paintLoc.x, yoff = paintLoc.y;
			boolean invis = (flags & (1L << GameObject.invisible)) != 0;
			if (invis && partyId < 0 && this != gwin.getMainActor())
				return;	// Don't render invisible NPCs not in party.
			/* ++++++++++FINISH else if (invis)
				paint_invisible(xoff, yoff);
			*/ else
				paintShape(xoff, yoff);
			
			paintWeapon();
			if (hit)		// Want a momentary red outline.
				paintOutline(xoff, yoff, HIT_PIXEL);
			else if ((flags & ((1<<GameObject.protection) | 
			    (1 << GameObject.poisoned) | (1 << GameObject.cursed) |
			    	(1 << GameObject.charmed) | (1 << GameObject.paralyzed))) != 0)
				{
				if ((flags & (1 << GameObject.poisoned)) != 0)
					paintOutline(xoff,yoff,POISON_PIXEL);
				else if ((flags & (1 << GameObject.cursed)) != 0)
					paintOutline(xoff,yoff,CURSED_PIXEL);
				else if ((flags & (1 << GameObject.charmed)) != 0)
					paintOutline(xoff, yoff,
									CHARMED_PIXEL);
				else if ((flags & (1 << GameObject.paralyzed)) != 0)
					paintOutline(xoff, yoff, PARALYZE_PIXEL);
				else
					paintOutline(xoff, yoff, PROTECT_PIXEL);
				}
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
			(show_party_inv && partyId >= 0)  || // Party
			// Pickpocket cheat && double click
			(cheat.inPickpocket() && event == 1))
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
		else if (partyId >= 0 || gwin.isTimeStopped() == 0)
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
		/* ++++++++++REMOVE
		if (obj.getShapeNum() == 857) {
			//+++++JSF-Android - Dragonbreath: Kludge until we read in paperdoll info.
			if (spot == Ready.backpack)
				return false;
		}
		*/
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
		else if (spot == Ready.lhand || spot == Ready.rhand) {
			if (obj.getShapeNum() == 857)
				System.out.println("fitsInSpot " + spot + " successful");
			return true;
		}
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
		if (index < 0) {
			System.out.println("Can't add obj " + obj.getShapeNum() +
					", to " + getShapeNum());
			return false;
		}
		spots[index] = obj;		// Store in correct spot.
		if (index == Ready.lhand && schedule != null && !noset)
			schedule.setWeapon(false);	// Tell combat-schedule about it.
		obj.setShapePos(0, 0);	// Clear coords. (set by gump).
		if (!dont_check)
			callReadiedUsecode(index, obj, UsecodeMachine.readied);
						// (Readied usecode now in drop().)
		ShapeInfo info = obj.getInfo();

		if (info.isLightSource() &&
				(index == Ready.lhand || index == Ready.rhand))
			lightSources++;

		// Refigure granted immunities.
		
		gearImmunities |= info.getArmorImmunity();
		gearPowers |= info.getObjectFlags(obj.getFrameNum(),
					info.hasQuality() ? obj.getQuality() : -1);
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
			
			if (index == Ready.lhand && schedule != null)
				schedule.setWeapon(true);
			// Recheck armor immunities and frame powers.
			refigureGear();
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
				gump = ShapeID.getInfo(get_sexed_coloured_shape()).get_gump_shape();
			if (gump < 0)
				gump = ShapeID.getInfo(get_shape_real()).get_gump_shape();
			
			if (gump < 0) {
				int shape = getTypeFlag(Actor.tf_sex) ?
					Shapeinfo_lookup.GetFemaleAvShape() :
					Shapeinfo_lookup.GetMaleAvShape();
				gump = ShapeID.getInfo(shape).get_gump_shape();
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
			int ind = findReadied(obj);
			if (ind >= 0)
				callReadiedUsecode(ind, obj, UsecodeMachine.readied);
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
	/*
	 *	Want one value to approach another.
	 */
	private int approach
		(
		int from, int to,
		int dist			// Desired distance.
		) {
		if (from <= to)			// Going forwards?
			return (to - from <= dist ? from : to - dist);
		else				// Going backwards.
			return (from - to <= dist ? from : to + dist);
	}
	/*
	 *	Follow the leader.
	 */
	public void follow(Actor leader){
		if (isDead())
			return;			// Not when dead.
		int delay = 0;
		int dist;			// How close to aim for.
		Tile leaderpos = new Tile();
		leader.getTile(leaderpos);
		Tile pos = new Tile();
		getTile(pos);
		Tile goal = new Tile();
		if (leader.isMoving()) {	// Figure where to aim.
						// Aim for leader's dest.
			dist = 2 + partyId/3;
			leader.getDest(goal);
			goal.tx = (short) approach(pos.tx, goal.tx, dist);
			goal.ty = (short) approach(pos.ty, goal.ty, dist);
		} else {			// Leader stopped?
			goal = leaderpos;	// Aim for leader.
			if (distance(leader) <= 6)
				return;		// In formation, & close enough.
//			cout << "Follow:  Leader is stopped" << endl;
			// ++For formation, why not get correct positions?
			final int xoffs[] = {-1, 1, -2, 2, -3, 3, -4, 4, -5, 5},
				   	   yoffs[] = {1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
			if (partyId < 0 || partyId >= xoffs.length) {
				System.out.println("Actor.follow: partyId = " + partyId +
						", Npc # = " + npcNum);
				return;
			}
			goal.tx += xoffs[partyId] + 1 - EUtil.rand()%3;
			goal.ty += yoffs[partyId] + 1 - EUtil.rand()%3;
			dist = 1;
			}
						// Already aiming along a path?
		if (isMoving() && action != null && action.followingSmartPath()) {
						// And leader moving, or dest ~= goal?
			if (leader.isMoving())
				return;
			Tile dest = new Tile();
			getDest(dest);
			if (goal.distance(dest) <= 5)
				return;
		}
						// Tiles to goal.
		int goaldist = goal.distance(pos);
		if (goaldist < dist) {		// Already close enough?
			if (!leader.isMoving())
				stop();
			return;
		}
						// Is leader following a path?
		boolean leaderpath = leader.action != null && 
					leader.action.followingSmartPath();
						// Get leader's distance from goal.
		int leaderdist = leader.distance(goal);
						// Get his speed.
		int speed = leader.getFrameTime();
		if (speed == 0) {			// Not moving?
			speed = 100;
			if (goaldist < leaderdist)	// Closer than leader?
						// Delay a bit IF not moving.
				delay = (1 + leaderdist - goaldist)*100;
			}
		if (goaldist - leaderdist >= 5)
			speed -= 20;		// Speed up if too far.
						// Get window rect. in tiles.
		Rectangle wrect = new Rectangle();
		gwin.getWinTileRect(wrect);
		int dist2lead = leader.distance(pos);
						// Getting kind of far away?
		if (dist2lead > wrect.w + wrect.w/2 &&
		    partyId >= 0 &&		// And a member of the party.
		    !leaderpath)		// But leader is not following path.
			{			// Approach, or teleport.
						// Try to approach from offscreen.
			if (approachAnother(leader, false))
				return;
						// Find a free spot.
			leader.getTile(goal);
			if (!MapChunk.findSpot(goal, 2, this, 0, MapChunk.anywhere)) {
				move(goal.tx, goal.ty, goal.tz);
				if (EUtil.rand()%2 != 0)
					say(ItemNames.first_catchup, ItemNames.last_catchup);
				gwin.paint();
				return;
				}
			}
						// NOTE:  Avoid delay when moving,
						//  as it creates jerkiness.  AND,
						//  0 retries if blocked.
		walkToTile(goal, speed, delay, 0);
		}

	
	/*
	 *	Approach another actor from offscreen.
	 *
	 *	Output:	false if failed.
	 */
	public boolean approachAnother
		(
		Actor other,
		boolean wait			// If true, game hangs until arrival.
		) {
		Tile dest = new Tile();
		other.getTile(dest);
						// Look outwards for free spot.
		if (!MapChunk.findSpot(dest, 8, getShapeNum(), getFrameNum(), 0))
			return false;
						// Where are we now?
		Tile src = new Tile();
		getTile(src);
		Rectangle r = new Rectangle();
		gwin.getWinTileRect(r);
		if (!r.hasPoint(src.tx - src.tz/2, 
								src.ty - src.tz/2))
						// Off-screen?
			src.set(-1, -1, 0);
		int destmap = other.getMapNum();
		int srcmap = getMapNum();
		if (destmap != -1 && srcmap != -1 && srcmap != destmap) {
			src.set(-1, -1, 0);
			move(src.tx, src.ty, src.tz, destmap);
		}
		ActorAction action = new ActorAction.PathWalking(null);
		if (action.walkToTile(this, src, dest, 0) == null) {
			return false;
		}
		setAction(action);
		int speed = 1;
		start(speed, 0);			// Walk fairly fast.
		
		if (wait)			// Only wait ~1/5 sec.
			waitForArrival(dest, 2);
		return true;
	}
	public final int getOppressor() {
		return oppressor;
	}
	public final void setOppressor(int opp) {
		oppressor = (short)opp;
	}
	public final GameObject getTarget() {
		return target;
	}
	public final void setTarget(GameObject obj, boolean startCombat) {
		target = obj;
		boolean im_party = getFlag(GameObject.in_party) || this == gwin.getMainActor();
		if (startCombat && !im_party &&
				(scheduleType != Schedule.combat || schedule == null))
			setScheduleType(Schedule.combat);
		Actor opponent = obj != null ? obj.asActor() : null;
		if (opponent != null)
			opponent.setOppressor(getNpcNum());
			// Pure guess.
		Actor oppr = oppressor >= 0 ? gwin.getNpc(oppressor) : null;
		if (oppr != null && (oppr.getTarget() != this ||
				oppr.getScheduleType() != Schedule.combat))
			oppressor = -1;
	}
	public final void setTarget(GameObject obj) {
		setTarget(obj, false);
	}
	public final boolean canAct() {
		return !(getFlag(GameObject.paralyzed) || getFlag(GameObject.asleep)
				|| isDead() || getProperty(health) <= 0);
	}
	//	Get destination or current spot if none.
	public void getDest(Tile dest) {
		if (action != null && action.getDest(dest))
			return;
		else
			getTile(dest);
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
			action = new ActorAction.PathWalking(zombiePath, maxblk);
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
	/*
	 *	Find a path towards a given tile.
	 *	Output:	0 if failed.
	 */
	public boolean walkPathToTile
		(
		Tile src,			// Our location, or an off-screen
						//   location to try path from.
		Tile dest,		// Destination.
		int speed,			// Time between frames (msecs).
		int delay,			// Delay before starting (msecs) (only
						//   if not already moving).
		int dist,			// Distance to get within dest.
		int maxblk			// Max. # retries if blocked.
		) {
		setAction(new ActorAction.PathWalking(new AStarPathFinder(), maxblk));
		setAction(action.walkToTile(this, src, dest, dist));
		if (action != null) {			// Successful at setting path?
			start(speed, delay);
			return true;
		}
		frameTime = 0;			// Not moving.
		return false;
		}
	public boolean walkPathToTile(Tile dest, int speed, int delay, int dist) {
		getTile(walkSrc);
		return walkPathToTile(walkSrc, dest, speed, delay, dist, 3);
	}
	public boolean walkPathToTile(Tile src, Tile dest, int speed, int delay) {
		return walkPathToTile(src, dest, speed, delay, 0, 3);
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
			int new_lift = gmap.spotAvailable(ztiles, 
					t.tx, t.ty, t.tz, move_flags | getTypeFlags(), 1, -1);
			if (new_lift >= 0) {
				t.tz = (short)new_lift;
				return true;
			} else
				return false;
		}
		if (f == null) {
			f = new Tile();
			getTile(f);
		}
		return MapChunk.areaAvailable(xtiles, ytiles, ztiles,
				f, t, move_flags | getTypeFlags(), 1, -1);
	}
	//	Wait for actor to stop walking, or 'maxticks' if maxticks > 0.
    public void waitForArrival(Tile dest, int maxticks) {
    	ActorAction origAction = getAction();
    	boolean timeout = false;
    	int stopTime = TimeQueue.ticks + maxticks;
    	while (isMoving() && getAction() == origAction && !timeout &&
    			(getTileX() != dest.tx || getTileY() != dest.ty ||
    			 getLift() != dest.tz)) {
    		try {
    			Thread.sleep(200);
    		} catch (InterruptedException e) { break; }
    		if (maxticks > 0 && TimeQueue.ticks > stopTime)
    			timeout = true;
    	}		
    }
	/*
	 *	Read in actor from a given file.
	 */
	public void read
		(
		InputStream out,		// 'npc.dat', generally.
		int num,			// NPC #, or -1.
		boolean has_usecode		// 1 if a 'type1' NPC.
		) throws IOException {
		npcNum = (short) num;

		// This is used to get around parts of the files that we don't know
		// what the uses are. 'fix_first' is used fix issues in the originals
		// files that cause problems for the extra info we save.
		boolean fix_first = game.isNewGame();
		if (num == 0) System.out.println("isNewGame = " + fix_first);		
		init();				// Clear rest of stuff.
		int locx = out.read()&0xff;	// Get chunk/tile coords.
		int locy = out.read()&0xff;
							// Read & set shape #, frame #.
		int shnum = EUtil.Read2(out)&0xffff;
		if (num == 0 && !game.isBG() && (shnum & 0x3ff) < 12)
			setShape((shnum & 0x3ff) | 0x400);
		else
			setShape(shnum & 0x3ff);

		setFrame(shnum >> 10);
			
		int iflag1 = EUtil.Read2(out);	// Inventory flag.
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
		int schunk = out.read();	// Superchunk #.
							// For multi-map:1
		int map_num = out.read();
		//System.out.println("Npc #" + num + ", shape = " + getShapeNum() + ", map = " + map_num);
		if (fix_first)
			map_num = 0;
		GameMap npcmap = gwin.getMap(map_num);
		int usefun = EUtil.Read2(out);	// Get usecode function #.
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
		int health_val = out.read();
		setProperty(Actor.health, health_val);
		out.skip(3);	// Skip 3 bytes.
		int iflag2 = EUtil.Read2(out);	// The 'used-in-game' flag.
		if (iflag2 == 0 && num >= 0 /* ++++ && !fix_unused */) {
			if (num == 0)		// Old (bad) savegame?
				/* +++++ fix_unused = true; */;
			else
				unused = true;
		}
		boolean has_contents = fix_first ? (iflag1 != 0 && !unused) : (iflag1&1) != 0;
		// Read first set of flags
		int rflags = EUtil.Read2(out);
			
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
		int strength_val = out.read();

		if (game.isBG()) {
			setProperty(Actor.strength, strength_val & 0x3F);

			if (num == 0) {
				if (!extended_skin) {	// We will do it later for extended skins.
					/*+++++++++++
					if (Game.get_avskin() >= 0)
						setSkinColor (Game.get_avskin());
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
					if (Game.get_avskin() >= 0 && Game.get_avskin() <= 2)
						set_skin_color (Game.get_avskin());
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
		setProperty(Actor.dexterity, out.read());
		// Intelligence (0-4), read(5), Tournament (6), polymorph (7)
		int intel_val = out.read();

		setProperty(Actor.intelligence, intel_val & 0x1F);
		if (((intel_val >> 5) & 1) != 0) 
			setFlag (GameObject.read);
							// Tournament.
		if (((intel_val >> 6) & 1) != 0)
			setFlag (GameObject.tournament);
		if (((intel_val >> 7) & 1) != 0) 
			setFlag (GameObject.polymorph);

			// Combat skill (0-6), Petra (7)
		int combat_val = out.read();

		setProperty(Actor.combat, combat_val & 0x7F);
		if (((combat_val << 7) & 1) != 0) 
			setFlag (GameObject.petra);
		scheduleType = (byte) out.read();
		int amode = out.read();	// Default attack mode
							// Just stealing 2 spare bits:
		combatProtected = (amode&(1<<4)) != 0;
		userSetAttack = (amode&(1<<5)) != 0;
		attackMode = (amode&0xf);

		out.skip(1); 		// Unknown
		int unk0 = out.read();	// We set high bit of this value.
		int unk1 = out.read();
		int magic = 0, mana= 0, temp, flags3, ident = 0;
		if (fix_first || unk0 == 0) {	// How U7 stored things.
			// If NPC 0: MaxMagic (0-4), TempHigh (5-7) and Mana(0-4), 
			//						TempLow (5-7)
			// Else: ID# (0-4), TempHigh (5-7) and Met (0), 
			//	No Spell Casting (1), Zombie (2), TempLow (5-7)
			int magic_val = out.read();
			int mana_val = out.read();
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
			temp = out.read();
			flags3 = out.read();
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

		faceNum = (short)EUtil.Read2(out);	// NOTE:  Exult's using 2 bytes,
		if (fix_first)	// Not used in the original.
			//faceNum &= 0xff;	// Just 1 byte in orig.
			faceNum = npcNum;
		else if (faceNum == 0 && npcNum > 0)	// Fix older savegames.
			faceNum = npcNum;
		out.skip(1);	// Unknown

		setProperty(Actor.exp, EUtil.Read4(out));
		setProperty(Actor.training, out.read());

		out.skip (2);	// Primary Attacker
		out.skip (2);	// Secondary Attacker
		oppressor = (short)EUtil.Read2(out);	// Oppressor NPC id.
		out.skip (4);	//I-Vr ??? (refer to U7tech.txt)
		if (scheduleLoc == null)
			scheduleLoc = new Tile();
		scheduleLoc.tx = (short)EUtil.Read2(out);	//S-Vr Where npc is supposed to 
		scheduleLoc.ty = (short)EUtil.Read2(out);	//be for schedule)
		// Type flags 2
		int tflags = EUtil.Read2(out);
		// First time round, all the flags are garbage
		if (fix_first) {
			setTypeFlags (1 << tf_walk);
				// Correct for SI, no problems for BG:
			if ((tflags & (1 << tf_sex)) != 0)
				clearTypeFlag (tf_sex);
			else
				setTypeFlag (tf_sex);
		} else
			setTypeFlags (tflags);
		/* ++++++++FINISH
		if (num == 0 && Game.get_avsex() == 0) {
			clearTypeFlag (Actor.tf_sex);
		} else if (num == 0 && Game.get_avsex() == 1) {
			setTypeFlag (Actor.tf_sex);
		}
		*/
		out.skip (5);	// Unknown
		nextSchedule = (byte)out.read();	// Acty ????? what is this??
		out.skip (1);	// SN ????? (refer to U7tech.txt)
		out.skip (2);	// V1 ????? (refer to U7tech.txt)
		out.skip (2);	// V2 ????? (refer to U7tech.txt)

		// 16 Bit Shape Numbers, allows for shapes > 1023
		shnum = EUtil.Read2(out);
		if (!fix_first && shnum != 0) {
				// ++Testing
			if (npcNum == 0)
				setActorShape();
			else
				setShape(shnum);		// 16 Bit Shape Number

			shnum = EUtil.Read2(out);	// 16 Bit Polymorph Shape Number
			if (getFlag (GameObject.polymorph)) {
						// Try to fix messed-up flag.
				if (shnum != getShapeNum())
					; /* +++++ finish set_polymorph(shnum); */
				else
					clearFlag(GameObject.polymorph);
			}
		} else {
			out.skip (2);
			// +++++++++ set_polymorph_default();
		}

		// More Exult stuff
		if (!fix_first) {
			int	f;
			// Flags
			f = EUtil.Read4(out);
			flags |= f;
			if (getFlag(GameObject.invisible))	// Force timer.	
				needTimers().startInvisibility();
			// SIFlags -- no longer used.
			out.skip (2);
			// Flags2	But don't set polymorph.
			boolean polym = getFlag(GameObject.polymorph)!= false;
			f = EUtil.Read4(out);
			flags2 |= f;
			if (!polym && getFlag(GameObject.polymorph))
				clearFlag(GameObject.polymorph);
			/*
			if (usecode_name_used) {	// Support for named functions.
				int funsize = out.read();
				char *nm = new char[funsize+1];
				out.read(nm, funsize);
				nm[funsize] = 0;
				usecode_name = nm;
				usecode = ucmachine.find_function(nm);
				delete [] nm;
			}
			*/
			
			/* ++++FINISH int skin = */ out.read();
			/*++++++++++++++++
			if (extended_skin) {
				if (Game.get_avskin() >= 0)
					set_skin_color (Game.get_avskin());
				else
					set_skin_color(skin);
			}
			*/
		} else {
			// Flags
			out.skip (4);
			// SIFlags
			out.skip (2);
			// Flags2 
			out.skip (4);
			// Extended skins
			out.skip (1);
		}
		// Skip 14
		out.skip (14);
						// Get (signed) food level.
		int food_read = out.read();
		if (fix_first) food_read = 18;
		setProperty(Actor.food_level, food_read);
		// Skip 7
		out.skip(7);
		byte namebuf[] = new byte[16];
		out.read(namebuf);
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
		if (num == 0 && Game.get_avname()) {
			name = Game.get_avname();
		} else
		*/
			name = new String(namebuf, 0, len);		// Store copy of it.

							// Get abs. chunk. coords. of schunk.
		int scy = 16*(schunk/12);
		int scx = 16*(schunk%12);
		if (has_contents)		// Inventory?  Read.
			npcmap.readIregObjects(out, scx, scy, this, 0);
		if (read_sched_usecode)		// Read in scheduled usecode.
			npcmap.readSpecialIreg(out, this);
		int cx = locx >> 4;		// Get chunk indices within schunk.
		int cy = locy >> 4;
		
							// Get tile #'s.
		int tilex = locx & 0xf;
		int tiley = locy & 0xf;
		// if (num == 0)//+++++++DEBUG
			 //System.out.printf("Reading npc# %1$d, %2$s, locx = %3$d, cx = %4$d, tilex = %5$d\n",
			//		num, getName(), locx, cx, tilex);
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
		// We do this here because we need the NPC's final shape.
		if (health_val <= 0 && !unused) {
			// If a monster can't die, force it to have at least 1 hit point,
			// but only if the monster is used.
			// Maybe we should restore it to full health?
			MonsterInfo minf = getInfo().getMonsterInfo();
			if (minf != null && minf.cantDie())
				setProperty(Actor.health, getProperty(Actor.strength));
		}
		// Only do ready best weapon if we are in BG, this is the first time
		// and we are the Avatar or Iolo
		if (game.isBG() && game.getAvName() != null && (num == 0 || num == 1))
			readyBestWeapon();				
	}
	public void writeIreg(OutputStream out) throws IOException {	// Don't write to IREG.
	}
	public final void write(OutputStream out) throws IOException {
		int i;
		byte buf4[] = new byte[4]; // Write coords., shape, frame.

		int old_shape = getShapeNum();	// Backup shape because we might change it
		setShape( getShapeReal() );	// Change the shape out non polymorph one
		int shapenum = getShapeNum(), framenum = getFrameNum();
		buf4[0] = (byte)(((getCx()%16) << 4) | getTx());
		buf4[1] = (byte)(((getCy()%16) << 4) | getTy());
		// ++++++Is this even needed anymore? We already save 16-bit shapes below.
		buf4[2] = (byte)(shapenum&0xff);
		buf4[3] = (byte)(((shapenum>>8)&3) | (framenum<<2));
		out.write(buf4);
		setShape( old_shape );		// Revert the shape to what it was

						// Inventory flag.
						// Bit0 = has_contents (our use).
						// Bit1 = our savegame, with sched.
						//   usecode script following this.
						// iflag1:2 == usecode # assigned by
						//   ES, so always use it.
						// iflag1:3 == usecode fun name assigned
						//   by ES, so use it instead.
						// iflag1:4 == Extended skin number
		int iflag1 = objects.isEmpty() ? 0 : 1;
		iflag1 |= 2;			// We're always doing write_scheduled()
		if (usecodeAssigned) {	// # assigned by EStudio?
			// If we have a name, use it instead of the usecode #.
			if (usecodeName.length() != 0)
				iflag1 |= 8;		// Set bit 3.
			else
				iflag1 |= 4;		// Set bit 2.
		}
		iflag1 |= 16;	// Set bit 4.

		EUtil.Write2(out, iflag1);
				// Superchunk #.
		out.write((getCy()/16)*12 + getCx()/16);

		int map_num = getMapNum();
		if (map_num < 0)
			// we store all off-map actors in map 0
			map_num = 0;
		assert(map_num >= 0 && map_num < 256);
		out.write(map_num);		// Borrowing for map #.
						// Usecode.
		int usefun = getUsecode() & 0xfff;
						// Lift is in high 4 bits.
		usefun |= ((getLift()&15) << 12);
		EUtil.Write2(out, usefun);
		out.write(getProperty(Actor.health));
		out.write(0);			// Unknown 3 bytes.
		EUtil.Write2(out, 0);
		EUtil.Write2(out, unused ? 0 : 1);	// Write 0 if unused.

		//Write first set of flags
		int iout = 0;
		
		if (getFlag (GameObject.asleep)) iout |= 1 << 0x7;
		if (getFlag (GameObject.charmed)) iout |= 1 << 0x8;
		if (getFlag (GameObject.cursed)) iout |= 1 << 0x9;
		if (getFlag (GameObject.in_party)) iout |= 1 << 0xB;
		if (getFlag (GameObject.paralyzed)) iout |= 1 << 0xC;
		if (getFlag (GameObject.poisoned)) iout |= 1 << 0xD;
		if (getFlag (GameObject.protection)) iout |= 1 << 0xE;

		// Guess
		if (getFlag (GameObject.on_moving_barge)) iout |= 1 << 0xA;
						// Alignment is bits 3-4.

		// Unknownm using for is_temp
		if (getFlag (GameObject.is_temporary)) iout |= 1 << 0x6;

		iout |= ((alignment&3) << 3);
		EUtil.Write2(out, iout);
						// Write char. attributes.
		iout = getProperty(Actor.strength);
		/* ++++++++FINISH
		if (!game.isBG() && npcNum == 0) 
			iout |= (getSkinColor () & 3) << 5;
		else if (npcNum == 0) iout |= ((getSkinColor()+1) & 3) << 6;
		*/
		if (getFlag(GameObject.freeze)) 
			iout |= 1 << 7;
		out.write(iout);
		out.write(getProperty(Actor.dexterity));
		
		iout = getProperty(Actor.intelligence);
		if (getFlag (GameObject.read)) iout |= 1 << 5;
						// Tournament
		if (getFlag (GameObject.tournament)) iout |= 1 << 6;
		if (getFlag (GameObject.polymorph)) iout |= 1 << 7;
		out.write(iout);

		iout = getProperty(Actor.combat);
		if (getFlag (GameObject.petra)) iout |= 1 << 7;
		out.write(iout);
		
		out.write(getScheduleType());
		int amode = attackMode | 
			(combatProtected ? (1<<4) : 0) |
			(userSetAttack ? (1<<5) : 0);
		out.write((byte)amode);
		out.write(0);		// Skip 1.
		// Exult is using the 2 unknown bytes to store magic, mana for all
		//   NPC's, and to store temperature more simply.
		int magic = getProperty(Actor.magic);
		out.write(magic|0x80);	// Set high bit to flag new format.
		out.write(getProperty(Actor.mana));
		out.write(getTemperature());
		int flags3 = 0;
		if (getFlag (GameObject.met)) flags3 |= 1;
		if (getFlag (GameObject.no_spell_casting)) flags3 |= 1 << 1;
		if (getFlag (GameObject.si_zombie)) flags3 |= 1 << 2;
		flags3 |= (get_ident()<<3);
		out.write(flags3);

		EUtil.Write2(out, faceNum);
		out.write(0);		// Skip 2

		EUtil.Write4(out, getProperty(Actor.exp));
		out.write(getProperty(Actor.training));
				// 0x40 unknown.

		EUtil.Write2(out, 0);	// Skip 2*2
		EUtil.Write2(out, 0);
		EUtil.Write2(out, oppressor);	// Oppressor.

		EUtil.Write4(out, 0);	// Skip 2*2
		
		EUtil.Write2(out, scheduleLoc.tx);	//S-Vr Where npc is supposed to 
		EUtil.Write2(out, scheduleLoc.ty);	//be for schedule)
		//EUtil.Write4(out, 0);

		EUtil.Write2(out, getTypeFlags());	// Typeflags
		
		EUtil.Write4(out, 0);	// Skip 5
		out.write(0);

		out.write(nextSchedule);	// Acty ????? what is this??

		out.write(0);		// Skip 1
		EUtil.Write2(out, 0);	// Skip 2
		EUtil.Write2(out, 0);	// Skip 2

		// 16 Bit Shapes
		if (getFlag (GameObject.polymorph)) {
			EUtil.Write2(out, shapeSave);	// 16 Bit Shape Num
			EUtil.Write2(out, getShapeNum());	// 16 Bit Polymorph Shape
		} else {
			EUtil.Write2(out, getShapeNum());	// 16 Bit Shape Num
			EUtil.Write2(out, 0);		// 16 Bit Polymorph Shape
		}

		// Flags
		EUtil.Write4(out, flags);

		// SIFlags 
		EUtil.Write2(out, 0);

		// flags2
		EUtil.Write4(out, flags2);

		if (usecodeAssigned && usecodeName.length() > 0) {
			// Support for named functions.
			int size = usecodeName.length();
			out.write(size);
			byte nm[] = usecodeName.getBytes();
			out.write(nm);
		}

		out.write(0 /* ++++++FINISH (byte)getSkinColor()*/);
		// Skip 14
		for (i = 0; i < 14; i++)
			out.write(0);
		// Food
		out.write(getProperty (Actor.food_level));
		// Skip 7
		for (i = 0; i < 7; i++)
			out.write(0);
		// Write 16 byte name.
		byte namestr[] = name.length() == 0 ? super.getName().getBytes() : name.getBytes();
		int namelen = Math.min(16, namestr.length);
		out.write(namestr, 0, namelen);
		for ( ; namelen < 16; ++namelen)
			out.write(0);
		writeContents(out);		// Write what he holds.
		/* ++++++++++FINISH
		if (atts != null) {
			Actor.Atts_vector attlist;
			get_attributes(attlist);
			GameMap.writeAttributes(out, attlist);
		}
		*/
						// Write scheduled usecode.
		GameMap.writeScheduled(out, this, true);
	}
	public void writeContents(OutputStream out) throws IOException {
		if (!objects.isEmpty()) {	// Now write out what's inside.
			int num_spots = spots.length;
			int i;
			for (i = 0; i < num_spots; ++i) {
				// Spot Increment
				if (spots[i] != null) {
					// Write 2 byte index id
					out.write(0x02);
					EUtil.Write2(out, i);
					spots[i].writeIreg(out);
				}
			}
			GameObject obj;
			ObjectList.ObjectIterator iter = new ObjectList.ObjectIterator(objects);
			while ((obj = iter.next()) != null) {
				for (i = 0; i < num_spots; ++i)
					if (spots[i] == obj)
						break;
				if (i == num_spots) {
					// Write 2 byte index id (-1 = no slot)
					i = -1;
					out.write(0x02);
					EUtil.Write2(out, i);
					obj.writeIreg(out);
				}
			}
			out.write(0x01);		// A 01 terminates the list.
		}
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
	/*
	 *	An actor's dead body:
	 */
	public static class DeadBody extends ContainerGameObject {
		private int npcNum;			// # of NPC it came from, or -1.
		public DeadBody(int shapenum, int framenum, int tilex, 
				int tiley, int lft, int n) {
			super(shapenum, framenum, tilex, tiley, lft, 0);
			npcNum = n;
		}
		@Override
		public int getLiveNpcNum() {
			return npcNum;
		}
		@Override				// Under attack.
		public GameObject attacked(GameObject attacker, int weapon_shape,
						int ammo_shape, boolean explosion)
			{ return this; }	// Not affected.
		@Override
		public void writeIreg(OutputStream out) throws IOException {
			byte buf[] = new byte[21];		// 13-byte entry - Exult extension.
			int ind = writeCommonIreg(13, buf);
			GameObject first = objects.getFirst(); // Guessing:
			short tword = (short)(first != null ? first.getPrev().getShapeNum() : 0);
			EUtil.Write2(buf, ind, tword);
			ind += 2;
			buf[ind++] = 0;			// Unknown.
			buf[ind++] = (byte)getQuality();
			int npc = getLiveNpcNum();	// If body, get source.
				// Here, store NPC # more simply.
			EUtil.Write2(buf, ind, npc);	// Allowing larger range of NPC bodies.
			ind += 2;
			buf[ind++] = (byte)((getLift()&15)<<4);	// Lift 
			buf[ind++] = (byte)getObjHp();		// Resistance.
							// Flags:  B0=invis. B3=okay_to_take.
			buf[ind++] = (byte)((getFlag(GameObject.invisible)?1:0) +
							((getFlag(GameObject.okay_to_take)?1:0) << 3));
			out.write(buf, 0, ind);
			writeContents(out);		// Write what's contained within.
							// Write scheduled usecode.
			GameMap.writeScheduled(out, this, false);	
		}
		@Override
		public int getIregSize() {
			int size = super.getIregSize();
			return size < 0 ? size : size + 1;
		}
	}
}

