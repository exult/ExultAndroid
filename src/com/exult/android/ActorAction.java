package com.exult.android;

abstract public class ActorAction extends GameSingletons {
	private static long seqcnt = 0;	// To check for deletion.
	protected long seq;				// Unique sequence #.
	protected boolean getParty;		// Party should follow at each step.
	public ActorAction() {
		seq = ++seqcnt;
	}
	public final void setGetParty(Boolean tf) {
		getParty = tf;
	}
	/*
	 * Check for action being replaced.
	 * Returns delay from handleEvent, or 0 if we've been replaced.
	 */
	public final int handleEventSafely(Actor actor) {
		ActorAction old_action = actor.getAction();
		long old_seq = old_action.seq;
						// Do current action.
		int delay = handleEvent(actor);
		if (actor.getAction() != old_action ||
		    old_action.seq != old_seq) {
			return 0;		// We've been replaced.
		}
		return delay;
	}
	// Handle time event.
	abstract public int handleEvent(Actor actor);
	public void stop(Actor actor) {
	}
	public ActorAction walkToTile(Actor npc, Tile src, Tile dest, int dist) {
		// Do it the dumb way.
		ZombiePathFinder path = new ZombiePathFinder();
		getParty = false;
						// Set up new path.
		if (path.NewPath(src, dest, null))
			return (new PathWalking(path));
		else
			return null;
	}
	public int getSpeed() {
		return 0;
	}
	public boolean getDest(Tile dest) {
		return false;
	}
	public boolean followingSmartPath() {
		return false;
	}
	public IfElsePath asUsecodePath() {
		return null; 
	}
	/*
	 *	Call a usecode function.
	 */
	public static class Usecode extends ActorAction {
		int fun;			// Fun. #.
		GameObject item;		// Call it on this item.	
		int eventid;
		public Usecode(int f, GameObject i, int ev) {
			fun = f; item = i; eventid = ev;
		}
						// Handle time event.
		public int handleEvent(Actor actor) {
			ucmachine.callUsecode(fun, item, eventid);
			gwin.setAllDirty();		// Clean up screen.
			return 0;			// That's all.
		}
	}
	/*
	 *	Set up an action to get an actor to a location (via pathfinding), and
	 *	then execute another action when he gets there.
	 */
	public static ActorAction createActionSequence(Actor actor, 
			Tile dest, ActorAction when_there, boolean from_off_screen) {
		ActorAction act = when_there;
		Tile actloc = new Tile();
		actor.getTile(actloc);
		if (from_off_screen)
			actloc.tx = actloc.ty = -1;
		if (!dest.equals(actloc)) {		// Get to destination.
			ActorAction w = new PathWalking(new AStarPathFinder());
			ActorAction w2 = w.walkToTile(actor, actloc, dest, 0);
			if (w2 == null)		// Failed?  Teleport.
				w2 = new Move(dest);
						// And teleport if blocked walking.
			ActorAction tel = new Move(dest);
						// Walk there, then do whatever.
			Sequence seq;
			act = seq = new Sequence(w2, tel, act, null);
			seq.setSpeed(0);	// No delay between actions.
			}
		return act;
	}
	/*
	 *	Follow a path.
	 */
	public static class PathWalking extends ActorAction {
		protected boolean reached_end;		// Reached end of path.
		protected PathFinder path;		// Allocated pathfinder.
		protected boolean deleted;			// True if the action has been killed.
		private int original_dir;		// From src. to dest. (0-7).
		private int speed;			// Time between frames.
		private boolean from_offscreen;		// Walking from offscreen.
		private ActorAction subseq;		// For opening doors.
		private byte blocked;		// Blocked-tile retries.
		private byte max_blocked;	// Try this many times.
		private byte blocked_frame;	// Frame for blocked tile.
		private Tile blocked_tile;	// Tile to retry.
		private Tile stepTile = new Tile(), curTile = new Tile();
		private void setSubseq(ActorAction sub) {
			subseq = sub;
		}
		public PathWalking(PathFinder p, int maxblk) {
			path = p != null ? p : new AStarPathFinder();
			max_blocked = (byte) maxblk;
		}
		public PathWalking(PathFinder p) {
			path = p != null ? p : new AStarPathFinder();
			max_blocked = 3;
		}	
		public static PathWalking createPath(Tile src, Tile dest, 
						PathFinder.Client cost) {
			AStarPathFinder path = new AStarPathFinder();
			// Get to within 1 tile.
			if (path.NewPath(src, dest, cost))
				return new PathWalking(path);
			else
				return null;
		}
						// Handle time event.
		public int handleEvent(Actor actor) {
			if (subseq != null)	{		// Going through a door?
				int delay = subseq.handleEvent(actor);
				if (delay != 0)
					return delay;	// Still going.
				setSubseq(null);
						// He was stopped, so restore speed.
				actor.setFrameTime(speed);
				return speed;		// Come back in a moment.
			}
			if (blocked != 0) {
				if (actor.step(blocked_tile, blocked_frame, false)) {
							// Successful?
					if (deleted) return 0;
					blocked = 0;
						// He was stopped, so restore speed.
					actor.setFrameTime(speed);
					return speed;
			}
						// Wait up to 1.6 secs.
			return deleted ? 0 : (blocked++ > max_blocked ? 0 
						: 1 + (blocked*(EUtil.rand()%500))/TimeQueue.tickMsecs);
			}
			speed = actor.getFrameTime();// Get time between frames.
			if (speed == 0)
				return 0;		// Not moving.
			if (!path.getNextStep(stepTile)) {
				reached_end = true;	// Did it.
				return (0);
			}
			if (path.isDone())			// In case we're deleted.
				reached_end = true;
			actor.getTile(curTile);
			int newdir = EUtil.getDirection4(curTile.ty - stepTile.ty, 
								stepTile.tx - curTile.tx);
			Actor.FramesSequence frames = actor.getFrames(newdir);
			int stepIndex = actor.getStepIndex();
			if (stepIndex == 0)		// First time?  Init.
				stepIndex = frames.findUnrotated((byte)actor.getFrameNum());
						// Get next (updates step_index).
			stepIndex = frames.nextIndex(stepIndex);
			actor.setStepIndex(stepIndex);
			int cur_speed = speed;		// Step() might delete us!
			if (from_offscreen)	{	// Teleport to 1st spot.
				from_offscreen = false;
				actor.move(stepTile);
				return cur_speed;
			} else if (actor.step(stepTile, frames.get(stepIndex), false)) {// Successful.
				if (deleted) 
					return 0;
				if (getParty) {		// MUST be the Avatar.
					partyman.getFollowers(newdir);
					if (reached_end)
						gwin.getMainActor().getFollowers();
				}
				if (reached_end)		// Was this the last step?
					return (0);
				return cur_speed;
			}
			if (deleted) 
				return 0;
			reached_end = false;
		actor.setStepIndex(frames.prevIndex(stepIndex));	// We didn't take the step.
						// Blocked by a door?
		if (actor.distance(stepTile) <= 2 &&
			actor.getScheduleType() != Schedule.loiter &&
			(actor.getInfo().getShapeClass() == ShapeInfo.human ||
				actor.getEffectiveProp(Actor.intelligence) > 7))
					// +++Check for intelligence; guessing how to do it.
			{
			GameObject door = GameObject.findDoor(stepTile);
			if (door != null && door.isClosedDoor() &&
						// Make sure it's not locked!
					door.getFrameNum()%4 < 2) {
						// Try to open it.
				if (openDoor(actor, door))
					return speed;
				}
			}
			if (max_blocked == 0 ||		// No retries allowed?
				actor.isDormant())	// Or actor off-screen?
				return 0;
			blocked = 1;
			blocked_tile = stepTile;
			blocked_frame = (byte)frames.get(stepIndex);
			return (1 + EUtil.rand()%4);	// Wait 1 to 4 ticks.
		}
		public void stop(Actor actor) { // Stop moving.
			// Don't set slimes.
			if (!actor.getInfo().hasStrangeMovement() && actor.canAct()) {
							// For now, just use original dir.
				Actor.FramesSequence frames = actor.getFrames(original_dir);
				actor.changeFrame(frames.getResting());
				}
		}
						// Set simple path to destination.
		public ActorAction walkToTile(Actor npc, Tile src, Tile dest, int dist) {
			blocked = 0;			// Clear 'blocked' count.
			reached_end = false;		// Starting new path.
			getParty = false;
			from_offscreen = false;
						//+++++Should dist be used below??:
							// Set up new path.
							// Don't care about 1 coord.?
			if (dest.tx == -1 || dest.ty == -1) {
				if (dest.tx == dest.ty)	// Completely off-screen?
					{
					PathFinder.OffScreenClient cost =
									new PathFinder.OffScreenClient(npc);
					if (!path.NewPath(src, dest, cost))
						return null;
				} else {
					PathFinder.OneCoordClient cost =
								new PathFinder.OneCoordClient(npc);
					if (!path.NewPath(src, dest, cost))
						return null;
				}
			} else if (src.tx == -1 || src.ty == -1) {	// How about from source?
							// Figure path in opposite dir.
				if (src.tx == src.ty) {	// Both -1?
							// Aim from NPC's current pos.
					Tile t = new Tile();
					npc.getTile(t);
					PathFinder.OffScreenClient cost =
							new PathFinder.OffScreenClient(npc, t);
					if (!path.NewPath(dest, src, cost))
						return null;
				} else {
					PathFinder.OneCoordClient cost = 
										new PathFinder.OneCoordClient(npc);
					if (!path.NewPath(dest, src, cost))
						return null;
				}
				from_offscreen = true;
							// Set to go backwards.
				if (!path.setBackwards())
					return null;
			} else {
				PathFinder.ActorClient cost = new PathFinder.ActorClient(npc, dist);
				if (!path.NewPath(src, dest, cost)) {
					System.out.println("PathWalking.walkToTile failed.");
					return null;
				}
			}
							// Reset direction (but not index).
			original_dir = EUtil.getDirection4(src.ty - dest.ty, dest.tx - src.tx);
			return this;
		}
		/*
		 *	Open door that's blocking the NPC, and set action to walk past and
		 *	close it.
		 */
		public boolean openDoor(Actor actor, GameObject door) {
			int curTx = actor.getTileX(), curTy = actor.getTileY(),
				curTz = actor.getLift();
			System.out.println("openDoor: NPC # " + actor.getNpcNum() + 
					", schedule = " + actor.getScheduleType());
			// Get door's footprint in tiles.
			Rectangle foot = new Rectangle();
			door.getFootprint(foot);
						// Open it, but kludge quality to
						//   avoid unwanted usecode.
			int savequal = door.getQuality();
			door.setQuality(0);
			door.activate();
			door.setQuality(savequal);
			Tile past = new Tile();	// Tile on other side of door.	
			past.tz = (short)curTz;
			int dir;			// Get dir to face door afterwards.
			if (foot.w > foot.h) {		// Horizontal?
				past.tx = (short)(foot.x + foot.w/2);
				if (curTy <= foot.y) {	// N. of door?
					past.ty = (short)(foot.y + foot.h);
					dir = 0;
				} else {			// S. of door?
					past.ty = (short)(foot.y - 1);
					dir = 4;
				}
			} else {				// Vertical.
				past.ty = (short)(foot.y + foot.h/2);
				if (curTx <= foot.x) {	// W. of door?
					past.tx = (short)(foot.x + foot.w);
					dir = 6;
				} else {			// E. of door?
					past.tx = (short)(foot.x - 1);
					dir = 2;
				}
			}
			if (MapChunk.findSpot(past, 1, actor, 1, MapChunk.anywhere)) {
								// Succeeded.  Walk past and close it.
				byte frames[] = new byte[2];
				frames[0] = (byte)actor.getDirFramenum(dir, Actor.standing);
				frames[1] = (byte)actor.getDirFramenum(dir, 3);
				setSubseq(createActionSequence(actor, past,
						new Sequence(
								new Frames(frames, 2),
								new Activate(door),
								new Frames(frames, 1),
								null), false));
				return true;
			}
			return false;
		}
		public boolean getDest(Tile dest) {
			path.getDest(dest);
			return true;
		}
		public boolean followingSmartPath() {
			return path != null && path.followingSmartPath();
		}
		public int getSpeed()
			{ return speed; }
		public ActorAction kill() {
			deleted = true;
			return this;
		}
	}
	/*
	 *	Follow a path to approach a given object, and stop half-way if it
	 *	moved.
	 */
	public static class Approach extends PathWalking {
		GameObject dest_obj;		// Destination object.
		int goal_dist;			// Stop if within this distance.
		Tile orig_dest_pos;	// Dest_obj's pos. when we start.
		int cur_step;			// Count steps.
		int check_step;			// Check at this step.
		boolean for_projectile;		// Check for proj. path.
		public Approach(PathFinder p, GameObject d, int gdist, boolean for_proj) {
			super(p, 0);	// Stop if blocked.
			dest_obj = d;
			orig_dest_pos = new Tile();
			d.getTile(orig_dest_pos);
			goal_dist = gdist;
			for_projectile = for_proj;
			// Get length of path.
			int nsteps = path.getNumSteps();
			System.out.println("Aproach nsteps is " + nsteps + ".");
			if (nsteps >= 6)		// (May have to play with this).
				check_step = nsteps > 18 ? 9 : nsteps/2;
			else
				check_step = 10000;
		}
		public static Approach createPath(Tile src,
					GameObject dest, int gdist, PathFinder.Client cost) {
			AStarPathFinder path = new AStarPathFinder();
			// Get to within 1 tile.
			Tile t = new Tile();
			dest.getTile(t);
			if (path.NewPath(src, t, cost))
				return new Approach(path, dest, gdist, false);
			else
				return null;
		}
		// Handle time event.
		public int handleEvent(Actor actor) {
			int delay = super.handleEvent(actor);
			if (delay == 0 || deleted)			// Done or blocked.
				return 0;
							// Close enough?
			if (goal_dist >= 0 && actor.distance(dest_obj) <= goal_dist)
				return 0;
			if (++cur_step == check_step) {	// Time to check.
				if (dest_obj.distance(orig_dest_pos) > 2)
					return 0;	// Moved too much, so stop.
				if (for_projectile &&
				    PathFinder.FastClient.isStraightPath(actor, dest_obj))
					return 0;	// Can fire projectile.
							// Figure next check.
				int nsteps = path.getNumSteps();
				if (nsteps >= 6)
					// Try checking more often.
					check_step += 3;
			}
			return delay;
		}
	}	
	/*
	 *	Follow a path and execute one action if successful, another if
	 *	failed.
	 */
	public static class IfElsePath extends PathWalking {
		boolean succeeded, failed, done;
		ActorAction success, failure;
		public IfElsePath(Actor actor, Tile dest, ActorAction s, ActorAction f) {
			super(null, 6);
			success = s; failure = f;
			Tile t = new Tile();
			actor.getTile(t);
			if (walkToTile(actor, t, dest, 0) == null) {
				done = failed = true;
			}
		}
		public void setFailure(ActorAction f) {
			failure = f;
			done = false;
		}
		boolean doneAndFailed()		// Happens if no path found in ctor.
			{ return done && failed; }
						// Handle time event.
		public int handleEvent(Actor actor) {
			if (done)
				return 0;		// Shouldn't really get here.
			//boolean del;
			int delay;
			if (succeeded) {			// Doing the success action?
				if ((delay = success.handleEvent(actor)) == 0)
					done = true;
				return delay;
			} else if (failed) {
				if ((delay = failure.handleEvent(actor)) == 0)
					done = true;
				return delay;
			}
			delay = super.handleEvent(actor);
			if (delay != 0)
				return delay;
			if (deleted)
				return 0;
			if (!reached_end) {			// Didn't get there.
				if (failure != null) {
					failed = true;
					delay = failure.handleEvent(actor);
					/*++++++++STILL NEED this?
					if (del)	// Are we gone?
						return 0;
					*/
				}
			} else {				// Success.
				if (success != null) {
					succeeded = true;
					delay = success.handleEvent(actor);
				}
			}
			if (delay == 0)
				done = true;		// All done now.
			return delay;
		}
		public IfElsePath asUsecodePath()
			{ return this; }
		}
	/*
	 *	Just move (i.e. teleport) to a desired location.
	 */
	public static class Move extends ActorAction {
		Tile dest;		// Where to go.
		public Move(Tile d) {
			dest = d;
		}
						// Handle time event.
		public int handleEvent(Actor actor) {
			if (dest.tx < 0)
				return 0;
			if (actor.getTileX() == dest.tx && actor.getTileY() == dest.ty &&
							actor.getLift() == dest.tz)
				return 0;
			actor.move(dest);		// Zip right there.
			if (actor == gwin.getMainActor())
							// Teleported Avatar?
				gwin.centerView(dest);
			dest.tx = -1;			// Set to stop.
			return (1);			// Wait 1 tick.
		}
	}
	/*
	 * 	Activate an object.
	 */
	public static class Activate extends ActorAction {
		private GameObject obj;
		public Activate(GameObject o) {
			obj = o;
		}
		public int handleEvent(Actor actor) {
			obj.activate();
			return 0;
		}
	}
	/*
	 *	Go through a series of frames.
	 */
	public static class Frames extends ActorAction {
		byte frames[];		// List to go through (a -1 means to
							//   leave frame alone.)
		int cnt;			// Size of list.
		int index;			// Index for next.
		int speed;			// Frame delay in 1/1000 secs.
		GameObject obj;		// Object to animate
		public Frames(byte f[], int c, int spd, GameObject o) {
			frames = f;
			cnt = c;
			speed = spd;
			obj = o;
		}
		public Frames(int oneFrame, int spd, GameObject o) {
			frames = new byte[1];
			frames[0] = (byte)oneFrame;
			cnt = 1;
			speed = spd;
			obj = o;
		}
		public Frames(byte f[], int c) {
			frames = f;
			cnt = c;
			speed = 1;
			obj = null;
		}
						// Handle time event.
		public int handleEvent(Actor actor) {
			if (index == cnt)
				return (0);		// Done.
			int frnum = frames[index++];	// Get frame.
			if (frnum >= 0) {
				if (obj != null)
					obj.changeFrame(frnum);
				else
					actor.changeFrame(frnum);
			}
			return (speed);
		}
		public int getIndex()
			{ return index; }
		public int getSpeed()
			{ return speed; }
	}
	/*
	 *	Do a sequence of actions.
	 */
	public static class Sequence extends ActorAction {
		ActorAction actions[];		// List of actions, ending with null.
		int index;			// Index into list.
		int speed;			// Frame delay in 1/1000 secs. between actions.
		public Sequence(ActorAction act[], int spd) {
			actions = act;
			index = 0; speed = spd;
		}
						// Create with up to 4.
		public Sequence(ActorAction a0, ActorAction a1,
					ActorAction a2, ActorAction a3) {
			actions = new ActorAction[4];
			actions[0] = a0; actions[1] = a1; actions[2] = a2; actions[3] = a3;
		}				// Create with 2.
		public Sequence(ActorAction a0, ActorAction a1) {
			actions = new ActorAction[2];
			actions[0] = a0; actions[1] = a1;
		}
		public int getSpeed()
			{ return speed; }
		void setSpeed(int spd)
			{ speed = spd; }
						// Handle time event.
		public int handleEvent(Actor actor) {
			if (index >= actions.length || actions[index] == null)		// Done?
				return (0);
							// Do current action.
			// +++++STILL NEEDED  boolean deleted;
			int delay = actions[index].handleEvent(actor);
			if (delay == 0) {
				index++;		// That one's done now.
				if (speed == 0)		// Immediately?  Run with next.
					return handleEvent(actor);
				delay = speed;
			}
			return (delay);
		}
	}
	/*
	 *	Rotate through an object's frames.
	 */
	public static class ObjectAnimate extends ActorAction {
		private GameObject obj;
		private int nframes;			// # of frames.
		private int cycles;			// # of cycles to do.
		private int speed;			// Time between frames.

