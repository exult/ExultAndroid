package com.exult.android;
import com.exult.android.shapeinf.MonsterInfo;

import java.util.Iterator;
import java.util.Vector;

public abstract class Schedule extends GameSingletons {
	public static final int
	combat = 0,	horiz_pace = 1,
	vert_pace = 2,	talk = 3,
	dance = 4,	eat = 5,
	farm = 6,	tend_shop = 7,
	miner = 8,	hound = 9,
	stand = 10,	loiter = 11,
	wander = 12,	blacksmith = 13,
	sleep = 14,	wait = 15,
	sit = 16,	graze = 17,
	bake = 18,	sew = 19,
	shy = 20,	lab = 21,
	thief = 22,	waiter = 23,
	special = 24,	kid_games = 25,
	eat_at_inn = 26,duel = 27,
	preach = 28,	patrol = 29,
	desk_work = 30,	follow_avatar = 31,
				// Our own:
	walk_to_schedule = 32,
	street_maintenance = 33,
	first_scripted_schedule = 0x80;	
	protected Actor npc;		// Who this controls.
	protected Tile blocked;		// Tile where actor was blocked.
	protected short prevType;		// Actor's previous schedule.
	protected int streetMaintenanceFailures;// # times failed to find path.
	protected long streetMaintenanceTime;	// Time (msecs) when last tried.
	
