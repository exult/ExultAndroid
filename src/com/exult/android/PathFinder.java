package com.exult.android;

public abstract class PathFinder {
	/* +++++MAYBE not needed.
	protected Tile src, dest;
	
	public final void getSrc(Tile s) {
		return s.set(src);
	}
	public final void getDest(Tile d) {
		return d.set(dest);
	}
	*/
	abstract public boolean NewPath(Tile s, Tile d);
	abstract public boolean getNextStep(Tile n);
	abstract public int getNumSteps();
	abstract public boolean isDone();
	
	/*
	 *	This class provides A* cost methods.
	 */
	public static abstract class Client extends GameSingletons {
		protected int moveFlags;
						// Figure when to give up.
		public int getMaxCost(int cost_to_goal) {
			int max_cost = 3*cost_to_goal;
			// (Raised from 64 on 9/4/2000).
			return (max_cost < 74 ? 74 : max_cost);
		}
						// Figure cost for a single step.
		public abstract int getStepCost(Tile from, Tile to);
						// Estimate cost between two points.
		public abstract int estimateCost(Tile from, Tile to);
						// Is tile at the goal?
		public abstract boolean atGoal(Tile tile, Tile goal);
		
		public int getMoveFlags() { return moveFlags; }
		public void setMoveFlags(int m) { moveFlags = m;}
	}
	/*
	 * 	For an NPC trying to get from one spot to another:
	 */
	public static class ActorClient extends Client {
		private int dist;			// Distance for success.
		private Actor npc;			// Who this represents.
		public ActorClient(Actor n, int d) {
			npc = n;
			dist = d;
			setMoveFlags(npc.getTypeFlags());
		}
		/*
		 *	Figure when to give up.
		 */
		public int getMaxCost(int cost_to_goal) {
			int max_cost = 3*cost_to_goal;
						// Do at least 3 screens width.
			int min_max_cost = (gwin.getWidth()/EConst.c_tilesize)*2*3;
			return max_cost > min_max_cost ? max_cost : min_max_cost;
		}
		/*
		 *	Figure cost going from one tile to an adjacent tile (for pathfinding).
		 *	Output:	Cost, or -1 if blocked.
		 *		The 'tz' field in 'to' tile may be modified.
		 */
		public int getStepCost(Tile from, Tile to) {
			int cx = to.tx/EConst.c_tiles_per_chunk, cy = to.ty/EConst.c_tiles_per_chunk;
			MapChunk olist = gmap.getChunk(cx, cy);
			int tx = to.tx%EConst.c_tiles_per_chunk;	// Get tile within chunk.
			int ty = to.ty%EConst.c_tiles_per_chunk;
			int cost = 1;
			boolean water, poison = false;		// Get tile info.
			/* ++++++++FINISH
			Actor.getTileInfo(0, gwin, olist, tx, ty, water, poison);
			*/
			int old_lift = to.tz;		// Might climb/descend.
			if (!npc.areaAvailable(to, from, moveFlags)) {	// Blocked, but check for a door.
				return -1; /* FINISH++++++++++++++
				GameObject block = GameObject.findDoor(to);
				if (!block)
					return -1;
				if (!block.is_closed_door() ||
						// Can't get past locked doors.
						block.getFrameNum()%4 >= 2)
					return -1;
						// Can't be either end of door.
				Rectangle foot = block.get_footprint();
				if (foot.h == 1 && (to.tx == foot.x || to.tx == foot.x +
								foot.w - 1))
					return -1;
				else if (foot.w == 1 && (to.ty == foot.y || to.ty == foot.y + 
								foot.h - 1))
					return -1;
				if (foot.hasPoint(from.tx, from.ty))
					return -1;	// Don't walk within doorway.
				cost++;			// But try to avoid them.
				*/
			}
			if (old_lift != to.tz)
				cost++;
						// On the diagonal?
			if (from.tx != to.tx || from.ty != to.ty)
				cost *= 3;		// Make it 50% more expensive.
			else
				cost *= 2;
			if (poison && to.tz == 0)
				cost *= 2;		// And avoid poison if possible.
						// Get 'flat' shapenum.
			int shapenum = olist.getTerrain().getShapeNum(tx, ty);
			if (shapenum == 24) {		// Cobblestone path in BlackGate?
				int framenum = olist.getTerrain().getFrameNum(tx, ty);
				if (framenum <= 1)
					cost--;
			}
		return (cost);
		}
		/*
		 *	Estimate cost from one point to another.
		 */
		public int estimateCost(Tile from, Tile to) {
			int dx = to.tx - from.tx;
			if (dx < -EConst.c_num_tiles/2)	// Wrap around the world.
				dx += EConst.c_num_tiles;
			else if (dx < 0)
				dx = -dx;
			int dy = to.ty - from.ty;
			if (dy < -EConst.c_num_tiles/2)
				dy += EConst.c_num_tiles;
			else if (dy < 0)
				dy = -dy;
			int larger, smaller;		// Start with larger.
			if (dy <= dx) {
				larger = dx;
				smaller = dy;
			} else {
				larger = dy;
				smaller = dx;
			}
			return (2*larger + smaller);	// Straight = 2, diag = 3.
		}
		/*
		 *	Is tile at goal?
		 */
		public boolean atGoal(Tile tile, Tile goal) {
			return (goal.tz==-1 ? tile.distance2d(goal) : tile.distance(goal))<= dist;
		}
	}
}
