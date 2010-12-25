package com.exult.android;

public final class PartyManager extends GameSingletons {
	public static final int EXULT_PARTY_MAX = 8;
	private int party[];	// NPC #'s of party members.
	private int partyCount;		// # of NPC's in party.
	private int deadParty[];		// NPC #'s of dead party members.
	private int deadPartyCount;
	private Actor valid[];	// NPC's able to walk with Avatar.
	private int validcnt;
	public PartyManager() {
		party = new int[EXULT_PARTY_MAX];
		deadParty = new int[16];
		valid = new Actor[EXULT_PARTY_MAX];
	}
	public void setCount(int n)		// For initializing from file.
		{ partyCount = n; }
	public void setMember(int i, int npcnum)
		{ party[i] = npcnum; }
	public int getCount()			// Get # party members.
		{ return partyCount; }
	public int getMember(int i)		// Get npc# of i'th party member.
		{ return party[i]; }
	public int getDeadCount()		// Same for dead party members.
		{ return deadPartyCount; }
	public int getDeadMember(int i)
		{ return deadParty[i]; }
					// Add/remove party member.
	/*
	 *	Add NPC to party.
	 *	Output:	false if no room or already a member.
	 */
	public boolean addToParty(Actor npc) {
		int maxparty = party.length;
		if (npc == null || partyCount == maxparty || npc.getFlag(GameObject.in_party))
			return false;
		removeFromDeadParty(npc);	// Just to be sure.
		npc.setPartyId(partyCount);
		npc.setFlag (GameObject.in_party);
						// We can take items.
		npc.setFlagRecursively(GameObject.okay_to_take);
		party[partyCount++] = npc.getNpcNum();
		return true;
	}
	/*
	 *	Remove party member.
	 *	Output:	false if not found.
	 */
	public boolean removeFromParty(Actor npc) {
		if (npc == null)
			return false;
		int id = npc.getPartyId();
		if (id == -1)			// Not in party?
			return false;
		int npc_num = npc.getNpcNum();
		if (party[id] != npc_num) {
			System.out.println("Party mismatch!!");
			return false;
		}
						// Shift the rest down.
		for (int i = id + 1; i < partyCount; i++) {
			Actor npc2 = gwin.getNpc(party[i]);
			if (npc2 != null)
				npc2.setPartyId(i - 1);
			party[i - 1] = party[i];
		}
		npc.clearFlag (GameObject.in_party);
		partyCount--;
		party[partyCount] = 0;
		npc.setPartyId(-1);
		return true;
	}
	/*
	 *	Find index of NPC in dead party list.
	 *	Output:	Index, or -1 if not found.
	 */
	public int inDeadParty(Actor npc) {
		int num = npc.getNpcNum();
		for (int i = 0; i < deadPartyCount; i++)
			if (deadParty[i] == num)
				return i;
		return -1;
	}
	/*
	 *	Add NPC to dead party list.
	 *	Output:	false if no room or already a member.
	 */
	public boolean addToDeadParty(Actor npc) {
		int maxparty = deadParty.length;
		if (npc == null || deadPartyCount == maxparty || inDeadParty(npc) >= 0)
			return false;
		deadParty[deadPartyCount++] = npc.getNpcNum();
		return true;
	}
	/*
	 *	Remove NPC from dead party list.
	 *	Output:	false if not found.
	 */
	public boolean removeFromDeadParty(Actor npc) {
		if (npc == null)
			return false;
		int id = inDeadParty(npc);	// Get index.
		if (id == -1)			// Not in list?
			return false;
						// Shift the rest down.
		for (int i = id + 1; i < deadPartyCount; i++)
			deadParty[i - 1] = deadParty[i];
		deadPartyCount--;
		deadParty[deadPartyCount] = 0;
		return true;
	}
	// Update status of NPC that died or was resurrected.
	public void updatePartyStatus(Actor npc) {
		if (npc.isDead()) {		// Dead?
					// Move party members to dead list.
			if (removeFromParty(npc))
				addToDeadParty(npc);
		} else {				// Alive.
			if (removeFromDeadParty(npc))
				addToParty(npc);
		}
	}
	/*
	 *	In case NPC's were read after usecode, set party members' id's, and
	 *	move dead members into separate list.
	 */
	public void linkParty() {
		// avatar is a party member too
		gwin.getMainActor().setFlag(GameObject.in_party);
						// You own your own stuff.
		gwin.getMainActor().setFlagRecursively(GameObject.okay_to_take);
		int maxparty = party.length;
		int tmpParty[] = new int[maxparty];
		int tmpPartyCount = partyCount;
		int i;
		for (i = 0; i < maxparty; i++)
			tmpParty[i] = party[i];
		partyCount = deadPartyCount = 0;
						// Now process them.
		for (i = 0; i < tmpPartyCount; i++) {
			if (party[i] <= 0)	// Fix corruption.
				continue;
			Actor npc = gwin.getNpc(party[i]);
			int oldid;
			if (npc == null ||		// Shouldn't happen!
						// But this has happened:
			    ((oldid = npc.getPartyId()) >= 0 && 
								oldid < partyCount))
				continue;	// Skip bad entry.
			int npc_num = npc.getNpcNum();
			if (npc.isDead()) {	// Put dead in special list.
				npc.setPartyId(-1);
				if (deadPartyCount >= deadParty.length)
					continue;
				deadParty[deadPartyCount++] = npc_num;
				continue;
			}
			npc.setPartyId(partyCount);
			party[partyCount++] = npc_num;
						// We can use all his/her items.
			npc.setFlagRecursively(GameObject.okay_to_take);
			npc.setFlag (GameObject.in_party);
		}
	}
	/*
	 *	For each party member, this array has the party ID's (or -1) of the
	 *	two member's followers, arrayed as follows:
	 *				A
	 *		       0 1
	 *		      2 3 4
	 *		     7 5 6 8
	 */
	private static final int followers[][] = {
		{0, 1},				// These follow Avatar (ID = -1).
		{2, 3},				// Follow 0.
		{-1, 4},			// Follow 1.
		{7, -1},			// Follow 2.
		{5, 6},				// Follow 3.
		{-1, 8},			// Follow 4.
		{-1, -1}, {-1, -1}, {-1, -1}};
	/*
	 *	Offsets for the follower, depending on direction (0-3, with
	 *	0 = North, 1 = East, 2 = South, 3 = West).
	 */
	// Follower is behind and to left.
	private static final int left_offsets[][] = {	
		{-2, 2},			// North.
		{-2, -2},			// East.
		{2, -2},			// South.
		{2, 2} };			// West.
	// Follower is behind and to right.
	private static final int right_offsets[][] = {	
		{2, 2},				// North.
		{-2, 2},			// East.
		{-2, -2},			// South.
		{2, -2} };			// West.
					// Formation-walking:
	public void getFollowers(int dir) {	
		validcnt = 0;			// Get party members to control.
		for (int i = 0; i < partyCount; i++) {
			Actor npc = gwin.getNpc(party[i]);
			if (npc == null || npc.getFlag(GameObject.asleep) ||
					npc.getFlag(GameObject.paralyzed) ||
					npc.isDead())
				continue;	// Not available.
			int sched = npc.getScheduleType();
				// Skip if in combat or set to 'wait'.
			if (sched != Schedule.combat && sched != Schedule.wait &&
					// Loiter added for SI.
					sched != Schedule.loiter &&
					!npc.inQueue())	// Already walking?
				valid[validcnt++] = npc;
		}
		if (validcnt > 0)
			moveFollowers(gwin.getMainActor(), -1, dir);
	}
	/*
	 *	To walk in formation, each party member will have one or two other
	 *	party members who will follow him on each step.
	 */
	private void moveFollowers
		(
		Actor npc,			// Party member who just stepped.
		int vindex,			// Index within 'valid'.
		int dir				// Direction (0-7) he stepped in.
		)
		{
		int id = npc.getPartyId();	// (-1 if Avatar).
		int tx = npc.getTileX(), ty = npc.getTileY(), tz = npc.getLift();
		int lnum = followers[1 + id][0], rnum = followers[1 + id][1];
		if (lnum == -1 && rnum == -1)
			return;			// Nothing to do.
		int dir4 = dir/2;		// 0-3 now.
		Actor lnpc = (lnum == -1 || lnum >= validcnt) ? null : valid[lnum];
		Actor rnpc = (rnum == -1 || rnum >= validcnt) ? null : valid[rnum];
		int ldir = -1, rdir = -1;
						// Have each take a step.
						// ++++++NICE not to have keep creating Tile's.
		if (lnpc != null)
			ldir = step(lnpc, npc, dir, new Tile(
				tx + left_offsets[dir4][0], 
				ty + left_offsets[dir4][1], tz));
		if (rnpc != null)
			rdir = step(rnpc, npc, dir, new Tile(
				tx + right_offsets[dir4][0], 
				ty + right_offsets[dir4][1], tz));
		if (ldir >= 0 && !lnpc.isDead())
			moveFollowers(lnpc, lnum, ldir);
		if (rdir >= 0 && !rnpc.isDead())
			moveFollowers(rnpc, rnum, rdir);
		}

