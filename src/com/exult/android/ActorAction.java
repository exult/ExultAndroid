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
		return null;
	}
	public int getSpeed() {
		return 0;
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
			path = p;
			max_blocked = (byte) maxblk;
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
			/* +++++++++++++
			if (blocked != 0) {
				if (actor->step(blocked_tile, blocked_frame))
					{		// Successful?
					if (deleted) return 0;
					blocked = 0;
						// He was stopped, so restore speed.
					actor->set_frame_time(speed);
					return speed;
			}
						// Wait up to 1.6 secs.
			return deleted ? 0 : (blocked++ > max_blocked ? 0 
						: 100 + blocked*(std::rand()%500));
			}
			 */
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
				//++++++ finish actor.move(tile.tx, tile.ty, tile.tz);
				return cur_speed;
			} else if (actor.step(stepTile, frames.get(stepIndex), false)) {// Successful.
				if (deleted) 
					return 0;
				if (getParty) {		// MUST be the Avatar.
					partyman.getFollowers(newdir);
					/* +++++NOT SURE about this
					if (done)
						gwin.getMainActor().get_followers();
					*/
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
			(actor.getInfo().getShapeClass() == ShapeInfo.human ||
				actor.getEffectiveProp(Actor.intelligence) > 7))
					// +++++Check for intelligence; guessing how to do it.
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
				if (!path.NewPath(src, dest, cost))
					return null;
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
			return false;//+++++++++++FINISH
		}
		/* ++++++
						// Get destination, or ret. 0.
		public int get_dest(Tile dest) {
			
		}
		*/
						// Check for Astar.
		public boolean followingSmartPath() {
			return false; //++++++++++++++
		}
		public int getSpeed()
			{ return speed; }
		public ActorAction kill() {
			deleted = true;
			return this;
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
		public Frames(byte f[], int spd, GameObject o) {
			frames = f;
			cnt = f.length;
			speed = spd;
			obj = o;
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
}
