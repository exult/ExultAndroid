package com.exult.android;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Comparator;

public class AStarPathFinder extends PathFinder {
	private PriorityQueue<SearchNode> open;	// Nodes to be done, by priority.
	private HashSet<SearchNode> lookup;		// For finding each tile's node.
	private Tile path[];					// The resulting path.
	private int dir;						// -1 or 1
	private int stop;						// Index in path to stop at.
	private int nextIndex;					// Index of next tile in 'path' to return.
	private static NodeComparator cmp = new NodeComparator();
	public AStarPathFinder() {
		open = new PriorityQueue<SearchNode>(300, cmp);
		lookup = new HashSet<SearchNode>(300);
	}
	public boolean NewPath(Tile s, Tile d) {
		return false; //++++++++++++
	}
	public boolean getNextStep(Tile n) {
		return false;//+++++++++
	}
	public int getNumSteps() {
		return 0;//++++++++++++
	}
	public boolean isDone() {
		return true;//++++++++++++++++
	}
	private void add(SearchNode nd) {
		open.offer(nd);
		lookup.add(nd);
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
			//++++++++++FINISH
		}
			
		return false;	// Failed if here.
	}
	
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
		SearchNode(Tile t, int scost, int gcost, SearchNode p) {
			tile = t;
			startCost = scost; goalCost = gcost;
			totalCost = scost + gcost;
		}
		void set(short scost, short gcost, SearchNode p) {
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
