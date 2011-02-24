package com.exult.android;
import com.exult.android.shapeinf.*;
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
	*/
	public static void resume() {	// Always resume.
		if (paused) {
			tqueue.resume(TimeQueue.ticks);
			paused = false;
		}
	}
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
	protected int weaponShape;		// Weapon's shape in shapes.vga.
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
			audio.startMusicCombat(EUtil.rand()%2 != 0 ? 
						Audio.CSAttacked1 : Audio.CSAttacked2, false);
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
	private static boolean canSummon(Actor npc) {
		if (npc.getFlag(GameObject.no_spell_casting))
			return false;
		return npc.getInfo().canSummon();
	}
	private static boolean canBeInvisible(Actor npc) {
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
		eman.addEffect(new EffectsManager.FireField(src));
		int sfx = Audio.gameSfx(43);
		audio.playSfx(sfx, npc);	// The weird noise.
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
		new Animator.ObjectSfx(npc, Audio.gameSfx(44), 0);
		eman.addEffect(
			new EffectsManager.SpritesEffect(12, npc, 0, 0, 0, 0, 0, -1));
		npc.setFlag(GameObject.invisible);
		npc.addDirty();
		npc.start(1, 1);		// Back into queue.
		return true;
	}
	/*
	 *	See if we need a new opponent.
	 */
	private boolean needNewOpponent(Actor npc) {
		GameObject opponent = npc.getTarget();
		Actor act;
		MonsterInfo minf = npc.getInfo().getMonsterInfo();
		boolean see_invisible = minf != null ?
			(minf.getFlags() & (1<<MonsterInfo.see_invisible))!=0 : false;
						// Nonexistent or dead?
		if (opponent == null || 
		    ((act = opponent.asActor()) != null && act.isDead()) ||
						// Or invisible?
		    (!see_invisible && opponent.getFlag(GameObject.invisible)
				&& EUtil.rand()%4 == 0))
			return true;
						// See if off screen.
		return offScreen(opponent) && !offScreen(npc);
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
			int least_str = 100;
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
	 * Get npc's weapon and set 'weapon', 'weaponShape'.
	 */
	private WeaponInfo getWeapon() {
		WeaponInfo winf;
		weapon = npc.getWeapon();
		if (weapon != null) {
			weaponShape = weapon.getShapeNum();
			winf = weapon.getInfo().getWeaponInfo();
		} else {
			weaponShape = -1;
			winf = null;
		}
		return winf;
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
		WeaponInfo winf = getWeapon();	// Set 'weapon', 'weaponShape'.
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
		WeaponInfo winf = weaponShape >= 0 ?
				ShapeID.getInfo(weaponShape).getWeaponInfo() : null;
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
				int need_ammo = npc.getWeaponAmmo(weaponShape,
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
		if (check_lof && !PathFinder.FastClient.isStraightPath(npc, opponent)) {
			state = approach;
			approachFoe(true);	// Try to get adjacent.
			return;
		}
		if (!startedBattle)
			startBattle();	// Play music if first time.
		if (combatTrace) {
			System.out.println(npc.getName() + " attacks " + opponent.getName());
		}
		int dir = npc.getDirection(opponent);
		byte frames[] = new byte[12];
		int cnt = npc.getAttackFrames(weaponShape, ranged, dir, frames);
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
			new Animator.ObjectSfx(npc, sfx, delay);
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
	public CombatSchedule(Actor n, int prev_sched) {
		super(n);
		state = initial;
		prevSchedule = prev_sched;
		weaponShape = -1;
		alignment = npc.getEffectiveAlignment();
		setWeapon();
		// Cache some data.
		MonsterInfo minf = npc.getInfo().getMonsterInfo();
		canYell = minf == null || !minf.cantYell();
		int curtime = TimeQueue.ticks;
		summonTime = curtime + 4000/TimeQueue.tickMsecs;
		invisibleTime = curtime + 4500/TimeQueue.tickMsecs;
		opponents = new Vector<Actor>();
		nearby = new Vector<GameObject>();
		winRect = new Rectangle();
		npcPos = new Tile(); oppPos = new Tile();
		ammoTemp = new GameObject[1];
	}
	/*
	 *	This (static) method is called when a monster dies.  It checks to
	 *	see if there are still hostile NPC's around.  If not, it plays
	 *	'victory' music.
	 */
	public static void monsterDied() {
		if (battleEndTime >= battleTime)// Battle raging?
			return;			// No, it's over.
		Vector<GameObject> nearby = new Vector<GameObject>();// Get all nearby NPC's.
		gwin.getMainActor().findNearbyActors(nearby, EConst.c_any_shapenum, 
				2*EConst.c_tiles_per_chunk);
		for (GameObject obj : nearby) {
			Actor actor = (Actor)obj;
			if (!actor.isDead() && 
				actor.getAttackMode() != Actor.flee &&
				actor.getEffectiveAlignment() >= NpcActor.hostile)
				return;		// Still possible enemies.
		}
		battleEndTime = TimeQueue.ticks;
						// Figure #seconds battle lasted.
		int len = ((battleEndTime - battleTime)*TimeQueue.tickMsecs)/1000;
		boolean hard = len > 15 && (EUtil.rand()%60 < len);
		audio.startMusicCombat (hard ? Audio.CSBattle_Over : Audio.CSVictory, 
				false);
	}
	/*
	 *	This (static) method is called to stop attacking a given NPC.
	 *	This can happen because the NPC died, fell asleep or became
	 *	invisible.
	 */
	public static void stopAttackingNpc(GameObject npc) {
		Vector<GameObject> nearby = new Vector<GameObject>();// Get all nearby NPC's.
		npc.findNearbyActors(nearby, EConst.c_any_shapenum, 
									2*EConst.c_tiles_per_chunk);
		for (GameObject obj : nearby) {
			Actor actor = (Actor) obj;
			if (actor.getTarget() == npc)
				actor.setTarget(null);	
		}		
	}
	/*
	 *	This (static) method is called to stop attacking a given NPC.
	 *	This can happen because the NPC died or fell asleep.
	 */
	public static void stopAttackingInvisible(GameObject npc) {
		Vector<GameObject> nearby = new Vector<GameObject>();// Get all nearby NPC's.
		npc.findNearbyActors(nearby, EConst.c_any_shapenum, 
											2*EConst.c_tiles_per_chunk);
		for (GameObject obj : nearby) {
			Actor actor = (Actor)obj;
			if (actor.getTarget() == npc) {
				MonsterInfo minf = actor.getInfo().getMonsterInfo();
				if (minf != null && 
						(minf.getFlags() & (1<<MonsterInfo.see_invisible))==0)
					actor.setTarget(null);
			}
		}
	}
	/*
	 *	Set weapon 'max_range' and 'ammo'.  Ready a new weapon if needed.
	 */
	@Override
	public void setWeapon(boolean removed) {
		WeaponInfo info = getWeapon();	// Set 'weapon', 'weaponShape'.
		if (info == null && !removed &&	// No weapon?
		    (spellbook = readiedSpellbook()) == null &&	// No spellbook?
						// Not dragging?
		    /* ++NEEDED? !gwin.isDragging() && */
						// And not dueling?
		    npc.getScheduleType() != Schedule.duel &&
		    state != wait_return) {	// And not waiting for boomerang.
			npc.readyBestWeapon();
			info = getWeapon();
		}
		if (info == null) {			// Still nothing.
			if (spellbook != null)		// Did we find a spellbook?
				noBlocking = true;
			else	// Don't do this if using spellbook.
				setHandToHand();
		} else {
			noBlocking = false;
		}
		state = approach;	// Got to restart attack.
	}
	public final void setWeapon() {
		setWeapon(false);
	}
	/*
	 *	Set for hand-to-hand combat (no weapon).
	 */
	public final void setHandToHand() {
		weapon = null;
		weaponShape = -1;
		noBlocking = false;
					// Put aside weapon.
		GameObject weapon = npc.getReadied(Ready.lhand);
		if (weapon != null) {
			npc.remove(weapon);
			if (!npc.addReadied(weapon, Ready.belt, true, false, false) &&
					!npc.addReadied(weapon, Ready.back_2h, true, false, false) &&
					!npc.addReadied(weapon, Ready.back_shield, true, false, false) &&
					!npc.addReadied(weapon, Ready.rhand, true, false, false) &&
					!npc.addReadied(weapon, Ready.backpack, true, false, false))
				npc.add(weapon, false, false, true);
		}
	}
	@Override
	public void nowWhat() {
		if (state == initial) {		// Do NOTHING in initial state so
						//   usecode can, e.g., set opponent.
						// Way far away (50 tiles)?
			if (npc.distance(gwin.getCameraActor()) > 50) {
				npc.setDormant();
				return;		// Just go dormant.
			}
			state = approach;
			npc.start(1, 1);
			return;
		}
		if (npc.getFlag(GameObject.asleep)) {
			npc.start(1, 5);	// Check again in a second.
			return;
		}
						// Running away?
		if (npc.getAttackMode() == Actor.flee) {
						// If not in combat, stop running.
			MonsterInfo minf = npc.getInfo().getMonsterInfo();
			if (minf != null && minf.cantDie())
				npc.setAttackMode(Actor.nearest, false);
			else if (fleed > 2 && !gwin.inCombat() && 
							npc.getPartyId() >= 0)
						// WARNING:  Destroys ourself.
				npc.setScheduleType(Schedule.follow_avatar);
			else
				runAway();
			return;
		}
						// Check if opponent still breathes.
		if (needNewOpponent(npc)) {
			npc.setTarget(null);
			state = approach;
		}
		GameObject opponent = npc.getTarget();
		switch (state) {			// Note:  state's action has finished.
		case approach:
			if (opponent == null)
				approachFoe(false);
			else if (dexPoints >= dexToAttack) {
				int effint = npc.getEffectiveProp(Actor.intelligence);
				if (!npc.getFlag(GameObject.invisible) &&
				    canBeInvisible(npc) && EUtil.rand()%300 < effint) {
					beInvisible();
					dexPoints -= dexToAttack;
				} else if (canSummon(npc) && EUtil.rand()%600 < effint && 
									summon())
					dexPoints -= dexToAttack;
				else
					startStrike();
			} else {
				dexPoints += npc.getProperty(Actor.dexterity);
				npc.start(1, 1);
			}
			break;
		case strike:			// He hasn't moved away?
			state = approach;
						// Back into queue.
			npc.start(1, 1);
			Actor safenpc = npc;
				// Change back to ready frame.
			byte frame[] = new byte[1];
			frame[0] = (byte)npc.getDirFramenum(Actor.ready_frame);
			npc.setAction(new ActorAction.Frames(frame, 1, 1, null));
			npc.start(1, 1);
			if (attackTarget(npc, opponent, null, 
								weaponShape, true, ammoTemp)) {
						// Strike but once at objects.
				GameObject newtarg = safenpc.getTarget();
				if (newtarg != null && newtarg.asActor() == null)
					safenpc.setTarget(null);
				return;		// We may no longer exist!
			}
			break;
		case fire:			// Range weapon.
			failures = 0;
			state = approach;
			if (spellbook != null) {		// Cast the spell.
				if (!spellbook.doSpell(npc, true))
					setWeapon();
			} else
				attackTarget(npc, opponent, null, weaponShape, true, ammoTemp);
			
			int delay = 1;
			if (spellbook != null) {
				UsecodeScript scr = UsecodeScript.find(npc);
				// Warning: assuming that the most recent script for the
				// actor is the spellcasting script.
				delay += (scr != null ? UsecodeScript.getCount() : 0) + 2;
			}
				// Change back to ready frame.
			byte frame2[] = new byte[1];
			frame2[0] = (byte) npc.getDirFramenum(Actor.ready_frame);
			npc.setAction(new ActorAction.Frames(frame2, 1, delay, null));
			npc.start(1, delay);

			// Strike but once at objects.
			GameObject newtarg = npc.getTarget();
			if (newtarg != null && newtarg.asActor() == null) {
				npc.setTarget(null);
				return;		// We may no longer exist!
			}
			break;
		case wait_return:		// Boomerang should have returned.
			state = approach;
			dexPoints += npc.getProperty(Actor.dexterity);
			npc.start(1, 1);
			break;
		default:
			break;
			}
		if (failures > 5 && npc != gwin.getCameraActor()) {
					// Too many failures.  Give up for now.
			if (combatTrace) {
				System.out.println(npc.getName() + " is giving up");
			}
			if (npc.getPartyId() >= 0) {		// Party member.
				gwin.getMainActor().getTile(npcPos);
				npc.walkToTile(npcPos, 1, 1);
						// WARNING:  Destroys ourself.
				npc.setScheduleType(Schedule.follow_avatar);
			} else if (offScreen(npc)) {
						// Off screen?  Stop trying.
				tqueue.remove(npc);
				npc.setDormant();
			} else if (npc.getAlignment() == NpcActor.friendly &&
					prevSchedule != Schedule.combat) {
						// Return to normal schedule.
				npc.updateSchedule(clock.getHour()/3, 7, -1);
				if (npc.getScheduleType() == Schedule.combat)
					npc.setScheduleType(prevSchedule);
			} else {		// Wander randomly.
				npc.getTile(npcPos);
				int dist = 2+EUtil.rand()%3;
				npcPos.tx = (short)(npcPos.tx - dist + EUtil.rand()%(2*dist));
				npcPos.ty = (short)(npcPos.ty - dist + EUtil.rand()%(2*dist));
						// Wait a bit.
				
				npc.walkToTile(npcPos, 2, (EUtil.rand()%1000)/TimeQueue.tickMsecs);
			}
		}
	}
	@Override	// Npc calls this when it goes dormant.
	public void imDormant() {
		if (npc.getEffectiveAlignment() == NpcActor.friendly && 
				prevSchedule != npc.getScheduleType() && 
									(npc instanceof MonsterActor))
							// Friendly, so end combat.
				npc.setScheduleType(prevSchedule);
	}
	@Override 	// Switching to another schedule.
	public void ending(int newtype) {
		if (gwin.getMainActor() == npc && 
				// Not if called from usecode.
				!ucmachine.inUsecode()) { // See if being a coward.
			findOpponents();
			boolean found = false;	// Find a close-by enemy.
			npc.getTile(npcPos);
			for (Actor opp : opponents) {
				if (opp.distance(npc) < (EConst.c_screen_tile_size/2 - 2) &&
					PathFinder.FastClient.isGrabable(npc, opp) ) {
				found = true;
				break;
				}
			}
		if (found)
			audio.startMusicCombat(Audio.CSRun_Away, false);
		}
	}
	public void setState(int s) {
		state = s;
	}
	/*
	 *	This static method causes the NPC to attack a given target/tile
	 *	using the given weapon shape as weapon. This does not add
	 *	an attack animation; rather, it is the actual strike attempt.
	 *
	 *	This function is called from (a) an intrinsic, (b) a script opcode
	 *	or (c) the combat schedule.
	 *
	 *	Output:	Returns false the attack cannot be realized (no ammo,
	 *	out of range, etc.) or if a melee attack misses, true otherwise.
	 */
	public static boolean attackTarget
		(
		GameObject attacker,		// Who/what is attacking.
		GameObject target,		// Who/what is being attacked.
		Tile tile,				// What tile is under fire, if no target.
		int weapon,					// What is being used as weapon.
									// or < 0 for none.
		boolean combat,				// We got here from combat schedule.
		GameObject ammoTemp[]		// Use this, or create if null.
		) {
		// Bail out if no attacker or if no target and no valid tile.
		if (attacker == null || (target == null && (tile == null || tile.tx == -1)))
			return false;

		// Do not proceed if target is dead.
		Actor att = attacker.asActor();
		if (att != null && att.isDead())
			return false;
		boolean flash_mouse = !combat && att != null && gwin.getMainActor() == att
				&& att.getAttackMode() != Actor.manual;
		ShapeInfo info = ShapeID.getInfo(weapon);
		WeaponInfo winf = weapon >= 0 ? info.getWeaponInfo() : null;

		int reach;
		int family = -1;	// Ammo, is needed, is the weapon itself.
		int proj = -1;	// This is what we will use as projectile sprite.
		if (winf == null) {
			MonsterInfo minf = attacker.getInfo().getMonsterInfo();
			reach = minf != null ? minf.getReach() 
					: MonsterInfo.getDefault().getReach();
		} else {
			reach = winf.getRange();
			proj = winf.getProjectile();
			family = winf.getAmmoConsumed();
		}
		int dist = target != null ? attacker.distance(target) 
								  : attacker.distance(tile);
		boolean ranged = notInMeleeRange(winf, dist, reach);
			// Out of range?
		if (attacker.getEffectiveRange(winf, reach) < dist) {
			// We are out of range.
			if (flash_mouse)
				Mouse.mouse.flashShape(Mouse.outofrange);
			return false;
		}
			// See if we need ammo.
		if (ammoTemp == null)
			ammoTemp = new GameObject[1];
		int need_ammo = attacker.getWeaponAmmo(weapon, family,
				proj, ranged, ammoTemp, false);
		GameObject ammo = ammoTemp[0];
		if (need_ammo != 0 && ammo == null) {
			if (flash_mouse)
				Mouse.mouse.flashShape(Mouse.outofammo);
			// We don't have ammo, so bail out.
			return false;
		}
			// proj == -3 means use weapon shape for projectile sprite.
		if (proj == -3)
			proj = weapon;
		AmmoInfo ainf;
		int basesprite;
		if (need_ammo != 0 && family >= 0) {
				// ammo should be nonzero here.
			ainf = ammo.getInfo().getAmmoInfo();
			basesprite = ammo.getShapeNum();
		} else {
			ainf = info.getAmmoInfo();
			basesprite = weapon;
		}
		if (ainf != null) {
			int sprite = ainf.getSpriteShape();
			if (sprite == -3)
				proj = basesprite;
			else if (sprite != -1 && sprite != ainf.getFamilyShape())
				proj = sprite;
		} else 
			ainf = AmmoInfo.getDefault();	// So we don't need to keep checking.

			// By now, proj should be >=0 or -1 for none.
		assert(proj >= -1);
		if (winf == null)	// So we don't have to keep checking.
			winf = WeaponInfo.getDefault();
		if (need_ammo != 0) {
			// We should only ever get here for containers and NPCs.
			// Also, ammo should never be zero in this branch.
			boolean need_new_weapon = false;
			boolean ready = att != null ? att.findReadied(ammo) >= 0 : false;

			// Time to use up ammo.
			if (winf.usesCharges()) {
				if (ammo.getInfo().hasQuality())
					ammo.setQuality(ammo.getQuality() - need_ammo);
				if (winf.deleteDepleted() &&
						(ammo.getQuality() == 0 || !ammo.getInfo().hasQuality())) {
					// Call unready usecode if needed.
					if (att != null)
						att.remove(ammo);
					ammo.removeThis();
					need_new_weapon = true;
				}
			} else {
				int quant = ammo.getQuantity();
					// Call unready usecode if needed.
				if (att != null && quant == need_ammo)
					att.remove(ammo);
				ammo.modifyQuantity(-need_ammo);
				need_new_weapon = ammo.getQuantity() == 0;
			}
			if (att != null && need_new_weapon && ready) {
				// Readied weapon was depleted; we need a new one.
				if (winf.returns() || ainf.returns()) {
						// Weapon will return, so wait for it.
					if (combat) {	// We got here due to combat schedule.
						Schedule s = att.getSchedule();
						if (s instanceof CombatSchedule) 
							((CombatSchedule)s).setState(wait_return);
					}
				} else if (att != null && !att.readyAmmo()) { // Try readying ammo first.
					// Need new weapon.
					att.readyBestWeapon();
						// Tell schedule about it.
					att.getSchedule().setWeapon(true);
				}
			}
		}
		Actor trg = target != null ? target.asActor() : null;
		boolean trg_party = trg != null ? trg.getFlag(GameObject.in_party) : false;
		boolean att_party = att != null ? att.getFlag(GameObject.in_party) : false;
		int attval = att != null ? att.getEffectiveProp(Actor.combat) : 0;
		// These two give the correct statistics:
		attval += (winf.lucky() ? 3 : 0);
		attval += (ainf.lucky() ? 3 : 0);
		int bias = trg_party ? difficulty :
				(att_party ? -difficulty : 0);
		attval += 2*bias;	// Apply all bias to the attack value.
		if (ranged) {
			int uses = winf.getUses();
			attval += 6;
			// This seems reasonably close to how the originals do it,
			// although the error bands of the statistics are too wide here.
			if (uses == WeaponInfo.poor_thrown)
				attval -= dist;
			else if (uses == WeaponInfo.good_thrown)
				attval -= dist/2;
			// We need to pass the attack value here to guard against
			// the possibility of the attacker's combat be lowered
			// (e.g., due to being paralyzed) while the projectile is
			// in flight and before it hits.
			EffectsManager.Projectile projectile;
			if (target != null)
				projectile = new EffectsManager.Projectile(attacker, target, 
						weapon, ammo != null ? ammo.getShapeNum() : proj, 
								proj, attval, -1);
			else
				projectile = new EffectsManager.Projectile(attacker, tile, 
						weapon, ammo != null ? ammo.getShapeNum() : proj, 
								proj, attval, -1, false);
			eman.addEffect(projectile);
			return true;
		} else if (target != null) {
			// Do nothing when attacking tiles in melee.
			boolean autohit = winf.autohits() || ainf.autohits();
			// godmode effects:
			if (cheat.inGodMode())
				autohit = trg_party ? false : (att_party ? true : autohit);
			if (!autohit && !target.tryToHit(attacker, attval))
				return false;	// Missed.
			target.playHitSfx(weapon, false);
			if (info.isExplosive()) {	// Powder keg.
				// Blow up *instead*.
				Tile pos = new Tile();
				target.getTile(pos);
				pos.tz += target.getInfo().get3dHeight()/2;
				eman.addEffect(new EffectsManager.ExplosionEffect(pos,
						target, 0, weapon, -1, attacker));
			} else
				target.attacked(attacker, weapon,
						ammo != null ? ammo.getShapeNum() : -1, false);
			return true;
		}
		return false;
	}	
	public static class Duel extends CombatSchedule {
		private Tile start;		// Starting position.
		private int attacks;			// Count strikes.
		@Override
		protected void findOpponents() {
			//+++++++++FINISH
		}
		public Duel(Actor n) {
			super(n, Schedule.duel);
			n.getTile(start=new Tile());
			startedBattle = true;		// Avoid playing music.
		}
		@Override
		public void nowWhat() {
			//+++++++++++++FINISH
		}
	}
}
