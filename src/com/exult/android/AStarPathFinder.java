package com.exult.android;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;

public class AStarPathFinder extends PathFinder {
	public static boolean debug = false;
	private PriorityQueue<SearchNode> open;	// Nodes to be done, by priority.
	private HashMap<Tile,SearchNode> lookup;		// For finding each tile's node.
	private Tile ntile = new Tile();		// For going through neighbors.
	private Tile path[];					// The resulting path.
	private int dir;						// -1 or 1
	private int stop;						// Index in path to stop at.
	private int nextIndex;					// Index of next tile in 'path' to return.
	private static NodeComparator cmp;
	public AStarPathFinder() {
		cmp = new NodeComparator();
		open = new PriorityQueue<SearchNode>(300, cmp);
		lookup = new HashMap<Tile,SearchNode>(300);
	}	
	public boolean followingSmartPath() {
		return true;
	}
	public boolean NewPath(Tile s, Tile d, Client client) {
		// Store start, destination.
		if (src == null)
			src = new Tile();
		if (dest == null)
			dest = new Tile();
		src.set(s);
		dest.set(d);
		path = null;		// Clear out old path, if there.
		nextIndex = 0;
		dir = 1;
		stop = 0;
		open.clear();
		lookup.clear();
		if (!findPath(s, d, client))
			return false;
		stop = path.length;
		return true;
	}
	public boolean getNextStep(Tile n) {
		if (nextIndex == stop) {
			// done = true;
			return false;
		}
		n.set(path[nextIndex]);
		nextIndex += dir;
		//done = (nextIndex == stop);
		return true;
	}
	public int getNumSteps() {
		return (stop - nextIndex)*dir;
	}
	public boolean isDone() {
		return nextIndex == stop;
	}
	public boolean setBackwards() {
		dir = -1;
		stop = -1;
		nextIndex = path.length - 1;
		return true;
	}
	private void add(SearchNode nd) {
		open.offer(nd);
		lookup.put(nd.tile, nd);
	}
	private SearchNode find(Tile t) {
		return lookup.get(t);
	}
	private boolean findPath(Tile start, Tile goal, PathFinder.Client client) {
		int maxCost = client.estimateCost(start, goal);
		// Create start node.
		add(new SearchNode(start, 0, maxCost, null));
		// Figure when to give up.
		maxCost = client.getMaxCost(maxCost);
		SearchNode node;		// Try 'best' node each iteration.
		while ((node = open.poll()) != null) {
			Tile curtile = node.tile;
			//System.out.printf("AStar: curtile is %1$d, %2$d, goal is %3$d, %4$d\n", 
			//		curtile.tx, curtile.ty, goal.tx, goal.ty);
			//System.out.println("Curtile totalCost = " + node.totalCost);
			if (client.atGoal(curtile, goal)) {
						// Success.
				path = node.createPath();
				if (debug)
					System.out.printf("AStar: SUCCESS.  Path.length = %1$d\n", path.length);
				return true;
			}
			// Go through neighbors.
			for (int dir = 0; dir < 8; ++dir) {
				
				curtile.getNeighbor(ntile, dir);
				
				// Get cost to next tile.
				int stepCost = client.getStepCost(curtile, ntile);
				//System.out.printf("AStar: neighbor is %1$d, %2$d, stepCost = %3$d\n", 
				//	ntile.tx, ntile.ty, stepCost);
						// Blocked?
				if (stepCost == -1)
					continue;
						// Get cost from start to ntile.
				int newCost = node.startCost + stepCost;
						// See if next tile already seen.
				SearchNode next = find(ntile);
				//if (next != null)
				//	System.out.printf("next.startCost = %1$d, newCost = %2$d", 
				//			next.startCost, newCost);
						// Already there, and cheaper?
				if (next != null && next.startCost <= newCost)
					continue;
				int newGoalCost = client.estimateCost(ntile, goal);
				// System.out.println("newGoalCost = " + newGoalCost);
						// Skip nodes too far away.
				if (newCost + newGoalCost >= maxCost)
					continue;
				if (next == null) {	// Create if necessary.
					next = new SearchNode(ntile, newCost,
							newGoalCost, node);
					add(next);
				} else {	// It's going to move.
					open.remove(next);
					next.set(newCost, newGoalCost, node);
					open.offer(next);
				}
			}
		}
		return false;	// Failed if here.
	}
	/*
	 * Local classes.
	 */
	static class NodeComparator implements Comparator<SearchNode> {
		public int compare(SearchNode n1, SearchNode n2) {
			return n1.totalCost - n2.totalCost;
		}
	}
	static class SearchNode {
		Tile tile;			// The coords (x, y, z) in tiles.
		int startCost;		// Actual cost from start.
		int goalCost;		// Estimated cost to goal.
		int totalCost;		// Sum of the two above.
		boolean open;		// In priority queue.
		SearchNode parent;		// Prev. in path.
		SearchNode() {
		}
		SearchNode(Tile t, int scost, int gcost, SearchNode p) {
			tile = new Tile(t.tx, t.ty, t.tz);
			startCost = scost; goalCost = gcost;
			totalCost = scost + gcost;
			parent = p;
		}
		void set(int scost, int gcost, SearchNode p) {
			startCost = scost;
			goalCost = gcost;
			totalCost = gcost + scost;
			parent = p;
		}
		@Override
		public int hashCode() {
			return ((tile.tz << 24) + (tile.ty << 12) + tile.tx);
		}
		@Override
		public boolean equals(Object o2) {
			SearchNode nd2 = (SearchNode)o2;
			return tile.equals(nd2.tile);
		}
		//	Create path back to start.
		Tile[] createPath() {
			int cnt = 1;	// This.			
			// Count back to start.
			SearchNode each = this;
			while ((each = each.parent) != null)
				cnt++;
			int pathlen = cnt - 1;	// Don't want starting tile.
			Tile path[] = new Tile[pathlen];
			each = this;
			for (int i = pathlen - 1; i >= 0; i--) {
				path[i] = each.tile;
				each = each.parent;
			}
			return path;
		}
	}
}
