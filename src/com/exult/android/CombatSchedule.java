package com.exult.android;
import com.exult.android.shapeinf.*;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

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
	protected Vector<Actor> opponents;	// Possible opponents.
	protected Vector<GameObject> nearby;			// For searching for opponents.
	protected Rectangle winRect;			// Temp.
	protected GameObject practiceTarget;	// Only for duel schedule.
	protected GameObject weapon;
	protected int weapon_shape;		// Weapon's shape in shapes.vga.
	protected SpellbookObject spellbook;	// If readied.
					// Ranges in tiles.  
					//   0 means not applicable.
	protected boolean noBlocking;		// Weapon/ammo goes through walls.
	protected boolean yelled;		// Yell when first opponent targeted.
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
	/*
	 *	Off-screen?
	 */

	private boolean offScreen(GameObject npc) {
						// See if off screen.
		gwin.getWinTileRect(winRect);
		winRect.enlarge(2);
		return (!winRect.hasPoint(npc.getTileX(), npc.getTileY()));
	}
	private static boolean isEnemy(int align, int other){
		switch (align) {
		case NpcActor.friendly:
			return other == NpcActor.hostile || other == NpcActor.unknown_align; 
		case NpcActor.hostile:
			return other == NpcActor.friendly || other == NpcActor.unknown_align; 
		case NpcActor.neutral:
			return false; 
		case NpcActor.unknown_align:
			return other == NpcActor.hostile || other == NpcActor.friendly; 
		}
		return true;	// This should never happen.
	}
	/*
	 *	Find nearby opponents in the 9 surrounding chunks.
	 */
	protected void findOpponents() {
		opponents.clear();
		nearby.clear();	// Get all nearby NPC's
		Actor avatar = gwin.getMainActor();
		npc.findNearbyActors(nearby, EConst.c_any_shapenum, 
										2*EConst.c_tiles_per_chunk);
		nearby.add(avatar);	// Incl. Avatar!
						// See if we're a party member.
		boolean in_party = npc.getFlag(GameObject.in_party) || npc == avatar;
		int npc_align = npc.getEffectiveAlignment();
		boolean attack_avatar = isEnemy(npc_align, NpcActor.friendly);
		MonsterInfo minf = npc.getInfo().getMonsterInfo();
		boolean see_invisible = minf != null ?
			(minf.getFlags() & (1<<MonsterInfo.see_invisible))!=0 : false;
		for (GameObject each : nearby) {
			Actor actor = (Actor) each;
			if (actor.isDead() || actor.getFlag(GameObject.asleep) ||
			    (!see_invisible && actor.getFlag(GameObject.invisible)))
				continue;	// Dead, sleeping or invisible.
			if (isEnemy(npc_align, actor.getEffectiveAlignment()))
				opponents.add(actor);
			else if (attack_avatar && actor == avatar)
				opponents.add(actor);
			else if (in_party) {	// Attacking party member?
				GameObject t = actor.getTarget();
				if (t == null)
					continue;
				if (t.getFlag(GameObject.in_party) || t == avatar)
					opponents.add(actor);
				int oppressor = actor.getOppressor();
				if (oppressor < 0)
					continue;
				Actor oppr = gwin.getNpc(oppressor);
				if (oppr.getFlag(GameObject.in_party) || oppr == avatar)
					opponents.add(actor);
			}
		}
						// None found?  Use Avatar's.
		if (opponents.isEmpty() && in_party) {
			GameObject opp = avatar.getTarget();
			Actor oppnpc = opp != null ? opp.asActor() : null;
			if (oppnpc != null && oppnpc != npc
					&& oppnpc.getScheduleType()==Schedule.combat)
				opponents.add(oppnpc);
		}
	}
	/*
	 *	Find 'protected' party member's attackers.
	 *
	 *	Output:	Index of attacker in 'opponents', or -1 if none.
	 */
	protected int findProtectedAttacker() {
		if (!npc.getFlag(GameObject.in_party))	// Not in party?
			return -1;
		Actor prot_actor = null;// Look through party.
		if (gwin.getMainActor().isCombatProtected())
			prot_actor = gwin.getMainActor();
		else {
			int cnt = partyman.getCount();
			for (int i = 0; prot_actor == null && i < cnt; ++i) {
				Actor n = gwin.getNpc(partyman.getMember(i));
				if (n.isCombatProtected())
					prot_actor = n;
			}
		}
		if (prot_actor == null)		// Not found?
			return -1;
						// Find closest attacker.
		int dist, best_dist = 4*EConst.c_tiles_per_chunk, ind = 0, best_ind = -1;
		ListIterator<Actor> iter = opponents.listIterator();
		while (iter.hasNext()) {
			Actor opp = iter.next();
			if (opp.getTarget() == prot_actor &&
			    (dist = npc.distance(opp)) < best_dist) {
				best_dist = dist;
				best_ind = ind;
			}
			++ind;
		}
		if (best_ind == -1)
			return -1;
		if (failures < 5 && yelled && EUtil.rand()%2 != 0 && npc != prot_actor)
			npc.say(ItemNames.first_will_help, ItemNames.last_will_help);
		return best_ind;
	}
	/*
	 *	Find a foe.
	 *
	 *	Output:	Opponent that was found.
	 */
	protected GameObject findFoe(int mode) {
		if (combatTrace) {
			System.out.println("'" + npc.getName() + "' is looking for a foe"); 
		}
		int new_align = npc.getEffectiveAlignment();
		if (new_align != alignment) {	// Alignment changed.
			opponents.clear();
			alignment = new_align;
		}
						// Remove any that died.
		ListIterator<Actor> it = opponents.listIterator();
		while (it.hasNext()) {
			Actor a = it.next();
			if (a.isDead())
				it.remove();
			}
		if (opponents.isEmpty()) {	// No more from last scan?
			findOpponents();	// Find all nearby.
			if (practiceTarget != null)	// For dueling.
				return practiceTarget;
		}
		int str, ind, new_opp_ind = -1;
		switch (mode) {
		case Actor.weakest:
			int least_str = 100, least_ind = -1;
			ind = 0;
			for (Actor opp : opponents) {
				str = opp.getProperty(Actor.strength);
				if (str < least_str) {
					least_str = str;
					new_opp_ind = ind;
				}
				++ind;
			}
			break;
		case Actor.strongest:
			int best_str = -100;
			ind = 0;
			for (Actor opp : opponents) {
				str = opp.getProperty(Actor.strength);
				if (str > best_str) {
					best_str = str;
					new_opp_ind = ind;
				}
				++ind;
			}
			break;
		case Actor.nearest:
			int best_dist = 4*EConst.c_tiles_per_chunk;
			ind = 0;
			for (Actor opp : opponents) {
				int dist = npc.distance(opp);
				if (opp.getAttackMode() == Actor.flee)
					dist += 16;	// Avoid fleeing.
				if (dist < best_dist) {
					best_dist = dist;
					new_opp_ind = ind;
				}
				++ind;
			}
			break;
		case Actor.protect:
			new_opp_ind = findProtectedAttacker();
			if (new_opp_ind != -1)
				break;		// Found one.
						// FALL THROUGH to 'random'.
		case Actor.random:
		default:			// Default to random.
			if (!opponents.isEmpty())
				new_opp_ind = 0;
			break;
		}
		Actor new_opponent;
		if (new_opp_ind == -1)
			new_opponent = null;
		else
			new_opponent = opponents.remove(new_opp_ind);
		return new_opponent;
	} 
	protected GameObject findFoe() {
	if (npc.getAttackMode() == Actor.manual)
		return null;		// Find it yourself.
	return findFoe(npc.getAttackMode());
	}
//+++++++++++++++\\
	public CombatSchedule(Actor n, int prev_schedule) {
		super(n);
		//+++++++++FINISH
	}
	@Override
	public void nowWhat() {
		// TODO Auto-generated method stub

	}

}
