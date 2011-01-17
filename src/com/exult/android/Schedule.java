package com.exult.android;

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
			if (rand() % 3 == 0)		// Check for lamps, etc.
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
			{ return ty >= Schedule::first_scripted_schedule ? 
			    script_names[ty - Schedule::first_scripted_schedule] : 0; }
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
