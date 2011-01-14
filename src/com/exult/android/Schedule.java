package com.exult.android;

public abstract class Schedule {
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
	public void im_dormant()	// Npc calls this when it goes from
		{  }			//   being active to dormant.
	public void ending(int newtype)// Switching to another schedule.
		{  }
	public void set_weapon(boolean removed)	// Set weapon info.
		{  }
					// Set where to sleep.
	public void set_bed(GameObject b)
		{  }
					// Notify that schedule's obj. has
					//   been moved.
	public void notify_object_gone(GameObject obj)
		{  }
					// For Usecode intrinsic.
	public int get_actual_type(Actor npc) {
		return npc.getScheduleType();
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
			npc.walkToTile(loc, 2, EUtil.rand()%8);
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
					!npc.walkPathToTile(pos, 1, EUtil.rand()%8, 1))
							// Failed?  Try again a little later.
				npc.start(250, EUtil.rand()%12);
		}
	}
}
