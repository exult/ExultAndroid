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
	private static final int dexToAttack = 30;
	protected static int battleTime;// Time when battle started.
	protected static int battleEndTime;	// And when it ended.
	protected int state;
	protected int prevSchedule;	// Before going into combat.
	protected Vector<Actor> opponents;	// Possible opponents.
	protected Vector<GameObject> nearby;			// For searching for opponents.
	protected Rectangle winRect;			// Temp.
	protected Tile npcPos, oppPos;			// Temp.
	protected GameObject ammoTemp[];		// Temp.
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
	 *	Can a given shape teleport? summon?  turn invisible?
	 */
	private static boolean canTeleport(Actor npc) {
		if (npc.getFlag(GameObject.no_spell_casting))
			return false;
		return npc.getInfo().canTeleport();
	}
	private static boolean Can_summon(Actor npc) {
		if (npc.getFlag(GameObject.no_spell_casting))
			return false;
		return npc.getInfo().canSummon();
	}
	private static boolean Can_be_invisible(Actor npc) {
		if (npc.getFlag(GameObject.no_spell_casting))
			return false;
		return npc.getInfo().canBeInvisible();
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
	/*
	 *	Handle the 'approach' state.
	 */
	protected void approachFoe
		(
		boolean for_projectile	// Want to attack with projectile.
								// FOR NOW:  Called as last resort,
								//  and we try to reach target.
		) {
		int points = 0;
		WeaponInfo winf = null;
		weapon = npc.getWeapon();
		if (weapon != null) {
			weapon_shape = weapon.getShapeNum();
			winf = weapon.getInfo().getWeaponInfo();
			if (winf != null)
				points = winf.getDamage();
		} else
			weapon_shape = -1;
		int dist = for_projectile ? 1 : winf != null ? winf.getRange() : 3;
		GameObject opponent = npc.getTarget();
						// Find opponent.
		if (opponent == null && (opponent = findFoe()) == null) {
			failures++;
			npc.start(1, 2);	// Try again in 2/5 sec.
			return;			// No one left to fight.
		}
		npc.setTarget(opponent);
		int mode = npc.getAttackMode();
						// Time to run?
		MonsterInfo minf = npc.getInfo().getMonsterInfo();
		if ((minf == null || !minf.cantDie()) &&
			(mode == Actor.flee || 
				(mode != Actor.berserk && 
					(npc.getTypeFlags()&EConst.MOVE_ALL) != 0 &&
				npc != gwin.getMainActor() &&
							npc.getProperty(Actor.health) < 3))) {
			runAway();
			return;
		}
		if (EUtil.rand()%4 == 0 && canTeleport(npc) &&	// Try 1/4 to teleport.
		    teleport()) {
			startBattle();
			npc.start(1, 1);
			return;
		}
		PathFinder path = new AStarPathFinder();
						// Try this for now:
		PathFinder.MonsterClient cost = new PathFinder.MonsterClient(
										npc, dist, opponent);
		npc.getTile(npcPos);
		opponent.getTile(oppPos);
		if (!path.NewPath(npcPos, oppPos, cost)) {
			// Failed?  Try nearest opponent.
			failures++;
			boolean retry_ok = false;
			if (npc.getAttackMode() != Actor.manual) {
				GameObject closest = findFoe(Actor.nearest);
				if (closest == null) {	// No one nearby.
					if (combatTrace)
						System.out.println(npc.getName() + 
								" has no opponents nearby.");
					npc.setTarget(null);
					retry_ok = false;
				} else if (closest != opponent) {
					opponent = closest;
					opponent.getTile(oppPos);
					npc.setTarget(opponent);
					cost = new PathFinder.MonsterClient(npc, dist, opponent);
					retry_ok = (opponent != null && path.NewPath(
						npcPos, oppPos, cost));
				}
			}
			if (!retry_ok) {
						// Really failed.  Try again in 
						//  after wandering.
						// Just try to walk towards opponent.
				npc.getTile(npcPos);
				int toposx = opponent.getTileX(), toposy = opponent.getTileY();
				int dirx = toposx > npcPos.tx ? 2
					: (toposx < npcPos.tx ? -2 : (EUtil.rand()%3 - 1));
				int diry = toposy > npcPos.ty ? 2
					: (toposy < npcPos.ty ? -2 : (EUtil.rand()%3 - 1));
				npcPos.tx += dirx * (1 + EUtil.rand()%4);
				npcPos.ty += diry * (1 + EUtil.rand()%4);
				npc.walkToTile(npcPos, 2, 2 + EUtil.rand()%3);
				failures++;
				return;
			}
		}
		failures = 0;			// Clear count.  We succeeded.
		startBattle();			// Music if first time.
		if (combatTrace) {
			System.out.println(npc.getName() + " is pursuing " + opponent.getName());
		}
						// First time (or 256th), visible?
		if (!yelled && gwin.addDirty(npc)) {
			yelled = true;
			if (canYell && EUtil.rand()%2 != 0) { // Half the time.
						// Goblin?
				if (game.isSI() &&
					 (npc.getShapeNum() == 0x1de ||
					  npc.getShapeNum() == 0x2b3 ||
					  npc.getShapeNum() == 0x2d5 ||
					  npc.getShapeNum() == 0x2e8))
					npc.say(0x4c9, 0x4d1);
		    	else
					npc.say(ItemNames.first_to_battle, ItemNames.last_to_battle);
			}
		}
		int extra_delay = 0;
						// Walk there, & check half-way.
		npc.setAction(new ActorAction.Approach(path, opponent, -1,
								for_projectile));
						// Start walking.  Delay a bit if
						//   opponent is off-screen.
		npc.start(1, extra_delay + (offScreen(opponent) ? 5 : 1));
	}
	/*
	 *	Check for a useful weapon at a given ready-spot.
	 */
	private GameObject getUsableWeapon
		(
		Actor npc,
		int index			// Ready-spot to check.
		)
		{
		GameObject bobj = npc.getReadied(index);
		if (bobj == null)
			return null;
		ShapeInfo info = bobj.getInfo();
		WeaponInfo winf = info.getWeaponInfo();
		if (winf == null)
			return null;		// Not a weapon.
		// Check ranged first.
		int need_ammo = npc.getWeaponAmmo(bobj.getShapeNum(),
				winf.getAmmoConsumed(), winf.getProjectile(),
				true, ammoTemp, false);
		if (need_ammo != 0) {
			if (ammoTemp[0] == null)	// Try melee.
				need_ammo = npc.getWeaponAmmo(bobj.getShapeNum(),
						winf.getAmmoConsumed(), winf.getProjectile(),
						false, ammoTemp, false);
			if (need_ammo != 0 && ammoTemp[0] == null)
				return null;
		}
		if (info.getReadyType() == Ready.both_hands &&
			npc.getReadied(Ready.rhand) != null)
			return null;		// Needs two free hands.
		return bobj;
	}
	// Note: winf = null for monsters or natural weaponry.
	private static boolean notInMeleeRange(WeaponInfo winf, int dist, int reach) {
		if (winf == null)
			return dist > reach;
		return (winf.getUses() == WeaponInfo.ranged) || (dist > reach);
	}
	/*
	 *	Swap weapon with the one in the belt.
	 *
	 *	Output:	1 if successful.
	 */
	private boolean swapWeapons(Actor npc) {
		int index = Ready.belt;
		GameObject bobj = getUsableWeapon(npc, index);
		if (bobj == null) {
			index = Ready.back_2h;
			bobj = getUsableWeapon(npc, index);
			if (bobj == null) {		// Do thorough search for NPC's.
				if (!npc.getFlag(GameObject.in_party))
					return npc.readyBestWeapon();
				else
					return false;
			}
		}
		GameObject oldweap = npc.getReadied(Ready.lhand);
		if (oldweap != null)
			npc.remove(oldweap);
		npc.remove(bobj);
		npc.add(bobj, true);		// Should go into weapon hand.
		if (oldweap != null)			// Put old where new one was.
			npc.addReadied(oldweap, index, true, true, false);	
		return true;
	}
	/*
	 *	Begin a strike at the opponent.
	 */
	public void startStrike(){
		GameObject opponent = npc.getTarget();
		boolean check_lof = !noBlocking;
						// Get difference in lift.
		WeaponInfo winf = weapon_shape >= 0 ?
				ShapeID.getInfo(weapon_shape).getWeaponInfo() : null;
		int dist = npc.distance(opponent);
		int reach;
		if (winf == null) {
			MonsterInfo minf = npc.getInfo().getMonsterInfo();
			reach = minf != null ? minf.getReach() 
								: MonsterInfo.getDefault().getReach();
		} else
			reach = winf.getRange();
		boolean ranged = notInMeleeRange(winf, dist, reach);
			// Out of range?
		if (spellbook == null && npc.getEffectiveRange(winf, reach) < dist) {
			state = approach;
			approachFoe(false);		// Get a path.
			return;
		} else if (spellbook != null || ranged){
			boolean weapon_dead = false;
			if (spellbook != null)
				weapon_dead = !spellbook.canDoSpell(npc);
			else if (winf != null) {
						// See if we can fire spell/projectile.
				int need_ammo = npc.getWeaponAmmo(weapon_shape,
						winf.getAmmoConsumed(), winf.getProjectile(),
						ranged, ammoTemp, false);
				if (need_ammo != 0 && ammoTemp[0] == null && !npc.readyAmmo())
					weapon_dead = true;
			}
			if (weapon_dead) {		// Out of ammo/reagents/charges.
				if (npc.getScheduleType() != Schedule.duel) {
						// Look in pack for ammo.
					if (swapWeapons(npc))
						setWeapon();
					else
						setHandToHand();
				}
				if (!npc.getInfo().hasStrangeMovement())
					npc.changeFrame(npc.getDirFramenum(
								Actor.standing));
				state = approach;
				npc.setTarget(null);
				npc.start(1, 3);
				return;
			}
			state = fire;		// Clear to go.
		} else {
			check_lof = (reach > 1);
			state = strike;
		}
		// At this point, we're within range, with state set.
		/*++++++++FINISH
		if (check_lof &&
		    !Fast_pathfinder_client::is_straight_path(npc, opponent)) {
			state = approach;
			approach_foe(true);	// Try to get adjacent.
			return;
		}
		*/
		if (!startedBattle)
			startBattle();	// Play music if first time.
		if (combatTrace) {
			System.out.println(npc.getName() + " attacks " + opponent.getName());
		}
		int dir = npc.getDirection(opponent);
		byte frames[] = new byte[12];
		//++++signed char frames[12];		// Get frames to show.
		int cnt = npc.getAttackFrames(weapon_shape, ranged, dir, frames);
		if (cnt != 0)
			npc.setAction(new ActorAction.Frames(frames, cnt, 1, null));
		npc.start(1, 0);			// Get back into time queue.
		int sfx = -1;			// Play sfx.
		if (winf != null)
			sfx = winf.getSfx();
		if (sfx < 0 || winf == null) {
			MonsterInfo minf = ShapeID.getInfo(
					npc.getShapeNum()).getMonsterInfo();
			if (minf != null)
				sfx = minf.getHitsfx();
		}
		if (sfx >= 0) {
			int delay = ranged ? cnt : cnt/2;
			//+++++++FINISH new Object_sfx(npc, sfx, delay);
		}
		dexPoints -= dexToAttack;
	}
	protected void runAway() {
		fleed++;
		// Might be nice to run from opp...
		int rx = EUtil.rand();		// Get random position away from here.
		int ry = EUtil.rand();
		int dirx = 2*(rx%2) - 1;	// Get 1 or -1.
		int diry = 2*(ry%2) - 1;
		npc.getTile(npcPos);
		npcPos.tx += dirx*(8 + rx%8);
		npcPos.ty += diry*(8 + ry%8);
		npc.walkToTile(npcPos, 1, 0);
		if (fleed == 1 && !npc.getFlag(GameObject.tournament) &&
						EUtil.rand()%3 != 0 && gwin.addDirty(npc)) {
			yelled = true;
			if (canYell)
				npc.say(ItemNames.first_flee, ItemNames.last_flee);
		}
	}
	/*
	 *	See if a spellbook is readied with a spell
	 *	available.
	 *
	 *	Output:	spellbook if so, else 0.
	 */
	protected SpellbookObject readiedSpellbook (){
		SpellbookObject book = null;
						// Check both hands.
		GameObject obj = npc.getReadied(Ready.lhand);
		if (obj != null && obj.getInfo().getShapeClass() == ShapeInfo.spellbook) {
			book = (SpellbookObject)(obj);
			if (book.canDoSpell(npc))
				return book;
		}
		obj = npc.getReadied(Ready.rhand);
		if (obj != null && obj.getInfo().getShapeClass() == ShapeInfo.spellbook) {
			book = (SpellbookObject)(obj);
			if (book.canDoSpell(npc))
				return book;
		}
		return null;
	}
	public CombatSchedule(Actor n, int prev_schedule) {
		super(n);
		//+++++++++FINISH
	}
	/*
	 *	Set weapon 'max_range' and 'ammo'.  Ready a new weapon if needed.
	 */
	public final void setWeapon(boolean removed) {
		//++++++++++FINISH
	}
	public final void setWeapon() {
		setWeapon(false);
	}
	/*
	 *	Set for hand-to-hand combat (no weapon).
	 */
	public final void setHandToHand() {
		//++++++++++FINISH
	}
	@Override
	public void nowWhat() {
		// TODO Auto-generated method stub

	}

}
