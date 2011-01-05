package com.exult.android;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;

public class AStarPathFinder extends PathFinder {
	private PriorityQueue<SearchNode> open;	// Nodes to be done, by priority.
	private HashMap<Tile,SearchNode> lookup;		// For finding each tile's node.
	private Tile ntile = new Tile();		// For going through neighbors.
	private Tile path[];					// The resulting path.
	private int dir;						// -1 or 1
	private int stop;						// Index in path to stop at.
	private int nextIndex;					// Index of next tile in 'path' to return.
	private static NodeComparator cmp = new NodeComparator();
	public AStarPathFinder() {
		open = new PriorityQueue<SearchNode>(300, cmp);
		lookup = new HashMap<Tile,SearchNode>(300);
	}
	public boolean NewPath(Tile s, Tile d, Client client) {	
		/* Not using these for now.
		src = s;			// Store start, destination.
		dest = d;
		*/
		path = null;		// Clear out old path, if there.
		nextIndex = 0;
		dir = 1;
		stop = 0;
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
		n.set(path[nextIndex].tx, path[nextIndex].ty, path[nextIndex].tz);
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
			if (client.atGoal(curtile, goal)) {
						// Success.
				path = node.createPath();
				return true;
			}
			// Go through neighbors.
			for (int dir = 0; dir < 8; ++dir) {
				curtile.getNeighbor(ntile, dir);
				// Get cost to next tile.
				int stepCost = client.getStepCost(curtile, ntile);
						// Blocked?
				if (stepCost == -1)
					continue;
						// Get cost from start to ntile.
				int newCost = node.startCost + stepCost;
						// See if next tile already seen.
				SearchNode next = find(ntile);
						// Already there, and cheaper?
				if (next != null && next.startCost <= newCost)
					continue;
				int newGoalCost = client.estimateCost(ntile, goal);
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
			Tile t1 = n1.tile, t2 = n2.tile;
			if (t1.tx < t2.tx) return -1;
			else if (t1.tx > t2.tx) return 1;
			else if (t1.ty < t2.ty) return -1;
			else if (t1.ty > t2.ty) return 1;
			else if (t1.tz < t2.tz) return -1;
			else if (t1.tz > t2.tz) return 1;
			else return 0;
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
			tile = t;
			startCost = scost; goalCost = gcost;
			totalCost = scost + gcost;
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