	public Schedule(Actor n) {
		npc = n;
	}
	public void setBlocked(Tile t) {
		if (blocked == null)
			blocked = new Tile(t);
		else
			blocked.set(t);
	}
	public static void setActionSequence(Actor actor, Tile dest, 
			ActorAction when_there, boolean from_off_screen, int delay) {
		actor.setAction(ActorAction.createActionSequence(
				actor, dest, when_there, from_off_screen));
		actor.start(2, delay);	// Get into time queue.
	}
	public abstract void nowWhat();
	public void imDormant()	// Npc calls this when it goes from
		{  }			//   being active to dormant.
	public void ending(int newtype)// Switching to another schedule.
		{  }
	public void setWeapon(boolean removed)	// Set weapon info.
		{  }
					// Set where to sleep.
	public void setBed(GameObject b)
		{  }
					// Notify that schedule's obj. has
					//   been moved.
	public void notifyObjectGone(GameObject obj)
		{  }
					// For Usecode intrinsic.
	public int getActualType(Actor npc) {
		return npc.getScheduleType();
	}
	public boolean seekFoes() {
		Vector<GameObject> vec = new Vector<GameObject>();	// Look within 10 tiles (guess).
		npc.findNearbyActors(vec, EConst.c_any_shapenum, 10, 0x28);
		int npc_align = npc.getEffectiveAlignment();
		Actor foe = null;
		MonsterInfo minf = npc.getInfo().getMonsterInfo();
		boolean see_invisible = minf != null ?
			(minf.getFlags() & (1<<MonsterInfo.see_invisible))!=0 : false;
		for (GameObject each : vec) {
			Actor actor = (Actor)each;
			if (actor.isDead() || actor.getFlag(GameObject.asleep) ||
			    (!see_invisible && actor.getFlag(GameObject.invisible)))
				continue;	// Dead, asleep or invisible and can't see invisible.
			if ((npc_align == Actor.friendly &&
					actor.getEffectiveAlignment() >= Actor.hostile) ||
				(npc_align == Actor.hostile &&
					actor.getEffectiveAlignment() == Actor.friendly)) {
				foe = actor;
				break;
			}
		}
		if (foe != null) {
			npc.setScheduleType(Schedule.combat, null);
			npc.setTarget(foe);
			return true;
		}
		return false;
	}
	/*
	 *	Look for lamps to light/unlight and shutters to open/close.
	 *
	 *	Output:	1 if successful, which means npc's schedule has changed!
	 */
	protected boolean tryStreetMaintenance() {
						// What to look for:
		final int night[] = {322, 372, 889};
		final int sinight[] = {290, 291, 889};
		final int day[] = {290, 291, 526};

		int curtime = TimeQueue.ticks;
		if (curtime < streetMaintenanceTime)
			return false;		// Not time yet.
		if (npc.getNpcNum() <= 0 ||
		    npc == gwin.getCameraActor())
			return false;		// Only want normal NPC's.
						// At least 30secs. before next one.
		streetMaintenanceTime = curtime + 
			(30000 + streetMaintenanceFailures*5000)/TimeQueue.tickMsecs;
		int shapes[];
		int hour = clock.getHour();
		boolean bg = game.isBG();
		if (hour >= 9 && hour < 18)
			shapes = day;
		else if (hour >= 18 || hour < 6)
			shapes = bg ? night : sinight;
		else
			return false;		// Dusk or dawn.
		int npctx = npc.getTileX(), npcty = npc.getTileY();
						// Look at screen + 1/2.
		Rectangle winrect = new Rectangle();
		gwin.getWinTileRect(winrect);
		winrect.enlarge(winrect.w/4);
		if (!winrect.hasPoint(npctx, npcty))
			return false;
						// Get to within 1 tile.
		PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 2);
		Tile t = new Tile(), npcpos = new Tile(npctx, npcty, npc.getLift());
		GameObject found = null;		// Find one we can get to.
		ActorAction pact = null;		// Gets .action to walk there.
		for (int i = 0; found == null && i < night.length; i++) {
			Vector<GameObject> objs = new Vector<GameObject>();// Find nearby.
			npc.findNearby(objs, shapes[i], 20, 0);
			for (GameObject obj : objs) {
				int shnum = obj.getShapeNum();
				if (!bg &&	// Serpent isle?  Shutters?
				    (shnum == 290 || shnum == 291))
						// Want closed during day.
					if ((shapes == day) !=
						(obj.getFrameNum() <= 3))
						continue;
				obj.getTile(t);
				if ((pact = ActorAction.PathWalking.createPath(
				    npcpos, t, cost)) != null) {
					found = obj;
					break;
				}
				streetMaintenanceFailures++;
			}
		}
		if (found == null)
			return false;		// Failed.
						// Set actor to walk there.
		npc.setScheduleType(Schedule.street_maintenance,
				new StreetMaintenance(npc, pact, found));
		// Warning: we are deleted here
		return true;
	}
	protected final void bark() {
		ucmachine.callUsecode(npc.getUsecode(), npc, UsecodeMachine.npc_proximity);
	}
	/*
	 * 	THE SCHEDULES
	 */
	/*
	 *	Street maintenance (turn lamps on/off).
	 */
	public static class StreetMaintenance extends Schedule {
		GameObject obj;		// Lamp/shutters.
		int shapenum, framenum;		// Save original shapenum.
		ActorAction paction;		// Path to follow to get there.
		public StreetMaintenance(Actor n, ActorAction p, GameObject o) {
			super(n);
			obj = o;
			shapenum = o.getShapeNum();
			framenum = o.getFrameNum();
			paction = p;
		}
		@Override
		public void nowWhat() {
			if (paction != null) {			// First time?
						// Set to follow given path.
				npc.setAction(paction);
				npc.start(1, 0);
				paction = null;
				return;
			}
			if (npc.distance(obj) <= 2 &&	// We're there.
			    obj.getShapeNum() == shapenum && obj.getFrameNum() == framenum) {
				int dir = npc.getDirection(obj);
				byte frames[] = new byte[2];
				frames[0] = (byte)npc.getDirFramenum(dir, Actor.standing);
				frames[1] = (byte)npc.getDirFramenum(dir, 3);
				npc.setAction(new ActorAction.Sequence(
					new ActorAction.Frames(frames, frames.length),
					new ActorAction.Activate(obj),
					new ActorAction.Frames(frames, 1), null));
				npc.start(1, 0);
				switch (shapenum)
					{
				case 322:		// Closing shutters.
				case 372:
					npc.say(ItemNames.first_close_shutters, ItemNames.last_close_shutters);
					break;
				case 290:		// Open shutters (or both for SI).
				case 291:
					if (game.isBG())
						npc.say(ItemNames.first_open_shutters, 
								ItemNames.last_open_shutters);
					else		// SI.
						if (framenum <= 3)
							npc.say(ItemNames.first_open_shutters, 
									ItemNames.last_open_shutters);
						else
							npc.say(ItemNames.first_close_shutters, 
									ItemNames.last_close_shutters);
					break;
				case 889:		// Turn on lamp.
					npc.say(ItemNames.first_lamp_on, ItemNames.last_lamp_on);
					break;
				case 526:		// Turn off lamp.
					npc.say(ItemNames.lamp_off, ItemNames.lamp_off);
					break;
					}
				shapenum = 0;		// Don't want to repeat.
				return;
			}
						// Set back to old schedule.
			int period = clock.getHour()/3;
			npc.updateSchedule(period, 7, 0);
		}
		@Override			// For Usecode intrinsic.
		public int getActualType(Actor npc) {
			return prevType;
		}
	}
	/*
	 *	For following the Avatar (by party members):
	 */
	public static class FollowAvatar extends Schedule {
		int nextPathTime;	// Next time we're allowed to use
							//   pathfinding to follow leader.
		Tile pos, goal;
		public FollowAvatar(Actor n) {
			super(n);
			pos = new Tile();
			goal = new Tile();
		}
		public void nowWhat() {	// Now what should NPC do?
			boolean is_blocked = blocked != null && blocked.tx != -1;
			if (blocked == null)
				blocked = new Tile();
			blocked.set(-1, -1, -1);
			if (npc.getFlag(GameObject.asleep) || npc.isDead() ||
			    npc.getFlag(GameObject.paralyzed) ||
			    gwin.mainActorDontMove())	// Under Usecode control.
				return;			// Disabled.
			Actor av = gwin.getMainActor();
			av.getTile(goal);
			npc.getTile(pos);
			int dist2lead = av.distance(pos);
			if (!av.isMoving() &&		// Avatar stopped.
			    dist2lead <= 12)		// And we're already close enough.
				return;
			int curtime = TimeQueue.ticks;// Want the REAL time here.
			if (!is_blocked) {		// Not blocked?
				npc.follow(av);		// Then continue following.
				return;
			}
			if (curtime < nextPathTime) {	// Failed pathfinding recently?
							// Wait a bit.
				npc.start(1, nextPathTime - curtime);
				return;
			}
							// Find a free spot within 3 tiles.
			int where = MapChunk.anywhere;
							// And try to be inside/outside.
			where = gwin.isMainActorInside() ?
							MapChunk.inside : MapChunk.outside;
			if (!MapChunk.findSpot(goal, 3, npc, 0, where)) {
							// No free spot?  Give up.
				System.out.println(npc.getName() + " can't find free spot");
				nextPathTime = TimeQueue.ticks + 1000/TimeQueue.tickMsecs;
				return;
			}
							// Get his speed.
			int speed = av.getFrameTime();
			if (speed == 0)			// Avatar stopped?
				speed = 1;
			if (pos.distance(goal) <= 3)
				return;			// Already close enough!
							// Succeed if within 3 tiles of goal.
			if (npc.walkPathToTile(pos, goal, speed - speed/4, 0, 3, 1))
				return;			// Success.
			System.out.println("... but failed to find path.");
							// On screen (roughly)?
			boolean ok;
							// Get window rect. in tiles.
			Rectangle wrect = new Rectangle();
			gwin.getWinTileRect(wrect);
			if (wrect.hasPoint(pos.tx - pos.tz/2, pos.ty - pos.tz/2)) {
							// Try walking off-screen.
				goal.set(-1, -1, -1);
				ok = npc.walkPathToTile(pos, goal,
									speed - speed/4, 0);
			} else				// Off screen already?
				ok = npc.approachAnother(av, false);
			if (!ok)			// Failed? Don't try again for a bit.
				nextPathTime = 1 + 1000/TimeQueue.tickMsecs;
		}
	}
	/*
	 *	A do-nothing schedule.
	 */
	public static class Wait extends Schedule {
		public Wait(Actor n) {
			super(n);
		}
		public void nowWhat() {
		}
	}
	/*
	 *	A schedule for pacing between two obstacles:
	 */
	public static class Pace extends Schedule {
		int which;		// 0 for north-south, 1 for east-west
		Tile loc;	// The starting position of the schedule
		Tile npcpos;
		int phase;		// Current phase
		public Pace(Actor n, int dir, Tile pos) {
			super(n);
			which = dir;
			loc = pos;
			npcpos = new Tile();
		}
						// Create common schedules:
		public static Pace createHoriz(Actor n) {
			Tile t = new Tile();
			n.getTile(t);
			return (new Pace(n, 1, t));
		}
		public static Pace createVert(Actor n) {
			Tile t = new Tile();
			n.getTile(t);
			return (new Pace(n, 0, t));
		}
		public void nowWhat() {	// Now what should NPC do?
			if (EUtil.rand() % 6 == 0)		// Check for lamps, etc.
				if (tryStreetMaintenance())
					return;		// We no longer exist.
			if (npc.isDormant()) {
				return;
			}
			int dir = npc.getDirFacing();	// Use NPC facing for starting direction
			int delay = 1;
			switch (phase) {
			case 0:
				phase++;
				npc.getTile(npcpos);
				if (!loc.equals(npcpos))
					npc.walkToTile(loc, delay, delay);
				else
					npc.start(delay, delay);
				break;
			case 1: {
				boolean changedir = false;
				int offsetx = 0, offsety = 0;
				switch (dir) {
				case EConst.north:
				case EConst.south:
					if (which != 0)
						changedir = true;
					else
						offsety = dir == EConst.south ? 1 : -1;
					break;
				case EConst.east:
				case EConst.west:
					if (which == 0)
						changedir = true;
					else
						offsetx = dir == EConst.east ? 1 : -1;
					break;
				}
				if (changedir) {
					phase = 4;
					npc.start(delay, delay);
					return;
				}
				if (blocked != null && blocked.tx != -1) {		// Blocked?
					GameObject obj = npc.findBlocking(blocked, dir);
					if (obj != null) {
						Actor act = obj.asActor();
						if (act != null) {
							MonsterInfo minfo = npc.getInfo().getMonsterInfo();
							if (minfo == null || !minfo.cantYell()) {
								npc.say(ItemNames.first_move_aside, 
										ItemNames.last_move_aside);
										// Ask NPC to move aside.
								if (act.moveAside(npc, dir))
											// Wait longer.
									npc.start(3*delay, 3*delay);
								else  // Wait longer.
									npc.start(delay, delay);
								return;
							}
						}
						blocked.tx = -1;
						changedir = true;
					}
				}	
				if (changedir)
					phase++;
				else {
					Tile p0 = new Tile();
					npc.getTile(p0);
					p0.set(p0.tx + offsetx, p0.ty + offsety, p0.tz);
					Actor.FramesSequence frames = npc.getFrames(dir);
					int step_index = npc.getStepIndex();
					if (step_index == 0)		// First time?  Init.
						step_index = frames.findUnrotated(npc.getFrameNum());
										// Get next (updates step_index).
					step_index = frames.nextIndex(step_index);
							// One step at a time.
					if (!npc.step(p0, frames.get(step_index), false)) {
						if (npc.isDormant()) {
							return;
						}
					}
				}
				npc.start(delay, delay);
				break;
				}
			case 2:
				phase++;
				npc.changeFrame(npc.getDirFramenum(npc.getDirFacing(), Actor.standing));
				npc.start(3*delay, 3*delay);
				break;
			case 3:
			case 4: {
				phase++;
				int facedirs[] = {EConst.west, EConst.north, EConst.north, EConst.east, 
						EConst.east, EConst.south, EConst.south, EConst.west};
				npc.changeFrame(npc.getDirFramenum(
						facedirs[dir], Actor.standing));
				npc.start(3*delay, 3*delay);
				break;
				}
			default:
				phase = 1;
				npc.start(2*delay, 2*delay);
				break;
			}
		}
	}
	/*
	 *	A schedule for eating at an inn.
	 */
	public static class EatAtInn extends Schedule {
		public EatAtInn(Actor n) {
			super(n);
		}
		public void nowWhat() {	// Now what should NPC do?
			int frnum = npc.getFrameNum();
			if ((frnum&0xf) != Actor.sit_frame)
				{			// First have to sit down.
				if (Schedule.Sit.setAction(npc, null, 0) == null)
							// Try again in a while.
					npc.start(1, 5000/TimeQueue.tickMsecs);
				return;
				}
			Vector<GameObject> foods = new Vector<GameObject>();// Food nearby?
			int cnt = npc.findNearby(foods, 377, 2, 0);
			if (cnt > 0) {			// Found?
				// Find closest.
				GameObject food = null;
				int dist = 500;
				for (GameObject obj : foods) {
					int odist = obj.distance(npc);
					if (odist < dist) {
						dist = odist;
						food = obj;
					}
				}
				if (EUtil.rand()%5 == 0) {
					gwin.addDirty(food);
					food.removeThis();
				}
				if (EUtil.rand()%4 != 0)
					npc.say(ItemNames.first_munch, ItemNames.last_munch);
			} else if (EUtil.rand()%4 != 0)
				npc.say(ItemNames.first_more_food, ItemNames.last_more_food);
							// Wake up in a little while.
			npc.start(2, (5000 + EUtil.rand()%12000)/TimeQueue.tickMsecs);
		}
	}

	/*
	 *	A schedule for preaching.
	 */
	public static class Preach extends Schedule {
		static boolean debug = true;
		static final int
			find_podium = 0,
			at_podium = 1,
			exhort = 2,
			visit = 3,
			talk_member = 4,
			find_icon = 5,
			pray = 6;
		int state;
		Tile npcPos = new Tile(), podiumPos = new Tile();
		public Preach(Actor n) {
			super(n);
			state = find_podium;
		}
		public void nowWhat() {	// Now what should NPC do?
			if (debug) System.out.println("Preach: state = " + state);
			switch (state) {
			case find_podium:
				{
				Vector<GameObject> vec = new Vector<GameObject>();
				if (npc.findNearby(vec, 697, 17, 0) == 0) {
					npc.setScheduleType(loiter);
					return;
				}
				GameObject podium = vec.firstElement();
				podium.getTile(podiumPos);
				final int deltas[][] = {{-1, 0},{1, 0},{0, -2},{0, 1}};
				int frnum = podium.getFrameNum()%4;
				podiumPos.tx += deltas[frnum][0];
				podiumPos.ty += deltas[frnum][1];
				npc.getTile(npcPos);
				PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 0);
				ActorAction pact = ActorAction.PathWalking.createPath(
											npcPos, podiumPos, cost);
				if (pact != null) {
					state = at_podium;
					npc.setAction(new ActorAction.Sequence(pact,
							new ActorAction.FacePos(podium, 1)));
					npc.start(1, 0);
					return;
				}
				// Try again later.
				npc.start(1, (5000 + EUtil.rand()%5000)/TimeQueue.tickMsecs);	
				return;
				}
			case at_podium:
				if (EUtil.rand()%2 != 0)		// Just wait a little.
					npc.start(1, (EUtil.rand()%3000)/TimeQueue.tickMsecs);
				else  {
					if (EUtil.rand()%3 != 0)
						state = exhort;
					else if (game.isSI() || EUtil.rand()%3 != 0)
						state = visit;
					else
						state = find_icon;
					npc.start(1, (2000 + EUtil.rand()%2000)/TimeQueue.tickMsecs);
				}
				return;
			case exhort:
				{
				byte frames[] = new byte[8];		// Frames.
				int cnt = 1 + EUtil.rand()%(frames.length - 1);
						// Frames to choose from:
				final byte choices[] = {0, 8, 9};
				for (int i = 0; i < cnt - 1; i++)
					frames[i] = (byte)npc.getDirFramenum(
						choices[EUtil.rand()%(choices.length)]);
						// Make last one standing.
				frames[cnt - 1] = (byte)npc.getDirFramenum(Actor.standing);
				npc.setAction(new ActorAction.Frames(frames, cnt, 1, null));
				npc.start(1, 0);
				npc.say(ItemNames.first_preach, ItemNames.last_preach);
				state = at_podium;
				Actor member = findCongregant(npc);
				if (member != null) {
					UsecodeScript scr = new UsecodeScript(member);
					scr.add(UsecodeScript.delay_ticks, 3);
					scr.add(UsecodeScript.face_dir, member.getDirFacing());
					scr.add(UsecodeScript.npc_frame + Actor.standing);
					scr.add(UsecodeScript.say).add(ItemNames.msgs[ItemNames.first_amen +
					EUtil.rand()%(ItemNames.last_amen - ItemNames.first_amen + 1)]);
					scr.add(UsecodeScript.delay_ticks, 2);
					scr.add(UsecodeScript.npc_frame + Actor.sit_frame);
					scr.start();	// Start next tick.
				}
				return;
				}
			case visit:
				{
				state = find_podium;
				npc.start(1, (1000 + EUtil.rand()%2000)/TimeQueue.tickMsecs);
				Actor member = findCongregant(npc);
				if (member == null)
					return;
				Tile pos = new Tile();
				member.getTile(pos);
				npc.getTile(npcPos);
				PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 1);
				ActorAction pact = ActorAction.PathWalking.createPath(
														npcPos, pos, cost);
				if (pact == null)
					return;
				npc.setAction(new ActorAction.Sequence(pact,
					new ActorAction.FacePos(member, 1)));
				state = talk_member;
				return;
				}
			case talk_member:
				state = find_podium;
				npc.say(ItemNames.first_preach2, ItemNames.last_preach2);
				npc.start(2, 2000/TimeQueue.tickMsecs);
				return;
			case find_icon:
				{
				state = find_podium;		// In case we fail.
				npc.start(2, 0);
				GameObject icon = npc.findClosest(724);
				if (icon == null)
					return;
				Tile pos = new Tile();
				icon.getTile(pos);
				pos.tx += 2;
				pos.ty -= 1;
				npc.getTile(npcPos);
				PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 0);
				ActorAction pact = ActorAction.PathWalking.createPath(
												npcPos, pos, cost);
				if (pact != null) {
					npc.setAction(pact);
					state = pray;
				}
				return;
				}
			case pray:
				{
				UsecodeScript scr = new UsecodeScript(npc);
				scr.add(UsecodeScript.face_dir, 6,	// Face west.
						UsecodeScript.npc_frame + Actor.standing,
						UsecodeScript.npc_frame + Actor.bow_frame,
						UsecodeScript.delay_ticks, 3,
						UsecodeScript.npc_frame + Actor.kneel_frame);
				scr.add(UsecodeScript.say).add(
						ItemNames.msgs[ItemNames.first_amen + EUtil.rand()%2]);
				scr.add(UsecodeScript.delay_ticks, 5,
						UsecodeScript.npc_frame + Actor.bow_frame,
						UsecodeScript.delay_ticks, 3,
						UsecodeScript.npc_frame + Actor.standing);
				scr.start();	// Start next tick.
				state = find_podium;
				npc.start(2, 4000/TimeQueue.tickMsecs);
				return;
				}
			default:
				state = find_podium;
				npc.start(2, 0);
				return;
			}
		}
		/*
		 * Find someone listening.
		 */
		private Actor findCongregant(Actor npc) {
			Vector<GameObject> vec = new Vector<GameObject>();
			if (npc.findNearbyActors(vec, EConst.c_any_shapenum, 16) == 0)
				return null;
			Iterator<GameObject> iter = vec.iterator();
			while (iter.hasNext()) {
				Actor n = iter.next().asActor();
				if (n == null || n.getScheduleType() != Schedule.sit ||
						n.getFlag(Actor.in_party))
					iter.remove();
			}
			int sz = vec.size();
			return sz > 0 ? vec.elementAt(EUtil.rand()%sz).asActor() : null;
		}
	}
	/*
	 *	A schedule for patrolling along 'path' objects.
	 */
	public static class Patrol extends Schedule {
		Vector<GameObject> paths;	// Each 'path' object.
		int pathnum;			// # of next we're heading towards.
		int dir;				// 1 or -1;
		boolean wrap;				// If true, wraps to zero when reached last path
		int failures;			// # of failures to find marker.
		int state;				// The patrol state.
		Tile center;		// For 'loiter' and 'pace' path eggs.
		Tile pos;			// A temp.
		byte whichdir;			// For 'pace' path eggs.
		int pace_count;			// For 'pace' path eggs.
		GameObject hammer;	// For 'hammer' path eggs.
		GameObject book;		// For 'read' path eggs.
		boolean seek_combat;		// The NPC should seek enemies while patrolling.
		public Patrol(Actor n) {
			super(n);
			pathnum = -1;
			dir = 1;
			wrap = true;
			center = new Tile(0, 0, 0);
			pos = new Tile();
			paths = new Vector<GameObject>();
		}
		private static final int speed = 1;
		private static final int PATH_SHAPE = 607;
		private void findNextPath() {
			
			pathnum += dir;			// Find next path.
			if (pathnum == 0 && dir == -1)
				dir = 1;	// Start over from zero.
			if (pathnum >= paths.size())
				paths.setSize(pathnum + 1);
							// Already know its location?
			GameObject path =  pathnum >= 0 ? paths.elementAt(pathnum) : null;
			if (path == null) {			// No, so look around.
				path = npc.findClosest(PATH_SHAPE, 25, 0x10, 
							EConst.c_any_qual, pathnum);
				if (path != null) {		// Save it.
					failures = 0;
					paths.setElementAt(path, pathnum);
				} else {			// Turn back if at end.
					failures++;
					dir = 1;
					if (pathnum == 0) {	// At start?  Retry.
						if (failures < 4) {
							pathnum = -1;
							npc.start(speed, 2*speed);
								// Makes some enemies more prone to attacking.
							seek_combat = true;
							return;
						}
							// After 4, fall through.
					} else {		// At end, go back to start.
						if (wrap) {
							pathnum = 0;
							path = !paths.isEmpty() ? paths.firstElement() : null;
						} else {
							pathnum = pathnum-2 >= 0 ? pathnum-2 : 0;
							dir = -1;
							path = !paths.isEmpty() ? paths.elementAt(pathnum) 
									: null;
						}
					}
				}
				if (path == null) {	// Still failed?
							// Wiggle a bit.
					npc.getTile(pos);
					int dist = failures + 2;
					pos.set(pos.tx + EUtil.rand()%dist - dist/2,
							pos.ty + EUtil.rand()%dist - dist, pos.tz);
					npc.walkToTile(pos, speed, 
									(failures*300)/TimeQueue.tickMsecs);
					int pathcnt = paths.size();
					pathnum = EUtil.rand()%(pathcnt < 4 ? 4 : pathcnt);
						// Makes some enemies more prone to attacking.
					seek_combat = true;
					return;
				}
			}
			path.getTile(pos);
			if (!npc.walkPathToTile(pos, speed, 2*speed + EUtil.rand()%3, 0)) {		
							// Look for free tile within 1 square.
				path.getTile(pos);
				if (!MapChunk.findSpot(pos, 1, npc.getShapeNum(),
														npc.getFrameNum(), 1) ||
					!npc.walkPathToTile(pos, speed, EUtil.rand()%5, 0)) {
							// Failed.  Later.
					npc.start(speed, 2000/TimeQueue.tickMsecs);
					return;
				}
			}
			state = 1;	// Walking to path.
		}
		private void doPathAction(GameObject path) {
			whichdir = 1;	// Default to East-West pace.
			int delay = 2;
			// Scripts for all actions. At worst, display standing frame
			// once the path egg is reached.
			UsecodeScript scr = new UsecodeScript(npc);
			scr.add(UsecodeScript.npc_frame + Actor.standing);
						// Quality = type.  (I think high bits
						// are flags).
			int qual = path.getQuality();
			seek_combat = (qual&32)!=0;
			// TODO: Find out what flags 64 and 128 mean. It would seem
			// that they are 'Repeat Forever' and 'Exc. Reserved.', but
			// what does those mean?
			switch (qual&31)
				{
			case 0:			// None.
				break;
			case 25:		// 50% wrap to 0.
				if (EUtil.rand()%2 != 0)
					break;
				// Fall through to wrap.
			case 1:			// Wrap to 0.
				pathnum = -1;
				dir = 1;
				break;
			case 2:			// Pause; guessing 3 ticks.
				{
				scr.add(UsecodeScript.delay_ticks, 3);
				delay = 5;
				break;
				}
			case 24:		// Read
				// Find the book which will be read.
				book = npc.findClosest(642, 4);
				// Fall through to sit.
			case 3:			// Sit.
				if (Sit.setAction(npc) != null) {
					scr.start();	// Start next tick.
					state = 2;
					return;
				}
				break;
			case 4:			// Kneel at tombstone.
			case 5:			// Kneel
					// Both seem to work identically.
				scr.add(UsecodeScript.delay_ticks, 2,
					UsecodeScript.npc_frame + Actor.bow_frame,
					UsecodeScript.delay_ticks, 4,
					UsecodeScript.npc_frame + Actor.kneel_frame,
					UsecodeScript.delay_ticks, 20,
					UsecodeScript.npc_frame + Actor.bow_frame,
					UsecodeScript.delay_ticks, 4,
					UsecodeScript.npc_frame + Actor.standing); 
				delay = 36;
				break;
			case 6:			// Loiter.
				{
				scr.start();	// Start next tick.
				path.getTile(center);
				state = 4;
				npc.start(speed, speed*delay);
				return;
				}
			case 7:			// Right about-face.
			case 8:			// Left about-face.
				{
				wrap = false;	// Guessing; seems to match the original.
				final int dirs_left[] = {EConst.west, EConst.north, EConst.north, 
						EConst.east, EConst.east, EConst.south, EConst.south, 
						EConst.west};
				final int dirs_right[] = {EConst.east, EConst.east, EConst.south, 
						EConst.south, EConst.west, EConst.west, EConst.north, 
						EConst.north};
				final int face_dirs[];
				if ((path.getQuality()&31) == 7)
					face_dirs = dirs_right;
				else
					face_dirs = dirs_left;
				int facing = npc.getDirFacing();
				for (int i=0; i<2; i++)
					{
					scr.add(UsecodeScript.delay_ticks, 2,
							UsecodeScript.face_dir, face_dirs[facing]);
					facing = face_dirs[facing];
					}
				delay = 8;
				break;
				}
			// Both 9 and 10 appear to pace vertically in the originals.
			// I am having 9 as vert. pace for the guards in Fawn.
			case 9:			// Vert. pace.
				whichdir = 0;
			case 10:		// Horiz. pace.
				pace_count = -1;
				scr.start();	// Start next tick.
				path.getTile(center);
				state = 5;
				npc.start(speed, speed*delay);
				return;
			case 11:		// 50% reverse.
				if (EUtil.rand()%2 != 0)
					dir *= -1;
				break;
			case 12:		// 50% skip next.
				if (EUtil.rand()%2 != 0)
					pathnum += dir;
				break;
			case 13:		// Hammer.
				if (hammer == null)	// Create hammer if does not exist.
					hammer = new IregGameObject(623, 0, 0, 0, 0);
					// For safety, unready weapon first.
				npc.unreadyWeapon();
				npc.addDirty();
					// Ready the hammer in the weapon hand.
				npc.addReadied(hammer, Ready.lhand, false, true, false);
				npc.addDirty();

				int hammersfx= game.isBG() ? 45:49;
				scr.add(UsecodeScript.delay_ticks, 2,
					UsecodeScript.npc_frame + Actor.ready_frame,
					UsecodeScript.delay_ticks, 2,
					UsecodeScript.npc_frame + Actor.raise1_frame,
					UsecodeScript.delay_ticks, 2,
					UsecodeScript.sfx, hammersfx,
					UsecodeScript.npc_frame + Actor.out_frame,
					UsecodeScript.repeat, -11, 1,
					UsecodeScript.delay_ticks, 2,
					UsecodeScript.npc_frame + Actor.ready_frame,
					UsecodeScript.delay_ticks, 2,
					UsecodeScript.npc_frame + Actor.standing);
				delay = 24;
				break;
			case 15:	// Usecode.
						// Don't let this script halt others,
						//   as it messes up automaton in
						//   SI-Freedom.
				scr.add(UsecodeScript.usecode2, npc.getUsecode(),
					  UsecodeMachine.npc_proximity);
				delay = 3;
				break;
			case 16:		// Bow to ground.
			case 17:		// Bow from ground, seems to work exactly like 16
				scr.add(UsecodeScript.delay_ticks, 2,
						UsecodeScript.npc_frame + Actor.bow_frame,
						UsecodeScript.delay_ticks, 2,
						UsecodeScript.npc_frame + Actor.standing);
				delay = 8;
				break;
			case 20:		// Ready weapon
				npc.readyBestWeapon();
				break;
			case 14:		// Check area.
				// Maybe could be improved?
				delay += 2;
				scr.add(UsecodeScript.delay_ticks, 2);
			case 21:		// Unready weapon
				npc.unreadyWeapon();
				break;
			case 22:		// One-handed swing.
			case 23:		// Two-handed swing.
				int dir = npc.getDirFacing();
				byte frames[] = new byte[12];		// Get frames to show.
				GameObject weap = npc.getReadied(Ready.rhand);
				
				int cnt = npc.getAttackFrames(weap != null ? weap.getShapeNum() 
						: 0, false, dir, frames);
				if (cnt != 0)
					npc.setAction(new ActorAction.Frames(frames, cnt, speed, null));
				npc.start(speed, speed*(delay+1));		// Get back into time queue.
				break;
			// What should these two do???
			case 18:		// Wait for semaphore+++++
			case 19:		// Release semaphore+++++
			default:
				System.out.println("Unhandled path egg quality in patrol schedule: " + qual);
				break;
			}
			scr.start();	// Start next tick.
			state = 0;	// THEN, find next path.
			npc.start(speed, speed*delay);
		}
		public void nowWhat() {	// Now what should NPC do?
			if (EUtil.rand() % 8 == 0)		// Check for lamps, etc.
				if (tryStreetMaintenance())
					return;		// We no longer exist.
			
			if (seek_combat && seekFoes())	// Check for nearby foes.
				return;
			switch (state){
			case 0:	// Find next path.
				findNextPath();
				break;
			case 1:	// Walk to next path.
				GameObject path;
				if (pathnum >= 0 &&		// Arrived at path?
					pathnum < paths.size() &&
					(path = paths.elementAt(pathnum)) != null &&
											npc.distance(path) < 2) {
					doPathAction(path);
				} else {
					state = 0;	// Walking to path.
					npc.start(speed, speed);
				}
				break;
			case 2:	// Sitting/reading.
							// Stay 5-15 secs.
				if ((npc.getFrameNum()&0xf) == Actor.sit_frame) {
					if (book != null) {	// Open book if reading.
						int frnum = book.getFrameNum();
						if (frnum%3 != 0)
							book.changeFrame(frnum - frnum%3);
						else	// Book already open; we shouldn't close it then.
							book = null;
						}
					npc.start(2, (5000 + EUtil.rand()%10000)/TimeQueue.tickMsecs);
					}
				else			// Not sitting.
					npc.start(2, (EUtil.rand()%1000)/TimeQueue.tickMsecs);
				state = 3;	// Continue on afterward.
				break;
			case 3:	// Stand up.
				if ((npc.getFrameNum()&0xf) == Actor.sit_frame) {
					if (book != null) {	// Close book we opened it.
						int frnum = book.getFrameNum();
						book.changeFrame(frnum - frnum%3 + 1);
						book = null;
					}
					// Standing up animation.
					UsecodeScript scr = new UsecodeScript(npc);
					scr.add(UsecodeScript.delay_ticks, 2,
						UsecodeScript.npc_frame + Actor.bow_frame,
						UsecodeScript.delay_ticks, 2,
						UsecodeScript.npc_frame + Actor.standing);
					scr.start();	// Start next tick.
					npc.start(speed, speed*7);
				}
				state = 0;
				break;
			case 4:	// Loiter.
				if (EUtil.rand()%5 == 0) {
					state = 0;
					npc.start(speed, speed);
				} else {
					int dist = 12;
					int newx = center.tx - dist + EUtil.rand()%(2*dist);
					int newy = center.ty - dist + EUtil.rand()%(2*dist);
										// Wait a bit.
					npc.walkToTile(new Tile(newx, newy, center.tz), speed, 
										(EUtil.rand()%2000)/TimeQueue.tickMsecs);
				}
				break;
			case 5:	// Pacing.
				if (npc.distance(center) < 1) {
					pace_count++;
					if (pace_count > 0 && pace_count == 6) {
						UsecodeScript scr = new UsecodeScript(npc);
						scr.add(UsecodeScript.npc_frame + Actor.standing);
						scr.start();	// Start next tick.
						state = 0;
						npc.start(speed, 2*speed);
						return;
					}
				}
				int dir = npc.getDirFacing();	// Use NPC facing for starting direction
				boolean changedir = false;
				int offx = 0, offy = 0;
				switch (dir)
					{
					case EConst.north:
					case EConst.south:
						if (whichdir != 0)
							changedir = true;
						else
							offy = dir == EConst.south ? 1 : -1;
						break;
					case EConst.east:
					case EConst.west:
						if (whichdir == 0)
							changedir = true;
						else
							offx = dir == EConst.east ? 1 : -1;
						break;
					}
				if (blocked.tx != -1) {		// Blocked?
					GameObject obj = npc.findBlocking(blocked, dir);
					if (obj != null) {
						blocked.tx = -1;
						changedir = true;
						if (obj.asActor() != null) {
							MonsterInfo minfo = npc.getInfo().getMonsterInfo();
							if (minfo == null || !minfo.cantYell()) {
								npc.say(ItemNames.first_move_aside, 
										ItemNames.last_move_aside);
									// Wait longer.
								npc.start(speed, speed);
								return;
							}
						}
					}
				}
				if (changedir) {
					if (npc.distance(center) < 1)
						pace_count--;
					int facedirs[] = {EConst.west, EConst.north, EConst.north, 
									EConst.east, EConst.east, EConst.south, 
									EConst.south, EConst.west};
					npc.changeFrame(npc.getDirFramenum(
										facedirs[dir], Actor.standing));
					npc.start(2*speed, 2*speed);
					return;
				}
				npc.getTile(pos);
				pos.tx += offx; pos.ty += offy;
				Actor.FramesSequence frames = npc.getFrames(dir);
				int step_index = npc.getStepIndex();
				if (step_index == 0)		// First time?  Init.
					step_index = frames.findUnrotated(npc.getFrameNum());
									// Get next (updates step_index).
				step_index = frames.nextIndex(step_index);
				int frame = frames.get(step_index);
						// One step at a time.
				npc.step(pos, frame, false);
				npc.start(speed, speed);
				break;
			default:
					// Just in case.
				break;
			}	
		}
		public void ending(int newtype) { // Switching to another schedule
			if (hammer != null) {
				hammer.removeThis();
				hammer = null;
			}
		}
	}
	/*
	 *	Talk to avatar.
	 */
	public static class Talk extends Schedule {
		static boolean debug = true;
		int phase;			// 0=walk to Av., 1=talk, 2=done.
		Tile pos = new Tile();	// Temp.
		public Talk(Actor n) {
			super(n);
			phase = 0;
		}
		public void nowWhat() {	// Now what should NPC do?
			int speed = 1;
			if (debug) System.out.println("Talk: phase = " + phase);
			// Switch to phase 3 if we are reasonable close
			if (phase < 3 && 
			    npc.distance(gwin.getMainActor()) < 6) {
				phase = 3;
				npc.start(speed, 1);
				return;
			}
			switch (phase) {
			case 0:				// Start by approaching Avatar.
				{
				if (npc.distance(gwin.getMainActor()) > 50) {// Too far?  
							// Try a little later.
					npc.start(speed, 5000/TimeQueue.tickMsecs);
					return;
				}
							// Aim for within 5 tiles.
				PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 5);
				npc.getTile(pos);
				ActorAction pact = ActorAction.Approach.createPath(
										pos, gwin.getMainActor(), 5, cost);
				if (pact == null) {
					// No path found; try again a little later.
					npc.start(speed, 3);
					return;
				} else {
					if (EUtil.rand()%3 == 0)
						npc.say(ItemNames.first_talk, ItemNames.last_talk);
							// Walk there, and retry if
							//   blocked.
					npc.setAction(pact);
					npc.start(speed, 0);	// Start walking.
					}
				phase++;
				return;
				}
			case 1:				// Wait a second.
			case 2:
				{
				if (EUtil.rand()%3 == 0)
					npc.say(ItemNames.first_talk, ItemNames.last_talk);
				// Step towards Avatar.
				npc.getTile(pos);
				int destx = gwin.getMainActor().getTileX(),
					desty = gwin.getMainActor().getTileY();
				int dx = destx > pos.tx ? 1 : (destx < pos.tx ? -1 : 0);
				int dy = desty > pos.ty ? 1 : (desty < pos.ty ? -1 : 0);
				pos.tx += dx;
				pos.ty += dy;
				npc.walkToTile(pos, speed, 2);
				phase = 3;
				return;
				}
			case 3:				// Talk.
				{
				int dist = npc.distance(gwin.getMainActor());
							// Got to be close & reachable.
				if (dist > 5 ||
					!PathFinder.FastClient.isGrabable(npc, gwin.getMainActor())) {
					phase = 0;
					npc.start(speed, 2);
					return;
				}
							// But first face Avatar.
				npc.changeFrame(npc.getDirFramenum(npc.getDirection(
						gwin.getMainActor()), Actor.standing));
				phase++;
				npc.start(speed, 1);	// Wait another 1/4 sec.
				break;
				}
			case 4:
				npc.stop();		// Stop moving.
							// NOTE:  This could DESTROY us!
				if (game.isSI())
					npc.activate(9);
				else
					npc.activate(1);
							// SO don't refer to any instance
							//   variables from here on.
				gwin.paint();
				return;
			default:
				break;
			}
		}
	}
	/*
	 *	Loiter within a rectangle.
	 */
	public static class Loiter extends Schedule {
		private Tile loc;
		protected Tile center;		// Center of rectangle.
		protected int dist;			// Distance in tiles to roam in each dir.
		public Loiter(Actor n) {
			super(n);
			dist = 12;
			n.getTile(center = new Tile());
		}
		public Loiter(Actor n, int d) {
			super(n);
			dist = d;
			n.getTile(center = new Tile());
		}
		public void nowWhat() {	// Now what should NPC do?
			if (EUtil.rand() % 3 == 0)		// Check for lamps, etc.
				if (tryStreetMaintenance())
					return;		// We no longer exist.
			loc = new Tile(center.tx - dist + EUtil.rand()%(2*dist),
								center.ty - dist + EUtil.rand()%(2*dist), center.tz);
							// Wait a bit.
			npc.walkToTile(loc, 2+EUtil.rand()%2, EUtil.rand()%8);
			if (EUtil.rand()%4 == 0)
				bark();
		}
	}
	/*
	 *	Wander all over the place, using pathfinding.
	 */
	public static class Wander extends Loiter {
		public Wander(Actor n) {
			super(n, 128);
		}
		public void nowWhat() {	// Now what should NPC do?
			if (EUtil.rand() % 2 != 0)			// 1/2 time, check for lamps, etc.
				if (tryStreetMaintenance())
					return;		// We no longer exist.
			Tile pos = new Tile();
			npc.getTile(pos);
			int legdist = 32;
							// Go a ways from current pos.
			pos.tx += -legdist + EUtil.rand()%(2*legdist);
			pos.ty += -legdist + EUtil.rand()%(2*legdist);
							// Don't go too far from center.
			if (pos.tx - center.tx > dist)
				pos.tx = (short)(center.tx + dist);
			else if (center.tx - pos.tx > dist)
				pos.tx = (short)(center.tx - dist);
			if (pos.ty - center.ty > dist)
				pos.ty = (short)(center.ty + dist);
			else if (center.ty - pos.ty > dist)
				pos.ty = (short)(center.ty - dist);
							// Find a free spot.
			if (!MapChunk.findSpot(pos, 4, npc.getShapeNum(), 0, 1) ||
					!npc.walkPathToTile(pos, 2+EUtil.rand()%2, EUtil.rand()%8, 1))
							// Failed?  Try again a little later.
				npc.start(2, EUtil.rand()%12);
			else if (EUtil.rand()%3 == 0)
				bark();
		}
	}
	/*
	 *	Kid games.
	 */
	public static class KidGames extends Loiter {
		private Tile pos = new Tile(), kidpos = new Tile();
		private Vector<Actor> kids;			// Other kids playing.
		public KidGames(Actor n) {
			super(n, 10);
			kids = new Vector<Actor>();
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			npc.getTile(pos);
			Actor kid = null;			// Get a kid to chase.
							// But don't run too far.
			while (!kids.isEmpty()) {
				kid = kids.remove(kids.size() - 1);
				if (npc.distance(kid) < 16)
					break;
				kid = null;
			}
			if (kid != null) {
				kid.getTile(kidpos);
				PathFinder.FastClient cost = new PathFinder.FastClient(1);
				ActorAction pact = ActorAction.PathWalking.createPath(pos, kidpos, cost);
				if (pact != null) {
					npc.setAction(pact);
					npc.start(1, 1); // Run.
					return;
				}
			} else {				// No more kids?  Search.
				Vector<GameObject> vec = new Vector<GameObject>();
				npc.findNearbyActors(vec, EConst.c_any_shapenum, 16);
				for (GameObject each : vec) {
					Actor act = (Actor)each;
					if (act.getScheduleType() == kid_games)
						kids.add(act);
				}
			}
			super.nowWhat();	// Wander around the start.
		}
	}
	/*
	 *	Dance.
	 */
	public static class Dance extends Loiter {
		private Tile cur = new Tile(), dest = new Tile();
		public Dance(Actor n) {
			super(n, 4);
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			dest.set(center);	// Pick new spot to walk to.
			dest.tx += -dist + EUtil.rand()%(2*dist);
			dest.ty += -dist + EUtil.rand()%(2*dist);
			npc.getTile(cur);
			int dir = EUtil.getDirection4(cur.ty - dest.ty, 
									dest.tx - cur.tx);
			byte frames[] = new byte[4];
			for (int i = 0; i < 4; i++)
							// Spin with 'hands outstretched'.
				frames[i] = (byte)npc.getDirFramenum((2*(dir + i))%8, 
						   (npc.getShapeNum() == 846 && game.isSI()) ? 15 : 9);
							// Create action to walk.
			ActorAction walk = new ActorAction.PathWalking(new ZombiePathFinder());
			walk.walkToTile(npc, cur, dest, 0);
							// Walk, then spin.
			npc.setAction(new ActorAction.Sequence(walk,
				new ActorAction.Frames(frames, frames.length, 1, null)));
			npc.start(1, 500/TimeQueue.tickMsecs);		// Start in 1/2 sec.
		}
	}
	/*
	 *	Miner/farmer:
	 */
	public static class Tool extends Loiter {
		protected int toolshape;			// Pick/scythe shape.
		protected GameObject tool;
		protected void getTool() {
			tool = new IregGameObject(toolshape, 0, 0, 0, 0);
			// Free up both hands.
			GameObject obj = npc.getReadied(Ready.rhand);
			if (obj != null)
				obj.removeThis();
				if ((obj = npc.getReadied(Ready.lhand)) != null)
					obj.removeThis();
					npc.addReadied(tool, Ready.lhand);
		}
		public Tool(Actor n, int shnum) {
			super(n, 12);
			toolshape = shnum;
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			if (tool == null)			// First time?
				getTool();
			if (EUtil.rand()%4 == 0) {		// 1/4 time, walk somewhere.
				super.nowWhat();
				return;
			}
			if (EUtil.rand()%10 == 0) {
				int ty = npc.getScheduleType();
				if (ty == Schedule.farm) {
					if (EUtil.rand()%2 != 0)
						npc.say(ItemNames.first_farmer, ItemNames.last_farmer);
					else
						npc.say(ItemNames.first_farmer2, ItemNames.last_farmer2);
				}
			}
			byte frames[] = new byte[12];		// Use tool.
			int cnt = npc.getAttackFrames(toolshape, false, EUtil.rand()%8, frames);
			if (cnt > 0)
				npc.setAction(new ActorAction.Frames(frames, cnt));
			npc.start(0, 0);			// Get back into time queue.
		}
		@Override
		public void ending(int newtype) {	// Switching to another schedule.
			if (tool != null)
				tool.removeThis();	// Should safely remove from NPC.
		}
	}

	/*
	 *	Miner.
	 */
	public static class Miner extends Tool {
		private GameObject ore;
		private Tile npcPos = new Tile(), orePos = new Tile();
		private static final int // enum {
			find_ore = 0,
			attack_ore = 1,
			ore_attacked = 2,
			wander = 3;
		private int state;
		public Miner(Actor n)  {
			super(n, 624);
			state = find_ore;
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			int delay = 0, cnt;
			if (tool == null)			// First time?
				getTool();
			switch (state) {
			case find_ore:
				final int oreshapes[] = {915, 916};
				Vector<GameObject> ores = new Vector<GameObject>();
				npc.findClosest(ores, oreshapes, oreshapes.length);
				int from, to;
				cnt = ores.size();
				// Filter out frame #3 (dust).
				for (from = to = 0; from < cnt; ++from) {
					GameObject ore = ores.elementAt(from);
					if (ore.getFrameNum() < 3)
						ores.setElementAt(ore, to++);
				}
				cnt = to;
				if (cnt > 0) {
					ore = ores.elementAt(EUtil.rand()%cnt);
					npc.getTile(npcPos);
					ore.getTile(orePos);
					PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 2);
					ActorAction pact = ActorAction.PathWalking.createPath(
						     npcPos, orePos, cost);
					if (pact != null) {
						state = attack_ore;
						npc.setAction(new ActorAction.Sequence(pact,
						    new ActorAction.FacePos(ore, 200)));
						break;
					}
				}
				ore = null;
				state = wander;
				delay = 1000 + EUtil.rand()%1000;	// Try again later.
				break;
			case attack_ore:
				if (ore.isPosInvalid() || npc.distance(ore) > 2) {
					state = find_ore;
					break;
				}
				byte frames[] = new byte[20];		// Use pick.
				int dir = npc.getDirection(ore);
				cnt = npc.getAttackFrames(toolshape, false, dir,
										frames);
				if (cnt > 0) {
					frames[cnt++] = (byte)npc.getDirFramenum(dir, Actor.standing);
					npc.setAction(new ActorAction.Frames(frames, cnt));
					state = ore_attacked;
				} else
					state = wander;
				break;
			case ore_attacked:
				if (ore.isPosInvalid()) {
					state = find_ore;
					break;
				}
				state = attack_ore;
				if (EUtil.rand()%6 == 0) {		// Break up piece.
					int shnum, frnum = ore.getFrameNum();
					if (frnum == 3)
						state = find_ore;	// Dust.
					else if (EUtil.rand()%(4+2*frnum) == 0) {
						npc.say(ItemNames.first_miner_gold, ItemNames.last_miner_gold);
						ore.getTile(orePos);
						ore.removeThis();
						if (frnum == 0) {	// Gold.
							shnum = 645;
							frnum = EUtil.rand()%2;
						} else {		// Gem.
							shnum = 760;
							frnum = EUtil.rand()%10;
						}
						GameObject newobj = new IregGameObject(
									shnum, frnum, 0, 0, 0);
						newobj.move(orePos);
						newobj.setFlag(GameObject.is_temporary);
						state = find_ore;
						break;
					} else {
						ore.changeFrame(frnum + 1);
						if (ore.getFrameNum() == 3)
							state = find_ore;// Dust.
					}
				}
				if (EUtil.rand()%4 == 0) {
					npc.say(ItemNames.first_miner, ItemNames.last_miner);
				}
				delay = 500 + EUtil.rand()%2000;
				break;
			case wander:
				if (EUtil.rand()%2 == 0) {
					super.nowWhat();
					return;
				} else
					state = find_ore;
				break;
			}
			npc.start(1, delay);
		}
	}
	/*
	 *	Hound the Avatar.
	 */
	public static class Hound extends Schedule {
		Tile avPos = new Tile(), npcPos = new Tile();
		public Hound(Actor n) {
			super(n);
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			Actor av = gwin.getMainActor();
			av.getTile(avPos);
			npc.getTile(npcPos);
							// How far away is Avatar?
			int dist = npc.distance(av);
			if (dist > 20 || dist < 3) {	// Too far, or close enough?
							// Check again in a few seconds.
				npc.start(1, (500 + EUtil.rand()%1000)/TimeQueue.tickMsecs);
				return;
			}
			int newdist = 1 + EUtil.rand()%2;	// Aim for about 3 tiles from Avatar.
			PathFinder.FastClient cost = new PathFinder.FastClient(newdist);
			avPos.tx += EUtil.rand()%3 - 1;	// Vary a bit randomly.
			avPos.ty += EUtil.rand()%3 - 1;
			ActorAction pact = ActorAction.PathWalking.createPath(npcPos,
									avPos, cost);
			if (pact != null) {
				npc.setAction(pact);
				npc.start(1, 1);
			} else				// Try again.
				npc.start(1, (2000 + EUtil.rand()%3000)/TimeQueue.tickMsecs);
		}
	}
	/*
	 *	Sleep in a  bed.
	 */
	public static class Sleep extends Schedule {
		public boolean debug;
		private Tile floorloc;		// Where NPC was standing before.
		private GameObject bed;		// Bed being slept on, or 0.
		private int state;
		private int spread0, spread1;		// Range of bedspread frames.
		// Stand up if not already.
		public static void standUp(Actor npc) {
			if ((npc.getFrameNum()&0xf) != Actor.standing)
				// Stand.
				npc.changeFrame(Actor.standing);
		}
		public Sleep(Actor n) {
			super(n);
			floorloc = new Tile(-1, -1, -1);
			if (game.isBG()) {
				spread0 = 3;
				spread1 = 16;
			} else {		// Serpent Isle.
				spread0 = 7;
				spread1 = 20;
			}
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			if (bed == null && state == 0 &&		// Always find bed.
					!npc.getFlag(GameObject.asleep)) {	// Unless flag already set
							// Find closest EW or NS bed.
				final int bedshapes[] = {696, 1011};
				standUp(npc);
				bed = npc.findClosest(bedshapes);
				if (bed == null && game.isBG()) {	// Check for Gargoyle beds.
					final int gbeds[] = {363, 312};
					bed = npc.findClosest(gbeds);
				}
			}
			int frnum = npc.getFrameNum();
			if ((frnum&0xf) == Actor.sleep_frame)
				return;			// Already sleeping.
			if (debug) System.out.println("sleep: state = " + state);
			switch (state) {
			case 0:				// Find path to bed.
				if (bed == null) {
							// Just lie down at current spot.
					int dirbits = npc.getFrameNum()&(0x30);
					npc.changeFrame(Actor.sleep_frame|dirbits);
					npc.forceSleep();
					return;
				}
				state = 1;
				Vector<GameObject> tops = new Vector<GameObject>();	
				// Want to find top of bed.
				bed.findNearby(tops, bed.getShapeNum(), 1, 0);
				for (GameObject top:tops) {
					frnum = top.getFrameNum();
					if (frnum >= spread0 && frnum <= spread1) {
						bed = top;
						break;
					}
				}
				Tile bloc = new Tile();
				bed.getTile(bloc);
				bloc.tz -= bloc.tz%5;	// Round down to floor level.
				ShapeInfo info = bed.getInfo();
				bloc.tx -= info.get3dXtiles(bed.getFrameNum())/2;
				bloc.ty -= info.get3dYtiles(bed.getFrameNum())/2;
							// Get within 3 tiles.
				PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 3);
				Tile pos = new Tile();
				npc.getTile(pos);
				System.out.println("sleep: creating path to bed");
				ActorAction pact = ActorAction.PathWalking.createPath(
						pos, bloc, cost);
				if (debug) System.out.println("sleep:  found path: " + 
						(pact != null?"true":"false"));
				if (pact != null)
					npc.setAction(pact);
				npc.start(1, 0);	// Start walking.
				break;
			case 1:				// Go to bed.
				npc.stop();		// Just to be sure.
				int bedshape = bed.getShapeNum();
				int dir = (bedshape == 696 || bedshape == 363) 
										? EConst.west : EConst.north;
				npc.setFrame(npc.getDirFramenum(dir, Actor.sleep_frame));
							// Get bed info.
				info = bed.getInfo();
				Tile bedloc = new Tile();
				bed.getTile(bedloc);
				npc.getTile(floorloc);
				int bedframe = bed.getFrameNum();// Unmake bed.
				if (bedframe >= spread0 && bedframe < spread1 && 
											(bedframe%2) != 0) {
					bedframe++;
					bed.changeFrame(bedframe);
					}
				boolean bedspread = (bedframe >= spread0 && (bedframe%2) == 0);
							// Put NPC on top of bed.
				npc.move(bedloc.tx, bedloc.ty, bedloc.tz + 
						(bedspread ? 0 : info.get3dHeight()));
				npc.forceSleep();
				state = 2;
				break;
			default:
				break;
			}
		}
		@Override
		public void ending(int newtype) {// Switching to another schedule.
			if (newtype == wait ||	
				// Not time to get up, Penumbra!
					newtype == sleep)
				return;			// ++++Does this leave NPC's stuck?
			if (bed != null &&			// Still in bed?
					(npc.getFrameNum()&0xf) == Actor.sleep_frame &&
					npc.distance(bed) < 8) {
				// Locate free spot.
				if (floorloc.tx == -1)
						// Want spot on floor.
					npc.getTile(floorloc);
				floorloc.tz -= floorloc.tz%5;
				Tile pos = new Tile(floorloc);
				boolean found = MapChunk.findSpot(pos, 6, npc.getShapeNum(), 
						Actor.standing, 0, -1, MapChunk.anywhere);
				if (!found)
					// Failed?  Allow change in lift.
					found = MapChunk.findSpot(pos, 6, npc.getShapeNum(), 
								Actor.standing, 1, -1, MapChunk.anywhere);
				floorloc = pos;
				if (!found)
					floorloc.tx = -1;
						// Make bed.
				int frnum = bed.getFrameNum();
				// Unless there's another occupant.
					
				if (frnum >= spread0 && frnum <= spread1 && (frnum%2) == 0) {
					Vector<GameObject> occ = new Vector<GameObject>();
					if (bed.findNearbyActors(occ, EConst.c_any_shapenum, 0) < 2)
						bed.setFrame(frnum - 1);
				}
			}
			if (floorloc.tx >= 0)		// Get back on floor.
				npc.move(floorloc);
			npc.clearSleep();
			npc.setFrame(Actor.standing);
			gwin.setAllDirty();		// Update all, since Av. stands up.
			state = 0;			// In case we go back to sleep.
		}
		@Override			// Set where to sleep.
		public void setBed(GameObject b)
			{ bed = b; state = 0; }
		};
	
	/*
	 *	Action to sit in the chair NPC is in front of.
	 */

	static class SitActorAction extends ActorAction.Frames {
		GameObject chair;		// Chair.
		Tile chairloc;		// Original chair location.
		Tile sitloc;		// Actually where NPC sits.
		byte frames[];
		static short offsets[] = {0,-1, 1,0, 0,1, -1,0};// Offsets where NPC should sit.
		static byte [] init(GameObject chairobj, Actor actor) {
						// Frame 0 faces N, 1 E, etc.
			int dir = 2*(chairobj.getFrameNum()%4);
			byte frames[] = new byte[2];
			frames[0] = (byte)actor.getDirFramenum(dir, Actor.bow_frame);
			frames[1] = (byte)actor.getDirFramenum(dir, Actor.sit_frame);
			return frames;
		}
		static boolean isOccupied(Tile sitloc, Actor actor)
			{
			Vector<GameObject> occ = new Vector<GameObject>();	// See if occupied.
			if (gmap.findNearby(occ, sitloc, EConst.c_any_shapenum, 0, 8) > 0) {
				for (GameObject npc : occ) {
					System.out.printf("isOccupied: found %1$d at %2$s\n",
							npc.getShapeNum(), sitloc);
					if (npc == actor)
						continue;
					int frnum = npc.getFrameNum() & 15;
					if (frnum == Actor.sit_frame ||
					    frnum == Actor.bow_frame)
						return true;
				}
			}
			/* Seems to work.  Added Nov. 2, 2001 */
			Tile pos = new Tile();
			actor.getTile(pos);
			if (pos.equals(sitloc))
				return false;	// We're standing there.
						// See if spot is blocked.
			pos.set(sitloc);// Careful, .tz gets updated.
			if (gmap.spotAvailable(actor.getInfo().get3dHeight(), 
					pos.tx, pos.ty, pos.tz, EConst.MOVE_WALK, 0, -1) < 0)
				return true;
			return false;
		}
		public SitActorAction(GameObject o, Actor actor) {
			super(init(o, actor), 2);
			chair = o;
			sitloc = new Tile(); chairloc = new Tile();
			chair.getTile(sitloc);
			chairloc.set(sitloc);
						// Frame 0 faces N, 1 E, etc.
			int nsew = o.getFrameNum()%4;
			sitloc.tx += offsets[2*nsew];
			sitloc.ty += offsets[2*nsew + 1];
		}
		Tile getSitloc()
			{ return sitloc; }
		static boolean isOccupied(GameObject chair, Actor actor) {
			int dir = 2*(chair.getFrameNum()%4);
			Tile t = new Tile();
			chair.getTile(t);
			t.tx += offsets[dir]; t.ty += offsets[dir + 1];
			return isOccupied(t, actor);
		}
						// Handle time event.
		public int handleEvent(Actor actor) {
			if (getIndex() == 0) {		// First time?
				if (isOccupied(sitloc, actor))
					return 0;	// Abort.
				if (chairloc.tx != chair.getTileX() || 
						chairloc.ty != chair.getTileY() ||
								chairloc.tz != chair.getLift()) {
					// Chair was moved!
					actor.say(ItemNames.first_chair_thief, ItemNames.last_chair_thief);
					return 0;
				}
			}
			return super.handleEvent(actor);
		}
	}
	/*
	 *	Sit in a chair.
	 */
	public static class Sit extends Schedule {
		GameObject chair;		// What to sit in.
		boolean sat;			// True if we already sat down.
		boolean did_barge_usecode;		// So we only call it once.
		public Sit(Actor n, GameObject ch) {
			super(n);
			chair = ch;
		}
		public void nowWhat() {	// Now what should NPC do?
			int frnum = npc.getFrameNum();
			if (chair != null && (frnum&0xf) == Actor.sit_frame && 
			    npc.distance(chair) <= 1) {			// Already sitting.
							// Seat on barge?
				if (chair.getInfo().getBargeType() != ShapeInfo.barge_seat)
					return;
				if (did_barge_usecode)
					return;		// But NOT more than once for party.
				did_barge_usecode = true;
				if (gwin.getMovingBarge() != null)
					return;		// Already moving.
				if (!npc.getFlag(Actor.in_party))
					return;		// Not a party member.
				int cnt = partyman.getCount();	// See if all sitting.
				for (int i = 0; i < cnt; i++) {
					Actor npc = gwin.getNpc(partyman.getMember(i));
					if ((npc.getFrameNum()&0xf) != Actor.sit_frame)
						return;	// Nope.
				}
							// Find barge.
				GameObject barge = chair.findClosest(961);
				if (barge == null)
					return;
				int usefun = 0x634;	// I hate using constants like this.
				did_barge_usecode = true;
							// Special usecode for barge pieces:
							// (Call with item=Avatar to avoid
							//   running nearby barges.)
				ucmachine.callUsecode(usefun, gwin.getMainActor(),
							UsecodeMachine.double_click);
				return;
			}
			System.out.println("Setting Sit action for NPC #" + npc.getNpcNum());
							// Wait a while if we got up.
			if ((chair = setAction(npc, chair, sat ? 
					(2000 + EUtil.rand()%3000)/TimeQueue.tickMsecs : 0)) == null)
				npc.start(1, 5000/TimeQueue.tickMsecs);	// Failed?  Try again later.
			else
				sat = true;
		}
		public static boolean isOccupied(GameObject chairobj, Actor actor) {
			return SitActorAction.isOccupied(chairobj, actor);
		}
		//	Return chair found.
		public static GameObject setAction(Actor actor, GameObject chairobj,
					int delay) {
			final int chairshapes[] = {873,292};
			Vector<GameObject> chairs = new Vector<GameObject>();
			if (chairobj == null) {			// Find chair if not given.
				actor.findClosest(chairs, chairshapes);
				System.out.println("setAction: checking chairs: " + chairs.size());
				for (GameObject ch : chairs) {
					if (!SitActorAction.isOccupied(ch, actor)) {
							// Found an unused one.
						chairobj = ch;
						break;
					}
				}
				if (chairobj == null)
					return null;
			} else if (SitActorAction.isOccupied(chairobj, actor))
				return null;		// Given chair is occupied.
			System.out.println("setAction: found chair for NPC #" + actor.getNpcNum());
			SitActorAction act = new SitActorAction(chairobj, actor);
							// Walk there, then sit.
			setActionSequence(actor, act.getSitloc(), act, false, delay);
			return chairobj;
		}
		public static GameObject setAction(Actor actor) {
			return setAction(actor, null, 0);
		}
	}
	/*
	 *	Desk work - Just sit in front of desk.
	 */
	public static class Desk extends Schedule {
		private GameObject chair;		// What to sit in.
		public Desk(Actor n) {
			super(n);
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			if (chair == null) {			// No chair found yet.
				final int desks[] = {283, 407};
				final int chairs[] = {873,292};
				Sleep.standUp(npc);
				GameObject desk = npc.findClosest(desks);
				if (desk != null)
					chair = desk.findClosest(chairs);
				if (chair == null) {		// Failed.
					// Try again in a few seconds.
					npc.start(1, 5000/TimeQueue.tickMsecs);
					return;	
				}
			}
			int frnum = npc.getFrameNum();
			if ((frnum&0xf) != Actor.sit_frame) {
				if (Sit.setAction(npc, chair, 0) == null) {
					chair = null;	// Look for any nearby chair.
					npc.start(1, 5000/TimeQueue.tickMsecs);	// Failed?  Try again later.
				} else
					npc.start(1, 0);
			} else {			// Stand up a second.
				byte frames[] = { 
						(byte)npc.getDirFramenum(Actor.standing),
						(byte)npc.getDirFramenum(Actor.bow_frame),
						(byte)npc.getDirFramenum(Actor.sit_frame) };
				npc.setAction(new ActorAction.Frames(frames, frames.length));
				npc.start(1, (10000 + EUtil.rand()%5000)/TimeQueue.tickMsecs);
			}
		}
	}
	/*
	 *	Shy away from Avatar.
	 */
	public static class Shy extends Schedule {
		private Tile npcpos = new Tile(), dest = new Tile();
		public Shy(Actor n) {
			super(n);
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			Actor av = gwin.getMainActor();
			int avtx = av.getTileX(), avty = av.getTileY();
			npc.getTile(npcpos);
						// How far away is Avatar?
			int dist = npc.distance(av);
			if (dist > 10) {			// Far enough?
			   			// Check again in a few seconds.
				if (EUtil.rand()%3 != 0)		// Just wait.
					npc.start(1, (1000 + EUtil.rand()%1000)/TimeQueue.tickMsecs);
				else {			// Sometimes wander.
					dest.set(npcpos.tx + EUtil.rand()%6 - 3,
						npcpos.ty + EUtil.rand()%6 - 3, npcpos.tz);
					npc.walkToTile(dest, 1, 0);
					return;
				}
			}
						// Get deltas.
			int dx = npcpos.tx - avtx, dy = npcpos.ty - avty;
			int adx = dx < 0 ? -dx : dx;
			int ady = dy < 0 ? -dy : dy;
						// Which is farthest?
			int farthest = adx < ady ? ady : adx;
			int factor = farthest < 2 ? 9 : farthest < 4 ? 4 
					: farthest < 7 ? 2 : 1;
						// Walk away.
			dest.set(npcpos.tx + dx*factor, npcpos.ty + dy*factor, npcpos.tz);
			dest.tx += EUtil.rand()%3;
			dest.ty += EUtil.rand()%3;
			PathFinder.MonsterClient cost = new PathFinder.MonsterClient(npc, dest, 4);
			ActorAction pact = ActorAction.PathWalking.createPath(
								npcpos, dest, cost);
			if (pact != null) {			// Found path?
				npc.setAction(pact);
				npc.start(1, 1 + EUtil.rand()%2);	// Start walking.
			} else					// Try again in a couple secs.
				npc.start(1, (500 + EUtil.rand()%1000)/TimeQueue.tickMsecs);
		}
	}
	/*
	 *	A class for indexing the perimeter of a rectangle.
	 */
	public static class Perimeter {
		Rectangle perim;		// Outside given rect.
		int sz;				// # squares.
		public Perimeter(Rectangle r) {
			sz = (2*r.w + 2*r.h + 4);
			perim = new Rectangle(r);
			perim.enlarge(1);
		}
		int size() { return sz; }
		/*
		 *	Get the i'th perimeter tile and the tile in the original rect.
		 *	that's adjacent.
		 */
		void get(int i, Tile ptile, Tile atile) {
			if (i < perim.w - 1) {		// Spiral around from top-left.
				ptile.set(perim.x + i, perim.y, 0);
				atile.set(ptile.tx + (i==0 ? 1 : 0), ptile.ty + 1, ptile.tz);
				return;
			}
			i -= perim.w - 1;
			if (i < perim.h - 1) {
				ptile.set(perim.x + perim.w - 1, perim.y + i, 0);
				atile.set(ptile.tx - 1, ptile.ty + (i==0 ? 1 : 0), ptile.tz);
				return;
			}
			i -= perim.h - 1;
			if (i < perim.w - 1) {
				ptile.set(perim.x + perim.w - 1 - i,
						perim.y + perim.h - 1, 0);
				atile.set(ptile.tx + (i==0 ? -1 : 0), ptile.ty - 1, ptile.tz);
				return;
			}
			i -= perim.w - 1; {
				ptile.set(perim.x, perim.y + perim.h - 1 - i, 0);
				atile.set(ptile.tx + 1, ptile.ty + (i==0 ? -1 : 0), ptile.tz);
				return;
			}
						// Bad index if here.
			//UNUSED get(i%sz, ptile, atile);
		}
	}
	/*
	 *	Lab work.
	 */
	public static class Lab extends Schedule {
		Tile npcpos = new Tile(), objpos = new Tile();
		private Vector<GameObject> tables = new Vector<GameObject>();
		private GameObject chair;		// Chair to sit in.
		private GameObject book;		// Book to read.
		private GameObject cauldron;
		private Tile spotOnTable = new Tile();
	 	private final int // enum {
			start = 0,
			walk_to_cauldron = 1,
			use_cauldron = 2,
			sit_down = 3,
			read_book = 4,
			stand_up = 5,
			walk_to_table = 6,
			use_potion = 7;
	 	private int state;
		private void init() {
			chair = book = null;
			cauldron = npc.findClosest(995, 20);
							// Find 'lab' tables.
			npc.findNearby(tables, 1003, 20, 0);
			npc.findNearby(tables, 1018, 20, 0);
			int cnt = tables.size();	// Look for book, chair.
			for (int i = 0; (book == null || chair == null) && i < cnt; i++) {
				final int chairs[] = {873,292};
				GameObject table = tables.elementAt(i);
				Rectangle foot = new Rectangle();
				table.getFootprint(foot);
							// Book on table?
				if (book == null && (book = table.findClosest(642, 4)) != null) {
					int tx = book.getTileX(), ty = book.getTileY();
					if (!foot.hasPoint(tx, ty))
						book = null;
				}
				if (chair == null)
					chair = table.findClosest(chairs, 4);
			}
		}
		
		public Lab(Actor n) {
			super(n);
			state = start;
			init();
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			GameObject p;
			npc.getTile(npcpos);
			int r, dir, frnum, delay = 1;	
			// Often want to get within 1 tile.
			PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 1);
			switch (state) {
			case start:
			default:
				if (cauldron == null) {		// Try looking again.
					init();
					if (cauldron == null)	// Again a little later.
						delay = 6000/TimeQueue.tickMsecs;
					break;
				}
				r = EUtil.rand()%5;	// Pick a state.
				if (r == 0)			// Sit less often.
					state = sit_down;
				else
					state = r <= 2 ? walk_to_cauldron : walk_to_table;
				break;
			case walk_to_cauldron:
				state = start;		// In case we fail.
				if (cauldron == null)
					break;
				cauldron.getTile(objpos);
				ActorAction pact = ActorAction.PathWalking.createPath(
					npcpos, objpos, cost);
				if (pact != null) {
					npc.setAction(new ActorAction.Sequence(pact,
								new ActorAction.FacePos(cauldron, 1)));
					state = use_cauldron;
				}
				break;
			case use_cauldron:
				dir = npc.getDirection(cauldron);
						// Set random frame (skip last frame).
				cauldron.changeFrame(EUtil.rand()%(cauldron.getNumFrames() -1));
				npc.changeFrame(npc.getDirFramenum(dir, Actor.bow_frame));
				r = EUtil.rand()%5;
				state = r == 0 ? use_cauldron : (r <= 2 ? sit_down : walk_to_table);
				break;
			case sit_down:
				if (chair == null || Sit.setAction(npc, chair, 1) == null)
					state = start;
				else
					state = read_book;
				break;
			case read_book:	
				state = stand_up;
				if (book == null || npc.distance(book) > 4)
					break;
						// Read a little while.
				delay = (1000 + 1000*(EUtil.rand()%5))/TimeQueue.tickMsecs;
						// Open book.
				frnum = book.getFrameNum();
				book.changeFrame(frnum - frnum%3);
				break;
			case stand_up:
				if (book != null && npc.distance(book) < 4) {		// Close book.
					frnum = book.getFrameNum();
					book.changeFrame(frnum - frnum%3 + 1);
				}
				state = start;
				break;
			case walk_to_table:
				state = start;		// In case we fail.
				int ntables = tables.size();
				if (ntables == 0)
					break;
				GameObject table = tables.elementAt(EUtil.rand()%ntables);
				Rectangle trect = new Rectangle();
				table.getFootprint(trect);
				Perimeter perim = new Perimeter(trect);		// Find spot adjacent to table.
				// Also get closest spot on table.
				perim.get(EUtil.rand()%perim.size(), objpos, spotOnTable);
				PathFinder.ActorClient cost0 = new PathFinder.ActorClient(npc, 0);
				pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost0);
				if (pact == null)
					break;		// Failed.
				ShapeInfo info = table.getInfo();
				spotOnTable.tz += info.get3dHeight();
				npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.FacePos(spotOnTable, 1)));
				state = use_potion;
				break;
			case use_potion:
				state = start;
				Vector<GameObject> potions = new Vector<GameObject>();
				gmap.findNearby(potions, spotOnTable, 340, 0, 0);
				if (!potions.isEmpty()) {	// Found a potion.  Remove it.
					gwin.addDirty(potions.elementAt(0));
					potions.elementAt(0).removeThis();
				} else {		// Create potion if spot is empty.
					objpos.set(spotOnTable);
					if (MapChunk.findSpot(objpos, 0, 340, 0, 0) && 
							objpos.tz == spotOnTable.tz) {
						// create a potion randomly, but don't use the last frame
						int nframes = ShapeFiles.SHAPES_VGA.getFile().getNumFrames(340) - 1;
						p = IregGameObject.create(
								ShapeID.getInfo(340), 340, EUtil.rand()%nframes, 0, 0, 0);
						p.move(objpos);
					}
				}
				dir = npc.getDirection(spotOnTable);
						// Reach out hand:
				byte frames[] = { (byte)npc.getDirFramenum(dir, 1),
								  (byte)npc.getDirFramenum(dir, Actor.standing) };
				npc.setAction(new ActorAction.Frames(frames, 2));
				break;
			}
		npc.start(1, delay);	// Back in queue.
		}
	}
	/*
	 *	Be a thief.
	 */
	public static class Thief extends Schedule {
		private Tile pos = new Tile(), dest = new Tile();
		private int nextStealTime;	// Next time we can try to steal.
		private void steal(Actor from) {
			npc.say(ItemNames.first_thief, ItemNames.last_thief);
			int shnum = EUtil.rand()%3;		// Gold coin, nugget, bar.
			GameObject obj = null;
			for (int i = 0; obj == null && i < 3; ++i) {
				obj = from.findItem(644+shnum, EConst.c_any_qual, EConst.c_any_framenum);
				shnum = (shnum + 1)%3;
			}
			if (obj != null) {
				obj.removeThis();
				npc.add(obj, false);
			}
		}
		public Thief(Actor n) {
			super(n);
			nextStealTime = 0;
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			int curtime = TimeQueue.ticks;
			Actor av = gwin.getMainActor();
			if (curtime < nextStealTime) {
							// Not time?  Wander.
				
				int centerx = av.getTileX(), centery = av.getTileY();;
				final int dist = 6;
				pos.set(centerx - dist + EUtil.rand()%(2*dist),
						centery - dist + EUtil.rand()%(2*dist),
						av.getLift());
							// Wait a bit.
				npc.walkToTile(pos, 
						2, (EUtil.rand()%4000)/TimeQueue.tickMsecs);
				return;
			}
			if (npc.distance(av) <= 1) {			// Next to Avatar.
				if (EUtil.rand()%3 != 0)
					steal(av);

				nextStealTime = (curtime+8000+EUtil.rand()%8000)/TimeQueue.tickMsecs;
				npc.start(1, (1000 + EUtil.rand()%2000)/TimeQueue.tickMsecs);
			} else {			// Get within 1 tile of Avatar.
				av.getTile(dest);
				npc.getTile(pos);
				PathFinder.MonsterClient cost = new PathFinder.MonsterClient(npc, dest, 1);
				ActorAction pact = ActorAction.PathWalking.createPath(pos, dest, cost);
				if (pact != null) {		// Found path?
					npc.setAction(pact);
					npc.start(1, (1000 + EUtil.rand()%1000)/TimeQueue.tickMsecs);
				} else			// Try again in a couple secs.
					npc.start(1, (2000 + EUtil.rand()%2000)/TimeQueue.tickMsecs);
			}
		}
	}

	/*
	 *	Wait tables.
	 */
	public static class Waiter extends Schedule {
		static boolean debug = false;
		Tile startPos;		// Starting position.
		Actor customer;		// Current customer.
		GameObject prepTable;	// Table we're working at.
		Vector<GameObject> customers;	// List of customers.
		Vector<GameObject> prepTables;// Prep. tables.
		Vector<GameObject> eatingTables;// Tables with chairs around them.
		final static int
			waiter_setup = 0,
			get_customer = 1,
			get_order = 2,
			prep_food = 3,
			serve_food = 4;
		int state;
		boolean findCustomer() {
			if (debug) System.out.println("findCustomer");
			if (customers == null || customers.isEmpty()) {			// Got to search?
				customers = new Vector<GameObject>();
						// Look within 32 tiles;
				npc.findNearbyActors(customers, EConst.c_any_shapenum, 32);
				Iterator<GameObject> iter = customers.iterator();
				while (iter.hasNext()) {
					Actor each = iter.next().asActor();
					if (each == null || each.getScheduleType() !=
														Schedule.eat_at_inn)
						iter.remove();
				}
			}
			if (!customers.isEmpty()) {
				customer = customers.remove(customers.size() - 1).asActor();
			}
			return customer != null;
		}
		void findTables(int shapenum) {
			eatingTables = new Vector<GameObject>();
			npc.findNearby(eatingTables, shapenum, 32, 0);
			Vector<GameObject> chairs = new Vector<GameObject>();
			int floor = npc.getLift()/5;	// Make sure it's on same floor.
			Iterator<GameObject> iter = eatingTables.iterator();
			while (iter.hasNext()) {
				GameObject table = iter.next();
				if (table.getLift()/5 != floor) {
					iter.remove();
					continue;
				}
				chairs.setSize(0);;		// No chairs by it?
				if (table.findNearby(chairs, 873, 3, 0) == 0 &&
					table.findNearby(chairs, 292, 3, 0) == 0) {
					if (prepTables == null)
						prepTables = new Vector<GameObject>();
					prepTables.add(table);
					iter.remove();
				}
			}
		}
		boolean walkToCustomer(int min_delay) {	
			if (debug) System.out.println("walkToCustomer");
			if (customer != null) {
				if (customer.getScheduleType() != Schedule.eat_at_inn)
				// Customer schedule changed. Tell schedule to refresh the list
				// (this happens with Hawk & others in SI).
					customers.clear();
				else {
					Tile dest = new Tile();
					customer.getTile(dest);
					if (MapChunk.findSpot(dest, 3, npc) &&
							npc.walkPathToTile(dest, 1, min_delay + 
								(EUtil.rand()%1000)/TimeQueue.tickMsecs,0))
						return true;		// Walking there.
				}
			}
			// Failed so try again later.
			npc.start(2, (2000 + EUtil.rand()%4000)/TimeQueue.tickMsecs);	
			return false;
		}
		boolean walkToPrep() {
			if (debug) System.out.println("walkToPrep");
			Tile pos = new Tile();
			if (prepTables != null)	{	// Walk to a 'prep' table.
				prepTable = prepTables.elementAt(EUtil.rand()%prepTables.size());
				prepTable.getTile(pos);
				if (MapChunk.findSpot(pos, 1, npc) &&
				    npc.walkPathToTile(pos, 1, 
								(1000 + EUtil.rand()%1000)/TimeQueue.tickMsecs, 0))
					return true;
			} else
				prepTable = null;
			final int dist = 8;		// Bad luck?  Walk randomly.
			pos.set(startPos.tx - dist + EUtil.rand()%(2*dist),
					startPos.ty - dist + EUtil.rand()%(2*dist), startPos.tz);
			npc.walkToTile(pos, 2, (EUtil.rand()%2000)/TimeQueue.tickMsecs, 0);
			return false;
		}
		//	Return plate if found, with spot set.
		GameObject findServingSpot(Tile spot) {
			if (debug) System.out.println("findServingSpot");
			GameObject plate = null;
			Vector<GameObject> plates = new Vector<GameObject>();
			int cnt = npc.findNearby(plates, 717, 1, 0);
			if (cnt == 0)
				cnt = npc.findNearby(plates, 717, 2, 0);
			int floor = npc.getLift()/5;	// Make sure it's on same floor.
			for (GameObject p : plates) {
				if (p.getLift()/5 == floor) {
					p.getTile(spot);
					spot.tz++;	// Just above plate.
					return p;
				}
			}
			Tile cpos = new Tile();
			customer.getTile(cpos);			
				// Go through tables.
			if (eatingTables == null)
				return null;
			Rectangle foot = new Rectangle();
			for (GameObject table : eatingTables) {
			
				table.getFootprint(foot);
				if (foot.distance(cpos.tx, cpos.ty) > 2)
					continue;
							// Found it.
				spot = cpos;		// Start here.
							// East/West of table?
				if (cpos.ty >= foot.y && cpos.ty < foot.y + foot.h)
					spot.tx = (short)(cpos.tx <= foot.x ? foot.x
									: foot.x + foot.w - 1);
				else			// North/south.
					spot.ty = (short)(cpos.ty <= foot.y ? foot.y
								: foot.y + foot.h - 1);
				if (foot.hasPoint(spot.tx, spot.ty)) {		// Passes test.
					ShapeInfo info = table.getInfo();
					spot.tz = (short)(table.getLift() + info.get3dHeight());
					plate = IregGameObject.create(717, 0);
					plate.move(spot);
					spot.tz++;	// Food goes above plate.
					return plate;
				}
			}
			return null;			// Failed.
		}
		public Waiter(Actor n) {
			super(n);
			startPos = new Tile();
			n.getTile(startPos);
			state = waiter_setup;
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			GameObject food;
			if (state == get_customer &&
			    EUtil.rand() % 4 == 0)		// Check for lamps, etc.
				if (tryStreetMaintenance())
					return;		// We no longer exist.
			if (state == get_order || state == serve_food) {
				int dist = customer != null ? npc.distance(customer) : 5000;
				if (dist > 32) {	// Need a new customer?
					state = get_customer;
					npc.start(1, (1000 + EUtil.rand()%1000)/TimeQueue.tickMsecs);
					return;
				}
							// Not close enough, so try again.
				if (dist >= 3 && !walkToCustomer(0)) {
					state = get_customer;
					return;
				}
			}
			switch (state) {
			case waiter_setup:
				findTables(971);
				findTables(633);
				findTables(847);
				findTables(1003);
				findTables(1018);
				findTables(890);
				findTables(964);
				findTables(333);
				state = get_customer;
				/* FALL THROUGH */
			case get_customer:
				if (!findCustomer()) {
					walkToPrep();
					state = prep_food;
				} else if (walkToCustomer(0))
					state = get_order;
				break;
			case get_order: {
				Vector<GameObject> foods = new Vector<GameObject>();
							// Close enough to customer?
				if (customer.findNearby(foods, 377, 2, 0) > 0) {
					if (EUtil.rand()%4 != 0)
						npc.say(ItemNames.first_waiter_banter, 
								ItemNames.last_waiter_banter);
					state = get_customer;
					npc.start(1, (1000 + EUtil.rand()%2000)/TimeQueue.tickMsecs);
					break;
				}
							// Ask for order.
				npc.say(ItemNames.first_waiter_ask, ItemNames.last_waiter_ask);
				walkToPrep();
				state = prep_food;
				break;
			}
			case prep_food:
				if (prepTable != null && npc.distance(prepTable) <= 3) {
					npc.changeFrame(npc.getDirFramenum(
						npc.getFacingDirection(prepTable),
									Actor.standing));
					UsecodeScript scr = new UsecodeScript(npc);
					scr.add(UsecodeScript.face_dir, npc.getDirFacing());
					for (int cnt = 1 + EUtil.rand()%3; cnt != 0; --cnt) {
						scr.add(UsecodeScript.npc_frame + Actor.ready_frame,
								UsecodeScript.delay_ticks, 1,
								UsecodeScript.npc_frame + Actor.raise1_frame,
								UsecodeScript.delay_ticks, 1);
					}
					scr.add(UsecodeScript.npc_frame + Actor.standing);
					scr.start();	// Start next tick.
				}
				if (npc.getReadied(Ready.lhand) == null) {
							// Acquire some food.
					int nfoods = ShapeFiles.SHAPES_VGA.getFile().getNumFrames(377);
					int frame = EUtil.rand()%nfoods;
					food = new IregGameObject(377, frame, 0, 0, 0);
					npc.addReadied(food, Ready.lhand);
				}
				if (!walkToCustomer(3000/TimeQueue.tickMsecs)) {
					state = get_customer;
					if (EUtil.rand()%3 == 0)
						ucmachine.callUsecode(
							npc.getUsecode(), npc,
							UsecodeMachine.npc_proximity);
				} else {
					state = serve_food;
				}
				break;
			case serve_food:
				food = npc.getReadied(Ready.lhand);
				Tile spot = new Tile();
				if (food != null && food.getShapeNum() == 377 &&
				    findServingSpot(spot) != null) {
					npc.changeFrame(npc.getDirFramenum(
						npc.getDirection(customer),
									Actor.standing));
					npc.remove(food);
					food.setInvalid();
					food.move(spot);
					if (EUtil.rand()%3 != 0)
						npc.say(ItemNames.first_waiter_serve,
								ItemNames.last_waiter_serve);
					UsecodeScript scr = new UsecodeScript(npc);
					scr.add(UsecodeScript.face_dir, npc.getDirFacing(),
							UsecodeScript.npc_frame + Actor.ready_frame,
							UsecodeScript.delay_ticks, 2,
							UsecodeScript.npc_frame + Actor.standing);
					scr.start();	// Start next tick.
				}
				state = get_customer;
				customer = null;		// Done with this one.
				npc.start(1, (1000 + EUtil.rand()%2000)/TimeQueue.tickMsecs);
				return;
			}

		}
		@Override
		public void ending(int newtype) {// Switching to another schedule.
			// Remove what he/she is carrying.
			GameObject obj = npc.getReadied(Ready.lhand);
			if (obj != null)
				obj.removeThis();
			obj = npc.getReadied(Ready.rhand);
			if (obj != null)
				obj.removeThis();
		}
	}
	/*
	 *	Sew/weave schedule.
	 */
	public static class Sew extends Schedule {
		private Tile npcpos = new Tile(), objpos = new Tile();
		private Rectangle foot = new Rectangle();
		private GameObject bale;		// Bale of wool.
		private GameObject spinwheel;
		private GameObject chair;		// In front of spinning wheel.
		private GameObject spindle;		// Spindle of thread.
		private GameObject loom;
		private GameObject cloth;
		private GameObject work_table, wares_table;
		private int sew_clothes_cnt;
	 	private final static int // enum {
			get_wool = 0,
			sit_at_wheel = 1,
			spin_wool = 2,
			get_thread = 3,
			weave_cloth = 4,
			get_cloth = 5,
			to_work_table = 6,
			set_to_sew = 7,
			sew_clothes = 8,
			get_clothes = 9,
			display_clothes = 10,
			done = 11;
		private int state;
		public Sew(Actor n) {
			super(n);
			state = get_wool;
		}
		@Override
		public void nowWhat() {
			npc.getTile(npcpos);
							// Often want to get within 1 tile.
			PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 1);
			ActorAction pact;
			switch (state) {
			case get_wool:
				if (spindle != null)		// Clean up any remainders.
					spindle.removeThis();
				if (cloth != null)
					cloth.removeThis();
				cloth = spindle = null;
				npc.removeQuantity(2, 654, EConst.c_any_qual, EConst.c_any_framenum);
				npc.removeQuantity(2, 851, EConst.c_any_qual, EConst.c_any_framenum);

				bale = npc.findClosest(653);
				if (bale == null) {		// Just skip this step.
					state = sit_at_wheel;
					break;
				}
				bale.getTile(objpos);
				pact = ActorAction.PathWalking.createPath(
						npcpos, objpos, cost);
				if (pact != null)
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.Pickup(bale, 1),
						new ActorAction.Pickup(bale, objpos, 1, false), null));
				state = sit_at_wheel;
				break;
			case sit_at_wheel:
				chair = npc.findClosest(873);
				if (chair == null || Sit.setAction(npc, chair, 1) == null) {
					// uh-oh... try again in a few seconds
					npc.start(1, 10);
					return;
				}
				state = spin_wool;
				break;
			case spin_wool:			// Cycle spinning wheel 8 times.
				spinwheel = npc.findClosest(651);
				if (spinwheel == null) {
					// uh-oh... try again in a few seconds?
					npc.start(1, 10);
					return;
				}
				npc.setAction(new ActorAction.ObjectAnimate(spinwheel,
										8, 1));
				state = get_thread;
				break;
			case get_thread:
				spinwheel.getTile(objpos);
				if (MapChunk.findSpot(objpos, 1, 654, 0, 0)) {
					// Space to create thread?
					spindle = new IregGameObject(654, 0, 0, 0, 0);
					spindle.move(objpos);
					gwin.addDirty(spindle);
					npc.setAction(new ActorAction.Pickup(spindle, 1));
				}
				state = weave_cloth;
				break;
			case weave_cloth:
				if (spindle != null)		// Should be held by NPC.
					spindle.removeThis();
				spindle = null;
				loom = npc.findClosest(261);
				if (loom == null) {		// No loom found?
					state = get_wool;
					break;
				}
				loom.getTile(objpos);
				objpos.tx--;
				pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost);
				if (pact != null)
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.FacePos(loom, 1),
						new ActorAction.ObjectAnimate(loom, 4, 1), null));
				state = get_cloth;
				break;
			case get_cloth:
				loom.getTile(objpos);
				if (MapChunk.findSpot(objpos, 1, 851, 0, 0)) {
						// Space to create it?
					cloth = new IregGameObject(851, EUtil.rand()%2, 0, 0, 0);
					cloth.move(objpos);
					gwin.addDirty(cloth);
					npc.setAction(
						new ActorAction.Pickup(cloth, 1));
					}
				state = to_work_table;
				break;
			case to_work_table:
				work_table = npc.findClosest(971);
				if (work_table == null || cloth == null) {
					state = get_wool;
					break;
				}
				work_table.getTile(objpos);
				objpos.tx += 1; objpos.ty -= 2;
				pact = ActorAction.PathWalking.createPath(
							npcpos, objpos, cost);
							// Find where to put cloth.
				work_table.getFootprint(foot);
				ShapeInfo info = work_table.getInfo();
				objpos.set(foot.x + foot.w/2, foot.y + foot.h/2,
					work_table.getLift() + info.get3dHeight());
				if (pact != null)
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.FacePos(work_table, 1),
						new ActorAction.Pickup(cloth, objpos, 1, false), null));
				state = set_to_sew;
				break;
			case set_to_sew:
				GameObject shears = npc.getReadied(Ready.lhand);
				if (shears != null && shears.getShapeNum() != 698) {
							// Something's not right.
					shears.removeThis();
					shears = null;
				}
				if (shears == null) {
							// Shears on table?
					Vector<GameObject> vec = new Vector<GameObject>();
					if (npc.findNearby(vec, 698, 3, 0) > 0) {
						shears = vec.firstElement();
						gwin.addDirty(shears);
						shears.removeThis();
					}
					else
						shears = new IregGameObject(698, 0, 0, 0, 0);
					npc.addReadied(shears, Ready.lhand);
				}
				state = sew_clothes;
				sew_clothes_cnt = 0;
				break;
			case sew_clothes:
				int dir = npc.getDirection(cloth);
				byte frames[] = new byte[5];
				int nframes = npc.getAttackFrames(698, false, 
										dir, frames);
				if (nframes > 0)
					npc.setAction(new ActorAction.Frames(frames, nframes));
				sew_clothes_cnt++;
				if (sew_clothes_cnt > 1 && sew_clothes_cnt < 5) {
					int num_cloth_frames = ShapeFiles.SHAPES_VGA.getFile().getNumFrames(851);
					cloth.changeFrame(EUtil.rand()%num_cloth_frames);
				} else if (sew_clothes_cnt == 5) {
					gwin.addDirty(cloth);
					cloth.getTile(objpos);
					cloth.removeThis();
							// Top or pants.
					int shnum = game.isSI() ? 403 : (EUtil.rand()%2 != 0 ? 738 : 249);
					cloth.setShape(shnum);
					nframes = ShapeFiles.SHAPES_VGA.getFile().getNumFrames(shnum);
					cloth.setFrame(EUtil.rand()%nframes);
					cloth.move(objpos);
					state = get_clothes;
				}
				break;
			case get_clothes:
				shears = npc.getReadied(Ready.lhand);
				if (shears != null) {
					cloth.getTile(objpos);
					npc.setAction(new ActorAction.Sequence(
									new ActorAction.Pickup(cloth, 1),
									new ActorAction.Pickup(shears, objpos, 1, false)));
				} else {
					// ++++ maybe create shears? anyway, leaving this till after
					// possible/probable schedule system rewrite
					npc.setAction(new ActorAction.Pickup(cloth, 1));
				}
				state = display_clothes;
				break;
			case display_clothes:
				state = done;
				wares_table = npc.findClosest(890);
				if (wares_table == null) {
					cloth.removeThis();
					cloth = null;
					break;
				}
				wares_table.getTile(objpos);
				objpos.tx += 1; objpos.ty -= 2;
				pact = ActorAction.PathWalking.createPath(
							npcpos, objpos, cost);
							// Find where to put cloth.
				wares_table.getFootprint(foot);
				info = wares_table.getInfo();
				objpos.set(foot.x + EUtil.rand()%foot.w, foot.y + EUtil.rand()%foot.h,
					wares_table.getLift() + info.get3dHeight());
				if (pact != null)
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.Pickup(cloth, objpos, 1, true)));
				cloth = null;			// Leave it be.
				break;
			case done:				// Just put down clothing.
				state = get_wool;
				Vector<GameObject> vec = new Vector<GameObject>();// Don't create too many.
				int cnt = 0;
				if (game.isSI())
					cnt += npc.findNearby(vec, 403, 5, 0);
				else {			// BG shapes.
					cnt += npc.findNearby(vec, 738, 5, 0);
					cnt += npc.findNearby(vec, 249, 5, 0);
				}
				if (cnt >= 3) {
					GameObject obj = vec.elementAt(EUtil.rand()%cnt);
					gwin.addDirty(obj);
					obj.removeThis();
				}
				break;
			default:			// Back to start.
				state = get_wool;
				break;
			}
			npc.start(1, 1);		// Back in queue.
		}
		@Override
		public void ending(int newtype) {// Switching to another schedule.
			GameObject obj = npc.getReadied(Ready.lhand);
			if (obj != null)
				obj.removeThis();
			if (cloth != null) {		// Don't leave cloth lying around.
				if (cloth.getOwner() == null)
					gwin.addDirty(cloth);
				cloth.removeThis();
				cloth = null;
			}
		}
	}
	/*
	 *	Bake schedule
	 */
	public static class Bake extends Schedule {
		private Tile npcpos = new Tile();
		private GameObject oven;
		private GameObject worktable;
		private GameObject displaytable;
		private GameObject flourbag;
		private GameObject dough;
		private GameObject dough_in_oven;
		private boolean clearing;
		private final static int // enum {
			find_leftovers = 0,		// Look for misplaced dough already made by this schedule
			to_flour = 1,			// Looks for flourbag and walks to it if found
			get_flour = 2,			// Bend over flourbag and change the frame to zero if nonzero
			to_table = 3,			// Walk over to worktable and create flour
			make_dough = 4,			// Changes flour to flat dough then dough ball
			remove_from_oven = 5,	// Changes dough in oven to food %7 and picks it up
			display_wares = 6,		// Walk to displaytable. Put food on it. If table full, go to
									// clear_display which eventualy comes back here to place food
			clear_display = 7,		// Mark food for deletion by remove_food
			remove_food = 8,		// Delete food on display table one by one with a slight delay
			get_dough = 9,			// Walk to work table and pick up dough
			put_in_oven = 10;			// Walk to oven and put dough on in.
		private int state;
		public Bake(Actor n) {
			super(n);
			state = find_leftovers;
		}
		@Override
		public void nowWhat() {
			npc.getTile(npcpos);
			ActorAction pact;
			PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 1);
			PathFinder.ActorClient cost2 = new PathFinder.ActorClient(npc, 2);
			int dir, delay = 1;
			int dough_shp = (game.isSI() ? 863 : 658);
			GameObject stove = npc.findClosest(664);

			switch (state) {
			case find_leftovers:	// Look for misplaced dough already made by this schedule
				state = to_flour;
				if (dough_in_oven == null) {
					// look for baking dough
					Vector<GameObject> baking_dough = new Vector<GameObject>();
					int frnum = (game.isSI() ? 18 : 2);
					gmap.findNearby(baking_dough, npcpos, dough_shp, 20, 0, 51, frnum);
					if (!baking_dough.isEmpty()) {	// found dough
						dough_in_oven = baking_dough.firstElement();
						state = remove_from_oven;
						break;
					}
					// looking for cooked food left in oven
					oven = npc.findClosest(831);
					if (oven == null)
						oven = stove;
					if (oven != null) {
						Vector<GameObject> food = new Vector<GameObject>();
						Tile Opos = new Tile();
						oven.getTile(Opos);
						gmap.findNearby(food, Opos, 377, 2, 0, 51, EConst.c_any_framenum);
						if (!food.isEmpty()) {	// found food
							dough_in_oven = food.firstElement();
							state = remove_from_oven;
							break;
						}
					}
				}
				if (dough == null) {		// Looking for unused dough on tables
					Vector<GameObject> leftovers = new Vector<GameObject>();
					if (game.isSI()) {
						gmap.findNearby(leftovers, npcpos, dough_shp, 20, 0, 50, 16);
						gmap.findNearby(leftovers, npcpos, dough_shp, 20, 0, 50, 17);
						gmap.findNearby(leftovers, npcpos, dough_shp, 20, 0, 50, 18);
					}
					else
						gmap.findNearby(leftovers, npcpos, dough_shp, 20, 0, 50, 
								EConst.c_any_framenum);
					if (!leftovers.isEmpty()) {	// found dough
						dough = leftovers.firstElement();
						state = make_dough;
						delay = 0;
						Tile t = new Tile();
						dough.getTile(t);
						pact = ActorAction.PathWalking.createPath(npcpos, t, cost2);
						if (pact != null)	// walk to dough if we can
							npc.setAction(pact);
					}
				}
				break;
			case to_flour:		// Looks for flourbag and walks to it if found
				Vector<GameObject> items = new Vector<GameObject>();
				gmap.findNearby(items, npcpos, 863, -1, 0, EConst.c_any_qual, 0);
				gmap.findNearby(items, npcpos, 863, -1, 0, EConst.c_any_qual, 13);
				gmap.findNearby(items, npcpos, 863, -1, 0, EConst.c_any_qual, 14);

				if (items.isEmpty()) {
					state = to_table;
					break;
				}

				int nr = EUtil.rand()%items.size();
				flourbag = items.elementAt(nr);

				Tile tpos = new Tile();
				flourbag.getTile(tpos);
				pact = ActorAction.PathWalking.createPath(
							npcpos, tpos, cost);
				if (pact != null) {
					npc.setAction(pact);
				} else {
					// just ignore it
					state = to_table;
					break;
				}

				state = get_flour;
				break;
			case get_flour:		// Bend over flourbag and change the frame to zero if nonzero
				if (flourbag == null) {
					// what are we doing here then? back to start
					state = to_flour;
					break;
				}

				dir = npc.getDirection(flourbag);
				npc.changeFrame(npc.getDirFramenum(dir,Actor.bow_frame));

				if (flourbag.getFrameNum() != 0)
					flourbag.changeFrame(0);

				delay = 750;
				state = to_table;
				break;
			case to_table:		// Walk over to worktable and create flour
				GameObject table1 = npc.findClosest(1003);
				GameObject table2 = npc.findClosest(1018);
				if (stove != null) {
					Vector<GameObject> table = new Vector<GameObject>();
					gmap.findNearby(table, npcpos, 890, -1, 0, EConst.c_any_qual, 5);
					if (table.size() == 1)
						worktable = table.firstElement();
					else if (table.size() > 1) {
						if (EUtil.rand()%2 != 0)
							worktable = table.firstElement();
						else
							worktable = table.elementAt(1);
					}
				}
				else if (table1 == null)
					worktable = table2;
				else if (table2 == null)
					worktable = table1;
				else if (table1.distance(npc) < table2.distance(npc))
					worktable = table1;
				else
					worktable = table2;

				if (worktable == null)
					worktable = npc.findClosest(1018);
				if (worktable == null) {
					// problem... try again in a few seconds
					delay = 2500/TimeQueue.tickMsecs;
					state = to_flour;
					break;
				}

							// Find where to put dough.
				Rectangle foot = new Rectangle();
				worktable.getFootprint(foot);
				ShapeInfo info = worktable.getInfo();
				Tile cpos = new Tile(foot.x + EUtil.rand()%foot.w, foot.y + EUtil.rand()%foot.h,
					worktable.getLift() + info.get3dHeight());
				Tile tablepos = new Tile(cpos);
				cpos.tz = 0;

				pact = ActorAction.PathWalking.createPath(
							npcpos, cpos, cost);
				if (pact != null) {
					if (dough != null) {
						dough.removeThis();
						dough = null;
					}
					if (game.isSI())
						dough = new IregGameObject(dough_shp, 16, 0, 0, 0);
					else
						dough = new IregGameObject(dough_shp, 0, 0, 0, 0);
					dough.setQuality(50);
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.Pickup(dough,tablepos,1,false)));
				} else {
					// not good... try again
					delay = 2500/TimeQueue.tickMsecs;
					state = to_flour;
					break;
				}

				state = make_dough;
				break;
			case make_dough:	// Changes flour to flat dough then dough ball
				if (dough == null) {
					// better try again...
					delay = 2500/TimeQueue.tickMsecs;
					state = to_table;
					break;
				}

				dir = npc.getDirection(dough);
				byte fr[] = new byte[2];
				fr[0] = (byte)npc.getDirFramenum(dir, 3);
				fr[1] = (byte)npc.getDirFramenum(dir, 0);

				int frame = dough.getFrameNum();
				if (game.isSI() ? frame == 16: frame == 0)
					npc.setAction(new ActorAction.Sequence(
						new ActorAction.Frames(fr, 2, 2, null),
					((game.isSI()) ?
						new ActorAction.Frames(0x11,1,dough) :
						new ActorAction.Frames(0x01,1,dough)),
						new ActorAction.Frames(fr, 2, 2, null),
					((game.isSI()) ?
						new ActorAction.Frames(0x12,1,dough) :
						new ActorAction.Frames(0x02,1,dough))
					));
				else if (game.isSI() ? frame == 17: frame == 1)
					npc.setAction(new ActorAction.Sequence(
						new ActorAction.Frames(fr, 2, 2, null),
						((game.isSI()) ?
						new ActorAction.Frames(0x12,1,dough) :
						new ActorAction.Frames(0x02,1,dough))
						));
				
				state = remove_from_oven;
				break;
			case remove_from_oven:	// Changes dough in oven to food %7 and picks it up
				if (dough_in_oven == null) {
					// nothing in oven yet
					state = get_dough;
					break;
				}
				if (stove != null)
					oven = stove;
				else
					oven = npc.findClosest(831);
				if (oven == null) {
					// this really shouldn't happen...
					dough_in_oven.removeThis();
					dough_in_oven = null;

					delay = 2500/TimeQueue.tickMsecs;
					state = to_table;
					break;
				}

				if (dough_in_oven.getShapeNum() != 377){
					gwin.addDirty(dough_in_oven);
					dough_in_oven.setShape(377);
					dough_in_oven.setFrame(EUtil.rand()%7);
					gwin.addDirty(dough_in_oven);
				}
				tpos = new Tile();
				oven.getTile(tpos);
				tpos.tx++;
				tpos.ty++;
				pact = ActorAction.PathWalking.createPath(
							npcpos, tpos, cost);
				if (pact != null) {
					npc.setAction(new ActorAction.Sequence(
						pact,
						new ActorAction.Pickup(dough_in_oven, 1)));
				} else {
					// just pick it up
					npc.setAction(
						new ActorAction.Pickup(dough_in_oven, 1));
				}

				state = display_wares;
				break;
			case display_wares:		// Walk to displaytable. Put food on it. If table full, go to
									// clear_display which eventualy comes back here to place food
				if (dough_in_oven == null) {
					// try again
					delay = 2500/TimeQueue.tickMsecs;
					state = find_leftovers;
					break;
				}
				displaytable = npc.findClosest(633); // Britain
				if (displaytable == null) {
					Vector<GameObject> table = new Vector<GameObject>();
					int table_shp = 890; // Moonshade table
					int table_frm = 1;
					if (stove != null) {
						table_shp = 1003;
						table_frm = 2;
					}
					gmap.findNearby(table, npcpos, table_shp, -1, 0, 
													EConst.c_any_qual, table_frm);
					if (table.size() == 1)
						displaytable = table.firstElement();
					else if (table.size() > 1)
					{
						if (EUtil.rand()%2 != 0)
							displaytable = table.firstElement();
						else
							displaytable = table.elementAt(1);
					}
				}
				if (displaytable == null) {
					// uh-oh...
					dough_in_oven.removeThis();
					dough_in_oven = null;

					delay = 2500/TimeQueue.tickMsecs;
					state = find_leftovers;
					break;
				}
				Rectangle r = new Rectangle();
				displaytable.getFootprint(r);
				Perimeter p = new Perimeter(r);		// Find spot adjacent to table.
				Tile spot = new Tile();	// Also get closest spot on table.
				Tile spot_on_table = new Tile();
				p.get(EUtil.rand()%p.size(), spot, spot_on_table);
				PathFinder.ActorClient COST = (game.isSI() ? cost : cost2);
				pact = ActorAction.PathWalking.createPath(npcpos, spot, COST);
				info = displaytable.getInfo();
				spot_on_table.tz += info.get3dHeight();

				// Place baked goods if spot is empty.
				Tile t = new Tile(spot_on_table);
				if (MapChunk.findSpot(t, 0, 377, 0, 0) && t.tz == spot_on_table.tz) {
					npc.setAction(new ActorAction.Sequence(pact,
							new ActorAction.Pickup(dough_in_oven,
									 spot_on_table, 1, false)));
					dough_in_oven = null;
					state = get_dough;
				} else {
					displaytable.getTile(t);
					pact = ActorAction.PathWalking.createPath(npcpos, t, cost);
					npc.setAction(new ActorAction.Sequence(pact, 
												new ActorAction.FacePos(t, 1)));
					delay = 1;
					state = clear_display;
				}		
				clearing = false;
				break;
			case clear_display:		// Mark food for deletion by remove_food
				Vector<GameObject> food = new Vector<GameObject>();
				gmap.findNearby(food, npcpos, 377, 4, 0, 51, EConst.c_any_framenum);
				if (food.size() == 0 && !clearing) { // none of our food on the table
											   // so we can't clear it
					if (dough_in_oven != null)
						dough_in_oven.removeThis();
					dough_in_oven = null;
					state = get_dough;
					break;
				}
				clearing = true;
				if (!food.isEmpty()) {
					delay = 2;
					state = remove_food;
					break;
				}
				if (food.size() == 0)
					state = display_wares;
				break;
			case remove_food:	// Delete food on display table one by one with a slight delay
				Vector<GameObject> food2 = new Vector<GameObject>();
				gmap.findNearby(food2, npcpos, 377, 4, 0, 51, EConst.c_any_framenum);
				if (food2.size() > 0) {
					delay = 2;
					state = clear_display;
					gwin.addDirty(food2.firstElement());
					food2.firstElement().removeThis();
				}
				break;
			case get_dough:		// Walk to work table and pick up dough
				if (dough == null) {
					// try again
					delay = 2500/TimeQueue.tickMsecs;
					state = find_leftovers;
					break;
				}
				if (stove != null)
					oven = stove;
				else
					oven = npc.findClosest(831);
				if (oven == null) {
					// wait a while
					delay = 2500/TimeQueue.tickMsecs;
					state = find_leftovers;
					break;
				}

				tpos = new Tile();
				dough.getTile(tpos);
				pact = ActorAction.PathWalking.createPath(
							npcpos, tpos, cost2);
				if (pact != null) {
					npc.setAction(new ActorAction.Sequence(pact,
								new ActorAction.Pickup(dough, 1)));
				} else {
					// just pick it up
					npc.setAction(new ActorAction.Pickup(dough, 1));
				}

				state = put_in_oven;
				break;
			case put_in_oven:	// Walk to oven and put dough on in.
				if (dough == null) {
					// try again
					delay = 2500/TimeQueue.tickMsecs;
					state = find_leftovers;
					break;
				}
				if (stove != null)
					oven = stove;
				else
					oven = npc.findClosest(831);
				if (oven == null) {
					// oops... retry
					dough.removeThis();
					dough = null;

					delay = 2500/TimeQueue.tickMsecs;
					state = to_table;
					break;
				}

				tpos = new Tile();
				oven.getTile(tpos);
				tpos.tx++; tpos.ty++; 
				pact = ActorAction.PathWalking.createPath(npcpos, tpos, cost);

				// offsets for oven placement
				int offX = +1, offY = 0, offZ = 0;
				if (stove != null) { // hide dough
					offX = -3; offY = 0; offZ = -2;
				}
				foot = new Rectangle();
				oven.getFootprint(foot);
				info = oven.getInfo();
				cpos = new Tile(foot.x + offX, foot.y + offY, 
						oven.getLift() + info.get3dHeight() + offZ);

				if (pact != null) {
					npc.setAction(new ActorAction.Sequence(
						pact,
						new ActorAction.Pickup(dough, cpos, 1, false)));

					dough.setQuality(51);
					dough_in_oven = dough;
					dough = null;
				} else {
					dough.removeThis();
					dough = null;
				}

				state = find_leftovers;
				break;
			}
			npc.start(1, delay);		// Back in queue.
		}
		@Override
		public void ending(int newtype) {
			if (dough != null) {
				dough.removeThis();
				dough = null;
			}

			if (dough_in_oven != null) {
				dough_in_oven.removeThis();
				dough_in_oven = null;
			}
		}
		@Override
		public void notifyObjectGone(GameObject obj) {
			if (obj == dough)		// Someone stole the dough!
				dough = null;
			if (obj == dough_in_oven)
				dough_in_oven = null;
		}
	}
	/*
	 *	Blacksmith schedule
	 */
	public static class Forge extends Schedule {
		private Tile npcpos = new Tile(), objpos = new Tile();
		private Rectangle foot = new Rectangle();
		private GameObject tongs;
		private GameObject hammer;
		private GameObject blank;
		private GameObject firepit;
		private GameObject anvil;
		private GameObject trough;
		private GameObject bellows;
		private final static int // enum {
			put_sword_on_firepit = 0,
			use_bellows = 1,
			get_tongs = 2,
			sword_on_anvil = 3,
			get_hammer = 4,
			use_hammer = 5,
			walk_to_trough = 6,
			fill_trough = 7,
			get_tongs2 = 8,
			use_trough = 9,
			done = 10;
		private int state;
		public Forge(Actor n) {
			super(n);
			state = put_sword_on_firepit;
		}
		@Override
		public void nowWhat() {
			npc.getTile(npcpos);
							// Often want to get within 1 tile.
			PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, 1);
			ActorAction pact;
			switch (state) {
			case put_sword_on_firepit:
				if (blank == null) {
					blank = npc.findClosest(668);
					//TODO: go and get it...
				}
				if (blank == null)
					blank = new IregGameObject(668, 0, 0, 0, 0);

				firepit = npc.findClosest(739);
				if (firepit == null) {
					// uh-oh... try again in a few seconds
					npc.start(1, 10);
					return;
				}
				firepit.getTile(objpos);
				pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost);

				firepit.getFootprint(foot);
				ShapeInfo info = firepit.getInfo();
				objpos.set(foot.x + foot.w/2 + 1, foot.y + foot.h/2,
					firepit.getLift() + info.get3dHeight());
				if (pact != null) {
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.Pickup(blank, objpos, 1, false)));
				} else {
					npc.setAction(
						new ActorAction.Pickup(blank, objpos, 1, false));
				}

				state = use_bellows;
				break;
			case use_bellows:
				bellows = npc.findClosest(431);
				firepit = npc.findClosest(739);
				if (bellows == null || firepit == null || blank == null) {
					// uh-oh... try again in a few second
					npc.start(1, 10);
					state = put_sword_on_firepit;
					return;
				}
				bellows.getTile(objpos);
				objpos.tx += 3;
				pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost);

				ActorAction a[] = new ActorAction[35];
				a[0] = pact;
				a[1] = new ActorAction.FacePos(bellows, 1);
				a[2] = new ActorAction.Frames(0x2b, 0);
				a[3] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[4] = new ActorAction.Frames(0x20, 0);
				a[5] = new ActorAction.Frames(0x01, 0, firepit);
				a[6] = new ActorAction.Frames(0x01, 0, blank);
				a[7] = new ActorAction.Frames(0x2b, 0);
				a[8] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[9] = new ActorAction.Frames(0x20, 0);
				a[10] = new ActorAction.Frames(0x02, 0, blank);
				a[11] = new ActorAction.Frames(0x2b, 0);
				a[12] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[13] = new ActorAction.Frames(0x20, 0);
				a[14] = new ActorAction.Frames(0x02, 0, firepit);
				a[15] = new ActorAction.Frames(0x03, 0, blank);
				a[16] = new ActorAction.Frames(0x2b, 0);
				a[17] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[18] = new ActorAction.Frames(0x20, 0);
				a[19] = new ActorAction.Frames(0x03, 0, firepit);
				a[20] = new ActorAction.Frames(0x04, 0, blank);
				a[21] = new ActorAction.Frames(0x2b, 0);
				a[22] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[23] = new ActorAction.Frames(0x20, 0);
				a[24] = new ActorAction.Frames(0x2b, 0);
				a[25] =	new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[26] = new ActorAction.Frames(0x20, 0);
				a[27] = new ActorAction.Frames(0x2b, 0);
				a[28] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[29] = new ActorAction.Frames(0x20, 0);
				a[30] = new ActorAction.Frames(0x2b, 0);
				a[31] = new ActorAction.ObjectAnimate(bellows, 3, 1, 300);
				a[32] = new ActorAction.Frames(0x20, 0);
				a[33] = new ActorAction.Frames(0x00, 0, bellows);
				a[34] = null;


				npc.setAction(new ActorAction.Sequence(a));
				state = get_tongs;
				break;
			case get_tongs:
				if (tongs == null)
					tongs = new IregGameObject(994, 0, 0, 0, 0);

				npc.addDirty();
				npc.unreadyWeapon(); // make sure the tongs can be equipped
				npc.addReadied(tongs, Ready.lhand);
				npc.addDirty();

				state = sword_on_anvil;
				break;
			case sword_on_anvil:
				anvil = npc.findClosest(991);
				firepit = npc.findClosest(739);
				if (anvil == null || firepit == null || blank == null) {
					// uh-oh... try again in a few second
					npc.start(1, 10);
					state = put_sword_on_firepit;
					return;
				}

				firepit.getTile(objpos);
				pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost);
				Tile tpos2 = new Tile();
				anvil.getTile(tpos2);
				tpos2.ty++; 
				ActorAction pact2 = ActorAction.PathWalking.createPath(
						objpos, tpos2, cost);
				anvil.getFootprint(foot);
				info = anvil.getInfo();
				objpos.set(foot.x + 2, foot.y,
					anvil.getLift() + info.get3dHeight());
				if (pact != null && pact2 != null) {
					npc.setAction(new ActorAction.Sequence(pact,
						new ActorAction.Pickup(blank, 1),
						pact2,
						new ActorAction.Pickup(blank, objpos, 1, false)));
				} else {
					npc.setAction(new ActorAction.Sequence(
						new ActorAction.Pickup(blank, 1),
						new ActorAction.Pickup(blank, objpos, 1, false)));
				}
				state = get_hammer;
				break;
			case get_hammer:

				if (hammer == null)
					hammer = new IregGameObject(623, 0, 0, 0, 0);

				npc.addDirty();
				if (tongs != null) {
					tongs.removeThis();
					tongs = null;
				}
				npc.unreadyWeapon(); // make sure the hammer can be equipped
				npc.addReadied(hammer, Ready.lhand);
				npc.addDirty();

				state = use_hammer;
				break;
			case use_hammer:
				anvil = npc.findClosest(991);
				firepit = npc.findClosest(739);
				if (anvil == null || firepit == null || blank == null) {
					// uh-oh... try again in a few seconds
					npc.start(1, 10);
					state = put_sword_on_firepit;
					return;
				}

				byte frames[] = new byte[12];
				int cnt = npc.getAttackFrames(623, false, 0, frames);
				if (cnt > 0)
					npc.setAction(new ActorAction.Frames(frames, cnt));
				
				a = new ActorAction[10];
				a[0] = new ActorAction.Frames(frames, cnt);
				a[1] = new ActorAction.Frames(0x03, 0, blank);
				a[2] = new ActorAction.Frames(0x02, 0, firepit);
				a[3] = new ActorAction.Frames(frames, cnt);
				a[4] = new ActorAction.Frames(0x02, 0, blank);
				a[5] = new ActorAction.Frames(0x01, 0, firepit);
				a[6] = new ActorAction.Frames(frames, cnt);
				a[7] = new ActorAction.Frames(0x01, 0, blank);
				a[8] = new ActorAction.Frames(0x00, 0, firepit);
				a[9] = null;
				npc.setAction(new ActorAction.Sequence(a));

				state = walk_to_trough;
				break;
			case walk_to_trough:
				npc.addDirty();
				if (hammer != null) {
					hammer.removeThis();
					hammer = null;
				}
				npc.addDirty();

				trough = npc.findClosest(719);
				if (trough == null) {
					// uh-oh... try again in a few seconds
					npc.start(1, 10);
					state = put_sword_on_firepit;
					return;
				}

				if (trough.getFrameNum() == 0) {
					trough.getTile(objpos);
					objpos.ty += 2;
					pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost);
					npc.setAction(pact);
					state = fill_trough;
					break;
				}
				state = get_tongs2;
				break;
			case fill_trough:
				trough = npc.findClosest(719);
				if (trough == null) {
					// uh-oh... try again in a few seconds
					npc.start(1, 10);
					state = put_sword_on_firepit;
					return;
				}

				int dir = npc.getDirection(trough);
				trough.changeFrame(3);
				npc.changeFrame(
					npc.getDirFramenum(dir, Actor.bow_frame));

				state = get_tongs2;
				break;
			case get_tongs2:
				if (tongs == null)
					tongs = new IregGameObject(994, 0, 0, 0, 0);

				npc.addDirty();
				npc.unreadyWeapon(); // make sure the tongs can be equipped
				npc.addReadied(tongs, Ready.lhand);
				npc.addDirty();

				state = use_trough;
				break;
			case use_trough:
				trough = npc.findClosest(719);
				anvil = npc.findClosest(991);
				if (trough == null || anvil == null || blank == null) {
					// uh-oh... try again in a few seconds
					npc.start(1, 10);
					state = put_sword_on_firepit;
					return;
				}
				anvil.getTile(objpos);
				objpos.ty++;
				pact = ActorAction.PathWalking.createPath(npcpos, objpos, cost);

				tpos2 = new Tile();
				trough.getTile(tpos2);
				tpos2.ty += 2;
				pact2 = ActorAction.PathWalking.createPath(objpos, tpos2, cost);

				if (pact != null && pact2 != null) {
					byte troughframe = (byte)(trough.getFrameNum() - 1);
					if (troughframe < 0) troughframe = 0;

					dir = npc.getDirection(trough);
					byte npcframe = (byte)npc.getDirFramenum(dir, Actor.bow_frame);

					a = new ActorAction[7];
					a[0] = pact;
					a[1] = new ActorAction.Pickup(blank, 1);
					a[2] = pact2;
					a[3] = new ActorAction.Frames(npcframe, 1);
					a[4] = new ActorAction.Frames(troughframe, 0, trough);
					a[5] = new ActorAction.Frames(0x00, 0, blank);
					a[6] = null;
					npc.setAction(new ActorAction.Sequence(a));
				} else {
					// no path found, just pick up sword blank
					npc.setAction(new ActorAction.Sequence(
					 	new ActorAction.Pickup(blank, 1),
						new ActorAction.Frames(0, 0, blank)));
				}	
				state = done;
				break;
			case done:
				npc.addDirty();
				if (tongs != null) {
					tongs.removeThis();
					tongs = null;
				}
				npc.addDirty();
				state = put_sword_on_firepit;
			}
			npc.start(1, 1);		// Back in queue.
		}

		@Override
		public void ending(int newtype) { // Switching to another schedule
			if (tongs != null) {
				tongs.removeThis();
				tongs = null;
			}
			if (hammer != null) {
				hammer.removeThis();
				hammer = null;
			}
			firepit = npc.findClosest(739);
			bellows = npc.findClosest(431);

			if (firepit != null && firepit.getFrameNum() != 0)
				firepit.changeFrame(0);
				if (bellows != null && bellows.getFrameNum() != 0)
					bellows.changeFrame(0);
			if (blank != null && blank.getFrameNum() != 0)
				blank.changeFrame(0);
		}
	}
	/*
	 *	Walk to the destination for a new schedule.
	 */
	public static class WalkToSchedule extends Schedule {
		Rectangle screen;
		Tile from, to;
		Tile dest;			// Where we're going.
		int firstDelay;		// Starting delay (1/1000's sec.)
		int newSchedule;		// Schedule to set when we get there.
		int retries;			// # failures at finding path.
		int legs;			// # times restarted walk.
		/*
		 * 	Set to walk off screen.  We MUST get outside the screen rect. so we don't
		 * 	keep repeating this.
		 */
		private void walkOffScreen(Tile goal) {
			// Destination.
			if (goal.tx >= screen.x + screen.w) {
				goal.tx = (short)(screen.x + screen.w);
				goal.ty = -1;
			} else if (goal.tx < screen.x) {
				goal.tx = (short)(screen.x - 1);
				goal.ty = -1;
			} else if (goal.ty >= screen.y + screen.h) {
				goal.ty = (short)(screen.y + screen.h);
				goal.tx = -1;
			} else if (goal.ty < screen.y) {
				goal.ty = (short)(screen.y - 1);
				goal.tx = -1;
			}
		}
		public WalkToSchedule(Actor n, Tile d, int new_sched, int delay) {
			super(n);
			screen = new Rectangle();
			from = new Tile();
			to = new Tile();
			dest = d;
			newSchedule = new_sched;
			// Delay 0-20 secs.
			firstDelay = delay >= 0 ? delay : 
							(2*(EUtil.rand()%10000)/TimeQueue.tickMsecs);
		}
		@Override
		public void nowWhat() {	// Now what should NPC do?
			if (npc.distance(dest) <= 3) {	// Close enough!
				npc.setScheduleType(newSchedule);
				return;
			}
			if (legs >= 40 || retries >= 2) {	// Trying too hard?  (Following
		  				//   Patterson takes about 30.)
						// Going to jump there.
				npc.move(dest.tx, dest.ty, dest.tz);
				npc.setScheduleType(newSchedule);
				System.out.println("WalkToSchedule: Teleporting #" 
						+ npc.getNpcNum());
				return;
			}
						// Get screen rect. in tiles.
			gwin.getWinTileRect(screen);
			screen.enlarge(5);		// Enlarge in all dirs.
						// Might do part of it first.
			npc.getTile(from);
			to.set(dest);
						// Destination off the screen?
			if (!screen.hasPoint(to.tx, to.ty)) {
				System.out.println("Npc #"+ npc.getNpcNum() + 
						": !screen.hasPoint: screen=" + screen +
						", from=" + from + ", dormant = " + npc.isDormant());
				if (!screen.hasPoint(from.tx, from.ty)) {
						// Force teleport on next tick.
					retries = 100;
					npc.start(2, 1);
					return;
				}
						// Don't walk off screen if close, or
						//   if lots of legs, indicating that
						//   Avatar is following this NPC.
				if (from.distance(to) > 80 || legs < 10)
						// Modify 'dest'. to walk off.
					walkOffScreen(to);
			} else if (!screen.hasPoint(from.tx, from.ty))
						// Modify src. to walk from off-screen.
				walkOffScreen(from);
			blocked = new Tile(-1, -1, -1);
			System.out.println("WalkToSchedule: Finding path to schedule for #" 
					+ npc.getNpcNum() + " from " + from + " to " + to);
						// Create path to dest., delaying
						//   0 to 1 seconds.
			if (!npc.walkPathToTile(from, to, 1,
							firstDelay + (EUtil.rand()%1000)/TimeQueue.tickMsecs)) {
						// Wait 1 sec., then try again.
				System.out.println("Failed to find path for #" + npc.getNpcNum());
				npc.walkToTile(dest, 1, 1000/TimeQueue.tickMsecs);
				retries++;		// Failed.  Try again next tick.
			} else {				// Okay.  He's walking there.
				legs++;
				retries = 0;
			}
			firstDelay = 0;
		}
		@Override
		public void imDormant() {	// Just went dormant.
			nowWhat();				// Get there by any means.
		}
									// For Usecode intrinsic.
		@Override
		public int getActualType(Actor npc) {
			return newSchedule;
		}
		};
	
	/*
	 *	An NPC schedule change:
	 */
	public static class ScheduleChange {
		//+++++FINISH static vector<char *> script_names;	// For Scripted_schedule's.
		private byte time;		// Time*3hours when this takes effect.
		private byte type;		// Schedule_type value.
		private byte days;		// A bit for each day (0-6).  We don't
						//   yet use this.
		Tile pos;			// Location.
		public ScheduleChange() {
			days = 0x7f;
		}
		/* ++++++++++FINISH
		static void clear() {
			script_names.setSize(0);
		}
		static vector<char *>& get_script_names()
			{ return script_names; }
		*/
		public int getType()
			{ return type; }
		public int getTime()
			{ return time; }
		public Tile getPos()
			{ return pos; }
		/* ++++++++FINISH
		static char *get_script_name(int ty)
			{ return ty >= Schedule.first_scripted_schedule ? 
			    script_names[ty - Schedule.first_scripted_schedule] : 0; }
		*/
		/*
		 *	Set a schedule from a U7 'schedule.dat' entry.
		 */
		public void set4
			(
			byte entry[]		// 4 bytes read from schedule.dat.
			) {
			time = (byte)(entry[0]&7);
			type = (byte)((entry[0]>>3)&0x1f);
			days = 0x7f;			// All days of the week.
			int schunk = ((int)entry[3])&0xff;
			int x = ((int)entry[1])&0xff, y = ((int)entry[2])&0xff;
			int sx = schunk%EConst.c_num_schunks,
			    sy = schunk/EConst.c_num_schunks;
			pos = new Tile(sx*EConst.c_tiles_per_schunk + x, 
					 sy*EConst.c_tiles_per_schunk + y, 0);
		}
		/*
		 *	Set a schedule from an Exult 'schedule.dat' entry (vers. -1).
		 */

		public void set8
			(
			byte entry[]		// 8 bytes read from schedule.dat.
			)
			{
			pos = new Tile();
			pos.tx = (short)EUtil.Read2(entry, 0);
			pos.ty = (short)EUtil.Read2(entry, 2);
			pos.tz = (short)(entry[4]&0xff);
			time = (byte)((int)entry[5]&0xff);
			type = (byte)((int)entry[6]&0xff);
			days = (byte)((int)entry[7]&0xff);
		}
		/*
		 *	Write out schedule for Exult's 'schedule.dat'.
		 */

		public void write8
			(
			byte entry[]		// 8 bytes to write to schedule.dat.
			)
			{
			EUtil.Write2(entry, 0, pos.tx);
			EUtil.Write2(entry, 2, pos.ty);		// 4
			entry[4] = (byte) pos.tz;		// 5
			entry[5] = time;		// 6
			entry[6] = type;		// 7
			entry[7] = days;		// 8
		}
		/*
		 *	Set a schedule.
		 */
		public void set(int ax, int ay, int az, byte stype, byte stime) {
			time = stime;
			type = stype;
			pos = new Tile(ax, ay, az);
		}

	}
}
