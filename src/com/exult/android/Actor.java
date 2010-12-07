package com.exult.android;
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
	private Tile walkSrc = new Tile();
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
	protected byte scheduleType;	// Schedule type (Schedule_type).	
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
		if (prop == Actor::sex_flag)
			// Correct in BG and SI, but the flag is never normally set
			// for anyone but avatar in BG.
			return get_type_flag(Actor::tf_sex);
		else if (prop == Actor::missile_weapon)
			{
			// Seems to give the same results as in the originals.
			Game_object *weapon = get_readied(lhand);
			Weapon_info *winf = weapon ? weapon->get_info().get_weapon_info() : 0;
			if (!winf)
				return 0;
			return (winf->get_uses() >= 2);
			}
		*/
		return (prop >= 0 && prop < Actor.sex_flag) ? properties[prop] : 0;
	}
	public final void setProperty(int prop, int val) {
		//+++++++++FINISH
	}
	public void setFlag(int flag) {
		//++++FINISH
	}
	public final void set_type_flag(int flag) {
		//+++++++++FINISH
	}
	public void clear_flag(int flag) {
		// +++++++++FINISH
	}
	public void clear_type_flag(int flag) {
		//++++++++FINISH
	}
	public int get_type_flag(int flag) {
		return 0; //+++++++++FINISH
	}
	public void set_type_flags(int tflags) {
		//++++++++++FINISH
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
	public final boolean isDormant() {
		return dormant;
	}
	public void setAttribute(String nm, int val) {
		//++++++++++LATER
	}
	public int getAttribute(String nm) {
		return 0;	// +++++++LATER
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
	int get_temperature()	// Get/set measure of coldness.
		{ return temperature; }
	void set_temperature(int t) {
		//+++++++FINISH
	}
	public void set_actor_shape() { 	// Set shape based on sex, skin color
		//+++++FINISH
	}
	public boolean addDirty(boolean figureWeapon) {
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
		if (!shape || shape->is_empty())
			{		// Swap 1hand <=> 2hand frames.
			frnum = (frnum&48)|visible_frames[frnum&15];
			id.set_frame(frnum);
			if (!(shape = id.get_shape()) || shape->is_empty())
				frnum = (frnum&48)|Actor::standing;
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
	public boolean inUsecodeControl() {
		if (getFlag(GameObject.dont_render) || getFlag(GameObject.dont_move))
			return true;
		/* +++++++
		Usecode_script *scr = 0;
		Actor *act = const_cast<Actor *>(this);
		while ((scr = Usecode_script::find_active(act, scr)) != 0)
			// no_halt scripts seem not to prevent movement.
			if (!scr->is_no_halt())
				return true;
		*/
		return false;
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
		set_temperature (temp);
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
			set_type_flags (1 << tf_walk);
				// Correct for SI, no problems for BG:
			if ((tflags & (1 << tf_sex)) != 0)
				clear_type_flag (tf_sex);
			else
				set_type_flag (tf_sex);
		} else
			set_type_flags (tflags);
		/* ++++++++FINISH
		if (num == 0 && Game::get_avsex() == 0) {
			clear_type_flag (Actor::tf_sex);
		} else if (num == 0 && Game::get_avsex() == 1) {
			set_type_flag (Actor::tf_sex);
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
				set_actor_shape();
			else
				setShape(shnum);		// 16 Bit Shape Number

			shnum = EUtil.Read2(nfile);	// 16 Bit Polymorph Shape Number
			if (getFlag (GameObject.polymorph)) {
						// Try to fix messed-up flag.
				if (shnum != getShapeNum())
					; /* +++++ finish set_polymorph(shnum); */
				else
					clear_flag(GameObject.polymorph);
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
			if (get_flag(GameObject.invisible))	// Force timer.	
				need_timers()->start_invisibility();
			*/
			// SIFlags -- no longer used.
			nfile.skip (2);
			// Flags2	But don't set polymorph.
			boolean polym = getFlag(GameObject.polymorph)!= false;
			f = EUtil.Read4(nfile);
			flags2 |= f;
			if (!polym && getFlag(GameObject.polymorph))
				clear_flag(GameObject.polymorph);
			/*
			if (usecode_name_used) {	// Support for named functions.
				int funsize = nfile.read();
				char *nm = new char[funsize+1];
				nfile->read(nm, funsize);
				nm[funsize] = 0;
				usecode_name = nm;
				usecode = ucmachine->find_function(nm);
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
			if (minf && minf->cant_die())
				setProperty(Actor.static_cast<int>(Actor::health),
					get_property(static_cast<int>(Actor::strength)));
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
		public int findUnrotated(byte frame) {
			for (int i = frames.length - 1; i > 0; i--)
				if (((frame ^ frames[i])&0xf) == 0)
					return i;
			return 0;
		}
	}
}