	/*
	 *	Get tile to step to, given destination tile (possibly more than 1
	 *	step away), and the party's direction.
	 */
	private static final void getStepTile
		(
		Tile result,		// Filled in.
		Tile pos,			// Current pos.
		Tile dest,			// Desired dest.
		int dir				// Dir. party is moving (0-7).
		)
		{
		int dx = dest.tx - pos.tx, dy = dest.ty - pos.ty;
		if (dx < -1)
			dx = -1;		// Limit to 1 tile.
		else if (dx > 1)
			dx = 1;
		if (dy < -1)
			dy = -1;
		else if (dy > 1)
			dy = 1;
		result.set(pos.tx + dx, pos.ty + dy, pos.tz);
	}
	/*
	 *	Find the party member occupying a given tile, starting with a given
	 *	party #.
	 *	Note:	Maybe it should check a rectangle of tiles someday if we want
	 *		to have NPC's bigger than 1 tile.
	 */
	private final Actor findMemberBlocking
		(
		Tile pos,			// Position to check.
		int first			// Party ID to start with.
		)
		{
		for (int i = first; i < partyCount; i++) {
			Actor npc = gwin.getNpc(getMember(i));
			pos.tz = (short)npc.getLift();// Use NPC's, since it might be up/dn
						//   by a step.
			if (npc.blocks(pos))
				return npc;	// Found.
		}
		return null;
	 }
	/*
	 *	Get the direction from a tile to NPC's position.
	 */

