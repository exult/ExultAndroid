package com.exult.android;
import com.exult.android.shapeinf.MonsterInfo;
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
		return false;	//+++++++++FINISH
	}
	/*
	 * 	THE SCHEDULES
	 */
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
			/*+++++++FINISH
			if (EUtil.rand() % 6 == 0)		// Check for lamps, etc.
				if (try_street_maintenance())
					return;		// We no longer exist.
			*/
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
					System.out.println("Blocked by object at " + blocked);
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
			/* ++++++++FINISH
			if (EUtil.rand() % 3 == 0)		// Check for lamps, etc.
				if (try_street_maintenance())
					return;		// We no longer exist.
			*/
			loc = new Tile(center.tx - dist + EUtil.rand()%(2*dist),
								center.ty - dist + EUtil.rand()%(2*dist), center.tz);
							// Wait a bit.
			npc.walkToTile(loc, 2+EUtil.rand()%2, EUtil.rand()%8);
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
			/* ++++++++FINISH
			if (EUtil.rand() % 2)			// 1/2 time, check for lamps, etc.
				if (try_street_maintenance())
					return;		// We no longer exist.
			*/
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
		}
	}
	/*
	 *	Action to sit in the chair NPC is in front of.
	 */

	static class SitActorAction extends ActorAction.Frames {
		GameObject chair;		// Chair.
		Tile chairloc;		// Original chair location.
		Tile sitloc;		// Actually where NPC sits.
		byte frames[];
		static short offsets[] = {0,-1, 1,0, 0,1, -1,0};// Offsets where NPC should sit.
		byte [] init(GameObject chairobj, Actor actor) {
						// Frame 0 faces N, 1 E, etc.
			int dir = 2*(chairobj.getFrameNum()%4);
			frames = new byte[2];
			frames[0] = (byte)actor.getDirFramenum(dir, Actor.bow_frame);
			frames[1] = (byte)actor.getDirFramenum(dir, Actor.sit_frame);
			return frames;
		}
		static boolean isOccupied(Tile sitloc, Actor actor)
			{
			Vector<GameObject> occ = new Vector<GameObject>();	// See if occupied.
			if (gmap.findNearby(occ, sitloc, EConst.c_any_shapenum, 0, 8) > 0) {
				for (GameObject npc : occ) {
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
			MapChunk nlist = gmap.getChunk(pos.tx/EConst.c_tiles_per_chunk, 
										   pos.ty/EConst.c_tiles_per_chunk);
			if (nlist.spotAvailable(actor.getInfo().get3dHeight(), 
					pos.tx, pos.ty, pos.tz, EConst.MOVE_WALK, 0, -1) >= 0)
				return true;
			return false;
		}
		public SitActorAction(GameObject o, Actor actor) {
			super(null,0);//++++++super(init(o, actor), 2);
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
				/* ++++++++FINISH
				if (chair.getInfo().getBargeType() != ShapeInfo.barge_seat)
					return;
				*/
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
				GameObject barge = null; //+++++FINISH chair.findClosest(961);
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
							// Wait a while if we got up.
			if (setAction(npc, chair, sat ? (2000 + EUtil.rand()%3000) : 0) == null)
				npc.start(200, 5000);	// Failed?  Try again later.
			else
				sat = true;
		}
		public static boolean isOccupied(GameObject chairobj, Actor actor) {
			return false; //++++++++++FINISH
		}
		//	Return chair found.
		public static GameObject setAction(Actor actor, GameObject chairobj,
					int delay) {
			/* ++++++++FINISH
			final int chairshapes[] = {873,292};
			Vector<GameObject> chairs = new Vector<GameObject>();
			if (chairobj == null) {			// Find chair if not given.
				actor.findClosest(chairs, chairshapes, chairshapes.length);
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
			SitActorAction act = new SitActorAction(chairobj, actor);
							// Walk there, then sit.
			setActionSequence(actor, act.getSitloc(), act, false, delay);
			*/
			return chairobj;
		}
		public static GameObject setAction(Actor actor) {
			return setAction(actor, null, 0);
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
					npc.start(200, 100);
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
