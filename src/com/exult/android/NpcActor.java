package com.exult.android;

public class NpcActor extends Actor {
	protected Schedule.ScheduleChange schedules[];
	
	public NpcActor(String nm, int shapenum) { 
		super(nm, shapenum, -1, -1); 
	}
	public NpcActor(String nm, int shapenum, int num, int uc) {
		super(nm, shapenum, num, uc);
	}
	/*
	 *	Run usecode when double-clicked.
	 */
	public void activate(int event) {
		if (!isDead())
			super.activate(event);;
	}
	/*
	 *	Handle a time event (for TimeSensitive).
	 */
	public void handleEvent(int ctime, Object udata) {
		if ((getFlag(GameObject.paralyzed) || isDead() ||
				getProperty(health) <= 0 ||
				(getFlag(GameObject.asleep) && scheduleType != Schedule.sleep))) {
			tqueue.add(ctime + 1, this, udata);
			return;
		}
		// Prevent actor from doing anything if not in the active map.
		// ... but not if the NPC is not on the map (breaks pathfinding
		// from offscreen if NPC not on map).
		if (getMap() != null && getMap() != gwin.getMap()) {
			setAction(null);
			dormant = true;
			if (schedule != null)
				schedule.imDormant();
			return;
		}
		if (schedule != null && partyId < 0 && canAct() && 
				(scheduleType != Schedule.combat ||	// Not if already in combat.
							// Patrol schedule already does this.
					scheduleType != Schedule.patrol ||
					scheduleType != Schedule.sleep ||
					scheduleType != Schedule.wait) &&
				(EUtil.rand()%3) == 0)	// Don't do it every time.
			schedule.seekFoes();

		if (action == null) {			// Not doing anything?
			if (inUsecodeControl() || !canAct())
					// Can't move on our own. Keep trying.
				tqueue.add(ctime + 1, this, udata);
			else if (schedule != null) {
					// Should try seeking foes?
				if (partyId < 0 && canAct() &&
							// Not if already in combat.
						(scheduleType != Schedule.combat ||
							// Patrol schedule already does this.
							scheduleType != Schedule.patrol ||
							scheduleType != Schedule.sleep ||
							scheduleType != Schedule.wait) &&
						(EUtil.rand()%4) == 0) {	// Don't do it every time.
					schedule.seekFoes();
						// Get back into queue.
					tqueue.add(ctime + 1, this, udata);
				} else if (dormant && scheduleType != Schedule.walk_to_schedule)
					tqueue.add(ctime + 3, this, udata);	// Check in 1/2 sec.
				else
					schedule.nowWhat();
			}
		} else {			// Do what we should.
			int delay = partyId < 0 ? gwin.isTimeStopped() : 0;
			if (delay <= 0) {		// Time not stopped?
				int speed = action.getSpeed();
				delay = action.handleEvent(this);
				if (delay == 0) {	// Action finished. Add a slight delay.
					frameTime = speed;
					if (frameTime == 0)	// Not a path. Add a delay anyway.
						frameTime = 1;
					delay = frameTime;
					setAction(null);
					}
				}
			tqueue.add(ctime + delay, this, udata);
		}
	}
	/*
	 *	Step onto an adjacent tile.
	 *
	 *	Output:	0 if blocked (or paralyzed).
	 *		Dormant is set if off screen.
	 */
	public boolean step(Tile t, int frame, boolean force) {
		if (getFlag(GameObject.paralyzed) || getMap() != gmap)
			return false;
		int oldtx = getTileX(), oldty = getTileY();
		//System.out.println("Npc #" + npcNum + " stepping to " + t.tx + "," + t.ty);
						// Get old chunk.
		MapChunk olist = getChunk();
		t.fixme();
						// Get chunk.
		int cx = t.tx/EConst.c_tiles_per_chunk, cy = t.ty/EConst.c_tiles_per_chunk;
						// Get rel. tile coords.
		int tx = t.tx%EConst.c_tiles_per_chunk, ty = t.ty%EConst.c_tiles_per_chunk;
						// Get .new chunk.
		MapChunk nlist = gmap.getChunk(cx, cy);
		if (nlist == null || !nlist.isRead()) {
			stop();
			dormant = true;
			return false;
		}
		int flags = getTileInfo(this, nlist, tx, ty);
		boolean poison = (flags&Actor.tilePoison) != 0; 
		if (!areaAvailable(t, null, force ? EConst.MOVE_ALL : 0)) {
			if (isReallyBlocked(t, force)) {
				if (schedule != null)		// Tell scheduler.
					schedule.setBlocked(t);
				stop();
							// Offscreen, but not in party?
				if (gwin.addDirty(this) && partyId < 0 &&
							// And > a screenful away?
					distance(gwin.getCameraActor()) > 
										1 + EConst.c_screen_tile_size)
					dormant = true;	// Go dormant.
				return false;		// Done.
			}
		}
		if (poison && t.tz == 0)
			setFlag(GameObject.poisoned);
		addDirty(false);			// Set to repaint old area.
						// Move it.
		movef(olist, nlist, tx, ty, frame, t.tz);
						// Near an egg?  (Do this last, since
						//   it may teleport.)
		nlist.activateEggs(this, t.tx, t.ty, t.tz, oldtx, oldty, false);
						// Offscreen, but not in party?
		if (!addDirty(true) && partyId < 0 &&
						// And > a screenful away?
		    distance(gwin.getCameraActor()) > 
						1 + EConst.c_screen_tile_size &&
				//+++Try getting rid of the 'talk' line:
						getScheduleType() != Schedule.talk &&
						getScheduleType() != Schedule.walk_to_schedule &&
						getScheduleType() != Schedule.street_maintenance) {
						// No longer on screen.
			stop();
			dormant = true;
			return false;
		}
		quakeOnWalk();
		return true;			// Add back to queue for next time.
	}
	/*
	 *	Remove an object from its container, or from the world.
	 *	The object is deleted.
	 */
	public void removeThis() {
		setAction(null);
	// Messes up resurrection	num_schedules = 0;
		tqueue.remove(this);// Remove from time queue.
						// Store old chunk list.
		MapChunk olist = getChunk();
		super.removeThis();	// Remove, but don't ever delete an NPC
		switchedChunks(olist, null);
		setInvalid();
	}
	/*
	 *	Move (teleport) to a new spot.
	 */
	public void move(int newtx, int newty, int newlift, int newmap) {
		MapChunk olist = getChunk();	// Store old chunk list.
						// Move it.
		super.move(newtx, newty, newlift, newmap);
		MapChunk nlist = getChunk();
		if (nlist != olist) {
			switchedChunks(olist, nlist);
			if (olist != null)		// Moving back into world?
				dormant = true;	// Cause activation if painted.
			}
	}
	public void setSchedules(Schedule.ScheduleChange sched[]) {
		schedules = sched;
	}
	public Schedule.ScheduleChange[] getSchedules() {
		return schedules;
	}
	/*
	 *	Find day's schedule for a given time-of-day.
	 *
	 *	Output:	index of schedule change.
	 *		-1 if not found, or if a party member.
	 */
	int findScheduleChange
		(
		int hour3			// 0=midnight, 1=3am, etc.
		) {
		if (partyId >= 0 || isDead())
			return (-1);		// Fail if a party member or dead.
		int cnt = schedules == null ? 0 : schedules.length;
		for (int i = 0; i < cnt; i++)
			if (schedules[i].getTime() == hour3)
				return i;
		return -1;
	}
	/*
	 *	Update schedule at a 3-hour time change.
	 */
	@Override
	public void updateSchedule
		(
		int hour3,			// 0=midnight, 1=3am, etc.
		int backwards,		// Extra periods to look backwards.
		int delay			// Delay in msecs, or -1 for random.
		) {
		int i = findScheduleChange(hour3);
		if (i < 0) {	// Not found?  Look at prev.?
						// Always if noon of first day.
			long hour = clock.getTotalHours();
			if (hour == 12 && backwards == 0)
				backwards++;
			while (backwards-- != 0 && i < 0)
				i = findScheduleChange((--hour3 + 8)%8);
			if (i < 0)
				return;
			// This is bad, not always true
			// location might be different
			//if (scheduleType == schedules[i].get_type())
			//	return;		// Already in it.
			}
		setScheduleAndLoc(schedules[i].getType(), schedules[i].getPos(),
									delay);
	}
	/*
	 *	Render.
	 */
	public void paint() {
		super.paint();			// Draw on screen.
		if (dormant && schedule != null &&	// Resume schedule.
						// FOR NOW:  Not when in formation.
		    (partyId < 0 || scheduleType != Schedule.follow_avatar)) {
			dormant = false;	// But clear out old entries first.??
			tqueue.remove(this);
						// Force schedule->now_what() in about 1/2 sec.
						// DO NOT call now_what here!!!
			int curtime = TimeQueue.ticks;
			tqueue.add(curtime + 3, this, gwin);
		}
	}
}
