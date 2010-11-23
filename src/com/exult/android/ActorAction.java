package com.exult.android;

abstract public class ActorAction {
	private static long seqcnt = 0;	// To check for deletion.
	protected long seq;				// Unique sequence #.
	
	public ActorAction() {
		seq = ++seqcnt;
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
	
	/*
	 *	Follow a path.
	 */
	public static class PathWalkingActorAction extends ActorAction {
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
		private Tile stepTile = new Tile(), curTile = new Tile();;
		private void setSubseq(ActorAction sub) {
			subseq = sub;
		}
		public PathWalkingActorAction(PathFinder p, int maxblk) {
			
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
				/*
				if (get_party) {		// MUST be the Avatar.
					GameWindow gwin = GameWindow.instanceOf();
					gwin.get_party_man().get_followers(newdir);
					if (done)
						gwin.getMainActor().get_followers();
				}
				*/
				if (reached_end)		// Was this the last step?
					return (0);
				return cur_speed;
			}
			if (deleted) 
				return 0;
			reached_end = false;
		actor.setStepIndex(frames.prevIndex(stepIndex));	// We didn't take the step.
		/*
						// Blocked by a door?
		if (actor.distance(stepTile) <= 2 &&
			(actor.getInfo().getShapeClass() == ShapeInfo.human ||
				actor.getEffectiveProp(Actor.intelligence) > 7))
					// +++++Check for intelligence; guessing how to do it.
			{
			GameObject door = Game_object::find_door(stepTile);
			if (door != 0 && door->is_closed_door() &&
						// Make sure it's not locked!
			    door->get_framenum()%4 < 2)

						// Try to open it.
				{
				if (open_door(actor, door))
					return speed;
				}
			}
			*/
			if (max_blocked == 0 ||		// No retries allowed?
				actor.isDormant())	// Or actor off-screen?
				return 0;
			blocked = 1;
			blocked_tile = stepTile;
			blocked_frame = (byte)frames.get(stepIndex);
			return (1 + EUtil.rand()%4);	// Wait 1 to 4 ticks.
		}
		int openDoor(Actor actor, GameObject door) {
			return 0;
		}
		public void stop(Actor actor) { // Stop moving.
			//+++++++++++
		}
						// Set simple path to destination.
		public ActorAction walkToTile(Actor npc, Tile src, Tile dest, int dist) {
			return null;
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

}
