package com.exult.android;

public final class PartyManager extends GameSingletons {
	public static final int EXULTParty_MAX = 8;
	private int party[];	// NPC #'s of party members.
	private int partyCount;		// # of NPC's in party.
	private int deadParty[];		// NPC #'s of dead party members.
	private int deadPartyCount;
	private Actor valid[];	// NPC's able to walk with Avatar.
	private int validcnt;
					// Formation-walking:
	/* ++++++++FINISH
	void moveFollowers(Actor npc, int vindex, int dir);
	int step(Actor npc, Actor leader, int dir, Tile_coord dest);
	*/
	public PartyManager() {
		party = new int[EXULTParty_MAX];
		deadParty = new int[16];
		valid = new Actor[EXULTParty_MAX];
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
		npc.clear_flag (GameObject.in_party);
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
					// Formation-walking:
	public void getFollowers(int dir) {
		//+++++++++++FINISH
	}
}