	private static int getDirFrom
		(
		Actor npc,
		Tile from
		)
		{
		int tx = npc.getTileX(), ty = npc.getTileY();
		return EUtil.getDirection(from.ty - ty, tx - from.tx);
	}
	/*
	 *	Is the straight path to the leader clear, and less than 5 tiles?
	 */

	private final boolean clearToLeader
		(
		Actor npc,
		Actor leader,
		Tile from			// Start from here.
		)
		{
		int destTz = leader.getLift();
		int dist = leader.distance(from);
		if (dist > 4)
			return false;		// Too far.
		Tile next = new Tile();
		while (--dist > 0)			// Check tiles up to there.
			{
			int dir = getDirFrom(leader, from);
			from.getNeighbor(next, dir);
			if (!npc.areaAvailable(next, from, 0)) {
				Actor bnpc = findMemberBlocking(next, 0);
				if (bnpc == null)
					return false;	// Blocked by non-party-member.
				next.tz = (short)bnpc.getLift();
			}
			from.set(next.tx, next.ty, next.tz);
		}
		int difftz = from.tz - destTz;	// Check diff. in z-coords.
		return difftz*difftz <= 1;	// Can't be more than 2.
	}
	/*
	 *	Get a notion of 'cost' for stepping to a particular tile.
	 *
	 *	Output:	Currently, 10000 if blocked, or (dist)**2.
	 */
	private static final int max_cost = 10000;

