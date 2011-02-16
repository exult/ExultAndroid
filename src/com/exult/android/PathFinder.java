package com.exult.android;

public abstract class PathFinder {
	protected Tile dest;
	/* +++++MAYBE not needed.
	protected Tile src;
	
	public final void getSrc(Tile s) {
		return s.set(src);
	}
	*/
	public final void getDest(Tile d) {
		d.set(dest);
	}
	abstract public boolean NewPath(Tile s, Tile d, Client c);
	abstract public boolean getNextStep(Tile n);
	abstract public int getNumSteps();
	abstract public boolean isDone();
	boolean setBackwards() {
		return false;	// Default: Can't do it.
	}
	public boolean followingSmartPath() {
		return false;
	}
	public boolean NewPath(Tile s, Tile d) {
		return NewPath(s, d, null);
	}
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
			/*
			System.out.printf("getStepCost: from %1$d,%2$d,%3$d to %4$d,%5$d,%6$d\n", 
					from.tx, from.ty, from.tz, to.tx, to.ty, to.tz);
			*/
			int old_lift = to.tz;		// Might climb/descend.
			if (!npc.areaAvailable(to, from, moveFlags)) {	// Blocked; check for door.
				GameObject block = GameObject.findDoor(to);
				if (block == null)
					return -1;
				if (!block.isClosedDoor() ||
						// Can't get past locked doors.
						block.getFrameNum()%4 >= 2)
					return -1;
						// Can't be either end of door.
				Rectangle foot = new Rectangle();
				block.getFootprint(foot);
				if (foot.h == 1 && (to.tx == foot.x || to.tx == foot.x +
								foot.w - 1))
					return -1;
				else if (foot.w == 1 && (to.ty == foot.y || to.ty == foot.y + 
								foot.h - 1))
					return -1;
				if (foot.hasPoint(from.tx, from.ty))
					return -1;	// Don't walk within doorway.
				cost++;			// But try to avoid them.
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
			ChunkTerrain ter = olist.getTerrain();
			if (ter == null)
				return -1;
			int shapenum = ter.getShapeNum(tx, ty);
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
	/*
	 *	This client succeeds when the path makes it to just one X/Y coord.
	 *	It assumes that a -1 was placed in the coord. that we should ignore.
	 */
	public static class OneCoordClient extends ActorClient {
		public OneCoordClient(Actor n) {
			super(n, 0);
		}
						// Estimate cost between two points.
		public int estimateCost(Tile from, Tile to) {
			if (to.tx == -1)		// Just care about Y?
			{			// Cost = 2/tile.
			int dy = to.ty - from.ty;
			if (dy < -EConst.c_num_tiles/2)
				dy += EConst.c_num_tiles;
			else if (dy < 0)
				dy = -dy;
			return (2*dy);
			}
		else if (to.ty == -1)
			{
			int dx = to.tx - from.tx;
			if (dx < -EConst.c_num_tiles/2)
				dx += EConst.c_num_tiles;
			else if (dx < 0)
				dx = -dx;
			return (2*dx);
			}
		else				// Shouldn't get here.
			return super.estimateCost(from, to);
		}
						// Is tile at the goal?
		public boolean atGoal(Tile tile, Tile goal) {
			return ((goal.tx == -1 || tile.tx == goal.tx) && 
					(goal.ty == -1 || tile.ty == goal.ty) &&
					(goal.tz == -1 || tile.tz == goal.tz));
		}
	}

	/*
	 *	This client succeeds when the path makes it offscreen.
	 *	Only the tz coord. of the dest. is used.
	 */
	public static class OffScreenClient extends ActorClient {
		Rectangle screen;		// Screen rect. in tiles.
		Tile best;				// Best offscreen pt. to aim for.
		public OffScreenClient(Actor n) {
			super(n, 0);
			screen = new Rectangle();
			gwin.getWinTileRect(screen);
			screen.enlarge(3);
		}
		public OffScreenClient(Actor n, Tile b) {
			super(n, 0);
			screen = new Rectangle();
			gwin.getWinTileRect(screen);
			
			best = b;
			if (best != null && best.tx != -1) { // Scale (roughly) to edge of screen.
				Rectangle scr = screen;
						// Get center.
				int cx = scr.x + scr.w/2, cy = scr.y + scr.h/2;
						// More than 4 screens away?
				if (best.distance2d(cx, cy) > 4*scr.w) {
					best.tx = best.ty = -1;
				} else {
					if (best.tx > cx + scr.w)
						// Too far to right of screen.
						best.tx = (short)(scr.x + scr.w + 1);
					else if (best.tx < cx - scr.w)
						best.tx = (short)(scr.x - 1);
					if (best.ty > cy + scr.h)
						best.ty = (short)(scr.y + scr.h + 1);
					else if (best.ty < cy - scr.h)
						best.ty = (short)(scr.y - 1);
						// Give up if it doesn't look right.
					if (best.distance2d(cx, cy) > scr.w)
						best.tx = best.ty = -1;
				}
			}
			screen.enlarge(3);
		}
						// Figure cost for a single step.
		public int getStepCost(Tile from, Tile to) {
			int cost = super.getStepCost(from, to);
			if (cost == -1)
				return cost;
			if (best.tx != -1)		// Penalize for moving away from best.
				{
				if ((to.tx - from.tx)*(best.tx - from.tx) < 0)
					cost++;
				if ((to.ty - from.ty)*(best.ty - from.ty) < 0)
					cost++;
				}
			return cost;
		}
						// Estimate cost between two points.
		public int estimateCost(Tile from, Tile to) {
			if (best.tx != -1)		// Off-screen goal?
				return super.estimateCost(from, best);
		//++++++World-wrapping here????
			int dx = from.tx - screen.x;	// Figure shortest dist.
			int dx1 = screen.x + screen.w - from.tx;
			if (dx1 < dx)
				dx = dx1;
			int dy = from.ty - screen.y;
			int dy1 = screen.y + screen.h - from.ty;
			if (dy1 < dy)
				dy = dy1;
			int cost = dx < dy ? dx : dy;
			if (cost < 0)
				cost = 0;
			if (to.tz != -1 && from.tz != to.tz)
				cost++;
			return 2*cost;
		}
						// Is tile at the goal?
		public boolean atGoal(Tile tile, Tile goal) {
			return !screen.hasPoint(tile.tx - tile.tz/2, tile.ty - tile.tz/2);
		}
	}
	/*
	 *	This client is supposed to fail quickly, so that it can be used to
	 *	test for when an object can be grabbed.
	 */
	public static class FastClient extends Client {
		private int dist;			// Succeeds at this distance from goal.
		public FastClient(int d, int mf) {
			dist = d;
			setMoveFlags(mf); 
		}
		public FastClient(int d) {
			dist = d;
			setMoveFlags( 1 << 5);
		}
		@Override			// Figure when to give up.
		public int getMaxCost(int cost_to_goal) {
			int max_cost = 2*cost_to_goal;	// Don't try too hard.
			if (max_cost < 8)
				max_cost = 8;
			else if (max_cost > 64)
				max_cost = 64;
			return max_cost;
		}
		@Override				// Figure cost for a single step.
		public int getStepCost(Tile from, Tile to) {
			int new_lift;			// Might climb/descend.
							// For now, look at 1 tile's height,
							//   and step up/down 2 (needed for SI
							//   Crystal Ball).
			new_lift = gmap.spotAvailable(1, to.tx, to.ty, to.tz, getMoveFlags(), 2, 2);
			if (new_lift < 0)
				return -1;
			to.tz = (short)new_lift;		// (I don't think we should do this
							//    above...)
			return 1;
		}
		@Override				// Estimate cost between two points.
		public int estimateCost(Tile from, Tile to) {
			return from.distance2d(to);	// Distance() does world-wrapping.
		}
		@Override				// Is tile at the goal?
		public boolean atGoal(Tile tile, Tile goal) {
			if (tile.distance2d(goal) > dist)
				return false;		// Not close enough.
			int dz = tile.tz - goal.tz;	// Want to be within 1 story.
			return dz <= 5 && dz >= -5;
		}
		/* ++++++++++FINISH
		static int is_grabable(Tile_coord from, Tile_coord to);
		static int is_grabable(Game_object *from, Game_object *to);
		static int is_grabable(Game_object *from, Tile_coord to);
						// Check for unblocked straight path.
		static int is_straight_path(Tile_coord from, Tile_coord to);
		static int is_straight_path(Game_object *from, Game_object *to);
		*/
	}
	public static class MonsterClient extends FastClient {	
		private Rectangle destbox = new Rectangle();// Got to intersect this box.
		private Rectangle abox = new Rectangle();	// Temp.
		private int intelligence;		// NPC's intelligence.
		private int axtiles, aytiles, aztiles;	// NPC's dims. in tiles.
		// For combat:
		public MonsterClient(Actor attacker, int reach, GameObject opponent) {
			super(reach);
			intelligence = attacker.getProperty(Actor.intelligence);
			ShapeInfo info1 = attacker.getInfo();
			int frame1 = attacker.getFrameNum();
			axtiles = info1.get3dXtiles(frame1);
			aytiles = info1.get3dYtiles(frame1);
			aztiles = info1.get3dHeight();
			if (opponent == null)
				return;			// Means this isn't usable.
			setMoveFlags(attacker.getTypeFlags());
			ShapeInfo info2 = opponent.getInfo();
			int opptx = opponent.getTileX(), oppty = opponent.getTileY();
			int frame2 = opponent.getFrameNum();
			int oxtiles = info2.get3dXtiles(frame2), 
				oytiles = info2.get3dYtiles(frame2);
			destbox.set(opptx - oxtiles + 1, oppty - oytiles + 1,
									oxtiles, oytiles);
			destbox.enlarge(reach);		// This is how close we need to get.
		}
		@Override			// Figure when to give up.
		public int getMaxCost(int cost_to_goal) {
			int max_cost = 2*cost_to_goal;	// Don't try to hard.
			// Use intelligence.
			max_cost += intelligence/2;
			// Limit to 3/4 screen width.
			int scost = ((3*gwin.getWidth())/4)/EConst.c_tilesize;
			if (max_cost > scost)
				max_cost = scost;
			if (max_cost < 18)		// But not too small.
				max_cost = 18;
			return max_cost;
		}
		@Override				// Is tile at the goal?
		public boolean atGoal(Tile tile, Tile goal) {
			int dz = tile.tz - goal.tz;	// Got to be on same floor.
			if (dz/5 != 0)
				return false;
			abox.set(tile.tx - axtiles + 1, tile.ty - aytiles + 1,
								axtiles, aytiles);
			return abox.intersects(destbox);
		}
		@Override				// Figure cost for a single step.
		public int getStepCost(Tile from, Tile to) {
			// NOTE: the 'to.tz' field may be modified.
			if (!MapChunk.areaAvailable(axtiles, aytiles, aztiles,
					from, to, getMoveFlags(), 1, -1))
				return -1;
			else
				return 1;
		}
	}
}