		public ObjectAnimate(GameObject o, int cy, int spd) {
			obj = o; cycles = cy; speed = spd;
			nframes = obj.getNumFrames();
		}
		public ObjectAnimate(GameObject o, int nfr, int cy, int spd) {
			obj = o; cycles = cy; speed = spd;
			nframes = nfr;
		}
		@Override				// Handle time event.
		public int handleEvent(Actor actor) {
			if (cycles == 0) 
				return 0;
			int frnum = (obj.getFrameNum() + 1) % nframes;
			if (frnum == 0)			// New cycle?
				--cycles;
			obj.changeFrame(frnum);
			return (cycles > 0 ? speed : 0);
		}
		@Override
		public int getSpeed()
			{ return speed; }
	}
	/*
	 *	Action to pick up an item or put it down.
	 */
	public static class Pickup extends ActorAction {
		private GameObject obj;		// What to pick up/put down.
		private boolean pickup;			// 1 to pick up, 0 to put down.
		private int speed;			// Time between frames.
		private int cnt;			// 0, 1, 2.
		private Tile objpos = new Tile();		// Where to put it.
		private int dir;			// Direction to face.
		private boolean temp;			// True to make object temporary on drop.
						// To pick up an object:
		public Pickup(GameObject o, int spd) {
			obj = o;
			pickup = true;
			speed = spd;
			obj.getTile(objpos);
		}
						// To put down an object:
		Pickup(GameObject o, Tile opos, int spd, boolean t) {
			obj = o;
			pickup = false;
			speed = spd;
			objpos.set(opos);
			temp = t;
		}
		@Override
		public int handleEvent(Actor actor) {
			int frnum = -1;
			switch (cnt) {
			case 0:				// Face object.
				dir = actor.getDirection(objpos);
				frnum = actor.getDirFramenum(dir, Actor.standing);
				cnt++;
				break;
			case 1:				// Bend down.
				frnum = actor.getDirFramenum(dir, Actor.bow_frame);
				cnt++;
				if (pickup) {
					if (actor.distance(obj) > 8) {	// No longer nearby.
						actor.notifyObjectGone(obj);
						break;
					}
					gwin.addDirty(obj);
					obj.removeThis();
					actor.add(obj, true);
				} else {
					obj.removeThis();
					obj.move(objpos);
					if (temp)
						obj.setFlag(GameObject.is_temporary);
					gwin.addDirty(obj);
				}
				break;
			default:
				return 0;		// Done.
			}
			actor.changeFrame(frnum);
			return speed;
		}
		@Override
		public int getSpeed()
			{ return speed; }
		}
	/*
	 *	Action to turn towards an object or spot.
	 */
	public static class FacePos extends ActorAction {
		int speed;			// Time between frames.
		Tile pos;			// Where to put it.
		public FacePos(Tile p, int spd) {
			pos = p;
			speed = spd;
		}
						// To pick up an object:
		public FacePos(GameObject o, int spd) {
			pos = new Tile();
			o.getTile(pos);
			speed = spd;
		}
		@Override
		public int handleEvent(Actor actor) {
			int dir = actor.getDirection(pos);
			int frnum = actor.getDirFramenum(dir, Actor.standing);
			if (actor.getFrameNum() == frnum)
				return 0;		// There.
			actor.changeFrame(frnum);
			return speed;
		}
		@Override
		public int getSpeed()
			{ return speed; }
	}
}