	private int getCost
		(
		Actor npc,			// NPC to take the step.
		Actor leader,			// NPC he's following.
		Tile to,			// Tile to step to.
		Actor find_blocking[]	// Returns blocking party member.
		)
		{
		int cost = 0;
		find_blocking[0] = null;
		if (!npc.areaAvailable(to, null, 0)) {	// (To.tz is updated.)
						// Can't go there.
						// Find member we can swap with.
			find_blocking[0] = findMemberBlocking(to,
							1 + npc.getPartyId());
			if (find_blocking[0] == null)
				return max_cost;
			to.tz = (short)find_blocking[0].getLift();
			cost += 1;	// Assess one point to swap.
		}
		int lposx = leader.getTileX(), lposy = leader.getTileY(),
			lposz = leader.getLift();
		int difftz = to.tz - lposz,	// Measure closeness.
		    diffty = Tile.delta(to.ty, lposy),
		    difftx = Tile.delta(to.tx, lposx);
						// Get dist**2 in x-y plane.
		int xydist2 = diffty*diffty + difftx*difftx;
		cost += difftz*difftz + xydist2;
		if (xydist2 > 2) {		// More than 1 tile away?
						// Check 1 more tile towards leader.
			if (!clearToLeader(npc, leader, to))
				cost += 16;	// If blocked, try to avoid.
		}
		return cost;
	}
	/*
	 *	Take best step to follow the leader.
	 *
	 *	Output:	True if a step taken.
	 */
	private boolean takeBestStep
		(
		Actor npc,
		Actor leader,
		Tile pos,			// Current pos.
		int frame,			// Frame to show.
		int dir				// Direction we want to go.
		)
		{
		final int deltadir[] = {0, 1, 7, 2, 6, 3, 5};
		int cnt = deltadir.length;

		int best_cost = max_cost + 8;
		Tile best = new Tile(-1, -1, -1);
		Actor best_in_way = null;
		Actor in_way[] = new Actor[1];
		Tile to = new Tile();
		for (int i = 0; i < cnt; i++)
			{
			int diri = (dir + deltadir[i])%8;
			pos.getNeighbor(to, diri);
								// Fudge cost with diff. in dir.
			int cost = getCost(npc, leader, to, in_way);
			if (cost < best_cost) {
				best_cost = cost;
				best_in_way = in_way[0];
				best.set(to.tx, to.ty, to.tz);
			}
		}
		if (best_cost >= max_cost)
			return false;
		if (best_in_way == null)		// Nobody in way?
			return npc.step(best, frame, false);
		best_in_way.getTile(best);	// Swap positions.
		npc.removeThis();
		best_in_way.removeThis();
		npc.setFrame(frame);		// Appear to take a step.
		npc.move(best);
		best_in_way.move(pos);
		return true;
	}
	/*
	 *	See if a step is reasonable.  This is the first test made.
	 */
	private boolean isStepOkay
		(
		Actor npc,			// NPC to take the step.
		Actor leader,			// NPC he's following.
		Tile to			// Tile to step to.
		)
		{
		if (npc.getChunk() == null ||
		    !npc.areaAvailable(to, null, 0))	// (To.tz is updated.)
			return false;
		int difftz = to.tz - leader.getLift();
		difftz *= difftz;		// Deltaz squared.
		if (difftz > 4)			// More than 2?
			return false;		// We'll want to find best dir.
						// How close in XY?
		int dist = leader.distance(to);
		if (dist == 1)
			return (difftz <= 1);	// 1 tile away, so want dz <= 1.
		if (!clearToLeader(npc, leader, to))
			return false;		// Couldn't take a 2nd step.
		return true;
		}

	/*
	 *	Move one follower to its destination (if possible).
	 *
	 *	Output:	Direction (0-7) moved (or given 'dir' if we don't move).
	 */

	int step
		(
		Actor npc,
		Actor leader,			// Who NPC is following.
		int dir,			// Direction we're walking (0-7).
		Tile dest			// Destination tile.
		)
		{
		Tile pos = new Tile();
		npc.getTile(pos);	// Current position.
		Tile to = new Tile();
		getStepTile(to, pos, dest, dir);
		if (to.tx == pos.tx && to.ty == pos.ty)
			return dir;		// Not moving.
	//TEST:
		if (npc.inQueue() || npc.isMoving())
			System.out.println(npc.getName() + " shouldn't be stepping!");
		Actor.FramesSequence frames = npc.getFrames(dir);
		int step_index = npc.getStepIndex();
		if (step_index == 0)		// First time?  Init.
			step_index = frames.findUnrotated(npc.getFrameNum());
						// Get next (updates step_index).
		step_index = frames.nextIndex(step_index);
		int frame = frames.get(step_index);
		npc.setStepIndex(step_index);
						// Want dz<=1, dx<=2, dy<=2.
		if (isStepOkay(npc, leader, to) && npc.step(to, frame, false))
			;
						// Could have died from stepping on
						//   something.
		else if (npc.isDead() ||
			 !takeBestStep(npc, leader, pos, frame, 	
							npc.getDirection(dest)))
			{			// Failed to take a step.
			System.out.println(npc.getName() + " failed to take a step");
			npc.setStepIndex(frames.prevIndex(step_index));
			return dir;
			}
		return getDirFrom(npc, pos);
	}

}
