package com.exult.android;
import java.util.LinkedList;

public class CombatSchedule extends Schedule {
	//	Combat options:
	public static boolean combatTrace;
	private static boolean paused;		// For suspending.
	public static int difficulty;		// 0=normal, >0 harder, <0 easier.
	public static final int // enum Mode
			original = 0,		// All automatic,
			keypause = 1;		// Kbd (space) suspends/resumes.
	public static int mode = original;
	public static boolean showHits;		// Display #'s.
						// In game:
	/* MAYBE FINISH
	public static void togglePause();	// Pause/resume.
	public static void resume()		// Always resume.
	*/
	public static boolean isPaused()
			{ return paused; }
	/*
	 * The schedule data and methods.
	 */
	public static final int // enum Phase			// We'll be a finite-state-machine.
		initial = 0,		// Just constructed.
		approach = 1,		// Approaching a foe to attack.
		retreat = 2,		// Avoiding a foe.
		flee = 3,		// Run away!
		strike = 4,		// In the process of striking.
		fire = 5,		// In process of firing range weapon.
		parry = 6,		// In the process of parrying a blow.
		stunned = 7,		// Just been hit.
		wait_return = 8		// Wait for boomerang.
		;
	protected static int battleTime;// Time when battle started.
	protected static int battleEndTime;	// And when it ended.
	protected int state;
	protected int prevSchedule;	// Before going into combat.
	protected LinkedList<Actor> opponents;	// Possible opponents.
	protected GameObject practiceTarget;	// Only for duel schedule.
	protected GameObject weapon;
	protected int weapon_shape;		// Weapon's shape in shapes.vga.
	protected SpellbookObject spellbook;	// If readied.
					// Ranges in tiles.  
					//   0 means not applicable.
	protected boolean noBlocking;		// Weapon/ammo goes through walls.
	protected int yelled;		// Yell when first opponent targeted.
	protected boolean startedBattle;		// 1st opponent targeted.
	protected int fleed;		// Incremented when fleeing.
	protected boolean canYell;
	protected int failures;			// # failures to find opponent.
	protected int teleportTime;	// Next time we can teleport.
	protected int summonTime;
	protected int invisibleTime;
	protected int dexPoints;	// Need these to attack.
	protected int alignment;			// So we can tell if it changed.
	/*
	 *	Start music if battle has recently started.
	 */
	protected void startBattle (){
		if (startedBattle)
			return;
						// But only if Avatar is main char.
		if (gwin.getCameraActor() != gwin.getMainActor())
			return;
		int curtime = TimeQueue.ticks;
						// .5 minute since last start?
		if (curtime - battleTime >= 30000/TimeQueue.tickMsecs) {
			/* +++++FINISH
			audio.startMusicCombat(EUtil.rand()%2 ? 
						CSAttacked1 : CSAttacked2, 0);
			*/
			battleTime = curtime;
			battleEndTime = curtime - 1;
		}
		startedBattle = true;
	}
	/*
	 *	Certain monsters (wisps, mages) can teleport during battle.
	 */
	protected boolean teleport() {
		GameObject trg = npc.getTarget();	// Want to get close to targ.
		if (trg == null)
			return false;
		int curtime = TimeQueue.ticks;
		if (curtime < teleportTime)
			return false;
		teleportTime = curtime + (2000 + EUtil.rand()%2000)/TimeQueue.tickMsecs;
		Tile dest = new Tile();
		trg.getTile(dest);
		dest.tx += 4 - EUtil.rand()%8;
		dest.ty += 4 - EUtil.rand()%8;
		if (!MapChunk.findSpot(dest, 3, npc, 1, MapChunk.anywhere))
			return false;		// No spot found.
		Tile src = new Tile();
		npc.getTile(src);
		if (dest.distance(src) > 7 && EUtil.rand()%2 != 0)
			return false;		// Got to give Avatar a chance to
						//   get away.
						// Create fire-field where he was.
		src.tz = (short)npc.getChunk().getHighestBlocked(src.tz,
				src.tx%EConst.c_tiles_per_chunk, src.ty%EConst.c_tiles_per_chunk);
		if (src.tz < 0)
			src.tz = 0;
		//+++++ FINISH eman.add_effect(new FireFieldEffect(src));
		int sfx = audio.gameSfx(43);
		audio.playSfx(sfx /* ++++FINISH, npc */);	// The weird noise.
		npc.move(dest.tx, dest.ty, dest.tz);
						// Show the stars.
		eman.addEffect(new EffectsManager.SpritesEffect(7, npc, 0, 0, 0, 0, 0, -1));
		return true;
	}
	/*
	 *	Some monsters can do the "summon" spell.
	 */
	protected boolean summon() {
		ucmachine.callUsecode(0x685,
				    npc, UsecodeMachine.double_click);
		npc.start(1, 1);		// Back into queue.
		return true;
	}
	/*
	 *	Some can turn invisible.
	 */
	protected boolean beInvisible() {
		//+++++FINISH new ObjectSfx(npc, Audio.gameSfx(44));
		eman.addEffect(
			new EffectsManager.SpritesEffect(12, npc, 0, 0, 0, 0, 0, -1));
		npc.setFlag(GameObject.invisible);
		npc.addDirty();
		npc.start(1, 1);		// Back into queue.
		return true;
	}
//+++++++++++++++findOpponents
	public CombatSchedule(Actor n, int prev_schedule) {
		super(n);
		//+++++++++FINISH
	}
	@Override
	public void nowWhat() {
		// TODO Auto-generated method stub

	}

}
