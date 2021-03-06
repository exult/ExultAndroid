package com.exult.android;
import com.exult.android.shapeinf.*;

public class MainActor extends Actor {
	static Tile stepFrom = new Tile();
	public MainActor(String nm, int shapenum) {
		super(nm, shapenum, -1, -1);
		frames = avatarFrames; 
		setFlag(GameObject.in_party);
	}
	/*
	 *	Handle a time event (for TimeSensitive).
	 */
	public void handleEvent(int ctime, Object udata) {
		if (action != null)	{		// Doing anything?
							// Do what we should.
			int speed = action.getSpeed();
			int delay = action.handleEvent(this);
			if (delay == 0) {
						// Action finished.
						// This makes for a smoother scrolling and prevents the
						// avatar from skipping a step when walking.
				frameTime = speed;
				if (frameTime == 0)	// Not a path. Add a delay anyway.
					frameTime = 1;	// 1 tick.
				delay = frameTime;
				setAction(null);
			}
			gwin.getTqueue().add(ctime + delay, this, udata);
		} else if (inUsecodeControl() || getFlag(GameObject.paralyzed))
				// Keep trying if we are in usecode control.
			gwin.getTqueue().add(ctime + 1, this, udata);
		else if (schedule != null)
			schedule.nowWhat();
	}
	/*
	 *	Get the party to follow.
	 */
	public final void getFollowers() {
		int cnt = partyman.getCount();
		for (int i = 0; i < cnt; i++)
			{
			Actor npc = gwin.getNpc(partyman.getMember(i));
			if (npc == null || npc.getFlag(GameObject.asleep) ||
			    npc.isDead())
				continue;
			int sched = npc.getScheduleType();
						// Skip if in combat or set to 'wait'.
			if (sched != Schedule.combat &&
			    sched != Schedule.wait &&
						// Loiter added for SI.
			    sched != Schedule.loiter) {
				if (sched != Schedule.follow_avatar)
					npc.setScheduleType(
							Schedule.follow_avatar);
				else
					npc.follow(this);
			}
		}
	}
	public boolean step(Tile t, int frame, boolean force) {
		restTime = 0;			// Reset counter.
		t.fixme();
						// Get chunk.
		int cx = t.tx/EConst.c_tiles_per_chunk, cy = t.ty/EConst.c_tiles_per_chunk;
						// Get rel. tile coords.
		int tx = t.tx%EConst.c_tiles_per_chunk, ty = t.ty%EConst.c_tiles_per_chunk;
		MapChunk nlist = gmap.getChunk(cx, cy);
		int flags = getTileInfo(this, nlist, tx, ty);
		boolean poison = (flags&Actor.tilePoison) != 0; 
		if (!areaAvailable(t, null, force ? EConst.MOVE_ALL : 0)) {
			if (isReallyBlocked(t, force)) {
				if (schedule != null)		// Tell scheduler.
					schedule.setBlocked(t);
				stop();
				return false;
			}
		}
		if (poison && t.tz == 0)
			setFlag(GameObject.poisoned);
						// Check for scrolling.
		gwin.scrollIfNeeded(this, t);
		addDirty(false);			// Set to update old location.
						// Get old chunk, old tile.
		MapChunk olist = getChunk();
		getTile(stepFrom);
						// Move it.
		movef(olist, nlist, tx, ty, frame, t.tz);
		addDirty(true);			// Set to update new.
						// In a new chunk?
		if (olist != nlist)
			this.switchedChunks(olist, nlist);
		int roof_height = nlist.isRoof (tx, ty, t.tz);
		gwin.setIceDungeon(nlist.isIceDungeon(tx, ty));
		if (gwin.setAboveMainActor(roof_height)) {
			gwin.setInDungeon(nlist.hasDungeon()?
						nlist.isDungeon(tx, ty):0);
			gwin.setAllDirty();
		}
		else if (roof_height < 31 && gwin.setInDungeon(nlist.hasDungeon()?
	 					nlist.isDungeon(tx, ty):0))
			gwin.setAllDirty();
						// Near an egg?  (Do this last, since
						//   it may teleport.)
		nlist.activateEggs(this, t.tx, t.ty, t.tz,
							stepFrom.tx, stepFrom.ty, false);
		/*
		quake_on_walk();
		*/
		return true;
	}
	public void switchedChunks(MapChunk olist, MapChunk nlist) {
		/* ++ MAYBE NOT NEED anymore.
		int newcx = nlist.getCx(), newcy = nlist.getCy();
		int xfrom, xto, yfrom, yto;	// Get range of chunks.
		if (olist == null ||		// No old, or new map?  Use all 9.
		     olist.getMap() != nlist.getMap()) {
			xfrom = newcx > 0 ? newcx - 1 : newcx;
			xto = newcx < EConst.c_num_chunks - 1 ? newcx + 1 : newcx;
			yfrom = newcy > 0 ? newcy - 1 : newcy;
			yto = newcy < EConst.c_num_chunks - 1 ? newcy + 1 : newcy;
		} else {
			int oldcx = olist.getCx(), oldcy = olist.getCy();
			if (newcx == oldcx + 1)
				{
				xfrom = newcx;
				xto = newcx < EConst.c_num_chunks - 1 ? newcx + 1 : newcx;
				}
			else if (newcx == oldcx - 1)
				{
				xfrom = newcx > 0 ? newcx - 1 : newcx;
				xto = newcx;
				}
			else
				{
				xfrom = newcx > 0 ? newcx - 1 : newcx;
				xto = newcx < EConst.c_num_chunks - 1 ? newcx + 1 : newcx;
				}
			if (newcy == oldcy + 1)
				{
				yfrom = newcy;
				yto = newcy < EConst.c_num_chunks - 1 ? newcy + 1 : newcy;
				}
			else if (newcy == oldcy - 1)
				{
				yfrom = newcy > 0 ? newcy - 1 : newcy;
				yto = newcy;
				}
			else
				{
				yfrom = newcy > 0 ? newcy - 1 : newcy;
				yto = newcy < EConst.c_num_chunks - 1 ? newcy + 1 : newcy;
				}
			}
		*/
		// If change in Superchunk number, apply Old Style caching emulation
		gwin.emulateCache(olist, nlist);
	}
	/*
	 *	Move (teleport) to a new spot.
	 */
	public void move(int newtx, int newty, int newlift, int newmap) {
		MapChunk olist = getChunk();	// Store old chunk list.
		// Move it.
		super.move(newtx, newty, newlift, newmap);
		MapChunk nlist = getChunk();
		if (nlist != olist)
			switchedChunks(olist, nlist);
		int tx = getTx(), ty = getTy();
		gwin.setIceDungeon(nlist.isIceDungeon(tx, ty));
		if (gwin.setAboveMainActor(nlist.isRoof(tx, ty, newlift)))
			gwin.setInDungeon(nlist.hasDungeon() ? nlist.isDungeon(tx, ty) : 0);
	}
	/*
	 *	We're dead.
	 */
	@Override
	public void die(GameObject attacker) {
		if (gwin.inCombat())
			gwin.toggleCombat();	// Hope this is safe....
		super.setFlag(GameObject.dead);
		gumpman.closeAllGumps(false);	// Obviously.
						// Special function for dying:
		ShapeInfoLookup.UsecodeFunctionData info = ShapeInfoLookup.getAvUsecode(0);
		ucmachine.callUsecode(info.funId, this, info.eventId);
	}
}
