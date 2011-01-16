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
				} else
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
		/* +++++++++FINISH
		int water, poison;		// Get tile info.
		get_tile_info(this, gwin, nlist, tx, ty, water, poison);
		*/
		if (!areaAvailable(t, null, force ? EConst.MOVE_ALL : 0)) {
			/* +++++++++++++
			if (is_really_blocked(t, force))
			*/
				{
				/* ++++++++++++++
				if (schedule)		// Tell scheduler.
					schedule.set_blocked(t);
				*/
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
		/* ++++++++++++++
		if (poison && t.tz == 0)
			Actor::set_flag(static_cast<int>(GameObject.poisoned));
		*/
		/* ++++++++IS THIS NEEDED?
						// Check for scrolling.
		gwin.scroll_if_needed(this, t);
		*/
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
				//++++++++Try getting rid of the 'talk' line:
						getScheduleType() != Schedule.talk &&
						getScheduleType() != Schedule.street_maintenance) {
						// No longer on screen.
			stop();
			dormant = true;
			return false;
		}
		/* +++++++FINISH
		quake_on_walk();
		*/
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
		// +++++++++ FINISH gwin.remove_nearby_npc(this);	// Remove from nearby list.
						// Store old chunk list.
		MapChunk olist = getChunk();
		super.removeThis();	// Remove, but don't ever delete an NPC
		switchedChunks(olist, null);
		setInvalid();
		/* +++++ I think we don't need this.
		if (!nodel && npc_num > 0)	// Really going?
			unused = true;		// Mark unused if a numbered NPC.
		*/
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
}
